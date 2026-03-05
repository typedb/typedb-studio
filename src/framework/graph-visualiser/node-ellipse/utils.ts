import { Attributes } from "graphology-types";
import { drawDiscNodeLabel } from "sigma/rendering";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";

export function drawEllipseNodeLabel<
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

export function drawEllipseNodeHover<
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
            radiusX = Math.max(data.size * aspect, size / 2) + PADDING,
            radiusY = Math.max(data.size, size / 2) + PADDING;

        const angleRadian = Math.asin(Math.min(1.0, boxHeight / 2 / radiusY));
        const xDeltaCoord = radiusX * Math.cos(angleRadian);

        context.beginPath();
        context.moveTo(data.x + xDeltaCoord, data.y + boxHeight / 2);
        context.lineTo(data.x + radiusX + boxWidth, data.y + boxHeight / 2);
        context.lineTo(data.x + radiusX + boxWidth, data.y - boxHeight / 2);
        context.lineTo(data.x + xDeltaCoord, data.y - boxHeight / 2);
        context.ellipse(data.x, data.y, radiusX, radiusY, 0, -angleRadian, -2 * Math.PI + angleRadian, true);
        context.closePath();
        context.fill();
    } else {
        context.beginPath();
        context.ellipse(data.x, data.y, data.size * aspect + PADDING, data.size + PADDING, 0, 0, Math.PI * 2);
        context.closePath();
        context.fill();
    }

    context.shadowOffsetX = 0;
    context.shadowOffsetY = 0;
    context.shadowBlur = 0;

    drawEllipseNodeLabel(context, data, settings);
}
