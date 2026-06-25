/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, inject, Input, OnChanges, SimpleChanges } from "@angular/core";
import { CommonModule } from "@angular/common";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltip, MatTooltipModule } from "@angular/material/tooltip";
import { Clipboard } from "@angular/cdk/clipboard";
import { GraphViewState } from "../../../service/graph-view-state.service";
import { RunOutputState } from "../../../service/query-page-state.service";
import { SchemaConcept, SchemaRelation, SchemaRole } from "../../../service/schema-state.service";
import { AttributeData, InstanceDetailState, LinkData, RelationInstanceData } from "../../../service/instance-detail-state.service";
import { GraphVisualiser } from "../engine";

/** Sticky-state key for "this instance's link to a specific relation instance
 *  has been loaded". Namespaced with a `rel:` prefix so a relation IID can
 *  never be confused with a relation/role *type* label stored in the same
 *  per-instance set (those track whole-type loads). */
function relationInstanceKey(relationIID: string): string {
    return `rel:${relationIID}`;
}

/** Sticky-state key for "a specific role-player link (relation IID + player
 *  IID) has been loaded". Adding a relation pulls in all its player links, so
 *  each is marked with this key; a single role-player add marks just one. */
function linkInstanceKey(relationIID: string, playerIID: string): string {
    return `link:${relationIID}:${playerIID}`;
}

@Component({
    selector: "ts-graph-instance-explorer",
    templateUrl: "./graph-instance-explorer.component.html",
    styleUrls: [
        // Reuse the data-side inspector's section/card/attribute styling so we
        // don't duplicate ~600 lines of SCSS. Angular's view encapsulation
        // keeps them scoped to this component. Listed first so the
        // explorer's own SCSS (below) wins on the :host overrides.
        "../../../module/data/instance-detail/instance-detail.component.scss",
        "./graph-instance-explorer.component.scss",
    ],
    providers: [InstanceDetailState],
    imports: [
        CommonModule,
        MatProgressSpinnerModule,
        MatButtonModule,
        MatTooltipModule,
    ],
})
export class GraphInstanceExplorerComponent implements OnChanges {
    @Input() type: SchemaConcept | null = null;
    @Input() instanceIID: string | null = null;
    @Input() run: RunOutputState | null = null;
    @Input() visualiser: GraphVisualiser | null = null;

    state = inject(InstanceDetailState);
    // null when no selection — only valid to access state.* fields when this is true
    get hasSelection(): boolean { return !!(this.type && this.instanceIID); }

    selectedRelationType: string | null = null;
    linksCollapsed = false;
    attributesCollapsed = false;
    relationsCollapsed = false;
    ownersCollapsed = false;

    private graphViewState = inject(GraphViewState);
    private clipboard = inject(Clipboard);

    ngOnChanges(changes: SimpleChanges) {
        if ((changes["type"] || changes["instanceIID"]) && this.type && this.instanceIID) {
            this.selectedRelationType = null;
            this.state.initialize(this.type, this.instanceIID);
        }
    }

    get filteredRelations(): RelationInstanceData[] {
        if (this.selectedRelationType === null) return this.state.allRelations;
        return this.state.allRelations.filter(r => r.relationTypeLabel === this.selectedRelationType);
    }

    selectRelationType(type: string | null) {
        this.selectedRelationType = type;
    }

    copyValue(value: string, tooltip: MatTooltip) {
        this.clipboard.copy(value);
        tooltip.show(0);
        setTimeout(() => tooltip.hide(0), 1000);
    }

    addAllAttributes() {
        if (!this.run || !this.type || !this.instanceIID) return;
        if (!this.graphViewState.guardExploration()) return;
        this.visualiser?.freezeViewport();
        this.graphViewState
            .fetchAttributesOf(this.run, this.type, [this.instanceIID])
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                // Newly-added attributes are neighbors of the current selection
                // but weren't around when collectHighlightedNeighbors last ran,
                // so re-evaluate to bring them into the highlight set.
                this.visualiser?.interactionHandler.recomputeHighlightSet();
                // Mark every owned attribute type loaded for THIS instance, so
                // the context menu's "here" attribute chips show as loaded.
                this.ownedAttributeLabels().forEach(label => this.markInstanceLoaded(label));
            });
    }

    /** Add only the attributes of a specific type (one row in the Attributes table). */
    addAttribute(attr: AttributeData) {
        if (!this.run || !this.type || !this.instanceIID) return;
        if (!this.graphViewState.guardExploration()) return;
        this.visualiser?.freezeViewport();
        this.graphViewState
            .fetchAttributesOfTypeFor(this.run, this.type, [this.instanceIID], attr.type)
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.interactionHandler.recomputeHighlightSet();
                this.markInstanceLoaded(attr.type);
            });
    }

    /** True once this attribute type has been loaded into the graph for the
     *  currently inspected instance. */
    isAttributeAdded(attr: AttributeData): boolean {
        if (!this.run || !this.instanceIID) return false;
        return this.graphViewState.isInstanceConnectionLoaded(this.run, this.instanceIID, attr.type);
    }

    /** Frame this instance's attribute(s) of the given type in the graph
     *  (without changing the panel selection). */
    revealAttribute(attr: AttributeData) {
        if (!this.type || !this.instanceIID || this.type.kind === "attributeType") return;
        const ownerKind = this.type.kind === "relationType" ? "relation" : "entity";
        const keys = this.visualiser?.attributeNodeKeysOf(ownerKind, this.type.label, this.instanceIID, attr.type) ?? [];
        this.visualiser?.revealNodes(keys);
    }

    addAllRelations() {
        if (!this.run || !this.type || !this.instanceIID) return;
        if (!this.graphViewState.guardExploration()) return;
        this.visualiser?.freezeViewport();
        this.graphViewState
            .fetchLinksOf(this.run, this.type, [this.instanceIID])
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.interactionHandler.recomputeHighlightSet();
                // `fetchLinksOf` pulls in every relation the instance plays a
                // role in (and, for a relation, every role player), so mark all
                // the matching connection labels loaded for this instance.
                this.connectionLabelsForLinks().forEach(label => this.markInstanceLoaded(label));
            });
    }

    /** Record `connLabel` as loaded for the currently-inspected instance so the
     *  context menu's "here" chips reflect what the Explorer has loaded. */
    private markInstanceLoaded(connLabel: string): void {
        if (!this.run || !this.instanceIID) return;
        this.graphViewState.markInstanceConnectionLoaded(this.run, this.instanceIID, connLabel);
    }

    /** Attribute type labels the inspected type owns — matches the context
     *  menu's attribute rows. */
    private ownedAttributeLabels(): string[] {
        const owned = (this.type && "ownedAttributes" in this.type ? this.type.ownedAttributes : []) ?? [];
        return owned.map(a => a.label);
    }

    /** Connection labels loaded by `fetchLinksOf` — relation-type labels for an
     *  entity (the relations it plays a role in), scoped role labels for a
     *  relation (the roles it relates). Mirrors the context menu's row keys. */
    private connectionLabelsForLinks(): string[] {
        if (!this.type) return [];
        if (this.type.kind === "relationType") {
            return ((this.type as SchemaRelation).relatedRoles ?? []).map((r: SchemaRole) => r.label);
        }
        const playedRoles = ("playedRoles" in this.type ? this.type.playedRoles : []) as SchemaRole[];
        const seen = new Set<string>();
        for (const role of playedRoles ?? []) {
            seen.add(role.label.split(":")[0]);
        }
        return Array.from(seen);
    }

    /** Add a relation instance (with all its role players) to the graph,
     *  keeping the current instance selected rather than navigating to it. */
    addRelation(rel: RelationInstanceData) {
        if (!this.run || this.isRelationAdded(rel)) return;
        if (!this.graphViewState.guardExploration()) return;
        this.visualiser?.freezeViewport();
        this.graphViewState
            .fetchRelation(this.run, rel.relationIID)
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.interactionHandler.recomputeHighlightSet();
                // Track this specific relation as loaded for the current
                // instance: the key is the (instance IID, relation IID) pair.
                // This is stricter than "is the relation node present" — it
                // records that *this instance's link to this relation* was
                // retrieved, which is what `fetchRelation` actually pulls in.
                this.markInstanceLoaded(relationInstanceKey(rel.relationIID));
                // `fetchRelation` also pulls in every role player + role edge,
                // so mark each of the relation's links loaded too.
                rel.links.forEach(link =>
                    this.markInstanceLoaded(linkInstanceKey(rel.relationIID, link.playerIID)));
                // If this completes every relation of its type for this
                // instance, mark the relation *type* loaded too — that's the
                // key the context menu's relation chip reads, so loading the
                // last (or only) relation of a type ticks + disables that chip.
                this.maybeMarkRelationTypeLoaded(rel.relationTypeLabel);
            });
    }

    /** Add every relation of `relationTypeLabel` the current instance plays a
     *  role in, in one fetch — the per-card "Add all to graph" affordance. */
    addAllRelationsOfType(relationTypeLabel: string) {
        if (!this.run || !this.type || !this.instanceIID) return;
        if (!this.graphViewState.guardExploration()) return;
        const ofType = this.state.allRelations.filter(r => r.relationTypeLabel === relationTypeLabel);
        this.visualiser?.freezeViewport();
        this.graphViewState
            .fetchRelationsOfTypeForPlayers(this.run, this.type, [this.instanceIID], relationTypeLabel)
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.interactionHandler.recomputeHighlightSet();
                // Mark each relation (and its links) of this type loaded for the
                // instance, then the relation type itself.
                ofType.forEach(rel => {
                    this.markInstanceLoaded(relationInstanceKey(rel.relationIID));
                    rel.links.forEach(link =>
                        this.markInstanceLoaded(linkInstanceKey(rel.relationIID, link.playerIID)));
                });
                this.maybeMarkRelationTypeLoaded(relationTypeLabel);
            });
    }

    /** True once every relation of `relationTypeLabel` for the current instance
     *  has been added — hides the per-card "Add all to graph" button. */
    allRelationsOfTypeAdded(relationTypeLabel: string): boolean {
        const ofType = this.state.allRelations.filter(r => r.relationTypeLabel === relationTypeLabel);
        return ofType.length > 0 && ofType.every(r => this.isRelationAdded(r));
    }

    /** Mark a relation TYPE loaded for the current instance once all of its
     *  relation instances have been individually added — keeping the context
     *  menu's per-type relation chip in sync with individual Explorer adds. */
    private maybeMarkRelationTypeLoaded(relationTypeLabel: string): void {
        const ofType = this.state.allRelations.filter(r => r.relationTypeLabel === relationTypeLabel);
        if (ofType.length > 0 && ofType.every(r => this.isRelationAdded(r))) {
            this.markInstanceLoaded(relationTypeLabel);
        }
    }

    /** True once this relation has been added to the graph for the currently
     *  inspected instance (its source→relation link was loaded). */
    isRelationAdded(rel: RelationInstanceData): boolean {
        if (!this.run || !this.instanceIID) return false;
        return this.graphViewState.isInstanceConnectionLoaded(
            this.run, this.instanceIID, relationInstanceKey(rel.relationIID));
    }

    /** Frame the already-added relation node in the graph (without changing the
     *  panel selection). */
    revealRelation(rel: RelationInstanceData) {
        const key = this.visualiser?.instanceNodeKey("relation", rel.relationTypeLabel, rel.relationIID);
        if (key) this.visualiser?.revealNodes([key]);
    }

    /** Frame the inspected instance itself in the graph (it's always present).
     *  No selection change — just pan/zoom to it. */
    revealSelf() {
        if (!this.type || !this.instanceIID) return;
        const kind = this.type.kind === "relationType" ? "relation"
            : this.type.kind === "attributeType" ? "attribute" : "entity";
        const key = this.visualiser?.instanceNodeKey(kind, this.type.label, this.instanceIID);
        if (key) this.visualiser?.revealNodes([key]);
    }

    addLink(_link: LinkData) {
        // No parent relation in scope for a single link row inside a relation
        // card, so fall back to refetching all of the current instance's
        // relations — graph dedupes the rest.
        this.addAllRelations();
    }

    /** Add a role-player (and its containing relation) to the graph, keeping
     *  the current instance selected. */
    addPlayer(parentRel: RelationInstanceData, link: LinkData) {
        this.addLinkInRelation(parentRel.relationIID, link);
    }

    /** Add a role-player of the relation the Explorer is currently showing.
     *  The inspected instance itself is the parent relation. */
    addOwnLink(link: LinkData) {
        if (!this.instanceIID) return;
        this.addLinkInRelation(this.instanceIID, link);
    }

    private addLinkInRelation(relationIID: string, link: LinkData): void {
        if (!this.run) return;
        this.visualiser?.freezeViewport();
        // Pass the currently-inspected instance as `originalIID` so the query
        // pulls in the role edge back to it — keeps the new player connected to
        // whatever the user is viewing (no-op when viewing the relation itself).
        this.graphViewState
            .fetchPlayerInRelation(this.run, relationIID, link.playerIID, this.instanceIID ?? undefined)
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.interactionHandler.recomputeHighlightSet();
                this.markInstanceLoaded(linkInstanceKey(relationIID, link.playerIID));
            });
    }

    /** True once this role-player link has been loaded for the current instance
     *  — either directly, or implicitly by adding its parent relation. */
    isLinkAdded(relationIID: string, link: LinkData): boolean {
        if (!this.run || !this.instanceIID) return false;
        return this.graphViewState.isInstanceConnectionLoaded(
            this.run, this.instanceIID, linkInstanceKey(relationIID, link.playerIID));
    }

    /** Frame the already-added role-player in the graph (without changing the
     *  panel selection). */
    revealLink(link: LinkData) {
        const key = this.visualiser?.nodeKeyByIid(link.playerIID);
        if (key) this.visualiser?.revealNodes([key]);
    }

}
