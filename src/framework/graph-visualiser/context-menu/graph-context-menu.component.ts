/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, inject, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from "@angular/core";
import { MatMenu, MatMenuModule, MatMenuTrigger } from "@angular/material/menu";
import { MatDividerModule } from "@angular/material/divider";
import { MatTooltipModule } from "@angular/material/tooltip";
import { Subscription } from "rxjs";
import { GraphViewState } from "../../../service/graph-view-state.service";
import { GraphStyleService } from "../../../service/graph-style.service";
import { RunOutputState } from "../../../service/query-page-state.service";
import { SchemaAttribute, SchemaConcept, SchemaRelation, SchemaRole, SchemaState } from "../../../service/schema-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { GraphVisualiser } from "../engine";
import { InspectableSelection } from "../engine/interaction-handler";

type SelectionKind = "entity" | "relation" | "attribute";
type Scope = "instance" | "type";

function typeKindFromSelectionKind(kind: SelectionKind): "entityType" | "relationType" | "attributeType" {
    switch (kind) {
        case "entity": return "entityType";
        case "relation": return "relationType";
        case "attribute": return "attributeType";
    }
}

interface AttributeRow {
    type: SchemaAttribute;
    label: string;
}

interface RelationRow {
    type: SchemaRelation;
    label: string;
}

interface RoleRow {
    role: SchemaRole;
    label: string;
    shortName: string;
}

/**
 * Right-click context menu for graph instance nodes.
 *
 * Two top-level entries — "Attributes" and "Links" — each open a submenu of
 * scoped actions. Every row inside a submenu (the bulk "Load all" row plus one
 * per individual attribute / relation / role type) carries two scope chips:
 *
 *   - "This instance"  → load just for the clicked IID
 *   - "All '<type>'"   → load for every in-graph IID of the clicked node's type
 *
 * The chip is the click target, so the user picks scope + target in one click
 * instead of navigating a separate "this instance" vs "all of type" branch.
 */
@Component({
    selector: "ts-graph-context-menu",
    templateUrl: "./graph-context-menu.component.html",
    styleUrls: ["./graph-context-menu.component.scss"],
    imports: [MatMenuModule, MatDividerModule, MatTooltipModule],
})
export class GraphContextMenuComponent implements OnChanges, OnDestroy {
    @Input() visualiser: GraphVisualiser | null = null;
    @Input() run: RunOutputState | null = null;

    @ViewChild("trigger", { static: true }) trigger!: MatMenuTrigger;
    @ViewChild(MatMenu, { static: true }) menu!: MatMenu;

    triggerPosition = { x: 0, y: 0 };
    target: InspectableSelection | null = null;

    /** Displayed label of the target node (the text drawn on the node in the
     *  canvas). Snapshot when the menu opens; falls back to the type label if
     *  the node has no display label. */
    nodeLabel = "";
    /** Owned attribute types of the target's type — populated when the menu
     *  opens. Empty for attribute targets. */
    attributeRows: AttributeRow[] = [];
    /** Relation types the target's (entity) type plays roles in. Empty for
     *  relation / attribute targets. */
    relationRows: RelationRow[] = [];
    /** Roles related by the target's (relation) type. Empty for entity /
     *  attribute targets. */
    roleRows: RoleRow[] = [];

    private graphViewState = inject(GraphViewState);
    private schemaState = inject(SchemaState);
    private styleService = inject(GraphStyleService);
    private snackbar = inject(SnackbarService);
    private sub: Subscription | null = null;

    /** A restricted, Google-Calendar-style palette for the per-type colour
     *  picker: 12 medium-tone hues, none too close to the default kind colours
     *  (pink / yellow / blue) and none too light or too dark. */
    readonly colorSwatches: readonly string[] = [
        "#e8403a", // bright red
        "#c2603c", // terracotta
        "#d98a3d", // orange
        "#a3d182", // pale lime
        "#0ae632", // vivid green
        "#3fb6a8", // teal
        "#1ec8d8", // bold cyan
        "#6464ff", // pure blue
        "#b45cf0", // purple
        "#e63bd0", // neon magenta
        "#b0b5c0", // light slate grey
        "#838897", // dark slate grey
    ];

    ngOnChanges(changes: SimpleChanges): void {
        if (changes["visualiser"]) {
            this.sub?.unsubscribe();
            this.sub = null;
            const v = this.visualiser;
            if (v) {
                this.sub = v.interactionHandler.nodeContextMenu$.subscribe(ev => {
                    this.triggerPosition = { x: ev.clientX, y: ev.clientY };
                    this.target = ev.target;
                    this.refreshRows();
                    // setTimeout so position styles flush to the DOM before
                    // mat-menu measures the trigger. Setting `_openedBy` to
                    // "mouse" lets FocusMonitor suppress the focus ring on
                    // the first auto-focused item — equivalent to what would
                    // happen if the user had clicked the trigger directly
                    // instead of us calling openMenu() programmatically.
                    setTimeout(() => {
                        (this.trigger as any)._openedBy = "mouse";
                        this.trigger.openMenu();
                    });
                });
            }
        }
    }

    ngOnDestroy(): void {
        this.sub?.unsubscribe();
    }

    /** Snapshot the per-type rows for the target. Called once per open so the
     *  submenus render against a fixed list — schema changes mid-menu are
     *  extremely unlikely but avoiding live reads also keeps the @for stable. */
    private refreshRows(): void {
        this.attributeRows = [];
        this.relationRows = [];
        this.roleRows = [];
        this.nodeLabel = "";
        if (!this.target) return;
        this.nodeLabel = this.lookupNodeLabel(this.target.instanceId) || this.target.typeLabel;
        if (this.isAttributeTarget) return;
        const type = this.lookupType(this.target);
        if (!type || type.kind === "attributeType") return;

        const owned = ("ownedAttributes" in type ? type.ownedAttributes : []) ?? [];
        this.attributeRows = owned
            .map(attr => ({ type: attr, label: attr.label }))
            .sort((a, b) => a.label.localeCompare(b.label));

        if (type.kind === "entityType" || type.kind === "relationType") {
            const schema = this.schemaState.value$.value;
            const seen = new Set<string>();
            const rels: RelationRow[] = [];
            for (const role of (type.playedRoles ?? []) as SchemaRole[]) {
                const relLabel = role.label.split(":")[0];
                if (seen.has(relLabel)) continue;
                seen.add(relLabel);
                const relType = schema?.relations[relLabel];
                if (relType) rels.push({ type: relType, label: relLabel });
            }
            rels.sort((a, b) => a.label.localeCompare(b.label));
            this.relationRows = rels;
        }

        if (type.kind === "relationType") {
            const related = (type as SchemaRelation).relatedRoles ?? [];
            this.roleRows = related
                .map(r => ({
                    role: r,
                    label: r.label,
                    shortName: r.label.split(":").at(-1) ?? r.label,
                }))
                .sort((a, b) => a.shortName.localeCompare(b.shortName));
        }
    }

    /** True when both action groups should be hidden — attributes don't have
     *  links/attributes of their own, only owners. */
    get isAttributeTarget(): boolean {
        return this.target?.kind === "attribute";
    }

    /** False when the target type owns no attributes — submenu would be empty. */
    get hasAttributes(): boolean {
        return this.attributeRows.length > 0;
    }

    /** False when the target's type plays no roles in any relation — the
     *  "Load relations" section would be empty. */
    get hasRelations(): boolean {
        return this.relationRows.length > 0;
    }

    /** False when the target isn't a relation (only relations relate roles) —
     *  the "Load links" section would be empty. */
    get hasRoles(): boolean {
        return this.roleRows.length > 0;
    }

    get typeChipLabel(): string {
        return this.target?.typeLabel ?? "";
    }

    // -- Per-type colour --

    /** The currently-applied type-level colour override for the target's type,
     *  or null if none is set (so no swatch shows a ring). */
    get currentTypeColor(): string | null {
        if (!this.target) return null;
        return this.styleService.typeStyles[this.target.typeLabel]?.color ?? null;
    }

    isColorSelected(hex: string): boolean {
        const c = this.currentTypeColor;
        return c != null && c.toLowerCase() === hex.toLowerCase();
    }

    /** Set the node colour for the target's whole type (toggle off if the same
     *  swatch is re-clicked), then re-render the graph. */
    setNodeColor(hex: string): void {
        if (!this.target) return;
        const typeLabel = this.target.typeLabel;
        if (this.isColorSelected(hex)) {
            this.styleService.setTypeStyle(typeLabel, { color: undefined });
        } else {
            this.styleService.setTypeStyle(typeLabel, { color: hex });
        }
        this.visualiser?.applyStyleUpdate();
    }

    // -- Loaded state --
    //
    // Two parallel sticky states, both per-tab:
    //   - "type" scope ("every '<type>'" chips): mirrors the type inspector —
    //     a connection is loaded once pulled in for *every* in-graph instance
    //     of the target's type. Keyed by (sourceTypeLabel, connectionLabel) in
    //     `loadedConnections`, so context menu and type details stay in sync.
    //   - "instance" scope ("here" chips): loaded for just the target IID,
    //     keyed by (instanceId, connectionLabel) in `loadedInstanceConnections`.
    //
    // A type-level load implies the instance is loaded too, so the "here"
    // checks OR in the type-level state.

    private isConnLoaded(connLabel: string): boolean {
        if (!this.run || !this.target) return false;
        return this.graphViewState.isConnectionLoaded(this.run, this.target.typeLabel, connLabel);
    }

    private isConnLoadedHere(connLabel: string): boolean {
        if (!this.run || !this.target) return false;
        return this.graphViewState.isInstanceConnectionLoaded(this.run, this.target.instanceId, connLabel)
            || this.isConnLoaded(connLabel);
    }

    /** Record a connection as loaded for the requested scope. */
    private markConnForScope(connLabel: string, scope: Scope): void {
        if (!this.run || !this.target) return;
        if (scope === "type") {
            this.graphViewState.markConnectionLoaded(this.run, this.target.typeLabel, connLabel);
        } else {
            this.graphViewState.markInstanceConnectionLoaded(this.run, this.target.instanceId, connLabel);
        }
    }

    isAttributeLoaded(row: AttributeRow): boolean { return this.isConnLoaded(row.label); }
    isRelationLoaded(row: RelationRow): boolean { return this.isConnLoaded(row.label); }
    isRoleLoaded(row: RoleRow): boolean { return this.isConnLoaded(row.label); }

    isAttributeLoadedHere(row: AttributeRow): boolean { return this.isConnLoadedHere(row.label); }
    isRelationLoadedHere(row: RelationRow): boolean { return this.isConnLoadedHere(row.label); }
    isRoleLoadedHere(row: RoleRow): boolean { return this.isConnLoadedHere(row.label); }

    get allAttributesLoaded(): boolean {
        return this.attributeRows.length > 0 && this.attributeRows.every(r => this.isAttributeLoaded(r));
    }

    get allAttributesLoadedHere(): boolean {
        return this.attributeRows.length > 0 && this.attributeRows.every(r => this.isAttributeLoadedHere(r));
    }

    get allRelationsLoaded(): boolean {
        return this.relationRows.length > 0 && this.relationRows.every(r => this.isRelationLoaded(r));
    }

    get allRelationsLoadedHere(): boolean {
        return this.relationRows.length > 0 && this.relationRows.every(r => this.isRelationLoadedHere(r));
    }

    get allRolesLoaded(): boolean {
        return this.roleRows.length > 0 && this.roleRows.every(r => this.isRoleLoaded(r));
    }

    get allRolesLoadedHere(): boolean {
        return this.roleRows.length > 0 && this.roleRows.every(r => this.isRoleLoadedHere(r));
    }

    // -- Actions --

    loadAllAttributes(scope: Scope): void {
        if (scope === "type" ? this.allAttributesLoaded : this.allAttributesLoadedHere) return; // sticky
        this.withTargetType((type, iids) => {
            this.runFetch(
                state => state.fetchAttributesOf(this.run!, type, iids),
                scope,
                () => this.attributeRows.forEach(r => this.markConnForScope(r.label, scope)),
            );
        }, scope);
    }

    loadAttribute(row: AttributeRow, scope: Scope): void {
        if (scope === "type" ? this.isAttributeLoaded(row) : this.isAttributeLoadedHere(row)) return; // sticky
        this.withTargetType((type, iids) => {
            this.runFetch(
                state => state.fetchAttributesOfTypeFor(this.run!, type, iids, row.label),
                scope,
                () => this.markConnForScope(row.label, scope),
            );
        }, scope);
    }

    loadAllRelations(scope: Scope): void {
        if (scope === "type" ? this.allRelationsLoaded : this.allRelationsLoadedHere) return; // sticky
        this.withTargetType((type, iids) => {
            this.runFetch(
                state => Promise.all(this.relationRows.map(r =>
                    state.fetchRelationsOfTypeForPlayers(this.run!, type, iids, r.label))).then(() => {}),
                scope,
                () => this.relationRows.forEach(r => this.markConnForScope(r.label, scope)),
            );
        }, scope);
    }

    loadAllRoles(scope: Scope): void {
        if (scope === "type" ? this.allRolesLoaded : this.allRolesLoadedHere) return; // sticky
        this.withTargetType((type, iids) => {
            if (type.kind !== "relationType") return;
            this.runFetch(
                state => Promise.all(this.roleRows.map(r =>
                    state.fetchRolePlayersOfTypeFor(this.run!, type, iids, r.shortName))).then(() => {}),
                scope,
                () => this.roleRows.forEach(r => this.markConnForScope(r.label, scope)),
            );
        }, scope);
    }

    loadRelation(row: RelationRow, scope: Scope): void {
        if (scope === "type" ? this.isRelationLoaded(row) : this.isRelationLoadedHere(row)) return; // sticky
        this.withTargetType((type, iids) => {
            this.runFetch(
                state => state.fetchRelationsOfTypeForPlayers(this.run!, type, iids, row.label),
                scope,
                () => this.markConnForScope(row.label, scope),
            );
        }, scope);
    }

    loadRole(row: RoleRow, scope: Scope): void {
        if (scope === "type" ? this.isRoleLoaded(row) : this.isRoleLoadedHere(row)) return; // sticky
        this.withTargetType((type, iids) => {
            if (type.kind !== "relationType") return;
            this.runFetch(
                state => state.fetchRolePlayersOfTypeFor(this.run!, type, iids, row.shortName),
                scope,
                () => this.markConnForScope(row.label, scope),
            );
        }, scope);
    }

    // -- Toggles: a connection chip loads when unloaded and UNLOADS (deletes
    //    the matching nodes/edges and reheats) when loaded. Works for both the
    //    "every '<type>'" (type) and "here" (instance) scopes. The "here" toggle
    //    keys off the instance-scope flag only, so a type-loaded connection's
    //    "here" chip (shown loaded via the OR) doesn't try to instance-unload. --

    /** Pure instance-scope loaded flags (no OR-in of the type-level state). */
    private isInstanceLoaded(connLabel: string): boolean {
        if (!this.run || !this.target) return false;
        return this.graphViewState.isInstanceConnectionLoaded(this.run, this.target.instanceId, connLabel);
    }

    /**
     * Unload one or more connections at the given scope: drop the matching
     * nodes/edges, clear the loaded flag, then reheat + close the menu once.
     */
    private unloadConnections(scope: Scope, rows: { label: string; unload: (v: GraphVisualiser, scope: Scope) => void }[]): void {
        if (!this.run || !this.visualiser || !this.target || rows.length === 0) return;
        const t = this.target;
        this.visualiser.freezeViewport();
        for (const r of rows) {
            r.unload(this.visualiser, scope);
            if (scope === "type") {
                this.graphViewState.removeConnectionLoaded(this.run, t.typeLabel, r.label);
            } else {
                this.graphViewState.removeInstanceConnectionLoaded(this.run, t.instanceId, r.label);
            }
        }
        this.visualiser.reheat({ soft: true, preserveCamera: true });
        this.trigger.closeMenu();
    }

    private get tk(): SelectionKind { return this.target!.kind; }
    private get tl(): string { return this.target!.typeLabel; }
    private get ti(): string { return this.target!.instanceId; }

    toggleAllAttributes(scope: Scope): void {
        const loaded = scope === "type" ? this.allAttributesLoaded : this.attributeRows.every(r => this.isInstanceLoaded(r.label));
        if (loaded) {
            this.unloadConnections(scope, this.attributeRows.map(r =>
                ({ label: r.label, unload: (v, s) => v.unloadAttribute(s, this.tk, this.tl, this.ti, r.label) })));
        } else {
            this.loadAllAttributes(scope);
        }
    }

    toggleAttribute(row: AttributeRow, scope: Scope): void {
        const loaded = scope === "type" ? this.isAttributeLoaded(row) : this.isInstanceLoaded(row.label);
        if (loaded) {
            this.unloadConnections(scope, [{ label: row.label, unload: (v, s) => v.unloadAttribute(s, this.tk, this.tl, this.ti, row.label) }]);
        } else {
            this.loadAttribute(row, scope);
        }
    }

    toggleAllRelations(scope: Scope): void {
        const loaded = scope === "type" ? this.allRelationsLoaded : this.relationRows.every(r => this.isInstanceLoaded(r.label));
        if (loaded) {
            this.unloadConnections(scope, this.relationRows.map(r =>
                ({ label: r.label, unload: (v, s) => v.unloadRelation(s, this.tk, this.tl, this.ti, r.label) })));
        } else {
            this.loadAllRelations(scope);
        }
    }

    toggleRelation(row: RelationRow, scope: Scope): void {
        const loaded = scope === "type" ? this.isRelationLoaded(row) : this.isInstanceLoaded(row.label);
        if (loaded) {
            this.unloadConnections(scope, [{ label: row.label, unload: (v, s) => v.unloadRelation(s, this.tk, this.tl, this.ti, row.label) }]);
        } else {
            this.loadRelation(row, scope);
        }
    }

    toggleAllRoles(scope: Scope): void {
        const loaded = scope === "type" ? this.allRolesLoaded : this.roleRows.every(r => this.isInstanceLoaded(r.label));
        if (loaded) {
            this.unloadConnections(scope, this.roleRows.map(r =>
                ({ label: r.label, unload: (v, s) => v.unloadRole(s, this.tk, this.tl, this.ti, r.shortName) })));
        } else {
            this.loadAllRoles(scope);
        }
    }

    toggleRole(row: RoleRow, scope: Scope): void {
        const loaded = scope === "type" ? this.isRoleLoaded(row) : this.isInstanceLoaded(row.label);
        if (loaded) {
            this.unloadConnections(scope, [{ label: row.label, unload: (v, s) => v.unloadRole(s, this.tk, this.tl, this.ti, row.shortName) }]);
        } else {
            this.loadRole(row, scope);
        }
    }

    /** Common pre-flight: look up the schema type for the target, collect IIDs
     *  for the requested scope, and forward both to the action. Attribute
     *  targets and unknown types short-circuit. */
    private withTargetType(op: (type: SchemaConcept, iids: string[]) => void, scope: Scope): void {
        if (!this.target) return;
        const type = this.lookupType(this.target);
        if (!type || type.kind === "attributeType") return;
        const iids = scope === "instance"
            ? [this.target.instanceId]
            : this.collectInstanceIidsOfType(this.target.typeLabel);
        if (iids.length === 0) return;
        op(type, iids);
    }

    private lookupType(target: InspectableSelection): SchemaConcept | null {
        const schema = this.schemaState.value$.value;
        if (!schema) {
            this.snackbar.errorPersistent("Schema not loaded");
            return null;
        }
        const kind = typeKindFromSelectionKind(target.kind);
        switch (kind) {
            case "entityType": return schema.entities[target.typeLabel] ?? null;
            case "relationType": return schema.relations[target.typeLabel] ?? null;
            case "attributeType": return schema.attributes[target.typeLabel] ?? null;
        }
    }

    /** Find the displayed label of the graph node whose concept iid matches.
     *  Returns "" when no matching node is in the graph or it has no label. */
    private lookupNodeLabel(instanceId: string): string {
        if (!this.visualiser) return "";
        for (const key of this.visualiser.graph.nodes()) {
            try {
                const attrs = this.visualiser.graph.getNodeAttributes(key);
                const concept: any = attrs?.["metadata"]?.concept;
                if (concept?.iid === instanceId) {
                    return (attrs?.["label"] as string) ?? "";
                }
            } catch { /* missing metadata mid-mutation */ }
        }
        return "";
    }

    /** Walk the graph, gathering IIDs of every entity/relation node whose
     *  concept type label matches. */
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

    private runFetch(op: (state: GraphViewState) => Promise<void>, scope: Scope, markLoaded?: () => void): void {
        if (!this.run || !this.visualiser || !this.target) return;
        const target = this.target;
        this.visualiser.freezeViewport();
        op(this.graphViewState).then(() => {
            // A context-menu load is additive: keep whatever the panel is
            // currently inspecting rather than yanking it to the right-clicked
            // node (which blanked/jumped the Explorer). Only when nothing is
            // selected do we take focus, so the freshly-loaded neighbourhood is
            // highlighted; otherwise just recompute the highlight so the new
            // nodes light up under the existing selection.
            if (this.visualiser?.hasActiveSelection) {
                this.visualiser?.refreshHighlight();
            } else if (scope === "type") {
                this.visualiser?.focusType(target.kind, target.typeLabel, target.instanceId);
            } else {
                this.visualiser?.focusInstance(target.kind, target.typeLabel, target.instanceId);
            }
            this.visualiser?.reheat({ soft: true, preserveCamera: true });
            // Record the load so the relevant chips show the sticky "loaded"
            // fill next time the menu opens (and, for "type" scope, stay in
            // sync with the type-details inspector, which reads the same state).
            markLoaded?.();
        });
        this.trigger.closeMenu();
    }
}
