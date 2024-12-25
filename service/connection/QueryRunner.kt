/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.connection

import com.typedb.studio.service.common.NotificationService
import com.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.typedb.studio.service.common.PreferenceService
import com.typedb.studio.service.connection.QueryRunner.Response.Message.Type.ERROR
import com.typedb.studio.service.connection.QueryRunner.Response.Message.Type.INFO
import com.typedb.studio.service.connection.QueryRunner.Response.Message.Type.SUCCESS
import com.typedb.studio.service.connection.QueryRunner.Response.Message.Type.TYPEQL
import com.typedb.studio.service.connection.QueryRunner.Response.Stream.ConceptRows.Source.GET
import com.typedb.studio.service.connection.QueryRunner.Response.Stream.ConceptRows.Source.INSERT
import com.typedb.studio.service.connection.QueryRunner.Response.Stream.ConceptRows.Source.UPDATE
import com.typedb.common.collection.Either
import com.typedb.driver.api.answer.ConceptRow
import com.typedb.driver.api.answer.JSON
import com.typeql.lang.TypeQL
import com.typeql.lang.query.TypeQLDefine
import com.typeql.lang.query.TypeQLDelete
import com.typeql.lang.query.TypeQLFetch
import com.typeql.lang.query.TypeQLGet
import com.typeql.lang.query.TypeQLInsert
import com.typeql.lang.query.TypeQLQuery
import com.typeql.lang.query.TypeQLUndefine
import com.typeql.lang.query.TypeQLUpdate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mu.KotlinLogging

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
            class ConceptRows constructor(val source: Source) : Stream<ConceptRow>() {
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
            runQueries(queries)
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

    private fun runQueries(queries: List<TypeQLQuery>) = queries.forEach { query ->
        if (hasStopSignal) return@forEach
        when (query) {
            is TypeQLDefine -> runDefineQuery(query)
            is TypeQLUndefine -> runUndefineQuery(query)
            is TypeQLDelete -> runDeleteQuery(query)
            is TypeQLInsert -> runInsertQuery(query)
            is TypeQLUpdate -> runUpdateQuery(query)
            is TypeQLGet -> runGetQuery(query)
            is TypeQLGet.Aggregate -> runGetAggregateQuery(query)
//            is TypeQLGet.Group -> runGetGroupQuery(query)
//            is TypeQLGet.Group.Aggregate -> runGetGroupAggregateQuery(query)
            is TypeQLFetch -> runFetchQuery(query)
            else -> throw IllegalStateException("Unrecognised TypeQL query")
        }
    }

    private fun runQueries(queries: String) {
        if (hasStopSignal) return
        runStreamingQuery(
            name = GET_QUERY,
            successMsg = GET_QUERY_SUCCESS,
            noResultMsg = GET_QUERY_NO_RESULT,
            queryStr = queries,
            stream = Response.Stream.ConceptRows(GET)
        ) {
            val answer = transaction.query(queries).resolve()
            if (answer.isOk) return@runStreamingQuery Stream.empty()
            else if (answer.isConceptRows) return@runStreamingQuery answer.asConceptRows().stream()
//            else if (answer.isConceptDocuments) return@runStreamingQuery answer.asConceptDocuments().stream()
            else throw IllegalArgumentException()
        }
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
        stream = Response.Stream.ConceptRows(INSERT)
    ) { transaction.query(query.toString()).resolve().asConceptRows().stream() } // TODO: prefetch = true option

    private fun runUpdateQuery(query: TypeQLUpdate) = runStreamingQuery(
        name = UPDATE_QUERY,
        successMsg = UPDATE_QUERY_SUCCESS,
        noResultMsg = UPDATE_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.ConceptRows(UPDATE)
    ) { transaction.query(query.toString()).resolve().asConceptRows().stream() }

    private fun runGetQuery(query: TypeQLGet) = runStreamingQuery(
        name = GET_QUERY,
        successMsg = GET_QUERY_SUCCESS,
        noResultMsg = GET_QUERY_NO_RESULT,
        queryStr = query.toString(),
        stream = Response.Stream.ConceptRows(GET)
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
