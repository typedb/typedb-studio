import { Attributes } from "graphology-types";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";
import { drawCenteredNodeLabel, drawExternalNodeLabel, getLabelsVisible, getShowHoverLabel, zoomScaledFontSize } from "../../sigma-label-utils";

export function drawRoundedRectNodeLabel<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
): void {
    context.save();
    buildRoundedRectPath(context, data);
    // Erase any canvas content behind this node (e.g. labels from lower-z nodes)
    context.globalCompositeOperation = "destination-out";
    context.fillStyle = "#000";
    context.fill();
    context.globalCompositeOperation = "source-over";
    // Draw label clipped to shape
    context.clip();
    drawCenteredNodeLabel<N, E, G>(context, data, settings);
    context.restore();
}

function buildRoundedRectPath(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "color">,
): void {
    const rawW = (data as any).width ?? data.size;
    const rawH = (data as any).height ?? data.size;
    const scale = data.size / Math.max(rawW, rawH);
    const radiusX = rawW * scale;
    const radiusY = rawH * scale;
    const cornerR = Math.min(radiusX, radiusY) * 0.25;

    context.beginPath();
    context.moveTo(data.x + radiusX - cornerR, data.y - radiusY);
    context.arcTo(data.x + radiusX, data.y - radiusY, data.x + radiusX, data.y - radiusY + cornerR, cornerR);
    context.lineTo(data.x + radiusX, data.y + radiusY - cornerR);
    context.arcTo(data.x + radiusX, data.y + radiusY, data.x + radiusX - cornerR, data.y + radiusY, cornerR);
    context.lineTo(data.x - radiusX + cornerR, data.y + radiusY);
    context.arcTo(data.x - radiusX, data.y + radiusY, data.x - radiusX, data.y + radiusY - cornerR, cornerR);
    context.lineTo(data.x - radiusX, data.y - radiusY + cornerR);
    context.arcTo(data.x - radiusX, data.y - radiusY, data.x - radiusX + cornerR, data.y - radiusY, cornerR);
    context.closePath();
}

export function drawRoundedRectNodeHover<
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
        const scaledDown = zoomScaledFontSize(data as any, settings.labelSize) < settings.labelSize;
        if ((truncated || scaledDown) && getShowHoverLabel()) drawExternalNodeLabel(context, data, settings);
        return;
    }
    if (!getShowHoverLabel()) return;

    const rawW = (data as any).width ?? data.size;
    const rawH = (data as any).height ?? data.size;
    const scale = data.size / Math.max(rawW, rawH);
    const radiusX = rawW * scale + 2;
    const radiusY = rawH * scale + 2;
    const cornerR = Math.min(radiusX, radiusY) * 0.25;

    context.fillStyle = "rgba(255, 255, 255, 0.15)";
    context.beginPath();
    context.moveTo(data.x + radiusX - cornerR, data.y - radiusY);
    context.arcTo(data.x + radiusX, data.y - radiusY, data.x + radiusX, data.y - radiusY + cornerR, cornerR);
    context.lineTo(data.x + radiusX, data.y + radiusY - cornerR);
    context.arcTo(data.x + radiusX, data.y + radiusY, data.x + radiusX - cornerR, data.y + radiusY, cornerR);
    context.lineTo(data.x - radiusX + cornerR, data.y + radiusY);
    context.arcTo(data.x - radiusX, data.y + radiusY, data.x - radiusX, data.y + radiusY - cornerR, cornerR);
    context.lineTo(data.x - radiusX, data.y - radiusY + cornerR);
    context.arcTo(data.x - radiusX, data.y - radiusY, data.x - radiusX + cornerR, data.y - radiusY, cornerR);
    context.closePath();
    context.fill();

    drawExternalNodeLabel(context, data, settings);
}
