/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { inject, Injectable, OnDestroy } from "@angular/core";
import { Subscription } from "rxjs";
import { filter, take } from "rxjs/operators";
import { ApiResponse, Concept, ConceptRowAnswer, Entity, isApiErrorResponse, QueryResponse, Relation, RoleType } from "@typedb/driver-http";
import { DriverState } from "./driver-state.service";
import { SchemaAttribute, SchemaConcept } from "./schema-state.service";
import { SnackbarService } from "./snackbar.service";
import { extractErrorMessage } from "../framework/util/observable";

export interface AttributeData {
    type: string;
    valueType: string;
    values: string[];
}

export interface LinkData {
    roleLabel: string;
    playerIID: string;
    playerTypeLabel: string;
    isSelf: boolean;
    attributes: AttributeData[];
    expanded: boolean;
}

export interface RelationInstanceData {
    relationTypeLabel: string;
    relationIID: string;
    links: LinkData[];
    attributes: AttributeData[];
    attributesLoaded: boolean;
}

export interface OwnerData {
    ownerIID: string;
    ownerTypeLabel: string;
    ownerKind: "entity" | "relation";
    attributes: AttributeData[];
}

function isInstance(concept: Concept | undefined): concept is Entity | Relation {
    return concept?.kind === "entity" || concept?.kind === "relation";
}

function isRoleType(concept: Concept | undefined): concept is RoleType {
    return concept?.kind === "roleType";
}

/**
 * View-model backing the Instance Details panel — fetches and exposes the
 * attribute / relation / owner data for a single instance. Designed to be
 * provided per-component (not in root) so each panel gets its own state.
 *
 * Lifecycle: call `initialize(type, iid)` once after construction; Angular
 * will call `ngOnDestroy` when the host component is destroyed.
 */
@Injectable()
export class InstanceDetailViewModel implements OnDestroy {

    private driver = inject(DriverState);
    private snackbar = inject(SnackbarService);

    type!: SchemaConcept;
    instanceIID!: string;

    attributes: AttributeData[] = [];
    allRelations: RelationInstanceData[] = [];
    relationTypes: string[] = [];
    ownLinks: LinkData[] = [];
    owners: OwnerData[] = [];

    loading = false;
    relationsLoading = false;
    linksLoading = false;
    ownersLoading = false;

    /** True when in manual mode with no open transaction. */
    needsTransaction = false;
    /** True when data was loaded in a different transaction than the current one. */
    isDataStale = false;

    private loadedWithTransactionId: string | null = null;
    private subscriptions: Subscription[] = [];

    /**
     * Initialise (or re-initialise) with a new instance. Calling again with
     * different inputs resets all state and re-fetches — useful when the
     * host component stays mounted while the selected instance changes
     * (e.g. clicking different nodes in the graph).
     */
    initialize(type: SchemaConcept, instanceIID: string): void {
        if (this.type === type && this.instanceIID === instanceIID) return;
        this.reset();
        this.type = type;
        this.instanceIID = instanceIID;

        if (this.checkNeedsTransaction()) {
            // Wait for a transaction to open, then auto-load.
            this.subscriptions.push(
                this.driver.transaction$.pipe(
                    filter(tx => tx != null),
                    take(1),
                ).subscribe(() => {
                    this.needsTransaction = false;
                    this.loadData();
                })
            );
        } else {
            this.loadData();
        }

        // Detect stale data (only relevant in manual mode).
        this.subscriptions.push(
            this.driver.transaction$.subscribe(tx => {
                if (!this.driver.autoTransactionEnabled$.value && tx != null && tx.id !== this.loadedWithTransactionId) {
                    this.isDataStale = true;
                }
                if (this.driver.autoTransactionEnabled$.value || tx == null) {
                    this.isDataStale = false;
                }
            })
        );

        // Auto-load when switching from manual to auto mode.
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
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    private reset(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
        this.subscriptions = [];
        this.attributes = [];
        this.allRelations = [];
        this.relationTypes = [];
        this.ownLinks = [];
        this.owners = [];
        this.loading = false;
        this.relationsLoading = false;
        this.linksLoading = false;
        this.ownersLoading = false;
        this.needsTransaction = false;
        this.isDataStale = false;
        this.loadedWithTransactionId = null;
    }

    refresh(): void {
        this.loadData();
    }

    openTransactionAndLoad(): void {
        this.driver.openTransaction("read").subscribe();
        // Data loads automatically via the transaction$ subscription set up in initialize().
    }

    getRelationTypeCount(typeLabel: string): number {
        return this.allRelations.filter(r => r.relationTypeLabel === typeLabel).length;
    }

    get typeKindLabel(): string {
        switch (this.type.kind) {
            case "entityType": return "entity";
            case "relationType": return "relation";
            case "attributeType": return "attribute";
            default: return "";
        }
    }

    private checkNeedsTransaction(): boolean {
        if (!this.driver.autoTransactionEnabled$.value && !this.driver.transactionOpen) {
            this.needsTransaction = true;
            return true;
        }
        return false;
    }

    private loadData() {
        this.loadedWithTransactionId = this.driver.currentTransaction?.id ?? null;
        this.isDataStale = false;

        if (this.type.kind === "attributeType") {
            this.fetchOwners();
        } else {
            this.fetchAttributes();
            this.fetchRelations();
            if (this.type.kind === "relationType") {
                this.fetchOwnLinks();
            }
        }
    }

    private fetchAttributes() {
        const hasOwnedAttributes = "ownedAttributes" in this.type && this.type.ownedAttributes.length > 0;
        if (!hasOwnedAttributes) {
            this.loading = false;
            this.attributes = [];
            return;
        }

        this.loading = true;
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
        this.attributes = Array.from(attrMap.entries()).map(([type, data]) => ({
            type,
            valueType: data.valueType,
            values: data.values,
        }));
    }

    private typeOrSubtypePlaysRoles(type: SchemaConcept): boolean {
        if ("playedRoles" in type && type.playedRoles.length > 0) return true;
        if ("subtypes" in type) {
            for (const sub of type.subtypes) {
                if (this.typeOrSubtypePlaysRoles(sub)) return true;
            }
        }
        return false;
    }

    private fetchRelations() {
        if (!this.typeOrSubtypePlaysRoles(this.type)) {
            this.relationsLoading = false;
            this.allRelations = [];
            return;
        }

        this.relationsLoading = true;
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

    private fetchOwnLinks() {
        this.linksLoading = true;
        const query = `
match
    $rel iid ${this.instanceIID};
    $rel links ($role: $player);
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                this.linksLoading = false;
                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching links: ${res.err.message}`);
                    return;
                }
                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    this.processOwnLinks(answers);
                }
            },
            error: (err) => {
                this.linksLoading = false;
                this.snackbar.errorPersistent(`Error fetching links: ${extractErrorMessage(err)}`);
            }
        });
    }

    private processOwnLinks(conceptRowAnswers: ConceptRowAnswer[]) {
        const linkMap = new Map<string, LinkData>();
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
            const key = `${role.label}:${player.iid}`;
            if (!linkMap.has(key)) {
                linkMap.set(key, {
                    roleLabel: role.label,
                    playerIID: player.iid,
                    playerTypeLabel: player.type.label,
                    isSelf: false,
                    attributes: [],
                    expanded: false,
                });
            }
        }
        this.ownLinks = Array.from(linkMap.values());
        this.loadOwnLinkAttributes();
    }

    private loadOwnLinkAttributes() {
        if (this.ownLinks.length === 0) return;
        const iidList = this.ownLinks.map(rp => rp.playerIID);
        const iidConditions = iidList.map(iid => `{ $player iid ${iid}; }`).join(" or ");
        const query = `
match
    ${iidConditions};
    $player has $attr;
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching link attributes: ${res.err.message}`);
                    return;
                }
                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    const playerAttrMap = this.buildPlayerAttributeMap(answers);
                    for (const link of this.ownLinks) {
                        const attrMap = playerAttrMap.get(link.playerIID);
                        if (attrMap) {
                            link.attributes = Array.from(attrMap.entries()).map(([type, data]) => ({
                                type,
                                valueType: data.valueType,
                                values: data.values,
                            }));
                        }
                    }
                }
            },
            error: (err) => {
                this.snackbar.errorPersistent(`Error fetching link attributes: ${extractErrorMessage(err)}`);
            }
        });
    }

    private processRelations(conceptRowAnswers: ConceptRowAnswer[]) {
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

            const relIID = rel.iid;
            const roleLabel = otherRole.label;
            const playerIID = player.iid;
            const playerTypeLabel = player.type.label;

            typeSet.add(rel.type.label);

            if (!instanceMap.has(relIID)) {
                instanceMap.set(relIID, {
                    relationTypeLabel: rel.type.label,
                    relationIID: relIID,
                    links: [],
                    attributes: [],
                    attributesLoaded: false,
                });
            }
            const relInstance = instanceMap.get(relIID)!;

            const isSelf = playerIID === this.instanceIID;
            const existingLink = relInstance.links.find(
                rp => rp.playerIID === playerIID && rp.roleLabel === roleLabel
            );
            if (!existingLink) {
                relInstance.links.push({
                    roleLabel,
                    playerIID,
                    playerTypeLabel,
                    isSelf,
                    attributes: [],
                    expanded: false,
                });
            }
        }

        const relations = Array.from(instanceMap.values());
        for (const rel of relations) {
            // "this" (self) always appears last
            rel.links.sort((a, b) => (a.isSelf === b.isSelf ? 0 : a.isSelf ? 1 : -1));
        }
        this.allRelations = relations;
        this.relationTypes = Array.from(typeSet).sort();

        this.loadAllLinkAttributes();
        this.loadAllRelationAttributes();
    }

    private formatAttributeValue(value: string, valueType: string): string {
        switch (valueType) {
            case "string":
                return `"${value.replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`;
            case "boolean":
            case "long":
            case "double":
                return value;
            case "datetime":
            case "datetime-tz":
            case "date":
            case "duration":
                return value;
            default:
                return `"${value.replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`;
        }
    }

    private fetchOwners() {
        this.ownersLoading = true;
        const attrType = this.type as SchemaAttribute;
        const formattedValue = this.formatAttributeValue(this.instanceIID, attrType.valueType);

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
            if (!ownerMap.has(owner.iid)) {
                ownerMap.set(owner.iid, {
                    ownerIID: owner.iid,
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

    private loadAllLinkAttributes() {
        const allIIDs = new Set<string>();
        for (const instance of this.allRelations) {
            for (const rp of instance.links) {
                if (!rp.isSelf) allIIDs.add(rp.playerIID);
            }
        }

        if (allIIDs.size === 0) {
            for (const instance of this.allRelations) {
                instance.attributesLoaded = true;
            }
            return;
        }

        const iidConditions = Array.from(allIIDs).map(iid => `{ $player iid ${iid}; }`).join(" or ");
        const query = `
match
    ${iidConditions};
    $player has $attr;
        `.trim();

        this.driver.query(query).subscribe({
            next: (res: ApiResponse<QueryResponse>) => {
                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(`Error fetching link attributes: ${res.err.message}`);
                    return;
                }
                if (res.ok.answerType === "conceptRows") {
                    const answers = (res.ok as any).answers as ConceptRowAnswer[];
                    this.processAllLinkAttributes(answers);
                }
                for (const instance of this.allRelations) {
                    instance.attributesLoaded = true;
                }
            },
            error: (err) => {
                const message = err?.message || (typeof err === "object" ? JSON.stringify(err) : String(err));
                this.snackbar.errorPersistent(`Error fetching link attributes: ${message}`);
                for (const instance of this.allRelations) {
                    instance.attributesLoaded = true;
                }
            }
        });
    }

    private loadAllRelationAttributes() {
        const allIIDs = this.allRelations.map(r => r.relationIID);
        if (allIIDs.length === 0) return;

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

    private processAllLinkAttributes(conceptRowAnswers: ConceptRowAnswer[]) {
        const playerAttrMap = this.buildPlayerAttributeMap(conceptRowAnswers);
        for (const instance of this.allRelations) {
            for (const link of instance.links) {
                if (link.isSelf) continue;
                const attrMap = playerAttrMap.get(link.playerIID);
                if (attrMap) {
                    link.attributes = Array.from(attrMap.entries()).map(([type, data]) => ({
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
