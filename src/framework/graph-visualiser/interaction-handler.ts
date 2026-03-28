import Sigma from "sigma";
import MultiGraph from "graphology";
import chroma from "chroma-js";
import {SigmaEventPayload, SigmaNodeEventPayload, SigmaStageEventPayload} from "sigma/types";
import {GraphStyles} from "./styles";
import {LayoutWrapper} from "./layout";
import type {GraphStyleService} from "../../service/graph-style.service";

// Ref: https://www.sigmajs.org/docs/advanced/events/
// and: https://www.sigmajs.org/storybook/?path=/story/mouse-manipulations--story

export interface StudioState {
    activeQueryDatabase: string | null;
}

interface InteractionState {
    draggedNode: string | null;
    didDrag: boolean;
    highlightedAnswer: number | null; // demonstrative
    selectedNode: string | null;
    selectedNeighbors: Set<string> | null;
}

export class InteractionHandler {
    state: InteractionState;
    layout: LayoutWrapper | null = null;

    constructor(public graph: MultiGraph, public renderer: Sigma, private studioState: StudioState, public styleParams: GraphStyles, public styleService?: GraphStyleService) {
        this.state = {
            draggedNode : null,
            didDrag: false,
            highlightedAnswer: null,
            selectedNode: null,
            selectedNeighbors: null,
        };
        this.registerAll(renderer);
    }

    registerAll(renderer: Sigma) {
        renderer.on(StudioSigmaEventType.enterNode, (e) => this.onEnterNode(e));
        renderer.on(StudioSigmaEventType.leaveNode, (e) => this.onLeaveNode(e));
        renderer.on(StudioSigmaEventType.moveBody, (e) => this.onMoveBody(e));
        renderer.on(StudioSigmaEventType.downNode, (e) => this.onDownNode(e));
        renderer.on(StudioSigmaEventType.upStage, (e) => this.onUpStage(e));
        renderer.on(StudioSigmaEventType.upNode, (e) => this.onUpNode(e));
        renderer.on(StudioSigmaEventType.clickNode, (e) => this.onClickNode(e));
        renderer.on(StudioSigmaEventType.clickStage, () => this.onClickStage());
        renderer.on(StudioSigmaEventType.doubleClickNode, (e) => this.onDoubleClickNode(e));
    }


    onEnterNode(event: SigmaNodeEventPayload) {
        const node = event.node;
        const color = this.graph.getNodeAttribute(node, "color");
        this.graph.setNodeAttribute(node, "_originalColor", color);
        this.graph.setNodeAttribute(node, "color", chroma(color).darken(0.3).hex());

        if (this.styleService && !this.styleService.labelsVisible) {
            const attrs = this.graph.getNodeAttributes(node);
            const concept = attrs["metadata"]?.concept;
            if (concept) {
                this.graph.setNodeAttribute(node, "label", this.styleParams.vertexDefaultLabel(concept));
            }
        }
    }

    onLeaveNode(event: SigmaNodeEventPayload) {
        const node = event.node;
        const original = this.graph.getNodeAttribute(node, "_originalColor");
        if (original) {
            this.graph.setNodeAttribute(node, "color", original);
            this.graph.removeNodeAttribute(node, "_originalColor");
        }

        if (this.styleService && !this.styleService.labelsVisible) {
            this.graph.setNodeAttribute(node, "label", "");
        }
    }

    onDownNode(event: SigmaNodeEventPayload) {
        const node = event.node;
        this.state.draggedNode = node;
        const original = this.graph.getNodeAttribute(node, "_originalColor") ?? this.graph.getNodeAttribute(node, "color");
        this.graph.setNodeAttribute(node, "color", chroma(original).darken(0.9).hex());
        const attrs = this.graph.getNodeAttributes(node);
        this.layout?.fixNode(node, attrs["x"], attrs["y"]);
        if (!this.renderer.getCustomBBox()) {
            this.renderer.setCustomBBox(this.renderer.getBBox());
        }
    }

    onMoveBody(event: SigmaStageEventPayload) {
        // On mouse move, if the drag mode is enabled, we change the position of the draggedNode
        let mouseCoords = event.event;
        if (this.state.draggedNode == null) return;

        // Get new position of node
        const pos = this.renderer.viewportToGraph(mouseCoords);
        this.graph.setNodeAttribute(this.state.draggedNode, "x", pos.x);
        this.graph.setNodeAttribute(this.state.draggedNode, "y", pos.y);
        this.layout?.fixNode(this.state.draggedNode, pos.x, pos.y);
        this.state.didDrag = true;

        // Prevent sigma to move camera:
        mouseCoords.preventSigmaDefault();
        mouseCoords.original.preventDefault();
        mouseCoords.original.stopPropagation();
    }

    onUpNode(_event: SigmaNodeEventPayload) {
        if (this.state.draggedNode != null) {
            this.layout?.unfixNode(this.state.draggedNode);
            const original = this.graph.getNodeAttribute(this.state.draggedNode, "_originalColor");
            if (original) {
                this.graph.setNodeAttribute(this.state.draggedNode, "color", chroma(original).darken(0.3).hex());
            }
            this.state.draggedNode = null;
        }
    }

    onUpStage(_event: SigmaEventPayload) {
        if (this.state.draggedNode != null) {
            this.layout?.unfixNode(this.state.draggedNode);
            const original = this.graph.getNodeAttribute(this.state.draggedNode, "_originalColor");
            if (original) {
                this.graph.setNodeAttribute(this.state.draggedNode, "color", original);
                this.graph.removeNodeAttribute(this.state.draggedNode, "_originalColor");
            }
            this.state.draggedNode = null;
        }
    }

    onClickNode(event: SigmaNodeEventPayload) {
        if (this.state.didDrag) {
            this.state.didDrag = false;
            return;
        }
        const node = event.node;
        if (this.state.selectedNode === node) {
            this.clearSelection();
        } else {
            this.state.selectedNode = node;
            this.state.selectedNeighbors = this.collectHighlightedNeighbors(node);
            this.renderer.refresh();
        }
    }

    /**
     * Recursively collects neighbors to highlight when a node is selected.
     *
     * Starting from the clicked node's direct neighbors:
     * - Entity / entityType: also highlight its connected attributes / attributeTypes
     * - Relation / relationType: also highlight all its connected concepts (entities, relations, attributes / their type equivalents)
     * - Attribute / attributeType / value: no further expansion
     *
     * Recurses up to maxDepth (4) to follow relation chains without blowing up.
     */
    private collectHighlightedNeighbors(root: string, maxDepth = 4): Set<string> {
        const highlighted = new Set<string>();

        // Seed with direct neighbors of the root
        const directNeighbors = this.graph.neighbors(root);
        const queue: { node: string; depth: number }[] = [];
        for (const neighbor of directNeighbors) {
            if (!highlighted.has(neighbor)) {
                highlighted.add(neighbor);
                queue.push({ node: neighbor, depth: 1 });
            }
        }

        while (queue.length > 0) {
            const { node, depth } = queue.shift()!;
            if (depth >= maxDepth) continue;

            const kind = this.getNodeKind(node);
            let shouldExpand = false;
            let expandFilter: ((neighborKind: string | null) => boolean) | null = null;

            if (kind === "entity" || kind === "entityType") {
                // Expand to connected attributes/attributeTypes only
                shouldExpand = true;
                expandFilter = (nk) => nk === "attribute" || nk === "attributeType";
            } else if (kind === "relation" || kind === "relationType") {
                // Expand to all connected concepts
                shouldExpand = true;
                expandFilter = () => true;
            }

            if (shouldExpand) {
                for (const neighbor of this.graph.neighbors(node)) {
                    if (neighbor === root || highlighted.has(neighbor)) continue;
                    const neighborKind = this.getNodeKind(neighbor);
                    if (expandFilter!(neighborKind)) {
                        highlighted.add(neighbor);
                        queue.push({ node: neighbor, depth: depth + 1 });
                    }
                }
            }
        }

        return highlighted;
    }

    private getNodeKind(node: string): string | null {
        try {
            return this.graph.getNodeAttributes(node)?.["metadata"]?.concept?.kind ?? null;
        } catch {
            return null;
        }
    }

    onClickStage() {
        if (this.state.selectedNode != null) {
            this.clearSelection();
        }
    }

    clearSelection() {
        this.state.selectedNode = null;
        this.state.selectedNeighbors = null;
        this.renderer.refresh();
    }

    onDoubleClickNode(event: SigmaNodeEventPayload) {
    }

    highlightAnswer(answerIndex: number) {
        // TODO: Maybe add indexing so I don't have to iterate
        if (this.state.highlightedAnswer != null) {
            this.removeHighlightFromAnswer(this.state.highlightedAnswer);
            this.state.highlightedAnswer = null;
        }
        this.graph.edges().forEach(edge => {
            if (answerIndex == this.graph.getEdgeAttributes(edge)["metadata"].answerIndex) {
                this.graph.setEdgeAttribute(edge, "color", this.styleParams.edgeHighlightColor.hex());
            }
        })
        this.state.highlightedAnswer = answerIndex;
    }

    removeHighlightFromAnswer(answerIndex: number) {
        // TODO: Maybe add indexing so I don't have to iterate
        this.graph.edges().forEach(edge => {
            const metadata = this.graph.getEdgeAttributes(edge)["metadata"];
            if (answerIndex == metadata.answerIndex) {
                const tag = metadata.dataEdge.tag;
                const color = this.styleParams.edgeLabelColors?.[tag]
                    ?? this.styleParams.edgeColor.hex();
                this.graph.setEdgeAttribute(edge, "color", color);
            }
        })
    }

    searchGraph(term: string) {
        function safeString(str: string | undefined): string {
            return (str == undefined) ? "" : str.toLowerCase();
        }

        this.graph.nodes().forEach(node => this.graph.setNodeAttribute(node, "highlighted", false));
        if (term !== "") {
            this.graph.nodes().forEach(node => {
                const attributes = this.graph.getNodeAttributes(node);
                if ("concept" in attributes["metadata"]) {
                    const concept = attributes["metadata"].concept;
                    if (("iid" in concept && safeString(concept.iid).indexOf(term) !== -1)
                        || ("label" in concept && safeString(concept.label).indexOf(term) !== -1)
                        || ("value" in concept && safeString(concept.value).indexOf(term) !== -1)) {
                        this.graph.setNodeAttribute(node, "highlighted", true);
                    }
                }
            });
        }
    }
}

enum StudioSigmaEventType {
    enterNode = "enterNode",
    leaveNode = "leaveNode",
    downNode = "downNode",
    upNode = "upNode",
    clickNode = "clickNode",
    rightClickNode = "rightClickNode",
    doubleClickNode = "doubleClickNode",
    wheelNode = "wheelNode",

    enterEdge = "enterEdge",
    leaveEdge = "leaveEdge",
    downEdge = "downEdge",
    clickEdge = "clickEdge",
    rightClickEdge = "rightClickEdge",
    doubleClickEdge = "doubleClickEdge",
    wheelEdge = "wheelEdge",

    moveBody = "moveBody",
    upStage = "upStage",
    clickStage = "clickStage",

    // Remaining: downStage, rightClickStage, doubleClickStage, wheelStage
    // Remaining: beforeRender, afterRender, resize, kill
}
