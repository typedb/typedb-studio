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
