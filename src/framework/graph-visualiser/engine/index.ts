import {
    ApiResponse,
    isApiErrorResponse,
    QueryResponse,
} from "@typedb/driver-http";
import chroma from "chroma-js";
import Sigma from "sigma";
import { Subscription } from "rxjs";
import { buildBackgroundCSS } from "../../../service/graph-style.service";
import type { GraphStyleService } from "../../../service/graph-style.service";

import { getTypeLabel, DataVertex } from "@typedb/graph-utils";
import { buildStructuredAnswers } from "@typedb/graph-utils";
import { AnalyzedPipelineBackCompat } from "./types";
import { Graph, GraphBuilderStructureParams, defaultStructureParams } from "./graph";
import { GraphBuilder } from "./graph-builder";
import { GraphStyles, colorEdgesByConstraintIndex as _colorEdgesByConstraintIndex, colorQuery as _colorQuery } from "./styles";
import { setUseBorderColorForLabels, setLabelsVisible, setShowHoverLabel } from "./sigma-label-utils";
import { InteractionHandler, StudioState } from "./interaction-handler";
import { LayoutWrapper } from "./layout";
import { createSigmaRenderer, defaultSigmaSettings } from "./sigma-settings";

export type GraphPngExportMode = "currentView" | "wholeGraph";
const MAX_EXPORT_DIMENSION = 8192;

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
    /**
     * When set, every layout tick re-projects this world point back to the
     * current bbox's coordinate space and snaps the camera there. Used by
     * `reheat({ preserveCamera })` so the camera stays pinned to the world
     * spot the user was looking at even as the simulation re-positions nodes
     * and Sigma's bbox shifts under it.
     */
    private pinnedCameraWorld: { worldX: number; worldY: number; ratio: number } | null = null;
    private stylesSub!: Subscription;

    constructor(public graph: Graph, public sigma: Sigma, public layout: LayoutWrapper, public styleService: GraphStyleService) {
        this.state = { activeQueryDatabase: null };
        this.styleParams = this.syncStyles();
        this.interactionHandler = new InteractionHandler(graph, sigma, this.state, this.styleParams, this.styleService);
        this.interactionHandler.visualiser = this;
        this.interactionHandler.layout = this.layout;
        this.setupReducers();
        this.layout.onTick = () => {
            if (this.pinnedCameraWorld) {
                this.restoreCameraWorld(this.pinnedCameraWorld);
            } else if (this.autoZoomEnabled) {
                this.centerCamera();
            }
        };
        this.sigma.getCamera().addListener("updated", () => {
            if (!this.settingCameraProgrammatically) {
                this.autoZoomEnabled = false;
                // User took control — release the pin so future Explores don't
                // snap back to a stale saved position.
                this.pinnedCameraWorld = null;
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
        const FADE_RATIO = 0.15; // mix 15% original color, 85% background
        const PREVIEW_FADE_RATIO = 0.3; // mix 30% original color, 70% background — gentler than the regular fade

        const fade = (color: string) => chroma.mix(this.styleService.effectiveBackgroundHex, color, FADE_RATIO).hex();
        const fadeForPreview = (color: string) => chroma.mix(this.styleService.effectiveBackgroundHex, color, PREVIEW_FADE_RATIO).hex();
        const buildFadeSoft = (alpha: number) => (color: string) => {
            // Output premultiplied-alpha hex for Sigma's gl.blendFunc(ONE, ONE_MINUS_SRC_ALPHA)
            const [r, g, b] = chroma(color).rgb();
            const pr = Math.round(r * alpha);
            const pg = Math.round(g * alpha);
            const pb = Math.round(b * alpha);
            const pa = Math.round(alpha * 255);
            return `#${pr.toString(16).padStart(2, "0")}${pg.toString(16).padStart(2, "0")}${pb.toString(16).padStart(2, "0")}${pa.toString(16).padStart(2, "0")}`;
        };
        const fadeSoft = buildFadeSoft(0.25);
        const fadeSoftForPreview = buildFadeSoft(0.3);

        this.sigma.setSetting("nodeReducer", (node, data) => {
            const state = this.interactionHandler.state;
            let shouldFade = false;
            let isPreviewFade = false;

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
                        } else if (state.selectedNode == null && this.styleService.isPreviewActive()) {
                            // Hover-preview: only kicks in when there's no real highlight or selection
                            const attrs = this.graph.getNodeAttributes(node);
                            const concept = attrs.metadata.concept;
                            if (!this.styleService.shouldPreviewNode(concept.kind as any, getTypeLabel(concept as any))) {
                                shouldFade = true;
                                isPreviewFade = true;
                            }
                        }
                    } catch (_) { /* guard against missing metadata during graph mutations */ }
                }
            }

            if (!shouldFade) return { ...data, zIndex: 1 };
            const res = { ...data };
            if (isPreviewFade) {
                res["color"] = fadeSoftForPreview(data["color"]);
                if (data["borderColor"]) res["borderColor"] = fadeSoftForPreview(data["borderColor"]);
                // Keep the label visible for a subtle preview
                res["zIndex"] = 0;
            } else {
                res["color"] = fadeSoft(data["color"]);
                if (data["borderColor"]) res["borderColor"] = fadeSoft(data["borderColor"]);
                res["label"] = "";
                res["zIndex"] = 0;
            }
            return res;
        });

        this.sigma.setSetting("edgeReducer", (edge, data) => {
            const state = this.interactionHandler.state;
            let shouldFade = false;
            let isPreviewFade = false;

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
                            const source = this.graph.source(edge);
                            const target = this.graph.target(edge);
                            const sourceAttrs = this.graph.getNodeAttributes(source);
                            const targetAttrs = this.graph.getNodeAttributes(target);
                            const sourceConcept = sourceAttrs.metadata.concept;
                            const targetConcept = targetAttrs.metadata.concept;
                            const sourceHighlighted = this.styleService.shouldHighlightNode(sourceConcept.kind as any, getTypeLabel(sourceConcept as any));
                            const targetHighlighted = this.styleService.shouldHighlightNode(targetConcept.kind as any, getTypeLabel(targetConcept as any));
                            if (!sourceHighlighted || !targetHighlighted) {
                                shouldFade = true;
                            }
                            const tag = this.graph.getEdgeAttributes(edge).metadata?.dataEdge?.tag;
                            if (tag && !this.styleService.shouldHighlightEdge(tag)) {
                                shouldFade = true;
                            }
                        } else if (state.selectedNode == null && this.styleService.isPreviewActive()) {
                            const source = this.graph.source(edge);
                            const target = this.graph.target(edge);
                            const sourceAttrs = this.graph.getNodeAttributes(source);
                            const targetAttrs = this.graph.getNodeAttributes(target);
                            const sourceConcept = sourceAttrs.metadata.concept;
                            const targetConcept = targetAttrs.metadata.concept;
                            const sourcePreviewed = this.styleService.shouldPreviewNode(sourceConcept.kind as any, getTypeLabel(sourceConcept as any));
                            const targetPreviewed = this.styleService.shouldPreviewNode(targetConcept.kind as any, getTypeLabel(targetConcept as any));
                            if (!sourcePreviewed || !targetPreviewed) {
                                shouldFade = true;
                                isPreviewFade = true;
                            }
                            const tag = this.graph.getEdgeAttributes(edge).metadata?.dataEdge?.tag;
                            if (tag && !this.styleService.shouldPreviewEdge(tag)) {
                                shouldFade = true;
                                isPreviewFade = true;
                            }
                        }
                    } catch (_) { /* guard against missing metadata during graph mutations */ }
                }
            }

            if (!shouldFade) return { ...data, zIndex: 1 };
            const res = { ...data };
            res["color"] = (isPreviewFade ? fadeForPreview : fade)(data["color"] ?? "#ccc");
            if (!isPreviewFade) res["label"] = "";
            res["zIndex"] = 0;
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
            this.graph.setNodeAttribute(nodeKey, "color", style.fillColor);
            this.graph.setNodeAttribute(nodeKey, "borderColor", style.color);
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
        this.pinnedCameraWorld = null;
        this.unfreezeViewport();
        this.graph.nodes().forEach(node => {
            this.graph.setNodeAttribute(node, "x", Math.random());
            this.graph.setNodeAttribute(node, "y", Math.random());
        });
        // Positions are now random — drop the "settled" set so the simulation
        // starts fresh and doesn't try to anchor new nodes to stale data.
        this.layout.forgetSettled();
        this.layout.startOrRedraw();
        this.centerCamera();
    }

    /**
     * Resume the layout simulation so newly added nodes settle into the
     * existing graph. Unlike `reLayout`, this preserves current node
     * positions — the simulation restarts from where things are now.
     *
     * - `soft`: use a lower initial alpha + higher alpha decay so the
     *   simulation perturbs the layout less and settles faster. Good for
     *   incremental Explore/Add actions where we don't want the existing
     *   layout to swirl.
     * - `preserveCamera`: don't re-enable auto-zoom and don't recenter.
     *   Use when the user has framed the view themselves and an Explore
     *   shouldn't yank the camera around.
     */
    /**
     * Pin the current bbox so that subsequent graph mutations (new nodes
     * appearing, simulation re-laying out positions) don't visually shift
     * the camera: Sigma stores the camera in coords normalized to the bbox,
     * so an auto-growing bbox makes the same camera state cover more world
     * space — which looks like a zoom-out. Same trick used in
     * `onDownNode` when a drag begins. No-op if already pinned.
     */
    freezeViewport(): void {
        if (!this.sigma.getCustomBBox()) {
            this.sigma.setCustomBBox(this.sigma.getBBox());
        }
    }

    /** Release the pinned bbox so Sigma resumes auto-fitting to the graph. */
    unfreezeViewport(): void {
        this.sigma.setCustomBBox(null);
    }

    /**
     * Capture the camera's current world coordinates + ratio. Combined with
     * `restoreCameraWorld`, this lets a caller pin the camera to the same
     * world point across operations that mutate the bbox.
     */
    captureCameraWorld(): { worldX: number; worldY: number; ratio: number } {
        const cam = this.sigma.getCamera().getState();
        const bbox = this.sigma.getCustomBBox() ?? this.sigma.getBBox();
        const bboxW = (bbox.x[1] - bbox.x[0]) || 1;
        const bboxH = (bbox.y[1] - bbox.y[0]) || 1;
        return {
            worldX: bbox.x[0] + cam.x * bboxW,
            worldY: bbox.y[0] + cam.y * bboxH,
            ratio: cam.ratio,
        };
    }

    /** Convert saved world coords back to current-bbox-relative camera state. */
    restoreCameraWorld(saved: { worldX: number; worldY: number; ratio: number }): void {
        const bbox = this.sigma.getCustomBBox() ?? this.sigma.getBBox();
        const bboxW = (bbox.x[1] - bbox.x[0]) || 1;
        const bboxH = (bbox.y[1] - bbox.y[0]) || 1;
        const x = (saved.worldX - bbox.x[0]) / bboxW;
        const y = (saved.worldY - bbox.y[0]) / bboxH;
        this.settingCameraProgrammatically = true;
        this.sigma.getCamera().setState({ x, y, ratio: saved.ratio, angle: 0 });
        this.settingCameraProgrammatically = false;
    }

    reheat(opts?: { soft?: boolean; preserveCamera?: boolean }): void {
        if (opts?.preserveCamera) {
            // Force off — without this, an already-true `autoZoomEnabled`
            // would have the onTick callback recentering the camera on every
            // frame as nodes shift around.
            this.autoZoomEnabled = false;
            // Pin the camera to the world point it's currently looking at so
            // the per-tick onTick callback can snap it back there as the
            // simulation re-positions things and Sigma's bbox shifts.
            this.pinnedCameraWorld = this.captureCameraWorld();
        } else {
            this.autoZoomEnabled = true;
            this.peakCameraRatio = 0;
            this.pinnedCameraWorld = null;
        }
        if (opts?.soft) {
            this.layout.start({ initialAlpha: 0.5, alphaDecay: 0.04 });
        } else {
            this.layout.start();
        }
        if (!opts?.preserveCamera) {
            this.centerCamera();
        }
    }

    centerCamera(zoomOutOnly = false): void {
        const nodes = this.graph.nodes();
        if (nodes.length === 0) return;

        const { width, height } = this.sigma.getDimensions();
        if (width === 0 || height === 0) return;

        // Reset view should always re-fit the entire graph; clear any pinned
        // bbox so x/y = 0.5 actually centers on the full extent rather than
        // on a stale frozen subregion.
        this.unfreezeViewport();
        this.pinnedCameraWorld = null;

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
            // Snapshot whether the graph was empty *before* this push. A
            // first-time push (e.g. opening a new type tab) gets the full
            // auto-fit treatment; subsequent incremental pushes (Inspector
            // Explore/Add actions) leave the camera and layout supervisor
            // alone so the user's focused view isn't yanked away. The
            // inspector kicks its own `reheat({ preserveCamera })` after.
            const wasEmpty = this.graph.order === 0;
            this.state.activeQueryDatabase = database;
            this.handleQueryResult(res);
            if (this.styleService.degreeScaling) this.applyStyleUpdate();
            if (wasEmpty) {
                this.autoZoomEnabled = true;
                this.peakCameraRatio = 0;
                this.layout.startOrRedraw();
                this.centerCamera();
            }
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

    /**
     * Programmatically select an instance node — equivalent to the user
     * clicking it: highlight ring, neighbor fade, Inspector update. For
     * entity/relation nodes we match by IID; for attributes we match by
     * (typeLabel, value). No-op if the node isn't in the graph yet.
     */
    selectInstance(kind: "entity" | "relation" | "attribute", typeLabel: string, instanceId: string): void {
        const nodeKey = this.findInstanceNode(kind, typeLabel, instanceId);
        if (nodeKey == null) return;
        this.interactionHandler.selectNode(nodeKey);
    }

    /**
     * Set the secondary highlight anchors by instance identity (kind +
     * typeLabel + iid/value) — used by the Inspector to keep every step in
     * its breadcrumb trail visually lit alongside the current selection.
     * Entries that don't map to a graph node are silently dropped.
     */
    setHighlightAnchorsByInstance(specs: { kind: "entity" | "relation" | "attribute"; typeLabel: string; instanceId: string }[]): void {
        const keys = new Set<string>();
        for (const spec of specs) {
            const key = this.findInstanceNode(spec.kind, spec.typeLabel, spec.instanceId);
            if (key != null) keys.add(key);
        }
        this.interactionHandler.setSecondaryAnchors(keys);
    }

    /**
     * Pan + zoom the camera to enclose the current selection (selected node +
     * its highlighted neighbors). No-op if nothing is selected. Mirrors the
     * bbox-fitting math of `focusSearchMatches`.
     */
    focusSelection(): void {
        const handler = this.interactionHandler;
        const selectedNode = handler.state.selectedNode;
        if (selectedNode == null) return;

        const { width, height } = this.sigma.getDimensions();
        if (width === 0 || height === 0) return;

        const nodes = new Set<string>([selectedNode]);
        handler.state.selectedNeighbors?.forEach(n => nodes.add(n));

        let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
        nodes.forEach(node => {
            try {
                const attrs = this.graph.getNodeAttributes(node);
                if (attrs.x == null || attrs.y == null) return;
                minX = Math.min(minX, attrs.x);
                maxX = Math.max(maxX, attrs.x);
                minY = Math.min(minY, attrs.y);
                maxY = Math.max(maxY, attrs.y);
            } catch { /* missing metadata mid-mutation */ }
        });
        if (!isFinite(minX)) return;

        const graphWidth = maxX - minX || 1;
        const graphHeight = maxY - minY || 1;
        const padding = 1.3;
        const rawRatio = Math.max(graphWidth / width, graphHeight / height) * padding;
        const ratio = Math.max(Math.min(rawRatio, 20), 1);

        const centerX = (minX + maxX) / 2;
        const centerY = (minY + maxY) / 2;
        const bbox = this.sigma.getCustomBBox() || this.sigma.getBBox();
        const x = (centerX - bbox.x[0]) / (bbox.x[1] - bbox.x[0]) || 0.5;
        const y = (centerY - bbox.y[0]) / (bbox.y[1] - bbox.y[0]) || 0.5;

        // Focus is establishing a new framing — drop any prior world pin so
        // the next onTick doesn't snap us back somewhere else.
        this.autoZoomEnabled = false;
        this.pinnedCameraWorld = null;
        this.settingCameraProgrammatically = true;
        this.sigma.getCamera().setState({ x, y, ratio, angle: 0 });
        this.settingCameraProgrammatically = false;
    }

    private findInstanceNode(kind: "entity" | "relation" | "attribute", typeLabel: string, instanceId: string): string | null {
        let found: string | null = null;
        this.graph.nodes().forEach(node => {
            if (found != null) return;
            try {
                const concept = this.graph.getNodeAttributes(node)?.["metadata"]?.concept;
                if (!concept) return;
                if (kind === "attribute") {
                    if (concept.kind === "attribute"
                        && concept.type?.label === typeLabel
                        && String(concept.value) === instanceId) {
                        found = node;
                    }
                } else if (concept.kind === kind && concept.iid === instanceId) {
                    found = node;
                }
            } catch { /* missing metadata mid-mutation */ }
        });
        return found;
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
            this.graph.setNodeAttribute(nodeKey, "color", style.fillColor);
            this.graph.setNodeAttribute(nodeKey, "borderColor", style.color);
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
            this.graph.setNodeAttribute(nodeKey, "color", style.fillColor);
            this.graph.setNodeAttribute(nodeKey, "borderColor", style.color);
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

    /**
     * Render the current graph into an offscreen Sigma instance at 100% zoom (ratio = 1)
     * and composite all of its canvas layers (WebGL nodes/edges + 2D labels) into a single
     * PNG blob.
     *
     * - "currentView": the captured graph region matches what the user can currently see,
     *   but resolved at 1 graph unit = 1 pixel. Output canvas size = liveViewport × liveCameraRatio.
     * - "wholeGraph": fits every node, also at 1 graph unit = 1 pixel, with padding for node radii.
     *
     * Throws if the graph has no nodes. Dimensions are capped at MAX_EXPORT_DIMENSION per
     * side to stay within browser canvas limits.
     */
    async exportPng(mode: GraphPngExportMode): Promise<Blob> {
        if (this.graph.order === 0) throw new Error("Graph is empty");

        let width: number;
        let height: number;
        let cameraState: { x: number; y: number; ratio: number; angle: number };

        if (mode === "wholeGraph") {
            let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
            let maxNodeRadius = 0;
            this.graph.nodes().forEach(n => {
                const a = this.graph.getNodeAttributes(n);
                minX = Math.min(minX, a.x);
                maxX = Math.max(maxX, a.x);
                minY = Math.min(minY, a.y);
                maxY = Math.max(maxY, a.y);
                const w = (a as any).width ?? a.size ?? 0;
                const h = (a as any).height ?? a.size ?? 0;
                maxNodeRadius = Math.max(maxNodeRadius, Math.max(w, h) / 2);
            });
            const padding = Math.ceil(maxNodeRadius + 32);
            width = Math.max(1, Math.ceil(maxX - minX) + 2 * padding);
            height = Math.max(1, Math.ceil(maxY - minY) + 2 * padding);
            cameraState = { x: 0.5, y: 0.5, ratio: 1, angle: 0 };
        } else {
            const liveCam = this.sigma.getCamera().getState();
            const liveDims = this.sigma.getDimensions();
            width = Math.max(1, Math.ceil(liveDims.width * liveCam.ratio));
            height = Math.max(1, Math.ceil(liveDims.height * liveCam.ratio));
            cameraState = { x: liveCam.x, y: liveCam.y, ratio: 1, angle: liveCam.angle };
        }

        // Clamp to browser canvas limits, preserving aspect ratio.
        const scale = Math.min(1, MAX_EXPORT_DIMENSION / Math.max(width, height));
        if (scale < 1) {
            width = Math.max(1, Math.floor(width * scale));
            height = Math.max(1, Math.floor(height * scale));
            cameraState = { ...cameraState, ratio: 1 / scale };
        }

        const bgHex = this.styleService.effectiveBackgroundHex;
        const liveNodeReducer = this.sigma.getSetting("nodeReducer");
        const liveEdgeReducer = this.sigma.getSetting("edgeReducer");

        const container = document.createElement("div");
        container.style.position = "fixed";
        container.style.left = "-99999px";
        container.style.top = "0";
        container.style.width = `${width}px`;
        container.style.height = `${height}px`;
        container.style.visibility = "hidden";
        document.body.appendChild(container);

        let exportSigma: Sigma | null = null;
        try {
            exportSigma = createSigmaRenderer(container, defaultSigmaSettings as any, this.graph);
            if (liveNodeReducer) exportSigma.setSetting("nodeReducer", liveNodeReducer);
            if (liveEdgeReducer) exportSigma.setSetting("edgeReducer", liveEdgeReducer);
            // For currentView, propagate the live sigma's effective bbox so that the
            // normalised camera (x, y) coords resolve to the same graph coordinates in
            // both sigmas. For wholeGraph, let the export sigma auto-compute its bbox
            // from current node positions so (0.5, 0.5) lands on the actual graph center.
            if (mode === "currentView") {
                const liveBBox = this.sigma.getCustomBBox() ?? this.sigma.getBBox();
                exportSigma.setCustomBBox(liveBBox);
            }
            exportSigma.getCamera().setState(cameraState);

            return await new Promise<Blob>((resolve, reject) => {
                const sigma = exportSigma!;
                const onRendered = () => {
                    sigma.removeListener("afterRender", onRendered);
                    try {
                        // Sigma sizes its internal canvases at (cssDimensions × devicePixelRatio)
                        // physical pixels. Match the final canvas to physical pixel dimensions
                        // so drawImage copies 1:1 — otherwise we'd clip the source on hi-DPI displays.
                        const sourceCanvas = container.querySelector("canvas") as HTMLCanvasElement | null;
                        const physW = sourceCanvas?.width ?? width;
                        const physH = sourceCanvas?.height ?? height;
                        const finalCanvas = document.createElement("canvas");
                        finalCanvas.width = physW;
                        finalCanvas.height = physH;
                        const ctx = finalCanvas.getContext("2d");
                        if (!ctx) throw new Error("Could not create 2D context for PNG export");
                        ctx.fillStyle = bgHex;
                        ctx.fillRect(0, 0, physW, physH);
                        container.querySelectorAll("canvas").forEach(c => ctx.drawImage(c, 0, 0));
                        finalCanvas.toBlob(blob => {
                            if (blob) resolve(blob);
                            else reject(new Error("Canvas.toBlob returned null"));
                        }, "image/png");
                    } catch (err) {
                        reject(err);
                    }
                };
                sigma.on("afterRender", onRendered);
                sigma.refresh();
            });
        } finally {
            if (exportSigma) {
                container.querySelectorAll("canvas").forEach(c => {
                    const gl = (c as HTMLCanvasElement).getContext("webgl2") || (c as HTMLCanvasElement).getContext("webgl");
                    if (gl) gl.getExtension("WEBGL_lose_context")?.loseContext();
                });
                exportSigma.kill();
            }
            container.parentNode?.removeChild(container);
        }
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
