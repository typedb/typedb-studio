import { ApiResponse, isApiErrorResponse, QueryConstraintAny, QueryConstraintSpan, QueryResponse, QueryStructure } from "typedb-driver-http";
import MultiGraph from "graphology";
import Sigma from "sigma";
import ForceSupervisor from "graphology-layout-force/worker";
import { Settings as SigmaSettings } from "sigma/settings";
import { StudioConverterStructureParameters, StudioConverterStyleParameters } from "./config";

import * as studioDefaultSettings from "./defaults";
import {constructGraphFromRowsResult, QueryCoordinates, VisualGraph} from "./graph";
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
        if (res.ok.answerType == "conceptRows" && res.ok.query != null) {
            (window as any)._lastQueryAnswers = res.ok.answers; // TODO: Remove once schema based autocomplete is stable.
            let converter = new StudioConverter(this.graph, res.ok.query, false, this.structureParameters, this.styleParameters);
            let logicalGraph = constructGraphFromRowsResult(res.ok); // In memory, not visualised
            convertLogicalGraphWith(logicalGraph, converter);
        }
    }

    handleExplorationQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType == "conceptRows" && res.ok.query != null) {
            let converter = new StudioConverter(this.graph, res.ok.query, true, this.structureParameters, this.styleParameters);
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
                if ("concept" in attributes.metadata) {
                    const concept = attributes.metadata.concept;
                    if (("iid" in concept && safe_str(concept.iid).indexOf(term) !== -1)
                        || ("label" in concept && safe_str(concept.label).indexOf(term) !== -1)
                        || ("value" in concept && safe_str(concept.value).indexOf(term) !== -1)) {
                        this.graph.setNodeAttribute(node, "highlighted", true);
                    }
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
        let branchIndex = attributes.metadata.dataEdge.queryCoordinates.branch;
        return this.getColorForConstraintIndex(branchIndex, constraintIndex);
    }

    colorQuery(queryString: string, queryStructure: QueryStructure): string {
        function shouldColourConstraint(constraint: QueryConstraintAny): boolean {
            switch (constraint.tag) {
                case "isa": return shouldCreateEdge(queryStructure, constraint, constraint.instance, constraint.type);
                case "isa!": return shouldCreateEdge(queryStructure, constraint, constraint.instance, constraint.type);
                case "has":  return shouldCreateEdge(queryStructure, constraint, constraint.owner, constraint.attribute);
                case "links":
                    return shouldCreateEdge(queryStructure, constraint, constraint.relation, constraint.player);
                case "sub":
                    return shouldCreateEdge(queryStructure, constraint, constraint.subtype, constraint.supertype);
                case "sub!":
                    return shouldCreateEdge(queryStructure, constraint, constraint.subtype, constraint.supertype);
                case "owns":
                    return shouldCreateEdge(queryStructure, constraint, constraint.owner, constraint.attribute);
                case "relates":
                    return shouldCreateEdge(queryStructure, constraint, constraint.relation, constraint.role);
                case "plays":
                    return shouldCreateEdge(queryStructure, constraint, constraint.player, constraint.role);
                case "expression":
                    return (
                        constraint.arguments.map(arg => shouldCreateNode(queryStructure, arg)).reduce((a,b) => a || b, false)
                        || constraint.assigned.map(assigned => shouldCreateNode(queryStructure, assigned)).reduce((a,b) => a || b, false)
                    );
                case "functionCall":
                    return (
                        constraint.arguments.map(arg => shouldCreateNode(queryStructure, arg)).reduce((a,b) => a || b, false)
                        || constraint.assigned.map(assigned => shouldCreateNode(queryStructure, assigned)).reduce((a,b) => a || b, false)
                    );
                case "comparison": return false;
                case "is": return false;
                case "iid": return false;
                case "kind": return false;
                case "label": return false;
                case "value": return false;
            }
        }
        let spans: { span: QueryConstraintSpan, coordinates: QueryCoordinates}[] = [];
        queryStructure.blocks.forEach((branch, branchIndex) => {
            branch.constraints.forEach((constraint, constraintIndex) => {
                if (shouldColourConstraint(constraint)) {
                    if (constraint.textSpan != null) {
                        spans.push({span: constraint.textSpan, coordinates: { branch: branchIndex, constraint: constraintIndex}});
                    }
                }
            })
        });
        // Add one to end-offset so we're AFTER the last character
        let starts_ends_separate = spans.flatMap(span => [
            { offset: span.span.begin, coordinatesIfStartElseNull: span.coordinates },
            { offset: span.span.end + 1, coordinatesIfStartElseNull: null }
    ]);
        starts_ends_separate.sort((a,b) => a.offset - b.offset);
        let se_index = 0;
        let highlighted = "";
        for(let i= 0; i<queryString.length; i++) {
            while (se_index < starts_ends_separate.length && starts_ends_separate[se_index].offset == i) {
                let coordinatesOrNullIfEnd = starts_ends_separate[se_index].coordinatesIfStartElseNull;
                if (coordinatesOrNullIfEnd == null) {
                    highlighted += "</span>"
                } else {
                    let color = this.getColorForConstraintIndex(coordinatesOrNullIfEnd.branch, coordinatesOrNullIfEnd.constraint)
                    highlighted += "<span style=\"color: " + color.hex() + "\">";
                }
                se_index += 1;
            }
            highlighted += (queryString[i] == "\n") ? "<br/>": queryString[i];
        }
        return highlighted;
    }

    private getColorForConstraintIndex(branchIndex: number, constraintIndex: number): chroma.Color {
        const OFFSET1 = 153;
        const OFFSET2 = 173;
        const OFFSET3 = 199;
        let r = ((branchIndex + 1) * OFFSET3 + (constraintIndex+1) * OFFSET1) % 256;
        let g = ((branchIndex + 1) * OFFSET2 + (constraintIndex+1) * OFFSET2) % 256;
        let b = ((branchIndex + 1) * OFFSET1 + (constraintIndex+1) * OFFSET3) % 256;
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
