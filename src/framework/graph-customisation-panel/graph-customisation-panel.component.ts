import { Component, inject, Input, OnChanges, SimpleChanges } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatMenuModule } from "@angular/material/menu";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { GraphStyleService, CustomPreset, GraphBackgroundType } from "../../service/graph-style.service";
import { GraphVisualiser } from "../graph-visualiser";
import { VertexKind } from "@typedb/graph-utils";

interface KindRow {
    kind: VertexKind;
    label: string;
}

interface TypeRow {
    typeLabel: string;
    kind: VertexKind;
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

const KIND_ORDER: Record<string, number> = Object.fromEntries(DISPLAY_KINDS.map((k, i) => [k.kind, i]));
function kindOrder(kind: string): number { return KIND_ORDER[kind] ?? Infinity; }

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
        MatButtonModule, MatMenuModule, MatSelectModule, MatTooltipModule,
        MatFormFieldModule, MatInputModule, MatSlideToggleModule,
    ],
})
export class GraphCustomisationPanelComponent implements OnChanges {

    @Input() visualiser: GraphVisualiser | null = null;

    styleService = inject(GraphStyleService);
    topTab: "highlights" | "presets" | "customise" = "highlights";
    activeTab: "kind" | "type" | "edge" | "background" = "kind";

    readonly displayKinds = DISPLAY_KINDS;
    readonly shapes = AVAILABLE_SHAPES;
    readonly edgeLabels = DISPLAY_EDGE_LABELS;

    discoveredTypes: TypeRow[] = [];

    ngOnChanges(changes: SimpleChanges): void {
        if (changes["visualiser"]) {
            this.refreshDiscoveredTypes();
        }
    }

    refreshDiscoveredTypes(): void {
        if (!this.visualiser) return;
        const typeMap = new Map<string, VertexKind>();
        this.visualiser.graph.nodes().forEach(nodeKey => {
            const attrs = this.visualiser!.graph.getNodeAttributes(nodeKey);
            const concept = attrs.metadata.concept;
            if ("type" in concept && concept.type && "label" in concept.type) {
                typeMap.set(concept.type.label, concept.kind as any);
            } else if ("label" in concept && !["unavailable", "expression", "functionCall"].includes(concept.kind)) {
                typeMap.set((concept as any).label, concept.kind as any);
            }
        });
        this.discoveredTypes = Array.from(typeMap.entries())
            .map(([typeLabel, kind]) => ({ typeLabel, kind }))
            .sort((a, b) => kindOrder(a.kind) - kindOrder(b.kind) || a.typeLabel.localeCompare(b.typeLabel));
    }

    // -- Kind getters --

    getKindColor(kind: VertexKind): string {
        return this.styleService.getKindStyle(kind).color;
    }

    getKindBorderColor(kind: VertexKind): string {
        return this.styleService.getKindStyle(kind).borderColor;
    }

    getKindShape(kind: VertexKind): string {
        return this.styleService.getKindStyle(kind).shape;
    }

    getKindWidth(kind: VertexKind): number {
        return this.styleService.getKindStyle(kind).width;
    }

    getKindHeight(kind: VertexKind): number {
        return this.styleService.getKindStyle(kind).height;
    }

    // -- Kind setters --

    setKindColor(kind: VertexKind, color: string): void {
        this.styleService.setKindStyle(kind, { color: color });
        this.applyStyles();
    }

    setKindBorderColor(kind: VertexKind, borderColor: string): void {
        this.styleService.setKindStyle(kind, { borderColor: borderColor });
        this.applyStyles();
    }

    setKindShape(kind: VertexKind, shape: string): void {
        this.styleService.setKindStyle(kind, { shape });
        this.applyStyles();
    }

    setKindWidth(kind: VertexKind, width: number): void {
        this.styleService.setKindStyle(kind, { width });
        this.applyStyles();
    }

    setKindHeight(kind: VertexKind, height: number): void {
        this.styleService.setKindStyle(kind, { height });
        this.applyStyles();
    }

    // -- Type getters --

    getTypeColor(typeLabel: string, kind: VertexKind): string {
        return this.styleService.resolveNodeStyle(kind, typeLabel).color;
    }

    getTypeBorderColor(typeLabel: string, kind: VertexKind): string {
        return this.styleService.resolveNodeStyle(kind, typeLabel).borderColor;
    }

    getTypeShape(typeLabel: string, kind: VertexKind): string {
        return this.styleService.resolveNodeStyle(kind, typeLabel).shape;
    }

    getTypeWidth(typeLabel: string, kind: VertexKind): number {
        return this.styleService.resolveNodeStyle(kind, typeLabel).width;
    }

    getTypeHeight(typeLabel: string, kind: VertexKind): number {
        return this.styleService.resolveNodeStyle(kind, typeLabel).height;
    }

    hasTypeOverride(typeLabel: string): boolean {
        return !!this.styleService.typeStyles[typeLabel];
    }

    // -- Type setters --

    setTypeColor(typeLabel: string, color: string): void {
        this.styleService.setTypeStyle(typeLabel, { color: color });
        this.applyStyles();
    }

    setTypeBorderColor(typeLabel: string, borderColor: string): void {
        this.styleService.setTypeStyle(typeLabel, { borderColor: borderColor });
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

    hasKindOverride(kind: VertexKind): boolean {
        return this.styleService.hasKindOverride(kind);
    }

    clearKindOverride(kind: VertexKind): void {
        this.styleService.removeKindStyle(kind);
        this.applyStyles();
    }

    // -- Edge label colors --

    getEdgeLabelColor(tag: string): string {
        return this.styleService.getEdgeLabelColor(tag);
    }

    setEdgeLabelColor(tag: string, color: string): void {
        this.styleService.setEdgeLabelColor(tag, color);
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

    get labelMode(): "border" | "fixed" | "hidden" {
        if (!this.styleService.labelsVisible) return "hidden";
        return this.styleService.labelUseBorderColor ? "border" : "fixed";
    }

    setLabelMode(mode: "border" | "fixed" | "hidden"): void {
        if (mode === "hidden") {
            this.styleService.labelsVisible = false;
        } else {
            this.styleService.labelsVisible = true;
            this.styleService.labelUseBorderColor = mode === "border";
        }
        this.visualiser?.restoreLabels();
    }

    get showHoverLabel(): boolean {
        return this.styleService.showHoverLabel;
    }

    toggleShowHoverLabel(): void {
        this.styleService.showHoverLabel = !this.styleService.showHoverLabel;
        this.visualiser?.restoreLabels();
    }

    get degreeScaling(): boolean {
        return this.styleService.degreeScaling;
    }

    toggleDegreeScaling(): void {
        this.styleService.degreeScaling = !this.styleService.degreeScaling;
        if (this.styleService.degreeScaling) {
            this.visualiser?.applyStructureMode();
        } else {
            this.visualiser?.applyStyleUpdate();
        }
    }

    reLayout(): void {
        this.visualiser?.reLayout();
    }

    // -- Highlights --

    isKindHighlighted(kind: VertexKind): boolean {
        return this.styleService.highlightedKinds.has(kind);
    }

    isTypeHighlighted(typeLabel: string): boolean {
        return this.styleService.highlightedTypes.has(typeLabel);
    }

    isEdgeHighlighted(tag: string): boolean {
        return this.styleService.highlightedEdges.has(tag);
    }

    toggleHighlightKind(kind: VertexKind): void {
        this.styleService.toggleHighlightKind(kind);
        this.visualiser?.sigma.refresh();
    }

    toggleHighlightType(typeLabel: string): void {
        this.styleService.toggleHighlightType(typeLabel);
        this.visualiser?.sigma.refresh();
    }

    toggleHighlightEdge(tag: string): void {
        this.styleService.toggleHighlightEdge(tag);
        this.visualiser?.sigma.refresh();
    }

    selectAllKinds(): void {
        for (const row of this.displayKinds) this.styleService.highlightedKinds.add(row.kind);
        this.visualiser?.sigma.refresh();
    }

    unselectAllKinds(): void {
        this.styleService.highlightedKinds.clear();
        this.visualiser?.sigma.refresh();
    }

    selectAllTypes(): void {
        for (const row of this.discoveredTypes) this.styleService.highlightedTypes.add(row.typeLabel);
        this.visualiser?.sigma.refresh();
    }

    unselectAllTypes(): void {
        this.styleService.highlightedTypes.clear();
        this.visualiser?.sigma.refresh();
    }

    selectAllEdges(): void {
        for (const row of this.edgeLabels) this.styleService.highlightedEdges.add(row.tag);
        this.visualiser?.sigma.refresh();
    }

    unselectAllEdges(): void {
        this.styleService.highlightedEdges.clear();
        this.visualiser?.sigma.refresh();
    }

    soloHighlightKind(kind: VertexKind): void {
        this.styleService.highlightedKinds.clear();
        this.styleService.highlightedKinds.add(kind);
        this.visualiser?.sigma.refresh();
    }

    soloHighlightType(typeLabel: string): void {
        this.styleService.highlightedTypes.clear();
        this.styleService.highlightedTypes.add(typeLabel);
        this.visualiser?.sigma.refresh();
    }

    soloHighlightEdge(tag: string): void {
        this.styleService.highlightedEdges.clear();
        this.styleService.highlightedEdges.add(tag);
        this.visualiser?.sigma.refresh();
    }

    clearHighlights(): void {
        this.styleService.clearHighlights();
        this.visualiser?.sigma.refresh();
    }

    // -- Presets --

    get activePreset(): string | null {
        return this.styleService.activePreset;
    }

    appliedPreset: string | null = null;
    private appliedTimer: ReturnType<typeof setTimeout> | null = null;

    applyPreset(preset: "default" | "structure" | "uniform" | "classic" | "grayscale"): void {
        if (preset === "default") {
            this.styleService.applyDefaultPreset();
            this.visualiser?.restoreLabels();
            this.visualiser?.applyEdgeStyleUpdate();
            this.visualiser?.colorEdgesByConstraintIndex(true);
        } else if (preset === "structure") {
            this.styleService.applyStructurePreset();
            this.visualiser?.applyStructureMode();
        } else if (preset === "uniform") {
            this.styleService.applyUniformPreset();
            this.visualiser?.restoreLabels();
        } else if (preset === "classic") {
            this.styleService.applyClassicPreset();
            this.visualiser?.restoreLabels();
        } else if (preset === "grayscale") {
            this.styleService.applyGrayscalePreset();
            this.visualiser?.restoreLabels();
        }
        if (this.appliedTimer) clearTimeout(this.appliedTimer);
        this.appliedPreset = preset;
        this.appliedTimer = setTimeout(() => { this.appliedPreset = null; }, 2000);
    }

    // -- Custom presets --

    get customPresets(): readonly CustomPreset[] {
        return this.styleService.customPresets;
    }

    savingPreset = false;
    newPresetName = "";
    newPresetDescription = "";

    startSavingPreset(): void {
        this.savingPreset = true;
        this.newPresetName = "";
        this.newPresetDescription = "";
    }

    confirmSavePreset(): void {
        const name = this.newPresetName.trim();
        if (!name) return;
        this.styleService.saveCustomPreset(name, this.newPresetDescription.trim());
        this.savingPreset = false;
        this.newPresetName = "";
        this.newPresetDescription = "";
    }

    cancelSavePreset(): void {
        this.savingPreset = false;
        this.newPresetName = "";
        this.newPresetDescription = "";
    }

    applyCustomPreset(name: string): void {
        this.styleService.applyCustomPreset(name);
        this.visualiser?.restoreLabels();
        this.visualiser?.applyEdgeStyleUpdate();
        if (this.appliedTimer) clearTimeout(this.appliedTimer);
        this.appliedPreset = `custom:${name}`;
        this.appliedTimer = setTimeout(() => { this.appliedPreset = null; }, 2000);
    }

    editingPreset: string | null = null;
    editPresetName = "";
    editPresetDescription = "";

    startEditPreset(name: string): void {
        const preset = this.customPresets.find(p => p.name === name);
        if (!preset) return;
        this.editingPreset = name;
        this.editPresetName = preset.name;
        this.editPresetDescription = preset.description;
    }

    confirmEditPreset(originalName: string): void {
        const name = this.editPresetName.trim();
        if (!name) return;
        this.styleService.renameCustomPreset(originalName, name, this.editPresetDescription.trim());
        this.editingPreset = null;
    }

    cancelEditPreset(): void {
        this.editingPreset = null;
    }

    overwrittenPreset: string | null = null;
    private overwrittenTimer: ReturnType<typeof setTimeout> | null = null;

    overwriteCustomPreset(name: string): void {
        const preset = this.customPresets.find(p => p.name === name);
        if (!preset) return;
        this.styleService.saveCustomPreset(name, preset.description);
        if (this.overwrittenTimer) clearTimeout(this.overwrittenTimer);
        this.overwrittenPreset = name;
        this.overwrittenTimer = setTimeout(() => { this.overwrittenPreset = null; }, 2000);
    }

    deleteCustomPreset(name: string): void {
        this.styleService.deleteCustomPreset(name);
    }

    // -- Background --

    get backgroundType(): GraphBackgroundType { return this.styleService.background.type; }
    get backgroundColor1(): string { return this.styleService.background.color1; }
    get backgroundColor2(): string { return this.styleService.background.color2; }
    get backgroundAngle(): number { return this.styleService.background.gradientAngle; }

    setBackgroundType(type: GraphBackgroundType): void {
        if (type === "grid") {
            this.styleService.updateBackground({ type, color1: "#232135", color2: "#0e0e0e" });
        } else if (type === "dots") {
            this.styleService.updateBackground({ type, color1: "#4e4b63", color2: "#0e0e0e" });
        } else if (type === "party") {
            this.styleService.updateBackground({ type, color1: "#cc3344", color2: "#1a2766" });
        } else {
            this.styleService.updateBackground({ type });
        }
    }

    setBackgroundColor1(color1: string): void {
        this.styleService.updateBackground({ color1 });
    }

    setBackgroundColor2(color2: string): void {
        this.styleService.updateBackground({ color2 });
    }

    setBackgroundAngle(gradientAngle: number): void {
        this.styleService.updateBackground({ gradientAngle });
    }

    get hasBackgroundOverride(): boolean {
        const bg = this.styleService.background;
        return bg.type !== "solid" || bg.color1 !== "#0e0e0e";
    }

    resetBackground(): void {
        this.styleService.background = { type: "solid", color1: "#0e0e0e", color2: "#1A182A", gradientAngle: 180 };
    }

    private applyStyles(): void {
        this.visualiser?.applyStyleUpdate();
    }
}