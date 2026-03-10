import {
    ApiResponse, ConceptRowsQueryResponse,
    isApiErrorResponse,
    QueryResponse,
} from "@typedb/driver-http";
import Sigma from "sigma";
import type { GraphStyleService } from "../../service/graph-style.service";

import { getTypeLabel } from "./types";
import { buildLogicalGraph, AnalyzedPipelineBackCompat } from "./logical-graph-builder";
import { VisualGraph, StudioConverterStructureParameters, defaultStructureParameters } from "./visual-graph";
import { buildVisualGraph, VisualGraphBuilder } from "./visual-graph-builder";
import { GraphStyles, colorEdgesByConstraintIndex as _colorEdgesByConstraintIndex, colorQuery as _colorQuery } from "./styles";
import { setUseBorderColorForLabels } from "./sigma-label-utils";
import { InteractionHandler, StudioState } from "./interaction-handler";
import { LayoutWrapper } from "./layout";

// Re-export public API
export type { StudioState } from "./interaction-handler";
export type { VisualGraph } from "./visual-graph";
export { newVisualGraph, defaultStructureParameters } from "./visual-graph";
export type { StudioConverterStructureParameters } from "./visual-graph";
export { buildVisualGraph, VisualGraphBuilder, shouldCreateEdge, shouldCreateNode, vertexMapKey } from "./visual-graph-builder";
export type { DataVertexKind, DataVertex, QueryCoordinates } from "./types";
export { getTypeLabel } from "./types";
export { buildLogicalGraph } from "./logical-graph-builder";
export { backCompat_pipelineBlocks, backCompat_expressionAssigned } from "./logical-graph-builder";
export type { ConstraintBackCompat, ConceptRowsQueryResponseBackCompat, AnalyzedPipelineBackCompat } from "./logical-graph-builder";
export { GraphStyles, defaultQueryStyleParams, defaultExplorationQueryStyleParams, defaultEdgeLabelColors, darkPalette, colorEdgesByConstraintIndex, colorQuery } from "./styles";
export { createSigmaRenderer, defaultSigmaSettings } from "./sigma-settings";
export { setUseBorderColorForLabels } from "./sigma-label-utils";
export { Layouts } from "./layout";
export type { LayoutWrapper } from "./layout";
export { InteractionHandler } from "./interaction-handler";

export class GraphVisualiser {
    interactionHandler: InteractionHandler;
    state: StudioState;
    private styleParams: GraphStyles;
    private structureParams: StudioConverterStructureParameters = defaultStructureParameters;

    constructor(public graph: VisualGraph, public sigma: Sigma, public layout: LayoutWrapper, public styleService: GraphStyleService) {
        this.state = { activeQueryDatabase: null };
        this.styleParams = this.syncStyles();
        this.interactionHandler = new InteractionHandler(graph, sigma, this.state, this.styleParams);
    }

    private syncStyles(): GraphStyles {
        setUseBorderColorForLabels(this.styleService.labelUseBorderColor);
        this.styleParams = this.styleService.toGraphStyles();
        if (this.interactionHandler) {
            this.interactionHandler.styleParams = this.styleParams;
        }
        return this.styleParams;
    }

    applyStyleUpdate(): void {
        this.syncStyles();
        this.graph.nodes().forEach(nodeKey => {
            const attrs = this.graph.getNodeAttributes(nodeKey);
            const concept = attrs.metadata.concept;
            const style = this.styleService.resolveNodeStyle(concept.kind, getTypeLabel(concept));
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
        this.syncStyles();
        if (!this.styleService.colorEdgesByConstraint) {
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
            let builder = new VisualGraphBuilder(this.graph, res.ok.query, false, this.structureParams, this.styleParams);
            let logicalGraph = buildLogicalGraph(res.ok);
            buildVisualGraph(logicalGraph, builder);
        }
    }

    handleExplorationQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType == "conceptRows" && res.ok.query != null) {
            let builder = new VisualGraphBuilder(this.graph, res.ok.query, true, this.structureParams, this.styleParams);
            let logicalGraph = buildLogicalGraph(res.ok as ConceptRowsQueryResponse);
            buildVisualGraph(logicalGraph, builder);
        }
    }

    searchGraph(term: string) {
        this.interactionHandler.searchGraph(term);
    }

    colorEdgesByConstraintIndex(reset: boolean): void {
        _colorEdgesByConstraintIndex(this.graph, this.interactionHandler.styleParams, reset);
    }

    colorQuery(queryString: string, queryStructure: AnalyzedPipelineBackCompat): string {
        return _colorQuery(queryString, queryStructure);
    }

    destroy() {
        this.sigma.kill();
    }
}
