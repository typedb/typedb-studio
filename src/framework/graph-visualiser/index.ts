import {
    ApiResponse,
    isApiErrorResponse,
    QueryResponse,
} from "@typedb/driver-http";
import chroma from "chroma-js";
import Sigma from "sigma";
import { Subscription } from "rxjs";
import { buildBackgroundCSS } from "../../service/graph-style.service";
import type { GraphStyleService } from "../../service/graph-style.service";

import { getTypeLabel, DataVertex } from "@typedb/graph-utils";
import { buildStructuredAnswers } from "@typedb/graph-utils";
import { AnalyzedPipelineBackCompat } from "./types";
import { Graph, GraphBuilderStructureParams, defaultStructureParams } from "./graph";
import { GraphBuilder } from "./graph-builder";
import { GraphStyles, colorEdgesByConstraintIndex as _colorEdgesByConstraintIndex, colorQuery as _colorQuery } from "./styles";
import { setUseBorderColorForLabels, setLabelsVisible, setShowHoverLabel } from "./sigma-label-utils";
import { InteractionHandler, StudioState } from "./interaction-handler";
import { LayoutWrapper } from "./layout";

export class GraphVisualiser {
    interactionHandler: InteractionHandler;
    state: StudioState;
    searchTerm = "";
    searchMatches: Set<string> | null = null;
    private styleParams: GraphStyles;
    private structureParams: GraphBuilderStructureParams = defaultStructureParams;

    private autoZoomEnabled = true;
    private settingCameraProgrammatically = false;
    private peakCameraRatio = 0;
    private labelsAutoHidden = false;
    private stylesSub!: Subscription;

    constructor(public graph: Graph, public sigma: Sigma, public layout: LayoutWrapper, public styleService: GraphStyleService) {
        this.state = { activeQueryDatabase: null };
        this.styleParams = this.syncStyles();
        this.interactionHandler = new InteractionHandler(graph, sigma, this.state, this.styleParams, this.styleService);
        this.interactionHandler.visualiser = this;
        this.interactionHandler.layout = this.layout;
        this.setupReducers();
        this.layout.onTick = () => {
            if (this.autoZoomEnabled) this.centerCamera();
        };
        this.sigma.getCamera().addListener("updated", () => {
            if (!this.settingCameraProgrammatically) {
                this.autoZoomEnabled = false;
            }
            this.updateLabelVisibilityForZoom();
        });
        this.stylesSub = this.styleService.styles$.subscribe(() => {
            this.syncStyles();
            try {
                this.applyStyleUpdate();
                this.applyEdgeStyleUpdate();
            } catch (_) { /* sigma not renderable (e.g. hidden tab, lost WebGL context) */ }
        });
    }

    private syncStyles(): GraphStyles {
        setUseBorderColorForLabels(this.styleService.labelUseBorderColor);
        // User explicitly changed label visibility — reset auto-hide state
        this.labelsAutoHidden = false;
        setLabelsVisible(this.styleService.labelsVisible);
        setShowHoverLabel(this.styleService.showHoverLabel);
        this.sigma.setSetting("renderEdgeLabels", this.styleService.labelsVisible);
        this.styleParams = this.styleService.toGraphStyles();
        if (this.interactionHandler) {
            this.interactionHandler.styleParams = this.styleParams;
        }
        this.applyBackground();
        return this.styleParams;
    }

    /**
     * When the graph has many elements and the camera is zoomed out, hide
     * node and edge labels to avoid the rendering cost of measuring and
     * drawing thousands of text strings every frame.
     */
    private updateLabelVisibilityForZoom(): void {
        // Only auto-hide when the user hasn't explicitly turned labels off
        if (!this.styleService.labelsVisible) return;

        const elementCount = this.graph.order + this.graph.size; // nodes + edges
        const ratio = this.sigma.getCamera().ratio;

        // More elements → hide labels at a lower (closer) zoom level.
        // At 200 elements, hide when ratio > 5; at 1000 elements, hide when ratio > 1.
        const ELEMENT_THRESHOLD = 100;
        const zoomThreshold = Math.max(4, ELEMENT_THRESHOLD * 10 / elementCount);
        const shouldHide = elementCount > ELEMENT_THRESHOLD && ratio > zoomThreshold;

        if (shouldHide && !this.labelsAutoHidden) {
            this.labelsAutoHidden = true;
            setLabelsVisible(false);
            this.sigma.setSetting("renderEdgeLabels", false);
        } else if (!shouldHide && this.labelsAutoHidden) {
            this.labelsAutoHidden = false;
            setLabelsVisible(true);
            this.sigma.setSetting("renderEdgeLabels", true);
        }
    }

    private setupReducers(): void {
        const FADE_RATIO = 0.075; // mix 7.5% original color, 92.5% black

        const fade = (color: string) => chroma.mix("#000000", color, FADE_RATIO).hex();

        this.sigma.setSetting("nodeReducer", (node, data) => {
            const state = this.interactionHandler.state;
            let shouldFade = false;

            // Search takes priority over everything
            if (this.searchMatches != null) {
                shouldFade = !this.searchMatches.has(node);
            } else {
                // Selection-based fading
                const isSelectedOrNeighbor = state.selectedNode != null
                    && (node === state.selectedNode || (state.selectedNeighbors?.has(node) ?? false));
                if (state.selectedNode != null && !isSelectedOrNeighbor) {
                    shouldFade = true;
                }

                // Highlight-based fading (skipped for nodes in the active selection)
                if (!isSelectedOrNeighbor) {
                    try {
                        if (this.styleService.isHighlightActive()) {
                            const attrs = this.graph.getNodeAttributes(node);
                            const concept = attrs.metadata.concept;
                            if (!this.styleService.shouldHighlightNode(concept.kind as any, getTypeLabel(concept as any))) {
                                shouldFade = true;
                            }
                        }
                    } catch (_) { /* guard against missing metadata during graph mutations */ }
                }
            }

            if (!shouldFade) return { ...data, zIndex: 1 };
            const res = { ...data };
            res["color"] = fade(data["color"]);
            if (data["borderColor"]) res["borderColor"] = fade(data["borderColor"]);
            res["label"] = "";
            res["zIndex"] = 0;
            return res;
        });

        this.sigma.setSetting("edgeReducer", (edge, data) => {
            const state = this.interactionHandler.state;
            let shouldFade = false;

            // Search takes priority over everything
            if (this.searchMatches != null) {
                const source = this.graph.source(edge);
                const target = this.graph.target(edge);
                shouldFade = !this.searchMatches.has(source) || !this.searchMatches.has(target);
            } else {
                // Selection-based fading: keep edges where both endpoints are highlighted
                let edgeInSelection = false;
                if (state.selectedNode != null) {
                    const source = this.graph.source(edge);
                    const target = this.graph.target(edge);
                    const isNodeHighlighted = (n: string) => n === state.selectedNode || (state.selectedNeighbors?.has(n) ?? false);
                    edgeInSelection = isNodeHighlighted(source) && isNodeHighlighted(target);
                    if (!edgeInSelection) {
                        shouldFade = true;
                    }
                }

                // Highlight-based fading (skipped for edges in the active selection)
                if (!edgeInSelection) {
                    try {
                        if (this.styleService.isHighlightActive()) {
                            const tag = this.graph.getEdgeAttributes(edge).metadata?.dataEdge?.tag;
                            if (tag && !this.styleService.shouldHighlightEdge(tag)) {
                                shouldFade = true;
                            }
                        }
                    } catch (_) { /* guard against missing metadata during graph mutations */ }
                }
            }

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
            const style = this.styleService.resolveNodeStyle(concept.kind as any, getTypeLabel(concept as any));
            this.graph.setNodeAttribute(nodeKey, "color", style.color);
            this.graph.setNodeAttribute(nodeKey, "borderColor", style.borderColor);
            this.graph.setNodeAttribute(nodeKey, "type", style.shape);
            if (useDegreeScaling) {
                const degree = this.graph.degree(nodeKey);
                const w = style.width + Math.min(degree * 2, style.width * 4);
                const h = style.height + Math.min(degree * 2, style.height * 4);
                this.graph.setNodeAttribute(nodeKey, "width", w);
                this.graph.setNodeAttribute(nodeKey, "height", h);
                this.graph.setNodeAttribute(nodeKey, "size", Math.max(w, h));
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
            if (this.styleService.degreeScaling) this.applyStyleUpdate();
            this.layout.startOrRedraw();
            this.centerCamera();
        }
    }

    handleQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) return;
        if (res.ok.answerType == "conceptRows" && res.ok.query != null) {
            (window as any)._lastQueryAnswers = res.ok.answers; // TODO: Remove once schema based autocomplete is stable.
            let builder = new GraphBuilder(this.graph, res.ok.query, false, this.structureParams, this.styleParams);
            let answers = buildStructuredAnswers(res.ok as any);
            builder.build(answers);
        }
    }

    handleExplorationQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) return;

        if (res.ok.answerType == "conceptRows" && res.ok.query != null) {
            let builder = new GraphBuilder(this.graph, res.ok.query, true, this.structureParams, this.styleParams);
            let answers = buildStructuredAnswers(res.ok as any);
            builder.build(answers);
            if (this.styleService.degreeScaling) this.applyStyleUpdate();
        }
    }

    searchGraph(term: string) {
        this.searchTerm = term;
        if (term === "") {
            this.searchMatches = null;
            this.sigma.refresh();
            return;
        }

        const safeString = (str: string | undefined): string =>
            str == undefined ? "" : str.toLowerCase();

        const matches = new Set<string>();
        this.graph.nodes().forEach(node => {
            const attributes = this.graph.getNodeAttributes(node);
            if ("concept" in attributes["metadata"]) {
                const concept = attributes["metadata"].concept;
                if (("iid" in concept && safeString(concept.iid).indexOf(term) !== -1)
                    || ("value" in concept && safeString(concept.value).indexOf(term) !== -1)
                    || ("type" in concept && safeString(concept.type.label).indexOf(term) !== -1)
                    || ("label" in concept && safeString(concept.label).indexOf(term) !== -1)) {
                    matches.add(node);
                }
            }
        });
        this.searchMatches = matches;
        this.sigma.refresh();
    }

    clearSearch() {
        if (this.searchMatches == null) return;
        this.searchGraph("");
    }

    focusSearchMatches(): void {
        const matches = this.searchMatches;
        if (!matches || matches.size === 0) return;

        const { width, height } = this.sigma.getDimensions();
        if (width === 0 || height === 0) return;

        // Compute bounding box of matched nodes in graph coordinates
        let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
        matches.forEach(node => {
            const attrs = this.graph.getNodeAttributes(node);
            minX = Math.min(minX, attrs.x);
            maxX = Math.max(maxX, attrs.x);
            minY = Math.min(minY, attrs.y);
            maxY = Math.max(maxY, attrs.y);
        });

        // Compute needed ratio to fit matched nodes, capped at 1 (100% zoom)
        const graphWidth = maxX - minX || 1;
        const graphHeight = maxY - minY || 1;
        const padding = 1.3;
        const rawRatio = Math.max(graphWidth / width, graphHeight / height) * padding;
        const ratio = Math.max(Math.min(rawRatio, 20), 1);

        // Convert graph-coordinate center to sigma's normalized camera coordinates
        const centerX = (minX + maxX) / 2;
        const centerY = (minY + maxY) / 2;
        const bbox = this.sigma.getCustomBBox() || this.sigma.getBBox();
        const x = (centerX - bbox.x[0]) / (bbox.x[1] - bbox.x[0]) || 0.5;
        const y = (centerY - bbox.y[0]) / (bbox.y[1] - bbox.y[0]) || 0.5;

        this.autoZoomEnabled = false;
        this.settingCameraProgrammatically = true;
        this.sigma.getCamera().setState({ x, y, ratio, angle: 0 });
        this.settingCameraProgrammatically = false;
    }

    applyStructureMode(): void {
        this.syncStyles();
        this.graph.nodes().forEach(nodeKey => {
            const attrs = this.graph.getNodeAttributes(nodeKey);
            const concept = attrs.metadata.concept;
            const style = this.styleService.resolveNodeStyle(concept.kind as any, getTypeLabel(concept as any));
            const degree = this.graph.degree(nodeKey);
            const w = style.width + Math.min(degree * 2, style.width * 4);
            const h = style.height + Math.min(degree * 2, style.height * 4);
            this.graph.setNodeAttribute(nodeKey, "type", style.shape);
            this.graph.setNodeAttribute(nodeKey, "color", style.color);
            this.graph.setNodeAttribute(nodeKey, "borderColor", style.borderColor);
            this.graph.setNodeAttribute(nodeKey, "width", w);
            this.graph.setNodeAttribute(nodeKey, "height", h);
            this.graph.setNodeAttribute(nodeKey, "size", Math.max(w, h));
        });
        this.sigma.refresh();
    }

    restoreLabels(): void {
        this.syncStyles();
        const useDegreeScaling = this.styleService.degreeScaling;
        this.graph.nodes().forEach(nodeKey => {
            const attrs = this.graph.getNodeAttributes(nodeKey);
            const concept = attrs.metadata.concept as DataVertex;
            const style = this.styleService.resolveNodeStyle(concept.kind as any, getTypeLabel(concept as any));
            this.graph.setNodeAttribute(nodeKey, "label", this.styleParams.vertexDefaultLabel(concept));
            this.graph.setNodeAttribute(nodeKey, "type", style.shape);
            this.graph.setNodeAttribute(nodeKey, "color", style.color);
            this.graph.setNodeAttribute(nodeKey, "borderColor", style.borderColor);
            if (useDegreeScaling) {
                const degree = this.graph.degree(nodeKey);
                const w = style.width + Math.min(degree * 2, style.width * 4);
                const h = style.height + Math.min(degree * 2, style.height * 4);
                this.graph.setNodeAttribute(nodeKey, "width", w);
                this.graph.setNodeAttribute(nodeKey, "height", h);
                this.graph.setNodeAttribute(nodeKey, "size", Math.max(w, h));
            } else {
                this.graph.setNodeAttribute(nodeKey, "width", style.width);
                this.graph.setNodeAttribute(nodeKey, "height", style.height);
                this.graph.setNodeAttribute(nodeKey, "size", Math.max(style.width, style.height));
            }
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

    private applyBackground(): void {
        const container = this.sigma.getContainer();
        const bg = this.styleService.background;
        const css = buildBackgroundCSS(bg);
        container.style.backgroundColor = css.color;
        container.style.backgroundImage = css.image;
        container.style.backgroundSize = css.size;
        if (bg.type === "party") {
            container.style.setProperty("--party-color1", bg.color1);
            container.style.setProperty("--party-color2", bg.color2);
            container.classList.add("party-background");
        } else {
            container.style.removeProperty("--party-color1");
            container.style.removeProperty("--party-color2");
            container.classList.remove("party-background");
        }
    }

    destroy() {
        this.stylesSub.unsubscribe();
        // Force-lose WebGL contexts before killing sigma so the browser
        // reclaims context slots immediately instead of waiting for GC.
        const container = this.sigma.getContainer();
        container.querySelectorAll("canvas").forEach(canvas => {
            const gl = canvas.getContext("webgl2") || canvas.getContext("webgl");
            if (gl) gl.getExtension("WEBGL_lose_context")?.loseContext();
        });
        this.sigma.kill();
    }
}
