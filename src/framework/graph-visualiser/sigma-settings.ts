import { createEdgeCurveProgram, createDrawCurvedEdgeLabel, DEFAULT_EDGE_CURVE_PROGRAM_OPTIONS } from "@sigma/edge-curve";
import Sigma from "sigma";
import { drawStraightEdgeLabel } from "sigma/rendering";
import { Settings as SigmaSettings } from "sigma/settings";
import MultiGraph from "graphology";
import { NodeDiamondProgram } from "./node-programs/diamond";
import { NodeRoundedRectangleProgram } from "./node-programs/rounded-rect";
import { NodeEllipseProgram } from "./node-programs/ellipse";
import { zoomScaledFontSize } from "./sigma-label-utils";

function edgeLabelSize(sourceData: any, targetData: any, maxSize: number): number {
    return Math.max(zoomScaledFontSize(sourceData, maxSize), zoomScaledFontSize(targetData, maxSize));
}

function scaledDrawStraightEdgeLabel(context: CanvasRenderingContext2D, edgeData: any, sourceData: any, targetData: any, settings: any): void {
    const scaledSize = edgeLabelSize(sourceData, targetData, settings.edgeLabelSize);
    if (scaledSize < 3) return;
    drawStraightEdgeLabel(context, edgeData, sourceData, targetData, { ...settings, edgeLabelSize: scaledSize });
}

const defaultDrawCurvedLabel = createDrawCurvedEdgeLabel(DEFAULT_EDGE_CURVE_PROGRAM_OPTIONS as any);
const ScaledEdgeCurveProgram = createEdgeCurveProgram({
    drawLabel(context, edgeData, sourceData, targetData, settings) {
        const scaledSize = edgeLabelSize(sourceData, targetData, settings.edgeLabelSize);
        if (scaledSize < 3) return;
        defaultDrawCurvedLabel(
            context, edgeData, sourceData, targetData,
            { ...settings, edgeLabelSize: scaledSize },
        );
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
    labelRenderedSizeThreshold: 0,
    labelDensity: Infinity,
    defaultDrawEdgeLabel: scaledDrawStraightEdgeLabel as any,
    renderEdgeLabels: true,
    nodeProgramClasses: {
        "rounded-rect": NodeRoundedRectangleProgram,
        diamond: NodeDiamondProgram,
        ellipse: NodeEllipseProgram,
    },
    edgeProgramClasses: {
        curved: ScaledEdgeCurveProgram,
    },
};

export function createSigmaRenderer(containerEl: HTMLElement, sigmaSettings: SigmaSettings, graph: MultiGraph): Sigma {
    const renderer = new Sigma(graph, containerEl, sigmaSettings);
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

        // Sort by zIndex so higher-zIndex nodes erase and draw on top
        labelsToDisplay.sort((a: string, b: string) => {
            const zA = this.nodeDataCache[a]?.zIndex ?? 0;
            const zB = this.nodeDataCache[b]?.zIndex ?? 0;
            return zA - zB;
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

    // Convert wheel/trackpad scroll from zoom to pan, but let pinch-to-zoom
    // through (browsers fire pinch gestures as wheel events with ctrlKey set)
    const container = renderer.getContainer();
    container.addEventListener("wheel", (e: WheelEvent) => {
        if (e.ctrlKey) return;
        e.preventDefault();
        e.stopPropagation();
        const camera = renderer.getCamera();
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
