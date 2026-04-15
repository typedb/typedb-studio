import { Attributes } from "graphology-types";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";

const MAX_LINES = 3;
const LINE_HEIGHT = 1.3;
const PADDING_X = 6;
const LABEL_FONT_SIZE = 14;

let _useBorderColorForLabels = true;
let _labelsVisible = true;
let _showHoverLabel = true;

export function setUseBorderColorForLabels(value: boolean): void {
    _useBorderColorForLabels = value;
}

export function setLabelsVisible(value: boolean): void {
    _labelsVisible = value;
}

export function getLabelsVisible(): boolean {
    return _labelsVisible;
}

export function setShowHoverLabel(value: boolean): void {
    _showHoverLabel = value;
}

export function getShowHoverLabel(): boolean {
    return _showHoverLabel;
}

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
): boolean {
    if (!data.label || !_labelsVisible) return false;

    const rawW = (data as any).width ?? data.size;
    const rawH = (data as any).height ?? data.size;
    const zoom = data.size / Math.max(rawW, rawH);
    const screenHalfW = rawW * zoom;

    const fontSize = Math.min(settings.labelSize, LABEL_FONT_SIZE * zoom);
    if (fontSize < 3) return false;

    context.textAlign = "center";
    context.textBaseline = "middle";
    const borderColor = (data as any).borderColor;
    const labelColor = (_useBorderColorForLabels && borderColor)
        ? borderColor
        : contrastColor((data as any)._originalColor ?? data.color);
    const weight = labelColor === LABEL_DARK ? bumpWeight(settings.labelWeight) : settings.labelWeight;
    context.font = `${weight} ${fontSize}px ${settings.labelFont}`;
    context.fillStyle = labelColor;

    const maxWidth = screenHalfW * 2 - PADDING_X;
    const { lines, truncated } = wrapText(context, data.label, maxWidth);

    const lineH = fontSize * LINE_HEIGHT;
    const totalH = lines.length * lineH;
    const startY = data.y - totalH / 2 + lineH / 2;

    for (let i = 0; i < lines.length; i++) {
        context.fillText(lines[i], data.x, startY + i * lineH);
    }

    return truncated;
}

interface WrapResult {
    lines: string[];
    truncated: boolean;
}

/** Wrap text into lines that fit within maxWidth, respecting \n and limiting to MAX_LINES. */
function wrapText(context: CanvasRenderingContext2D, text: string, maxWidth: number): WrapResult {
    const paragraphs = text.split("\n");
    const lines: string[] = [];
    let truncated = false;

    for (const para of paragraphs) {
        if (lines.length >= MAX_LINES) { truncated = true; break; }

        const words = para.split(/\s+/).filter(w => w.length > 0);
        if (words.length === 0) {
            lines.push("");
            continue;
        }

        let currentLine = words[0];
        for (let i = 1; i < words.length; i++) {
            if (lines.length >= MAX_LINES) { truncated = true; break; }

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
        } else {
            truncated = true;
        }
    }

    // Truncate last line if it overflows
    if (lines.length > 0) {
        const last = lines.length - 1;
        const original = lines[last];
        lines[last] = truncateLine(context, original, maxWidth);
        if (lines[last] !== original) truncated = true;
    }

    return { lines, truncated };
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

/**
 * Draw a label to the right of the node (for hover when labels are hidden).
 * Draws a dark rounded background pill behind the text.
 */
export function drawExternalNodeLabel<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
): void {
    if (!data.label) return;

    const fontSize = settings.labelSize;
    const font = `${settings.labelWeight} ${fontSize}px ${settings.labelFont}`;
    context.font = font;

    const labelWidth = context.measureText(data.label).width;
    const x = data.x + data.size + 6;
    const y = data.y;
    const hPad = 5;
    const vPad = 3;

    // Background pill
    const bgX = x - hPad;
    const bgY = y - fontSize / 2 - vPad;
    const bgW = labelWidth + hPad * 2;
    const bgH = fontSize + vPad * 2;
    const bgR = 4;
    context.fillStyle = "rgba(0, 0, 0, 0.8)";
    context.beginPath();
    context.moveTo(bgX + bgR, bgY);
    context.arcTo(bgX + bgW, bgY, bgX + bgW, bgY + bgH, bgR);
    context.arcTo(bgX + bgW, bgY + bgH, bgX, bgY + bgH, bgR);
    context.arcTo(bgX, bgY + bgH, bgX, bgY, bgR);
    context.arcTo(bgX, bgY, bgX + bgW, bgY, bgR);
    context.closePath();
    context.fill();

    // Text
    context.fillStyle = "#d5ccff";
    context.textAlign = "left";
    context.textBaseline = "middle";
    context.fillText(data.label, x, y);
}

const LABEL_LIGHT = "#f3f3f3";
const LABEL_DARK = "#1A182A";

const NAMED_WEIGHTS: Record<string, number> = { normal: 400, bold: 700 };

/** Increase a CSS font-weight by 100 (e.g. "normal" → 500, "400" → 500). */
function bumpWeight(w: string): string {
    const n = NAMED_WEIGHTS[w] ?? parseInt(w, 10);
    return isNaN(n) ? w : String(Math.min(n + 100, 900));
}

/** Pick light or dark text for maximum contrast against the node's fill color. */
function contrastColor(color: string): string {
    if (!color || color.length < 7) return LABEL_LIGHT;
    const hex = color.startsWith("#") ? color.slice(1) : color;
    const r = parseInt(hex.substring(0, 2), 16) / 255;
    const g = parseInt(hex.substring(2, 4), 16) / 255;
    const b = parseInt(hex.substring(4, 6), 16) / 255;
    const luminance = 0.299 * r + 0.587 * g + 0.114 * b;
    return luminance > 0.5 ? LABEL_DARK : LABEL_LIGHT;
}
