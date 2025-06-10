/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import Graph from "graphology";
import { BehaviorSubject, combineLatest, distinctUntilChanged, finalize, first, map, ReplaySubject, startWith } from "rxjs";
import Sigma from "sigma";
import { createSigmaRenderer, GraphVisualiser } from "../framework/graph-visualiser";
import { defaultSigmaSettings } from "../framework/graph-visualiser/defaults";
import { newVisualGraph } from "../framework/graph-visualiser/graph";
import { Layouts } from "../framework/graph-visualiser/layouts";
import { Database } from "../framework/typedb-driver/database";
import { ApiResponse, isApiErrorResponse, QueryResponse } from "../framework/typedb-driver/response";
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
    queryResponses$ = new ReplaySubject<ApiResponse<QueryResponse>[]>(1);
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
    }

    refresh() {
        if (this.isRefreshing) return;

        this.driver.database$.pipe(first()).subscribe(db => {
            if (db == null) {
                this.visualiser.destroy();
                return;
            }

            this.initialiseOutput();
            const responses: ApiResponse<QueryResponse>[] = [];
            this.isRefreshing = true;
            this.driver.runBackgroundReadQueries(schemaQueriesList).pipe(
                finalize(() => { this.isRefreshing = false; })
            ).subscribe({
                next: (res) => { responses.push(res); },
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
                    + `Caused: Failed to execute query.`);
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
            if (el && this.savedState) {
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

    saveState(sigma: Sigma) {
        // Save the graph data
        const graph = sigma.getGraph().copy();

        // Save camera position/zoom
        const cameraState = {
            x: sigma.getCamera().x,
            y: sigma.getCamera().y,
            ratio: sigma.getCamera().ratio,
            angle: sigma.getCamera().angle
        };

        // Save any custom settings
        const settings = sigma.getSettings();

        return new SigmaState(graph, cameraState, settings);
    }

    restoreState(state: SigmaState, sigma: Sigma) {
        if (state.graph) {
            // Clear current graph and import saved state
            sigma.getGraph().clear();
            sigma.getGraph().import(state.graph);
        }

        if (state.cameraState) {
            // Restore camera position
            sigma.getCamera().setState(state.cameraState);
        }

        if (state.settings) {
            sigma.setSettings(state.settings);
        }
    }
}

export class SigmaState {
    constructor(public graph: Graph, public cameraState: any, public settings: any) {
    }
}
