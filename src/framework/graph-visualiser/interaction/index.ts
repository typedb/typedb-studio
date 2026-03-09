import Sigma from "sigma";
import MultiGraph from "graphology";
import chroma from "chroma-js";
import {SigmaEventPayload, SigmaNodeEventPayload, SigmaStageEventPayload} from "sigma/types";
import {StudioConverterStyleParameters} from "../style/parameters";
import {SpecialVertexKind} from "../data/types";

// Ref: https://www.sigmajs.org/docs/advanced/events/
// and: https://www.sigmajs.org/storybook/?path=/story/mouse-manipulations--story

export interface StudioState {
    activeQueryDatabase: string | null;
}

interface InteractionState {
    draggedNode: string | null;
    highlightedAnswer: number | null; // demonstrative
}

export class InteractionHandler {
    graph: MultiGraph;
    renderer: Sigma;
    state: InteractionState;
    styleParameters: StudioConverterStyleParameters;
    private studioState: StudioState;

    constructor(graph: MultiGraph, renderer: Sigma, studioState: StudioState, styleParameters: StudioConverterStyleParameters) {
        this.graph = graph;
        this.renderer = renderer;
        this.state = {
            draggedNode : null,
            highlightedAnswer: null,
        };
        this.studioState = studioState;
        this.styleParameters = styleParameters;
        this.registerAll(renderer);
    }

    registerAll(renderer: Sigma) {
        renderer.on(StudioSigmaEventType.enterNode, (e) => this.onEnterNode(e));
        renderer.on(StudioSigmaEventType.leaveNode, (e) => this.onLeaveNode(e));

        renderer.on(StudioSigmaEventType.moveBody, (e) => this.onMoveBody(e));
        renderer.on(StudioSigmaEventType.downNode, (e) => this.onDownNode(e));

        renderer.on(StudioSigmaEventType.upStage, (e) => this.onUpStage(e));
        renderer.on(StudioSigmaEventType.upNode, (e) => this.onUpNode(e));

        renderer.on(StudioSigmaEventType.doubleClickNode, (e) => this.onDoubleClickNode(e));
    }


    onEnterNode(event: SigmaNodeEventPayload) {
        const node = event.node;
        const color = this.graph.getNodeAttribute(node, "color");
        this.graph.setNodeAttribute(node, "_originalColor", color);
        this.graph.setNodeAttribute(node, "color", chroma(color).darken(0.3).hex());
    }

    onLeaveNode(event: SigmaNodeEventPayload) {
        const node = event.node;
        const original = this.graph.getNodeAttribute(node, "_originalColor");
        if (original) {
            this.graph.setNodeAttribute(node, "color", original);
            this.graph.removeNodeAttribute(node, "_originalColor");
        }
    }


    onDownNode(event: SigmaNodeEventPayload) {
        const node = event.node;
        this.state.draggedNode = node;
        const original = this.graph.getNodeAttribute(node, "_originalColor") ?? this.graph.getNodeAttribute(node, "color");
        this.graph.setNodeAttribute(node, "color", chroma(original).darken(0.6).hex());
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

        // Prevent sigma to move camera:
        mouseCoords.preventSigmaDefault();
        mouseCoords.original.preventDefault();
        mouseCoords.original.stopPropagation();
    }

    onUpNode(_event: SigmaNodeEventPayload) {
        if (this.state.draggedNode != null) {
            const original = this.graph.getNodeAttribute(this.state.draggedNode, "_originalColor");
            if (original) {
                this.graph.setNodeAttribute(this.state.draggedNode, "color", chroma(original).darken(0.3).hex());
            }
            this.state.draggedNode = null;
        }
    }

    onUpStage(_event: SigmaEventPayload) {
        if (this.state.draggedNode != null) {
            const original = this.graph.getNodeAttribute(this.state.draggedNode, "_originalColor");
            if (original) {
                this.graph.setNodeAttribute(this.state.draggedNode, "color", original);
                this.graph.removeNodeAttribute(this.state.draggedNode, "_originalColor");
            }
            this.state.draggedNode = null;
        }
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
                this.graph.setEdgeAttribute(edge, "color", this.styleParameters.edge_highlight_color.hex());
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
                const color = this.styleParameters.edge_label_colors?.[tag]
                    ?? this.styleParameters.edge_color.hex();
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

    // Remaining: downStage, clickStage, rightClickStage, doubleClickStage, wheelStage
    // Remaining: beforeRender, afterRender, resize, kill
}
