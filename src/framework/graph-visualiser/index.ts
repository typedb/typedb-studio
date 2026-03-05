import {
    ApiResponse, ConceptRowsQueryResponse, AnalyzedConjunction, AnalyzedPipeline,
    isApiErrorResponse,
    QueryResponse,
    ConstraintAny, ConstraintExpression, ConstraintSpan, ConstraintVertexVariable,
    ConstraintExpressionLegacy, ConstraintLinksLegacy,
    QueryStructureLegacy, QueryConjunctionLegacy, ConceptRowsQueryResponseLegacy
} from "@typedb/driver-http";
import MultiGraph from "graphology";
import Sigma from "sigma";
import { Settings as SigmaSettings } from "sigma/settings";
import { StudioConverterStructureParameters, StudioConverterStyleParameters } from "./config";

import * as studioDefaultSettings from "./defaults";
import {constructGraphFromRowsResult, QueryCoordinates, VisualGraph} from "./graph";
import {InteractionHandler} from "./interaction";
import { convertLogicalGraphWith } from "./visualisation";
import {LayoutWrapper} from "./layouts";
import chroma from "chroma-js";
import {shouldCreateEdge, shouldCreateNode, StudioConverter} from "./converter";
import type { GraphStyleService } from "../../service/graph-style.service";

export interface StudioState {
    activeQueryDatabase: string | null;
}

export class GraphVisualiser {
    graph: VisualGraph;
    sigma: Sigma;
    layout: LayoutWrapper;
    interactionHandler: InteractionHandler;
    state: StudioState;
    styleService: GraphStyleService | null = null;
    private styleParameters: StudioConverterStyleParameters = studioDefaultSettings.defaultQueryStyleParameters;
    private structureParameters: StudioConverterStructureParameters = studioDefaultSettings.defaultStructureParameters;

    constructor(graph: VisualGraph, sigma: Sigma, layout: LayoutWrapper, styleService?: GraphStyleService) {
        this.graph = graph;
        this.sigma = sigma;
        this.layout = layout;
        this.state = { activeQueryDatabase: null };
        this.styleService = styleService ?? null;
        if (styleService) {
            this.applyServiceStyles(styleService);
        }
        this.interactionHandler = new InteractionHandler(graph, sigma, this.state, this.styleParameters);
    }

    private applyServiceStyles(service: GraphStyleService): void {
        const kindColors: Record<string, string> = {} as any;
        const kindBorderColors: Record<string, string> = {} as any;
        const kindShapes: Record<string, string> = {} as any;
        const kindWidths: Record<string, number> = {} as any;
        const kindHeights: Record<string, number> = {} as any;
        const typeColors: Record<string, string> = {};
        const typeBorderColors: Record<string, string> = {};
        const typeShapes: Record<string, string> = {};
        const typeWidths: Record<string, number> = {};
        const typeHeights: Record<string, number> = {};

        const kinds = ["entity", "relation", "attribute", "entityType", "relationType",
            "attributeType", "roleType", "value", "unavailable", "expression", "functionCall"] as const;
        for (const kind of kinds) {
            const style = service.getKindStyle(kind);
            kindColors[kind] = style.color;
            kindBorderColors[kind] = style.borderColor;
            kindShapes[kind] = style.shape;
            kindWidths[kind] = style.width;
            kindHeights[kind] = style.height;
        }

        for (const [typeLabel, override] of Object.entries(service.typeStyles)) {
            if (override.color) typeColors[typeLabel] = override.color;
            if (override.borderColor) typeBorderColors[typeLabel] = override.borderColor;
            if (override.shape) typeShapes[typeLabel] = override.shape;
            if (override.width) typeWidths[typeLabel] = override.width;
            if (override.height) typeHeights[typeLabel] = override.height;
        }

        this.styleParameters = {
            ...this.styleParameters,
            vertex_widths: kindWidths as any,
            vertex_heights: kindHeights as any,
            vertex_colors: kindColors as any,
            vertex_border_colors: kindBorderColors as any,
            vertex_shapes: kindShapes as any,
            vertex_type_colors: Object.keys(typeColors).length ? typeColors : undefined,
            vertex_type_border_colors: Object.keys(typeBorderColors).length ? typeBorderColors : undefined,
            vertex_type_shapes: Object.keys(typeShapes).length ? typeShapes : undefined,
            vertex_type_widths: Object.keys(typeWidths).length ? typeWidths : undefined,
            vertex_type_heights: Object.keys(typeHeights).length ? typeHeights : undefined,
            edge_label_colors: service.getResolvedEdgeLabelColors(),
        };
    }

    applyStyleUpdate(): void {
        if (this.styleService) {
            this.applyServiceStyles(this.styleService);
        }
        this.graph.nodes().forEach(nodeKey => {
            const attrs = this.graph.getNodeAttributes(nodeKey);
            const concept = attrs.metadata.concept;
            const kind = concept.kind;
            let typeLabel: string | undefined;
            if ("type" in concept && concept.type && "label" in concept.type) {
                typeLabel = concept.type.label;
            } else if ("label" in concept && kind !== "unavailable") {
                typeLabel = concept.label;
            }

            const style = this.styleService
                ? this.styleService.getEffectiveStyle(kind, typeLabel)
                : {
                    color: this.styleParameters.vertex_colors[kind],
                    borderColor: this.styleParameters.vertex_border_colors[kind],
                    shape: this.styleParameters.vertex_shapes[kind],
                    width: this.styleParameters.vertex_widths[kind],
                    height: this.styleParameters.vertex_heights[kind],
                };
            this.graph.setNodeAttribute(nodeKey, "color", style.color);
            this.graph.setNodeAttribute(nodeKey, "borderColor", style.borderColor);
            this.graph.setNodeAttribute(nodeKey, "type", style.shape);
            this.graph.setNodeAttribute(nodeKey, "width", style.width);
            this.graph.setNodeAttribute(nodeKey, "height", style.height);
            this.graph.setNodeAttribute(nodeKey, "size", Math.min(style.width, style.height));
        });
        this.sigma.refresh();
    }

    applyEdgeStyleUpdate(): void {
        if (this.styleService) {
            this.applyServiceStyles(this.styleService);
        }
        if (!this.styleService?.colorEdgesByConstraint) {
            this.colorEdgesByConstraintIndex(true);
        }
        this.sigma.refresh();
    }

    reLayout(): void {
        this.graph.nodes().forEach(node => {
            this.graph.setNodeAttribute(node, "x", Math.random());
            this.graph.setNodeAttribute(node, "y", Math.random());
        });
        this.layout.startOrRedraw();
        this.centerCamera();
    }

    centerCamera(): void {
        const nodes = this.graph.nodes();
        if (nodes.length === 0) return;
        // With autoRescale off, sigma normalizes the extent to [0,1].
        // The graph center always maps to (0.5, 0.5) in normalized space.
        this.sigma.getCamera().setState({
            x: 0.5,
            y: 0.5,
            ratio: 1,
            angle: 0,
        });
    }

    handleQueryResponse(res: ApiResponse<QueryResponse>, database: string) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType === "conceptRows") {
            this.state.activeQueryDatabase = database;
            this.handleQueryResult(res);
            this.layout.startOrRedraw();
            this.centerCamera();
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
            let logicalGraph = constructGraphFromRowsResult(res.ok as ConceptRowsQueryResponse); // In memory, not visualised
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
        const params = this.interactionHandler.styleParameters;
        this.graph.edges().forEach(edgeKey => {
            if (reset) {
                const tag = this.graph.getEdgeAttributes(edgeKey).metadata.dataEdge.tag;
                const color = params.edge_label_colors?.[tag] ?? params.edge_color.hex();
                this.graph.setEdgeAttribute(edgeKey, "color", color);
            } else {
                const color = this.getColorForEdge(this.graph, edgeKey);
                this.graph.setEdgeAttribute(edgeKey, "color", color.hex());
            }
        });
    }

    private getColorForEdge(graph: VisualGraph, edgeKey: string): chroma.Color {
        let attributes = graph.getEdgeAttributes(edgeKey);
        let constraintIndex = attributes.metadata.dataEdge.queryCoordinates.constraint;
        let branchIndex = attributes.metadata.dataEdge.queryCoordinates.branch;
        return this.getColorForConstraintIndex(branchIndex, constraintIndex);
    }

    colorQuery(queryString: string, queryStructure: AnalyzedPipelineBackCompat): string {
        function shouldColourConstraint(constraint: ConstraintAny | ConstraintExpressionLegacy | ConstraintLinksLegacy): boolean {
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
                        || shouldCreateNode(queryStructure, backCompat_expressionAssigned(constraint))
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
                case "or":  return false;
                case "not": return false;
                case "try": return false;
            }
        }
        let spans: { span: ConstraintSpan, coordinates: QueryCoordinates}[] = [];

        backCompat_pipelineBlocks(queryStructure).forEach((branch, branchIndex) => {
            branch.constraints.forEach((constraint, constraintIndex) => {
                if (shouldColourConstraint(constraint)) {
                    let span = "textSpan" in constraint ? constraint["textSpan"] : null;
                    if (span != null) {
                        spans.push({span, coordinates: { branch: branchIndex, constraint: constraintIndex}});
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
    const renderer = new Sigma(graph, containerEl, sigma_settings);
    // Disable hover rendering (node re-draw on hover WebGL layer)
    (renderer as any).renderHighlightedNodes = () => {};
    return renderer;
}

export function backCompat_pipelineBlocks(pipeline : AnalyzedPipelineBackCompat): AnalyzedConjunction[] | QueryConjunctionLegacy[] {
    if ("blocks" in pipeline) {
        return pipeline["blocks"];
    } else if ("conjunctions" in pipeline) {
        return pipeline["conjunctions"];
    } else {
        throw new Error("Unreachable: pipeline neither had blocks nor conjunctions");
    }
}

export function backCompat_expressionAssigned(expr: ConstraintExpression | ConstraintExpressionLegacy): ConstraintVertexVariable {
    return (Array.isArray(expr.assigned) ? expr.assigned[0] : expr.assigned) as ConstraintVertexVariable;
}

export type ConstraintBackCompat = ConstraintAny | ConstraintLinksLegacy | ConstraintExpressionLegacy;
export type ConceptRowsQueryResponseBackCompat = ConceptRowsQueryResponse | ConceptRowsQueryResponseLegacy;
export type AnalyzedPipelineBackCompat = AnalyzedPipeline | QueryStructureLegacy;
