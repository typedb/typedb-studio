import { createEdgeCurveProgram, createDrawCurvedEdgeLabel, DEFAULT_EDGE_CURVE_PROGRAM_OPTIONS } from "@sigma/edge-curve";
import Sigma from "sigma";
import { drawStraightEdgeLabel } from "sigma/rendering";
import { Settings as SigmaSettings } from "sigma/settings";
import MultiGraph from "graphology";
import { NodeDiamondProgram } from "./node-diamond";
import { NodeRoundedRectangleProgram } from "./node-rounded-rect";
import { NodeEllipseProgram } from "./node-ellipse";
import { zoomScaledFontSize } from "./label-utils";

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
    itemSizesReference: "positions",
    autoRescale: false,
    zoomToSizeRatioFunction: (x) => x,
    minCameraRatio: 0.1,
    maxCameraRatio: 10,
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

export function createSigmaRenderer(containerEl: HTMLElement, sigma_settings: SigmaSettings, graph: MultiGraph): Sigma {
    const renderer = new Sigma(graph, containerEl, sigma_settings);
    // Disable hover rendering (node re-draw on hover WebGL layer)
    (renderer as any).renderHighlightedNodes = () => {};
    return renderer;
}
