import Sigma from "sigma";
import MultiGraph from "graphology";
import ForceSupervisor from "graphology-layout-force/worker";
import { QueryStructure } from "../typedb-driver/query-structure";
import { ApiResponse, isApiErrorResponse, QueryResponse } from "../typedb-driver/response";
import { StudioConverterStructureParameters, StudioConverterStyleParameters } from "./config";

import * as studioDefaultSettings from "./defaults";
import { constructGraphFromRowsResult } from "./graph";
import {InteractionHandler} from "./interaction";
import { convertLogicalGraphWith } from "./visualisation";
import {LayoutWrapper} from "./layouts";
import chroma from "chroma-js";
import { mustDrawEdge, StudioConverter } from "./converter";

export interface StudioState {
    activeQueryDatabase: string | null;
}

export class GraphVisualiser {
    graph: MultiGraph;
    sigma: Sigma;
    layout: LayoutWrapper;
    interactionHandler: InteractionHandler;
    state: StudioState;
    private styleParameters: StudioConverterStyleParameters = studioDefaultSettings.defaultQueryStyleParameters;
    private structureParameters: StudioConverterStructureParameters = studioDefaultSettings.defaultStructureParameters;

    constructor(graph: MultiGraph, sigma: Sigma, layout: LayoutWrapper) {
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
        if (term != "") {
            this.graph.nodes().forEach(node => {
                let attributes = this.graph.getNodeAttributes(node);
                // check concept.type.label if you want to match types of things.
                let any_match = -1 != safe_str(attributes["metadata"].concept.iid).indexOf(term)
                    || -1 != safe_str(attributes["metadata"].concept.label).indexOf(term)
                    || -1 != safe_str(attributes["metadata"].concept.value).indexOf(term);
                if (any_match) {
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

    private getColorForEdge(graph: MultiGraph, edgeKey: string): chroma.Color {
        let attributes = graph.getEdgeAttributes(edgeKey);
        let constraintIndex = attributes["metadata"].structureEdgeCoordinates.constraintIndex;
        return this.getColorForConstraintIndex(constraintIndex);
    }

    colorQuery(queryString: string, queryStructure: QueryStructure): string {
        let spans: Array<Array<number>> = [];
        queryStructure.branches.forEach(branch => {
            branch.edges.forEach((edge, constraintIndex) => {
                if (mustDrawEdge(edge, studioDefaultSettings.defaultStructureParameters)) {
                    if (edge.span != null) {
                        spans.push([edge.span.begin, edge.span.end, constraintIndex]);
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
        console.log(highlighted);
        return highlighted;
    }

    private getColorForConstraintIndex(constraintIndex: number): chroma.Color {
        let r = ((constraintIndex+1) * 153 % 256);
        let g = ((constraintIndex+1) * 173 % 256);
        let b = ((constraintIndex+1) * 199 % 256);
        return chroma([r,g,b]);
    }

    clear() {
        this.sigma = this.sigma.clear();
    }
}
