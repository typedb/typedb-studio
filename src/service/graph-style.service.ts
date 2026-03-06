import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { DataVertexKind } from "../framework/graph-visualiser/graph";
import { defaultEdgeLabelColors, defaultQueryStyleParameters } from "../framework/graph-visualiser/defaults";

export interface NodeStyle {
    color: string;
    borderColor: string;
    shape: string;
    width: number;
    height: number;
}

export type PartialNodeStyle = Partial<NodeStyle>;

const STORAGE_KEY = "typedb-studio-graph-styles";

const ALL_KINDS: DataVertexKind[] = [
    "entity", "relation", "attribute",
    "entityType", "relationType", "attributeType", "roleType",
    "value", "unavailable", "expression", "functionCall",
];

@Injectable({ providedIn: "root" })
export class GraphStyleService {

    private _kindStyles: Record<string, PartialNodeStyle> = {};
    private _typeStyles: Record<string, PartialNodeStyle> = {};
    private _edgeLabelColors: Record<string, string> = {};
    private _colorEdgesByConstraint = false;
    private _labelUseBorderColor = true;

    readonly styles$ = new BehaviorSubject<void>(undefined);

    constructor() {
        this.load();
    }

    getKindDefault(kind: DataVertexKind): NodeStyle {
        return {
            color: defaultQueryStyleParameters.vertex_colors[kind],
            borderColor: defaultQueryStyleParameters.vertex_border_colors[kind],
            shape: defaultQueryStyleParameters.vertex_shapes[kind],
            width: defaultQueryStyleParameters.vertex_widths[kind],
            height: defaultQueryStyleParameters.vertex_heights[kind],
        };
    }

    getKindStyle(kind: DataVertexKind): NodeStyle {
        const base = this.getKindDefault(kind);
        const override = this._kindStyles[kind];
        if (!override) return base;
        return {
            color: override.color ?? base.color,
            borderColor: override.borderColor ?? base.borderColor,
            shape: override.shape ?? base.shape,
            width: override.width ?? base.width,
            height: override.height ?? base.height,
        };
    }

    getEffectiveStyle(kind: DataVertexKind, typeLabel?: string): NodeStyle {
        const kindStyle = this.getKindStyle(kind);
        if (!typeLabel) return kindStyle;

        const typeOverride = this._typeStyles[typeLabel];
        if (!typeOverride) return kindStyle;

        return {
            color: typeOverride.color ?? kindStyle.color,
            borderColor: typeOverride.borderColor ?? kindStyle.borderColor,
            shape: typeOverride.shape ?? kindStyle.shape,
            width: typeOverride.width ?? kindStyle.width,
            height: typeOverride.height ?? kindStyle.height,
        };
    }

    setKindStyle(kind: DataVertexKind, style: PartialNodeStyle): void {
        this._kindStyles[kind] = { ...this._kindStyles[kind], ...style };
        this.save();
        this.styles$.next();
    }

    setTypeStyle(typeLabel: string, style: PartialNodeStyle): void {
        this._typeStyles[typeLabel] = { ...this._typeStyles[typeLabel], ...style };
        this.save();
        this.styles$.next();
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
            ?? defaultQueryStyleParameters.edge_color.hex();
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

    resetToDefaults(): void {
        this._kindStyles = {};
        this._typeStyles = {};
        this._edgeLabelColors = {};
        this._labelUseBorderColor = true;
        this._colorEdgesByConstraint = false;
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
            }
        } catch (e) {
            console.warn("Failed to load graph styles from localStorage:", e);
        }
    }
}
