import { Attributes } from "graphology-types";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";
import { drawCenteredNodeLabel, drawExternalNodeLabel, getLabelsVisible, getShowHoverLabel, zoomScaledFontSize } from "../../sigma-label-utils";

export function drawDiamondNodeLabel<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
): void {
    context.save();
    buildDiamondPath(context, data);
    context.globalCompositeOperation = "destination-out";
    context.fillStyle = "#000";
    context.fill();
    context.globalCompositeOperation = "source-over";
    context.clip();
    drawCenteredNodeLabel<N, E, G>(context, data, settings);
    context.restore();
}

function buildDiamondPath(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "color">,
): void {
    const rawW = (data as any).width ?? data.size;
    const rawH = (data as any).height ?? data.size;
    const scale = data.size / Math.max(rawW, rawH);
    const rx = rawW * scale;
    const ry = rawH * scale;

    context.beginPath();
    context.moveTo(data.x, data.y - ry);
    context.lineTo(data.x + rx, data.y);
    context.lineTo(data.x, data.y + ry);
    context.lineTo(data.x - rx, data.y);
    context.closePath();
}

export function drawDiamondNodeHover<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
): void {
    if (getLabelsVisible()) {
        const truncated = drawCenteredNodeLabel(context, data, settings);
        const scaledDown = zoomScaledFontSize(data as any, settings.labelSize) < settings.labelSize * 0.75;
        if ((truncated || scaledDown) && getShowHoverLabel()) drawExternalNodeLabel(context, data, settings);
        return;
    }
    if (!getShowHoverLabel()) return;

    const rawW = (data as any).width ?? data.size;
    const rawH = (data as any).height ?? data.size;
    const scale = data.size / Math.max(rawW, rawH);
    const rx = rawW * scale + 2;
    const ry = rawH * scale + 2;

    context.fillStyle = "rgba(255, 255, 255, 0.15)";
    context.beginPath();
    context.moveTo(data.x, data.y - ry);
    context.lineTo(data.x + rx, data.y);
    context.lineTo(data.x, data.y + ry);
    context.lineTo(data.x - rx, data.y);
    context.closePath();
    context.fill();

    drawExternalNodeLabel(context, data, settings);
}
