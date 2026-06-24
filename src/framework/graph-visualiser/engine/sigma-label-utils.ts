import { Attributes } from "graphology-types";
import { Settings } from "sigma/settings";
import { NodeDisplayData, PartialButFor } from "sigma/types";

const MAX_LINES = 3;
const LINE_HEIGHT = 1.3;
const PADDING_X = 6;
const LABEL_FONT_SIZE = 14;
/** How far a node label may extend past the node's own width before it wraps
 *  or truncates (1.0 = stay inside the node). The shape label clips scale by
 *  the same factor so the overflowing text is actually visible. */
export const LABEL_OVERFLOW = 1.5;

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
interface LabelLayout {
    fontSize: number;
    font: string;
    color: string;
    lines: string[];
    truncated: boolean;
}

/**
 * Resolve everything needed to paint a node's label — font, colour, and the
 * (memoized) wrapped lines plus whether they overflow. Returns null when there
 * is nothing to draw (no label, labels off, or font too small to read), so
 * callers can bail before doing any canvas work. Sets `context.font` as a
 * side-effect because the wrap's `measureText` needs it on a cache miss.
 */
function computeLabelLayout<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
): LabelLayout | null {
    if (!data.label || !_labelsVisible) return null;

    const rawW = (data as any).width ?? data.size;
    const rawH = (data as any).height ?? data.size;
    const zoom = data.size / Math.max(rawW, rawH);
    const screenHalfW = rawW * zoom;

    const rawFontSize = Math.min(settings.labelSize, LABEL_FONT_SIZE * zoom);
    if (rawFontSize < 3) return null;
    // Quantize the font size to whole px. Re-rasterizing glyphs at a brand-new
    // size is the dominant paint cost whenever the camera ratio changes every
    // frame — manual zoom, and the per-tick auto-fit during the force sim. It's
    // exactly why *panning* (ratio fixed → size fixed → cached glyph atlas) is
    // smooth while zooming janks. Snapping to integer px lets consecutive frames
    // reuse one cached atlas instead of rebuilding it; labels then scale in ~1px
    // steps, imperceptible against the node bodies gliding continuously beneath.
    const fontSize = Math.round(rawFontSize);

    const borderColor = (data as any).borderColor;
    const labelColor = (_useBorderColorForLabels && borderColor)
        ? borderColor
        : contrastColor((data as any)._originalColor ?? data.color);
    const weight = labelColor === LABEL_DARK ? bumpWeight(settings.labelWeight) : settings.labelWeight;
    const font = `${weight} ${fontSize}px ${settings.labelFont}`;
    context.font = font;

    // First try to wrap inside the node itself (up to MAX_LINES lines). Only
    // fall back to the wider LABEL_OVERFLOW budget if even that wrap had to
    // truncate — so labels that fit comfortably across two lines stay inside
    // the node, while genuinely-too-long names still get the overflow room.
    const nodeWidth = screenHalfW * 2 - PADDING_X;
    const { lines, truncated } = wrapTextCached(context, data.label, nodeWidth, font);

    return { fontSize, font, color: labelColor, lines, truncated };
}

/** Paint pre-resolved label lines centered on the node. */
function paintLabelLines(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y">,
    layout: LabelLayout,
): void {
    context.textAlign = "center";
    context.textBaseline = "middle";
    context.font = layout.font;
    context.fillStyle = layout.color;

    const lineH = layout.fontSize * LINE_HEIGHT;
    const totalH = layout.lines.length * lineH;
    const startY = data.y - totalH / 2 + lineH / 2;

    for (let i = 0; i < layout.lines.length; i++) {
        context.fillText(layout.lines[i], data.x, startY + i * lineH);
    }
}

export function drawCenteredNodeLabel<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
): boolean {
    const layout = computeLabelLayout<N, E, G>(context, data, settings);
    if (!layout) return false;
    paintLabelLines(context, data, layout);
    return layout.truncated;
}

/**
 * Knock the label area out of the node fill, then draw the centered label.
 * A label that *overflows* the node is clipped to the node shape widened
 * horizontally by `LABEL_OVERFLOW`, so it can spill past the node's edges
 * (bounded) instead of being hard-clipped at the outline; `buildPath` traces
 * the node's base shape and we scale the canvas horizontally around the node
 * centre while building that clip. A label that fits inside the node needs no
 * clip at all and skips it — clipping is the dominant paint cost, so avoiding
 * it for the common (non-overflowing) case is the main perf lever here.
 */
export function drawClippedNodeLabel<
    N extends Attributes = Attributes,
    E extends Attributes = Attributes,
    G extends Attributes = Attributes,
>(
    context: CanvasRenderingContext2D,
    data: PartialButFor<NodeDisplayData, "x" | "y" | "size" | "label" | "color">,
    settings: Settings<N, E, G>,
    buildPath: (ctx: CanvasRenderingContext2D, d: typeof data) => void,
): void {
    context.save();
    // Erase from the labels canvas only the node's *own body* shape, so a node
    // drawn on top clears the labels strictly behind its body — not a wider
    // box. Using the widened (overflow) shape here would carve out a band 50%
    // larger than the node and wipe neighbouring nodes' labels inside it, which
    // reads as an invisible box obstructing other nodes (most visible while
    // dragging a node past its neighbours).
    buildPath(context, data);
    context.globalCompositeOperation = "destination-out";
    context.fillStyle = "#000";
    context.fill();
    context.globalCompositeOperation = "source-over";

    // The body-erase above is for occlusion (front nodes clear labels behind
    // them) and must run for every node regardless of its own label. The label
    // itself, and the clip that bounds it, are only needed when there's
    // something readable to draw — bail before either otherwise.
    const layout = computeLabelLayout<N, E, G>(context, data, settings);
    if (layout) {
        // The clip is the single most expensive op here, and it only exists to
        // bound an *overflowing* label's bleed past the node edge. A label that
        // fits inside the node draws identically with or without it — so clip
        // ONLY when the label actually overflowed. With paint-bound rendering
        // (clip dominates the profile) this removes the clip for the common
        // case while still showing every label, overflow ones included.
        if (layout.truncated) {
            // Clip the label text to a horizontally-widened shape so a slightly-
            // too-long label can still spill past the node edges (bounded)
            // instead of being hard-clipped at the outline.
            context.translate(data.x, data.y);
            context.scale(LABEL_OVERFLOW, 1);
            context.translate(-data.x, -data.y);
            buildPath(context, data);
            context.clip();
            // Undo just our horizontal scale (preserving any DPR transform sigma
            // set) so the label text itself isn't stretched.
            context.translate(data.x, data.y);
            context.scale(1 / LABEL_OVERFLOW, 1);
            context.translate(-data.x, -data.y);
        }
        paintLabelLines(context, data, layout);
    }
    context.restore();
}

interface WrapResult {
    lines: string[];
    truncated: boolean;
}

/**
 * Memoized wrap. Wrapping a label is pure for a given (text, width, font): a
 * force simulation only changes node x/y between frames, not the text, the node
 * width, or the font — yet the inline wrap re-ran `measureText` (slow, per word)
 * for every visible node on every tick, then a *second* full pass whenever the
 * inner wrap truncated (common now that labels carry attribute values). Keying a
 * cache on font+width+text collapses all of that to a single wrap per distinct
 * label per zoom level; subsequent frames are pure Map lookups. The
 * LABEL_OVERFLOW fallback is folded in so callers get identical results to the
 * old inline path.
 */
const wrapCache = new Map<string, WrapResult>();
const WRAP_CACHE_MAX = 4000;

function wrapTextCached(context: CanvasRenderingContext2D, text: string, nodeWidth: number, font: string): WrapResult {
    // Quantize the key so a continuous zoom — which nudges fontSize and
    // nodeWidth every animation frame — reuses one wrap across a run of frames
    // instead of missing the cache on every one. Rounding the font px to the
    // nearest integer and the width to the nearest 4px never changes where the
    // lines break (a sub-pixel delta can't move a word across a boundary), and
    // the glyphs are still drawn at the exact fontSize by fillText, so this is
    // visually identical while turning per-frame misses into hits during zoom.
    const fontKey = font.replace(/(\d+(?:\.\d+)?)px/, (_m, n) => `${Math.round(parseFloat(n))}px`);
    const key = `${fontKey}|${Math.round(nodeWidth / 4) * 4}|${text}`;
    const cached = wrapCache.get(key);
    if (cached) return cached;
    let { lines, truncated } = wrapText(context, text, nodeWidth);
    if (truncated) {
        ({ lines, truncated } = wrapText(context, text, nodeWidth * LABEL_OVERFLOW));
    }
    const result: WrapResult = { lines, truncated };
    // Cheap unbounded-growth guard: keys multiply with distinct labels × zoom
    // levels. At a fixed zoom (e.g. mid-simulation) the key set is stable and
    // tiny, so a hard clear on overflow costs at most one extra wrap per label.
    if (wrapCache.size >= WRAP_CACHE_MAX) wrapCache.clear();
    wrapCache.set(key, result);
    return result;
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
