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

const NO_SERVER_CONNECTED = `No server connected`;
const NO_DATABASE_SELECTED = `No database selected`;

const schemaQueries = {
    typeHierarchy: `match { $t sub! $supertype; } or {$t sub $supertype; $t is $supertype; };`,
    ownedAttributes: `match { $t owns $attr; not { $t sub! $sown; $sown owns $attr; }; };`,
    roleplayers: `match { $t relates $related; not { $t sub! $srel; $srel relates $related; };  };`,
    playableRoles: `match { $t plays $played; not { $t sub! $splay; $splay plays $played; }; };`,
} as const satisfies Record<string, string>;
const schemaQueriesList = Object.values(schemaQueries);

type VisualiserStatus = "ok" | "running" | "noAnswers" | "error";

@Injectable({
    providedIn: "root",
})
export class SchemaState {

    readonly visualiser = new VisualiserState();
    readonly tree = new TreeState();
    queryResponses$ = new BehaviorSubject<ApiOkResponse<ConceptRowsQueryResponse>[] | null>(null);
    isRefreshing = false;
    readonly refreshDisabledReason$ = combineLatest([this.driver.status$, this.driver.database$]).pipe(map(([status, db]) => {
        if (status !== "connected") return NO_SERVER_CONNECTED;
        else if (db == null) return NO_DATABASE_SELECTED;
        else return null;
    }));
    readonly refreshEnabled$ = this.refreshDisabledReason$.pipe(map(x => x == null));

    constructor(private driver: DriverState, private snackbar: SnackbarService) {
        (window as any)["schemaToolState"] = this;
        this.driver.database$.pipe(
            distinctUntilChanged((x, y) => x?.name === y?.name)
        ).subscribe(() => {
            this.refresh();
        });
        this.queryResponses$.subscribe(data => {
            this.tree.push(data);
        });
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
                complete: () => { this.queryResponses$.next(responses); },
            });
        });
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

export class VisualiserState {

    status: VisualiserStatus = "ok";
    canvasEl$ = new BehaviorSubject<HTMLElement | null>(null);
    visualiser: GraphVisualiser | null = null;
    database?: string;
    savedState?: SigmaState;

    constructor() {
        this.canvasEl$.subscribe(el => {
            if (el && this.savedState && this.database) {
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

interface SchemaTreeEntity extends EntityType {
    supertype?: SchemaTreeEntity;
    ownedAttributes: SchemaTreeAttribute[];
    playableRoles: SchemaTreeRole[];
}

interface SchemaTreeRelation extends RelationType {
    supertype?: SchemaTreeRelation;
    ownedAttributes: SchemaTreeAttribute[];
    playableRoles: SchemaTreeRole[];
    roleplayers: SchemaTreeRole[];
}

interface SchemaTreeAttribute extends AttributeType {
    supertype?: SchemaTreeAttribute;
}

export type SchemaTreeType = SchemaTreeEntity | SchemaTreeRelation | SchemaTreeAttribute;

interface SchemaTreeRole extends RoleType {
    relationLabel: string;
}

export interface SchemaTree {
    entities: SchemaTreeEntity[];
    relations: SchemaTreeRelation[];
    attributes: SchemaTreeAttribute[];
}

function entityOf(entityType: EntityType): SchemaTreeEntity {
    return {
        kind: entityType.kind,
        label: entityType.label,
        supertype: undefined,
        ownedAttributes: [],
        playableRoles: [],
    };
}

function relationOf(relationType: RelationType): SchemaTreeRelation {
    return {
        kind: relationType.kind,
        label: relationType.label,
        supertype: undefined,
        ownedAttributes: [],
        playableRoles: [],
        roleplayers: [],
    };
}

function attributeOf(attributeType: AttributeType): SchemaTreeAttribute {
    return {
        kind: attributeType.kind,
        label: attributeType.label,
        supertype: undefined,
        valueType: attributeType.valueType,
    };
}

function typeOf(type: Type): SchemaTreeType {
    switch (type.kind) {
        case "entityType": return entityOf(type);
        case "relationType": return relationOf(type);
        case "attributeType": return attributeOf(type);
        default: throw `Unexpected type: ${JSON.stringify(type)}`;
    }
}

export class TreeState {
    readonly data$ = new BehaviorSubject<SchemaTree | null>(null);

    push(data: ApiOkResponse<ConceptRowsQueryResponse>[] | null) {
        if (!data) {
            this.data$.next(null);
            return;
        }

        const treeBuilder = new TreeBuilder(data);
        const tree = treeBuilder.build();
        this.data$.next(tree);
    }
}

class TreeBuilder {
    readonly typeHierarchy: ConceptRowsQueryResponse;
    readonly ownedAttributes: ConceptRowsQueryResponse;
    readonly roleplayers: ConceptRowsQueryResponse;
    readonly playableRoles: ConceptRowsQueryResponse;
    readonly entityTypes = {} as Record<string, SchemaTreeEntity>;
    readonly relationTypes = {} as Record<string, SchemaTreeRelation>;
    readonly attributeTypes = {} as Record<string, SchemaTreeAttribute>;

    constructor(data: ApiOkResponse<ConceptRowsQueryResponse>[]) {
        const [typeHierarchy, ownedAttributes, roleplayers, playableRoles] = data.map(x => x.ok);
        this.typeHierarchy = typeHierarchy;
        this.ownedAttributes = ownedAttributes;
        this.roleplayers = roleplayers;
        this.playableRoles = playableRoles;
    }

    build(): SchemaTree {
        this.processTypeHierarchy();
        return {
            entities: Object.values(this.entityTypes),
            relations: Object.values(this.relationTypes),
            attributes: Object.values(this.attributeTypes),
        };
    }

    processTypeHierarchy() {
        for (const answer of this.typeHierarchy.answers) {
            const [type, supertype] = [answer.data["t"], answer.data["supertype"]];
            if (!type || !supertype) throw `Unexpected type hierarchy answer: ${JSON.stringify(answer.data)}`;
            let treeType: SchemaTreeType;
            switch (type.kind) {
                case "entityType":
                    this.entityTypes[type.label] = treeType = entityOf(type);
                    break;
                case "relationType":
                    this.relationTypes[type.label] = treeType = relationOf(type);
                    break;
                case "attributeType":
                    this.attributeTypes[type.label] = treeType = attributeOf(type);
                    break;
                case "roleType":
                    return;
                default:
                    throw `Unexpected type hierarchy answer: ${JSON.stringify(answer.data)}`;
            }
            if (supertype.kind !== type.kind) throw `Unexpected type hierarchy answer: ${JSON.stringify(answer.data)}`;
            if (type.label !== supertype.label) {
                treeType.supertype = typeOf(supertype);
            }
        }
    }
}
