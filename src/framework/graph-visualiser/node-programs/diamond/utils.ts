import { Attributes } from "graphology-types";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";
import { drawCenteredNodeLabel, drawExternalNodeLabel, getLabelsVisible } from "../../sigma-label-utils";

export function drawDiamondNodeLabel<
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
        drawCenteredNodeLabel(context, data, settings);
        return;
    }

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
