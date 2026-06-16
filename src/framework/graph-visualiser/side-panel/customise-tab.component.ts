import { Component, inject, Input, OnChanges, SimpleChanges } from "@angular/core";
import { CommonModule } from "@angular/common";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSlideToggleModule } from "@angular/material/slide-toggle";
import { GraphStyleService, GraphBackgroundType, DEFAULT_BACKGROUND } from "../../../service/graph-style.service";
import { GraphVisualiser } from "../engine";
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
    { tag: "isa!", displayLabel: "isa!" },
    { tag: "sub", displayLabel: "sub" },
    { tag: "sub!", displayLabel: "sub!" },
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
    { value: "hexagon", label: "Hexagon" },
    { value: "ellipse", label: "Ellipse" },
];

const KIND_ORDER: Record<string, number> = Object.fromEntries(DISPLAY_KINDS.map((k, i) => [k.kind, i]));
function kindOrder(kind: string): number { return KIND_ORDER[kind] ?? Infinity; }

@Component({
    selector: "ts-graph-side-panel-customise-tab",
    templateUrl: "customise-tab.component.html",
    styleUrls: ["graph-side-panel.component.scss"],
    imports: [
        CommonModule,
        MatSelectModule, MatTooltipModule,
        MatFormFieldModule, MatInputModule, MatSlideToggleModule,
    ],
})
export class CustomiseTabComponent implements OnChanges {

    @Input() visualiser: GraphVisualiser | null = null;

    styleService = inject(GraphStyleService);
    activeTab: "graph" | "background" = "graph";

    settingsCollapsed = false;
    kindsCollapsed = false;
    /** Default-collapsed: each row in this section is heavy (color + mat-select + two number
     *  inputs), so on large schemas auto-expanding it freezes the app. */
    typesCollapsed = true;
    edgesCollapsed = false;

    readonly displayKinds = DISPLAY_KINDS;
    readonly shapes = AVAILABLE_SHAPES;
    readonly edgeLabels = DISPLAY_EDGE_LABELS;

    discoveredTypes: TypeRow[] = [];

    /** Filter input for the Types section. Live-narrows the rendered row list — necessary at
     *  scale (some schemas have 10k+ types and rendering a heavy row per type freezes the app). */
    typeFilter = "";
    /** Hard cap on the number of Types rows rendered at once. Smaller than the Elements chip
     *  limit because each Customise row is much heavier than a chip. */
    static readonly TYPE_DISPLAY_LIMIT = 100;
    displayedTypes: TypeRow[] = [];
    typeOverflow = 0;

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
        this.recomputeDisplayedTypes();
    }

    private recomputeDisplayedTypes(): void {
        const needle = this.typeFilter.trim().toLowerCase();
        const matches = needle
            ? this.discoveredTypes.filter(t => t.typeLabel.toLowerCase().includes(needle))
            : this.discoveredTypes;
        const limit = CustomiseTabComponent.TYPE_DISPLAY_LIMIT;
        if (matches.length > limit) {
            this.displayedTypes = matches.slice(0, limit);
            this.typeOverflow = matches.length - limit;
        } else {
            this.displayedTypes = matches;
            this.typeOverflow = 0;
        }
    }

    onTypeFilterInput(event: Event): void {
        this.typeFilter = (event.target as HTMLInputElement).value;
        this.recomputeDisplayedTypes();
    }

    clearTypeFilter(): void {
        if (!this.typeFilter) return;
        this.typeFilter = "";
        this.recomputeDisplayedTypes();
    }

    // -- Kind getters --

    getKindColor(kind: VertexKind): string {
        return this.styleService.getKindStyle(kind).color;
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
        this.styleService.setKindStyle(kind, { color });
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

    hasKindOverride(kind: VertexKind): boolean {
        return this.styleService.hasKindOverride(kind);
    }

    clearKindOverride(kind: VertexKind): void {
        this.styleService.removeKindStyle(kind);
        this.applyStyles();
    }

    // -- Type getters --

    getTypeColor(typeLabel: string, kind: VertexKind): string {
        return this.styleService.resolveNodeStyle(kind, typeLabel).color;
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
        this.styleService.setTypeStyle(typeLabel, { color });
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

    // -- Default ("all") edge color: applies to every edge without its own
    //    per-label override. --

    getDefaultEdgeColor(): string {
        return this.styleService.defaultEdgeColor;
    }

    setDefaultEdgeColor(color: string): void {
        this.styleService.defaultEdgeColor = color;
        this.visualiser?.applyEdgeStyleUpdate();
    }

    get hasDefaultEdgeColorOverride(): boolean {
        return this.styleService.hasDefaultEdgeColorOverride;
    }

    clearDefaultEdgeColor(): void {
        this.styleService.clearDefaultEdgeColor();
        this.visualiser?.applyEdgeStyleUpdate();
    }

    // -- Background --

    get backgroundType(): GraphBackgroundType { return this.styleService.background.type; }
    get backgroundColor1(): string { return this.styleService.background.color1; }
    get backgroundColor2(): string { return this.styleService.background.color2; }
    get backgroundAngle(): number { return this.styleService.background.gradientAngle; }

    setBackgroundType(type: GraphBackgroundType): void {
        if (type === "default") {
            this.styleService.updateBackground({ type });
        } else if (type === "grid") {
            this.styleService.updateBackground({ type, color1: "#0e0e0e", color2: "#1a1928" });
        } else if (type === "dots") {
            this.styleService.updateBackground({ type, color1: "#0e0e0e", color2: "#3a3848" });
        } else if (type === "party") {
            this.styleService.updateBackground({ type, color1: "#1a2766", color2: "#cc3344" });
        } else {
            this.styleService.updateBackground({ type });
        }
    }

    get fillOpacityPercent(): number {
        return Math.round(this.styleService.fillOpacity * 100);
    }

    setFillOpacityPercent(percent: number): void {
        this.styleService.fillOpacity = percent / 100;
        this.applyStyles();
    }

    // -- Settings: node label / scaling / edge coloring (moved here from the
    //    side-panel footer so all graph-wide settings live in one place). --

    get labelMode(): "auto" | "fixed" | "hidden" {
        if (!this.styleService.labelsVisible) return "hidden";
        return this.styleService.labelColorMode === "fixed" ? "fixed" : "auto";
    }

    setLabelMode(mode: "auto" | "fixed" | "hidden"): void {
        if (mode === "hidden") {
            this.styleService.labelsVisible = false;
        } else {
            this.styleService.labelsVisible = true;
            this.styleService.labelColorMode = mode === "fixed" ? "fixed" : "auto";
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

    get colorEdgesByConstraint(): boolean {
        return this.styleService.colorEdgesByConstraint;
    }

    toggleEdgeColoring(): void {
        this.styleService.colorEdgesByConstraint = !this.styleService.colorEdgesByConstraint;
        this.visualiser?.colorEdgesByConstraintIndex(!this.styleService.colorEdgesByConstraint);
    }

    get edgesCurvedByDefault(): boolean {
        return this.styleService.edgesCurvedByDefault;
    }

    toggleEdgesCurved(): void {
        this.styleService.edgesCurvedByDefault = !this.styleService.edgesCurvedByDefault;
        this.visualiser?.applyEdgeCurvature();
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
        return this.styleService.background.type !== "default";
    }

    resetBackground(): void {
        this.styleService.background = { ...DEFAULT_BACKGROUND };
    }

    private applyStyles(): void {
        this.visualiser?.applyStyleUpdate();
    }
}
