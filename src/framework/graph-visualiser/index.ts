import {
    ApiResponse, ConceptRowsQueryResponse,
    isApiErrorResponse,
    QueryResponse,
} from "@typedb/driver-http";
import chroma from "chroma-js";
import Sigma from "sigma";
import type { GraphStyleService } from "../../service/graph-style.service";

import { getTypeLabel, DataVertex } from "@typedb/graph-utils";
import { buildStructuredAnswers, AnalyzedPipelineBackCompat } from "@typedb/graph-utils";
import { Graph, GraphBuilderStructureParams, defaultStructureParams } from "./graph";
import { GraphBuilder } from "./graph-builder";
import { GraphStyles, colorEdgesByConstraintIndex as _colorEdgesByConstraintIndex, colorQuery as _colorQuery } from "./styles";
import { setUseBorderColorForLabels, setLabelsVisible } from "./sigma-label-utils";
import { InteractionHandler, StudioState } from "./interaction-handler";
import { LayoutWrapper } from "./layout";

export class GraphVisualiser {
    interactionHandler: InteractionHandler;
    state: StudioState;
    private styleParams: GraphStyles;
    private structureParams: GraphBuilderStructureParams = defaultStructureParams;

    private autoZoomEnabled = true;
    private settingCameraProgrammatically = false;
    private peakCameraRatio = 0;

    constructor(public graph: Graph, public sigma: Sigma, public layout: LayoutWrapper, public styleService: GraphStyleService) {
        this.state = { activeQueryDatabase: null };
        this.styleParams = this.syncStyles();
        this.interactionHandler = new InteractionHandler(graph, sigma, this.state, this.styleParams, this.styleService);
        this.interactionHandler.layout = this.layout;
        this.setupReducers();
        this.layout.onTick = () => {
            if (this.autoZoomEnabled) this.centerCamera();
        };
        this.sigma.getCamera().addListener("updated", () => {
            if (!this.settingCameraProgrammatically) {
                this.autoZoomEnabled = false;
            }
        });
    }

    private syncStyles(): GraphStyles {
        setUseBorderColorForLabels(this.styleService.labelUseBorderColor);
        setLabelsVisible(this.styleService.labelsVisible);
        this.styleParams = this.styleService.toGraphStyles();
        if (this.interactionHandler) {
            this.interactionHandler.styleParams = this.styleParams;
        }
        return this.styleParams;
    }

    private setupReducers(): void {
        const FADE_RATIO = 0.075; // mix 7.5% original color, 92.5% black

        const fade = (color: string) => chroma.mix("#000000", color, FADE_RATIO).hex();

        this.sigma.setSetting("nodeReducer", (node, data) => {
            const state = this.interactionHandler.state;
            let shouldFade = false;

            // Selection-based fading
            if (state.selectedNode != null && node !== state.selectedNode && !state.selectedNeighbors?.has(node)) {
                shouldFade = true;
            }

            // Highlight-based fading
            try {
                if (this.styleService.isHighlightActive()) {
                    const attrs = this.graph.getNodeAttributes(node);
                    const concept = attrs.metadata.concept;
                    if (!this.styleService.shouldHighlightNode(concept.kind, getTypeLabel(concept))) {
                        shouldFade = true;
                    }
                }
            } catch (_) { /* guard against missing metadata during graph mutations */ }

            if (!shouldFade) return data;
            const res = { ...data };
            res["color"] = fade(data["color"]);
            if (data["borderColor"]) res["borderColor"] = fade(data["borderColor"]);
            return res;
        });

        this.sigma.setSetting("edgeReducer", (edge, data) => {
            const state = this.interactionHandler.state;
            let shouldFade = false;

            // Selection-based fading
            if (state.selectedNode != null) {
                const source = this.graph.source(edge);
                const target = this.graph.target(edge);
                if (!((source === state.selectedNode && state.selectedNeighbors?.has(target))
                    || (target === state.selectedNode && state.selectedNeighbors?.has(source)))) {
                    shouldFade = true;
                }
            }

            // Highlight-based fading
            try {
                if (this.styleService.isHighlightActive()) {
                    const tag = this.graph.getEdgeAttributes(edge).metadata?.dataEdge?.tag;
                    if (tag && !this.styleService.shouldHighlightEdge(tag)) {
                        shouldFade = true;
                    }
                }
            } catch (_) { /* guard against missing metadata during graph mutations */ }

            if (!shouldFade) return data;
            const res = { ...data };
            res["color"] = fade(data["color"] ?? "#ccc");
            res["label"] = "";
            return res;
        });
    }

    applyStyleUpdate(): void {
        this.syncStyles();
        const useDegreeScaling = this.styleService.degreeScaling;
        this.graph.nodes().forEach(nodeKey => {
            const attrs = this.graph.getNodeAttributes(nodeKey);
            const concept = attrs.metadata.concept;
            const style = this.styleService.resolveNodeStyle(concept.kind, getTypeLabel(concept));
            this.graph.setNodeAttribute(nodeKey, "color", style.color);
            this.graph.setNodeAttribute(nodeKey, "borderColor", style.borderColor);
            this.graph.setNodeAttribute(nodeKey, "type", style.shape);
            if (useDegreeScaling) {
                const degree = this.graph.degree(nodeKey);
                const sz = 6 + Math.min(degree * 4, 54);
                this.graph.setNodeAttribute(nodeKey, "width", sz);
                this.graph.setNodeAttribute(nodeKey, "height", sz);
                this.graph.setNodeAttribute(nodeKey, "size", sz);
            } else {
                this.graph.setNodeAttribute(nodeKey, "width", style.width);
                this.graph.setNodeAttribute(nodeKey, "height", style.height);
                this.graph.setNodeAttribute(nodeKey, "size", Math.max(style.width, style.height));
            }
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
        this.autoZoomEnabled = true;
        this.peakCameraRatio = 0;
        this.graph.nodes().forEach(node => {
            this.graph.setNodeAttribute(node, "x", Math.random());
            this.graph.setNodeAttribute(node, "y", Math.random());
        });
        this.layout.startOrRedraw();
        this.centerCamera();
    }

    centerCamera(zoomOutOnly = false): void {
        const nodes = this.graph.nodes();
        if (nodes.length === 0) return;

        const { width, height } = this.sigma.getDimensions();
        if (width === 0 || height === 0) return;

        // Compute graph bounding box in graph coordinates
        let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
        nodes.forEach(node => {
            const attrs = this.graph.getNodeAttributes(node);
            minX = Math.min(minX, attrs.x);
            maxX = Math.max(maxX, attrs.x);
            minY = Math.min(minY, attrs.y);
            maxY = Math.max(maxY, attrs.y);
        });

        // With autoRescale off, sigma maps 1 graph unit ≈ 1 pixel (centered on graph center).
        // Camera ratio = how much of the viewport-sized region to show.
        // ratio=1 shows a viewport-sized window. We need ratio = graphExtent / viewportSize.
        const graphWidth = maxX - minX || 1;
        const graphHeight = maxY - minY || 1;
        const padding = 1.1;
        const rawRatio = Math.max(graphWidth / width, graphHeight / height, 1) * padding;
        // Cap ratio so nodes (rendered at fixed screen-pixel sizes) remain visible
        const ratio = Math.min(rawRatio, 20);

        // During simulation, only zoom out (grow ratio), never zoom back in
        if (zoomOutOnly && ratio <= this.peakCameraRatio) {
            this.settingCameraProgrammatically = true;
            this.sigma.getCamera().setState({ x: 0.5, y: 0.5, ratio: this.peakCameraRatio, angle: 0 });
            this.settingCameraProgrammatically = false;
            return;
        }
        this.peakCameraRatio = ratio;

        this.settingCameraProgrammatically = true;
        this.sigma.getCamera().setState({ x: 0.5, y: 0.5, ratio, angle: 0 });
        this.settingCameraProgrammatically = false;
    }

    handleQueryResponse(res: ApiResponse<QueryResponse>, database: string) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType === "conceptRows") {
            this.state.activeQueryDatabase = database;
            this.autoZoomEnabled = true;
            this.peakCameraRatio = 0;
            this.handleQueryResult(res);
            this.layout.startOrRedraw();
            this.centerCamera();
        }
    }

    handleQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) return;
        if (res.ok.answerType == "conceptRows" && res.ok.query != null) {
            (window as any)._lastQueryAnswers = res.ok.answers; // TODO: Remove once schema based autocomplete is stable.
            let builder = new GraphBuilder(this.graph, res.ok.query, false, this.structureParams, this.styleParams);
            let answers = buildStructuredAnswers(res.ok);
            builder.build(answers);
        }
    }

    handleExplorationQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType == "conceptRows" && res.ok.query != null) {
            let builder = new GraphBuilder(this.graph, res.ok.query, true, this.structureParams, this.styleParams);
            let answers = buildStructuredAnswers(res.ok as ConceptRowsQueryResponse);
            builder.build(answers);
        }
    }

    searchGraph(term: string) {
        this.interactionHandler.searchGraph(term);
    }

    applyStructureMode(): void {
        this.syncStyles();
        this.graph.nodes().forEach(nodeKey => {
            const attrs = this.graph.getNodeAttributes(nodeKey);
            const concept = attrs.metadata.concept;
            const style = this.styleService.resolveNodeStyle(concept.kind, getTypeLabel(concept));
            const degree = this.graph.degree(nodeKey);
            const sz = 6 + Math.min(degree * 4, 54);
            this.graph.setNodeAttribute(nodeKey, "type", style.shape);
            this.graph.setNodeAttribute(nodeKey, "color", style.color);
            this.graph.setNodeAttribute(nodeKey, "borderColor", style.borderColor);
            this.graph.setNodeAttribute(nodeKey, "width", sz);
            this.graph.setNodeAttribute(nodeKey, "height", sz);
            this.graph.setNodeAttribute(nodeKey, "size", sz);
        });
        this.sigma.refresh();
    }

    restoreLabels(): void {
        this.syncStyles();
        this.graph.nodes().forEach(nodeKey => {
            const attrs = this.graph.getNodeAttributes(nodeKey);
            const concept = attrs.metadata.concept as DataVertex;
            const style = this.styleService.resolveNodeStyle(concept.kind, getTypeLabel(concept));
            this.graph.setNodeAttribute(nodeKey, "label", this.styleParams.vertexDefaultLabel(concept));
            this.graph.setNodeAttribute(nodeKey, "type", style.shape);
            this.graph.setNodeAttribute(nodeKey, "color", style.color);
            this.graph.setNodeAttribute(nodeKey, "borderColor", style.borderColor);
            this.graph.setNodeAttribute(nodeKey, "width", style.width);
            this.graph.setNodeAttribute(nodeKey, "height", style.height);
            this.graph.setNodeAttribute(nodeKey, "size", Math.max(style.width, style.height));
        });
        this.graph.edges().forEach(edgeKey => {
            const metadata = this.graph.getEdgeAttributes(edgeKey).metadata;
            this.graph.setEdgeAttribute(edgeKey, "label", metadata?.dataEdge?.tag ?? "");
        });
        this.sigma.refresh();
    }

    colorEdgesByConstraintIndex(reset: boolean): void {
        _colorEdgesByConstraintIndex(this.graph, this.interactionHandler.styleParams, reset);
    }

    colorQuery(queryString: string, queryStructure: AnalyzedPipelineBackCompat): string {
        return _colorQuery(queryString, queryStructure);
    }

    get isLayoutRunning(): boolean {
        return this.layout.isRunning;
    }

    stopLayout(): void {
        this.layout.stop();
    }

    destroy() {
        this.sigma.kill();
    }
}
