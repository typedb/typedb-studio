/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { FormControl } from "@angular/forms";
import MultiGraph from "graphology";
import { BehaviorSubject, combineLatest, first, map, Observable, ReplaySubject, startWith } from "rxjs";
import { DriverAction } from "../concept/action";
import { createSigmaRenderer, GraphVisualiser } from "../framework/graph-visualiser";
import { defaultSigmaSettings } from "../framework/graph-visualiser/defaults";
import { newVisualGraph } from "../framework/graph-visualiser/graph";
import { Layouts } from "../framework/graph-visualiser/layouts";
import { Concept, Value } from "../framework/typedb-driver/concept";
import { ApiResponse, ConceptDocument, ConceptRow, isApiErrorResponse, QueryResponse } from "../framework/typedb-driver/response";
import { INTERNAL_ERROR } from "../framework/util/strings";
import { DriverState } from "./driver-state.service";
import { SchemaState, SchemaTree, SchemaTreeType } from "./schema-state.service";
import { SnackbarService } from "./snackbar.service";
import { FlatTreeControl } from "@angular/cdk/tree";
import { MatTreeFlatDataSource, MatTreeFlattener } from "@angular/material/tree";

export type OutputType = "raw" | "log" | "table" | "graph";

const NO_SERVER_CONNECTED = `No server connected`;
const NO_DATABASE_SELECTED = `No database selected`;
const NO_OPEN_TRANSACTION = `No open transaction`;
const QUERY_BLANK = `Query text is blank`;

@Injectable({
    providedIn: "root",
})
export class QueryToolState {

    queryControl = new FormControl("", {nonNullable: true});
    outputTypeControl = new FormControl("log" as OutputType, { nonNullable: true });
    outputTypes: OutputType[] = ["log", "table", "graph", "raw"];
    readonly schemaWindow = new SchemaWindowState(this.schema);
    readonly logOutput = new LogOutputState();
    readonly tableOutput = new TableOutputState();
    readonly graphOutput = new GraphOutputState();
    readonly rawOutput = new RawOutputState();
    readonly history = new HistoryWindowState(this.driver);
    answersOutputEnabled = true;
    readonly runDisabledReason$ = combineLatest(
        [this.driver.status$, this.driver.database$, this.driver.autoTransactionEnabled$, this.driver.transaction$, this.queryControl.valueChanges.pipe(startWith(this.queryControl.value))]
    ).pipe(map(([status, db, autoTransactionEnabled, tx, _query]) => {
        if (status !== "connected") return NO_SERVER_CONNECTED;
        else if (db == null) return NO_DATABASE_SELECTED;
        else if (!autoTransactionEnabled && !tx) return NO_OPEN_TRANSACTION;
        else if (!this.queryControl.value.length) return QUERY_BLANK; // _query becomes blank after a page navigation for some reason
        else return null;
    }));
    readonly runEnabled$ = this.runDisabledReason$.pipe(map(x => x == null));
    readonly outputDisabledReason$ = this.driver.status$.pipe(map(x => x === "connected" ? null : NO_SERVER_CONNECTED));
    readonly outputDisabled$ = this.outputDisabledReason$.pipe(map(x => x != null));

    constructor(private driver: DriverState, private schema: SchemaState, private snackbar: SnackbarService) {
        (window as any)["queryToolState"] = this;
        this.outputDisabled$.subscribe((disabled) => {
            if (disabled) this.outputTypeControl.disable();
            else this.outputTypeControl.enable();
        });
    }

    // TODO: LIMIT 1000 by default, configurable

    runQuery() {
        const query = this.queryControl.value;
        this.initialiseOutput(query);
        this.driver.query(query).subscribe({
            next: (res) => {
                this.outputQueryResponse(res);
                if (isApiErrorResponse(res)) {
                    this.snackbar.errorPersistent(res.err.message);
                }
            },
            error: (err) => {
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
            },
        });
    }

    private initialiseOutput(query: string) {
        this.logOutput.clear();
        this.tableOutput.clear();
        this.graphOutput.destroy();
        this.rawOutput.clear();

        this.logOutput.appendLines(RUNNING, query, ``, `${TIMESTAMP}${new Date().toISOString()}`);
        this.tableOutput.status = "running";
        this.graphOutput.status = "running";
        this.graphOutput.query = query;
        this.graphOutput.database = this.driver.requireDatabase(`${this.constructor.name}.${this.initialiseOutput.name} > requireValue(driver.database$)`).name;
    }

    private outputQueryResponse(res: ApiResponse<QueryResponse>) {
        if (this.answersOutputEnabled) this.outputQueryResponseWithAnswers(res);
        else this.outputQueryResponseNoAnswers();
    }

    private outputQueryResponseWithAnswers(res: ApiResponse<QueryResponse>) {
        this.logOutput.appendBlankLine();
        this.logOutput.appendQueryResult(res);
        this.tableOutput.push(res);
        this.graphOutput.push(res);
        this.rawOutput.push(JSON.stringify(res, null, 2));
    }

    private outputQueryResponseNoAnswers() {
        this.logOutput.appendBlankLine();
        this.logOutput.appendLines(`${RESULT}${SUCCESS}`);
        this.tableOutput.status = "answerOutputDisabled";
        this.graphOutput.status = "answerOutputDisabled";
        this.rawOutput.push(SUCCESS_RAW);
    }
}

interface SchemaTreeNode {
    label?: string;
    type?: SchemaTreeType;
    children: SchemaTreeNode[];
}

/** Flat node with expandable and level information */
interface FlatNode {
    expandable: boolean;
    name: string;
    level: number;
}

export class SchemaWindowState {
    private _transformer = (node: SchemaTreeNode, level: number) => {
        return {
            expandable: !!node.children.length,
            name: node.label || node.type?.label || ``,
            level: level,
        };
    };

    treeFlattener = new MatTreeFlattener(
        this._transformer,
        node => node.level,
        node => node.expandable,
        node => node.children,
    );

    treeControl = this.createTreeControl();
    entitiesTreeControl = this.createTreeControl();
    relationsTreeControl = this.createTreeControl();
    attributesTreeControl = this.createTreeControl();
    dataSource: MatTreeFlatDataSource<SchemaTreeNode, FlatNode> = new MatTreeFlatDataSource(this.treeControl, this.treeFlattener);
    dataSourcesObj: Record<"Entities" | "Relations" | "Attributes", { title: string, treeControl: FlatTreeControl<FlatNode>, dataSource: MatTreeFlatDataSource<SchemaTreeNode, FlatNode> }> = {
        "Entities": { title: "Entities", treeControl: this.entitiesTreeControl, dataSource: new MatTreeFlatDataSource(this.entitiesTreeControl, this.treeFlattener) },
        "Relations": { title: "Relations", treeControl: this.relationsTreeControl, dataSource: new MatTreeFlatDataSource(this.relationsTreeControl, this.treeFlattener) },
        "Attributes": { title: "Attributes", treeControl: this.attributesTreeControl, dataSource: new MatTreeFlatDataSource(this.attributesTreeControl, this.treeFlattener) },
    };
    dataSources = Object.values(this.dataSourcesObj);

    private createTreeControl() {
        return new FlatTreeControl<FlatNode>(
            node => node.level,
            node => node.expandable,
        );
    }

    constructor(public schemaState: SchemaState) {
        schemaState.tree.data$.subscribe(data => {
            this.populateDataSources(data);
        });
    }

    hasChild = (_: number, node: FlatNode) => node.expandable;

    private populateDataSources(data: SchemaTree | null) {
        if (!data) {
            this.dataSources.forEach(x => x.dataSource.data = []);
            return;
        }
        this.dataSource.data = [{
            label: "Entities",
            children: data.entities.map(x => ({
                type: x,
                children: [],
            })),
        }, {
            label: "Relations",
            children: data.relations.map(x => ({
                type: x,
                children: [],
            })),
        }, {
            label: "Attributes",
            children: data.attributes.map(x => ({
                type: x,
                children: [],
            })),
        }];
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
            return `${concept.type ? formatIsa(concept.type.label) : ""} ${concept.value}`;
        case "value":
            return `${concept.value}`;
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

    appendQueryResult(res: ApiResponse<QueryResponse>) {
        if (isApiErrorResponse(res)) {
            this.appendLines(`${RESULT}${ERROR}`, ``, res.err.message);
            return;
        }

        this.appendLines(`${RESULT}${SUCCESS}`, ``);

        switch (res.ok.answerType) {
            case "ok": {
                this.appendLines(`Finished ${res.ok.queryType} query.`);
                break;
            }
            case "conceptRows": {
                this.appendLines(`Finished ${res.ok.queryType} query compilation and validation...`);
                if (res.ok.queryType === "write") this.appendLines(`Finished writes. Printing rows...`);
                else this.appendLines(`Printing rows...`);

                if (res.ok.answers.length) {
                    const varNames = Object.keys(res.ok.answers[0]);
                    if (varNames.length) {
                        res.ok.answers.forEach(((x, idx) => this.appendConceptRow(x.data, idx === 0)));
                    } else this.appendLines(`No columns to show.`);
                }

                this.appendLines(`Finished. Total rows: ${res.ok.answers.length}`);
                break;
            }
            case "conceptDocuments": {
                this.appendLines(`Finished ${res.ok.queryType} query compilation and validation...`);
                if (res.ok.queryType === "write") this.appendLines(`Finished writes. Printing documents...`);
                else this.appendLines(`Printing documents...`);
                res.ok.answers.forEach(x => this.appendConceptDocument(x));
                this.appendLines(`Finished. Total documents: ${res.ok.answers.length}`);
                break;
            }
            default:
                throw INTERNAL_ERROR;
        }

        this.appendBlankLine();
    }

    private appendConceptDocument(document: ConceptDocument) {
        try {
            const pretty = JSON.stringify(document, null, 2);
            this.appendLines(pretty);
        } catch (err) {
            this.appendLines(`Error trying to print JSON: ${err}`);
        }
    }

    private appendConceptRow(row: ConceptRow, isFirst: boolean) {
        const columnNames = Object.keys(row);
        const variableColumnWidth = columnNames.length > 0 ? Math.max(...columnNames.map(s => s.length)) : 0;
        if (isFirst) this.appendLines(this.lineDashSeparator(variableColumnWidth));
        this.appendLines(this.conceptRowDisplayString(row, variableColumnWidth));
    }

    private conceptRowDisplayString(row: ConceptRow, variableColumnWidth: number) {
        const columnNames = Object.keys(row);
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
                if (res.ok.answers.length) {
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
                throw INTERNAL_ERROR;
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
    canvasEl!: HTMLElement;
    visualiser: GraphVisualiser | null = null;
    query?: string;
    database?: string;

    constructor() {
    }

    push(res: ApiResponse<QueryResponse>) {
        if (!this.canvasEl) throw `Missing canvas element`;

        if (!this.visualiser) {
            const graph = newVisualGraph();
            const sigma = createSigmaRenderer(this.canvasEl, defaultSigmaSettings as any, graph);
            const layout = Layouts.createForceAtlasStatic(graph, undefined); // This is the safe option
            // const layout = Layouts.createForceLayoutSupervisor(graph, studioDefaults.defaultForceSupervisorSettings);
            this.visualiser = new GraphVisualiser(graph, sigma, layout);
        }

        if (isApiErrorResponse(res)) {
            this.status = "error";
            return;
        }

        switch (res.ok.answerType) {
            case "ok": {
                this.status = "graphlessQueryType";
                break;
            }
            case "conceptRows": {
                this.visualiser.handleQueryResponse(res, this.database!);
                let highlightedQuery = "";
                if (res.ok.query) {
                    highlightedQuery = this.visualiser.colorQuery(this.query!, res.ok.query);
                }
                this.visualiser.colorEdgesByConstraintIndex(false);
                this.status = "ok";
                // document.getElementById("query-highlight-div").innerHTML = highlightedQuery;
                // if (res.queryStructure != null) {
                //     highlightedQuery = studio.colorQuery(form.query.value, query_result.queryStructure);
                //     studio.colorEdgesByConstraintIndex(false);
                // }
                // document.getElementById("query-highlight-div").innerHTML = highlightedQuery;
                // if (res.answers.length) {
                //     const varNames = Object.keys(res.answers[0]);
                //     if (varNames.length) {
                //         this.status = "ok";
                //         this.appendColumns(...varNames);
                //         setTimeout(() => {
                //             this.appendConceptRows(...res.answers);
                //         });
                //     } else this.status = "noColumns";
                // } else this.status = "noAnswers";
                break;
            }
            case "conceptDocuments": {
                this.status = "graphlessQueryType";
                break;
            }
            default:
                throw INTERNAL_ERROR;
        }
    }

    destroy() {
        this.visualiser?.destroy();
        this.visualiser = null;
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

/*
class QueryRunner constructor(
    val transactionState: TransactionState, // TODO: restrict in the future, when TypeDB 3.0 answers return complete info
    private val notificationSrv: NotificationService,
    private val preferenceSrv: PreferenceService,
    private val queries: String,
    private val onComplete: () -> Unit
) {

    sealed class Response {

        object Done : Response()

        data class Message(val type: Type, val text: String) : Response() {
            enum class Type { INFO, SUCCESS, ERROR, TYPEQL }
        }

        data class Value(val value: com.vaticle.typedb.driver.api.concept.value.Value?) : Response()

        sealed class Stream<T> : Response() {

        val queue = LinkedBlockingQueue<Either<T, Done>>()

        class ConceptMapGroups : Stream<ConceptMapGroup>()
        class ValueGroups : Stream<ValueGroup>()
        class JSONs : Stream<JSON>()
        class ConceptMaps constructor(val source: Source) : Stream<ConceptMap>() {
            enum class Source { INSERT, UPDATE, GET }
        }
    }
}

    companion object {
        const val RESULT_ = "## Result> "
        const val ERROR_ = "## Error> "
        const val RUNNING_ = "## Running> "
        const val COMPLETED = "## Completed"
        const val TERMINATED = "## Terminated"
        const val DEFINE_QUERY = "Define query:"
        const val DEFINE_QUERY_SUCCESS = "Define query successfully defined new types in the schema."
        const val UNDEFINE_QUERY = "Undefine query:"
        const val UNDEFINE_QUERY_SUCCESS = "Undefine query successfully undefined types in the schema."
        const val DELETE_QUERY = "Delete query:"
        const val DELETE_QUERY_SUCCESS = "Delete query successfully deleted things from the database."
        const val INSERT_QUERY = "Insert query:"
        const val INSERT_QUERY_SUCCESS = "Insert query successfully inserted new things to the database:"
        const val INSERT_QUERY_NO_RESULT = "Insert query did not insert any new thing to the database."
        const val UPDATE_QUERY = "Update query:"
        const val UPDATE_QUERY_SUCCESS = "Update query successfully updated things in the databases:"
        const val UPDATE_QUERY_NO_RESULT = "Update query did not update any thing in the databases."
        const val GET_QUERY = "Get query:"
        const val GET_QUERY_SUCCESS = "Get query successfully matched concepts in the database:"
        const val GET_QUERY_NO_RESULT = "Get query did not match any concepts in the database."
        const val GET_AGGREGATE_QUERY = "Get Aggregate query:"
        const val GET_AGGREGATE_QUERY_SUCCESS = "Get Aggregate query successfully calculated:"
        const val GET_GROUP_QUERY = "Get Group query:"
        const val GET_GROUP_QUERY_SUCCESS = "Get Group query successfully matched concept groups in the database:"
        const val GET_GROUP_QUERY_NO_RESULT = "Get Group query did not match any concept groups in the database."
        const val GET_GROUP_AGGREGATE_QUERY = "Get Group Aggregate query:"
        const val GET_GROUP_AGGREGATE_QUERY_SUCCESS =
            "Get Group Aggregate query successfully aggregated matched concept groups in the database:"
        const val GET_GROUP_AGGREGATE_QUERY_NO_RESULT =
            "Get Group Aggregate query did not match any concept groups to aggregate in the database."
        const val FETCH_QUERY = "Fetch query:"
        const val FETCH_QUERY_SUCCESS = "Fetch query successfully retrieved data from the database:"
        const val FETCH_QUERY_NO_RESULT = "Fetch query did not retrieve any data from the database."

        private const val COUNT_DOWN_LATCH_PERIOD_MS: Long = 50
    private val LOGGER = KotlinLogging.logger {}
    }

    var startTime: Long? = null
    var endTime: Long? = null
        val responses = LinkedBlockingQueue<Response>()
    val isConsumed: Boolean get() = consumerLatch.count == 0L
    val isRunning = AtomicBoolean(false)
private val consumerLatch = CountDownLatch(1)
private val coroutines = CoroutineScope(Dispatchers.Default)
private val hasStopSignal get() = transactionState.hasStopSignalAtomic
private val transaction get() = transactionState.transaction!!
private val onClose = LinkedBlockingQueue<() -> Unit>()

    fun onClose(function: () -> Unit) = onClose.put(function)

    fun setConsumed() = consumerLatch.countDown()

private fun collectEmptyLine() = collectMessage(INFO, "")

private fun collectMessage(type: Response.Message.Type, string: String) {
        responses.put(Response.Message(type, string))
    }

    internal fun launch() = coroutines.launchAndHandle(notificationSrv, LOGGER) {
        try {
            isRunning.set(true)
            startTime = System.currentTimeMillis()
            runQueries(TypeQL.parseQueries<TypeQLQuery>(queries).toList())
        } catch (e: Exception) {
            collectEmptyLine()
            collectMessage(ERROR, ERROR_ + e.message)
        } finally {
            endTime = System.currentTimeMillis()
            isRunning.set(false)
            responses.add(Response.Done)
            var isConsumed: Boolean
            if (!hasStopSignal.state) {
                do {
                    isConsumed = consumerLatch.count == 0L
                    if (!isConsumed) delay(COUNT_DOWN_LATCH_PERIOD_MS)
                } while (!isConsumed && !hasStopSignal.state)
            }
            onComplete()
        }
    }

private fun runQueries(queries: List<TypeQLQuery>) = queries.forEach { query ->
        if (hasStopSignal.state) return@forEach
        when (query) {
            is TypeQLDefine -> runDefineQuery(query)
            is TypeQLUndefine -> runUndefineQuery(query)
            is TypeQLDelete -> runDeleteQuery(query)
            is TypeQLInsert -> runInsertQuery(query)
            is TypeQLUpdate -> runUpdateQuery(query)
            is TypeQLGet -> runGetQuery(query)
            is TypeQLGet.Aggregate -> runGetAggregateQuery(query)
            is TypeQLGet.Group -> runGetGroupQuery(query)
            is TypeQLGet.Group.Aggregate -> runGetGroupAggregateQuery(query)
            is TypeQLFetch -> runFetchQuery(query)
        else -> throw IllegalStateException("Unrecognised TypeQL query")
        }
    }

private fun runDefineQuery(query: TypeQLDefine) = runUnitQuery(
        name = DEFINE_QUERY,
        successMsg = DEFINE_QUERY_SUCCESS,
        queryStr = query.toString()
    ) { transaction.query().define(query).resolve() }

private fun runUndefineQuery(query: TypeQLUndefine) = runUnitQuery(
        name = UNDEFINE_QUERY,
        successMsg = UNDEFINE_QUERY_SUCCESS,
        queryStr = query.toString()
    ) { transaction.query().undefine(query).resolve() }

private fun runDeleteQuery(query: TypeQLDelete) = runUnitQuery(
        name = DELETE_QUERY,
        successMsg = DELETE_QUERY_SUCCESS,
        queryStr = query.toString()
    ) { transaction.query().delete(query).resolve() }

private fun runInsertQuery(query: TypeQLInsert) = runStreamingQuery(
        name = INSERT_QUERY,
        successMsg = INSERT_QUERY_SUCCESS,
        noResultMsg = INSERT_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.ConceptMaps(INSERT)
    ) { transaction.query().insert(query, transactionState.defaultTypeDBOptions().prefetch(true)) }

private fun runUpdateQuery(query: TypeQLUpdate) = runStreamingQuery(
        name = UPDATE_QUERY,
        successMsg = UPDATE_QUERY_SUCCESS,
        noResultMsg = UPDATE_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.ConceptMaps(UPDATE)
    ) { transaction.query().update(query, transactionState.defaultTypeDBOptions().prefetch(true)) }

private fun runGetQuery(query: TypeQLGet) = runStreamingQuery(
        name = GET_QUERY,
        successMsg = GET_QUERY_SUCCESS,
        noResultMsg = GET_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.ConceptMaps(GET)
    ) {
        if (query.modifiers().limit().isPresent) {
            transaction.query().get(query)
        } else {
            val queryWithLimit = TypeQLGet.Limited(query, preferenceSrv.getQueryLimit)
            transaction.query().get(queryWithLimit)
        }
    }

private fun runGetAggregateQuery(query: TypeQLGet.Aggregate) {
        printQueryStart(GET_AGGREGATE_QUERY, query.toString())
        val result = transaction.query().get(query).resolve().orElse(null)
        collectEmptyLine()
        collectMessage(SUCCESS, RESULT_ + GET_AGGREGATE_QUERY_SUCCESS)
        responses.put(Response.Value(result))
    }

private fun runGetGroupQuery(query: TypeQLGet.Group) = runStreamingQuery(
        name = GET_GROUP_QUERY,
        successMsg = GET_GROUP_QUERY_SUCCESS,
        noResultMsg = GET_GROUP_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.ConceptMapGroups()
    ) { transaction.query().get(query) }

private fun runGetGroupAggregateQuery(query: TypeQLGet.Group.Aggregate) = runStreamingQuery(
        name = GET_GROUP_AGGREGATE_QUERY,
        successMsg = GET_GROUP_AGGREGATE_QUERY_SUCCESS,
        noResultMsg = GET_GROUP_AGGREGATE_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.ValueGroups()
    ) { transaction.query().get(query) }

private fun runFetchQuery(query: TypeQLFetch) = runStreamingQuery(
        name = FETCH_QUERY,
        successMsg = FETCH_QUERY_SUCCESS,
        noResultMsg = FETCH_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.JSONs()
    ) {
        if (query.modifiers().limit().isPresent) {
            transaction.query().fetch(query)
        } else {
            val queryWithLimit = TypeQLFetch.Limited(query, preferenceSrv.getQueryLimit)
            transaction.query().fetch(queryWithLimit)
        }
    }

private fun runUnitQuery(name: String, successMsg: String, queryStr: String, queryFn: () -> Unit) {
        printQueryStart(name, queryStr)
        queryFn()
        collectEmptyLine()
        collectMessage(SUCCESS, RESULT_ + successMsg)
    }

private fun <T : Any> runStreamingQuery(
        name: String,
        successMsg: String,
        noResultMsg: String,
        queryStr: String,
        stream: Response.Stream<T>,
        queryFn: () -> Stream<T>
) {
        printQueryStart(name, queryStr)
        collectResponseStream(queryFn(), successMsg, noResultMsg, stream)
    }

private fun printQueryStart(name: String, queryStr: String) {
        collectEmptyLine()
        collectMessage(INFO, RUNNING_ + name)
        collectMessage(TYPEQL, queryStr)
    }

private fun <T : Any> collectResponseStream(
        results: Stream<T>,
        successMsg: String,
        noResultMsg: String,
        stream: Response.Stream<T>
) {
        var started = false
        var error = false
        try {
            collectEmptyLine()
            results.peek {
                if (started) return@peek
                collectMessage(SUCCESS, RESULT_ + successMsg)
                responses.put(stream)
                started = true
            }.forEach {
                if (hasStopSignal.state) return@forEach
                stream.queue.put(Either.first(it))
            }
        } catch (e: Exception) {
            collectMessage(ERROR, ERROR_ + e.message)
            error = true
        } finally {
            if (started) stream.queue.put(Either.second(Response.Done))
            if (error || hasStopSignal.state) collectMessage(ERROR, TERMINATED)
            else if (started) collectMessage(INFO, COMPLETED)
            else collectMessage(SUCCESS, RESULT_ + noResultMsg)
        }
    }

    fun close() {
        hasStopSignal.set(true)
        onClose.forEach { it() }
    }
}
 */

/*
class QueryRunner(
    val transactionState: TransactionState, // TODO: restrict in the future, when TypeDB 3.0 answers return complete info
    private val notificationSrv: NotificationService,
    private val preferenceSrv: PreferenceService,
    private val queries: String,
    private val onComplete: () -> Unit
) {

    sealed class Response {

        object Done : Response()

        data class Message(val type: Type, val text: String) : Response() {
            enum class Type { INFO, SUCCESS, ERROR, TYPEQL }
        }

        data class Value(val value: com.typedb.driver.api.concept.value.Value) : Response()

        sealed class Stream<T> : Response() {

        val queue = LinkedBlockingQueue<Either<T, Done>>()

        class JSONs : Stream<JSON>()
        class ConceptRows : Stream<ConceptRow>()
    }
}

    companion object {
        const val RESULT_ = "## Result> "
        const val ERROR_ = "## Error> "
        const val RUNNING_ = "## Running> "
        const val COMPLETED = "## Completed"
        const val TERMINATED = "## Terminated"
        const val QUERY = "Query:"
        const val QUERY_SUCCESS = "Success."
        const val QUERY_NO_RESULT = "Query returned no results."
        const val DEFINE_QUERY = "Define query:"
        const val DEFINE_QUERY_SUCCESS = "Define query successfully defined new types in the schema."
        const val UNDEFINE_QUERY = "Undefine query:"
        const val UNDEFINE_QUERY_SUCCESS = "Undefine query successfully undefined types in the schema."
        const val DELETE_QUERY = "Delete query:"
        const val DELETE_QUERY_SUCCESS = "Delete query successfully deleted things from the database."
        const val INSERT_QUERY = "Insert query:"
        const val INSERT_QUERY_SUCCESS = "Insert query successfully inserted new things to the database:"
        const val INSERT_QUERY_NO_RESULT = "Insert query did not insert any new thing to the database."
        const val UPDATE_QUERY = "Update query:"
        const val UPDATE_QUERY_SUCCESS = "Update query successfully updated things in the databases:"
        const val UPDATE_QUERY_NO_RESULT = "Update query did not update any thing in the databases."
        const val GET_QUERY = "Get query:"
        const val GET_QUERY_SUCCESS = "Get query successfully matched concepts in the database:"
        const val GET_QUERY_NO_RESULT = "Get query did not match any concepts in the database."
        const val GET_AGGREGATE_QUERY = "Get Aggregate query:"
        const val GET_AGGREGATE_QUERY_SUCCESS = "Get Aggregate query successfully calculated:"
        const val GET_GROUP_QUERY = "Get Group query:"
        const val GET_GROUP_QUERY_SUCCESS = "Get Group query successfully matched concept groups in the database:"
        const val GET_GROUP_QUERY_NO_RESULT = "Get Group query did not match any concept groups in the database."
        const val GET_GROUP_AGGREGATE_QUERY = "Get Group Aggregate query:"
        const val GET_GROUP_AGGREGATE_QUERY_SUCCESS =
            "Get Group Aggregate query successfully aggregated matched concept groups in the database:"
        const val GET_GROUP_AGGREGATE_QUERY_NO_RESULT =
            "Get Group Aggregate query did not match any concept groups to aggregate in the database."
        const val FETCH_QUERY = "Fetch query:"
        const val FETCH_QUERY_SUCCESS = "Fetch query successfully retrieved data from the database:"
        const val FETCH_QUERY_NO_RESULT = "Fetch query did not retrieve any data from the database."

        private const val COUNT_DOWN_LATCH_PERIOD_MS: Long = 50
    private val LOGGER = KotlinLogging.logger {}
    }

    var startTime: Long? = null
    var endTime: Long? = null
        val responses = LinkedBlockingQueue<Response>()
    val isConsumed: Boolean get() = consumerLatch.count == 0L
    val isRunning = AtomicBoolean(false)
private val consumerLatch = CountDownLatch(1)
private val coroutines = CoroutineScope(Dispatchers.Default)
private val hasStopSignal get() = transactionState.hasStopSignal
private val transaction get() = transactionState.transaction!!
private val onClose = LinkedBlockingQueue<() -> Unit>()

    fun onClose(function: () -> Unit) = onClose.put(function)

    fun setConsumed() = consumerLatch.countDown()

private fun collectEmptyLine() = collectMessage(INFO, "")

private fun collectMessage(type: Response.Message.Type, string: String) {
        responses.put(Response.Message(type, string))
    }

    internal fun launch() = coroutines.launchAndHandle(notificationSrv, LOGGER) {
        try {
            isRunning.set(true)
            startTime = System.currentTimeMillis()
            runQuery(queries)
//            runQueries(TypeQL.parseQueries<TypeQLQuery>(queries).collect(Collectors.toList()))
        } catch (e: Exception) {
            collectEmptyLine()
            collectMessage(ERROR, ERROR_ + e.message)
        } finally {
            endTime = System.currentTimeMillis()
            isRunning.set(false)
            responses.add(Response.Done)
            var isConsumed: Boolean
            if (!hasStopSignal) {
                do {
                    isConsumed = consumerLatch.count == 0L
                    if (!isConsumed) delay(COUNT_DOWN_LATCH_PERIOD_MS)
                } while (!isConsumed && !hasStopSignal)
            }
            onComplete()
        }
    }

private fun runQuery(query: String) {
        if (hasStopSignal) return

        collectEmptyLine()
        collectMessage(INFO, RUNNING_)
        collectMessage(TYPEQL, query)

        val answer = transaction.query(query).resolve()

        if (answer.isOk) {
            collectEmptyLine()
            collectMessage(SUCCESS, RESULT_ + QUERY_SUCCESS)
            return
        } else if (answer.isConceptRows) {
            val streamRaw = answer.asConceptRows().stream()
            val stream = Response.Stream.ConceptRows()

            var started = false
            var error = false
            try {
                collectEmptyLine()
                streamRaw.peek {
                    if (started) return@peek
                    collectMessage(SUCCESS, RESULT_ + QUERY_SUCCESS)
                    responses.put(stream)
                    started = true
                }.forEach {
                    if (hasStopSignal) return@forEach
                    stream.queue.put(Either.first(it))
                }
            } catch (e: Exception) {
                collectMessage(ERROR, ERROR_ + e.message)
                error = true
            } finally {
                if (started) stream.queue.put(Either.second(Response.Done))
                if (error || hasStopSignal) collectMessage(ERROR, TERMINATED)
                else if (started) collectMessage(INFO, COMPLETED)
                else collectMessage(SUCCESS, RESULT_ + QUERY_NO_RESULT)
            }
        } else if (answer.isConceptDocuments) {
            val streamRaw = answer.asConceptDocuments().stream()
            val stream = Response.Stream.JSONs()

            var started = false
            var error = false
            try {
                collectEmptyLine()
                streamRaw.peek {
                    if (started) return@peek
                    collectMessage(SUCCESS, RESULT_ + QUERY_SUCCESS)
                    responses.put(stream)
                    started = true
                }.forEach {
                    if (hasStopSignal) return@forEach
                    stream.queue.put(Either.first(it))
                }
            } catch (e: Exception) {
                collectMessage(ERROR, ERROR_ + e.message)
                error = true
            } finally {
                if (started) stream.queue.put(Either.second(Response.Done))
                if (error || hasStopSignal) collectMessage(ERROR, TERMINATED)
                else if (started) collectMessage(INFO, COMPLETED)
                else collectMessage(SUCCESS, RESULT_ + QUERY_SUCCESS)
            }
        } else throw IllegalArgumentException()
    }

private fun runDefineQuery(query: TypeQLDefine) = runUnitQuery(
        name = DEFINE_QUERY,
        successMsg = DEFINE_QUERY_SUCCESS,
        queryStr = query.toString()
    ) { transaction.query(query.toString()).resolve() }

private fun runUndefineQuery(query: TypeQLUndefine) = runUnitQuery(
        name = UNDEFINE_QUERY,
        successMsg = UNDEFINE_QUERY_SUCCESS,
        queryStr = query.toString()
    ) { transaction.query(query.toString()).resolve() }

private fun runDeleteQuery(query: TypeQLDelete) = runUnitQuery(
        name = DELETE_QUERY,
        successMsg = DELETE_QUERY_SUCCESS,
        queryStr = query.toString()
    ) { transaction.query(query.toString()).resolve() }

private fun runInsertQuery(query: TypeQLInsert) = runStreamingQuery(
        name = INSERT_QUERY,
        successMsg = INSERT_QUERY_SUCCESS,
        noResultMsg = INSERT_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.ConceptRows()
    ) { transaction.query(query.toString()).resolve().asConceptRows().stream() } // TODO: prefetch = true option

private fun runUpdateQuery(query: TypeQLUpdate) = runStreamingQuery(
        name = UPDATE_QUERY,
        successMsg = UPDATE_QUERY_SUCCESS,
        noResultMsg = UPDATE_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.ConceptRows()
    ) { transaction.query(query.toString()).resolve().asConceptRows().stream() }

private fun runGetQuery(query: TypeQLGet) = runStreamingQuery(
        name = GET_QUERY,
        successMsg = GET_QUERY_SUCCESS,
        noResultMsg = GET_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.ConceptRows()
    ) {
//        if (query.modifiers().limit().isPresent) {
        transaction.query(query.toString()).resolve().asConceptRows().stream()
//        } else {
//            val queryWithLimit = TypeQLGet.Limited(query, preferenceSrv.getQueryLimit)
//            transaction.query().get(queryWithLimit)
//        }
    }

private fun runGetAggregateQuery(query: TypeQLGet.Aggregate) {
        collectMessage(INFO, "runGetAggregateQuery: unsupported")
//        printQueryStart(GET_AGGREGATE_QUERY, query.toString())
//        val result = transaction.query(query.toString()).resolve()
//        collectEmptyLine()
//        collectMessage(SUCCESS, RESULT_ + GET_AGGREGATE_QUERY_SUCCESS)
//        responses.put(Response.Value(result))
    }

private fun runFetchQuery(query: TypeQLFetch) = runStreamingQuery(
        name = FETCH_QUERY,
        successMsg = FETCH_QUERY_SUCCESS,
        noResultMsg = FETCH_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.JSONs()
    ) {
//        if (query.modifiers().limit().isPresent) {
        transaction.query(query.toString()).resolve().asConceptDocuments().stream()
//        } else {
//            val queryWithLimit = TypeQLFetch.Limited(query, preferenceSrv.getQueryLimit)
//            transaction.query().fetch(queryWithLimit)
//        }
    }

private fun runUnitQuery(name: String, successMsg: String, queryStr: String, queryFn: () -> Unit) {
        printQueryStart(name, queryStr)
        queryFn()
        collectEmptyLine()
        collectMessage(SUCCESS, RESULT_ + successMsg)
    }

private fun <T : Any> runStreamingQuery(
        name: String,
        successMsg: String,
        noResultMsg: String,
        queryStr: String,
        stream: Response.Stream<T>,
        queryFn: () -> Stream<T>
) {
        printQueryStart(name, queryStr)
        collectResponseStream(queryFn(), successMsg, noResultMsg, stream)
    }

private fun printQueryStart(name: String, queryStr: String) {
        collectEmptyLine()
        collectMessage(INFO, RUNNING_ + name)
        collectMessage(TYPEQL, queryStr)
    }

private fun <T : Any> collectResponseStream(
        results: Stream<T>,
        successMsg: String,
        noResultMsg: String,
        stream: Response.Stream<T>
) {
        var started = false
        var error = false
        try {
            collectEmptyLine()
            results.peek {
                if (started) return@peek
                collectMessage(SUCCESS, RESULT_ + successMsg)
                responses.put(stream)
                started = true
            }.forEach {
                if (hasStopSignal) return@forEach
                stream.queue.put(Either.first(it))
            }
        } catch (e: Exception) {
            collectMessage(ERROR, ERROR_ + e.message)
            error = true
        } finally {
            if (started) stream.queue.put(Either.second(Response.Done))
            if (error || hasStopSignal) collectMessage(ERROR, TERMINATED)
            else if (started) collectMessage(INFO, COMPLETED)
            else collectMessage(SUCCESS, RESULT_ + noResultMsg)
        }
    }

    fun close() {
        transactionState.sendStopSignal()
        onClose.forEach { it() }
    }
}
 */
