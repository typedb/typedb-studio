import { Injectable, OnDestroy } from "@angular/core";
import { BehaviorSubject, Subscription } from "rxjs";
import { VertexKind } from "@typedb/graph-utils";
import { GraphStyles, defaultEdgeLabelColors, defaultQueryStyleParams } from "../framework/graph-visualiser/engine/styles";
import { ThemeService } from "./theme.service";

export interface NodeStyle {
    color: string;
    fillColor: string;
    shape: string;
    width: number;
    height: number;
}

export type PartialNodeStyle = Partial<Pick<NodeStyle, "color" | "shape" | "width" | "height">>;

const STORAGE_KEY = "typedb-studio-graph-styles";
const CUSTOM_PRESETS_KEY = "typedb-studio-custom-presets";

export type GraphBackgroundType = "default" | "solid" | "gradient" | "grid" | "dots" | "party";

export interface GraphBackground {
    type: GraphBackgroundType;
    color1: string;
    color2: string;
    gradientAngle: number;
}

export const DEFAULT_BACKGROUND: GraphBackground = {
    type: "default",
    color1: "#0e0e0e",
    color2: "#1A182A",
    gradientAngle: 180,
};

export interface BackgroundCSS {
    color: string;
    image: string;
    size: string;
}

export function buildBackgroundCSS(bg: GraphBackground): BackgroundCSS {
    switch (bg.type) {
        case "default":
            return { color: "var(--theme-black)", image: "none", size: "" };
        case "solid":
            return { color: bg.color1, image: "none", size: "" };
        case "gradient":
            return { color: bg.color1, image: `linear-gradient(${bg.gradientAngle}deg, ${bg.color1}, ${bg.color2})`, size: "" };
        case "grid":
            return {
                color: bg.color1,
                image: `repeating-linear-gradient(0deg, transparent, transparent 29px, ${bg.color2} 29px, ${bg.color2} 30px), repeating-linear-gradient(90deg, transparent, transparent 29px, ${bg.color2} 29px, ${bg.color2} 30px)`,
                size: "",
            };
        case "dots":
            return { color: bg.color1, image: `radial-gradient(${bg.color2} 1px, transparent 1px)`, size: "20px 20px" };
        case "party":
            return { color: bg.color1, image: "none", size: "" };
        default:
            return { color: bg.color1, image: "none", size: "" };
    }
}

export interface CustomPreset {
    name: string;
    description: string;
    kindStyles: Record<string, PartialNodeStyle>;
    typeStyles: Record<string, PartialNodeStyle>;
    edgeLabelColors: Record<string, string>;
    colorEdgesByConstraint: boolean;
    labelColorMode?: "auto" | "border" | "fixed";
    labelUseBorderColor?: boolean; // legacy, for backwards compat
    labelsVisible: boolean;
    showHoverLabel: boolean;
    degreeScaling: boolean;
    background: GraphBackground;
}

function parseHex(hex: string): [number, number, number] {
    const h = hex.startsWith("#") ? hex.slice(1) : hex;
    return [parseInt(h.substring(0, 2), 16), parseInt(h.substring(2, 4), 16), parseInt(h.substring(4, 6), 16)];
}

function blendColors(foreHex: string, bgHex: string, amount: number): string {
    const [fr, fg, fb] = parseHex(foreHex);
    const [br, bg, bb] = parseHex(bgHex);
    const r = Math.round(fr * amount + br * (1 - amount));
    const g = Math.round(fg * amount + bg * (1 - amount));
    const b = Math.round(fb * amount + bb * (1 - amount));
    return `#${r.toString(16).padStart(2, "0")}${g.toString(16).padStart(2, "0")}${b.toString(16).padStart(2, "0")}`;
}

/** Migrate saved styles from the old borderColor+color model to the single color model. */
function migrateStyles(styles: Record<string, any>): Record<string, PartialNodeStyle> {
    const result: Record<string, PartialNodeStyle> = {};
    for (const [key, val] of Object.entries(styles)) {
        const { borderColor, color, ...rest } = val as any;
        result[key] = { ...rest, color: borderColor ?? color };
    }
    return result;
}

const ALL_KINDS: VertexKind[] = [
    "entity", "relation", "attribute",
    "entityType", "relationType", "attributeType", "roleType",
    "value", "unavailable",
];

@Injectable({ providedIn: "root" })
export class GraphStyleService implements OnDestroy {

    private _kindStyles: Record<string, PartialNodeStyle> = {};
    private _typeStyles: Record<string, PartialNodeStyle> = {};
    private _edgeLabelColors: Record<string, string> = {};
    private _colorEdgesByConstraint = false;
    private _labelColorMode: "auto" | "border" | "fixed" = "auto";
    private _highlightedKinds = new Set<VertexKind>();
    private _highlightedTypes = new Set<string>();
    private _highlightedEdges = new Set<string>();
    private _activePreset: string | null = null;
    private _labelsVisible = true;
    private _showHoverLabel = true;
    private _degreeScaling = false;
    private _fillOpacity = 0.25;
    private _background: GraphBackground = { ...DEFAULT_BACKGROUND };

    private _customPresets: CustomPreset[] = [];

    readonly styles$ = new BehaviorSubject<void>(undefined);
    private themeSubscription: Subscription;

    constructor(private themeService: ThemeService) {
        this.load();
        this.loadCustomPresets();
        this.themeSubscription = this.themeService.effectiveTheme$.subscribe(() => {
            if (this._background.type === "default") {
                this.styles$.next();
            }
        });
    }

    ngOnDestroy() {
        this.themeSubscription.unsubscribe();
    }

    get effectiveBackgroundHex(): string {
        const bg = this._background;
        if (bg.type === "default") {
            return getComputedStyle(document.documentElement).getPropertyValue("--theme-black").trim();
        }
        return bg.color1; // solid, gradient, grid, dots, party — color1 is always the base
    }

    private deriveFill(color: string): string {
        return blendColors(color, this.effectiveBackgroundHex, this._fillOpacity);
    }

    getKindDefault(kind: VertexKind): NodeStyle {
        const color = defaultQueryStyleParams.vertexBorderColors[kind];
        return {
            color,
            fillColor: this.deriveFill(color),
            shape: defaultQueryStyleParams.vertexShapes[kind],
            width: defaultQueryStyleParams.vertexWidths[kind],
            height: defaultQueryStyleParams.vertexHeights[kind],
        };
    }

    getKindStyle(kind: VertexKind): NodeStyle {
        const base = this.getKindDefault(kind);
        const override = this._kindStyles[kind];
        const color = override?.color ?? base.color;
        return {
            color,
            fillColor: this.deriveFill(color),
            shape: override?.shape ?? base.shape,
            width: override?.width ?? base.width,
            height: override?.height ?? base.height,
        };
    }

    resolveNodeStyle(kind: VertexKind, typeLabel?: string): NodeStyle {
        const kindStyle = this.getKindStyle(kind);
        if (!typeLabel) return kindStyle;

        const typeOverride = this._typeStyles[typeLabel];
        if (!typeOverride) return kindStyle;

        const color = typeOverride.color ?? kindStyle.color;
        return {
            color,
            fillColor: this.deriveFill(color),
            shape: typeOverride.shape ?? kindStyle.shape,
            width: typeOverride.width ?? kindStyle.width,
            height: typeOverride.height ?? kindStyle.height,
        };
    }

    setKindStyle(kind: VertexKind, style: PartialNodeStyle): void {
        this._kindStyles[kind] = { ...this._kindStyles[kind], ...style };
        this.save();
        this.styles$.next();
    }

    setTypeStyle(typeLabel: string, style: PartialNodeStyle): void {
        this._typeStyles[typeLabel] = { ...this._typeStyles[typeLabel], ...style };
        this.save();
        this.styles$.next();
    }

    removeKindStyle(kind: VertexKind): void {
        delete this._kindStyles[kind];
        this.save();
        this.styles$.next();
    }

    hasKindOverride(kind: VertexKind): boolean {
        const override = this._kindStyles[kind];
        return !!override && Object.keys(override).length > 0;
    }

    removeTypeStyle(typeLabel: string): void {
        delete this._typeStyles[typeLabel];
        this.save();
        this.styles$.next();
    }

    get kindStyles(): Record<string, PartialNodeStyle> {
        return this._kindStyles;
    }

    get typeStyles(): Record<string, PartialNodeStyle> {
        return this._typeStyles;
    }

    get colorEdgesByConstraint(): boolean {
        return this._colorEdgesByConstraint;
    }

    set colorEdgesByConstraint(value: boolean) {
        this._colorEdgesByConstraint = value;
        this.save();
    }

    get labelColorMode(): "auto" | "border" | "fixed" {
        return this._labelColorMode;
    }

    set labelColorMode(value: "auto" | "border" | "fixed") {
        this._labelColorMode = value;
        this.save();
        this.styles$.next();
    }

    /** Resolves "auto" to the effective mode based on background luminance. */
    get labelUseBorderColor(): boolean {
        if (this._labelColorMode === "auto") {
            const hex = this.effectiveBackgroundHex;
            const h = hex.startsWith("#") ? hex.slice(1) : hex;
            const r = parseInt(h.substring(0, 2), 16);
            const g = parseInt(h.substring(2, 4), 16);
            const b = parseInt(h.substring(4, 6), 16);
            const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
            return luminance < 0.5; // dark bg → border color; light bg → fixed
        }
        return this._labelColorMode === "border";
    }

    // -- Edge label colors --

    getEdgeLabelColor(tag: string): string {
        return this._edgeLabelColors[tag]
            ?? defaultEdgeLabelColors[tag]
            ?? defaultQueryStyleParams.edgeColor.hex();
    }

    setEdgeLabelColor(tag: string, color: string): void {
        this._edgeLabelColors[tag] = color;
        this.save();
        this.styles$.next();
    }

    removeEdgeLabelColor(tag: string): void {
        delete this._edgeLabelColors[tag];
        this.save();
        this.styles$.next();
    }

    get edgeLabelColors(): Record<string, string> {
        return this._edgeLabelColors;
    }

    getResolvedEdgeLabelColors(): Record<string, string> {
        return { ...defaultEdgeLabelColors, ...this._edgeLabelColors };
    }

    toGraphStyles(): GraphStyles {
        const vertexColors: Record<string, string> = {} as any;
        const vertexBorderColors: Record<string, string> = {} as any;
        const vertexShapes: Record<string, string> = {} as any;
        const vertexWidths: Record<string, number> = {} as any;
        const vertexHeights: Record<string, number> = {} as any;
        for (const kind of ALL_KINDS) {
            const style = this.getKindStyle(kind);
            vertexColors[kind] = style.fillColor;
            vertexBorderColors[kind] = style.color;
            vertexShapes[kind] = style.shape;
            vertexWidths[kind] = style.width;
            vertexHeights[kind] = style.height;
        }

        const vertexTypeColors: Record<string, string> = {};
        const vertexTypeBorderColors: Record<string, string> = {};
        const vertexTypeShapes: Record<string, string> = {};
        const vertexTypeWidths: Record<string, number> = {};
        const vertexTypeHeights: Record<string, number> = {};
        for (const [typeLabel, override] of Object.entries(this._typeStyles)) {
            const color = override.color;
            if (color) {
                vertexTypeBorderColors[typeLabel] = color;
                vertexTypeColors[typeLabel] = this.deriveFill(color);
            }
            if (override.shape) vertexTypeShapes[typeLabel] = override.shape;
            if (override.width) vertexTypeWidths[typeLabel] = override.width;
            if (override.height) vertexTypeHeights[typeLabel] = override.height;
        }

        return {
            ...defaultQueryStyleParams,
            vertexColors: vertexColors as any,
            vertexBorderColors: vertexBorderColors as any,
            vertexShapes: vertexShapes as any,
            vertexWidths: vertexWidths as any,
            vertexHeights: vertexHeights as any,
            vertexTypeColors: Object.keys(vertexTypeColors).length ? vertexTypeColors : undefined,
            vertexTypeBorderColors: Object.keys(vertexTypeBorderColors).length ? vertexTypeBorderColors : undefined,
            vertexTypeShapes: Object.keys(vertexTypeShapes).length ? vertexTypeShapes : undefined,
            vertexTypeWidths: Object.keys(vertexTypeWidths).length ? vertexTypeWidths : undefined,
            vertexTypeHeights: Object.keys(vertexTypeHeights).length ? vertexTypeHeights : undefined,
            edgeLabelColors: this.getResolvedEdgeLabelColors(),
        };
    }

    // -- Highlights --

    get highlightedKinds(): Set<VertexKind> { return this._highlightedKinds; }
    get highlightedTypes(): Set<string> { return this._highlightedTypes; }
    get highlightedEdges(): Set<string> { return this._highlightedEdges; }

    isHighlightActive(): boolean {
        return this._highlightedKinds.size > 0 || this._highlightedTypes.size > 0 || this._highlightedEdges.size > 0;
    }

    toggleHighlightKind(kind: VertexKind): void {
        if (this._highlightedKinds.has(kind)) this._highlightedKinds.delete(kind);
        else this._highlightedKinds.add(kind);
        this.styles$.next();
    }

    toggleHighlightType(typeLabel: string): void {
        if (this._highlightedTypes.has(typeLabel)) this._highlightedTypes.delete(typeLabel);
        else this._highlightedTypes.add(typeLabel);
        this.styles$.next();
    }

    toggleHighlightEdge(tag: string): void {
        if (this._highlightedEdges.has(tag)) this._highlightedEdges.delete(tag);
        else this._highlightedEdges.add(tag);
        this.styles$.next();
    }

    clearHighlights(): void {
        this._highlightedKinds.clear();
        this._highlightedTypes.clear();
        this._highlightedEdges.clear();
        this.styles$.next();
    }

    shouldHighlightNode(kind: VertexKind, typeLabel?: string): boolean {
        if (this._highlightedKinds.size === 0 && this._highlightedTypes.size === 0) return true;
        if (this._highlightedKinds.has(kind)) return true;
        if (typeLabel && this._highlightedTypes.has(typeLabel)) return true;
        return false;
    }

    shouldHighlightEdge(tag: string): boolean {
        if (this._highlightedEdges.size === 0) return true;
        if (this._highlightedEdges.has(tag)) return true;
        return false;
    }

    // -- Presets --

    get activePreset(): string | null { return this._activePreset; }

    set activePreset(value: string | null) {
        this._activePreset = value;
        this.save();
    }

    get background(): GraphBackground { return this._background; }

    set background(value: GraphBackground) {
        this._background = value;
        this.save();
        this.styles$.next();
    }

    updateBackground(partial: Partial<GraphBackground>): void {
        this._background = { ...this._background, ...partial };
        this.save();
        this.styles$.next();
    }

    get fillOpacity(): number { return this._fillOpacity; }

    set fillOpacity(value: number) {
        this._fillOpacity = Math.max(0, Math.min(1, value));
        this.save();
        this.styles$.next();
    }

    get degreeScaling(): boolean { return this._degreeScaling; }

    set degreeScaling(value: boolean) {
        this._degreeScaling = value;
        this.save();
        this.styles$.next();
    }

    get labelsVisible(): boolean { return this._labelsVisible; }

    set labelsVisible(value: boolean) {
        this._labelsVisible = value;
        this.save();
        this.styles$.next();
    }

    get showHoverLabel(): boolean { return this._showHoverLabel; }

    set showHoverLabel(value: boolean) {
        this._showHoverLabel = value;
        this.save();
        this.styles$.next();
    }

    applyStructurePreset(): void {
        for (const kind of ALL_KINDS) {
            const color = defaultQueryStyleParams.vertexBorderColors[kind];
            this._kindStyles[kind] = { color, shape: "ellipse", width: 6, height: 6 };
        }
        this._labelColorMode = "auto";
        this._labelsVisible = false;
        this._showHoverLabel = true;
        this._degreeScaling = true;
        this._activePreset = "structure";
        this.save();
        this.styles$.next();
    }

    applyUniformPreset(): void {
        for (const kind of ALL_KINDS) {
            const color = defaultQueryStyleParams.vertexBorderColors[kind];
            this._kindStyles[kind] = { color, shape: "rounded-rect", width: 56, height: 24 };
        }
        this._labelColorMode = "auto";
        this._labelsVisible = true;
        this._showHoverLabel = true;
        this._degreeScaling = false;
        this._activePreset = "uniform";
        this.save();
        this.styles$.next();
    }

    applyClassicPreset(): void {
        for (const kind of ALL_KINDS) {
            const color = defaultQueryStyleParams.vertexBorderColors[kind];
            const shape = defaultQueryStyleParams.vertexShapes[kind];
            const width = defaultQueryStyleParams.vertexWidths[kind];
            const height = defaultQueryStyleParams.vertexHeights[kind];
            this._kindStyles[kind] = { color, shape, width, height };
        }
        this._labelColorMode = "fixed";
        this._labelsVisible = true;
        this._showHoverLabel = true;
        this._degreeScaling = false;
        this._activePreset = "classic";
        this.save();
        this.styles$.next();
    }

    applyGrayscalePreset(): void {
        const grays: Record<string, string> = {
            entity: "#d6d6d6",
            relation: "#b8b8b8",
            attribute: "#8a8a8a",
            entityType: "#d6d6d6",
            relationType: "#b8b8b8",
            attributeType: "#8a8a8a",
            roleType: "#4a4a4a",
            value: "#4a4a4a",
            unavailable: "#404040",
        };
        for (const kind of ALL_KINDS) {
            const color = grays[kind] ?? "#5e5e5e";
            const shape = defaultQueryStyleParams.vertexShapes[kind];
            const width = defaultQueryStyleParams.vertexWidths[kind];
            const height = defaultQueryStyleParams.vertexHeights[kind];
            this._kindStyles[kind] = { color, shape, width, height };
        }
        this._labelColorMode = "fixed";
        this._labelsVisible = true;
        this._showHoverLabel = true;
        this._degreeScaling = false;
        this._activePreset = "grayscale";
        this.save();
        this.styles$.next();
    }

    applyDefaultPreset(): void {
        this.resetToDefaults();
        this._labelsVisible = true;
        this._activePreset = "default";
        this.save();
        this.styles$.next();
    }

    resetToDefaults(): void {
        this._kindStyles = {};
        this._typeStyles = {};
        this._edgeLabelColors = {};
        this._labelColorMode = "auto";
        this._colorEdgesByConstraint = false;
        this._labelsVisible = true;
        this._showHoverLabel = true;
        this._degreeScaling = false;
        this._activePreset = null;
        this.save();
        this.styles$.next();
    }

    // -- Custom presets --

    get customPresets(): readonly CustomPreset[] {
        return this._customPresets;
    }

    saveCustomPreset(name: string, description: string): void {
        const preset: CustomPreset = {
            name,
            description,
            kindStyles: structuredClone(this._kindStyles),
            typeStyles: structuredClone(this._typeStyles),
            edgeLabelColors: { ...this._edgeLabelColors },
            colorEdgesByConstraint: this._colorEdgesByConstraint,
            labelColorMode: this._labelColorMode,
            labelsVisible: this._labelsVisible,
            showHoverLabel: this._showHoverLabel,
            degreeScaling: this._degreeScaling,
            background: { ...this._background },
        };
        const idx = this._customPresets.findIndex(p => p.name === name);
        if (idx >= 0) {
            this._customPresets[idx] = preset;
        } else {
            this._customPresets.push(preset);
        }
        this.saveCustomPresets();
    }

    applyCustomPreset(name: string): void {
        const preset = this._customPresets.find(p => p.name === name);
        if (!preset) return;
        this._kindStyles = structuredClone(preset.kindStyles);
        this._typeStyles = structuredClone(preset.typeStyles);
        this._edgeLabelColors = { ...preset.edgeLabelColors };
        this._colorEdgesByConstraint = preset.colorEdgesByConstraint;
        this._labelColorMode = preset.labelColorMode ?? (preset.labelUseBorderColor ? "auto" : "fixed");
        this._labelsVisible = preset.labelsVisible;
        this._showHoverLabel = preset.showHoverLabel;
        this._degreeScaling = preset.degreeScaling;
        this._activePreset = `custom:${name}`;
        this.save();
        this.styles$.next();
    }

    renameCustomPreset(oldName: string, newName: string, newDescription: string): void {
        const preset = this._customPresets.find(p => p.name === oldName);
        if (!preset) return;
        preset.name = newName;
        preset.description = newDescription;
        if (this._activePreset === `custom:${oldName}`) {
            this._activePreset = `custom:${newName}`;
            this.save();
        }
        this.saveCustomPresets();
        this.styles$.next();
    }

    deleteCustomPreset(name: string): void {
        this._customPresets = this._customPresets.filter(p => p.name !== name);
        if (this._activePreset === `custom:${name}`) {
            this._activePreset = null;
            this.save();
        }
        this.saveCustomPresets();
        this.styles$.next();
    }

    private saveCustomPresets(): void {
        try {
            localStorage.setItem(CUSTOM_PRESETS_KEY, JSON.stringify(this._customPresets));
        } catch (e) {
            console.warn("Failed to save custom presets to localStorage:", e);
        }
    }

    private loadCustomPresets(): void {
        try {
            const raw = localStorage.getItem(CUSTOM_PRESETS_KEY);
            if (raw) {
                this._customPresets = JSON.parse(raw) ?? [];
            }
        } catch (e) {
            console.warn("Failed to load custom presets from localStorage:", e);
        }
    }

    private save(): void {
        try {
            const data = {
                kindStyles: this._kindStyles,
                typeStyles: this._typeStyles,
                edgeLabelColors: this._edgeLabelColors,
                colorEdgesByConstraint: this._colorEdgesByConstraint,
                labelColorMode: this._labelColorMode,
                activePreset: this._activePreset,
                labelsVisible: this._labelsVisible,
                showHoverLabel: this._showHoverLabel,
                degreeScaling: this._degreeScaling,
                fillOpacity: this._fillOpacity,
                background: this._background,
            };
            localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
        } catch (e) {
            console.warn("Failed to save graph styles to localStorage:", e);
        }
    }

    private load(): void {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            if (raw) {
                const data = JSON.parse(raw);
                this._kindStyles = migrateStyles(data.kindStyles ?? {});
                this._typeStyles = migrateStyles(data.typeStyles ?? {});
                this._edgeLabelColors = data.edgeLabelColors ?? {};
                this._colorEdgesByConstraint = data.colorEdgesByConstraint ?? false;
                this._labelColorMode = data.labelColorMode ?? (data.labelUseBorderColor === false ? "fixed" : "auto");
                this._activePreset = data.activePreset ?? null;
                this._labelsVisible = data.labelsVisible ?? true;
                this._showHoverLabel = data.showHoverLabel ?? true;
                this._degreeScaling = data.degreeScaling ?? false;
                this._fillOpacity = data.fillOpacity ?? 0.25;
                if (data.background) this._background = { ...DEFAULT_BACKGROUND, ...data.background };
            }
        } catch (e) {
            console.warn("Failed to load graph styles from localStorage:", e);
        }
    }
}
