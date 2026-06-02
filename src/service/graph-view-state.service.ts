/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable, inject } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { ApiResponse, isApiErrorResponse, QueryResponse } from "@typedb/driver-http";
import { DriverState } from "./driver-state.service";
import { SchemaConcept } from "./schema-state.service";
import { GraphStyleService } from "./graph-style.service";
import { AppData } from "./app-data.service";
import { createRunOutputState, RunOutputState } from "./query-page-state.service";

export interface GraphViewTab {
    type: SchemaConcept;
    run: RunOutputState;
}

export interface OpenTypeTabOptions {
    /** Also fetch relations the instances participate in. Defaults to false. */
    includeLinks?: boolean;
    /** Also fetch instance attributes. Defaults to false. */
    includeAttributes?: boolean;
}

@Injectable({ providedIn: "root" })
export class GraphViewState {

    private driver = inject(DriverState);
    private graphStyleService = inject(GraphStyleService);
    private appData = inject(AppData);

    openTabs$ = new BehaviorSubject<GraphViewTab[]>([]);
    selectedTabIndex$ = new BehaviorSubject<number>(0);

    constructor() {
        // Clear tabs when database changes — instances are database-scoped.
        this.driver.database$.subscribe(() => {
            for (const tab of this.openTabs$.value) {
                tab.run.graph.destroy();
            }
            this.openTabs$.next([]);
            this.selectedTabIndex$.next(0);
        });
    }

    async openTypeTab(type: SchemaConcept, options: OpenTypeTabOptions = {}): Promise<void> {
        // Same type → same tab; subsequent opens add to the existing graph.
        const tabs = this.openTabs$.value;
        let tab = tabs.find(t => t.type.label === type.label);
        if (!tab) {
            const baseQuery = `match $${this.instanceVar(type)} isa ${type.label};`;
            const run = createRunOutputState(type.label, baseQuery, this.graphStyleService);
            tab = { type, run };
            this.openTabs$.next([...tabs, tab]);
            this.selectedTabIndex$.next(this.openTabs$.value.indexOf(tab));
        } else {
            this.selectedTabIndex$.next(tabs.indexOf(tab));
        }

        const run = tab.run;
        run.graph.status = "running";
        run.graph.database = this.driver.requireDatabase().name;
        run.graph.query = `match $${this.instanceVar(type)} isa ${type.label};`;

        try {
            const iids = await this.fetchInstancesOfType(run, type);
            if (iids.length > 0 && type.kind !== "attributeType") {
                const tasks: Promise<void>[] = [];
                if (options.includeAttributes) tasks.push(this.fetchAttributesOf(run, type, iids));
                if (options.includeLinks) tasks.push(this.fetchLinksOf(run, type, iids));
                await Promise.all(tasks);
            }
            run.graph.status = "ok";
        } catch (err) {
            console.error("[Graph fetch]", err);
            run.graph.status = "error";
        }
    }

    closeTab(tab: GraphViewTab) {
        const tabs = this.openTabs$.value;
        const index = tabs.indexOf(tab);
        if (index === -1) return;
        tab.run.graph.destroy();
        const newTabs = tabs.filter(t => t !== tab);
        this.openTabs$.next(newTabs);
        const selected = this.selectedTabIndex$.value;
        if (selected >= newTabs.length && newTabs.length > 0) {
            this.selectedTabIndex$.next(newTabs.length - 1);
        }
    }

    closeAllTabs() {
        for (const tab of this.openTabs$.value) {
            tab.run.graph.destroy();
        }
        this.openTabs$.next([]);
        this.selectedTabIndex$.next(0);
    }

    /**
     * Fetch instances of `type` into `run.graph`. Returns IIDs of the returned
     * entities/relations (empty for attribute types). Pushes results via
     * `GraphOutputState.push`, which dedupes by IID/label.
     */
    async fetchInstancesOfType(run: RunOutputState, type: SchemaConcept): Promise<string[]> {
        const rowLimit = this.appData.preferences.queryRowLimit();
        const query = `match $${this.instanceVar(type)} isa ${type.label};`;
        const res = await this.runQuery(query, rowLimit);
        if (isApiErrorResponse(res)) return [];
        this.pushSafely(run, res);
        return this.extractIids(res, this.instanceVar(type));
    }

    /**
     * Fetch attributes of the given IIDs (must be entities or relations of `type`)
     * into `run.graph`. No-op for attribute types.
     */
    async fetchAttributesOf(run: RunOutputState, type: SchemaConcept, iids: string[]): Promise<void> {
        if (iids.length === 0) return;
        const queries = this.buildAttributeQueries(type, iids);
        const rowLimit = this.appData.preferences.queryRowLimit();
        await Promise.all(queries.map(q => this.runAndPush(run, q, rowLimit * 50)));
    }

    /**
     * Like `fetchAttributesOf`, but scoped to one specific attribute type
     * label — used by the per-row "Add" affordance in the Inspector's
     * Attributes section to pull in just one attribute kind.
     */
    async fetchAttributesOfTypeFor(
        run: RunOutputState,
        type: SchemaConcept,
        iids: string[],
        attrTypeLabel: string,
    ): Promise<void> {
        if (iids.length === 0) return;
        const rowLimit = this.appData.preferences.queryRowLimit();
        const instanceVar = this.instanceVar(type);
        if (instanceVar === "a") return; // attribute types don't have own attributes
        const branches = iids.map(iid => `{ $${instanceVar} iid ${iid}; }`).join(" or ");
        const query = `match ${branches}; $${instanceVar} has ${attrTypeLabel} $a;`;
        await this.runAndPush(run, query, rowLimit * 50);
    }

    /**
     * Fetch links (relation participation + role players) of the given IIDs
     * (must be entities or relations of `type`) into `run.graph`. IID-anchored
     * patterns sidestep TypeQL role-inference issues where a relation variable
     * is typed only by its players' types.
     */
    async fetchLinksOf(run: RunOutputState, type: SchemaConcept, iids: string[]): Promise<void> {
        if (iids.length === 0) return;
        const queries = this.buildLinksQueries(type, iids);
        const rowLimit = this.appData.preferences.queryRowLimit();
        await Promise.all(queries.map(q => this.runAndPush(run, q, rowLimit * 50)));
    }

    /**
     * Add a single relation instance to the graph along with its role players
     * and role edges — *nothing else*. Used by the Inspector's per-relation
     * "Explore" affordance. Distinct from `fetchLinksOf` (which also pulls in
     * secondary relations that the players happen to participate in — way too
     * much for a focused exploration of one relation).
     *
     * Binds the role variable so the graph builder can construct the role
     * edge between $r and $p — without it the edge is missing.
     */
    async fetchRelation(run: RunOutputState, relationIID: string): Promise<void> {
        const rowLimit = this.appData.preferences.queryRowLimit();
        const query = `match $r iid ${relationIID}; $r links ($role: $p);`;
        await this.runAndPush(run, query, rowLimit * 50);
    }

    /**
     * Add a specific role-player of a relation to the graph, including the
     * relation node and the role edge between them. Used by the Inspector's
     * per-role-player "Explore" affordance — the relation is included too so
     * the player has its connecting edge regardless of current graph state.
     *
     * If `originalIID` is supplied and differs from both the new player and
     * the relation, the query also pulls in the role edge from the relation
     * back to the original — so when the user explores a co-player from
     * inside the relation card of an entity they're inspecting, the result
     * stays connected to that entity in the graph.
     */
    async fetchPlayerInRelation(
        run: RunOutputState,
        relationIID: string,
        playerIID: string,
        originalIID?: string,
    ): Promise<void> {
        const rowLimit = this.appData.preferences.queryRowLimit();
        let query = `match $r iid ${relationIID}; $r links ($role: $p); $p iid ${playerIID};`;
        if (originalIID && originalIID !== playerIID && originalIID !== relationIID) {
            query += ` $r links ($origRole: $orig); $orig iid ${originalIID};`;
        }
        await this.runAndPush(run, query, rowLimit * 50);
    }

    private async runAndPush(run: RunOutputState, query: string, rowLimit: number): Promise<void> {
        try {
            const res = await this.runQuery(query, rowLimit);
            if (!isApiErrorResponse(res)) this.pushSafely(run, res);
        } catch (err) {
            console.error("[Graph query]", err);
        }
    }

    private runQuery(query: string, rowLimit: number): Promise<ApiResponse<QueryResponse>> {
        return new Promise((resolve, reject) => {
            this.driver.query(query, { answerCountLimit: rowLimit }).subscribe({
                next: (res) => resolve(res),
                error: (err) => reject(err),
            });
        });
    }

    private pushSafely(run: RunOutputState, res: ApiResponse<QueryResponse>): void {
        try { run.graph.push(res); } catch (err) { console.error("[Graph push]", err); }
    }

    private instanceVar(type: SchemaConcept): string {
        switch (type.kind) {
            case "entityType": return "x";
            case "relationType": return "r";
            case "attributeType": return "a";
        }
    }

    private extractIids(res: ApiResponse<QueryResponse>, varName: string): string[] {
        if (isApiErrorResponse(res)) return [];
        if (res.ok.answerType !== "conceptRows") return [];
        const iids: string[] = [];
        const seen = new Set<string>();
        for (const answer of res.ok.answers) {
            const concept = (answer.data as Record<string, any>)[varName];
            if (concept && (concept.kind === "entity" || concept.kind === "relation") && concept.iid) {
                if (!seen.has(concept.iid)) {
                    seen.add(concept.iid);
                    iids.push(concept.iid);
                }
            }
        }
        return iids;
    }

    private buildAttributeQueries(type: SchemaConcept, iids: string[]): string[] {
        if (type.kind === "entityType") {
            const branches = iids.map(iid => `{ $x iid ${iid}; }`).join(" or ");
            return [`match ${branches}; $x has $a;`];
        }
        if (type.kind === "relationType") {
            const branches = iids.map(iid => `{ $r iid ${iid}; }`).join(" or ");
            return [`match ${branches}; $r has $a;`];
        }
        return [];
    }

    private buildLinksQueries(type: SchemaConcept, iids: string[]): string[] {
        if (type.kind === "entityType") {
            const branches = iids.map(iid => `{ $x iid ${iid}; }`).join(" or ");
            return [`match ${branches}; $r links ($x); $r links ($other);`];
        }
        if (type.kind === "relationType") {
            const branches = iids.map(iid => `{ $r iid ${iid}; }`).join(" or ");
            return [
                `match ${branches}; $r links ($p);`,
                `match ${branches}; $r links ($p); $p has $pa;`,
                `match ${branches}; $r links ($p); $r2 links ($p); $r2 links ($other);`,
            ];
        }
        return [];
    }
}
