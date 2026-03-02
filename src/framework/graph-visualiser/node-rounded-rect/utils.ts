import { Attributes } from "graphology-types";
import { drawDiscNodeLabel } from "sigma/rendering";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";

const ASPECT = 2.5;

export function drawRoundedRectNodeLabel<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
): void {
    return drawDiscNodeLabel<N, E, G>(context, data, settings);
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
    const size = settings.labelSize,
        font = settings.labelFont,
        weight = settings.labelWeight;

    context.font = `${weight} ${size}px ${font}`;

    context.fillStyle = "#FFF";
    context.shadowOffsetX = 0;
    context.shadowOffsetY = 0;
    context.shadowBlur = 8;
    context.shadowColor = "#000";

    const PADDING = 2;

    if (typeof data.label === "string") {
        const textWidth = context.measureText(data.label).width,
            boxWidth = Math.round(textWidth + 5),
            boxHeight = Math.round(size + 2 * PADDING),
            radiusX = Math.max(data.size * ASPECT, size / 2) + PADDING,
            radiusY = Math.max(data.size, size / 2) + PADDING;

        const cornerR = Math.min(radiusX, radiusY) * 0.25;

        context.beginPath();
        context.moveTo(data.x + radiusX, data.y + boxHeight / 2);
        context.lineTo(data.x + radiusX + boxWidth, data.y + boxHeight / 2);
        context.lineTo(data.x + radiusX + boxWidth, data.y - boxHeight / 2);
        context.lineTo(data.x + radiusX, data.y - boxHeight / 2);
        context.lineTo(data.x + radiusX, data.y - radiusY + cornerR);
        context.arcTo(data.x + radiusX, data.y - radiusY, data.x + radiusX - cornerR, data.y - radiusY, cornerR);
        context.lineTo(data.x - radiusX + cornerR, data.y - radiusY);
        context.arcTo(data.x - radiusX, data.y - radiusY, data.x - radiusX, data.y - radiusY + cornerR, cornerR);
        context.lineTo(data.x - radiusX, data.y + radiusY - cornerR);
        context.arcTo(data.x - radiusX, data.y + radiusY, data.x - radiusX + cornerR, data.y + radiusY, cornerR);
        context.lineTo(data.x + radiusX - cornerR, data.y + radiusY);
        context.arcTo(data.x + radiusX, data.y + radiusY, data.x + radiusX, data.y + radiusY - cornerR, cornerR);
        context.lineTo(data.x + radiusX, data.y + boxHeight / 2);
        context.closePath();
        context.fill();
    } else {
        const radiusX = data.size * ASPECT + PADDING;
        const radiusY = data.size + PADDING;
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
    }

    context.shadowOffsetX = 0;
    context.shadowOffsetY = 0;
    context.shadowBlur = 0;

    drawRoundedRectNodeLabel(context, data, settings);
}
