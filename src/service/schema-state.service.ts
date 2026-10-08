/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { inject, Injectable } from "@angular/core";
import {
    ApiOkResponse, ApiResponse, AttributeType, ConceptRowsQueryResponse, EntityType,
    isApiErrorResponse, QueryResponse, RelationType, RoleType, Type
} from "@typedb/driver-http";
import Graph from "graphology";
import { BehaviorSubject, catchError, combineLatest, defer, distinctUntilChanged, EMPTY, first, map, Observable, of, Subject, switchMap, tap, toArray } from "rxjs";
import Sigma, { Camera } from "sigma";
import { GraphVisualiser } from "../framework/graph-visualiser/engine";
import { createSigmaRenderer, defaultSigmaSettings, WebGLUnavailableError } from "../framework/graph-visualiser/engine/sigma-settings";
import { newGraph } from "../framework/graph-visualiser/engine/graph";
import { Layouts } from "../framework/graph-visualiser/engine/layout";
import { DriverState } from "./driver-state.service";
import { GraphStyleService } from "./graph-style.service";
import { SnackbarService } from "./snackbar.service";
import {updateAutocomleteSchemaFromDB, updateAutocompleteFunctionsFromSchemaText} from "../framework/codemirror-lang-typeql";

const NO_SERVER_CONNECTED = `No server connected`;
const NO_DATABASE_SELECTED = `No database selected`;

const schemaQueries = {
    typeHierarchy: `match { $t sub! $supertype; } or {$t sub $supertype; $t is $supertype; };`,
    ownedAttributes: `match { $t owns $attr; not { $t sub! $sown; $sown owns $attr; }; };`,
    relatedRoles: `match { $t relates $related; not { $t sub! $srel; $srel relates $related; };  };`,
    playedRoles: `match { $t plays $played; not { $t sub! $splay; $splay plays $played; }; };`,
} as const satisfies Record<string, string>;
const schemaQueriesList = Object.values(schemaQueries);

/**
 * Queries for the part of the schema around `root`: the root and its subtypes, their capabilities,
 * and the types on the far side of each capability (owners of an attribute root, and the relations
 * or players at the other end of every role in scope). The root is shown with its inherited
 * capabilities, since its supertypes are out of scope; its subtypes, as in the full schema, only
 * with the capabilities they declare.
 *
 * Constraints between two variables draw edges in the visualiser, while labels draw nothing, so the
 * scope is pinned with labels and the edge-drawing constraints mirror those in `schemaQueries`.
 */
function subSchemaQueries(root: SchemaConcept, schema: Schema): string[] {
    const label = root.label;
    const inScope = collectSubtypes(root);
    const inScopeLabels = new Set(inScope.map(x => x.label));
    const capability = (cap: "owns" | "plays" | "relates", v: string) =>
        `match { $t label ${label}; $t ${cap} $${v}; } or { $t sub ${label}; not { $t label ${label}; }; $t ${cap} $${v}; `
        + `not { $t sub! $s; $s ${cap} $${v}; }; };`;
    const roleDisjunction = (v: string, roles: string[]) => roles.map(r => `{ $${v} label ${r}; }`).join(" or ");

    const queries = [`match { $t sub! $supertype; $supertype sub ${label}; } or { $t sub ${label}; $t is $supertype; };`];
    if (root.kind === "attributeType") {
        const owned = [...Object.values(schema.entities), ...Object.values(schema.relations)]
            .some(owner => owner.ownedAttributes.some(attr => inScopeLabels.has(attr.label)));
        if (owned) queries.push(`match $attr sub ${label}; $t owns $attr; not { $t sub! $s; $s owns $attr; };`);
        return queries;
    }

    // Only query for capabilities that something in scope has: TypeDB may reject a query outright
    // when type inference finds no type that could ever satisfy it.
    const objects = inScope as (SchemaEntity | SchemaRelation)[];
    if (objects.some(x => x.ownedAttributes.length)) queries.push(capability("owns", "attr"));
    const playedRoles = uniqueLabels(objects.flatMap(x => x.playedRoles));
    if (playedRoles.length) {
        queries.push(capability("plays", "played"));
        queries.push(`match $t relates $related; ${roleDisjunction("related", playedRoles)}; `
            + `not { $t sub! $s; $s relates $related; };`);
    }
    if (root.kind === "relationType") {
        queries.push(capability("relates", "related"));
        const relatedRoles = uniqueLabels((objects as SchemaRelation[]).flatMap(x => x.relatedRoles));
        // A relation may have no players yet; the role labels alone don't tell us.
        const played = [...Object.values(schema.entities), ...Object.values(schema.relations)]
            .some(player => player.playedRoles.some(role => relatedRoles.includes(role.label)));
        if (played) {
            queries.push(`match $t plays $played; ${roleDisjunction("played", relatedRoles)}; `
                + `not { $t sub! $s; $s plays $played; };`);
        }
    }
    return queries;
}

function collectSubtypes(root: SchemaConcept): SchemaConcept[] {
    return [root, ...(root.subtypes as SchemaConcept[]).flatMap(collectSubtypes)];
}

function uniqueLabels(roles: SchemaRole[]): string[] {
    return [...new Set(roles.map(x => x.label))];
}

/** The `sub` links from `root` up to its root type, and down through all its subtypes. */
function typeHierarchyQuery(root: SchemaConcept): string {
    const label = root.label;
    return `match { $t sub! $supertype; $supertype sub ${label}; } or { ${label} sub $t; $t sub! $supertype; } `
        + `or { $t label ${label}; $t is $supertype; };`;
}

/** A part of the schema shown in the visualiser instead of the full schema. */
export interface SchemaFocus {
    view: "subSchema" | "typeHierarchy";
    root: SchemaConcept;
}

type VisualiserRequest = { kind: "refresh" } | { kind: "fullSchema" } | { kind: "focus", focus: SchemaFocus };

type VisualiserStatus = "ok" | "running" | "emptySchema" | "error" | "webglUnavailable";

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

    private graphStyleService = inject(GraphStyleService);
    readonly visualiser = new VisualiserState(this.graphStyleService);
    queryResponses$ = new BehaviorSubject<ApiOkResponse<ConceptRowsQueryResponse>[] | null>(null);
    /** What the visualiser should draw: the full schema (`queryResponses$`), or the part in `focus$`. */
    readonly visualiserResponses$ = new BehaviorSubject<ApiOkResponse<ConceptRowsQueryResponse>[] | null>(null);
    /** The part of the schema the visualiser is showing, or null when it shows the full schema. */
    readonly focus$ = new BehaviorSubject<SchemaFocus | null>(null);
    readonly value$ = new BehaviorSubject<Schema | null>(null);
    private readonly visualiserRequest$ = new Subject<VisualiserRequest>();
    readonly interactionDisabledReason$ = combineLatest([this.driver.status$, this.driver.database$]).pipe(map(([status, db]) => {
        if (status !== "connected") return NO_SERVER_CONNECTED;
        else if (db == null) return NO_DATABASE_SELECTED;
        else return null;
    }));
    readonly interactable$ = this.interactionDisabledReason$.pipe(map(x => x == null));

    constructor(private driver: DriverState, private snackbar: SnackbarService) {
        (window as any)["schemaState"] = this;
        // Latest request wins: a refresh already in flight read a pre-commit snapshot,
        // so cancel it rather than letting it finish and overwrite the tree.
        this.visualiserRequest$.pipe(
            switchMap(req => {
                // Must stay inside the switchMap: an error escaping here would kill visualiserRequest$ for good.
                return this.runRequest$(req).pipe(catchError(err => { this.handleQueryError(err); return EMPTY; }));
            }),
        ).subscribe();
        this.driver.database$.pipe(
            distinctUntilChanged((x, y) => x?.name === y?.name)
        ).subscribe(() => {
            this.refresh();
        });
        this.driver.schemaCommitted$.subscribe(() => {
            this.refresh();
        });
        // Uncommitted schema edits in an open manual transaction (and their discard on
        // rollback/close) — refresh reads through the open transaction, so it sees them.
        this.driver.schemaChanged$.subscribe(() => {
            this.refresh();
        });
        this.queryResponses$.subscribe(data => {
            this.push(data);
        });
        this.value$.subscribe(schema => {
            if (schema != null) {
                updateAutocomleteSchemaFromDB(schema)
                this.refreshAutocompleteFunctions();
            }
        })
    }

    /** Function definitions aren't visible to the schema concept queries — they only
     *  exist in the database's schema text, so fetch that separately. */
    private refreshAutocompleteFunctions() {
        try {
            this.driver.getDatabaseSchemaText().subscribe(res => {
                if (!isApiErrorResponse(res)) updateAutocompleteFunctionsFromSchemaText(res.ok);
            });
        } catch (e) {
            // No driver/database (e.g. disconnected mid-refresh) — keep existing completions.
        }
    }

    refresh() {
        this.visualiserRequest$.next({ kind: "refresh" });
    }

    /** Shows only part of the schema in the visualiser. The tree keeps the full schema. */
    loadFocus(focus: SchemaFocus) {
        this.visualiserRequest$.next({ kind: "focus", focus });
    }

    /** Returns the visualiser from a focused view to the full schema, without querying again. */
    showFullSchema() {
        this.visualiserRequest$.next({ kind: "fullSchema" });
    }

    private runRequest$(req: VisualiserRequest): Observable<unknown> {
        return defer(() => {
            this.visualiser.dropSavedState();

            const db = this.driver.database$.value;
            if (db == null) {
                this.focus$.next(null);
                this.queryResponses$.next(null);
                this.visualiserResponses$.next(null);
                this.visualiser.destroy();
                this.visualiser.database = undefined;
                return EMPTY;
            }

            this.initialiseOutput();
            // Otherwise a schema page mounting mid-request would draw the previous graph, and then
            // skip the new one because a visualiser already exists.
            this.visualiserResponses$.next(null);
            switch (req.kind) {
                case "refresh": return this.refreshFullSchema$();
                case "fullSchema": return this.showInVisualiser$(of(this.queryResponses$.value ?? []), null);
                case "focus": return this.showInVisualiser$(this.fetchFocus$(req.focus), req.focus);
            }
        });
    }

    private refreshFullSchema$(): Observable<unknown> {
        // Server defaults this to 10k rows, which is exceeded by hierarchy/owns/plays/relates
        // queries on very large schemas — silently truncating the tree. Lift the cap so we get
        // a complete picture even on big schemas.
        return this.runSchemaQueries$(schemaQueriesList).pipe(
            switchMap(responses => {
                this.queryResponses$.next(responses);
                // Stay focused across schema changes, unless the focused type no longer exists.
                const focus = this.focus$.value;
                const root = this.findConcept(focus?.root.label);
                return focus && root
                    ? this.showInVisualiser$(this.fetchFocus$({ view: focus.view, root }), { view: focus.view, root })
                    : this.showInVisualiser$(of(responses), null);
            }),
        );
    }

    private fetchFocus$(focus: SchemaFocus): Observable<ApiOkResponse<ConceptRowsQueryResponse>[]> {
        return defer(() => this.runSchemaQueries$(focus.view === "subSchema"
            ? subSchemaQueries(focus.root, this.value$.value!)
            : [typeHierarchyQuery(focus.root)]));
    }

    private runSchemaQueries$(queries: string[]): Observable<ApiOkResponse<ConceptRowsQueryResponse>[]> {
        return this.driver.runBackgroundReadQueries(queries, { answerCountLimit: 100000 }).pipe(
            map(res => {
                if (res.ok.answerType !== `conceptRows`) throw `Unexpected answerType: '${res.ok.answerType}' (expected 'conceptRows')`;
                return res as ApiOkResponse<ConceptRowsQueryResponse>;
            }),
            toArray(),
        );
    }

    private showInVisualiser$(responses$: Observable<ApiOkResponse<ConceptRowsQueryResponse>[]>, focus: SchemaFocus | null): Observable<unknown> {
        this.focus$.next(focus);
        return responses$.pipe(
            tap(responses => {
                this.visualiserResponses$.next(responses);
                if (this.visualiser.status === "running") {
                    this.visualiser.status = responses[0]?.ok.answers.length ? "ok" : "emptySchema";
                }
            }),
        );
    }

    private findConcept(label: string | undefined): SchemaConcept | null {
        const schema = this.value$.value;
        if (label == null || schema == null) return null;
        return schema.entities[label] ?? schema.relations[label] ?? schema.attributes[label] ?? null;
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
        this.visualiser.database = this.driver.requireDatabase().name;
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
            if (!rel || !role || rel.kind !== "relationType" || role.kind !== "roleType") throw this.unexpectedLinksAnswer(answer);
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

    private unexpectedLinksAnswer(answer: ConceptRowsQueryResponse["answers"][number]) {
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
        this._status = value;
    }

    constructor(private styleService: GraphStyleService) {
        this.canvasEl$.subscribe(el => {
            if (el && this.savedState && this.database) {
                // The canvas can re-emit while a visualiser is still live (host
                // remounts); destroy it first or its document-level listeners
                // leak for the rest of the session.
                this.destroy();
                this._status = "ok";
                try {
                    const graph = newGraph();
                    const sigma = createSigmaRenderer(el, defaultSigmaSettings as any, graph);
                    const layout = Layouts.createD3ForceSupervisor(graph);
                    this.visualiser = new GraphVisualiser(graph, sigma, layout, this.styleService);
                    this.restoreState(this.savedState, sigma);
                } catch (err) {
                    if (!(err instanceof WebGLUnavailableError)) throw err;
                    this._status = "webglUnavailable";
                }
            }
        });
    }

    /** Takes every response for the graph at once, so the layout starts with all their nodes. */
    push(responses: ApiResponse<QueryResponse>[]) {
        if (!this.canvasEl$.value) throw `Missing canvas element`;

        if (!this.visualiser) {
            try {
                const graph = newGraph();
                const sigma = createSigmaRenderer(this.canvasEl$.value, defaultSigmaSettings as any, graph);
                const layout = Layouts.createD3ForceSupervisor(graph);
                this.visualiser = new GraphVisualiser(graph, sigma, layout, this.styleService);
            } catch (err) {
                if (!(err instanceof WebGLUnavailableError)) throw err;
                this.status = "webglUnavailable";
                return;
            }
        }

        if (responses.some(res => isApiErrorResponse(res))) {
            this.destroy();
            this.status = "error";
            return;
        }

        const unexpected = responses.find(res => !isApiErrorResponse(res) && res.ok.answerType !== "conceptRows");
        if (unexpected && !isApiErrorResponse(unexpected)) {
            this.status = "error";
            throw `Unexpected answerType: '${unexpected.ok.answerType}' (expected 'conceptRows')`;
        }

        this.visualiser.handleQueryResponse(responses, this.database!);
        this.visualiser.colorEdgesByConstraintIndex(!this.styleService?.colorEdgesByConstraint);
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
