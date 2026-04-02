import { Component, inject, Input, OnChanges, SimpleChanges } from "@angular/core";
import { GraphStyleService } from "../../service/graph-style.service";
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

@Component({
    selector: "ts-graph-styles-highlights-tab",
    templateUrl: "highlights-tab.component.html",
    styleUrls: ["graph-styles-pane.component.scss"],
})
export class HighlightsTabComponent implements OnChanges {

    @Input() visualiser: GraphVisualiser | null = null;

    styleService = inject(GraphStyleService);

    readonly displayKinds = DISPLAY_KINDS;
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

    getKindBorderColor(kind: VertexKind): string {
        return this.styleService.getKindStyle(kind).borderColor;
    }

    getTypeBorderColor(typeLabel: string, kind: VertexKind): string {
        return this.styleService.resolveNodeStyle(kind, typeLabel).borderColor;
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
}
