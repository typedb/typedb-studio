/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { inject, Injectable } from "@angular/core";
import { FormControl } from "@angular/forms";
import { BehaviorSubject, combineLatest, map, NEVER, Observable, pairwise, shareReplay, startWith, Subject, switchMap, takeUntil } from "rxjs";
import { DriverAction, queryRunActionOf } from "../concept/action";
import { GraphVisualiser } from "../framework/graph-visualiser/engine";
import { createSigmaRenderer, defaultSigmaSettings } from "../framework/graph-visualiser/engine/sigma-settings";
import { newGraph, Graph } from "../framework/graph-visualiser/engine/graph";
import { Layouts } from "../framework/graph-visualiser/engine/layout";
import { detectOS } from "../framework/util/os";
import { INTERNAL_ERROR } from "../framework/util/strings";
import { DriverState } from "./driver-state.service";
import { QueryTabsState } from "./query-tabs-state.service";
import { SchemaState } from "./schema-state.service";
import { SnackbarService } from "./snackbar.service";
import {
    ApiResponse, Attribute, Concept, ConceptDocument, ConceptRow, isApiErrorResponse, QueryResponse, Value
} from "@typedb/driver-http";
import { AppData, RowLimit } from "./app-data.service";
import { GraphStyleService } from "./graph-style.service";
import { splitTypeQLQueries } from "../framework/util/typeql-split";

export type OutputType = "raw" | "log" | "table" | "graph";
export { RowLimit } from "./app-data.service";

export interface RunResult {
    success: boolean;
    error?: unknown;
}

export interface SubQueryResult {
    index: number;
    label: string;
    queryText: string;
    table: TableOutputState;
}

export interface RunOutputState {
    id: string;
    label: string;
    query: string;
    pinned: boolean;
    multiQuery: boolean;
    batchSummary: boolean;
    batchTotal: number;
    batchCompleted: number;
    batchFailed: boolean;
    subResults: SubQueryResult[];
    log: LogOutputState;
    table: TableOutputState;
    graph: GraphOutputState;
    raw: RawOutputState;
    lastResponse: ApiResponse<QueryResponse> | null;
}

export function createRunOutputState(label: string, query: string, styleService: GraphStyleService): RunOutputState {
    return {
        id: crypto.randomUUID(),
        label,
        query,
        pinned: false,
        multiQuery: false,
        batchSummary: false,
        batchTotal: 0,
        batchCompleted: 0,
        batchFailed: false,
        subResults: [],
        log: new LogOutputState(),
        table: new TableOutputState(),
        graph: new GraphOutputState(styleService),
        raw: new RawOutputState(),
        lastResponse: null,
    };
}

export interface TabOutputState {
    runs: RunOutputState[];
    selectedRunIndex: number;
    runCounter: number;
    outputTypeControl: FormControl<OutputType>;
}

function createTabOutputState(initialOutputType?: string): TabOutputState {
    const outputType = (initialOutputType as OutputType) || "log";
    return {
        runs: [],
        selectedRunIndex: -1,
        runCounter: 0,
        outputTypeControl: new FormControl(outputType, { nonNullable: true }),
    };
}

function currentRun(tabState: TabOutputState): RunOutputState | null {
    if (tabState.selectedRunIndex < 0 || tabState.selectedRunIndex >= tabState.runs.length) {
        return null;
    }
    return tabState.runs[tabState.selectedRunIndex];
}

// Row limits are capped (no "No limit" option) to prevent resource starvation on large result sets.
export const ROW_LIMIT_OPTIONS: { value: RowLimit; label: string }[] = [
    { value: 10, label: "10" },
    { value: 50, label: "50" },
    { value: 100, label: "100" },
    { value: 500, label: "500" },
    { value: 1000, label: "1,000" },
    { value: 5000, label: "5,000" },
    { value: 10000, label: "10,000" },
    { value: 25000, label: "25,000" },
    { value: 100000, label: "100,000" },
];

const NO_SERVER_CONNECTED = `No server connected`;
const NO_DATABASE_SELECTED = `No database selected`;
const NO_OPEN_TRANSACTION = `No open transaction`;
const QUERY_BLANK = `Query text is blank`;
const QUERY_RUNNING = `A query is already running`;
const QUERY_HIGHLIGHT_DIV_ID = null;

const RUN_KEY_BINDING = detectOS() === "mac" ? "⌘+Enter" : "Ctrl+Enter";

@Injectable({
    providedIn: "root",
})
export class QueryPageState {

    private driver = inject(DriverState);
    private appData = inject(AppData);
    schema = inject(SchemaState);
    private snackbar = inject(SnackbarService);
    queryTabs = inject(QueryTabsState);
    private graphStyleService = inject(GraphStyleService);

    outputTypes: OutputType[] = ["log", "table", "graph", "raw"];
    rowLimitControl = new FormControl(this.appData.preferences.queryRowLimit(), { nonNullable: true });
    rowLimitOptions = ROW_LIMIT_OPTIONS;
    readonly history = new HistoryWindowState(this.driver);
    answersOutputEnabled = true;

    private tabOutputStates = new Map<string, TabOutputState>();
    private _fallbackOutputState = createTabOutputState();
    private _fallbackRunState = createRunOutputState("", "", this.graphStyleService);
    private _graphCanvasEl: HTMLElement | null = null;
    private _queryRunning$ = new BehaviorSubject<boolean>(false);
    private _queryStop$ = new Subject<void>();
    readonly queryRunning$ = this._queryRunning$.asObservable();

    getOrCreateTabOutputState(tabId: string): TabOutputState {
        let state = this.tabOutputStates.get(tabId);
        if (!state) {
            const tab = this.queryTabs.openTabs$.value.find(t => t.id === tabId);
            state = createTabOutputState(tab?.outputType);
            this.tabOutputStates.set(tabId, state);
            state.outputTypeControl.valueChanges.subscribe(value => {
                this.queryTabs.updateTabOutputType(tabId, value);
            });
        }
        return state;
    }

    private get currentTabOutputState(): TabOutputState {
        const tab = this.queryTabs.currentTab;
        if (!tab) return this._fallbackOutputState;
        return this.getOrCreateTabOutputState(tab.id);
    }

    private get currentRunState(): RunOutputState {
        return currentRun(this.currentTabOutputState) ?? this._fallbackRunState;
    }

    get outputTypeControl(): FormControl<OutputType> {
        return this.currentTabOutputState.outputTypeControl;
    }

    get currentTabRuns(): RunOutputState[] {
        return this.currentTabOutputState.runs;
    }

    get selectedRunIndex(): number {
        return this.currentTabOutputState.selectedRunIndex;
    }

    get logOutput(): LogOutputState {
        return this.currentRunState.log;
    }

    get tableOutput(): TableOutputState {
        return this.currentRunState.table;
    }

    get graphOutput(): GraphOutputState {
        return this.currentRunState.graph;
    }

    get rawOutput(): RawOutputState {
        return this.currentRunState.raw;
    }


    private readonly currentTabQuery$ = combineLatest([
        this.queryTabs.openTabs$,
        this.queryTabs.selectedTabIndex$
    ]).pipe(
        switchMap(() => {
            const currentTab = this.queryTabs.currentTab;
            if (!currentTab) return new BehaviorSubject("");
            const control = this.queryTabs.getTabControl(currentTab);
            return control.valueChanges.pipe(startWith(control.value));
        })
    );

    readonly runDisabledReason$ = combineLatest([
        this.driver.status$, this.driver.database$, this.driver.transactionOperationModeChanges$,
        this.driver.transaction$, this.currentTabQuery$, this.queryTabs.selectedTabIndex$,
        this._queryRunning$,
    ]).pipe(map(([status, db, txMode, tx, query, , running]) => {
        // Note: element at index 5 (selectedTabIndex) is unused but triggers recalculation
        if (running) return QUERY_RUNNING;
        else if (status !== "connected") return NO_SERVER_CONNECTED;
        else if (db == null) return NO_DATABASE_SELECTED;
        else if (txMode === "manual" && !tx) return NO_OPEN_TRANSACTION;
        else if (!query.length) return QUERY_BLANK;
        else return null;
    }), shareReplay(1));
    readonly runTooltip$ = this.runDisabledReason$.pipe(map(x => x ? x : `Run query (${RUN_KEY_BINDING})`));
    readonly runEnabled$ = this.runDisabledReason$.pipe(map(x => x == null));
    readonly outputDisabledReason$ = this.driver.status$.pipe(map(x => x === "connected" ? null : NO_SERVER_CONNECTED));
    readonly outputDisabled$ = this.outputDisabledReason$.pipe(map(x => x != null));

    constructor() {
        (window as any)["queryToolState"] = this;
        combineLatest([this.outputDisabled$, this.queryTabs.selectedTabIndex$]).subscribe(([disabled]) => {
            const control = this.outputTypeControl;
            if (disabled) control.disable();
            else control.enable();
        });
        this.rowLimitControl.valueChanges.subscribe((value) => {
            this.appData.preferences.setQueryRowLimit(value);
        });
        this.queryTabs.openTabs$.pipe(pairwise()).subscribe(([oldTabs, newTabs]) => {
            const newTabIds = new Set(newTabs.map(t => t.id));
            for (const oldTab of oldTabs) {
                if (!newTabIds.has(oldTab.id)) {
                    this.cleanupTabOutputState(oldTab.id);
                }
            }
        });
    }

    cleanupTabOutputState(tabId: string): void {
        const state = this.tabOutputStates.get(tabId);
        if (state) {
            for (const run of state.runs) {
                run.graph.destroy();
            }
            this.tabOutputStates.delete(tabId);
        }
    }

    setGraphCanvasEl(el: HTMLElement | null): void {
        this._graphCanvasEl = el;
        if (!el) return;
        const run = currentRun(this.currentTabOutputState);
        if (run) {
            requestAnimationFrame(() => {
                run.graph.detach();
                run.graph.attach(el);
                run.graph.resize();
            });
        }
    }

    handleTabSwitch(previousTabId: string | null): void {
        const currentTab = this.queryTabs.currentTab;
        if (previousTabId && previousTabId !== currentTab?.id) {
            const prevTabState = this.tabOutputStates.get(previousTabId);
            if (prevTabState) {
                const prevRun = currentRun(prevTabState);
                if (prevRun) prevRun.graph.detach();
            }
        }
        if (currentTab && this._graphCanvasEl) {
            const tabState = this.currentTabOutputState;
            const curRun = currentRun(tabState);
            if (curRun) {
                curRun.graph.attach(this._graphCanvasEl);
                if (tabState.outputTypeControl.value === "graph") {
                    requestAnimationFrame(() => curRun.graph.resize());
                }
            }
        }
    }

    detachAllGraphOutputs(): void {
        for (const tabState of this.tabOutputStates.values()) {
            for (const run of tabState.runs) {
                run.graph.detach();
            }
        }
    }

    destroyAllGraphOutputs(): void {
        for (const tabState of this.tabOutputStates.values()) {
            for (const run of tabState.runs) {
                run.graph.destroy();
            }
        }
    }

    selectRun(index: number) {
        const tabState = this.currentTabOutputState;
        if (index < 0 || index >= tabState.runs.length) return;
        if (index === tabState.selectedRunIndex) return;

        const oldRun = currentRun(tabState);
        if (oldRun) oldRun.graph.detach();

        tabState.selectedRunIndex = index;

        const newRun = currentRun(tabState);
        if (newRun && this._graphCanvasEl) {
            newRun.graph.attach(this._graphCanvasEl);
        }
    }

    closeRun(index: number) {
        const tabState = this.currentTabOutputState;
        if (index < 0 || index >= tabState.runs.length) return;

        const runToClose = tabState.runs[index];

        if (index === tabState.selectedRunIndex) {
            runToClose.graph.detach();
        }

        runToClose.graph.destroy();
        tabState.runs.splice(index, 1);

        if (tabState.runs.length === 0) {
            tabState.selectedRunIndex = -1;
        } else if (index < tabState.selectedRunIndex) {
            tabState.selectedRunIndex--;
        } else if (index === tabState.selectedRunIndex) {
            tabState.selectedRunIndex = Math.min(index, tabState.runs.length - 1);
            const newRun = currentRun(tabState);
            if (newRun && this._graphCanvasEl) {
                newRun.graph.attach(this._graphCanvasEl);
            }
        }
    }

    closeOtherRuns(run: RunOutputState) {
        const tabState = this.currentTabOutputState;
        for (let i = tabState.runs.length - 1; i >= 0; i--) {
            if (tabState.runs[i] !== run && !tabState.runs[i].pinned) {
                this.closeRun(i);
            }
        }
    }

    closeRunsToRight(run: RunOutputState) {
        const tabState = this.currentTabOutputState;
        const index = tabState.runs.indexOf(run);
        if (index < 0) return;
        for (let i = tabState.runs.length - 1; i > index; i--) {
            if (!tabState.runs[i].pinned) {
                this.closeRun(i);
            }
        }
    }

    closeAllRuns() {
        const tabState = this.currentTabOutputState;
        for (let i = tabState.runs.length - 1; i >= 0; i--) {
            if (!tabState.runs[i].pinned) {
                this.closeRun(i);
            }
        }
    }

    togglePinRun(run: RunOutputState) {
        run.pinned = !run.pinned;
        if (run.pinned) {
            // Move pinned run to be adjacent to other pinned runs
            const tabState = this.currentTabOutputState;
            const selectedRun = currentRun(tabState);
            const currentIndex = tabState.runs.indexOf(run);
            const lastPinnedIndex = tabState.runs.reduce((acc, r, i) => r.pinned && r !== run ? i : acc, -1);
            const targetIndex = lastPinnedIndex + 1;
            if (currentIndex !== targetIndex) {
                tabState.runs.splice(currentIndex, 1);
                tabState.runs.splice(targetIndex, 0, run);
                if (selectedRun) {
                    tabState.selectedRunIndex = tabState.runs.indexOf(selectedRun);
                }
            }
        }
    }

    renameRun(run: RunOutputState, newName: string) {
        run.label = newName;
    }

    get currentQueryControl(): FormControl<string> | null {
        const currentTab = this.queryTabs.currentTab;
        if (!currentTab) return null;
        return this.queryTabs.getTabControl(currentTab);
    }

    runCurrentTabQuery() {
        const currentTab = this.queryTabs.currentTab;
        if (!currentTab) return;
        this.runQuery(currentTab.query);
    }

    runQuery(query: string): Observable<RunResult> {
        this._queryRunning$.next(true);
        const tabState = this.currentTabOutputState;

        // Detach current run's graph before creating new run
        const oldRun = currentRun(tabState);
        if (oldRun) oldRun.graph.detach();

        // If the current run is not pinned, replace it; otherwise append
        const replaceIndex = oldRun && !oldRun.pinned ? tabState.selectedRunIndex : -1;
        if (replaceIndex >= 0) {
            oldRun!.graph.destroy();
            tabState.runs.splice(replaceIndex, 1);
        }

        // Create new run
        tabState.runCounter++;
        const newRun = createRunOutputState(`Run ${tabState.runCounter}`, query, this.graphStyleService);
        if (replaceIndex >= 0) {
            tabState.runs.splice(replaceIndex, 0, newRun);
            tabState.selectedRunIndex = replaceIndex;
        } else {
            tabState.runs.push(newRun);
            tabState.selectedRunIndex = tabState.runs.length - 1;
        }
        if (this._graphCanvasEl) {
            newRun.graph.canvasEl = this._graphCanvasEl;
        }

        const result$ = executeQueryToRun(newRun, query, {
            driver: this.driver,
            snackbar: this.snackbar,
            rowLimit: this.rowLimitControl.value,
            answersOutputEnabled: this.answersOutputEnabled,
            stopSignal$: this._queryStop$,
        });
        result$.subscribe({
            next: () => {},
            complete: () => this._queryRunning$.next(false),
            error: () => this._queryRunning$.next(false),
        });
        return result$;
    }

    stopQuery() {
        this._queryStop$.next();
    }
}

export interface RunExecutionDeps {
    driver: DriverState;
    snackbar: SnackbarService;
    rowLimit: RowLimit;
    /** Defaults to true; when false, query results are reported as success/error without answer details. */
    answersOutputEnabled?: boolean;
    stopSignal$?: Observable<void>;
}

/**
 * Execute a TypeQL document against a pre-created RunOutputState.
 * Splits the document via `splitTypeQLQueries` and routes to a single-query
 * (`driver.query`) or multi-query (`driver.multiQuery`) execution accordingly.
 *
 * The returned Observable mirrors completion via a Subject — work begins
 * synchronously regardless of whether the caller subscribes.
 */
export function executeQueryToRun(
    run: RunOutputState,
    queryText: string,
    deps: RunExecutionDeps,
): Observable<RunResult> {
    const completion$ = new Subject<RunResult>();
    const queries = splitTypeQLQueries(queryText);
    if (queries.length <= 1) {
        runSingleQueryToRun(run, queries[0] || queryText, deps, completion$);
    } else {
        runMultiQueryToRun(run, queries, deps, completion$);
    }
    return completion$.asObservable();
}

function runSingleQueryToRun(
    run: RunOutputState,
    query: string,
    deps: RunExecutionDeps,
    completion$: Subject<RunResult>,
) {
    run.log.appendLines(RUNNING, query, ``, `${TIMESTAMP}${new Date().toISOString()}`);
    run.table.status = "running";
    run.graph.status = "running";
    run.graph.query = query;
    run.graph.database = deps.driver.requireDatabase().name;

    const queryOptions = { answerCountLimit: deps.rowLimit };
    let completed = false;
    deps.driver.query(query, queryOptions).pipe(
        takeUntil(deps.stopSignal$ ?? NEVER),
    ).subscribe({
        next: (res) => {
            completed = true;
            outputQueryResponseToRun(run, res, deps);
            run.log.flush();
            const hasError = isApiErrorResponse(res);
            completion$.next({ success: !hasError, error: hasError ? (res as any).err : undefined });
            completion$.complete();
        },
        error: (err) => {
            completed = true;
            run.table.status = "error";
            run.graph.status = "error";
            handleQueryError(run, err, deps);
            run.log.flush();
            completion$.next({ success: false, error: err });
            completion$.complete();
        },
        complete: () => {
            if (!completed) {
                // Stopped by user
                run.table.status = "error";
                run.graph.status = "error";
                run.log.appendBlankLine();
                run.log.appendLines(`Query interrupted.`);
                run.log.flush();
                completion$.next({ success: false });
                completion$.complete();
            }
        },
    });
}

function runMultiQueryToRun(
    run: RunOutputState,
    queries: string[],
    deps: RunExecutionDeps,
    completion$: Subject<RunResult>,
) {
    run.multiQuery = true;
    const isBatchSummary = queries.length > 10;
    run.batchSummary = isBatchSummary;
    run.batchTotal = queries.length;
    run.log.appendLines(RUNNING, `${queries.length} queries`, ``, `${TIMESTAMP}${new Date().toISOString()}`);
    run.table.status = "running";
    run.graph.status = "multiQuery";

    // Create sub-results (skip for batch summary mode)
    if (!isBatchSummary) {
        for (let i = 0; i < queries.length; i++) {
            const sub: SubQueryResult = {
                index: i,
                label: `Query ${i + 1}`,
                queryText: queries[i],
                table: new TableOutputState(),
            };
            sub.table.status = "running";
            run.subResults.push(sub);
        }
    }

    const queryOptions = { answerCountLimit: deps.rowLimit };
    const rawResults: string[] = isBatchSummary ? [] : new Array(queries.length);
    let lastCompletedIndex = -1;
    // The driver only commits if the resolved transaction type is non-read. A write
    // query can't succeed in a read transaction, so any non-read queryType in the
    // results means the batch committed.
    let anyWriteOrSchema = false;

    const queryAction = queryRunActionOf(queries.join("\nend;\n"));
    queryAction.batch = true;
    deps.driver.emitAction(queryAction);

    deps.driver.multiQuery(queries, queryOptions).pipe(
        takeUntil(deps.stopSignal$ ?? NEVER),
    ).subscribe({
        next: ({ index, res, autoCommitted }) => {
            lastCompletedIndex = index;
            run.batchCompleted = index + 1;
            if (!isApiErrorResponse(res) && res.ok.queryType !== "read") {
                anyWriteOrSchema = true;
            }

            // Log output
            if (isBatchSummary) {
                run.log.setProgress(`Running query ${index + 1} of ${queries.length}...`);
            } else {
                run.log.appendBlankLine();
                run.log.appendLines(MULTI_QUERY_BORDER, `${MULTI_QUERY_HEADER}Query ${index + 1}`, MULTI_QUERY_BORDER, queries[index]);
                run.log.appendBlankLine();
                run.log.appendQueryResult(res, autoCommitted, deps.rowLimit);
            }

            if (!isBatchSummary) {
                const sub = run.subResults[index];
                sub.table.push(res);
                rawResults[index] = JSON.stringify(res, null, 2);
            }
        },
        error: (err) => {
            // Freeze pending progress so the failed-query header lands below it.
            run.log.freezeProgress();
            // Log the failed query's header
            const failedIndex = lastCompletedIndex + 1;
            if (failedIndex < queries.length) {
                run.log.appendBlankLine();
                run.log.appendLines(MULTI_QUERY_BORDER, `${MULTI_QUERY_HEADER}Query ${failedIndex + 1}`, MULTI_QUERY_BORDER, queries[failedIndex]);
            }

            run.batchFailed = true;
            if (!isBatchSummary) {
                for (const sub of run.subResults) {
                    if (sub.table.status === "running") sub.table.status = "error";
                }
            }
            run.table.status = "error";
            handleQueryError(run, err, deps);
            run.log.flush();
            queryAction.status = "error";
            queryAction.completedAtTimestamp = Date.now();
            queryAction.result = err;
            completion$.next({ success: false, error: err });
            completion$.complete();
        },
        complete: () => {
            const stopped = lastCompletedIndex < queries.length - 1;
            run.table.status = stopped ? "error" : "ok";
            completion$.next({ success: !stopped });
            completion$.complete();
            // Freeze any pending progress line in place before appending terminal content,
            // so e.g. "Committed." lands below the final progress line, not above it.
            run.log.freezeProgress();
            if (stopped) {
                run.batchFailed = true;
                run.log.appendBlankLine();
                run.log.appendLines(`Query batch interrupted.`);
                queryAction.status = "error";
                queryAction.completedAtTimestamp = Date.now();
                queryAction.result = { message: "Query batch interrupted" } as any;
            } else {
                queryAction.status = "success";
                queryAction.completedAtTimestamp = Date.now();
                const committed = deps.driver.autoTransactionEnabled$.value && anyWriteOrSchema;
                queryAction.autoCommitted = committed;
                if (committed) {
                    run.log.appendBlankLine();
                    run.log.appendLines(`Committed.`);
                }
            }
            if (!isBatchSummary) {
                for (const sub of run.subResults) {
                    if (sub.table.status === "running") sub.table.status = stopped ? "error" : "ok";
                }
            }
            run.log.flush();
            run.raw.push(`[\n${rawResults.filter(Boolean).join(',\n')}\n]`);
        }
    });
}

function handleQueryError(run: RunOutputState, err: any, deps: RunExecutionDeps) {
    run.raw.push(stringifyError(err));
    deps.driver.checkHealth().subscribe({
        next: () => {
            let msg = ``;
            if (isApiErrorResponse(err)) {
                msg = err.err.message;
            } else {
                msg = err?.message ?? err?.toString() ?? `Unknown error`;
            }
            run.log.appendBlankLine();
            run.log.appendLines(`${RESULT}${ERROR}`, ``, msg);
        },
        error: () => {
            const msg = `Unable to connect to TypeDB server.`;
            run.log.appendBlankLine();
            run.log.appendLines(`${RESULT}${ERROR}`, ``, msg);
        }
    });
}

function outputQueryResponseToRun(run: RunOutputState, res: ApiResponse<QueryResponse>, deps: RunExecutionDeps) {
    const autoCommitted = deps.driver.autoTransactionEnabled$.value && !isApiErrorResponse(res) && res.ok.queryType !== "read";
    run.lastResponse = res;
    if (deps.answersOutputEnabled !== false) outputQueryResponseWithAnswers(run, res, autoCommitted, deps);
    else outputQueryResponseNoAnswers(run, autoCommitted);
}

function outputQueryResponseWithAnswers(run: RunOutputState, res: ApiResponse<QueryResponse>, autoCommitted: boolean, deps: RunExecutionDeps) {
    run.log.appendBlankLine();
    run.log.appendQueryResult(res, autoCommitted, deps.rowLimit);
    run.table.push(res);
    try {
        run.graph.push(res);
    } catch (err) {
        console.error("[Graph Output Error]", err);
        run.graph.status = "error";
        deps.snackbar.errorPersistent(`Failed to render graph visualization: ${err}`);
    }
    run.raw.push(JSON.stringify(res, null, 2));
}

function outputQueryResponseNoAnswers(run: RunOutputState, autoCommitted: boolean) {
    run.log.appendBlankLine();
    run.log.appendLines(`${RESULT}${SUCCESS}`);
    if (autoCommitted) run.log.appendLines(`Committed.`);
    run.table.status = "answerOutputDisabled";
    run.graph.status = "answerOutputDisabled";
    run.raw.push(SUCCESS_RAW);
}

export class HistoryWindowState {

    readonly entries: DriverAction[] = [];

    constructor(private driver: DriverState) {
        this.driver.actionLog$.subscribe((action) => {
            this.entries.unshift(action);
        });
    }
}

const RUNNING = `## Running> `;
const TIMESTAMP = `## Timestamp> `;
const RESULT = `## Result> `;
const SUCCESS = `Success`;
const SUCCESS_RAW = `success`;
const ERROR = `Error`;
const MULTI_QUERY_HEADER = `## `;
const MULTI_QUERY_BORDER = `################################`;
const TABLE_INDENT = "   ";
const CONTENT_INDENT = "    ";
const TABLE_DASHES = 7;

function conceptDisplayString(concept: Concept | undefined): string {
    if (!concept) return "";

    switch (concept.kind) {
        case "entityType":
            return formatType(concept.label);
        case "relationType":
            return formatType(concept.label);
        case "roleType":
            return formatType(concept.label);
        case "attributeType":
            return formatType(concept.label);
        case "entity":
            return `${concept.type ? formatIsa(concept.type.label) : ""}, ${formatIid(concept.iid)}`;
        case "relation":
            return `${concept.type ? formatIsa(concept.type.label) : ""}, ${formatIid(concept.iid)}`;
        case "attribute":
            return `${concept.type ? formatIsa(concept.type.label) : ""} ${formatValue(concept)}`;
        case "value":
            return formatValue(concept);
    }
}

function formatValue(value: Attribute | Value): string {
    switch (value.valueType) {
        case "string":
            return `"${value.value}"`;
        default:
            return `${value.value}`;
    }
}

// TODO: syntax highlighting
function formatType(label: string): string {
    return label;
    // return `\x1b[95m${label}\x1b[0m`;
}

function formatIid(iid: string): string {
    return `${formatKeyword("iid")} ${iid}`;
}

function formatIsa(label: string): string {
    return `${formatKeyword("isa")} ${label}`;
    // return `${this.formatKeyword("isa")} \x1b[95m${label}\x1b[0m`;
}

function formatKeyword(keyword: string): string {
    return keyword;
    // return `\x1b[94m${keyword}\x1b[0m`;
}

function indent(indentation: string, string: string): string {
    return string.split('\n').map(s => `${indentation}${s}`).join('\n');
}

function stringifyError(err: any): string {
    if (isApiErrorResponse(err)) return JSON.stringify(err, null, 2);
    if (err instanceof Error) return JSON.stringify({ message: err.message, name: err.name }, null, 2);
    try {
        return JSON.stringify(err, null, 2);
    } catch {
        return JSON.stringify({ message: err?.toString() ?? `Unknown error` }, null, 2);
    }
}

export class LogOutputState {

    control = new FormControl("", {nonNullable: true});
    /** If true, the viewing component should keep the log scrolled to the bottom as new content arrives.
     *  Flipped to false the first time the user scrolls away from the bottom manually. */
    autoscrollEnabled = true;
    private buffer: string[] = [];
    private flushScheduled = false;

    constructor() {}

    appendLines(...lines: string[]) {
        this.buffer.push(lines.join(`\n`));
        this.scheduleFlush();
    }

    appendBlankLine() {
        this.buffer.push(``);
        this.scheduleFlush();
    }

    /** Flush buffered lines and/or progress to the FormControl.
     *  Progress is a "sticky-bottom" live-status line: while progress is still updating,
     *  it should always sit at the bottom of the log, with new content sliding in above it.
     *  Once progress stops updating, the last progress line freezes in place as historical
     *  content, and any further appended content (e.g. "Committed.") lands *below* it. */
    flush() {
        this.flushScheduled = false;
        let next = this.control.value;

        if (this.progressLine != null) {
            // Progress is still updating: strip the old trailing progress line so we can
            // append buffered content above the new progress and re-emit progress at the bottom.
            if (this.lastFlushedProgressLine != null && next.endsWith(this.lastFlushedProgressLine)) {
                next = next.slice(0, next.length - this.lastFlushedProgressLine.length);
            }
            if (this.buffer.length > 0) {
                next += this.buffer.join(`\n`) + `\n`;
                this.buffer.length = 0;
            }
            next += this.progressLine;
            this.lastFlushedProgressLine = this.progressLine;
            this.progressLine = null;
        } else if (this.buffer.length > 0) {
            // Progress is no longer updating: any prior progress line is now frozen
            // historical content. Just append the buffer below it (with a newline if needed).
            if (this.lastFlushedProgressLine != null && next.endsWith(this.lastFlushedProgressLine)) {
                next += `\n`;
            }
            next += this.buffer.join(`\n`) + `\n`;
            this.buffer.length = 0;
            // The progress line is no longer at the very end, so don't try to strip it later.
            this.lastFlushedProgressLine = null;
        }

        if (next !== this.control.value) this.control.patchValue(next);
    }

    private progressLine: string | null = null;
    private lastFlushedProgressLine: string | null = null;

    /** Set a progress line that overwrites the control value on next flush. */
    setProgress(line: string) {
        this.progressLine = line;
        this.scheduleFlush();
    }

    /** Convert the freshest progress line (pending or last-flushed) into frozen historical
     *  content, so subsequent appends will land below it rather than replacing it. Call this
     *  when progress updates have stopped (e.g. on completion or error). */
    freezeProgress() {
        const final = this.progressLine ?? this.lastFlushedProgressLine;
        this.progressLine = null;
        if (final == null) return;
        // Strip the old trailing progress text (if any) and re-emit the freshest as frozen content.
        if (this.lastFlushedProgressLine != null && this.control.value.endsWith(this.lastFlushedProgressLine)) {
            this.control.patchValue(this.control.value.slice(0, this.control.value.length - this.lastFlushedProgressLine.length) + final + `\n`);
        } else {
            this.control.patchValue(this.control.value + final + `\n`);
        }
        this.lastFlushedProgressLine = null;
    }

    private scheduleFlush() {
        if (this.flushScheduled) return;
        this.flushScheduled = true;
        setTimeout(() => this.flush(), 500);
    }

    appendQueryResult(res: ApiResponse<QueryResponse>, autoCommitted?: boolean, resultLimit?: number) {
        if (isApiErrorResponse(res)) {
            this.appendLines(`${RESULT}${ERROR}`, ``, res.err.message);
            return;
        }

        let lines: string[] = [];
        lines.push(`${RESULT}${SUCCESS}`, ``);

        switch (res.ok.answerType) {
            case "ok": {
                lines.push(`Finished ${res.ok.queryType} query.`);
                break;
            }
            case "conceptRows": {
                lines.push(`Finished ${res.ok.queryType} query compilation and validation...`);
                if (res.ok.queryType === "write") lines.push(`Finished writes. Printing rows...`);
                else lines.push(`Printing rows...`);

                const answers = res.ok.answers;

                if (answers.length) {
                    const varNames = Object.keys(answers[0]).sort();
                    if (varNames.length) {
                        const columnNames = Object.keys(answers[0].data);
                        const variableColumnWidth = columnNames.length > 0 ? Math.max(...columnNames.map(s => s.length)) : 0;
                        answers.forEach((rowAnswer, idx) => {
                            if (idx == 0) lines.push(this.lineDashSeparator(variableColumnWidth));
                            lines.push(this.conceptRowDisplayString(rowAnswer.data, variableColumnWidth))
                        })
                    } else lines.push(`No columns to show.`);
                }

                lines.push(`Finished. Total rows: ${answers.length}`);
                if (resultLimit && answers.length >= resultLimit) lines.push(`Results are limited to ${resultLimit} rows.`);
                break;
            }
            case "conceptDocuments": {
                lines.push(`Finished ${res.ok.queryType} query compilation and validation...`);
                if (res.ok.queryType === "write") lines.push(`Finished writes. Printing documents...`);
                else lines.push(`Printing documents...`);

                const answers = res.ok.answers;
                answers.forEach(x => lines.push(this.conceptDocumentDisplayString(x)));

                lines.push(`Finished. Total documents: ${answers.length}`);
                if (resultLimit && answers.length >= resultLimit) lines.push(`Results are limited to ${resultLimit} rows.`);
                break;
            }
            default:
                throw new Error(INTERNAL_ERROR);
        }

        if (autoCommitted) {
            lines.push(`Committed.`);
        }

        this.appendLines(...lines);
        this.appendBlankLine();
    }

    private conceptDocumentDisplayString(document: ConceptDocument): string {
        try {
            return JSON.stringify(document, null, 2);
        } catch (err) {
            return `Error trying to print JSON: ${err}`;
        }
    }

    private conceptRowDisplayString(row: ConceptRow, variableColumnWidth: number) {
        const columnNames = Object.keys(row).sort();
        const contents = columnNames.map((columnName) =>
            `\$${columnName}${" ".repeat(variableColumnWidth - columnName.length + 1)}| ${conceptDisplayString(row[columnName])}`
        );
        const content = contents.join("\n");
        return `${indent(CONTENT_INDENT, content)}\n${this.lineDashSeparator(variableColumnWidth)}`;
    }

    private lineDashSeparator(additionalDashesNum: number): string {
        return indent(TABLE_INDENT, "-".repeat(TABLE_DASHES + additionalDashesNum));
    }

    clear() {
        this.control.patchValue(``);
    }
}

type TableOutputStatus = "ok" | "running" | "answerlessQueryType" | "answerOutputDisabled" | "noColumns" | "noAnswers" | "error";

type TableRow = { [column: string]: string };

export class TableOutputState {

    status: TableOutputStatus = "ok";
    private _data$ = new BehaviorSubject<TableRow[]>([]);
    private _unsortedRows: TableRow[] = [];
    private _sortActive: string | null = null;
    private _sortDirection: "" | "asc" | "desc" = "";
    private _columns: string[] = [];
    private _displayedColumns: string[] = [];

    constructor() {}

    get data$(): Observable<TableRow[]> {
        return this._data$;
    }

    get columns(): string[] {
        return this._columns;
    }

    get displayedColumns(): string[] {
        return this._displayedColumns;
    }

    handleMatSortChange(e: { active: string; direction: "" | "asc" | "desc" }) {
        this._sortActive = e.active || null;
        this._sortDirection = e.direction;
        this.emitSortedView();
    }

    private emitSortedView() {
        if (!this._sortActive || this._sortDirection === "") {
            this._data$.next([...this._unsortedRows]);
            return;
        }
        const col = this._sortActive;
        const dir = this._sortDirection === "desc" ? -1 : 1;
        const sorted = [...this._unsortedRows].sort((a, b) => compareCells(a[col], b[col]) * dir);
        this._data$.next(sorted);
    }

    push(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) {
            this.status = "error";
            return;
        }

        switch (res.ok.answerType) {
            case "ok": {
                this.status = "answerlessQueryType";
                break;
            }
            case "conceptRows": {
                const answers = res.ok.answers;
                if (answers.length) {
                    const varNames = Object.keys(answers[0].data);
                    if (varNames.length) {
                        this.status = "ok";
                        this.appendColumns(...varNames);
                        setTimeout(() => {
                            this.appendConceptRows(...answers.map(x => x.data));
                        });
                    } else this.status = "noColumns";
                } else this.status = "noAnswers";
                break;
            }
            case "conceptDocuments": {
                const answers = res.ok.answers;
                if (answers.length) {
                    const seen = new Set<string>();
                    const keys: string[] = [];
                    for (const answer of answers) {
                        for (const key of Object.keys(answer)) {
                            if (!seen.has(key)) {
                                seen.add(key);
                                keys.push(key);
                            }
                        }
                    }
                    if (keys.length) {
                        this.status = "ok";
                        this.appendColumns(...keys);
                        setTimeout(() => {
                            this.appendRows(...answers.map(answer => Object.fromEntries(Object.entries(answer).map(([k, v]) => [k, JSON.stringify(v)]))));
                        });
                    } else this.status = "noColumns";
                } else this.status = "noAnswers";
                break;
            }
            default:
                throw new Error(INTERNAL_ERROR);
        }
    }

    private appendConceptRows(...rows: ConceptRow[]) {
        const tableRows: TableRow[] = rows.map(x => Object.fromEntries(Object.entries(x).map(
            ([varName, concept]) => [varName, this.conceptDisplayString(concept)]
        )));
        this.appendRows(...tableRows);
    }

    private appendColumns(...columns: string[]) {
        this.columns.push(...columns);
        this.displayedColumns.push(...columns);
    }

    private appendRows(...rows: { [column: string]: string }[]) {
        this._unsortedRows.push(...rows);
        this.emitSortedView();
    }

    private conceptDisplayString(concept: Concept | undefined): string {
        if (!concept) return "";

        switch (concept.kind) {
            case "entityType":
            case "relationType":
            case "roleType":
            case "attributeType":
                return concept.label;
            case "entity":
            case "relation":
                return `iid ${(concept.iid)}`;
            case "attribute":
                return `${concept.value}`;
            case "value":
                return `${concept.value}`;
        }
    }

    clearStatus() {
        this.status = "ok";
    }

    clear() {
        this.clearStatus();
        this.columns.length = 0;
        this.displayedColumns.length = 0;
        this._unsortedRows = [];
        this._sortActive = null;
        this._sortDirection = "";
        this._data$.next([]);
    }
}

function compareCells(a: string | undefined, b: string | undefined): number {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;
    const an = Number(a);
    const bn = Number(b);
    if (a !== "" && b !== "" && !isNaN(an) && !isNaN(bn)) {
        return an - bn;
    }
    return a.localeCompare(b);
}

export type GraphOutputStatus = "ok" | "running" | "graphlessQueryType" | "answerOutputDisabled" | "noQueryAnswers" | "noInstancesFound" | "error" | "multiQuery";

export class GraphOutputState {

    status: GraphOutputStatus = "ok";
    visualiser: GraphVisualiser | null = null;
    query?: string;
    database?: string;
    private _canvasEl: HTMLElement | null = null;
    private _preservedGraph: Graph | null = null;
    private _preservedCamera: { x: number; y: number; ratio: number; angle: number } | null = null;
    private _pendingResponses: ApiResponse<QueryResponse>[] = [];
    /** Display-attribute responses recorded *before* the visualiser exists.
     *  Drained into the visualiser as soon as `pushInternal` constructs it. */
    private _pendingDisplayAttrs: Array<{ res: ApiResponse<QueryResponse>; ownerVar: string; attrVar: string }> = [];
    /** Pre-visualiser buffer for the label override map (same pattern as
     *  display-attrs). Drained into the visualiser as soon as it's created. */
    private _pendingLabelOverrides: Map<string, string> | null = null;
    private _styleService: GraphStyleService;

    constructor(styleService: GraphStyleService) {
        this._styleService = styleService;
    }

    get canvasEl(): HTMLElement | null {
        return this._canvasEl;
    }

    set canvasEl(el: HTMLElement) {
        this._canvasEl = el;
        if (this._pendingResponses.length > 0) {
            const pending = [...this._pendingResponses];
            this._pendingResponses = [];
            for (const res of pending) {
                this.pushInternal(res);
            }
        }
    }

    push(res: ApiResponse<QueryResponse>) {
        if (!this._canvasEl) {
            this._pendingResponses.push(res);
            return;
        }
        this.pushInternal(res);
    }

    /** Forward a display-attribute response to the visualiser, or buffer it
     *  if the visualiser hasn't been constructed yet (instance push hasn't
     *  fired). Buffered records are replayed before any instance build runs. */
    recordDisplayAttributes(res: ApiResponse<QueryResponse>, ownerVar: string, attrVar: string = "a"): void {
        if (this.visualiser) {
            this.visualiser.recordDisplayAttributes(res, ownerVar, attrVar);
            this.visualiser.refreshLabels();
        } else {
            this._pendingDisplayAttrs.push({ res, ownerVar, attrVar });
        }
    }

    /** Apply the full per-type label override map to the visualiser, or buffer
     *  it if the visualiser doesn't exist yet (so the very first build's
     *  labels reflect persisted user overrides). */
    applyLabelOverrides(overrides: Map<string, string>): void {
        if (this.visualiser) {
            this.visualiser.applyLabelOverrides(overrides);
        } else {
            this._pendingLabelOverrides = new Map(overrides);
        }
    }

    private pushInternal(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) {
            this.status = "error";
            return;
        }

        if (!this.visualiser) {
            const graph = this._preservedGraph ?? newGraph();
            this._preservedGraph = graph;
            const sigma = createSigmaRenderer(this._canvasEl!, defaultSigmaSettings as any, graph);
            const layout = Layouts.createD3ForceSupervisor(graph);
            this.visualiser = new GraphVisualiser(graph, sigma, layout, this._styleService);
            // Replay any display-attribute responses that arrived before the
            // visualiser existed so the imminent build picks up correct labels.
            if (this._pendingDisplayAttrs.length > 0) {
                for (const p of this._pendingDisplayAttrs) {
                    this.visualiser.recordDisplayAttributes(p.res, p.ownerVar, p.attrVar);
                }
                this._pendingDisplayAttrs = [];
            }
            if (this._pendingLabelOverrides) {
                this.visualiser.applyLabelOverrides(this._pendingLabelOverrides);
                this._pendingLabelOverrides = null;
            }
        }

        switch (res.ok.answerType) {
            case "ok": {
                this.status = "graphlessQueryType";
                break;
            }
            case "conceptRows": {
                this.visualiser.handleQueryResponse(res, this.database!);
                let highlightedQuery = "";
                if (QUERY_HIGHLIGHT_DIV_ID != null) {
                    if (res.ok.query) {
                        highlightedQuery = this.visualiser.colorQuery(this.query!, res.ok.query);
                        document.getElementById("query-highlight-div")!.innerHTML = highlightedQuery;
                    }
                }
                this.visualiser.colorEdgesByConstraintIndex(!this._styleService.colorEdgesByConstraint);
                this.status = res.ok.answers.length > 0 ? "ok" : "noQueryAnswers";
                break;
            }
            case "conceptDocuments": {
                this.status = "graphlessQueryType";
                break;
            }
            default:
                throw new Error(INTERNAL_ERROR);
        }
    }

    resize() {
        this.visualiser?.sigma.resize();
        this.visualiser?.sigma.refresh();
    }

    detach(): void {
        if (this.visualiser) {
            this._preservedGraph = this.visualiser.graph;
            const cam = this.visualiser.sigma.getCamera().getState();
            this._preservedCamera = { x: cam.x, y: cam.y, ratio: cam.ratio, angle: cam.angle };
            this.visualiser.destroy();
            this.visualiser = null;
        }
        this._canvasEl = null;
    }

    attach(canvasEl: HTMLElement): void {
        this.canvasEl = canvasEl;
        if (this._preservedGraph && this._preservedGraph.nodes().length > 0 && !this.visualiser) {
            const sigma = createSigmaRenderer(canvasEl, defaultSigmaSettings as any, this._preservedGraph);
            const layout = Layouts.createD3ForceStatic(this._preservedGraph);
            this.visualiser = new GraphVisualiser(this._preservedGraph, sigma, layout, this._styleService);
            if (this._preservedCamera) {
                this.visualiser.sigma.getCamera().setState(this._preservedCamera);
                this._preservedCamera = null;
            }
        }
    }

    destroy() {
        this.visualiser?.destroy();
        this.visualiser = null;
        this._preservedGraph = null;
    }
}

export class RawOutputState {

    control = new FormControl("", {nonNullable: true});

    constructor() {}

    push(value: string) {
        this.control.patchValue(value);
    }

    clear() {
        this.control.patchValue(``);
    }
}
