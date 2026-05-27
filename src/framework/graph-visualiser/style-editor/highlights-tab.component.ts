import { Component, DoCheck, inject, Input, OnChanges, SimpleChanges } from "@angular/core";
import { GraphStyleService } from "../../../service/graph-style.service";
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

const KIND_ORDER: Record<string, number> = Object.fromEntries(DISPLAY_KINDS.map((k, i) => [k.kind, i]));
function kindOrder(kind: string): number { return KIND_ORDER[kind] ?? Infinity; }

@Component({
    selector: "ts-graph-styles-highlights-tab",
    templateUrl: "highlights-tab.component.html",
    styleUrls: ["graph-styles-pane.component.scss"],
})
export class HighlightsTabComponent implements OnChanges, DoCheck {

    @Input() visualiser: GraphVisualiser | null = null;

    styleService = inject(GraphStyleService);

    readonly displayKinds = DISPLAY_KINDS;
    readonly edgeLabels = DISPLAY_EDGE_LABELS;

    discoveredTypes: TypeRow[] = [];
    kindCounts = new Map<VertexKind, number>();
    typeCounts = new Map<string, number>();
    edgeCounts = new Map<string, number>();

    /** Filter input for the Types chip section. Live-narrows the rendered chip list — necessary
     *  at scale (some schemas have 10k+ types and rendering a chip per type cripples the UI). */
    typeFilter = "";
    /** Hard cap on the number of Types chips rendered at once. */
    static readonly TYPE_DISPLAY_LIMIT = 200;
    displayedTypes: TypeRow[] = [];
    typeOverflow = 0;

    private lastGraphOrder = -1;
    private lastGraphSize = -1;

    ngOnChanges(changes: SimpleChanges): void {
        if (changes["visualiser"]) {
            this.lastGraphOrder = -1; // force recompute on next DoCheck
            this.lastGraphSize = -1;
            this.refresh();
        }
    }

    ngDoCheck(): void {
        // Recompute when the underlying graph has gained or lost vertices/edges.
        // Cheap O(1) check; the full refresh runs only when something actually changed.
        if (!this.visualiser) return;
        const order = this.visualiser.graph.order;
        const size = this.visualiser.graph.size;
        if (order !== this.lastGraphOrder || size !== this.lastGraphSize) {
            this.refresh();
            this.lastGraphOrder = order;
            this.lastGraphSize = size;
        }
    }

    /** Reduce over the graph once: discover types, count vertices by kind and by type label,
     *  and count edges by tag. Used by chip rendering. */
    refresh(): void {
        this.discoveredTypes = [];
        this.kindCounts.clear();
        this.typeCounts.clear();
        this.edgeCounts.clear();
        if (!this.visualiser) return;

        const typeKinds = new Map<string, VertexKind>();
        for (const nodeKey of this.visualiser.graph.nodes()) {
            const attrs = this.visualiser.graph.getNodeAttributes(nodeKey);
            const concept = attrs.metadata.concept;
            const kind = concept.kind as VertexKind;
            this.kindCounts.set(kind, (this.kindCounts.get(kind) ?? 0) + 1);

            let typeLabel: string | undefined;
            if ("type" in concept && concept.type && "label" in concept.type) {
                typeLabel = concept.type.label;
            } else if ("label" in concept && !["unavailable", "expression", "functionCall"].includes(concept.kind)) {
                typeLabel = (concept as any).label;
            }
            if (typeLabel) {
                typeKinds.set(typeLabel, kind);
                this.typeCounts.set(typeLabel, (this.typeCounts.get(typeLabel) ?? 0) + 1);
            }
        }
        for (const edgeKey of this.visualiser.graph.edges()) {
            const attrs = this.visualiser.graph.getEdgeAttributes(edgeKey);
            const tag = attrs.metadata?.dataEdge?.tag;
            if (tag) this.edgeCounts.set(tag, (this.edgeCounts.get(tag) ?? 0) + 1);
        }

        this.discoveredTypes = Array.from(typeKinds.entries())
            .map(([typeLabel, kind]) => ({ typeLabel, kind }))
            .sort((a, b) => kindOrder(a.kind) - kindOrder(b.kind) || a.typeLabel.localeCompare(b.typeLabel));
        this.recomputeDisplayedTypes();
    }

    private recomputeDisplayedTypes(): void {
        const needle = this.typeFilter.trim().toLowerCase();
        const matches = needle
            ? this.discoveredTypes.filter(t => t.typeLabel.toLowerCase().includes(needle))
            : this.discoveredTypes;
        const limit = HighlightsTabComponent.TYPE_DISPLAY_LIMIT;
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

    getKindCount(kind: VertexKind): number { return this.kindCounts.get(kind) ?? 0; }
    getTypeCount(typeLabel: string): number { return this.typeCounts.get(typeLabel) ?? 0; }
    getEdgeCount(tag: string): number { return this.edgeCounts.get(tag) ?? 0; }

    getKindColor(kind: VertexKind): string {
        return this.styleService.getKindStyle(kind).color;
    }

    getTypeColor(typeLabel: string, kind: VertexKind): string {
        return this.styleService.resolveNodeStyle(kind, typeLabel).color;
    }

    getEdgeLabelColor(tag: string): string {
        return this.styleService.getEdgeLabelColor(tag);
    }

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

    /** True when the chip-hover preview is allowed to take effect (no real highlight or vertex selection active). */
    private canPreview(): boolean {
        if (this.styleService.isHighlightActive()) return false;
        if (this.visualiser?.interactionHandler.state.selectedNode != null) return false;
        return true;
    }

    onChipHoverKind(kind: VertexKind): void {
        if (!this.canPreview()) return;
        this.styleService.setPreviewKind(kind);
        this.visualiser?.sigma.refresh();
    }

    onChipHoverType(typeLabel: string): void {
        if (!this.canPreview()) return;
        this.styleService.setPreviewType(typeLabel);
        this.visualiser?.sigma.refresh();
    }

    onChipHoverEdge(tag: string): void {
        if (!this.canPreview()) return;
        this.styleService.setPreviewEdge(tag);
        this.visualiser?.sigma.refresh();
    }

    onChipHoverEnd(): void {
        if (!this.styleService.isPreviewActive()) return;
        this.styleService.clearPreview();
        this.visualiser?.sigma.refresh();
    }
}
