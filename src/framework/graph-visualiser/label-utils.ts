import { Attributes } from "graphology-types";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";

const MAX_LINES = 3;
const LINE_HEIGHT = 1.3;
const PADDING_X = 6;
const LABEL_FONT_SIZE = 12;

/**
 * Draw a node label centered inside the node.
 * Supports multi-line wrapping (up to 3 lines), explicit \n breaks,
 * and truncation with "…". Text color adapts to node background.
 */
export function drawCenteredNodeLabel<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
): void {
    if (!data.label) return;

    const rawW = (data as any).width ?? data.size;
    const rawH = (data as any).height ?? data.size;
    const zoom = data.size / Math.max(rawW, rawH);
    const screenHalfW = rawW * zoom;

    const fontSize = Math.min(settings.labelSize, LABEL_FONT_SIZE * zoom);
    if (fontSize < 3) return;

    context.font = `${settings.labelWeight} ${fontSize}px ${settings.labelFont}`;
    context.textAlign = "center";
    context.textBaseline = "middle";
    context.fillStyle = contrastColor((data as any)._originalColor ?? data.color);

    const maxWidth = screenHalfW * 2 - PADDING_X;
    const lines = wrapText(context, data.label, maxWidth);

    const lineH = fontSize * LINE_HEIGHT;
    const totalH = lines.length * lineH;
    const startY = data.y - totalH / 2 + lineH / 2;

    for (let i = 0; i < lines.length; i++) {
        context.fillText(lines[i], data.x, startY + i * lineH);
    }
}

/** Wrap text into lines that fit within maxWidth, respecting \n and limiting to MAX_LINES. */
function wrapText(context: CanvasRenderingContext2D, text: string, maxWidth: number): string[] {
    const paragraphs = text.split("\n");
    const lines: string[] = [];

    for (const para of paragraphs) {
        if (lines.length >= MAX_LINES) break;

        const words = para.split(/\s+/).filter(w => w.length > 0);
        if (words.length === 0) {
            lines.push("");
            continue;
        }

        let currentLine = words[0];
        for (let i = 1; i < words.length; i++) {
            if (lines.length >= MAX_LINES) break;

            const test = currentLine + " " + words[i];
            if (context.measureText(test).width <= maxWidth) {
                currentLine = test;
            } else {
                lines.push(currentLine);
                currentLine = words[i];
            }
        }
        if (lines.length < MAX_LINES) {
            lines.push(currentLine);
        }
    }

    // Truncate last line if it overflows
    if (lines.length > 0) {
        const last = lines.length - 1;
        lines[last] = truncateLine(context, lines[last], maxWidth);
    }

    return lines;
}

/** Truncate a single line with "…" if it exceeds maxWidth. */
function truncateLine(context: CanvasRenderingContext2D, line: string, maxWidth: number): string {
    if (context.measureText(line).width <= maxWidth) return line;
    while (line.length > 1 && context.measureText(line + "\u2026").width > maxWidth) {
        line = line.slice(0, -1);
    }
    return line + "\u2026";
}

/** Compute the zoom-scaled font size from any node's display data. */
export function zoomScaledFontSize(nodeData: Record<string, any>, maxSize: number): number {
    const rawW = nodeData["width"] ?? nodeData["size"];
    const rawH = nodeData["height"] ?? nodeData["size"];
    const zoom = nodeData["size"] / Math.max(rawW, rawH);
    return Math.min(maxSize, LABEL_FONT_SIZE * zoom);
}

/** Pick black or white text for maximum contrast against the node's fill color. */
function contrastColor(color: string): string {
    if (!color || color.length < 7) return "#fff";
    const hex = color.startsWith("#") ? color.slice(1) : color;
    const r = parseInt(hex.substring(0, 2), 16) / 255;
    const g = parseInt(hex.substring(2, 4), 16) / 255;
    const b = parseInt(hex.substring(4, 6), 16) / 255;
    const luminance = 0.299 * r + 0.587 * g + 0.114 * b;
    return luminance > 0.5 ? "#000" : "#fff";
}
