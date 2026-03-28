import { Attributes } from "graphology-types";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";
import { drawCenteredNodeLabel, drawExternalNodeLabel, getLabelsVisible } from "../../sigma-label-utils";

export function drawRoundedRectNodeLabel<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
): void {
    drawCenteredNodeLabel<N, E, G>(context, data, settings);
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
        if (truncated) drawExternalNodeLabel(context, data, settings);
        return;
    }

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
