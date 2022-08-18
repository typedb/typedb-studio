/*
 * Copyright (C) 2022 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.state.connection

import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.answer.ConceptMapGroup
import com.vaticle.typedb.client.api.answer.NumericGroup
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Message.Type.ERROR
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Message.Type.INFO
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Message.Type.SUCCESS
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Message.Type.TYPEQL
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Stream.ConceptMaps.Source.INSERT
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Stream.ConceptMaps.Source.MATCH
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Stream.ConceptMaps.Source.UPDATE
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLDefine
import com.vaticle.typeql.lang.query.TypeQLDelete
import com.vaticle.typeql.lang.query.TypeQLInsert
import com.vaticle.typeql.lang.query.TypeQLMatch
import com.vaticle.typeql.lang.query.TypeQLQuery
import com.vaticle.typeql.lang.query.TypeQLUndefine
import com.vaticle.typeql.lang.query.TypeQLUpdate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream
import kotlin.streams.toList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mu.KotlinLogging

class QueryRunner constructor(
    val transactionState: TransactionState, // TODO: restrict in the future, when TypeDB 3.0 answers return complete info
    private val notificationMgr: NotificationManager,
    private val queries: String,
    private val onComplete: () -> Unit
) {

    sealed class Response {

        object Done : Response()

        data class Message(val type: Type, val text: String) : Response() {
            enum class Type { INFO, SUCCESS, ERROR, TYPEQL }
        }

        data class Numeric(val value: com.vaticle.typedb.client.api.answer.Numeric) : Response()

        sealed class Stream<T> : Response() {

            val queue = LinkedBlockingQueue<Either<T, Done>>()

            class ConceptMapGroups : Stream<ConceptMapGroup>()
            class NumericGroups : Stream<NumericGroup>()
            class ConceptMaps constructor(val source: Source) : Stream<ConceptMap>() {
                enum class Source { INSERT, UPDATE, MATCH }
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
        const val MATCH_QUERY = "Match query:"
        const val MATCH_QUERY_SUCCESS = "Match query successfully matched concepts in the database:"
        const val MATCH_QUERY_NO_RESULT = "Match query did not match any concepts in the database."
        const val MATCH_AGGREGATE_QUERY = "Match Aggregate query:"
        const val MATCH_AGGREGATE_QUERY_SUCCESS = "Match Aggregate query successfully calculated:"
        const val MATCH_GROUP_QUERY = "Match Group query:"
        const val MATCH_GROUP_QUERY_SUCCESS = "Match Group query successfully matched concept groups in the database:"
        const val MATCH_GROUP_QUERY_NO_RESULT = "Match Group query did not match any concept groups in the database."
        const val MATCH_GROUP_AGGREGATE_QUERY = "Match Group Aggregate query:"
        const val MATCH_GROUP_AGGREGATE_QUERY_SUCCESS =
            "Match Group Aggregate query successfully aggregated matched concept groups in the database:"
        const val MATCH_GROUP_AGGREGATE_QUERY_NO_RESULT =
            "Match Group Aggregate query did not match any concept groups to aggregate in the database."

        private const val COUNT_DOWN_LATCH_PERIOD_MS: Long = 50
        private val LOGGER = KotlinLogging.logger {}
    }

    var startTime: Long? = null
    var endTime: Long? = null
    val responses = LinkedBlockingQueue<Response>()
    val isConsumed: Boolean get() = consumerLatch.count == 0L
    val isRunning = AtomicBoolean(false)
    private val consumerLatch = CountDownLatch(1)
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val hasStopSignal get() = transactionState.hasStopSignalAtomic
    private val transaction get() = transactionState.transaction!!
    private val onClose = LinkedBlockingQueue<() -> Unit>()

    fun onClose(function: () -> Unit) = onClose.put(function)

    fun setConsumed() = consumerLatch.countDown()

    private fun collectEmptyLine() = collectMessage(INFO, "")

    private fun collectMessage(type: Response.Message.Type, string: String) {
        responses.put(Response.Message(type, string))
    }

    internal fun launch() = coroutineScope.launchAndHandle(notificationMgr, LOGGER) {
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
            if (!hasStopSignal.atomic.get()) {
                do {
                    isConsumed = consumerLatch.count == 0L
                    if (!isConsumed) delay(COUNT_DOWN_LATCH_PERIOD_MS)
                } while (!isConsumed && !hasStopSignal.atomic.get())
            }
            onComplete()
        }
    }

    private fun runQueries(queries: List<TypeQLQuery>) {
        queries.forEach { query ->
            if (hasStopSignal.atomic.get()) return@forEach
            when (query) {
                is TypeQLDefine -> runDefineQuery(query)
                is TypeQLUndefine -> runUndefineQuery(query)
                is TypeQLDelete -> runDeleteQuery(query)
                is TypeQLInsert -> runInsertQuery(query)
                is TypeQLUpdate -> runUpdateQuery(query)
                is TypeQLMatch -> runMatchQuery(query)
                is TypeQLMatch.Aggregate -> runMatchAggregateQuery(query)
                is TypeQLMatch.Group -> runMatchGroupQuery(query)
                is TypeQLMatch.Group.Aggregate -> runMatchGroupAggregateQuery(query)
                else -> throw IllegalStateException("Unrecognised TypeQL query")
            }
        }
    }

    private fun runDefineQuery(query: TypeQLDefine) {
        runUnitQuery(DEFINE_QUERY, DEFINE_QUERY_SUCCESS, query.toString()) {
            transaction.query().define(query).get()
        }
    }

    private fun runUndefineQuery(query: TypeQLUndefine) {
        runUnitQuery(UNDEFINE_QUERY, UNDEFINE_QUERY_SUCCESS, query.toString()) {
            transaction.query().undefine(query).get()
        }
    }

    private fun runDeleteQuery(query: TypeQLDelete) {
        runUnitQuery(DELETE_QUERY, DELETE_QUERY_SUCCESS, query.toString()) {
            transaction.query().delete(query).get()
        }
    }

    private fun runInsertQuery(query: TypeQLInsert) {
        runStreamingQuery(
            name = INSERT_QUERY,
            successMsg = INSERT_QUERY_SUCCESS,
            noResultMsg = INSERT_QUERY_NO_RESULT,
            queryStr = query.toString(),
            stream = Response.Stream.ConceptMaps(INSERT)
        ) { transaction.query().insert(query, transactionState.typeDBOptions().prefetch(true)) }
    }

    private fun runUpdateQuery(query: TypeQLUpdate) {
        runStreamingQuery(
            name = UPDATE_QUERY,
            successMsg = UPDATE_QUERY_SUCCESS,
            noResultMsg = UPDATE_QUERY_NO_RESULT,
            queryStr = query.toString(),
            stream = Response.Stream.ConceptMaps(UPDATE)
        ) { transaction.query().update(query, transactionState.typeDBOptions().prefetch(true)) }
    }

    private fun runMatchQuery(query: TypeQLMatch) {
        runStreamingQuery(
            name = MATCH_QUERY,
            successMsg = MATCH_QUERY_SUCCESS,
            noResultMsg = MATCH_QUERY_NO_RESULT,
            queryStr = query.toString(),
            stream = Response.Stream.ConceptMaps(MATCH)
        ) { transaction.query().match(query) }
    }

    private fun runMatchAggregateQuery(query: TypeQLMatch.Aggregate) {
        printQueryStart(MATCH_AGGREGATE_QUERY, query.toString())
        val result = transaction.query().match(query).get()
        collectEmptyLine()
        collectMessage(SUCCESS, RESULT_ + MATCH_AGGREGATE_QUERY_SUCCESS)
        responses.put(Response.Numeric(result))
    }

    private fun runMatchGroupQuery(query: TypeQLMatch.Group) {
        runStreamingQuery(
            name = MATCH_GROUP_QUERY,
            successMsg = MATCH_GROUP_QUERY_SUCCESS,
            noResultMsg = MATCH_GROUP_QUERY_NO_RESULT,
            queryStr = query.toString(),
            stream = Response.Stream.ConceptMapGroups()
        ) { transaction.query().match(query) }
    }

    private fun runMatchGroupAggregateQuery(query: TypeQLMatch.Group.Aggregate) {
        runStreamingQuery(
            name = MATCH_GROUP_AGGREGATE_QUERY,
            successMsg = MATCH_GROUP_AGGREGATE_QUERY_SUCCESS,
            noResultMsg = MATCH_GROUP_AGGREGATE_QUERY_NO_RESULT,
            queryStr = query.toString(),
            stream = Response.Stream.NumericGroups()
        ) { transaction.query().match(query) }
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
                if (hasStopSignal.atomic.get()) return@forEach
                stream.queue.put(Either.first(it))
            }
        } catch (e: Exception) {
            collectMessage(ERROR, ERROR_ + e.message)
            error = true
        } finally {
            if (started) stream.queue.put(Either.second(Response.Done))
            if (error || hasStopSignal.atomic.get()) collectMessage(ERROR, TERMINATED)
            else if (started) collectMessage(INFO, COMPLETED)
            else collectMessage(SUCCESS, RESULT_ + noResultMsg)
        }
    }

    fun close() {
        hasStopSignal.set(true)
        onClose.forEach { it() }
    }
}
