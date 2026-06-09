/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, DoCheck, inject, Input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { GraphVisualiser } from "../engine";
import { GraphViewState } from "../../../service/graph-view-state.service";
import { AppData } from "../../../service/app-data.service";
import { RunOutputState } from "../../../service/query-page-state.service";
import { SchemaAttribute, SchemaConcept, SchemaRelation, SchemaState } from "../../../service/schema-state.service";

interface AttributeChipRow {
    type: SchemaAttribute;
    label: string;
}

interface RelationChipRow {
    type: SchemaRelation;
    label: string;
}

/**
 * Right-pane inspector for **type-level** exploration. Counterpart to
 * `GraphInspectorComponent`. The `GraphSidePanelComponent` swaps between the
 * two based on the tab's `selectionMode`.
 *
 * F4 wires the chip discovery + load actions. Each chip toggles a sticky
 * "loaded" state on the tab (see `GraphViewState.markConnectionLoaded`) —
 * clicking it ON triggers a fetch that loads the connection across every
 * instance of `selectedType` currently in the graph; clicking the same chip
 * a second time is a no-op (loaded stays loaded for the session). Use the
 * canvas's "Reset changes" button to wipe everything back to the initial
 * query.
 */
@Component({
    selector: "ts-graph-type-inspector",
    templateUrl: "./graph-type-inspector.component.html",
    styleUrls: [
        // Reuse the data-side inspector's section / type-row styling so we
        // don't duplicate it. Listed first so this component's own SCSS
        // wins on the :host overrides.
        "../../../module/data/instance-detail/instance-detail.component.scss",
        "./graph-type-inspector.component.scss",
    ],
    imports: [CommonModule, MatFormFieldModule, MatSelectModule],
})
export class GraphTypeInspectorComponent implements DoCheck {
    @Input() selectedType: SchemaConcept | null = null;
    @Input() run: RunOutputState | null = null;
    @Input() visualiser: GraphVisualiser | null = null;

    /** Live count of instances of `selectedType` currently in the graph. */
    instanceCount = 0;
    attributeChips: AttributeChipRow[] = [];
    relationChips: RelationChipRow[] = [];

    private graphViewState = inject(GraphViewState);
    private schemaState = inject(SchemaState);
    private appData = inject(AppData);

    private lastGraphOrder = -1;
    private lastSelectedTypeLabel: string | null = null;

    get hasSelection(): boolean { return this.selectedType != null; }

    get typeKindLabel(): string {
        switch (this.selectedType?.kind) {
            case "entityType":    return "entity";
            case "relationType":  return "relation";
            case "attributeType": return "attribute";
            default: return "";
        }
    }

    /** Whether the source type can have its own attributes / relations
     *  surfaced here (entities + relations qualify; attribute types are
     *  inverted — they're owned, not owning). */
    get supportsConnections(): boolean {
        return this.selectedType?.kind === "entityType" || this.selectedType?.kind === "relationType";
    }

    ngDoCheck(): void {
        const graph = this.visualiser?.graph;
        const order = graph?.order ?? 0;
        const label = this.selectedType?.label ?? null;
        const sameType = label === this.lastSelectedTypeLabel;
        const sameOrder = order === this.lastGraphOrder;
        if (sameType && sameOrder) return;
        this.lastGraphOrder = order;
        this.lastSelectedTypeLabel = label;
        this.instanceCount = this.computeInstanceCount();
        if (!sameType) this.refreshChips();
    }

    private refreshChips(): void {
        if (!this.selectedType || !this.supportsConnections) {
            this.attributeChips = [];
            this.relationChips = [];
            return;
        }
        const t = this.selectedType as { ownedAttributes: SchemaAttribute[]; playedRoles: { label: string }[] };
        // ownedAttributes is a flat list of SchemaAttribute entries.
        this.attributeChips = (t.ownedAttributes ?? []).map(attr => ({
            type: attr,
            label: attr.label,
        }));
        // playedRoles are scoped role labels like "friendship:friend". The
        // relation type is the prefix before ":". De-dupe across roles in
        // the same relation.
        const schema = this.schemaState.value$.value;
        const seenRels = new Set<string>();
        const rels: RelationChipRow[] = [];
        for (const role of t.playedRoles ?? []) {
            const relLabel = role.label.split(":")[0];
            if (seenRels.has(relLabel)) continue;
            seenRels.add(relLabel);
            const relType = schema?.relations[relLabel];
            if (relType) rels.push({ type: relType, label: relLabel });
        }
        rels.sort((a, b) => a.label.localeCompare(b.label));
        this.relationChips = rels;
    }

    /** Bound to the <select> value. Empty string means "(auto)" — i.e. no
     *  override; the heuristic picks. */
    get currentLabelOverride(): string {
        if (!this.selectedType || !this.visualiser) return "";
        return this.visualiser.labelOverridesByType.get(this.selectedType.label) ?? "";
    }

    onLabelOverrideChange(value: string): void {
        if (!this.selectedType || !this.visualiser || !this.run) return;
        const next = value === "" ? null : value;
        this.visualiser.setLabelOverride(this.selectedType.label, next);
        const database = this.run.graph.database;
        if (database) {
            this.appData.nodeLabelPrefs.set(database, this.selectedType.label, next);
        }
    }

    isAttributeLoaded(row: AttributeChipRow): boolean {
        if (!this.selectedType || !this.run) return false;
        return this.graphViewState.isConnectionLoaded(this.run, this.selectedType.label, row.label);
    }

    isRelationLoaded(row: RelationChipRow): boolean {
        if (!this.selectedType || !this.run) return false;
        return this.graphViewState.isConnectionLoaded(this.run, this.selectedType.label, row.label);
    }

    toggleAttributeChip(row: AttributeChipRow): void {
        if (!this.selectedType || !this.run || !this.visualiser) return;
        if (this.isAttributeLoaded(row)) return; // sticky: re-toggle is no-op
        const iids = this.collectInstanceIidsOfType(this.selectedType.label);
        if (iids.length === 0) {
            // Still mark loaded so the chip reflects the intent; nothing to fetch.
            this.graphViewState.markConnectionLoaded(this.run, this.selectedType.label, row.label);
            return;
        }
        this.visualiser.freezeViewport();
        this.graphViewState
            .fetchAttributesOfTypeFor(this.run, this.selectedType, iids, row.label)
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.interactionHandler.recomputeHighlightSet();
                this.graphViewState.markConnectionLoaded(this.run!, this.selectedType!.label, row.label);
            });
    }

    toggleRelationChip(row: RelationChipRow): void {
        if (!this.selectedType || !this.run || !this.visualiser) return;
        if (this.isRelationLoaded(row)) return; // sticky
        const iids = this.collectInstanceIidsOfType(this.selectedType.label);
        if (iids.length === 0) {
            this.graphViewState.markConnectionLoaded(this.run, this.selectedType.label, row.label);
            return;
        }
        this.visualiser.freezeViewport();
        this.graphViewState
            .fetchRelationsOfTypeForPlayers(this.run, this.selectedType, iids, row.label)
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.interactionHandler.recomputeHighlightSet();
                this.graphViewState.markConnectionLoaded(this.run!, this.selectedType!.label, row.label);
            });
    }

    private computeInstanceCount(): number {
        const graph = this.visualiser?.graph;
        const label = this.selectedType?.label;
        if (!graph || !label) return 0;
        let n = 0;
        graph.nodes().forEach(key => {
            try {
                const concept = graph.getNodeAttributes(key)?.["metadata"]?.concept;
                if (!concept) return;
                if ((concept.kind === "entity" || concept.kind === "relation" || concept.kind === "attribute")
                    && concept.type?.label === label) {
                    n++;
                }
            } catch { /* missing metadata mid-mutation */ }
        });
        return n;
    }

    private collectInstanceIidsOfType(typeLabel: string): string[] {
        const graph = this.visualiser?.graph;
        if (!graph) return [];
        const out: string[] = [];
        const seen = new Set<string>();
        graph.nodes().forEach(key => {
            try {
                const concept = graph.getNodeAttributes(key)?.["metadata"]?.concept;
                if (!concept) return;
                if ((concept.kind === "entity" || concept.kind === "relation")
                    && concept.type?.label === typeLabel
                    && concept.iid && !seen.has(concept.iid)) {
                    seen.add(concept.iid);
                    out.push(concept.iid);
                }
            } catch { /* missing metadata mid-mutation */ }
        });
        return out;
    }
}
