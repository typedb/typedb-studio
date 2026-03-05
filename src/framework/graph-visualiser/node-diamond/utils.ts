import { Attributes } from "graphology-types";
import { drawDiscNodeLabel } from "sigma/rendering";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";

export function drawDiamondNodeLabel<
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

export function drawDiamondNodeHover<
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

    const w = (data as any).width ?? data.size;
    const h = (data as any).height ?? data.size;
    const aspect = w / h;

    if (typeof data.label === "string") {
        const textWidth = context.measureText(data.label).width,
            boxWidth = Math.round(textWidth + 5),
            boxHeight = Math.round(size + 2 * PADDING),
            rx = (Math.max(data.size, size / 2) + PADDING) * aspect,
            ry = Math.max(data.size, size / 2) + PADDING;

        context.beginPath();
        context.moveTo(data.x + rx, data.y + boxHeight / 2);
        context.lineTo(data.x + rx + boxWidth, data.y + boxHeight / 2);
        context.lineTo(data.x + rx + boxWidth, data.y - boxHeight / 2);
        context.lineTo(data.x + rx, data.y - boxHeight / 2);
        context.lineTo(data.x + rx, data.y - ry);
        context.lineTo(data.x, data.y - ry);
        context.lineTo(data.x - rx, data.y);
        context.lineTo(data.x, data.y + ry);
        context.lineTo(data.x + rx, data.y + ry);
        context.lineTo(data.x + rx, data.y + boxHeight / 2);
        context.closePath();
        context.fill();
    } else {
        const rx = (data.size + PADDING) * aspect;
        const ry = data.size + PADDING;
        context.beginPath();
        context.moveTo(data.x, data.y - ry);
        context.lineTo(data.x + rx, data.y);
        context.lineTo(data.x, data.y + ry);
        context.lineTo(data.x - rx, data.y);
        context.closePath();
        context.fill();
    }

    context.shadowOffsetX = 0;
    context.shadowOffsetY = 0;
    context.shadowBlur = 0;

    drawDiamondNodeLabel(context, data, settings);
}
