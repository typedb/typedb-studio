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
import { GraphViewState, GraphViewTab } from "../../../service/graph-view-state.service";
import { SchemaConcept, SchemaState } from "../../../service/schema-state.service";
import { InstanceDetailViewModel, LinkData, OwnerData, RelationInstanceData } from "../../../service/instance-detail-view-model.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { GraphVisualiser } from "../engine";

@Component({
    selector: "ts-graph-inspector",
    templateUrl: "./graph-inspector.component.html",
    styleUrls: [
        "./graph-inspector.component.scss",
        // Reuse the data-side inspector's section/card/attribute styling so we
        // don't duplicate ~600 lines of SCSS. Angular's view encapsulation
        // keeps them scoped to this component.
        "../../../module/data/instance-detail/instance-detail.component.scss",
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
    @Input() tab: GraphViewTab | null = null;
    @Input() visualiser: GraphVisualiser | null = null;

    vm = inject(InstanceDetailViewModel);
    // null when no selection — only valid to access vm.* fields when this is true
    get hasSelection(): boolean { return !!(this.type && this.instanceIID); }

    selectedRelationType: string | null = null;
    typeCollapsed = false;
    linksCollapsed = false;
    attributesCollapsed = false;
    relationsCollapsed = false;
    ownersCollapsed = false;

    private graphViewState = inject(GraphViewState);
    private schemaState = inject(SchemaState);
    private snackbar = inject(SnackbarService);
    private clipboard = inject(Clipboard);

    ngOnChanges(changes: SimpleChanges) {
        if ((changes["type"] || changes["instanceIID"]) && this.type && this.instanceIID) {
            this.selectedRelationType = null;
            this.vm.initialize(this.type, this.instanceIID);
        }
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
        if (!this.tab || !this.type || !this.instanceIID) return;
        this.graphViewState
            .fetchAttributesOf(this.tab.run, this.type, [this.instanceIID])
            .then(() => this.visualiser?.reheat());
    }

    addAllRelations() {
        if (!this.tab || !this.type || !this.instanceIID) return;
        this.graphViewState
            .fetchLinksOf(this.tab.run, this.type, [this.instanceIID])
            .then(() => this.visualiser?.reheat());
    }

    addRelation(rel: RelationInstanceData) {
        if (!this.tab) return;
        const relationType = this.lookupType(rel.relationTypeLabel);
        if (!relationType) return;
        this.graphViewState
            .fetchLinksOf(this.tab.run, relationType, [rel.relationIID])
            .then(() => this.visualiser?.reheat());
    }

    addLink(link: LinkData) {
        // Add the player and the relation edge by fetching this relation's links.
        // We don't have the parent relation in scope for a single link row inside
        // a relation card, so fall back to refetching all of the current
        // instance's relations — graph dedupes the rest.
        this.addAllRelations();
    }

    addOwnLink(_link: LinkData) {
        // Own-links live on a relation instance — adding any one player pulls in
        // the relation's full link set anyway, so just refetch.
        if (!this.tab || !this.type || !this.instanceIID) return;
        this.graphViewState
            .fetchLinksOf(this.tab.run, this.type, [this.instanceIID])
            .then(() => this.visualiser?.reheat());
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
