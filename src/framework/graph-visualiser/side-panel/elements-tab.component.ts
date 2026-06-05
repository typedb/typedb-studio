import { Component, DoCheck, inject, Input, OnChanges, SimpleChanges, ViewChild } from "@angular/core";
import { MatMenuModule, MatMenuTrigger } from "@angular/material/menu";
import { GraphStyleService } from "../../../service/graph-style.service";
import { GraphViewState } from "../../../service/graph-view-state.service";
import { RunOutputState } from "../../../service/query-page-state.service";
import { SchemaConcept, SchemaState } from "../../../service/schema-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
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
    selector: "ts-graph-side-panel-elements-tab",
    templateUrl: "elements-tab.component.html",
    styleUrls: ["graph-side-panel.component.scss"],
    imports: [MatMenuModule],
})
export class ElementsTabComponent implements OnChanges, DoCheck {

    @Input() visualiser: GraphVisualiser | null = null;
    @Input() run: RunOutputState | null = null;

    @ViewChild("typeContextMenuTrigger", { static: true }) typeContextMenuTrigger!: MatMenuTrigger;
    typeMenuPosition = { x: 0, y: 0 };
    typeMenuTarget: TypeRow | null = null;

    styleService = inject(GraphStyleService);
    private graphViewState = inject(GraphViewState);
    private schemaState = inject(SchemaState);
    private snackbar = inject(SnackbarService);

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
    static readonly TYPE_DISPLAY_LIMIT = 100;
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
        const limit = ElementsTabComponent.TYPE_DISPLAY_LIMIT;
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

    // -- Type chip context menu ------------------------------------------------

    /**
     * Right-clicking a type chip opens a menu with "Load links" / "Load
     * attributes" actions scoped to every instance of that type currently in
     * the graph. No-op for attribute types (no own links/attributes).
     */
    onTypeChipContextMenu(event: MouseEvent, row: TypeRow): void {
        if (row.kind !== "entity" && row.kind !== "relation"
            && row.kind !== "entityType" && row.kind !== "relationType") return;
        event.preventDefault();
        event.stopPropagation();
        this.typeMenuPosition = { x: event.clientX, y: event.clientY };
        this.typeMenuTarget = row;
        // Setting `_openedBy` to "mouse" makes FocusMonitor suppress the
        // focus ring on the auto-focused first item — equivalent to opening
        // the menu via a real click on the trigger.
        setTimeout(() => {
            (this.typeContextMenuTrigger as any)._openedBy = "mouse";
            this.typeContextMenuTrigger.openMenu();
        });
    }

    loadLinksForAllOfType(): void {
        const target = this.typeMenuTarget;
        if (!target) return;
        const type = this.lookupType(target);
        if (!type || type.kind === "attributeType") return;
        const iids = this.collectInstanceIidsOfType(target.typeLabel);
        if (iids.length === 0) return;
        this.runFetch(() => this.graphViewState.fetchLinksOf(this.run!, type, iids));
    }

    loadAttributesForAllOfType(): void {
        const target = this.typeMenuTarget;
        if (!target) return;
        const type = this.lookupType(target);
        if (!type || type.kind === "attributeType") return;
        const iids = this.collectInstanceIidsOfType(target.typeLabel);
        if (iids.length === 0) return;
        this.runFetch(() => this.graphViewState.fetchAttributesOf(this.run!, type, iids));
    }

    private lookupType(row: TypeRow): SchemaConcept | null {
        const schema = this.schemaState.value$.value;
        if (!schema) {
            this.snackbar.errorPersistent("Schema not loaded");
            return null;
        }
        // Type rows from the Elements tab can be tagged with either the
        // instance kind (entity/relation/attribute) or the type kind
        // (entityType/...); both should resolve through the same maps.
        switch (row.kind) {
            case "entity":
            case "entityType":
                return schema.entities[row.typeLabel] ?? null;
            case "relation":
            case "relationType":
                return schema.relations[row.typeLabel] ?? null;
            case "attribute":
            case "attributeType":
                return schema.attributes[row.typeLabel] ?? null;
            default:
                return null;
        }
    }

    private collectInstanceIidsOfType(typeLabel: string): string[] {
        if (!this.visualiser) return [];
        const out: string[] = [];
        const seen = new Set<string>();
        this.visualiser.graph.nodes().forEach(key => {
            try {
                const concept = this.visualiser!.graph.getNodeAttributes(key)?.["metadata"]?.concept;
                if (!concept) return;
                if ((concept.kind === "entity" || concept.kind === "relation")
                    && concept.type?.label === typeLabel
                    && concept.iid
                    && !seen.has(concept.iid)) {
                    seen.add(concept.iid);
                    out.push(concept.iid);
                }
            } catch { /* missing metadata mid-mutation */ }
        });
        return out;
    }

    private runFetch(op: () => Promise<void>): void {
        if (!this.run || !this.visualiser) return;
        this.visualiser.freezeViewport();
        op().then(() => {
            this.visualiser?.reheat({ soft: true, preserveCamera: true });
            this.visualiser?.interactionHandler.recomputeHighlightSet();
        });
    }
}
