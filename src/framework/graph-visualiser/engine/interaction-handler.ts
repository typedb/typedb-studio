import type { GraphVisualiser } from "./index";
import Sigma from "sigma";
import MultiGraph from "graphology";
import chroma from "chroma-js";
import { BehaviorSubject, Subject } from "rxjs";
import {SigmaEventPayload, SigmaNodeEventPayload, SigmaStageEventPayload} from "sigma/types";
import {GraphStyles} from "./styles";
import {LayoutWrapper} from "./layout";
import type {GraphStyleService} from "../../../service/graph-style.service";

/**
 * Selection payload emitted when a graph instance node is clicked. Carries
 * just enough info for an upstream consumer (e.g. the Inspector) to look up
 * the full SchemaConcept via the application's schema state. Type nodes
 * (entityType / relationType / attributeType) are not surfaced — only
 * concrete instances are inspectable.
 */
export interface InspectableSelection {
    kind: "entity" | "relation" | "attribute";
    typeLabel: string;
    /** For entity/relation: the IID. For attribute: the value (attributes have
     *  no IID in TypeDB; values uniquely identify them per type). */
    instanceId: string;
}

/**
 * Emitted when the user right-clicks a graph instance node — gives upstream
 * UI (the context menu) what it needs to know which instance was clicked and
 * where on screen to open the menu.
 */
export interface NodeContextMenuEvent {
    target: InspectableSelection;
    /** Viewport coordinates of the click, for positioning the menu trigger. */
    clientX: number;
    clientY: number;
}

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
    visualiser: GraphVisualiser | null = null;
    selection$ = new BehaviorSubject<InspectableSelection | null>(null);
    /**
     * Fires every time the user right-clicks an instance node. Consumers
     * (typically a context-menu component) use this to open a menu at the
     * click coordinates. Type nodes don't emit — they're not actionable.
     */
    nodeContextMenu$ = new Subject<NodeContextMenuEvent>();
    /**
     * True iff the most recent selection change came from the user clicking a
     * node that was already in the previous selection's highlighted neighbor
     * set. The Inspector reads this to decide whether to extend the breadcrumb
     * trail or start a fresh one. Reset to false for programmatic selections
     * and selection clears.
     */
    lastSelectionWasFromHighlight = false;
    /**
     * Additional anchor nodes whose neighborhoods are highlighted alongside
     * the primary selection — populated by the Inspector with the node keys
     * for breadcrumb ancestors, so the union of every step in the exploration
     * chain stays lit up.
     */
    private secondaryAnchors: Set<string> = new Set();

    constructor(public graph: MultiGraph, public renderer: Sigma, private studioState: StudioState, public styleParams: GraphStyles, public styleService?: GraphStyleService) {
        this.state = {
            draggedNode : null,
            didDrag: false,
            highlightedAnswer: null,
            selectedNode: null,
            selectedNeighbors: null,
        };
        this.registerAll(renderer);
        // Sigma's rightClickNode fires from the mousedown event, but the
        // browser still fires its own `contextmenu` afterwards which would
        // overlay the default browser menu on top of ours. Suppress it on
        // the canvas container so only our menu appears.
        renderer.getContainer().addEventListener("contextmenu", e => e.preventDefault());
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
        renderer.on(StudioSigmaEventType.rightClickNode, (e) => this.onRightClickNode(e));
    }


    onEnterNode(event: SigmaNodeEventPayload) {
        const node = event.node;
        const color = this.graph.getNodeAttribute(node, "color");
        this.graph.setNodeAttribute(node, "_originalColor", color);
        this.graph.setNodeAttribute(node, "color", chroma(color).darken(0.3).hex());

        if (this.styleService?.showHoverLabel && !this.styleService.labelsVisible) {
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

        if (this.styleService?.showHoverLabel && !this.styleService.labelsVisible) {
            this.graph.setNodeAttribute(node, "label", "");
        }
    }

    onDownNode(event: SigmaNodeEventPayload) {
        // Only the primary (left) mouse button initiates a drag — right-click
        // is reserved for the context menu, middle-click for browser default.
        const button = (event.event?.original as MouseEvent | undefined)?.button;
        if (button != null && button !== 0) return;
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
        this.visualiser?.clearSearch();
        const node = event.node;
        if (this.state.selectedNode === node) return; // re-clicking the selected node is a no-op
        this.lastSelectionWasFromHighlight = this.state.selectedNeighbors?.has(node) ?? false;
        this.state.selectedNode = node;
        this.recomputeHighlightSet();
        this.selection$.next(this.extractInspectableSelection(node));
    }

    private extractInspectableSelection(node: string): InspectableSelection | null {
        try {
            const concept = this.graph.getNodeAttributes(node)?.["metadata"]?.concept;
            if (!concept) return null;
            if (concept.kind === "entity" || concept.kind === "relation") {
                if (!concept.iid || !concept.type?.label) return null;
                return { kind: concept.kind, typeLabel: concept.type.label, instanceId: concept.iid };
            }
            if (concept.kind === "attribute") {
                if (concept.value == null || !concept.type?.label) return null;
                return { kind: "attribute", typeLabel: concept.type.label, instanceId: String(concept.value) };
            }
            return null;
        } catch {
            return null;
        }
    }

    /**
     * Recursively collects neighbors to highlight when a node is selected.
     *
     * Starting from the clicked node's direct neighbors:
     * - Entity / entityType: also highlight its connected attributes / attributeTypes
     * - Relation / relationType: also highlight all its connected concepts (entities, relations, attributes / their type equivalents)
     * - Attribute / attributeType / value: only the direct owners — we do NOT
     *   expand to the owners' other attributes, since that would highlight
     *   most of the graph any time a common attribute (e.g. a name) is clicked.
     *
     * Recurses up to maxDepth (4) to follow relation chains without blowing up.
     */
    private collectHighlightedNeighbors(root: string, maxDepth = 4): Set<string> {
        const highlighted = new Set<string>();
        const rootKind = this.getNodeKind(root);

        // Seed with direct neighbors of the root
        const directNeighbors = this.graph.neighbors(root);
        const queue: { node: string; depth: number }[] = [];
        for (const neighbor of directNeighbors) {
            if (!highlighted.has(neighbor)) {
                highlighted.add(neighbor);
                queue.push({ node: neighbor, depth: 1 });
            }
        }

        // Attribute clicks stop at direct owners — no further expansion.
        if (rootKind === "attribute" || rootKind === "attributeType") {
            return highlighted;
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
        this.lastSelectionWasFromHighlight = false;
        this.recomputeHighlightSet();
        this.selection$.next(null);
    }

    /**
     * Programmatically select a node — same effect as clicking it: highlight
     * ring, neighbor fade, and selection$ emission for the Inspector.
     */
    selectNode(node: string): void {
        if (this.state.selectedNode === node) return;
        this.lastSelectionWasFromHighlight = false;
        this.state.selectedNode = node;
        this.recomputeHighlightSet();
        this.selection$.next(this.extractInspectableSelection(node));
    }

    /**
     * Replace the set of secondary anchor nodes — additional nodes whose
     * neighborhoods should also stay highlighted alongside the primary
     * selection. The Inspector calls this with the breadcrumb ancestors'
     * node keys whenever the trail changes.
     */
    setSecondaryAnchors(anchors: Set<string>): void {
        this.secondaryAnchors = anchors;
        this.recomputeHighlightSet();
    }

    /**
     * Rebuild `selectedNeighbors` as the union of:
     *   - the primary selection's highlighted neighbors, and
     *   - each secondary anchor (itself) and its highlighted neighbors.
     * Leaves it null when nothing is selected and no anchors are set so the
     * reducer treats the graph as un-highlighted (no fade).
     *
     * Public so callers that mutate the graph (e.g. bulk-add fetches in the
     * Inspector) can ask for the highlight to re-evaluate against the new
     * node set without having to change the selection.
     */
    recomputeHighlightSet(): void {
        if (this.state.selectedNode == null && this.secondaryAnchors.size === 0) {
            this.state.selectedNeighbors = null;
            this.renderer.refresh();
            return;
        }
        const all = new Set<string>();
        if (this.state.selectedNode != null) {
            this.collectHighlightedNeighbors(this.state.selectedNode).forEach(n => all.add(n));
        }
        this.secondaryAnchors.forEach(anchor => {
            all.add(anchor);
            this.collectHighlightedNeighbors(anchor).forEach(n => all.add(n));
        });
        this.state.selectedNeighbors = all;
        this.renderer.refresh();
    }

    onDoubleClickNode(event: SigmaNodeEventPayload) {
    }

    onRightClickNode(event: SigmaNodeEventPayload) {
        // Sigma's right-click event already preventsDefault on the underlying
        // mousedown event so the browser context menu won't show — we just
        // need to surface the target + coordinates.
        const target = this.extractInspectableSelection(event.node);
        if (!target) return; // type nodes etc. aren't actionable
        const mouseEvent = event.event?.original as MouseEvent | undefined;
        if (!mouseEvent) return;
        this.nodeContextMenu$.next({
            target,
            clientX: mouseEvent.clientX,
            clientY: mouseEvent.clientY,
        });
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
