import {
    ApiResponse, ConceptRowsQueryResponse,
    isApiErrorResponse,
    QueryResponse,
} from "@typedb/driver-http";
import Sigma from "sigma";
import type { GraphStyleService } from "../../service/graph-style.service";

// Domain imports
import { constructGraphFromRowsResult } from "./data/builder";
import { AnalyzedPipelineBackCompat } from "./data/back-compat";
import { VisualGraph, StudioConverterStructureParameters, defaultStructureParameters } from "./visual/types";
import { convertLogicalGraphWith, StudioConverter } from "./visual/converter";
import { StudioConverterStyleParameters, defaultQueryStyleParameters } from "./style/parameters";
import { colorEdgesByConstraintIndex as _colorEdgesByConstraintIndex, colorQuery as _colorQuery } from "./style/constraint-colors";
import { setUseBorderColorForLabels } from "./rendering/label-utils";
import { InteractionHandler, StudioState } from "./interaction";
import { LayoutWrapper } from "./layout";

// Re-export public API from domain modules
export type { StudioState } from "./interaction";
export type { VisualGraph } from "./visual/types";
export { newVisualGraph, defaultStructureParameters } from "./visual/types";
export type { StudioConverterStructureParameters } from "./visual/types";
export { convertLogicalGraphWith, StudioConverter, shouldCreateEdge, shouldCreateNode, vertexMapKey } from "./visual/converter";
export type { DataVertexKind, DataVertex, QueryCoordinates } from "./data/types";
export { constructGraphFromRowsResult } from "./data/builder";
export { backCompat_pipelineBlocks, backCompat_expressionAssigned } from "./data/back-compat";
export type { ConstraintBackCompat, ConceptRowsQueryResponseBackCompat, AnalyzedPipelineBackCompat } from "./data/back-compat";
export { StudioConverterStyleParameters, defaultQueryStyleParameters, defaultExplorationQueryStyleParameters, defaultEdgeLabelColors, darkPalette } from "./style/parameters";
export { colorEdgesByConstraintIndex, colorQuery } from "./style/constraint-colors";
export { createSigmaRenderer, defaultSigmaSettings } from "./rendering/sigma-settings";
export { setUseBorderColorForLabels } from "./rendering/label-utils";
export { Layouts } from "./layout";
export type { LayoutWrapper } from "./layout";
export { InteractionHandler } from "./interaction";

export class GraphVisualiser {
    graph: VisualGraph;
    sigma: Sigma;
    layout: LayoutWrapper;
    interactionHandler: InteractionHandler;
    state: StudioState;
    styleService: GraphStyleService | null = null;
    private styleParameters: StudioConverterStyleParameters = defaultQueryStyleParameters;
    private structureParameters: StudioConverterStructureParameters = defaultStructureParameters;

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
        setUseBorderColorForLabels(service.labelUseBorderColor);
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
        if (this.interactionHandler) {
            this.interactionHandler.styleParameters = this.styleParameters;
        }
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
            let logicalGraph = constructGraphFromRowsResult(res.ok);
            convertLogicalGraphWith(logicalGraph, converter);
        }
    }

    handleExplorationQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType == "conceptRows" && res.ok.query != null) {
            let converter = new StudioConverter(this.graph, res.ok.query, true, this.structureParameters, this.styleParameters);
            let logicalGraph = constructGraphFromRowsResult(res.ok as ConceptRowsQueryResponse);
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
        _colorEdgesByConstraintIndex(this.graph, this.interactionHandler.styleParameters, reset);
    }

    colorQuery(queryString: string, queryStructure: AnalyzedPipelineBackCompat): string {
        return _colorQuery(queryString, queryStructure);
    }

    destroy() {
        this.sigma.kill();
    }
}
