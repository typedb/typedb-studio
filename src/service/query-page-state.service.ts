/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { inject, Injectable } from "@angular/core";
import { FormControl } from "@angular/forms";
import { BehaviorSubject, combineLatest, map, Observable, pairwise, shareReplay, startWith, switchMap } from "rxjs";
import { DriverAction } from "../concept/action";
import {createSigmaRenderer, GraphVisualiser} from "../framework/graph-visualiser";
import { defaultSigmaSettings } from "../framework/graph-visualiser/defaults";
import { newVisualGraph, VisualGraph } from "../framework/graph-visualiser/graph";
import { Layouts } from "../framework/graph-visualiser/layouts";
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

export type OutputType = "raw" | "log" | "table" | "graph";
export { RowLimit } from "./app-data.service";

export interface RunOutputState {
    id: string;
    label: string;
    query: string;
    log: LogOutputState;
    table: TableOutputState;
    graph: GraphOutputState;
    raw: RawOutputState;
}

export function createRunOutputState(label: string, query: string): RunOutputState {
    return {
        id: crypto.randomUUID(),
        label,
        query,
        log: new LogOutputState(),
        table: new TableOutputState(),
        graph: new GraphOutputState(),
        raw: new RawOutputState(),
    };
}

export interface TabOutputState {
    runs: RunOutputState[];
    selectedRunIndex: number;
    runCounter: number;
    outputTypeControl: FormControl<OutputType>;
}

function createTabOutputState(): TabOutputState {
    return {
        runs: [],
        selectedRunIndex: -1,
        runCounter: 0,
        outputTypeControl: new FormControl("log" as OutputType, { nonNullable: true }),
    };
}

function currentRun(tabState: TabOutputState): RunOutputState | null {
    if (tabState.selectedRunIndex < 0 || tabState.selectedRunIndex >= tabState.runs.length) {
        return null;
    }
    return tabState.runs[tabState.selectedRunIndex];
}

export const ROW_LIMIT_OPTIONS: { value: RowLimit; label: string }[] = [
    { value: 10, label: "10" },
    { value: 50, label: "50" },
    { value: 100, label: "100" },
    { value: 500, label: "500" },
    { value: 1000, label: "1000" },
    { value: 5000, label: "5000" },
    { value: "none", label: "No limit" },
];

const NO_SERVER_CONNECTED = `No server connected`;
const NO_DATABASE_SELECTED = `No database selected`;
const NO_OPEN_TRANSACTION = `No open transaction`;
const QUERY_BLANK = `Query text is blank`;
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

    outputTypes: OutputType[] = ["log", "table", "graph", "raw"];
    rowLimitControl = new FormControl(this.appData.preferences.queryRowLimit(), { nonNullable: true });
    rowLimitOptions = ROW_LIMIT_OPTIONS;
    readonly history = new HistoryWindowState(this.driver);
    answersOutputEnabled = true;

    private tabOutputStates = new Map<string, TabOutputState>();
    private _fallbackOutputState = createTabOutputState();
    private _fallbackRunState = createRunOutputState("", "");
    private _graphCanvasEl: HTMLElement | null = null;

    getOrCreateTabOutputState(tabId: string): TabOutputState {
        let state = this.tabOutputStates.get(tabId);
        if (!state) {
            state = createTabOutputState();
            this.tabOutputStates.set(tabId, state);
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
        this.driver.transaction$, this.currentTabQuery$, this.queryTabs.selectedTabIndex$
    ]).pipe(map(([status, db, txMode, tx, query]) => {
        if (status !== "connected") return NO_SERVER_CONNECTED;
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

    setGraphCanvasEl(el: HTMLElement): void {
        this._graphCanvasEl = el;
        const run = currentRun(this.currentTabOutputState);
        if (run) {
            run.graph.canvasEl = el;
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
            const curRun = currentRun(this.currentTabOutputState);
            if (curRun) curRun.graph.attach(this._graphCanvasEl);
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

    runQuery(query: string) {
        const tabState = this.currentTabOutputState;

        // Detach current run's graph before creating new run
        const oldRun = currentRun(tabState);
        if (oldRun) oldRun.graph.detach();

        // Create new run
        tabState.runCounter++;
        const newRun = createRunOutputState(`Run ${tabState.runCounter}`, query);
        tabState.runs.push(newRun);
        tabState.selectedRunIndex = tabState.runs.length - 1;

        // Initialise the new run's outputs
        newRun.log.appendLines(RUNNING, query, ``, `${TIMESTAMP}${new Date().toISOString()}`);
        newRun.table.status = "running";
        newRun.graph.status = "running";
        newRun.graph.query = query;
        newRun.graph.database = this.driver.requireDatabase().name;
        if (this._graphCanvasEl) {
            newRun.graph.canvasEl = this._graphCanvasEl;
        }

        const rowLimit = this.rowLimitControl.value;
        const queryOptions = rowLimit !== "none" ? { answerCountLimit: rowLimit } : undefined;
        this.driver.query(query, queryOptions).subscribe({
            next: (res) => {
                this.outputQueryResponseToRun(newRun, res);
            },
            error: (err) => {
                newRun.table.status = "error";
                newRun.graph.status = "error";
                this.driver.checkHealth().subscribe({
                    next: () => {
                        let msg = ``;
                        if (isApiErrorResponse(err)) {
                            msg = err.err.message;
                        } else {
                            msg = err?.message ?? err?.toString() ?? `Unknown error`;
                        }
                        newRun.log.appendBlankLine();
                        newRun.log.appendLines(`${RESULT}${ERROR}`, ``, msg);
                    },
                    error: () => {
                        const msg = `Unable to connect to TypeDB server.`;
                        newRun.log.appendBlankLine();
                        newRun.log.appendLines(`${RESULT}${ERROR}`, ``, msg);
                    }
                });
            },
        });
    }

    private outputQueryResponseToRun(run: RunOutputState, res: ApiResponse<QueryResponse>) {
        const autoCommitted = this.driver.autoTransactionEnabled$.value && !isApiErrorResponse(res) && res.ok.queryType !== "read";
        if (this.answersOutputEnabled) this.outputQueryResponseWithAnswers(run, res, autoCommitted);
        else this.outputQueryResponseNoAnswers(run, autoCommitted);
    }

    private outputQueryResponseWithAnswers(run: RunOutputState, res: ApiResponse<QueryResponse>, autoCommitted: boolean) {
        run.log.appendBlankLine();
        run.log.appendQueryResult(res, autoCommitted);
        run.table.push(res);
        try {
            run.graph.push(res);
        } catch (err) {
            console.error("[Graph Output Error]", err);
            run.graph.status = "error";
            this.snackbar.errorPersistent(`Failed to render graph visualization: ${err}`);
        }
        run.raw.push(JSON.stringify(res, null, 2));
    }

    private outputQueryResponseNoAnswers(run: RunOutputState, autoCommitted: boolean) {
        run.log.appendBlankLine();
        run.log.appendLines(`${RESULT}${SUCCESS}`);
        if (autoCommitted) run.log.appendLines(`Committed.`);
        run.table.status = "answerOutputDisabled";
        run.graph.status = "answerOutputDisabled";
        run.raw.push(SUCCESS_RAW);
    }
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

export class LogOutputState {

    control = new FormControl("", {nonNullable: true});

    constructor() {}

    appendLines(...lines: string[]) {
        this.control.patchValue(`${this.control.value}${lines.join(`\n`)}\n`);
    }

    appendBlankLine() {
        this.appendLines(``);
    }

    appendQueryResult(res: ApiResponse<QueryResponse>, autoCommitted?: boolean) {
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
                break;
            }
            case "conceptDocuments": {
                lines.push(`Finished ${res.ok.queryType} query compilation and validation...`);
                if (res.ok.queryType === "write") lines.push(`Finished writes. Printing documents...`);
                else lines.push(`Printing documents...`);

                const answers = res.ok.answers;
                answers.forEach(x => lines.push(this.conceptDocumentDisplayString(x)));

                lines.push(`Finished. Total documents: ${answers.length}`);
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

    handleMatSortChange(e: any) {
        // TODO
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
                    const keys = Object.keys(answers[0]);
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
        this._data$.value.push(...rows);
        this._data$.next(this._data$.value);
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
        this._data$.next([]);
    }
}

type GraphOutputStatus = "ok" | "running" | "graphlessQueryType" | "answerOutputDisabled" | "noAnswers" | "error";

export class GraphOutputState {

    status: GraphOutputStatus = "ok";
    visualiser: GraphVisualiser | null = null;
    query?: string;
    database?: string;
    private _canvasEl: HTMLElement | null = null;
    private _preservedGraph: VisualGraph | null = null;
    private _pendingResponses: ApiResponse<QueryResponse>[] = [];

    constructor() {
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

    private pushInternal(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) {
            this.status = "error";
            return;
        }

        if (!this.visualiser) {
            const graph = this._preservedGraph ?? newVisualGraph();
            this._preservedGraph = graph;
            const sigma = createSigmaRenderer(this._canvasEl!, defaultSigmaSettings as any, graph);
            const layout = Layouts.createForceAtlasStatic(graph, undefined); // This is the safe option
            // const layout = Layouts.createForceLayoutSupervisor(graph, studioDefaults.defaultForceSupervisorSettings);
            this.visualiser = new GraphVisualiser(graph, sigma, layout);
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
                this.visualiser.colorEdgesByConstraintIndex(false);
                this.status = "ok";
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
            this.visualiser.sigma.kill();
            this.visualiser = null;
        }
        this._canvasEl = null;
    }

    attach(canvasEl: HTMLElement): void {
        this.canvasEl = canvasEl;
        if (this._preservedGraph && this._preservedGraph.nodes().length > 0 && !this.visualiser) {
            const sigma = createSigmaRenderer(canvasEl, defaultSigmaSettings as any, this._preservedGraph);
            const layout = Layouts.createForceAtlasStatic(this._preservedGraph, undefined);
            this.visualiser = new GraphVisualiser(this._preservedGraph, sigma, layout);
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
