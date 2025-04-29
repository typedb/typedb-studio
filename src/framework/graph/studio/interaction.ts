import Sigma from "sigma";
import MultiGraph from "graphology";
import {SigmaEventPayload, SigmaNodeEventPayload, SigmaStageEventPayload} from "sigma/types";
import {StudioConverterStyleParameters} from "./config";
import {ThingKind, TypeKind} from "../typedb/concept";
import {StudioState} from "./studio";
import {TypeDBQueryType} from "../typedb/answer.js";
import {SpecialVertexKind} from "../graph.js";

// Ref: https://www.sigmajs.org/docs/advanced/events/
// and: https://www.sigmajs.org/storybook/?path=/story/mouse-manipulations--story

interface InteractionState {
    draggedNode: string | null;
    highlightedAnswer: number | null; // demonstrative
}

export class StudioInteractionHandler {
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
        let node = event.node;
        this.graph.setNodeAttribute(node, "highlighted", true);
        this.graph.setNodeAttribute(node, "label", this.graph.getNodeAttributes(node)["metadata"].hoverLabel)
    }

    onLeaveNode(event: SigmaNodeEventPayload) {
        let node = event.node;
        this.graph.setNodeAttribute(node, "highlighted", false);
        this.graph.setNodeAttribute(node, "label", this.graph.getNodeAttributes(node)["metadata"].defaultLabel);
    }


    onDownNode(event: SigmaNodeEventPayload) {
        let node = event.node;
        this.state.draggedNode = node;
        this.graph.setNodeAttribute(node, "highlighted", true);
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

    onUpNode(event: SigmaNodeEventPayload) {
        if (this.state.draggedNode != null) {
            this.graph.removeNodeAttribute(this.state.draggedNode, "highlighted");
            this.state.draggedNode = null;
        }
    }

    onUpStage(event: SigmaEventPayload) {
        if (this.state.draggedNode != null) {
            this.graph.removeNodeAttribute(this.state.draggedNode, "highlighted");
            this.state.draggedNode = null;
        }
    }

    onDoubleClickNode(event: SigmaNodeEventPayload) {
        // let node = event.node;
        // let attributes = this.graph.getNodeAttributes(node) as { metadata: any };
        // if (this.studioState.activeQueryDatabase == null) {
        //     console.log("Could not dispatch explore query: Unknown active database") // unreachable
        //     return;
        // }
        // let queries = null;
        // switch(attributes.metadata.concept.kind) {
        //     case TypeKind.entityType: {
        //         queries = [
        //             QUERY_EXPLORE_SUBTYPES.replace("<<label>>", attributes.metadata.concept.label),
        //             QUERY_EXPLORE_SUPERTYPE.replace("<<label>>", attributes.metadata.concept.label),
        //             QUERY_EXPLORE_OWNED.replace("<<label>>", attributes.metadata.concept.label),
        //             QUERY_EXPLORE_PLAYS.replace("<<label>>", attributes.metadata.concept.label),
        //         ];
        //         break;
        //     }
        //     case TypeKind.relationType: {
        //         queries = [
        //             QUERY_EXPLORE_SUBTYPES.replace("<<label>>", attributes.metadata.concept.label),
        //             QUERY_EXPLORE_SUPERTYPE.replace("<<label>>", attributes.metadata.concept.label),
        //             QUERY_EXPLORE_RELATES.replace("<<label>>", attributes.metadata.concept.label),
        //             QUERY_EXPLORE_OWNED.replace("<<label>>", attributes.metadata.concept.label),
        //             QUERY_EXPLORE_PLAYS.replace("<<label>>", attributes.metadata.concept.label),
        //         ];
        //         break;
        //     }
        //     case TypeKind.attributeType:{
        //         queries = [
        //             QUERY_EXPLORE_SUBTYPES.replace("<<label>>", attributes.metadata.concept.label),
        //             QUERY_EXPLORE_SUPERTYPE.replace("<<label>>", attributes.metadata.concept.label),
        //             QUERY_EXPLORE_OWNERS.replace("<<label>>", attributes.metadata.concept.label),
        //         ];
        //         break;
        //     }
        //
        //     case ThingKind.entity: {
        //         queries = [
        //             QUERY_EXPLORE_ATTRIBUTES.replace("<<iid>>", attributes.metadata.concept.iid),
        //             QUERY_EXPLORE_RELATIONS.replace("<<iid>>", attributes.metadata.concept.iid),
        //         ];
        //         break;
        //     }
        //     case ThingKind.relation: {
        //         queries = [
        //             QUERY_EXPLORE_ATTRIBUTES.replace("<<iid>>", attributes.metadata.concept.iid),
        //             QUERY_EXPLORE_RELATIONS.replace("<<iid>>", attributes.metadata.concept.iid),
        //             QUERY_EXPLORE_PLAYERS.replace("<<iid>>", attributes.metadata.concept.iid),
        //         ];
        //         break;
        //     }
        //     case TypeKind.roleType:
        //     case ThingKind.attribute:
        //     case "value":
        //     case SpecialVertexKind.unavailable:
        //     case SpecialVertexKind.func:
        //     case SpecialVertexKind.expr:
        //     {
        //         console.log("Unexplorable kind: " + attributes.metadata.concept.kind);
        //         return;
        //     }
        // }
        // if (queries == null) {
        //     throw new Error("unreachable: Expected queries to be non-null");
        // }
        // queries.forEach(query => {
        //     this.driver.runExplorationQuery(this.studioState.activeQueryDatabase!, query, TypeDBQueryType.read)
        //         .then(result => {
        //             if ("err" in result) {
        //                 console.log("Error encountered in exploration query: " + JSON.stringify(result.err));
        //             }
        //         });
        // });
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
            if (answerIndex == this.graph.getEdgeAttributes(edge)["metadata"].answerIndex) {
                this.graph.setEdgeAttribute(edge, "color", this.styleParameters.edge_color.hex());
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


const QUERY_EXPLORE_ATTRIBUTES = "match $x iid <<iid>>; $x has $other;";
const QUERY_EXPLORE_RELATIONS = "match $x iid <<iid>>; $other links ($t: $x);";
const QUERY_EXPLORE_PLAYERS = "match $x iid <<iid>>; $x links ($t: $other);";
// const QUERY_EXPLORE_OWNERS = "match $x iid <<iid>>; $x links ($t: $other)";

const QUERY_EXPLORE_OWNED = "match $x label <<label>>; $x owns $other;";
const QUERY_EXPLORE_OWNERS = "match $x label <<label>>; $other owns $x;";
const QUERY_EXPLORE_RELATES  = "match $x label <<label>>; $x relates $t; $other plays $t;";
const QUERY_EXPLORE_PLAYS= "match $x label <<label>>; $x plays $t; $other relates $t;";

const QUERY_EXPLORE_SUPERTYPE = "match $x label <<label>>; $x sub! $other;";
const QUERY_EXPLORE_SUBTYPES = "match $x label <<label>>; $other sub! $x;";