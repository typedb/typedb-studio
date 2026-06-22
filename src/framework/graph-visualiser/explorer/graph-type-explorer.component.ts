/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, DoCheck, inject, Input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { MatTooltipModule } from "@angular/material/tooltip";
import { GraphVisualiser } from "../engine";
import { GraphViewState } from "../../../service/graph-view-state.service";
import { AppData } from "../../../service/app-data.service";
import { RunOutputState } from "../../../service/query-page-state.service";
import { SchemaAttribute, SchemaConcept, SchemaRelation, SchemaRole, SchemaState } from "../../../service/schema-state.service";

interface AttributeChipRow {
    type: SchemaAttribute;
    label: string;
}

interface RelationChipRow {
    type: SchemaRelation;
    label: string;
}

interface RoleChipRow {
    role: SchemaRole;
    /** Scoped role label (e.g. "friendship:friend") used as the unique key
     *  for sticky loaded-state. */
    label: string;
    /** Short role name (e.g. "friend") — used to display the chip and to
     *  build the TypeQL pattern `links (<shortName>: $p)`. */
    shortName: string;
}

/**
 * Right-pane explorer for **type-level** exploration. Counterpart to
 * `GraphInstanceExplorerComponent`. The `GraphSidePanelComponent` swaps between the
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
    selector: "ts-graph-type-explorer",
    templateUrl: "./graph-type-explorer.component.html",
    styleUrls: [
        // Reuse the data-side inspector's section / type-row styling so we
        // don't duplicate it. Listed first so this component's own SCSS
        // wins on the :host overrides.
        "../../../module/data/instance-detail/instance-detail.component.scss",
        "./graph-type-explorer.component.scss",
    ],
    imports: [CommonModule, MatFormFieldModule, MatSelectModule, MatTooltipModule],
})
export class GraphTypeExplorerComponent implements DoCheck {
    @Input() selectedType: SchemaConcept | SchemaRole | null = null;
    @Input() run: RunOutputState | null = null;
    @Input() visualiser: GraphVisualiser | null = null;
    /** True when this explorer is showing the schema visualiser's graph (type
     *  nodes only). Hides instance-oriented UI — the "N in graph" count and the
     *  connection-loading chips — which are meaningless without data instances. */
    @Input() schemaMode = false;

    /** Live count of instances of `selectedType` currently in the graph. */
    instanceCount = 0;
    attributeChips: AttributeChipRow[] = [];
    relationChips: RelationChipRow[] = [];
    /** Populated only when the selected type is a relation type. Each entry
     *  represents a role this relation relates; toggling the chip loads
     *  every role-player of that role across every in-graph instance of the
     *  relation. */
    roleChips: RoleChipRow[] = [];

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
            case "roleType":      return "role";
            default: return "";
        }
    }

    /** Supertype label of the selected type, or null. Roles (and types without
     *  a supertype) return null. Centralised here so the template doesn't have
     *  to reach into `.supertype`, which doesn't exist on every union member. */
    get supertypeLabel(): string | null {
        return (this.selectedType as { supertype?: { label?: string } } | null)?.supertype?.label ?? null;
    }

    /** Whether the source type can have its own attributes / relations
     *  surfaced here (entities + relations qualify; attribute types are
     *  inverted — they're owned, not owning). */
    get supportsConnections(): boolean {
        return this.selectedType?.kind === "entityType" || this.selectedType?.kind === "relationType";
    }

    /** Selected type narrowed to a connectable `SchemaConcept` (entity / relation
     *  / attribute), or null for role types. The connection-loading actions
     *  below only run when `supportsConnections` is true, so this is always
     *  non-null there — it just lets the calls type-check against the widened
     *  `selectedType` (which now also admits role types). */
    private get connectableType(): SchemaConcept | null {
        return this.selectedType && this.selectedType.kind !== "roleType" ? this.selectedType : null;
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
            this.roleChips = [];
            return;
        }
        const t = this.selectedType as {
            ownedAttributes: SchemaAttribute[];
            playedRoles: SchemaRole[];
            relatedRoles?: SchemaRole[];
        };
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
        // Roles section: only meaningful for relation types. Pull the relation's
        // related role list, derive the short name from the scoped "rel:role"
        // label, and sort alphabetically.
        if (this.selectedType.kind === "relationType") {
            this.roleChips = (t.relatedRoles ?? []).map(r => ({
                role: r,
                label: r.label,
                shortName: r.label.split(":").at(-1) ?? r.label,
            })).sort((a, b) => a.shortName.localeCompare(b.shortName));
        } else {
            this.roleChips = [];
        }
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

    isRoleLoaded(row: RoleChipRow): boolean {
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
            .fetchAttributesOfTypeFor(this.run, this.connectableType!, iids, row.label)
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.interactionHandler.recomputeHighlightSet();
                this.graphViewState.markConnectionLoaded(this.run!, this.selectedType!.label, row.label);
            });
    }

    toggleRoleChip(row: RoleChipRow): void {
        if (!this.selectedType || !this.run || !this.visualiser) return;
        if (this.isRoleLoaded(row)) return; // sticky
        const iids = this.collectInstanceIidsOfType(this.selectedType.label);
        if (iids.length === 0) {
            this.graphViewState.markConnectionLoaded(this.run, this.selectedType.label, row.label);
            return;
        }
        this.visualiser.freezeViewport();
        this.graphViewState
            .fetchRolePlayersOfTypeFor(this.run, this.connectableType!, iids, row.shortName)
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
            .fetchRelationsOfTypeForPlayers(this.run, this.connectableType!, iids, row.label)
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.interactionHandler.recomputeHighlightSet();
                this.graphViewState.markConnectionLoaded(this.run!, this.selectedType!.label, row.label);
            });
    }

    // -- "All" (*) chips: load every attribute / relation / role at once. The
    //    chip is highlighted (loaded) and non-interactive once every individual
    //    chip in its section is loaded. --

    get allAttributesLoaded(): boolean {
        return this.attributeChips.length > 0 && this.attributeChips.every(r => this.isAttributeLoaded(r));
    }

    get allRelationsLoaded(): boolean {
        return this.relationChips.length > 0 && this.relationChips.every(r => this.isRelationLoaded(r));
    }

    get allRolesLoaded(): boolean {
        return this.roleChips.length > 0 && this.roleChips.every(r => this.isRoleLoaded(r));
    }

    loadAllAttributes(): void {
        if (!this.selectedType || !this.run || !this.visualiser || this.allAttributesLoaded) return;
        const iids = this.collectInstanceIidsOfType(this.selectedType.label);
        const markAll = () => this.attributeChips.forEach(r =>
            this.graphViewState.markConnectionLoaded(this.run!, this.selectedType!.label, r.label));
        if (iids.length === 0) { markAll(); return; }
        // One fetch for every attribute kind in a single pass, then reheat once.
        this.visualiser.freezeViewport();
        this.graphViewState.fetchAttributesOf(this.run, this.connectableType!, iids).then(() => {
            this.afterLoadAll(markAll);
        });
    }

    loadAllRelations(): void {
        if (!this.selectedType || !this.run || !this.visualiser || this.allRelationsLoaded) return;
        const iids = this.collectInstanceIidsOfType(this.selectedType.label);
        const markAll = () => this.relationChips.forEach(r =>
            this.graphViewState.markConnectionLoaded(this.run!, this.selectedType!.label, r.label));
        if (iids.length === 0) { markAll(); return; }
        this.visualiser.freezeViewport();
        // Load each relation kind the type participates in, then reheat once.
        Promise.all(this.relationChips.map(r =>
            this.graphViewState.fetchRelationsOfTypeForPlayers(this.run!, this.connectableType!, iids, r.label),
        )).then(() => this.afterLoadAll(markAll));
    }

    loadAllRoles(): void {
        if (!this.selectedType || !this.run || !this.visualiser || this.allRolesLoaded) return;
        const iids = this.collectInstanceIidsOfType(this.selectedType.label);
        const markAll = () => this.roleChips.forEach(r =>
            this.graphViewState.markConnectionLoaded(this.run!, this.selectedType!.label, r.label));
        if (iids.length === 0) { markAll(); return; }
        this.visualiser.freezeViewport();
        Promise.all(this.roleChips.map(r =>
            this.graphViewState.fetchRolePlayersOfTypeFor(this.run!, this.connectableType!, iids, r.shortName),
        )).then(() => this.afterLoadAll(markAll));
    }

    /** Shared post-load step: reheat, re-light, and mark the section loaded. */
    private afterLoadAll(markAll: () => void): void {
        this.visualiser?.reheat({ soft: true, preserveCamera: true });
        this.visualiser?.interactionHandler.recomputeHighlightSet();
        markAll();
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
