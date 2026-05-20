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

    openTypeTab(type: SchemaConcept) {
        const tabs = this.openTabs$.value;
        const existing = tabs.find(t => t.type.label === type.label);
        if (existing) {
            this.selectedTabIndex$.next(tabs.indexOf(existing));
            return;
        }
        const label = `match $${this.instanceVar(type)} isa ${type.label};`;
        const run = createRunOutputState(type.label, label, this.graphStyleService);
        const tab: GraphViewTab = { type, run };
        this.openTabs$.next([...tabs, tab]);
        this.selectedTabIndex$.next(tabs.length);
        this.runQueriesForTab(tab);
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
     * Two-phase fetch mirroring the data explorer pattern:
     *
     *   1. Query the instances of the type.
     *   2. Anchor follow-up queries on those IIDs via `{ $v iid <iid>; } or …`,
     *      one query per connection kind (has, links, secondary links). Anchoring
     *      first sidesteps an issue where TypeQL chokes on unbound role inference
     *      when the relation variable in a `links` constraint is only typed via
     *      its players' types rather than a concrete iid.
     *
     * The graph builder merges every response into the same `RunOutputState.graph`
     * since nodes/edges dedupe by IID / label / composite key.
     */
    private runQueriesForTab(tab: GraphViewTab) {
        const run = tab.run;
        const databaseName = this.driver.requireDatabase().name;
        const rowLimit = this.appData.preferences.queryRowLimit();

        run.graph.status = "running";
        run.graph.database = databaseName;
        run.graph.query = `match $${this.instanceVar(tab.type)} isa ${tab.type.label};`;

        const instanceQuery = `match $${this.instanceVar(tab.type)} isa ${tab.type.label};`;
        this.driver.query(instanceQuery, { answerCountLimit: rowLimit }).subscribe({
            next: (res) => {
                if (isApiErrorResponse(res)) {
                    run.graph.status = "error";
                    return;
                }
                try { run.graph.push(res); } catch (err) { console.error("[Graph push]", err); }

                if (tab.type.kind === "attributeType") {
                    run.graph.status = "ok";
                    return;
                }

                const iids = this.extractIids(res, this.instanceVar(tab.type));
                if (iids.length === 0) {
                    run.graph.status = "ok";
                    return;
                }
                this.runFollowUps(tab, iids, rowLimit);
            },
            error: (err) => {
                console.error("[Graph instance query]", err);
                run.graph.status = "error";
            },
        });
    }

    private runFollowUps(tab: GraphViewTab, iids: string[], rowLimit: number) {
        const run = tab.run;
        const queries = this.buildFollowUpQueries(tab.type, iids);
        if (queries.length === 0) {
            run.graph.status = "ok";
            return;
        }
        // Run all follow-ups in parallel — the graph builder dedupes nodes/edges
        // so the order responses arrive doesn't matter.
        let pending = queries.length;
        for (const q of queries) {
            this.driver.query(q, { answerCountLimit: rowLimit * 50 }).subscribe({
                next: (res) => {
                    if (!isApiErrorResponse(res)) {
                        try { run.graph.push(res); } catch (err) { console.error("[Graph push]", err); }
                    }
                    if (--pending === 0) run.graph.status = "ok";
                },
                error: (err) => {
                    console.error("[Graph follow-up query]", err);
                    if (--pending === 0) run.graph.status = "ok";
                },
            });
        }
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

    private buildFollowUpQueries(type: SchemaConcept, iids: string[]): string[] {
        if (type.kind === "entityType") {
            const branches = iids.map(iid => `{ $x iid ${iid}; }`).join(" or ");
            return [
                `match ${branches}; $x has $a;`,
                `match ${branches}; $r links ($x); $r links ($other);`,
            ];
        }
        if (type.kind === "relationType") {
            const branches = iids.map(iid => `{ $r iid ${iid}; }`).join(" or ");
            return [
                `match ${branches}; $r has $a;`,
                `match ${branches}; $r links ($p);`,
                `match ${branches}; $r links ($p); $p has $pa;`,
                `match ${branches}; $r links ($p); $r2 links ($p); $r2 links ($other);`,
            ];
        }
        return [];
    }
}
