import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { VertexKind } from "@typedb/graph-utils";
import { GraphStyles, defaultEdgeLabelColors, defaultQueryStyleParams } from "../framework/graph-visualiser/styles";

export interface NodeStyle {
    color: string;
    borderColor: string;
    shape: string;
    width: number;
    height: number;
}

export type PartialNodeStyle = Partial<NodeStyle>;

const STORAGE_KEY = "typedb-studio-graph-styles";

function deriveFillFromBorder(borderHex: string): string {
    const hex = borderHex.startsWith("#") ? borderHex.slice(1) : borderHex;
    const r = Math.round(parseInt(hex.substring(0, 2), 16) * 0.25);
    const g = Math.round(parseInt(hex.substring(2, 4), 16) * 0.25);
    const b = Math.round(parseInt(hex.substring(4, 6), 16) * 0.25);
    return `#${r.toString(16).padStart(2, "0")}${g.toString(16).padStart(2, "0")}${b.toString(16).padStart(2, "0")}`;
}

const ALL_KINDS: VertexKind[] = [
    "entity", "relation", "attribute",
    "entityType", "relationType", "attributeType", "roleType",
    "value", "unavailable",
];

@Injectable({ providedIn: "root" })
export class GraphStyleService {

    private _kindStyles: Record<string, PartialNodeStyle> = {};
    private _typeStyles: Record<string, PartialNodeStyle> = {};
    private _edgeLabelColors: Record<string, string> = {};
    private _colorEdgesByConstraint = false;
    private _labelUseBorderColor = true;
    private _highlightedKinds = new Set<VertexKind>();
    private _highlightedTypes = new Set<string>();
    private _highlightedEdges = new Set<string>();
    private _activePreset: string | null = null;
    private _labelsVisible = true;
    private _degreeScaling = false;

    readonly styles$ = new BehaviorSubject<void>(undefined);

    constructor() {
        this.load();
    }

    getKindDefault(kind: VertexKind): NodeStyle {
        return {
            color: defaultQueryStyleParams.vertexColors[kind],
            borderColor: defaultQueryStyleParams.vertexBorderColors[kind],
            shape: defaultQueryStyleParams.vertexShapes[kind],
            width: defaultQueryStyleParams.vertexWidths[kind],
            height: defaultQueryStyleParams.vertexHeights[kind],
        };
    }

    getKindStyle(kind: VertexKind): NodeStyle {
        const base = this.getKindDefault(kind);
        const override = this._kindStyles[kind];
        const borderColor = override?.borderColor ?? base.borderColor;
        return {
            color: override?.color ?? deriveFillFromBorder(borderColor),
            borderColor,
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

        const borderColor = typeOverride.borderColor ?? kindStyle.borderColor;
        return {
            color: typeOverride.color ?? deriveFillFromBorder(borderColor),
            borderColor,
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

    get labelUseBorderColor(): boolean {
        return this._labelUseBorderColor;
    }

    set labelUseBorderColor(value: boolean) {
        this._labelUseBorderColor = value;
        this.save();
        this.styles$.next();
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
            vertexColors[kind] = style.color;
            vertexBorderColors[kind] = style.borderColor;
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
            if (override.color) vertexTypeColors[typeLabel] = override.color;
            if (override.borderColor) vertexTypeBorderColors[typeLabel] = override.borderColor;
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
        this.save();
        this.styles$.next();
    }

    toggleHighlightType(typeLabel: string): void {
        if (this._highlightedTypes.has(typeLabel)) this._highlightedTypes.delete(typeLabel);
        else this._highlightedTypes.add(typeLabel);
        this.save();
        this.styles$.next();
    }

    toggleHighlightEdge(tag: string): void {
        if (this._highlightedEdges.has(tag)) this._highlightedEdges.delete(tag);
        else this._highlightedEdges.add(tag);
        this.save();
        this.styles$.next();
    }

    clearHighlights(): void {
        this._highlightedKinds.clear();
        this._highlightedTypes.clear();
        this._highlightedEdges.clear();
        this.save();
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

    get degreeScaling(): boolean { return this._degreeScaling; }

    set degreeScaling(value: boolean) {
        this._degreeScaling = value;
        this.save();
        this.styles$.next();
    }

    get structureMode(): boolean { return this._activePreset === "structure"; }

    get labelsVisible(): boolean { return this._labelsVisible; }

    set labelsVisible(value: boolean) {
        this._labelsVisible = value;
        this.save();
        this.styles$.next();
    }

    applyStructurePreset(): void {
        for (const kind of ALL_KINDS) {
            this._kindStyles[kind] = { ...this._kindStyles[kind], shape: "ellipse", width: 6, height: 6 };
        }
        this._labelsVisible = false;
        this._degreeScaling = true;
        this._activePreset = "structure";
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
        this._labelUseBorderColor = true;
        this._colorEdgesByConstraint = false;
        this._labelsVisible = true;
        this._degreeScaling = false;
        this._highlightedKinds.clear();
        this._highlightedTypes.clear();
        this._highlightedEdges.clear();
        this._activePreset = null;
        this.save();
        this.styles$.next();
    }

    private save(): void {
        try {
            const data = {
                kindStyles: this._kindStyles,
                typeStyles: this._typeStyles,
                edgeLabelColors: this._edgeLabelColors,
                colorEdgesByConstraint: this._colorEdgesByConstraint,
                labelUseBorderColor: this._labelUseBorderColor,
                highlightedKinds: Array.from(this._highlightedKinds),
                highlightedTypes: Array.from(this._highlightedTypes),
                highlightedEdges: Array.from(this._highlightedEdges),
                activePreset: this._activePreset,
                labelsVisible: this._labelsVisible,
                degreeScaling: this._degreeScaling,
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
                this._kindStyles = data.kindStyles ?? {};
                this._typeStyles = data.typeStyles ?? {};
                this._edgeLabelColors = data.edgeLabelColors ?? {};
                this._colorEdgesByConstraint = data.colorEdgesByConstraint ?? false;
                this._labelUseBorderColor = data.labelUseBorderColor ?? true;
                this._highlightedKinds = new Set(data.highlightedKinds ?? []);
                this._highlightedTypes = new Set(data.highlightedTypes ?? []);
                this._highlightedEdges = new Set(data.highlightedEdges ?? []);
                this._activePreset = data.activePreset ?? null;
                this._labelsVisible = data.labelsVisible ?? true;
                this._degreeScaling = data.degreeScaling ?? false;
            }
        } catch (e) {
            console.warn("Failed to load graph styles from localStorage:", e);
        }
    }
}
