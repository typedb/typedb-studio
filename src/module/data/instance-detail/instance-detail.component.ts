/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Component, Input, OnInit, OnDestroy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltip, MatTooltipModule } from "@angular/material/tooltip";
import { Clipboard } from "@angular/cdk/clipboard";
import { Subscription } from "rxjs";
import { filter, take } from "rxjs/operators";
import { SchemaAttribute, SchemaConcept, SchemaState } from "../../../service/schema-state.service";
import { DriverState } from "../../../service/driver-state.service";
import { SnackbarService } from "../../../service/snackbar.service";
import { BreadcrumbItem, DataEditorState } from "../../../service/data-editor-state.service";
import { ApiResponse, Concept, ConceptRowAnswer, Entity, isApiErrorResponse, QueryResponse, Relation, RoleType } from "@typedb/driver-http";
import { extractErrorMessage } from "../../../framework/util/observable";

function isInstance(concept: Concept | undefined): concept is Entity | Relation {
    return concept?.kind === "entity" || concept?.kind === "relation";
}

function isRoleType(concept: Concept | undefined): concept is RoleType {
    return concept?.kind === "roleType";
}

interface AttributeData {
    type: string;
    valueType: string;
    values: string[];
}

interface RelationInstanceData {
    relationTypeLabel: string;
    relationIID: string;
    roleplayers: RoleplayerData[];
    attributes: AttributeData[];
    attributesLoaded: boolean;
}

interface RoleplayerData {
    roleLabel: string;
    playerIID: string;
    playerTypeLabel: string;
    isSelf: boolean;
    attributes: AttributeData[];
    expanded: boolean;
}

interface OwnerData {
    ownerIID: string;
    ownerTypeLabel: string;
    ownerKind: "entity" | "relation";
    attributes: AttributeData[];
}

@Component({
    selector: "ts-instance-detail",
    templateUrl: "./instance-detail.component.html",
    styleUrls: ["./instance-detail.component.scss"],
    imports: [
        CommonModule,
        MatProgressSpinnerModule,
        MatButtonModule,
        MatTooltipModule,
    ],
})
export class InstanceDetailComponent implements OnInit, OnDestroy {
    @Input({ required: true }) type!: SchemaConcept;
    @Input({ required: true }) instanceIID!: string;
    @Input() breadcrumbs: BreadcrumbItem[] = [];

    attributes: AttributeData[] = [];
    allRelations: RelationInstanceData[] = [];
    relationTypes: string[] = [];
    selectedRelationType: string | null = null; // null = "All"
    loading = false;
    relationsLoading = false;
    roleplayersLoading = false;
    ownRoleplayers: RoleplayerData[] = [];
    owners: OwnerData[] = [];
    ownersLoading = false;
    typeCollapsed = false;
    roleplayersCollapsed = false;
    attributesCollapsed = false;
    relationsCollapsed = false;
    ownersCollapsed = false;

    /** True when in manual mode with no open transaction */
    needsTransaction = false;

    /** Transaction ID used to load the current data (for stale detection) */
    private loadedWithTransactionId: string | null = null;

    /** True when data was loaded in a different transaction than the current one */
    isDataStale = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private driver: DriverState,
        private snackbar: SnackbarService,
        private clipboard: Clipboard,
        private schemaState: SchemaState,
        private dataEditorState: DataEditorState,
    ) {}

    get filteredRelations(): RelationInstanceData[] {
        if (this.selectedRelationType === null) {
            return this.allRelations;
        }
        return this.allRelations.filter(r => r.relationTypeLabel === this.selectedRelationType);
    }

    selectRelationType(type: string | null) {
        this.selectedRelationType = type;
    }

    getRelationTypeCount(type: string): number {
        return this.allRelations.filter(r => r.relationTypeLabel === type).length;
    }

    copyValue(value: string, tooltip: MatTooltip) {
        this.clipboard.copy(value);
        tooltip.show(0);
        setTimeout(() => tooltip.hide(0), 1000);
    }

    ngOnInit() {
        // Check if we need a transaction before loading data
        if (this.checkNeedsTransaction()) {
            // Subscribe to transaction state changes to auto-load when transaction opens
            this.subscriptions.push(
                this.driver.transaction$.pipe(
                    filter(tx => tx != null),
                    take(1) // Only react once when transaction first opens
                ).subscribe(() => {
                    this.needsTransaction = false;
                    this.loadData();
                })
            );
        } else {
            this.loadData();
        }

        // Subscribe to transaction changes to detect stale data (only relevant in manual mode)
        this.subscriptions.push(
            this.driver.transaction$.subscribe(tx => {
                // Only show stale warning in manual mode when a different transaction is open
                if (!this.driver.autoTransactionEnabled$.value && tx != null && tx.id !== this.loadedWithTransactionId) {
                    this.isDataStale = true;
                }
                // Clear stale flag when switching to auto mode or when transaction is closed
                if (this.driver.autoTransactionEnabled$.value || tx == null) {
                    this.isDataStale = false;
                }
            })
        );

        // Subscribe to auto mode changes to load data when switching from manual to auto
        this.subscriptions.push(
            this.driver.autoTransactionEnabled$.subscribe(autoEnabled => {
                if (autoEnabled && this.needsTransaction) {
                    this.needsTransaction = false;
                    this.loadData();
                }
            })
        );
    }

    ngOnDestroy() {
        this.subscriptions.forEach(sub => sub.unsubscribe());
    }

    /** Returns true if we're in manual mode without an open transaction */
    private checkNeedsTransaction(): boolean {
        if (!this.driver.autoTransactionEnabled$.value && !this.driver.transactionOpen) {
            this.needsTransaction = true;
            return true;
        }
        return false;
    }

    /** Called from template to open a transaction and load data */
    openTransactionAndLoad() {
        this.driver.openTransaction("read").subscribe();
        // Data will load automatically via the transaction$ subscription
    }

    /** Refresh data with current transaction */
    refresh() {
        this.loadData();
    }

    private loadData() {
        // Record current transaction ID for stale detection
        this.loadedWithTransactionId = this.driver.currentTransaction?.id ?? null;
        this.isDataStale = false;

        if (this.type.kind === "attributeType") {
            // For attributes, fetch owners instead of attributes/relations
            this.fetchOwners();
        } else {
            this.fetchAttributes();
            this.fetchRelations();
            if (this.type.kind === "relationType") {
                this.fetchOwnRoleplayers();
            }
        }
    }

    private fetchAttributes() {
        // Check if type has any owned attributes (only entities and relations can own attributes)
        const hasOwnedAttributes = "ownedAttributes" in this.type && this.type.ownedAttributes.length > 0;

        if (!hasOwnedAttributes) {
            // No attributes to fetch - nothing to do
            this.loading = false;
            this.attributes = [];
            return;
        }

        this.loading = true;

        // Fetch all attributes for this instance
        const query = `
match
    $instance isa ${this.type.label};
    $instance iid ${this.instanceIID};
    $instance has $attr;
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                this.loading = false;

                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching attributes: ${res.err.message}`);
                    return;
                }

                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    this.processAttributes(answers);
                }
            },
            error: (err) => {
                this.loading = false;
                this.snackbar.errorPersistent(`Error fetching attributes: ${extractErrorMessage(err)}`);
            }
        });
    }

    private processAttributes(conceptRowAnswers: ConceptRowAnswer[]) {
        // Group attributes by type
        const attrMap = new Map<string, { valueType: string; values: string[] }>();

        for (const answer of conceptRowAnswers) {
            const row = answer.data;
            const attrConcept = row["attr"];
            if (attrConcept && attrConcept.kind === "attribute") {
                const attrType = attrConcept.type?.label || "unknown";
                const attrValueType = attrConcept.type?.valueType || "unknown";
                const attrValue = String(attrConcept.value);

                if (!attrMap.has(attrType)) {
                    attrMap.set(attrType, { valueType: attrValueType, values: [] });
                }
                attrMap.get(attrType)!.values.push(attrValue);
            }
        }

        // Convert to AttributeData array
        this.attributes = Array.from(attrMap.entries()).map(([type, data]) => ({
            type,
            valueType: data.valueType,
            values: data.values,
        }));
    }

    private fetchRelations() {
        this.relationsLoading = true;

        // Fetch all relations where this instance plays a role, along with all roleplayers
        const query = `
match
    $instance iid ${this.instanceIID};
    $rel links ($role: $instance);
    $rel links ($otherRole: $player);
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                this.relationsLoading = false;

                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching relations: ${res.err.message}`);
                    return;
                }

                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    this.processRelations(answers);
                }
            },
            error: (err) => {
                this.relationsLoading = false;
                this.snackbar.errorPersistent(`Error fetching relations: ${extractErrorMessage(err)}`);
            }
        });
    }

    private fetchOwnRoleplayers() {
        this.roleplayersLoading = true;

        // Fetch roleplayers of this relation instance
        const query = `
match
    $rel iid ${this.instanceIID};
    $rel links ($role: $player);
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                this.roleplayersLoading = false;

                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching roleplayers: ${res.err.message}`);
                    return;
                }

                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    this.processOwnRoleplayers(answers);
                }
            },
            error: (err) => {
                this.roleplayersLoading = false;
                this.snackbar.errorPersistent(`Error fetching roleplayers: ${extractErrorMessage(err)}`);
            }
        });
    }

    private processOwnRoleplayers(conceptRowAnswers: ConceptRowAnswer[]) {
        const roleplayerMap = new Map<string, RoleplayerData>();

        for (const answer of conceptRowAnswers) {
            const row = answer.data;
            const role = row["role"];
            const player = row["player"];

            if (!isRoleType(role)) {
                throw new Error(`Expected role type for $role, got: ${JSON.stringify(role)}`);
            }
            if (!isInstance(player)) {
                throw new Error(`Expected entity or relation for $player, got: ${JSON.stringify(player)}`);
            }

            const roleLabel = role.label;
            const playerIID = player.iid;
            const playerTypeLabel = player.type.label;

            // Use role+playerIID as key to avoid duplicates
            const key = `${roleLabel}:${playerIID}`;
            if (!roleplayerMap.has(key)) {
                roleplayerMap.set(key, {
                    roleLabel,
                    playerIID,
                    playerTypeLabel,
                    isSelf: false,
                    attributes: [],
                    expanded: false,
                });
            }
        }

        this.ownRoleplayers = Array.from(roleplayerMap.values());
        this.loadOwnRoleplayerAttributes();
    }

    private loadOwnRoleplayerAttributes() {
        if (this.ownRoleplayers.length === 0) return;

        const iidList = this.ownRoleplayers.map(rp => rp.playerIID);
        const iidConditions = iidList.map(iid => `{ $player iid ${iid}; }`).join(" or ");
        const query = `
match
    ${iidConditions};
    $player has $attr;
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching roleplayer attributes: ${res.err.message}`);
                    return;
                }

                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    const playerAttrMap = this.buildPlayerAttributeMap(answers);

                    for (const roleplayer of this.ownRoleplayers) {
                        const attrMap = playerAttrMap.get(roleplayer.playerIID);
                        if (attrMap) {
                            roleplayer.attributes = Array.from(attrMap.entries()).map(([type, data]) => ({
                                type,
                                valueType: data.valueType,
                                values: data.values,
                            }));
                        }
                    }
                }
            },
            error: (err) => {
                this.snackbar.errorPersistent(`Error fetching roleplayer attributes: ${extractErrorMessage(err)}`);
            }
        });
    }

    private processRelations(conceptRowAnswers: ConceptRowAnswer[]) {
        // Build flat list of relation instances, keyed by IID
        const instanceMap = new Map<string, RelationInstanceData>();
        const typeSet = new Set<string>();

        for (const answer of conceptRowAnswers) {
            const row = answer.data;
            const rel = row["rel"];
            const otherRole = row["otherRole"];
            const player = row["player"];

            if (!rel || rel.kind !== "relation") {
                throw new Error(`Expected relation for $rel, got: ${JSON.stringify(rel)}`);
            }
            if (!isInstance(player)) {
                throw new Error(`Expected entity or relation for $player, got: ${JSON.stringify(player)}`);
            }
            if (!isRoleType(otherRole)) {
                throw new Error(`Expected role type for $otherRole, got: ${JSON.stringify(otherRole)}`);
            }

            const relTypeLabel = rel.type.label;
            const relIID = rel.iid;
            const roleLabel = otherRole.label;
            const playerIID = player.iid;
            const playerTypeLabel = player.type.label;

            typeSet.add(relTypeLabel);

            // Initialize relation instance if needed
            if (!instanceMap.has(relIID)) {
                instanceMap.set(relIID, {
                    relationTypeLabel: relTypeLabel,
                    relationIID: relIID,
                    roleplayers: [],
                    attributes: [],
                    attributesLoaded: false,
                });
            }
            const relInstance = instanceMap.get(relIID)!;

            // Add roleplayer if not already present
            const isSelf = playerIID === this.instanceIID;
            const existingRoleplayer = relInstance.roleplayers.find(
                rp => rp.playerIID === playerIID && rp.roleLabel === roleLabel
            );
            if (!existingRoleplayer) {
                relInstance.roleplayers.push({
                    roleLabel,
                    playerIID,
                    playerTypeLabel,
                    isSelf,
                    attributes: [],
                    expanded: false,
                });
            }
        }

        // Store flat list and unique types
        // Sort roleplayers so "this" (self) always appears last
        const relations = Array.from(instanceMap.values());
        for (const rel of relations) {
            rel.roleplayers.sort((a, b) => (a.isSelf === b.isSelf ? 0 : a.isSelf ? 1 : -1));
        }
        this.allRelations = relations;
        this.relationTypes = Array.from(typeSet).sort();

        // Load all roleplayer attributes and relation attributes upfront
        this.loadAllRoleplayerAttributes();
        this.loadAllRelationAttributes();
    }

    openRoleplayerDetail(roleplayer: RoleplayerData, event: Event) {
        event.stopPropagation();
        const schema = this.schemaState.value$.value;
        if (!schema) {
            this.snackbar.errorPersistent("Schema not loaded");
            return;
        }

        // Find the type in schema (could be entity or relation)
        const playerType = schema.entities[roleplayer.playerTypeLabel] || schema.relations[roleplayer.playerTypeLabel];
        if (!playerType) {
            this.snackbar.errorPersistent(`Type '${roleplayer.playerTypeLabel}' not found in schema`);
            return;
        }

        // Build new breadcrumbs by appending current instance
        const newBreadcrumbs: BreadcrumbItem[] = [
            ...this.breadcrumbs,
            { kind: "instance-detail", typeLabel: this.type.label, typeKind: this.type.kind, instanceIID: this.instanceIID }
        ];

        this.dataEditorState.openInstanceDetail(playerType, roleplayer.playerIID, newBreadcrumbs);
    }

    openRelationDetail(instance: RelationInstanceData, event: Event) {
        event.stopPropagation();
        const schema = this.schemaState.value$.value;
        if (!schema) {
            this.snackbar.errorPersistent("Schema not loaded");
            return;
        }

        const relationType = schema.relations[instance.relationTypeLabel];
        if (!relationType) {
            this.snackbar.errorPersistent(`Relation type '${instance.relationTypeLabel}' not found in schema`);
            return;
        }

        // Build new breadcrumbs by appending current instance
        const newBreadcrumbs: BreadcrumbItem[] = [
            ...this.breadcrumbs,
            { kind: "instance-detail", typeLabel: this.type.label, typeKind: this.type.kind, instanceIID: this.instanceIID }
        ];

        this.dataEditorState.openInstanceDetail(relationType, instance.relationIID, newBreadcrumbs);
    }

    navigateToBreadcrumb(breadcrumb: BreadcrumbItem, index: number) {
        this.dataEditorState.navigateToBreadcrumb(breadcrumb, index, this.breadcrumbs);
    }

    exploreType() {
        const newBreadcrumbs: BreadcrumbItem[] = [
            ...this.breadcrumbs,
            { kind: "instance-detail", typeLabel: this.type.label, typeKind: this.type.kind, instanceIID: this.instanceIID }
        ];
        this.dataEditorState.openTypeTab(this.type, newBreadcrumbs);
    }

    openOwnRoleplayerDetail(roleplayer: RoleplayerData, event: Event) {
        event.stopPropagation();
        const schema = this.schemaState.value$.value;
        if (!schema) {
            this.snackbar.errorPersistent("Schema not loaded");
            return;
        }

        const playerType = schema.entities[roleplayer.playerTypeLabel] || schema.relations[roleplayer.playerTypeLabel];
        if (!playerType) {
            this.snackbar.errorPersistent(`Type '${roleplayer.playerTypeLabel}' not found in schema`);
            return;
        }

        const newBreadcrumbs: BreadcrumbItem[] = [
            ...this.breadcrumbs,
            { kind: "instance-detail", typeLabel: this.type.label, typeKind: this.type.kind, instanceIID: this.instanceIID }
        ];

        this.dataEditorState.openInstanceDetail(playerType, roleplayer.playerIID, newBreadcrumbs);
    }

    get typeKindLabel(): string {
        switch (this.type.kind) {
            case "entityType": return "entity";
            case "relationType": return "relation";
            case "attributeType": return "attribute";
            default: return "";
        }
    }

    getTypeIconClass(typeKind: string, filled: boolean): string {
        const style = filled ? "fa-solid" : "fa-regular";
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

    private formatAttributeValue(value: string, valueType: string): string {
        switch (valueType) {
            case "string":
                // Escape quotes and wrap in quotes
                return `"${value.replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`;
            case "boolean":
            case "long":
            case "double":
                // Numeric and boolean values don't need quotes
                return value;
            case "datetime":
            case "datetime-tz":
                // DateTime values are returned as ISO strings
                return value;
            case "date":
                // Date values
                return value;
            case "duration":
                // Duration values
                return value;
            default:
                // Default to quoting as string
                return `"${value.replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`;
        }
    }

    private fetchOwners() {
        this.ownersLoading = true;

        // For attributes, instanceIID is actually the attribute value (not a real IID)
        // We need to query by value, formatting appropriately for the value type
        const attrType = this.type as SchemaAttribute;
        const valueType = attrType.valueType;
        const formattedValue = this.formatAttributeValue(this.instanceIID, valueType);

        // Fetch all entities/relations that own this attribute
        const query = `
match
    $owner has ${this.type.label} ${formattedValue};
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                this.ownersLoading = false;

                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching owners: ${res.err.message}`);
                    return;
                }

                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    this.processOwners(answers);
                }
            },
            error: (err) => {
                this.ownersLoading = false;
                this.snackbar.errorPersistent(`Error fetching owners: ${extractErrorMessage(err)}`);
            }
        });
    }

    private processOwners(conceptRowAnswers: ConceptRowAnswer[]) {
        const ownerMap = new Map<string, OwnerData>();

        for (const answer of conceptRowAnswers) {
            const row = answer.data;
            const owner = row["owner"];

            if (!isInstance(owner)) {
                throw new Error(`Expected entity or relation for $owner, got: ${JSON.stringify(owner)}`);
            }

            const ownerIID = owner.iid;
            if (!ownerMap.has(ownerIID)) {
                ownerMap.set(ownerIID, {
                    ownerIID,
                    ownerTypeLabel: owner.type.label,
                    ownerKind: owner.kind as "entity" | "relation",
                    attributes: [],
                });
            }
        }

        this.owners = Array.from(ownerMap.values());
        this.loadOwnerAttributes();
    }

    private loadOwnerAttributes() {
        if (this.owners.length === 0) return;

        const iidList = this.owners.map(o => o.ownerIID);
        const iidConditions = iidList.map(iid => `{ $owner iid ${iid}; }`).join(" or ");
        const query = `
match
    ${iidConditions};
    $owner has $attr;
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching owner attributes: ${res.err.message}`);
                    return;
                }

                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    this.processOwnerAttributes(answers);
                }
            },
            error: (err) => {
                this.snackbar.errorPersistent(`Error fetching owner attributes: ${extractErrorMessage(err)}`);
            }
        });
    }

    private processOwnerAttributes(conceptRowAnswers: ConceptRowAnswer[]) {
        // Build a map of owner IID -> attribute type -> { valueType, values }
        const ownerAttrMap = new Map<string, Map<string, { valueType: string; values: string[] }>>();

        for (const answer of conceptRowAnswers) {
            const row = answer.data;
            const owner = row["owner"];
            const attr = row["attr"];

            if (!isInstance(owner)) {
                throw new Error(`Expected entity or relation for $owner, got: ${JSON.stringify(owner)}`);
            }
            if (!attr || attr.kind !== "attribute") {
                throw new Error(`Expected attribute for $attr, got: ${JSON.stringify(attr)}`);
            }

            const ownerIID = owner.iid;
            const attrType = attr.type.label;
            const attrValueType = attr.type.valueType || "unknown";
            const attrValue = String(attr.value);

            if (!ownerAttrMap.has(ownerIID)) {
                ownerAttrMap.set(ownerIID, new Map());
            }
            const attrMap = ownerAttrMap.get(ownerIID)!;

            if (!attrMap.has(attrType)) {
                attrMap.set(attrType, { valueType: attrValueType, values: [] });
            }
            attrMap.get(attrType)!.values.push(attrValue);
        }

        // Assign attributes to owners
        for (const owner of this.owners) {
            const attrMap = ownerAttrMap.get(owner.ownerIID);
            if (attrMap) {
                owner.attributes = Array.from(attrMap.entries()).map(([type, data]) => ({
                    type,
                    valueType: data.valueType,
                    values: data.values,
                }));
            }
        }
    }

    openOwnerDetail(owner: OwnerData, event: Event) {
        event.stopPropagation();
        const schema = this.schemaState.value$.value;
        if (!schema) {
            this.snackbar.errorPersistent("Schema not loaded");
            return;
        }

        const ownerType = owner.ownerKind === "entity"
            ? schema.entities[owner.ownerTypeLabel]
            : schema.relations[owner.ownerTypeLabel];

        if (!ownerType) {
            this.snackbar.errorPersistent(`Type '${owner.ownerTypeLabel}' not found in schema`);
            return;
        }

        const newBreadcrumbs: BreadcrumbItem[] = [
            ...this.breadcrumbs,
            { kind: "instance-detail", typeLabel: this.type.label, typeKind: this.type.kind, instanceIID: this.instanceIID }
        ];

        this.dataEditorState.openInstanceDetail(ownerType, owner.ownerIID, newBreadcrumbs);
    }

    private loadAllRoleplayerAttributes() {
        // Collect all non-self roleplayer IIDs from all instances
        const allIIDs = new Set<string>();

        for (const instance of this.allRelations) {
            for (const rp of instance.roleplayers) {
                if (!rp.isSelf) {
                    allIIDs.add(rp.playerIID);
                }
            }
        }

        if (allIIDs.size === 0) {
            for (const instance of this.allRelations) {
                instance.attributesLoaded = true;
            }
            return;
        }

        // Build query for all roleplayers' attributes
        const iidList = Array.from(allIIDs);
        const iidConditions = iidList.map(iid => `{ $player iid ${iid}; }`).join(" or ");
        const query = `
match
    ${iidConditions};
    $player has $attr;
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching roleplayer attributes: ${res.err.message}`);
                    return;
                }

                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    this.processAllRoleplayerAttributes(answers);
                }
                for (const instance of this.allRelations) {
                    instance.attributesLoaded = true;
                }
            },
            error: (err) => {
                const message = err?.message || (typeof err === "object" ? JSON.stringify(err) : String(err));
                this.snackbar.errorPersistent(`Error fetching roleplayer attributes: ${message}`);
                for (const instance of this.allRelations) {
                    instance.attributesLoaded = true;
                }
            }
        });
    }

    private loadAllRelationAttributes() {
        // Collect all relation IIDs
        const allIIDs = this.allRelations.map(r => r.relationIID);

        if (allIIDs.length === 0) {
            return;
        }

        // Build query for all relations' attributes
        const iidConditions = allIIDs.map(iid => `{ $rel iid ${iid}; }`).join(" or ");
        const query = `
match
    ${iidConditions};
    $rel has $attr;
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching relation attributes: ${res.err.message}`);
                    return;
                }

                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    this.processAllRelationAttributes(answers);
                }
            },
            error: (err) => {
                const message = err?.message || (typeof err === "object" ? JSON.stringify(err) : String(err));
                this.snackbar.errorPersistent(`Error fetching relation attributes: ${message}`);
            }
        });
    }

    private processAllRelationAttributes(conceptRowAnswers: ConceptRowAnswer[]) {
        // Build a map of relation IID -> attribute type -> { valueType, values }
        const relAttrMap = new Map<string, Map<string, { valueType: string; values: string[] }>>();

        for (const answer of conceptRowAnswers) {
            const row = answer.data;
            const rel = row["rel"];
            const attr = row["attr"];

            if (!rel || rel.kind !== "relation") {
                throw new Error(`Expected relation for $rel, got: ${JSON.stringify(rel)}`);
            }
            if (!attr || attr.kind !== "attribute") {
                throw new Error(`Expected attribute for $attr, got: ${JSON.stringify(attr)}`);
            }

            const relIID = rel.iid;
            const attrType = attr.type.label;
            const attrValueType = attr.type.valueType || "unknown";
            const attrValue = String(attr.value);

            if (!relAttrMap.has(relIID)) {
                relAttrMap.set(relIID, new Map());
            }
            const attrMap = relAttrMap.get(relIID)!;

            if (!attrMap.has(attrType)) {
                attrMap.set(attrType, { valueType: attrValueType, values: [] });
            }
            attrMap.get(attrType)!.values.push(attrValue);
        }

        // Assign attributes to all relations
        for (const instance of this.allRelations) {
            const attrMap = relAttrMap.get(instance.relationIID);
            if (attrMap) {
                instance.attributes = Array.from(attrMap.entries()).map(([type, data]) => ({
                    type,
                    valueType: data.valueType,
                    values: data.values,
                }));
            }
        }
    }

    private processAllRoleplayerAttributes(conceptRowAnswers: ConceptRowAnswer[]) {
        // Build a map of player IID -> attribute type -> { valueType, values }
        const playerAttrMap = this.buildPlayerAttributeMap(conceptRowAnswers);

        // Assign attributes to all roleplayers across all instances
        for (const instance of this.allRelations) {
            for (const roleplayer of instance.roleplayers) {
                if (roleplayer.isSelf) continue;

                const attrMap = playerAttrMap.get(roleplayer.playerIID);
                if (attrMap) {
                    roleplayer.attributes = Array.from(attrMap.entries()).map(([type, data]) => ({
                        type,
                        valueType: data.valueType,
                        values: data.values,
                    }));
                }
            }
        }
    }

    private buildPlayerAttributeMap(conceptRowAnswers: ConceptRowAnswer[]): Map<string, Map<string, { valueType: string; values: string[] }>> {
        const playerAttrMap = new Map<string, Map<string, { valueType: string; values: string[] }>>();

        for (const answer of conceptRowAnswers) {
            const row = answer.data;
            const player = row["player"];
            const attr = row["attr"];

            if (!isInstance(player)) {
                throw new Error(`Expected entity or relation for $player, got: ${JSON.stringify(player)}`);
            }
            if (!attr || attr.kind !== "attribute") {
                throw new Error(`Expected attribute for $attr, got: ${JSON.stringify(attr)}`);
            }

            const playerIID = player.iid;
            const attrType = attr.type.label;
            const attrValueType = attr.type.valueType || "unknown";
            const attrValue = String(attr.value);

            if (!playerAttrMap.has(playerIID)) {
                playerAttrMap.set(playerIID, new Map());
            }
            const attrMap = playerAttrMap.get(playerIID)!;

            if (!attrMap.has(attrType)) {
                attrMap.set(attrType, { valueType: attrValueType, values: [] });
            }
            attrMap.get(attrType)!.values.push(attrValue);
        }

        return playerAttrMap;
    }
}
