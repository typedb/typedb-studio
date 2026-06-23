import { createEdgeCurveProgram } from "@sigma/edge-curve";
import Sigma from "sigma";
import { drawStraightEdgeLabel } from "sigma/rendering";
import { Settings as SigmaSettings } from "sigma/settings";
import MultiGraph from "graphology";
import { NodeDiamondProgram } from "./node-programs/diamond";
import { NodeHexagonProgram } from "./node-programs/hexagon";
import { NodeRoundedRectangleProgram } from "./node-programs/rounded-rect";
import { NodeEllipseProgram } from "./node-programs/ellipse";
import { zoomScaledFontSize } from "./sigma-label-utils";

/**
 * Best-effort heuristic to tell a physical mouse wheel apart from a trackpad
 * two-finger swipe. There's no standard API, so we combine the signals that
 * are reliable per engine:
 *  - Firefox reports mouse-wheel notches in "lines" (deltaMode 1); trackpads
 *    (and high-resolution wheels) report pixels (deltaMode 0).
 *  - Blink/WebKit: a physical wheel notch yields a `wheelDelta` that is a
 *    multiple of 120, whereas trackpad scrolling produces irregular values.
 *  - Fallback: a chunky, vertical-only integer delta looks like a wheel.
 */
function isMouseWheelEvent(e: WheelEvent): boolean {
    if (e.deltaMode !== 0) return true;
    const wheelDelta = (e as any).wheelDeltaY ?? (e as any).wheelDelta;
    if (typeof wheelDelta === "number" && wheelDelta !== 0) {
        return Math.abs(wheelDelta) % 120 === 0;
    }
    return e.deltaX === 0 && Number.isInteger(e.deltaY) && Math.abs(e.deltaY) >= 50;
}

function edgeLabelSize(sourceData: any, targetData: any, maxSize: number): number {
    return Math.max(zoomScaledFontSize(sourceData, maxSize), zoomScaledFontSize(targetData, maxSize));
}

function scaledDrawStraightEdgeLabel(context: CanvasRenderingContext2D, edgeData: any, sourceData: any, targetData: any, settings: any): void {
    const scaledSize = edgeLabelSize(sourceData, targetData, settings.edgeLabelSize);
    if (scaledSize < 3) return;
    drawStraightEdgeLabel(context, edgeData, sourceData, targetData, { ...settings, edgeLabelSize: scaledSize });
}

// Curved edges keep their (cheap, 6-vertex shader) curved *bodies*, but draw
// their labels with the straight-edge renderer. The stock curved-label renderer
// lays text along the bezier by calling measureText AND save/rotate/fillText/
// restore *per character*, per edge, every frame — confirmed (by A/B test) to be
// the decisive cost that made a label-heavy, curve-heavy theme (e.g. Cal
// Aesthetics) crawl during a sim. A straight label at the edge midpoint reads
// fine, keeps the curved body, and costs a fraction.
const ScaledEdgeCurveProgram = createEdgeCurveProgram({
    drawLabel(context, edgeData, sourceData, targetData, settings) {
        scaledDrawStraightEdgeLabel(context, edgeData, sourceData, targetData, settings);
    },
});

export const defaultSigmaSettings: Partial<SigmaSettings> = {
    allowInvalidContainer: true,
    labelFont: '"Darkmode", sans-serif',
    itemSizesReference: "screen",
    autoRescale: false,
    zoomToSizeRatioFunction: (x) => x,
    minCameraRatio: 0.1,
    maxCameraRatio: 100000,
    labelColor: {
        color: `#958fa8`,
    },
    zIndex: true,
    // Show every on-screen node's label (no grid culling). Sigma's density
    // culling keeps only the largest label per fixed *screen-space* cell, which
    // makes labels flicker in/out as nodes slide across cell boundaries while
    // panning/zooming — so it can't be used for a stable, readable graph view.
    labelRenderedSizeThreshold: 0,
    labelDensity: Infinity,
    defaultDrawEdgeLabel: scaledDrawStraightEdgeLabel as any,
    renderEdgeLabels: true,
    nodeProgramClasses: {
        "rounded-rect": NodeRoundedRectangleProgram,
        diamond: NodeDiamondProgram,
        hexagon: NodeHexagonProgram,
        ellipse: NodeEllipseProgram,
    },
    edgeProgramClasses: {
        curved: ScaledEdgeCurveProgram,
    },
};

export function createSigmaRenderer(containerEl: HTMLElement, sigmaSettings: SigmaSettings, graph: MultiGraph): Sigma {
    const renderer = new Sigma(graph, containerEl, sigmaSettings);

    // Defensive heal: sigma occasionally ends up with graphology nodes/edges
    // that have no entry in its internal data caches. When that happens the
    // stock `process()` throws on `data.x = attrs.x` (undefined data) on
    // every frame. We re-index any missing items before delegating to the
    // original implementation.
    const originalProcess = (renderer as any).process;
    (renderer as any).process = function () {
        this.graph.forEachNode((node: string) => {
            if (!this.nodeDataCache[node]) this.addNode(node);
        });
        this.graph.forEachEdge((edge: string) => {
            if (!this.edgeDataCache[edge]) this.addEdge(edge);
        });
        return originalProcess.call(this);
    };

    // Override renderLabels to sort by zIndex. Each drawLabel erases canvas
    // content in its node's shape area before drawing its label, so front nodes
    // (drawn last) erase back nodes' labels where they overlap.
    const X_LABEL_MARGIN = 150;
    const Y_LABEL_MARGIN = 50;
    (renderer as any).renderLabels = function () {
        if (!this.settings.renderLabels) return this;
        const cameraState = this.camera.getState();

        const labelsToDisplay: string[] = this.labelGrid.getLabelsToDisplay(cameraState.ratio, this.settings.labelDensity);
        this.nodesWithForcedLabels.forEach((n: string) => {
            if (!labelsToDisplay.includes(n)) labelsToDisplay.push(n);
        });

        // Draw labels in the SAME back-to-front order Sigma draws the node
        // bodies. `nodeIndices` is the draw index Sigma assigns to each body
        // during indexation (already zIndex-ordered, with graph-insertion order
        // as the tie-break). Reusing it here keeps a node's body and label one
        // group for stacking: a node drawn on top both covers the body behind
        // it AND (via each label's destination-out erase) covers that node's
        // label. Sorting by zIndex alone tie-broke differently from the bodies,
        // which let a node's label sit over a neighbour whose body sat over it.
        labelsToDisplay.sort((a: string, b: string) => {
            const iA = this.nodeIndices[a] ?? 0;
            const iB = this.nodeIndices[b] ?? 0;
            return iA - iB;
        });

        this.displayedNodeLabels = new Set();
        const context = this.canvasContexts.labels;

        for (let i = 0; i < labelsToDisplay.length; i++) {
            const node = labelsToDisplay[i];
            const data = this.nodeDataCache[node];
            if (this.displayedNodeLabels.has(node)) continue;
            if (data.hidden) continue;

            const { x, y } = this.framedGraphToViewport(data);
            const size = this.scaleSize(data.size);
            if (!data.forceLabel && size < this.settings.labelRenderedSizeThreshold) continue;
            if (x < -X_LABEL_MARGIN || x > this.width + X_LABEL_MARGIN ||
                y < -Y_LABEL_MARGIN || y > this.height + Y_LABEL_MARGIN) continue;

            this.displayedNodeLabels.add(node);
            const nodeProgram = this.nodePrograms[data.type];
            const drawLabel = nodeProgram?.drawLabel || this.settings.defaultDrawNodeLabel;
            drawLabel(context, { key: node, ...data, size, x, y }, this.settings);
        }
        return this;
    };

    // Wheel handling:
    //  - Trackpad two-finger swipe → pan (both axes).
    //  - Physical mouse wheel → zoom toward the cursor. A mouse wheel only has
    //    a vertical axis, so panning vertically on it feels wrong; zooming is
    //    the natural mapping.
    //  - Pinch-to-zoom (fired as a wheel event with ctrlKey set) is left for
    //    sigma's own handler.
    const ZOOM_FACTOR = 1.2;
    const container = renderer.getContainer();
    container.addEventListener("wheel", (e: WheelEvent) => {
        if (e.ctrlKey) return;
        e.preventDefault();
        e.stopPropagation();
        const camera = renderer.getCamera();
        if (isMouseWheelEvent(e)) {
            const { ratio } = camera.getState();
            const newRatio = e.deltaY > 0 ? ratio * ZOOM_FACTOR : ratio / ZOOM_FACTOR;
            const rect = container.getBoundingClientRect();
            const target = { x: e.clientX - rect.left, y: e.clientY - rect.top };
            camera.setState(renderer.getViewportZoomedState(target, newRatio));
            return;
        }
        const { ratio } = camera.getState();
        const { width, height } = renderer.getDimensions();
        // Convert pixel deltas to graph-coordinate deltas
        const dx = (e.deltaX * ratio) / width;
        const dy = (e.deltaY * ratio) / height;
        camera.setState({ x: camera.getState().x + dx, y: camera.getState().y - dy });
    }, { capture: true });

    // Override renderHighlightedNodes to only draw hover on the canvas layer,
    // skipping the WebGL re-render which covers our canvas-drawn labels.
    (renderer as any).renderHighlightedNodes = function () {
        const context = (this as any).canvasContexts.hovers;
        context.clearRect(0, 0, (this as any).width, (this as any).height);
        // Clear the WebGL hover layer too
        (this as any).webGLContexts.hoverNodes.clear(WebGLRenderingContext.COLOR_BUFFER_BIT);

        const nodesToRender: string[] = [];
        const hoveredNode = (this as any).hoveredNode;
        if (hoveredNode && !(this as any).nodeDataCache[hoveredNode]?.hidden) {
            nodesToRender.push(hoveredNode);
        }
        (this as any).highlightedNodes.forEach((node: string) => {
            if (node !== hoveredNode) nodesToRender.push(node);
        });

        nodesToRender.forEach((node: string) => {
            const data = (this as any).nodeDataCache[node];
            const { x, y } = (this as any).framedGraphToViewport(data);
            const size = (this as any).scaleSize(data.size);
            const nodeProgram = (this as any).nodePrograms[data.type];
            const drawHover = nodeProgram?.drawHover || (this as any).settings.defaultDrawNodeHover;
            drawHover(context, { key: node, ...data, size, x, y }, (this as any).settings);
        });
    };
    return renderer;
}
