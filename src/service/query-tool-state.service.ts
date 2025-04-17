/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import { Injectable } from "@angular/core";
import { FormControl } from "@angular/forms";
import { INTERNAL_ERROR } from "../framework/util/strings";
import { Concept, ConceptDocument, ConceptRow, DriverAction, DriverState, QueryResponse, Value } from "./driver-state.service";

export type OutputType = "raw" | "log" | "table" | "structure";

@Injectable({
    providedIn: "root",
})
export class QueryToolState {

    queryControl = new FormControl("", {nonNullable: true});
    outputTypeControl = new FormControl("log" as OutputType, { nonNullable: true });
    outputTypes: OutputType[] = ["log", "table", "structure", "raw"];
    readonly history = new HistoryWindowState(this.driver);
    readonly logOutput = new LogOutputState();
    readonly rawOutput = new RawOutputState();
    answersOutputEnabled = true;

    constructor(private driver: DriverState) {}

    // TODO: LIMIT 1000 by default, configurable

    runQuery() {
        const query = this.queryControl.value;
        this.initialiseOutput(query);
        this.driver.query(query).subscribe((res) => {
            this.outputQueryResponse(res);
        });
    }

    private initialiseOutput(query: string) {
        this.logOutput.clear();
        this.rawOutput.clear();

        this.logOutput.appendLines(RUNNING, query);
    }

    private outputQueryResponse(res: QueryResponse) {
        if (this.answersOutputEnabled) this.outputQueryResponseWithAnswers(res);
        else this.outputQueryResponseNoAnswers();
    }

    private outputQueryResponseWithAnswers(res: QueryResponse) {
        this.logOutput.appendBlankLine();
        this.logOutput.appendQueryResult(res);

        this.rawOutput.push(JSON.stringify(res, null, 2));
    }

    private outputQueryResponseNoAnswers() {
        this.logOutput.appendBlankLine();
        this.logOutput.appendLines(`${RESULT}${SUCCESS}`);

        this.rawOutput.push(SUCCESS_RAW);
    }
}

export class HistoryWindowState {

    readonly entries: DriverAction[] = [];

    constructor(private driver: DriverState) {
        this.driver.actions$.subscribe((action) => {
            this.entries.unshift(action);
        });
    }
}

const RUNNING = `## Running> `;
const RESULT = `## Result> `;
const SUCCESS = `Success`;
const SUCCESS_RAW = `success`;
const TABLE_INDENT = "   ";
const CONTENT_INDENT = "    ";
const TABLE_DASHES = 7;

export class LogOutputState {

    control = new FormControl("", {nonNullable: true});

    constructor() {}

    appendLines(...lines: string[]) {
        this.control.patchValue(`${this.control.value}${lines.join(`\n`)}\n`);
    }

    appendBlankLine() {
        this.appendLines(``);
    }

    appendQueryResult(res: QueryResponse) {
        this.appendLines(`${RESULT}${SUCCESS}`);
        this.appendBlankLine();

        switch (res.answerType) {
            case "ok": {
                this.appendLines(`Finished ${res.queryType} query.`);
                break;
            }
            case "conceptRows": {
                this.appendLines(`Finished ${res.queryType} query compilation and validation...`);
                if (res.queryType === "write") this.appendLines(`Finished writes. Printing rows...`);
                else this.appendLines(`Printing rows...`);

                if (res.answers.length) {
                    const varNames = Object.keys(res.answers[0]);
                    if (varNames.length) {
                        res.answers.forEach(((x, idx) => this.appendConceptRow(x, idx === 0)));
                    } else this.appendLines(`No columns to show.`);
                }

                this.appendLines(`Finished. Total rows: ${res.answers.length}`);
                break;
            }
            case "conceptDocuments": {
                this.appendLines(`Finished ${res.queryType} query compilation and validation...`);
                if (res.queryType === "write") this.appendLines(`Finished writes. Printing documents...`);
                else this.appendLines(`Printing documents...`);
                res.answers.forEach(x => this.appendConceptDocument(x));
                this.appendLines(`Finished. Total documents: ${res.answers.length}`);
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
            `\$${columnName}${" ".repeat(variableColumnWidth - columnName.length + 1)}| ${this.conceptDisplayString(row[columnName])}`
        );
        const content = contents.join("\n");
        return `${this.indent(CONTENT_INDENT, content)}\n${this.lineDashSeparator(variableColumnWidth)}`;
    }

    private conceptDisplayString(concept: Concept | undefined): string {
        if (!concept) return "";

        switch (concept.kind) {
            case "entityType":
                return this.formatType(concept.label);
            case "relationType":
                return this.formatType(concept.label);
            case "roleType":
                return this.formatType(concept.label);
            case "attributeType":
                return this.formatType(concept.label);
            case "entity":
                return `${concept.type ? this.formatIsa(concept.type.label) : ""}, ${this.formatIid(concept.iid)}`;
            case "relation":
                return `${concept.type ? this.formatIsa(concept.type.label) : ""}, ${this.formatIid(concept.iid)}`;
            case "attribute":
                return `${concept.type ? this.formatIsa(concept.type.label) : ""} ${this.formatValue(concept.value)}`;
            case "value":
                return this.formatValue(concept);
        }
    }

    // TODO: syntax highlighting
    private formatType(label: string): string {
        return label;
        // return `\x1b[95m${label}\x1b[0m`;
    }

    private formatIid(iid: string): string {
        return `${this.formatKeyword("iid")} ${iid}`;
    }

    private formatIsa(label: string): string {
        return `${this.formatKeyword("isa")} ${label}`;
        // return `${this.formatKeyword("isa")} \x1b[95m${label}\x1b[0m`;
    }

    private formatValue(value: Value): string {
        return value.toString();
    }

    private formatKeyword(keyword: string): string {
        return keyword;
        // return `\x1b[94m${keyword}\x1b[0m`;
    }

    private indent(indentation: string, string: string): string {
        return string.split('\n').map(s => `${indentation}${s}`).join('\n');
    }

    private lineDashSeparator(additionalDashesNum: number): string {
        return this.indent(TABLE_INDENT, "-".repeat(TABLE_DASHES + additionalDashesNum));
    }

    clear() {
        this.control.patchValue(``);
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







/*
const QUERY_TYPE_TEMPLATE: &'static str = "<QUERY TYPE>";
const QUERY_COMPILATION_SUCCESS: &'static str = "Finished <QUERY TYPE> query validation and compilation...";
const QUERY_WRITE_FINISHED_STREAMING_ROWS: &'static str = "Finished writes. Streaming rows...";
const QUERY_WRITE_FINISHED_STREAMING_DOCUMENTS: &'static str = "Finished writes. Streaming rows...";
const QUERY_STREAMING_ROWS: &'static str = "Streaming rows...";
const QUERY_STREAMING_DOCUMENTS: &'static str = "Streaming documents...";
const ANSWER_COUNT_TEMPLATE: &'static str = "<ANSWER COUNT>";
const QUERY_FINISHED_COUNT: &'static str = "Finished. Total answers: <ANSWER COUNT>";

fn query_type_str(query_type: QueryType) -> &'static str {
    match query_type {
        QueryType::ReadQuery => "read",
        QueryType::WriteQuery => "write",
        QueryType::SchemaQuery => "schema",
    }
}

fn execute_query(context: &mut ConsoleContext, query: String, logging: bool) -> Result<(), typedb_driver::Error> {
    let (transaction, has_writes) =
        context.transaction.take().expect("Transaction query run without active transaction.");
    let (transaction, result, write_succes) = context.background_runtime.run(async move {
        let result = transaction.query(query).await;
        if logging {
            // note: print results in the async block so we don't have to collect first
            match result {
                Ok(answer) => {
                    match answer {
                        QueryAnswer::Ok(query_type) => {
                            println!("Finished {} query.", query_type_str(query_type));
                            let write_query = !matches!(query_type, QueryType::ReadQuery);
                            (transaction, Ok(()), write_query)
                        }
                        QueryAnswer::ConceptRowStream(header, mut rows_stream) => {
                            println!(
                                "{}",
                                QUERY_COMPILATION_SUCCESS
                                    .replace(QUERY_TYPE_TEMPLATE, query_type_str(header.query_type))
                            );
                            let write_query = if matches!(header.query_type, QueryType::WriteQuery) {
                                println!("{}", QUERY_WRITE_FINISHED_STREAMING_ROWS);
                                true
                            } else {
                                println!("{}", QUERY_STREAMING_ROWS);
                                false
                            };
                            let has_columns = !header.column_names.is_empty();
                            if !has_columns {
                                println!("\nNo columns to show.\n");
                            }
                            let mut count = 0;
                            while let Some(result) = rows_stream.next().await {
                                match result {
                                    Ok(row) => {
                                        if has_columns {
                                            print_row(row, count == 0);
                                        }
                                        count += 1;
                                    }
                                    Err(err) => return (transaction, Err(err), false),
                                }
                            }
                            println!("{}", QUERY_FINISHED_COUNT.replace(ANSWER_COUNT_TEMPLATE, &count.to_string()));
                            (transaction, Ok(()), write_query)
                        }
                        QueryAnswer::ConceptDocumentStream(header, mut documents_stream) => {
                            println!(
                                "{}",
                                QUERY_COMPILATION_SUCCESS
                                    .replace(QUERY_TYPE_TEMPLATE, query_type_str(header.query_type))
                            );
                            let write_query = if matches!(header.query_type, QueryType::WriteQuery) {
                                println!("{}", QUERY_WRITE_FINISHED_STREAMING_DOCUMENTS);
                                true
                            } else {
                                println!("{}", QUERY_STREAMING_DOCUMENTS);
                                false
                            };

                            let mut count = 0;
                            while let Some(result) = documents_stream.next().await {
                                match result {
                                    Ok(document) => {
                                        print_document(document);
                                        count += 1;
                                    }
                                    // Note: we don't necessarily have to terminate the transaction when we get an error
                                    // but the signalling isn't in place to do this canonically either!
                                    Err(err) => return (transaction, Err(err), false),
                                }
                            }
                            println!("{}", QUERY_FINISHED_COUNT.replace(ANSWER_COUNT_TEMPLATE, &count.to_string()));
                            (transaction, Ok(()), write_query)
                        }
                    }
                }
                Err(err) => (transaction, Err(err), false),
            }
        } else {
            match result {
                Ok(answer) => {
                    let write_query = !matches!(answer.get_query_type(), QueryType::ReadQuery);
                    (transaction, Ok(()), write_query)
                }
                Err(err) => (transaction, Err(err), false),
            }
        }
    });
    if !transaction.is_open() {
        // drop transaction
        // TODO: would be better to return a repl END type. In other places, return repl START(repl)
        context.repl_stack.pop();
    } else {
        context.transaction = Some((transaction, has_writes || write_succes));
    };
    result
}
 */