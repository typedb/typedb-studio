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

export type SelectionMode = "types" | "instances";

export interface GraphViewTab {
    type: SchemaConcept;
    run: RunOutputState;
    /** Options the tab was originally opened with — used by `resetTab` to
     *  replay the exact initial query when the user clicks "Reset changes". */
    initialOptions: OpenTypeTabOptions;
    /** Snapshot of `graph.order` taken right after the initial query
     *  completes; the UI compares the current order against this to decide
     *  whether anything's been added since (so it can enable the
     *  "Reset changes" button only when there's actually something to undo). */
    initialNodeCount: number;
    /** Whether clicking a node selects the node's type (Inspector shows
     *  type-detail) or the instance itself (Inspector shows instance-detail).
     *  Defaults to "types" — type-level exploration is the primary flow. */
    selectionMode: SelectionMode;
    /**
     * For each source type (key = type label), the set of target type labels
     * (attribute or relation type labels) that have been loaded into the
     * graph via a type-detail chip toggle. Tracked separately from the graph
     * itself so the chip state isn't conflated with whatever happened at the
     * single-instance level — toggling ON for the type means "load it across
     * every instance of this source type"; the chip's loaded indicator stays
     * sticky regardless of what other adds may have introduced.
     */
    loadedConnections: Map<string, Set<string>>;
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
        const isNew = !tab;
        if (!tab) {
            const baseQuery = `match $${this.instanceVar(type)} isa ${type.label};`;
            const run = createRunOutputState(type.label, baseQuery, this.graphStyleService);
            tab = {
                type, run,
                initialOptions: options,
                initialNodeCount: 0,
                selectionMode: "types",
                loadedConnections: new Map(),
            };
            this.openTabs$.next([...tabs, tab]);
            this.selectedTabIndex$.next(this.openTabs$.value.indexOf(tab));
        } else {
            this.selectedTabIndex$.next(tabs.indexOf(tab));
        }

        await this.runInitialFetches(tab, type, options, isNew);
    }

    /**
     * Run the initial query (instances of the type, plus any attribute/link
     * follow-ups requested by `options`) into the tab's graph. When `isNew`
     * is true the post-load node count is snapshotted onto the tab so the
     * "Reset changes" affordance can later tell whether the user has added
     * anything beyond the initial state.
     */
    private async runInitialFetches(tab: GraphViewTab, type: SchemaConcept, options: OpenTypeTabOptions, isNew: boolean): Promise<void> {
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
            if (isNew) {
                tab.initialNodeCount = run.graph.visualiser?.graph.order ?? 0;
            }
            run.graph.status = "ok";
        } catch (err) {
            console.error("[Graph fetch]", err);
            run.graph.status = "error";
        }
    }

    /**
     * "Reset changes": clear everything that's been added to the tab's graph
     * beyond what the initial query produced, then re-run that initial query
     * (with the same `OpenTypeTabOptions` the tab was opened with) on a blank
     * slate. Selection, secondary anchors, and any pinned viewport are also
     * cleared so the result behaves like a freshly-opened tab.
     */
    async resetTab(tab: GraphViewTab): Promise<void> {
        const visualiser = tab.run.graph.visualiser;
        if (visualiser) {
            visualiser.interactionHandler.clearSelection();
            visualiser.interactionHandler.setSecondaryAnchors(new Set());
            visualiser.unfreezeViewport();
            visualiser.graph.clear();
            visualiser.layout.forgetSettled();
        }
        tab.initialNodeCount = 0;
        tab.loadedConnections.clear();
        // Pass isNew=true so the post-reset node count is re-snapshotted; the
        // tab itself stays in openTabs$ throughout.
        await this.runInitialFetches(tab, tab.type, tab.initialOptions, true);
    }

    /** Whether the tab's graph has anything in it beyond the initial query's results. */
    tabHasChanges(tab: GraphViewTab): boolean {
        const order = tab.run.graph.visualiser?.graph.order ?? 0;
        return order > tab.initialNodeCount;
    }

    /** Find the tab whose `run` matches the given one, or null. */
    findTabForRun(run: RunOutputState): GraphViewTab | null {
        return this.openTabs$.value.find(t => t.run === run) ?? null;
    }

    /**
     * Whether the user has loaded `targetTypeLabel` for `sourceTypeLabel`
     * via a type-detail chip toggle on this tab.
     */
    isConnectionLoaded(run: RunOutputState, sourceTypeLabel: string, targetTypeLabel: string): boolean {
        const tab = this.findTabForRun(run);
        return tab?.loadedConnections.get(sourceTypeLabel)?.has(targetTypeLabel) ?? false;
    }

    /** Mark `targetTypeLabel` as loaded for `sourceTypeLabel` on this tab. */
    markConnectionLoaded(run: RunOutputState, sourceTypeLabel: string, targetTypeLabel: string): void {
        const tab = this.findTabForRun(run);
        if (!tab) return;
        let targets = tab.loadedConnections.get(sourceTypeLabel);
        if (!targets) {
            targets = new Set();
            tab.loadedConnections.set(sourceTypeLabel, targets);
        }
        targets.add(targetTypeLabel);
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

    /**
     * Load every relation of type `relationTypeLabel` that has *any* of
     * `sourceIids` as one of its role-players, including its other role
     * players + role edges. Used by the per-relation-type chip on the
     * type-detail Inspector. The source type is needed to choose the
     * right anchor variable in the IID-branches.
     */
    async fetchRelationsOfTypeForPlayers(
        run: RunOutputState,
        sourceType: SchemaConcept,
        sourceIids: string[],
        relationTypeLabel: string,
    ): Promise<void> {
        if (sourceIids.length === 0) return;
        const rowLimit = this.appData.preferences.queryRowLimit();
        const playerVar = this.instanceVar(sourceType);
        if (playerVar === "a") return; // attributes don't play roles
        const branches = sourceIids.map(iid => `{ $${playerVar} iid ${iid}; }`).join(" or ");
        // Two queries so we get both the relation→source edges AND the
        // relation's other role-players (with their role edges). Both
        // contribute to the same graph push.
        const queries = [
            `match ${branches}; $r isa ${relationTypeLabel}; $r links ($role: $${playerVar});`,
            `match ${branches}; $r isa ${relationTypeLabel}; $r links ($role1: $${playerVar}); $r links ($role2: $other);`,
        ];
        await Promise.all(queries.map(q => this.runAndPush(run, q, rowLimit * 50)));
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
