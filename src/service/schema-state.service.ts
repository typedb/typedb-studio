/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import Graph from "graphology";
import { BehaviorSubject, combineLatest, distinctUntilChanged, finalize, first, map } from "rxjs";
import Sigma, { Camera } from "sigma";
import { createSigmaRenderer, GraphVisualiser } from "../framework/graph-visualiser";
import { defaultSigmaSettings } from "../framework/graph-visualiser/defaults";
import { newVisualGraph } from "../framework/graph-visualiser/graph";
import { Layouts } from "../framework/graph-visualiser/layouts";
import { AttributeType, EntityType, RelationType, RoleType, Type } from "../framework/typedb-driver/concept";
import { ApiOkResponse, ApiResponse, ConceptRowsQueryResponse, isApiErrorResponse, QueryResponse } from "../framework/typedb-driver/response";
import { DriverState } from "./driver-state.service";
import { SnackbarService } from "./snackbar.service";
import {updateAutocomleteSchemaFromDB} from "../framework/codemirror-lang-typeql";

const NO_SERVER_CONNECTED = `No server connected`;
const NO_DATABASE_SELECTED = `No database selected`;

const schemaQueries = {
    typeHierarchy: `match { $t sub! $supertype; } or {$t sub $supertype; $t is $supertype; };`,
    ownedAttributes: `match { $t owns $attr; not { $t sub! $sown; $sown owns $attr; }; };`,
    relatedRoles: `match { $t relates $related; not { $t sub! $srel; $srel relates $related; };  };`,
    playedRoles: `match { $t plays $played; not { $t sub! $splay; $splay plays $played; }; };`,
} as const satisfies Record<string, string>;
const schemaQueriesList = Object.values(schemaQueries);

type VisualiserStatus = "ok" | "running" | "noAnswers" | "error";

export interface SchemaEntity extends EntityType {
    supertype?: SchemaEntity;
    subtypes: SchemaEntity[];
    ownedAttributes: SchemaAttribute[];
    playedRoles: SchemaRole[];
}

export interface SchemaRelation extends RelationType {
    supertype?: SchemaRelation;
    subtypes: SchemaRelation[];
    ownedAttributes: SchemaAttribute[];
    playedRoles: SchemaRole[];
    relatedRoles: SchemaRole[];
}

export interface SchemaAttribute extends AttributeType {
    supertype?: SchemaAttribute;
    subtypes: SchemaAttribute[];
}

export type SchemaConcept = SchemaEntity | SchemaRelation | SchemaAttribute;

export type SchemaRole = RoleType;

export interface Schema {
    entities: Record<string, SchemaEntity>;
    relations: Record<string, SchemaRelation>;
    attributes: Record<string, SchemaAttribute>;
}

@Injectable({
    providedIn: "root",
})
export class SchemaState {

    readonly visualiser = new VisualiserState();
    queryResponses$ = new BehaviorSubject<ApiOkResponse<ConceptRowsQueryResponse>[] | null>(null);
    readonly value$ = new BehaviorSubject<Schema | null>(null);
    isRefreshing = false;
    readonly refreshDisabledReason$ = combineLatest([this.driver.status$, this.driver.database$]).pipe(map(([status, db]) => {
        if (status !== "connected") return NO_SERVER_CONNECTED;
        else if (db == null) return NO_DATABASE_SELECTED;
        else return null;
    }));
    readonly refreshEnabled$ = this.refreshDisabledReason$.pipe(map(x => x == null));

    constructor(private driver: DriverState, private snackbar: SnackbarService) {
        (window as any)["schemaState"] = this;
        this.driver.database$.pipe(
            distinctUntilChanged((x, y) => x?.name === y?.name)
        ).subscribe(() => {
            this.refresh();
        });
        this.queryResponses$.subscribe(data => {
            this.push(data);
        });
        this.value$.subscribe(schema => {
            if (schema != null) {
                updateAutocomleteSchemaFromDB(schema)
            }
        })
    }

    refresh() {
        if (this.isRefreshing) return;

        this.driver.database$.pipe(first()).subscribe(db => {
            this.visualiser.dropSavedState();

            if (db == null) {
                this.queryResponses$.next(null);
                this.visualiser.destroy();
                this.visualiser.database = undefined;
                return;
            }

            this.initialiseOutput();
            const responses: ApiOkResponse<ConceptRowsQueryResponse>[] = [];
            this.isRefreshing = true;
            this.driver.runBackgroundReadQueries(schemaQueriesList).pipe(
                finalize(() => { this.isRefreshing = false; })
            ).subscribe({
                next: (res) => {
                    if (res.ok.answerType !== `conceptRows`) throw `Unexpected answerType: '${res.ok.answerType}' (expected 'conceptRows')`;
                    responses.push(res as ApiOkResponse<ConceptRowsQueryResponse>);
                },
                error: (err) => { this.handleQueryError(err); },
                complete: () => {
                    this.queryResponses$.next(responses);
                    if (this.visualiser.status === "running") {
                        if (responses[0].ok.answers.length) this.visualiser.status = "ok";
                        else this.visualiser.status = "noAnswers";
                    }
                },
            });
        });
    }

    push(data: ApiOkResponse<ConceptRowsQueryResponse>[] | null) {
        if (!data) {
            this.value$.next(null);
            return;
        }

        const schemaBuilder = new SchemaBuilder(data);
        const schema = schemaBuilder.build();
        this.value$.next(schema);
    }

    private initialiseOutput() {
        this.visualiser.destroy();
        this.visualiser.status = "running";
        this.visualiser.database = this.driver.requireDatabase(`${this.constructor.name}.${this.initialiseOutput.name} > requireValue(driver.database$)`).name;
    }

    private handleQueryError(err: any) {
        if (isApiErrorResponse(err)) {
            this.snackbar.errorPersistent(err.err.message);
            this.visualiser.destroy();
            this.visualiser.status = `error`;
            return;
        }
        this.driver.checkHealth().subscribe({
            next: () => {
                const msg = err?.message || err?.toString() || `Unknown error`;
                this.snackbar.errorPersistent(`Error: ${msg}\n`
                    + `Caused: Failed to load database schema.`);
            },
            error: () => {
                this.driver.connection$.pipe(first()).subscribe((connection) => {
                    if (connection && connection.url.includes(`localhost`)) {
                        this.snackbar.errorPersistent(`Unable to connect to TypeDB server.\n`
                            + `Ensure the server is still running.`);
                    } else {
                        this.snackbar.errorPersistent(`Unable to connect to TypeDB server.\n`
                            + `Check your network connection and ensure the server is still running.`);
                    }
                });
            }
        });
    }
}

function entityOf(entityType: EntityType): SchemaEntity {
    return {
        kind: entityType.kind,
        label: entityType.label,
        supertype: undefined,
        subtypes: [],
        ownedAttributes: [],
        playedRoles: [],
    };
}

function relationOf(relationType: RelationType): SchemaRelation {
    return {
        kind: relationType.kind,
        label: relationType.label,
        supertype: undefined,
        subtypes: [],
        ownedAttributes: [],
        playedRoles: [],
        relatedRoles: [],
    };
}

function attributeOf(attributeType: AttributeType): SchemaAttribute {
    return {
        kind: attributeType.kind,
        label: attributeType.label,
        supertype: undefined,
        subtypes: [],
        valueType: attributeType.valueType,
    };
}

class SchemaBuilder {
    readonly typeHierarchy: ConceptRowsQueryResponse;
    readonly ownedAttributes: ConceptRowsQueryResponse;
    readonly relatedRoles: ConceptRowsQueryResponse;
    readonly playedRoles: ConceptRowsQueryResponse;
    readonly entityTypes = {} as Record<string, SchemaEntity>;
    readonly relationTypes = {} as Record<string, SchemaRelation>;
    readonly attributeTypes = {} as Record<string, SchemaAttribute>;

    constructor(data: ApiOkResponse<ConceptRowsQueryResponse>[]) {
        const [typeHierarchy, ownedAttributes, relatedRoles, playedRoles] = data.map(x => x.ok);
        this.typeHierarchy = typeHierarchy;
        this.ownedAttributes = ownedAttributes;
        this.relatedRoles = relatedRoles;
        this.playedRoles = playedRoles;
    }

    build(): Schema {
        this.populateConcepts();
        this.buildTypeHierarchy();
        this.attachOwnedAttributes();
        this.attachPlayedRoles();
        this.attachRelatedRoles();
        return {
            entities: this.entityTypes,
            relations: this.relationTypes,
            attributes: this.attributeTypes,
        };
    }

    private populateConcepts() {
        for (const answer of this.typeHierarchy.answers) {
            const [type, supertype] = [answer.data["t"], answer.data["supertype"]];
            if (!type || !supertype) throw this.unexpectedTypeHierarchyAnswer(answer);
            switch (type.kind) {
                case "entityType":
                    this.entityTypes[type.label] = this.entityTypes[type.label] ?? entityOf(type);
                    break;
                case "relationType":
                    this.relationTypes[type.label] = this.relationTypes[type.label] ?? relationOf(type);
                    break;
                case "attributeType":
                    this.attributeTypes[type.label] = this.attributeTypes[type.label] ?? attributeOf(type);
                    break;
                case "roleType":
                    continue;
                default:
                    throw this.unexpectedTypeHierarchyAnswer(answer);
            }
        }
    }

    private buildTypeHierarchy() {
        for (const answer of this.typeHierarchy.answers) {
            const [type, supertype] = [answer.data["t"], answer.data["supertype"]] as Type[];
            if (type.label === supertype.label) continue;
            let node: SchemaConcept;
            let supernode: SchemaConcept;
            switch (type.kind) {
                case "entityType":
                    node = this.expectEntityType(type.label);
                    supernode = this.expectEntityType(supertype.label);
                    break;
                case "relationType":
                    node = this.expectRelationType(type.label);
                    supernode = this.expectRelationType(supertype.label);
                    break;
                case "attributeType":
                    node = this.expectAttributeType(type.label);
                    supernode = this.expectAttributeType(supertype.label);
                    break;
                case "roleType":
                    continue;
                default:
                    throw this.unexpectedTypeHierarchyAnswer(answer);
            }
            node.supertype = supernode;
            (supernode.subtypes as SchemaConcept[]).push(node);
        }
    }

    private attachOwnedAttributes() {
        for (const answer of this.ownedAttributes.answers) {
            const [ownerType, ownedAttr] = [answer.data["t"], answer.data["attr"]];
            if (!ownerType || !ownedAttr || ownedAttr.kind !== "attributeType") throw this.unexpectedOwnedAttributesAnswer(answer);
            let ownerNode: SchemaConcept;
            const ownedAttrNode: SchemaAttribute = this.expectAttributeType(ownedAttr.label);
            switch (ownerType.kind) {
                case "entityType":
                    ownerNode = this.expectEntityType(ownerType.label);
                    break;
                case "relationType":
                    ownerNode = this.expectRelationType(ownerType.label);
                    break;
                default:
                    throw this.unexpectedOwnedAttributesAnswer(answer);
            }
            this.propagateOwnedAttributes(ownerNode, ownedAttrNode);
        }
    }

    private propagateOwnedAttributes(ownerNode: SchemaEntity | SchemaRelation, ownedAttrNode: SchemaAttribute) {
        ownerNode.ownedAttributes.push(ownedAttrNode);
        for (const ownerSubnode of ownerNode.subtypes) {
            this.propagateOwnedAttributes(ownerSubnode, ownedAttrNode);
        }
    }

    private attachRelatedRoles() {
        for (const answer of this.relatedRoles.answers) {
            const [rel, role] = [answer.data["t"], answer.data["related"]];
            if (!rel || !role || rel.kind !== "relationType" || role.kind !== "roleType") throw this.unexpectedRoleplayersAnswer(answer);
            const relNode: SchemaRelation = this.expectRelationType(rel.label);
            this.propagateRelatedRoles(relNode, role);
        }
    }

    private propagateRelatedRoles(relNode: SchemaRelation, role: RoleType) {
        relNode.relatedRoles.push(role);
        for (const relSubnode of relNode.subtypes) {
            this.propagateRelatedRoles(relSubnode, role);
        }
    }

    private attachPlayedRoles() {
        for (const answer of this.playedRoles.answers) {
            const [obj, role] = [answer.data["t"], answer.data["played"]];
            if (!obj || !role || role.kind !== "roleType") throw this.unexpectedPlayedRolesAnswer(answer);
            let objNode: SchemaEntity | SchemaRelation;
            switch (obj.kind) {
                case "entityType":
                    objNode = this.expectEntityType(obj.label);
                    break;
                case "relationType":
                    objNode = this.expectRelationType(obj.label);
                    break;
                default:
                    throw this.unexpectedPlayedRolesAnswer(answer);
            }
            this.propagatePlayedRoles(objNode, role);
        }
    }

    private propagatePlayedRoles(objNode: SchemaEntity | SchemaRelation, role: RoleType) {
        objNode.playedRoles.push(role);
        for (const objSubnode of objNode.subtypes) {
            this.propagatePlayedRoles(objSubnode, role);
        }
    }

    private expectEntityType(label: string): SchemaEntity {
        const type = this.entityTypes[label];
        if (!type) throw `Missing expected entity type in schema with label '${label}'`;
        return type;
    }

    private expectRelationType(label: string): SchemaRelation {
        const type = this.relationTypes[label];
        if (!type) throw `Missing expected relation type in schema with label '${label}'`;
        return type;
    }

    private expectAttributeType(label: string): SchemaAttribute {
        const type = this.attributeTypes[label];
        if (!type) throw `Missing expected attribute type in schema with label '${label}'`;
        return type;
    }

    private unexpectedTypeHierarchyAnswer(answer: ConceptRowsQueryResponse["answers"][number]) {
        return `Unexpected type hierarchy answer: ${JSON.stringify(answer.data)}`;
    }

    private unexpectedOwnedAttributesAnswer(answer: ConceptRowsQueryResponse["answers"][number]) {
        return `Unexpected owned attributes answer: ${JSON.stringify(answer.data)}`;
    }

    private unexpectedPlayedRolesAnswer(answer: ConceptRowsQueryResponse["answers"][number]) {
        return `Unexpected played roles answer: ${JSON.stringify(answer.data)}`;
    }

    private unexpectedRoleplayersAnswer(answer: ConceptRowsQueryResponse["answers"][number]) {
        return `Unexpected related roles answer: ${JSON.stringify(answer.data)}`;
    }
}

export class VisualiserState {

    private _status: VisualiserStatus = "ok";
    canvasEl$ = new BehaviorSubject<HTMLElement | null>(null);
    visualiser: GraphVisualiser | null = null;
    database?: string;
    savedState?: SigmaState;

    get status() {
        return this._status;
    }

    set status(value: VisualiserStatus) {
        console.info(value);
        this._status = value;
    }

    constructor() {
        this.canvasEl$.subscribe(el => {
            if (el && this.savedState && this.database) {
                this._status = "ok";
                const graph = newVisualGraph();
                const sigma = createSigmaRenderer(el, defaultSigmaSettings as any, graph);
                const layout = Layouts.createForceAtlasStatic(graph, undefined);
                this.visualiser = new GraphVisualiser(graph, sigma, layout);
                this.restoreState(this.savedState, sigma);
            }
        });
    }

    push(res: ApiResponse<QueryResponse>) {
        if (!this.canvasEl$.value) throw `Missing canvas element`;

        if (!this.visualiser) {
            const graph = newVisualGraph();
            const sigma = createSigmaRenderer(this.canvasEl$.value, defaultSigmaSettings as any, graph);
            const layout = Layouts.createForceAtlasStatic(graph, undefined); // This is the safe option
            // const layout = Layouts.createForceLayoutSupervisor(graph, studioDefaults.defaultForceSupervisorSettings);
            this.visualiser = new GraphVisualiser(graph, sigma, layout);
        }

        if (isApiErrorResponse(res)) {
            this.destroy();
            this.status = "error";
            return;
        }

        switch (res.ok.answerType) {
            case "conceptRows": {
                this.visualiser.handleQueryResponse(res, this.database!);
                this.visualiser.colorEdgesByConstraintIndex(false);
                break;
            }
            default:
                this.status = "error";
                throw `Unexpected answerType: '${res.ok.answerType}' (expected 'conceptRows')`;
        }
    }

    destroy() {
        if (this.visualiser) {
            this.savedState = this.saveState(this.visualiser.sigma);
            this.visualiser.destroy();
            this.visualiser = null;
        }
    }

    saveState(sigma: Sigma): SigmaState {
        const graph = sigma.getGraph().copy();
        const camera = sigma.getCamera().copy();
        const settings = sigma.getSettings();
        return { graph, camera, settings };
    }

    restoreState(state: SigmaState, sigma: Sigma) {
        if (state.graph) {
            sigma.getGraph().clear();
            sigma.getGraph().import(state.graph);
        }
        if (state.camera) sigma.getCamera().setState(state.camera);
        if (state.settings) sigma.setSettings(state.settings);
    }

    dropSavedState() {
        this.savedState = undefined;
    }
}

export interface SigmaState {
    graph: Graph;
    camera: Camera;
    settings: any;
}
