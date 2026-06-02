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
import { SchemaConcept, SchemaState } from "../../../service/schema-state.service";
import { AttributeData, InstanceDetailViewModel, LinkData, OwnerData, RelationInstanceData } from "../../../service/instance-detail-view-model.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { GraphVisualiser } from "../engine";

interface InspectorBreadcrumb {
    /** SchemaConcept kind — matches the `kind` field on entity/relation/attribute schema concepts. */
    typeKind: "entityType" | "relationType" | "attributeType";
    typeLabel: string;
    /** IID for entity/relation, value for attribute. */
    instanceId: string;
}

type SelectionKind = "entity" | "relation" | "attribute";

function selectionKindFromTypeKind(typeKind: string): SelectionKind | null {
    switch (typeKind) {
        case "entityType": return "entity";
        case "relationType": return "relation";
        case "attributeType": return "attribute";
        default: return null;
    }
}

@Component({
    selector: "ts-graph-inspector",
    templateUrl: "./graph-inspector.component.html",
    styleUrls: [
        // Reuse the data-side inspector's section/card/attribute styling so we
        // don't duplicate ~600 lines of SCSS. Angular's view encapsulation
        // keeps them scoped to this component. Listed first so the
        // graph-inspector's own SCSS (below) wins on the :host overrides.
        "../../../module/data/instance-detail/instance-detail.component.scss",
        "./graph-inspector.component.scss",
    ],
    providers: [InstanceDetailViewModel],
    imports: [
        CommonModule,
        MatProgressSpinnerModule,
        MatButtonModule,
        MatTooltipModule,
    ],
})
export class GraphInspectorComponent implements OnChanges {
    @Input() type: SchemaConcept | null = null;
    @Input() instanceIID: string | null = null;
    @Input() run: RunOutputState | null = null;
    @Input() visualiser: GraphVisualiser | null = null;

    vm = inject(InstanceDetailViewModel);
    // null when no selection — only valid to access vm.* fields when this is true
    get hasSelection(): boolean { return !!(this.type && this.instanceIID); }

    selectedRelationType: string | null = null;
    linksCollapsed = false;
    attributesCollapsed = false;
    relationsCollapsed = false;
    ownersCollapsed = false;

    /** Exploration trail leading to the current selection. */
    get breadcrumbs(): InspectorBreadcrumb[] { return this._breadcrumbs; }
    set breadcrumbs(value: InspectorBreadcrumb[]) {
        this._breadcrumbs = value;
        this.syncHighlightAnchors();
    }
    private _breadcrumbs: InspectorBreadcrumb[] = [];

    private syncHighlightAnchors(): void {
        if (!this.visualiser) return;
        const specs: { kind: SelectionKind; typeLabel: string; instanceId: string }[] = [];
        for (const b of this._breadcrumbs) {
            const kind = selectionKindFromTypeKind(b.typeKind);
            if (!kind) continue;
            specs.push({ kind, typeLabel: b.typeLabel, instanceId: b.instanceId });
        }
        this.visualiser.setHighlightAnchorsByInstance(specs);
    }

    /**
     * Set immediately before any Explore- or breadcrumb-driven selection so
     * that when the resulting selection$ emission arrives via @Input we can
     * tell it apart from a direct canvas click (which clears the trail).
     */
    private pendingSelection: { kind: SelectionKind; typeLabel: string; instanceId: string } | null = null;

    private graphViewState = inject(GraphViewState);
    private schemaState = inject(SchemaState);
    private snackbar = inject(SnackbarService);
    private clipboard = inject(Clipboard);

    ngOnChanges(changes: SimpleChanges) {
        if ((changes["type"] || changes["instanceIID"]) && this.type && this.instanceIID) {
            this.reconcileBreadcrumbsOnSelectionChange(changes);
            this.selectedRelationType = null;
            this.vm.initialize(this.type, this.instanceIID);
        } else if ((changes["type"] || changes["instanceIID"]) && (!this.type || !this.instanceIID)) {
            // Selection cleared (clicked empty canvas) — drop the trail too.
            this.breadcrumbs = [];
            this.pendingSelection = null;
        }
    }

    private reconcileBreadcrumbsOnSelectionChange(changes: SimpleChanges): void {
        const expected = this.pendingSelection;
        this.pendingSelection = null;
        if (expected
            && this.type && this.instanceIID
            && selectionKindFromTypeKind(this.type.kind) === expected.kind
            && this.type.label === expected.typeLabel
            && this.instanceIID === expected.instanceId) {
            return; // came from Explore or breadcrumb click — trail was already updated
        }

        // Click-driven selection.
        // 1) If the clicked node is already in the trail, treat it as
        //    back-navigation — truncate the trail to that point (same as
        //    clicking the breadcrumb itself).
        const trailIndex = this.breadcrumbs.findIndex(b =>
            b.typeKind === this.type!.kind
            && b.typeLabel === this.type!.label
            && b.instanceId === this.instanceIID
        );
        if (trailIndex >= 0) {
            this.breadcrumbs = this.breadcrumbs.slice(0, trailIndex);
            return;
        }

        // 2) If the clicked node was a highlighted neighbor of what was
        //    previously selected, treat it like an implicit Explore — append
        //    the previous selection onto the trail.
        const fromHighlight = this.visualiser?.interactionHandler.lastSelectionWasFromHighlight ?? false;
        if (fromHighlight) {
            const prevType = (changes["type"]?.previousValue as SchemaConcept | undefined) ?? this.type;
            const prevIID = (changes["instanceIID"]?.previousValue as string | undefined) ?? this.instanceIID;
            if (prevType && prevIID) {
                this.breadcrumbs = [...this.breadcrumbs, {
                    typeKind: prevType.kind,
                    typeLabel: prevType.label,
                    instanceId: prevIID,
                }];
            }
            return;
        }

        // 3) Unrelated click — fresh trail.
        this.breadcrumbs = [];
    }

    get filteredRelations(): RelationInstanceData[] {
        if (this.selectedRelationType === null) return this.vm.allRelations;
        return this.vm.allRelations.filter(r => r.relationTypeLabel === this.selectedRelationType);
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
        this.visualiser?.freezeViewport();
        this.graphViewState
            .fetchAttributesOf(this.run, this.type, [this.instanceIID])
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                // Newly-added attributes are neighbors of the current selection
                // but weren't around when collectHighlightedNeighbors last ran,
                // so re-evaluate to bring them into the highlight set.
                this.visualiser?.interactionHandler.recomputeHighlightSet();
            });
    }

    /** Add only the attributes of a specific type (one row in the Attributes table). */
    addAttribute(attr: AttributeData) {
        if (!this.run || !this.type || !this.instanceIID) return;
        this.visualiser?.freezeViewport();
        this.graphViewState
            .fetchAttributesOfTypeFor(this.run, this.type, [this.instanceIID], attr.type)
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.interactionHandler.recomputeHighlightSet();
            });
    }

    addAllRelations() {
        if (!this.run || !this.type || !this.instanceIID) return;
        this.visualiser?.freezeViewport();
        this.graphViewState
            .fetchLinksOf(this.run, this.type, [this.instanceIID])
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.interactionHandler.recomputeHighlightSet();
            });
    }

    /** Add the relation to the graph (with all its role players) and select it. */
    exploreRelation(rel: RelationInstanceData) {
        if (!this.run) return;
        this.prepareExplore("relation", rel.relationTypeLabel, rel.relationIID);
        this.visualiser?.freezeViewport();
        this.graphViewState
            .fetchRelation(this.run, rel.relationIID)
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.selectInstance("relation", rel.relationTypeLabel, rel.relationIID);
            });
    }

    addLink(_link: LinkData) {
        // No parent relation in scope for a single link row inside a relation
        // card, so fall back to refetching all of the current instance's
        // relations — graph dedupes the rest.
        this.addAllRelations();
    }

    /**
     * Add a role-player (and its containing relation) to the graph and
     * select it. Attributes are intentionally left out — that's what the
     * inline attribute list in the Inspector itself is for.
     */
    explorePlayer(parentRel: RelationInstanceData, link: LinkData) {
        this.exploreLinkInRelation(parentRel.relationIID, link);
    }

    /**
     * Explore one role-player of the relation the Inspector is currently
     * showing. The inspected instance itself is the parent relation, so we
     * use `this.instanceIID` as the relation IID.
     */
    exploreOwnLink(link: LinkData) {
        if (!this.instanceIID) return;
        this.exploreLinkInRelation(this.instanceIID, link);
    }

    private exploreLinkInRelation(relationIID: string, link: LinkData): void {
        if (!this.run) return;
        const playerType = this.lookupType(link.playerTypeLabel);
        if (!playerType) return;
        const playerKind: "entity" | "relation" =
            playerType.kind === "relationType" ? "relation" : "entity";
        this.prepareExplore(playerKind, link.playerTypeLabel, link.playerIID);
        this.visualiser?.freezeViewport();
        // Pass the currently-inspected instance as `originalIID` so the
        // query pulls in the role edge back to it — keeps the new player
        // connected to whatever the user came from (no-op when the user is
        // already viewing the relation itself).
        this.graphViewState
            .fetchPlayerInRelation(this.run, relationIID, link.playerIID, this.instanceIID ?? undefined)
            .then(() => {
                this.visualiser?.reheat({ soft: true, preserveCamera: true });
                this.visualiser?.selectInstance(playerKind, link.playerTypeLabel, link.playerIID);
            });
    }

    /**
     * Push the currently-inspected instance onto the breadcrumb trail and mark
     * the upcoming selection as expected. Call right before kicking off the
     * fetch that culminates in a `selectInstance` call.
     */
    private prepareExplore(kind: SelectionKind, typeLabel: string, instanceId: string): void {
        if (this.type && this.instanceIID) {
            this.breadcrumbs = [...this.breadcrumbs, {
                typeKind: this.type.kind,
                typeLabel: this.type.label,
                instanceId: this.instanceIID,
            }];
        }
        this.pendingSelection = { kind, typeLabel, instanceId };
    }

    /**
     * Navigate back to a breadcrumb. Truncates the trail to the breadcrumbs
     * that preceded the target (so re-clicking the same crumb is a no-op) and
     * re-selects the target instance in the graph.
     */
    navigateToBreadcrumb(index: number): void {
        const target = this.breadcrumbs[index];
        if (!target) return;
        const kind = selectionKindFromTypeKind(target.typeKind);
        if (!kind) return;
        this.breadcrumbs = this.breadcrumbs.slice(0, index);
        this.pendingSelection = { kind, typeLabel: target.typeLabel, instanceId: target.instanceId };
        this.visualiser?.selectInstance(kind, target.typeLabel, target.instanceId);
    }

    private lookupType(label: string): SchemaConcept | null {
        const schema = this.schemaState.value$.value;
        if (!schema) {
            this.snackbar.errorPersistent("Schema not loaded");
            return null;
        }
        return schema.entities[label] || schema.relations[label] || null;
    }

    getTypeIconClass(typeKind: string, filled: boolean): string {
        const style = filled ? "fa-regular" : "fa-solid";
        switch (typeKind) {
            case "entityType": return `${style} fa-square entity`;
            case "relationType": return `${style} fa-diamond relation`;
            case "attributeType": return `${style} fa-circle attribute`;
            default: return `${style} fa-question`;
        }
    }
}
