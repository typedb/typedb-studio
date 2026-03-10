import { Component, inject, Input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { ColorPickerDirective } from "ngx-color-picker";
import { GraphStyleService } from "../../service/graph-style.service";
import { GraphVisualiser } from "../graph-visualiser";
import { DataVertex, DataVertexKind } from "../graph-visualiser/logical-graph";

interface KindRow {
    kind: DataVertexKind;
    label: string;
}

interface TypeRow {
    typeLabel: string;
    kind: DataVertexKind;
}

interface EdgeLabelRow {
    tag: string;
    displayLabel: string;
}

const DISPLAY_EDGE_LABELS: EdgeLabelRow[] = [
    { tag: "has", displayLabel: "has" },
    { tag: "links", displayLabel: "links" },
    { tag: "isa", displayLabel: "isa" },
    { tag: "sub", displayLabel: "sub" },
    { tag: "owns", displayLabel: "owns" },
    { tag: "relates", displayLabel: "relates" },
    { tag: "plays", displayLabel: "plays" },
];

const DISPLAY_KINDS: KindRow[] = [
    { kind: "entity", label: "Entity" },
    { kind: "relation", label: "Relation" },
    { kind: "attribute", label: "Attribute" },
    { kind: "entityType", label: "Entity Type" },
    { kind: "relationType", label: "Relation Type" },
    { kind: "attributeType", label: "Attribute Type" },
    { kind: "roleType", label: "Role Type" },
    { kind: "value", label: "Value" },
];

export const AVAILABLE_SHAPES = [
    { value: "rounded-rect", label: "Rectangle" },
    { value: "diamond", label: "Diamond" },
    { value: "ellipse", label: "Ellipse" },
];

@Component({
    selector: "ts-graph-customisation-panel",
    templateUrl: "graph-customisation-panel.component.html",
    styleUrls: ["graph-customisation-panel.component.scss"],
    imports: [
        CommonModule, FormsModule,
        MatButtonModule, MatSelectModule, MatTooltipModule,
        MatFormFieldModule, MatInputModule, MatSlideToggleModule,
        ColorPickerDirective,
    ],
})
export class GraphCustomisationPanelComponent {

    @Input() visualiser: GraphVisualiser | null = null;

    styleService = inject(GraphStyleService);
    isOpen = false;
    activeTab: "kind" | "type" | "edge" = "kind";

    readonly displayKinds = DISPLAY_KINDS;
    readonly shapes = AVAILABLE_SHAPES;
    readonly edgeLabels = DISPLAY_EDGE_LABELS;

    discoveredTypes: TypeRow[] = [];

    toggle(): void {
        this.isOpen = !this.isOpen;
        if (this.isOpen) {
            this.refreshDiscoveredTypes();
        }
    }

    refreshDiscoveredTypes(): void {
        if (!this.visualiser) return;
        const typeMap = new Map<string, DataVertexKind>();
        this.visualiser.graph.nodes().forEach(nodeKey => {
            const attrs = this.visualiser!.graph.getNodeAttributes(nodeKey);
            const concept = attrs.metadata.concept;
            if ("type" in concept && concept.type && "label" in concept.type) {
                typeMap.set(concept.type.label, concept.kind);
            } else if ("label" in concept && (concept as DataVertex).kind !== "unavailable"
                       && (concept as DataVertex).kind !== "expression" && (concept as DataVertex).kind !== "functionCall") {
                typeMap.set(concept.label, concept.kind);
            }
        });
        this.discoveredTypes = Array.from(typeMap.entries())
            .map(([typeLabel, kind]) => ({ typeLabel, kind }))
            .sort((a, b) => a.typeLabel.localeCompare(b.typeLabel));
    }

    // -- Kind getters --

    getKindColor(kind: DataVertexKind): string {
        return this.styleService.getKindStyle(kind).color;
    }

    getKindBorderColor(kind: DataVertexKind): string {
        return this.styleService.getKindStyle(kind).borderColor;
    }

    getKindShape(kind: DataVertexKind): string {
        return this.styleService.getKindStyle(kind).shape;
    }

    getKindWidth(kind: DataVertexKind): number {
        return this.styleService.getKindStyle(kind).width;
    }

    getKindHeight(kind: DataVertexKind): number {
        return this.styleService.getKindStyle(kind).height;
    }

    // -- Kind setters --

    setKindColor(kind: DataVertexKind, color: string): void {
        this.styleService.setKindStyle(kind, { color: normalizeColor(color) });
        this.applyStyles();
    }

    setKindBorderColor(kind: DataVertexKind, borderColor: string): void {
        this.styleService.setKindStyle(kind, { borderColor: normalizeColor(borderColor) });
        this.applyStyles();
    }

    setKindShape(kind: DataVertexKind, shape: string): void {
        this.styleService.setKindStyle(kind, { shape });
        this.applyStyles();
    }

    setKindWidth(kind: DataVertexKind, width: number): void {
        this.styleService.setKindStyle(kind, { width });
        this.applyStyles();
    }

    setKindHeight(kind: DataVertexKind, height: number): void {
        this.styleService.setKindStyle(kind, { height });
        this.applyStyles();
    }

    // -- Type getters --

    getTypeColor(typeLabel: string, kind: DataVertexKind): string {
        return this.styleService.resolveNodeStyle(kind, typeLabel).color;
    }

    getTypeBorderColor(typeLabel: string, kind: DataVertexKind): string {
        return this.styleService.resolveNodeStyle(kind, typeLabel).borderColor;
    }

    getTypeShape(typeLabel: string, kind: DataVertexKind): string {
        return this.styleService.resolveNodeStyle(kind, typeLabel).shape;
    }

    getTypeWidth(typeLabel: string, kind: DataVertexKind): number {
        return this.styleService.resolveNodeStyle(kind, typeLabel).width;
    }

    getTypeHeight(typeLabel: string, kind: DataVertexKind): number {
        return this.styleService.resolveNodeStyle(kind, typeLabel).height;
    }

    hasTypeOverride(typeLabel: string): boolean {
        return !!this.styleService.typeStyles[typeLabel];
    }

    // -- Type setters --

    setTypeColor(typeLabel: string, color: string): void {
        this.styleService.setTypeStyle(typeLabel, { color: normalizeColor(color) });
        this.applyStyles();
    }

    setTypeBorderColor(typeLabel: string, borderColor: string): void {
        this.styleService.setTypeStyle(typeLabel, { borderColor: normalizeColor(borderColor) });
        this.applyStyles();
    }

    setTypeShape(typeLabel: string, shape: string): void {
        this.styleService.setTypeStyle(typeLabel, { shape });
        this.applyStyles();
    }

    setTypeWidth(typeLabel: string, width: number): void {
        this.styleService.setTypeStyle(typeLabel, { width });
        this.applyStyles();
    }

    setTypeHeight(typeLabel: string, height: number): void {
        this.styleService.setTypeStyle(typeLabel, { height });
        this.applyStyles();
    }

    clearTypeOverride(typeLabel: string): void {
        this.styleService.removeTypeStyle(typeLabel);
        this.applyStyles();
    }

    hasKindOverride(kind: DataVertexKind): boolean {
        return this.styleService.hasKindOverride(kind);
    }

    clearKindOverride(kind: DataVertexKind): void {
        this.styleService.removeKindStyle(kind);
        this.applyStyles();
    }

    // -- Edge label colors --

    getEdgeLabelColor(tag: string): string {
        return this.styleService.getEdgeLabelColor(tag);
    }

    setEdgeLabelColor(tag: string, color: string): void {
        this.styleService.setEdgeLabelColor(tag, normalizeColor(color));
        this.visualiser?.applyEdgeStyleUpdate();
    }

    hasEdgeLabelOverride(tag: string): boolean {
        return !!this.styleService.edgeLabelColors[tag];
    }

    clearEdgeLabelOverride(tag: string): void {
        this.styleService.removeEdgeLabelColor(tag);
        this.visualiser?.applyEdgeStyleUpdate();
    }

    resetAll(): void {
        this.styleService.resetToDefaults();
        this.applyStyles();
        this.visualiser?.applyEdgeStyleUpdate();
        this.visualiser?.colorEdgesByConstraintIndex(true);
    }

    get colorEdgesByConstraint(): boolean {
        return this.styleService.colorEdgesByConstraint;
    }

    toggleEdgeColoring(): void {
        this.styleService.colorEdgesByConstraint = !this.styleService.colorEdgesByConstraint;
        this.visualiser?.colorEdgesByConstraintIndex(!this.styleService.colorEdgesByConstraint);
    }

    get labelUseBorderColor(): boolean {
        return this.styleService.labelUseBorderColor;
    }

    toggleLabelUseBorderColor(): void {
        this.styleService.labelUseBorderColor = !this.styleService.labelUseBorderColor;
        this.visualiser?.applyStyleUpdate();
    }

    reLayout(): void {
        this.visualiser?.reLayout();
    }

    private applyStyles(): void {
        this.visualiser?.applyStyleUpdate();
    }
}

/** Normalize rgba() output from ngx-color-picker to #RRGGBBAA hex for the shader pipeline. */
function normalizeColor(color: string): string {
    if (!color) return "#00000000";
    // Already hex
    if (color.startsWith("#")) return color;
    // rgba(r, g, b, a)
    const rgbaMatch = color.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)$/);
    if (rgbaMatch) {
        const r = parseInt(rgbaMatch[1]).toString(16).padStart(2, "0");
        const g = parseInt(rgbaMatch[2]).toString(16).padStart(2, "0");
        const b = parseInt(rgbaMatch[3]).toString(16).padStart(2, "0");
        const a = rgbaMatch[4] !== undefined
            ? Math.round(parseFloat(rgbaMatch[4]) * 255).toString(16).padStart(2, "0")
            : "ff";
        return `#${r}${g}${b}${a}`;
    }
    return color;
}
