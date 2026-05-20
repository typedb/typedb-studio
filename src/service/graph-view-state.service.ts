/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable, inject } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { DriverState } from "./driver-state.service";
import { SchemaConcept } from "./schema-state.service";
import { GraphStyleService } from "./graph-style.service";
import { SnackbarService } from "./snackbar.service";
import { AppData } from "./app-data.service";
import { createRunOutputState, executeQueryToRun, RunOutputState } from "./query-page-state.service";

export interface GraphViewTab {
    type: SchemaConcept;
    run: RunOutputState;
}

@Injectable({ providedIn: "root" })
export class GraphViewState {

    private driver = inject(DriverState);
    private snackbar = inject(SnackbarService);
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
        const query = this.buildQuery(type);
        const run = createRunOutputState(type.label, query, this.graphStyleService);
        const tab: GraphViewTab = { type, run };
        this.openTabs$.next([...tabs, tab]);
        this.selectedTabIndex$.next(tabs.length);

        executeQueryToRun(run, query, {
            driver: this.driver,
            snackbar: this.snackbar,
            rowLimit: this.appData.preferences.queryRowLimit(),
            answersOutputEnabled: true,
        }).subscribe();
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
     * Build a TypeQL query that fetches a type's instances plus their direct
     * connections, as a single union match. Branch 1 is a trivial "just $x isa T"
     * so an instance with no neighbours still appears in the graph.
     *
     * - Entity: instance + its attributes + relations it participates in + the
     *   other roleplayers of those relations.
     * - Relation: instance + its attributes + its roleplayers + attributes of
     *   those roleplayers + relations those roleplayers participate in (and
     *   those relations' other roleplayers).
     * - Attribute: just the attribute instances themselves, no edges.
     */
    private buildQuery(type: SchemaConcept): string {
        const label = type.label;
        switch (type.kind) {
            case "entityType":
                return [
                    `match`,
                    `  { $x isa ${label}; }`,
                    `  or { $x isa ${label}; $x has $a; }`,
                    `  or { $x isa ${label}; $r links ($x: $rx); }`,
                    `  or { $x isa ${label}; $r links ($x: $rx, $other: $ro); };`,
                ].join("\n");
            case "relationType":
                return [
                    `match`,
                    `  { $r isa ${label}; }`,
                    `  or { $r isa ${label}; $r has $a; }`,
                    `  or { $r isa ${label}; $r links ($p: $rp); }`,
                    `  or { $r isa ${label}; $r links ($p: $rp); $p has $pa; }`,
                    `  or { $r isa ${label}; $r links ($p: $rp); $r2 links ($p: $rp2); }`,
                    `  or { $r isa ${label}; $r links ($p: $rp); $r2 links ($p: $rp2, $other: $oro); };`,
                ].join("\n");
            case "attributeType":
                return `match $a isa ${label};`;
        }
    }
}
