/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, inject, Input, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltip, MatTooltipModule } from "@angular/material/tooltip";
import { Clipboard } from "@angular/cdk/clipboard";
import { SchemaConcept, SchemaState } from "../../../service/schema-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { BreadcrumbItem, DataEditorState } from "../../../service/data-editor-state.service";
import { InstanceDetailState, LinkData, OwnerData, RelationInstanceData } from "../../../service/instance-detail-state.service";

@Component({
    selector: "ts-instance-detail",
    templateUrl: "./instance-detail.component.html",
    styleUrls: ["./instance-detail.component.scss", "./instance-detail.host.scss"],
    providers: [InstanceDetailState],
    imports: [
        CommonModule,
        MatProgressSpinnerModule,
        MatButtonModule,
        MatTooltipModule,
    ],
})
export class InstanceDetailComponent implements OnInit {
    @Input({ required: true }) type!: SchemaConcept;
    @Input({ required: true }) instanceIID!: string;
    @Input() breadcrumbs: BreadcrumbItem[] = [];

    state = inject(InstanceDetailState);

    // Per-component UI state
    selectedRelationType: string | null = null; // null = "All"
    typeCollapsed = false;
    linksCollapsed = false;
    attributesCollapsed = false;
    relationsCollapsed = false;
    ownersCollapsed = false;

    private snackbar = inject(SnackbarService);
    private clipboard = inject(Clipboard);
    private schemaState = inject(SchemaState);
    private dataEditorState = inject(DataEditorState);

    ngOnInit() {
        this.state.initialize(this.type, this.instanceIID);
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

    openLinkDetail(link: LinkData, event: Event) {
        event.stopPropagation();
        this.navigateToInstance(link.playerTypeLabel, link.playerIID);
    }

    openOwnLinkDetail(link: LinkData, event: Event) {
        event.stopPropagation();
        this.navigateToInstance(link.playerTypeLabel, link.playerIID);
    }

    openRelationDetail(instance: RelationInstanceData, event: Event) {
        event.stopPropagation();
        this.navigateToInstance(instance.relationTypeLabel, instance.relationIID);
    }

    openOwnerDetail(owner: OwnerData, event: Event) {
        event.stopPropagation();
        this.navigateToInstance(owner.ownerTypeLabel, owner.ownerIID);
    }

    private navigateToInstance(typeLabel: string, instanceIID: string) {
        const schema = this.schemaState.value$.value;
        if (!schema) {
            this.snackbar.errorPersistent("Schema not loaded");
            return;
        }
        const targetType = schema.entities[typeLabel] || schema.relations[typeLabel];
        if (!targetType) {
            this.snackbar.errorPersistent(`Type '${typeLabel}' not found in schema`);
            return;
        }
        const newBreadcrumbs: BreadcrumbItem[] = [
            ...this.breadcrumbs,
            { kind: "instance-detail", typeLabel: this.type.label, typeKind: this.type.kind, instanceIID: this.instanceIID }
        ];
        this.dataEditorState.openInstanceDetail(targetType, instanceIID, newBreadcrumbs);
    }

    navigateToBreadcrumb(breadcrumb: BreadcrumbItem, index: number) {
        this.dataEditorState.navigateToBreadcrumb(breadcrumb, index, this.breadcrumbs);
    }

    exploreType() {
        this.dataEditorState.openTypeTab(this.type, [], true);
    }

    getTypeIconClass(typeKind: string, filled: boolean): string {
        const style = filled ? "fa-regular" : "fa-solid";
        switch (typeKind) {
            case "entityType":
                return `${style} fa-square entity`;
            case "relationType":
                return `${style} fa-diamond relation`;
            case "attributeType":
                return `${style} fa-circle attribute`;
            default:
                return `${style} fa-question`;
        }
    }
}
