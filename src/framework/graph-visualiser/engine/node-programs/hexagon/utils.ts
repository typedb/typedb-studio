import { Attributes } from "graphology-types";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";
import { drawCenteredNodeLabel, drawClippedNodeLabel, drawExternalNodeLabel, getLabelsVisible, getShowHoverLabel, zoomScaledFontSize } from "../../sigma-label-utils";

export function drawHexagonNodeLabel<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
): void {
    drawClippedNodeLabel<N, E, G>(context, data, settings, (ctx, d) => buildHexagonPath(ctx, d));
}

/** Trace a point-up regular hexagon: corners at top & bottom, and one corner
 *  on each of the upper/lower left & right (total 6). `pad` grows it for the
 *  hover highlight. */
function buildHexagonPath(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "color">,
    pad = 0,
): void {
    const rawW = (data as any).width ?? data.size;
    const rawH = (data as any).height ?? data.size;
    const scale = data.size / Math.max(rawW, rawH);
    // Point-up regular hexagon matching the shader: top/bottom corner at ±ry,
    // the left/right edges (apothem) at ±(cos30°·ry), and those side corners
    // at ±ry/2 vertically.
    const ry = rawH * scale + pad;
    const rx = ry * 0.8660254;          // apothem = cos(30°) · half-height
    const sideY = ry * 0.5;

    const x = data.x;
    const y = data.y;
    context.beginPath();
    context.moveTo(x, y - ry);          // top corner
    context.lineTo(x + rx, y - sideY);  // upper-right
    context.lineTo(x + rx, y + sideY);  // lower-right
    context.lineTo(x, y + ry);          // bottom corner
    context.lineTo(x - rx, y + sideY);  // lower-left
    context.lineTo(x - rx, y - sideY);  // upper-left
    context.closePath();
}

export function drawHexagonNodeHover<
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

    context.fillStyle = "rgba(255, 255, 255, 0.15)";
    buildHexagonPath(context, data, 2);
    context.fill();

    drawExternalNodeLabel(context, data, settings);
}
