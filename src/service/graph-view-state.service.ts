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

export type RootKind = "entity" | "relation" | "attribute";

export interface GraphViewTab {
    type: SchemaConcept;
    /** When set, the tab represents *all* instances of a root kind rather than
     *  a single SchemaConcept. `type` is a synthetic placeholder; downstream
     *  code paths that build TypeQL or fetch counts should branch on this. */
    rootKind?: RootKind;
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

    /**
     * Open (or focus) a tab that loads every instance of a root kind
     * (`entity` / `relation` / `attribute`). Uses the projected-out `select $x`
     * pattern so we get instance nodes for arbitrary subtypes in a single
     * round-trip.
     */
    async openKindTab(rootKind: RootKind): Promise<void> {
        const tabs = this.openTabs$.value;
        let tab = tabs.find(t => t.rootKind === rootKind);
        const isNew = !tab;
        if (!tab) {
            const syntheticType = syntheticRootKindConcept(rootKind);
            const baseQuery = kindInstancesQuery(rootKind);
            const run = createRunOutputState(rootKind, baseQuery, this.graphStyleService);
            tab = {
                type: syntheticType,
                rootKind,
                run,
                initialOptions: {},
                initialNodeCount: 0,
                selectionMode: "types",
                loadedConnections: new Map(),
            };
            this.openTabs$.next([...tabs, tab]);
            this.selectedTabIndex$.next(this.openTabs$.value.indexOf(tab));
        } else {
            this.selectedTabIndex$.next(tabs.indexOf(tab));
        }

        await this.runInitialFetches(tab, tab.type, tab.initialOptions, isNew);
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
        const databaseName = this.driver.requireDatabase().name;
        run.graph.database = databaseName;
        run.graph.query = tab.rootKind
            ? kindInstancesQuery(tab.rootKind)
            : `match $${this.instanceVar(type)} isa ${type.label};`;

        // Apply persisted per-type label overrides for this database. Buffered
        // until the visualiser is constructed, so labels come out correct on
        // the first frame.
        run.graph.applyLabelOverrides(this.appData.nodeLabelPrefs.getAll(databaseName));

        try {
            if (tab.rootKind) {
                if (tab.rootKind === "attribute") {
                    await this.fetchInstancesOfKind(run, tab.rootKind);
                } else {
                    // Two-step: fetch instance response without pushing, fetch
                    // display attributes off-graph, then push instances. This
                    // way the first frame the user sees already has the
                    // best-attribute labels resolved.
                    const rowLimit = this.appData.preferences.queryRowLimit();
                    const instancesRes = await this.runQuery(kindInstancesQuery(tab.rootKind), rowLimit);
                    if (!isApiErrorResponse(instancesRes)) {
                        const iids = this.extractIids(instancesRes, "x");
                        if (iids.length > 0) {
                            await this.fetchDisplayAttributes(run, tab.rootKind, iids);
                        }
                        this.pushSafely(run, instancesRes);
                    }
                }
            } else {
                if (type.kind === "attributeType") {
                    await this.fetchInstancesOfType(run, type);
                } else {
                    const ownerVar = this.instanceVar(type);
                    const rowLimit = this.appData.preferences.queryRowLimit();
                    const instancesRes = await this.runQuery(`match $${ownerVar} isa ${type.label};`, rowLimit);
                    if (!isApiErrorResponse(instancesRes)) {
                        const iids = this.extractIids(instancesRes, ownerVar);
                        if (iids.length > 0) {
                            const ownerKind = type.kind === "entityType" ? "entity" : "relation";
                            const tasks: Promise<void>[] = [
                                this.fetchDisplayAttributes(run, ownerKind, iids),
                            ];
                            if (options.includeAttributes) tasks.push(this.fetchAttributesOf(run, type, iids));
                            if (options.includeLinks) tasks.push(this.fetchLinksOf(run, type, iids));
                            await Promise.all(tasks);
                        }
                        this.pushSafely(run, instancesRes);
                    }
                }
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
            visualiser.clearDisplayAttributes();
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
        const rowLimit = this.appData.preferences.queryRowLimit();
        const instanceVar = this.instanceVar(type);
        if (instanceVar === "a") return; // attributes don't have own attributes
        await this.runIidBatched(run, iids, instanceVar, rowLimit * 50,
            branches => `match ${branches}; $${instanceVar} has $a;`);
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
        await this.runIidBatched(run, iids, instanceVar, rowLimit * 50,
            branches => `match ${branches}; $${instanceVar} has ${attrTypeLabel} $a;`);
    }

    /**
     * Given a relation type plus IIDs of in-graph instances of it, load every
     * role-player attached to those relations under the given role name. Used
     * by the per-role chip on the type-detail inspector when the selected
     * type is a relation.
     */
    async fetchRolePlayersOfTypeFor(
        run: RunOutputState,
        relationType: SchemaConcept,
        relationIids: string[],
        roleShortName: string,
    ): Promise<void> {
        if (relationIids.length === 0) return;
        if (relationType.kind !== "relationType") return;
        const rowLimit = this.appData.preferences.queryRowLimit();
        const harvested = await this.runIidBatchedHarvesting(
            run, relationIids, "r", rowLimit * 50,
            branches => `match ${branches}; $r links (${roleShortName}: $p);`,
            ["p"],
        );
        await this.fetchLabelAttributesFor(run, harvested);
    }

    /**
     * Fetch links (relation participation + role players) of the given IIDs
     * (must be entities or relations of `type`) into `run.graph`. IID-anchored
     * patterns sidestep TypeQL role-inference issues where a relation variable
     * is typed only by its players' types.
     */
    async fetchLinksOf(run: RunOutputState, type: SchemaConcept, iids: string[]): Promise<void> {
        if (iids.length === 0) return;
        const rowLimit = this.appData.preferences.queryRowLimit();
        const harvest = new Set<string>();
        if (type.kind === "entityType") {
            const h = await this.runIidBatchedHarvesting(run, iids, "x", rowLimit * 50,
                branches => `match ${branches}; $r links ($x); $r links ($other);`,
                ["r", "other"]);
            h.forEach(iid => harvest.add(iid));
        } else if (type.kind === "relationType") {
            const groups = await Promise.all([
                this.runIidBatchedHarvesting(run, iids, "r", rowLimit * 50,
                    branches => `match ${branches}; $r links ($p);`,
                    ["p"]),
                this.runIidBatchedHarvesting(run, iids, "r", rowLimit * 50,
                    branches => `match ${branches}; $r links ($p); $p has $pa;`,
                    ["p"]),
                this.runIidBatchedHarvesting(run, iids, "r", rowLimit * 50,
                    branches => `match ${branches}; $r links ($p); $r2 links ($p); $r2 links ($other);`,
                    ["p", "r2", "other"]),
            ]);
            groups.forEach(s => s.forEach(iid => harvest.add(iid)));
        }
        await this.fetchLabelAttributesFor(run, harvest);
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
        const harvested = await this.runIidBatchedHarvesting(
            run, [relationIID], "r", rowLimit * 50,
            branches => `match ${branches}; $r links ($role: $p);`,
            ["r", "p"],
        );
        await this.fetchLabelAttributesFor(run, harvested);
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
        const harvest: string[] = [relationIID, playerIID];
        if (originalIID && originalIID !== playerIID && originalIID !== relationIID) {
            query += ` $r links ($origRole: $orig); $orig iid ${originalIID};`;
            harvest.push(originalIID);
        }
        await this.runAndPush(run, query, rowLimit * 50);
        // No need to harvest from the response — every new node here was
        // pinned by IID in the query, so we already know them.
        await this.fetchLabelAttributesFor(run, harvest);
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
        // Two query templates so we get both the relation→source edges AND
        // the relation's other role-players (with their role edges). Each
        // template is batched separately and runs in parallel.
        const groups = await Promise.all([
            this.runIidBatchedHarvesting(run, sourceIids, playerVar, rowLimit * 50,
                branches => `match ${branches}; $r isa ${relationTypeLabel}; $r links ($role: $${playerVar});`,
                ["r"]),
            this.runIidBatchedHarvesting(run, sourceIids, playerVar, rowLimit * 50,
                branches => `match ${branches}; $r isa ${relationTypeLabel}; $r links ($role1: $${playerVar}); $r links ($role2: $other);`,
                ["r", "other"]),
        ]);
        const harvest = new Set<string>();
        groups.forEach(s => s.forEach(iid => harvest.add(iid)));
        await this.fetchLabelAttributesFor(run, harvest);
    }

    /**
     * Build a `{ $v iid X; } or ...` pattern from each batch of IIDs and run
     * the resulting query through `runAndPush` in parallel.
     *
     * Chunking is mandatory because the server silently returns
     * `query: null` (the AnalyzedPipeline) for very long OR-of-iid patterns,
     * which makes the graph-builder pipeline drop the response entirely.
     * Per the TypeDB team, the server-side limit is 64 branches; 50 leaves
     * comfortable headroom while still keeping the number of round-trips
     * low as the graph grows.
     */
    private static readonly IID_BATCH_SIZE = 50;

    /**
     * Same chunked-OR pipeline as `runIidBatched` but also harvests the IIDs
     * bound to a set of variable names in each response and returns their
     * union. Used by link-loading helpers so they can fire a follow-up
     * label-attribute fetch for the newly-introduced instances — without it,
     * new player nodes show up labelled with just their type name.
     */
    private async runIidBatchedHarvesting(
        run: RunOutputState,
        iids: string[],
        instanceVar: string,
        rowLimit: number,
        buildQuery: (branches: string) => string,
        harvestVars: string[],
    ): Promise<Set<string>> {
        const harvested = new Set<string>();
        if (iids.length === 0) return harvested;
        const batches: string[][] = [];
        for (let i = 0; i < iids.length; i += GraphViewState.IID_BATCH_SIZE) {
            batches.push(iids.slice(i, i + GraphViewState.IID_BATCH_SIZE));
        }
        await Promise.all(batches.map(async batch => {
            const branches = batch.map(iid => `{ $${instanceVar} iid ${iid}; }`).join(" or ");
            const query = buildQuery(branches);
            const res = await this.runQuery(query, rowLimit);
            if (isApiErrorResponse(res)) return;
            if (res.ok.answerType === "conceptRows") {
                for (const answer of (res.ok as any).answers) {
                    const data = answer.data as Record<string, any>;
                    for (const v of harvestVars) {
                        const c = data[v];
                        if (c?.iid && (c.kind === "entity" || c.kind === "relation")) {
                            harvested.add(c.iid);
                        }
                    }
                }
            }
            this.pushSafely(run, res);
        }));
        return harvested;
    }

    /**
     * Fetch off-graph display attributes for a set of entity/relation IIDs so
     * the label heuristic can render them as `<type>: <name>` from the moment
     * they enter the graph. Owner-kind agnostic: the underlying query uses
     * `$x` as the owner variable; the iid filter is what disambiguates.
     */
    private async fetchLabelAttributesFor(run: RunOutputState, iids: Set<string> | string[]): Promise<void> {
        const list = Array.isArray(iids) ? iids : [...iids];
        if (list.length === 0) return;
        await this.fetchDisplayAttributes(run, "entity", list);
    }

    private async runIidBatched(
        run: RunOutputState,
        iids: string[],
        instanceVar: string,
        rowLimit: number,
        buildQuery: (branches: string) => string,
    ): Promise<void> {
        if (iids.length === 0) return;
        const batches: string[][] = [];
        for (let i = 0; i < iids.length; i += GraphViewState.IID_BATCH_SIZE) {
            batches.push(iids.slice(i, i + GraphViewState.IID_BATCH_SIZE));
        }
        const queries = batches.map(batch => {
            const branches = batch.map(iid => `{ $${instanceVar} iid ${iid}; }`).join(" or ");
            return buildQuery(branches);
        });
        await Promise.all(queries.map(q => this.runAndPush(run, q, rowLimit)));
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
            // `includeQueryStructure` is required so the response carries the
            // AnalyzedPipeline that the graph builder pipeline consumes.
            // Without it, `handleQueryResult` sees `res.ok.query == null` and
            // silently skips the build, leaving the graph unchanged.
            this.driver.query(query, { answerCountLimit: rowLimit, includeQueryStructure: true }).subscribe({
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

    /**
     * Run the kind-instances query (entity/relation/attribute) into `run.graph`.
     * Returns the IIDs of the returned instances so the caller can do
     * follow-up fetches (e.g. label-only attribute fetch).
     */
    async fetchInstancesOfKind(run: RunOutputState, rootKind: RootKind): Promise<string[]> {
        const rowLimit = this.appData.preferences.queryRowLimit();
        const res = await this.runQuery(kindInstancesQuery(rootKind), rowLimit);
        if (isApiErrorResponse(res)) return [];
        this.pushSafely(run, res);
        return this.extractIids(res, "x");
    }

    /**
     * Fetch every attribute owned by the given IIDs *off-graph* — the
     * attribute values feed the label heuristic only, no attribute nodes or
     * edges are added to the visualiser. Skipped for attribute kinds (they
     * don't have attributes). `ownerKind` selects the right query template
     * (entities/relations) and matching variable name.
     */
    async fetchDisplayAttributes(run: RunOutputState, ownerKind: "entity" | "relation", iids: string[]): Promise<void> {
        if (iids.length === 0) return;
        const ownerVar = ownerKind === "entity" ? "x" : "r";
        const rowLimit = this.appData.preferences.queryRowLimit();
        const batches: string[][] = [];
        for (let i = 0; i < iids.length; i += GraphViewState.IID_BATCH_SIZE) {
            batches.push(iids.slice(i, i + GraphViewState.IID_BATCH_SIZE));
        }
        await Promise.all(batches.map(async batch => {
            const branches = batch.map(iid => `{ $${ownerVar} iid ${iid}; }`).join(" or ");
            const query = `match ${branches}; $${ownerVar} has $a;`;
            try {
                const res = await this.runQuery(query, rowLimit * 50);
                // `run.graph` buffers when the visualiser doesn't exist yet
                // (display attrs are typically fetched before the first push),
                // so the records survive until the visualiser is constructed.
                run.graph.recordDisplayAttributes(res, ownerVar);
            } catch (err) {
                console.error("[Display attributes fetch]", err);
            }
        }));
    }

}

/** TypeQL that returns every instance of a root kind. Used by kind-level tabs
 *  in both the Graph and Query pages. The intermediate type variable `$y` is
 *  projected out via `select $x` so the graph builder only renders instance
 *  vertices (no per-subtype-label nodes). */
export function kindInstancesQuery(rootKind: RootKind): string {
    return `match\n    $x isa $y;\n    ${rootKind} $y;\nselect $x;`;
}

/** Placeholder SchemaConcept used as `GraphViewTab.type` for kind tabs. Most
 *  downstream code consults `tab.rootKind` first when it matters; for the
 *  fields that don't (label, kind, icon class), this carries enough info. */
function syntheticRootKindConcept(rootKind: RootKind): SchemaConcept {
    const kind = rootKind === "entity" ? "entityType"
        : rootKind === "relation" ? "relationType" : "attributeType";
    return {
        kind,
        label: rootKind,
        subtypes: [],
        ownedAttributes: [],
        playedRoles: [],
        ...(rootKind === "relation" ? { relatedRoles: [] } : {}),
        ...(rootKind === "attribute" ? { valueType: "string" } : {}),
    } as unknown as SchemaConcept;
}
