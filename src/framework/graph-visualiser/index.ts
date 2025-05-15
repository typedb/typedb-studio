import MultiGraph from "graphology";
import Sigma from "sigma";
import ForceSupervisor from "graphology-layout-force/worker";
import { Settings as SigmaSettings } from "sigma/settings";
import {QueryConstraintAny, QueryStructure} from "../typedb-driver/query-structure";
import { ApiResponse, isApiErrorResponse, QueryResponse } from "../typedb-driver/response";
import { StudioConverterStructureParameters, StudioConverterStyleParameters } from "./config";

import * as studioDefaultSettings from "./defaults";
import { constructGraphFromRowsResult, VisualGraph } from "./graph";
import {InteractionHandler} from "./interaction";
import { convertLogicalGraphWith } from "./visualisation";
import {LayoutWrapper} from "./layouts";
import chroma from "chroma-js";
import {shouldCreateEdge, shouldCreateNode, StudioConverter} from "./converter";

export interface StudioState {
    activeQueryDatabase: string | null;
}

export class GraphVisualiser {
    graph: VisualGraph;
    sigma: Sigma;
    layout: LayoutWrapper;
    interactionHandler: InteractionHandler;
    state: StudioState;
    private styleParameters: StudioConverterStyleParameters = studioDefaultSettings.defaultQueryStyleParameters;
    private structureParameters: StudioConverterStructureParameters = studioDefaultSettings.defaultStructureParameters;

    constructor(graph: VisualGraph, sigma: Sigma, layout: LayoutWrapper) {
        this.graph = graph;
        this.sigma = sigma;
        this.layout = layout;
        this.state = { activeQueryDatabase: null };
        this.interactionHandler = new InteractionHandler(graph, sigma, this.state, studioDefaultSettings.defaultQueryStyleParameters);
    }

    handleQueryResponse(res: ApiResponse<QueryResponse>, database: string) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType === "conceptRows") {
            this.state.activeQueryDatabase = database;
            this.handleQueryResult(res);
            this.layout.startOrRedraw();
        }
    }

    handleQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType == "conceptRows" && res.ok.queryStructure != null) {
            let converter = new StudioConverter(this.graph, res.ok.queryStructure, false, this.structureParameters, this.styleParameters);
            let logicalGraph = constructGraphFromRowsResult(res.ok); // In memory, not visualised
            this.graph.clear();
            convertLogicalGraphWith(logicalGraph, converter);
        }
    }

    handleExplorationQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType == "conceptRows" && res.ok.queryStructure != null) {
            let converter = new StudioConverter(this.graph, res.ok.queryStructure, true, this.structureParameters, this.styleParameters);
            let logicalGraph = constructGraphFromRowsResult(res.ok); // In memory, not visualised
            convertLogicalGraphWith(logicalGraph, converter);
        }
    }

    searchGraph(term: string) {

        function safe_str(str: string | undefined): string {
            return (str == undefined) ? "" : str.toLowerCase();
        }

        this.graph.nodes().forEach(node => this.graph.setNodeAttribute(node, "highlighted", false));
        if (term !== "") {
            this.graph.nodes().forEach(node => {
                const attributes = this.graph.getNodeAttributes(node);
                // check concept.type.label if you want to match types of things.
                const concept = attributes.metadata.concept;
                if (("iid" in concept && safe_str(concept.iid).indexOf(term) !== -1)
                    || ("label" in concept && safe_str(concept.label).indexOf(term) !== -1)
                    || ("value" in concept && safe_str(concept.value).indexOf(term) !== -1)) {
                    this.graph.setNodeAttribute(node, "highlighted", true);
                }
            });
        }
    }

    colorEdgesByConstraintIndex(reset: boolean): void {
        this.graph.edges().forEach(edgeKey => {
            let color = reset ?
                this.interactionHandler.styleParameters.edge_color :
                this.getColorForEdge(this.graph, edgeKey);
            this.graph.setEdgeAttribute(edgeKey, "color", color.hex());
        })
    }

    private getColorForEdge(graph: VisualGraph, edgeKey: string): chroma.Color {
        let attributes = graph.getEdgeAttributes(edgeKey);
        let constraintIndex = attributes.metadata.dataEdge.queryCoordinates.constraint;
        return this.getColorForConstraintIndex(constraintIndex);
    }

    colorQuery(queryString: string, queryStructure: QueryStructure): string {
        function shouldColourConstraint(constraint: QueryConstraintAny): boolean {
            switch (constraint.tag) {
                case "isa": return shouldCreateEdge(constraint, constraint.instance, constraint.type);
                case "isa!": return shouldCreateEdge(constraint, constraint.instance, constraint.type);
                case "has":  return shouldCreateEdge(constraint, constraint.owner, constraint.attribute);
                case "links":
                    return shouldCreateEdge(constraint, constraint.relation, constraint.player);
                case "sub":
                    return shouldCreateEdge(constraint, constraint.subtype, constraint.supertype);
                case "sub!":
                    return shouldCreateEdge(constraint, constraint.subtype, constraint.supertype);
                case "owns":
                    return shouldCreateEdge(constraint, constraint.owner, constraint.attribute);
                case "relates":
                    return shouldCreateEdge(constraint, constraint.relation, constraint.role);
                case "plays":
                    return shouldCreateEdge(constraint, constraint.player, constraint.role);
                case "expression":
                    return (
                        constraint.arguments.map(arg => shouldCreateNode(arg)).reduce((a,b) => a || b, false)
                        || constraint.assigned.map(assigned => shouldCreateNode(assigned)).reduce((a,b) => a || b, false)
                    );
                case "functionCall":
                    return (
                        constraint.arguments.map(arg => shouldCreateNode(arg)).reduce((a,b) => a || b, false)
                        || constraint.assigned.map(assigned => shouldCreateNode(assigned)).reduce((a,b) => a || b, false)
                    );
            }
        }
        let spans: number[][] = [];
        queryStructure.blocks.forEach(branch => {
            branch.constraints.forEach((constraint, constraintIndex) => {
                if (shouldColourConstraint(constraint)) {
                    if (constraint.textSpan != null) {
                        spans.push([constraint.textSpan.begin, constraint.textSpan.end, constraintIndex]);
                    }
                }
            })
        })
        spans = spans.sort((a,b) => {
            return (a[0] != b[0]) ?
                a[0] - b[0]:
                b[1] - a[1]; // open ascending, end descending
        });
        // Add one to end-offset so we're AFTER the last character
        let starts_ends_separate = spans.flatMap(span => [[span[0], span[2]], [span[1] + 1, -1]]);
        starts_ends_separate.sort((a,b) => a[0] - b[0]);
        let se_index = 0;
        let highlighted = "";
        for(let i= 0; i<queryString.length; i++) {
            while (se_index < starts_ends_separate.length && starts_ends_separate[se_index][0] == i) {
                let constraintIndexOrEnd = starts_ends_separate[se_index][1];
                if (constraintIndexOrEnd == -1) {
                    highlighted += "</span>"
                } else {
                    let color = this.getColorForConstraintIndex(constraintIndexOrEnd)
                    highlighted += "<span style=\"color: " + color.hex() + "\">";
                }
                se_index += 1;
            }
            highlighted += (queryString[i] == "\n") ? "<br/>": queryString[i];
        }
        return highlighted;
    }

    private getColorForConstraintIndex(constraintIndex: number): chroma.Color {
        let r = ((constraintIndex+1) * 153 % 256);
        let g = ((constraintIndex+1) * 173 % 256);
        let b = ((constraintIndex+1) * 199 % 256);
        return chroma([r,g,b]);
    }

    destroy() {
        this.sigma.kill();
    }
}

export function createSigmaRenderer(containerEl: HTMLElement, sigma_settings: SigmaSettings, graph: MultiGraph) : Sigma {
    // Create the sigma
    return new Sigma(graph, containerEl, sigma_settings);
}
