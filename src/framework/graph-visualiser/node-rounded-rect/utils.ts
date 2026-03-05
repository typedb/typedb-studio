import { Attributes } from "graphology-types";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";
import { drawCenteredNodeLabel } from "../label-utils";

export function drawRoundedRectNodeLabel<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
): void {
    return drawCenteredNodeLabel<N, E, G>(context, data, settings);
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
    context.fillStyle = "#FFF";
    context.shadowOffsetX = 0;
    context.shadowOffsetY = 0;
    context.shadowBlur = 8;
    context.shadowColor = "#000";

    const PADDING = 2;

    const rawW = (data as any).width ?? data.size;
    const rawH = (data as any).height ?? data.size;
    const scale = data.size / Math.max(rawW, rawH);
    const radiusX = rawW * scale + PADDING;
    const radiusY = rawH * scale + PADDING;
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
    context.fill();

    context.shadowOffsetX = 0;
    context.shadowOffsetY = 0;
    context.shadowBlur = 0;

    drawRoundedRectNodeLabel(context, data, settings);
}
