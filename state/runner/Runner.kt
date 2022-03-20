/*
 * Copyright (C) 2021 Vaticle
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

package com.vaticle.typedb.studio.state.runner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.studio.state.runner.Response.Log.Entry.Type.ERROR
import com.vaticle.typedb.studio.state.runner.Response.Log.Entry.Type.INFO
import com.vaticle.typedb.studio.state.runner.Response.Log.Entry.Type.SUCCESS
import com.vaticle.typedb.studio.state.runner.Response.Log.Entry.Type.TYPEQL
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLDefine
import com.vaticle.typeql.lang.query.TypeQLDelete
import com.vaticle.typeql.lang.query.TypeQLInsert
import com.vaticle.typeql.lang.query.TypeQLMatch
import com.vaticle.typeql.lang.query.TypeQLQuery
import com.vaticle.typeql.lang.query.TypeQLUndefine
import com.vaticle.typeql.lang.query.TypeQLUpdate
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.streams.toList
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTime::class)
class Runner(
    private val transaction: TypeDBTransaction,
    private val queries: String,
    private val hasStopSignal: AtomicBoolean
) {

    companion object {
        const val RESULT_ = "## Result> "
        const val ERROR_ = "## Error> "
        const val RUNNING_ = "## Running> "
        const val DEFINE_QUERY = "Define Query:"
        const val UNDEFINE_QUERY = "Undefine Query:"
        const val INSERT_QUERY = "Insert Query:"
        const val DELETE_QUERY = "Delete Query:"
        const val UPDATE_QUERY = "Update Query:"
        const val MATCH_QUERY = "Match Query:"
        const val DEFINE_QUERY_SUCCESS = "Define query successfully defined new types in the schema."
        const val UNDEFINE_QUERY_SUCCESS = "Undefine query successfully undefined types in the schema."
        const val DELETE_QUERY_SUCCESS = "Delete query successfully deleted things from the database."
        const val INSERT_QUERY_SUCCESS = "Insert query successfully inserted new things to the database:"
        const val INSERT_QUERY_NO_RESULT = "Insert query did not insert any new thing to the database."
        const val UPDATE_QUERY_SUCCESS = "Update query successfully updated things in the databases:"
        const val UPDATE_QUERY_NO_RESULT = "Update query did not update any thing in the databases."
        const val MATCH_QUERY_SUCCESS = "Match query successfully matched concepts in the database:"
        const val MATCH_QUERY_NO_RESULT = "Match query did not match any concepts in the database."

        private val RUNNING_INDICATOR_DELAY = Duration.Companion.seconds(3)
    }

    var isSaved by mutableStateOf(false)
    val response = ResponseManager()
    private val isRunning = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)
    private val onComplete = LinkedBlockingDeque<(Runner) -> Unit>()

    fun save() {
        isSaved = true
    }

    fun onComplete(function: (Runner) -> Unit) {
        onComplete.push(function)
    }

    internal fun launch() {
        isRunning.set(true)
        coroutineScope.launch { runningQueryIndicator() }
        coroutineScope.launch { runQueries() }
    }

    private suspend fun runningQueryIndicator() {
        var duration = RUNNING_INDICATOR_DELAY
        while (isRunning.get()) {
            delay(duration)
            if (!isRunning.get()) break
            val sinceLastResponse = System.currentTimeMillis() - response.log.lastResponse.get()
            if (sinceLastResponse >= RUNNING_INDICATOR_DELAY.inWholeMilliseconds) {
                response.log.collect(INFO, "...")
                duration = RUNNING_INDICATOR_DELAY
            } else {
                duration = RUNNING_INDICATOR_DELAY - Duration.milliseconds(sinceLastResponse)
            }
        }
    }

    private fun runQueries() {
        try {
            runQueries(TypeQL.parseQueries<TypeQLQuery>(queries).toList())
        } catch (e: Exception) {
            response.log.emptyLine()
            response.log.collect(ERROR, ERROR_ + e.message)
        } finally {
            isRunning.set(false)
            onComplete.forEach { it(this@Runner) }
        }
    }

    private fun runQueries(queries: List<TypeQLQuery>) {
        queries.forEach { query ->
            if (hasStopSignal.get()) return@forEach
            when (query) {
                is TypeQLDefine -> runDefineQuery(query)
                is TypeQLUndefine -> runUndefineQuery(query)
                is TypeQLDelete -> runDeleteQuery(query)
                is TypeQLInsert -> runInsertQuery(query)
                is TypeQLUpdate -> runUpdateQuery(query)
                is TypeQLMatch -> runMatchQuery(query)
                else -> throw IllegalStateException("Unrecognised TypeQL query")
            }
        }
    }

    private fun runDefineQuery(query: TypeQLDefine) {
        response.log.emptyLine()
        response.log.collect(INFO, RUNNING_ + DEFINE_QUERY)
        response.log.collect(TYPEQL, query.toString())
        transaction.query().define(query).get()
        response.log.emptyLine()
        response.log.collect(SUCCESS, RESULT_ + DEFINE_QUERY_SUCCESS)
    }

    private fun runUndefineQuery(query: TypeQLUndefine) {
        response.log.emptyLine()
        response.log.collect(INFO, RUNNING_ + UNDEFINE_QUERY)
        response.log.collect(TYPEQL, query.toString())
        transaction.query().undefine(query).get()
        response.log.emptyLine()
        response.log.collect(SUCCESS, RESULT_ + UNDEFINE_QUERY_SUCCESS)
    }

    private fun runDeleteQuery(query: TypeQLDelete) {
        response.log.emptyLine()
        response.log.collect(INFO, RUNNING_ + DELETE_QUERY)
        response.log.collect(TYPEQL, query.toString())
        transaction.query().delete(query).get()
        response.log.emptyLine()
        response.log.collect(SUCCESS, RESULT_ + DELETE_QUERY_SUCCESS)
    }

    private fun runInsertQuery(query: TypeQLInsert) {
        response.log.emptyLine()
        response.log.collect(INFO, RUNNING_ + INSERT_QUERY)
        response.log.collect(TYPEQL, query.toString())
        logResultStream(
            results = transaction.query().insert(query),
            successMessage = RESULT_ + INSERT_QUERY_SUCCESS,
            noResultMessage = RESULT_ + INSERT_QUERY_NO_RESULT
        )
    }

    private fun runUpdateQuery(query: TypeQLUpdate) {
        response.log.emptyLine()
        response.log.collect(INFO, RUNNING_ + UPDATE_QUERY)
        response.log.collect(TYPEQL, query.toString())
        logResultStream(
            results = transaction.query().update(query),
            successMessage = RESULT_ + UPDATE_QUERY_SUCCESS,
            noResultMessage = RESULT_ + UPDATE_QUERY_NO_RESULT
        )
    }

    private fun runMatchQuery(query: TypeQLMatch) {
        response.log.emptyLine()
        response.log.collect(INFO, RUNNING_ + MATCH_QUERY)
        response.log.collect(TYPEQL, query.toString())
        logResultStream(
            results = transaction.query().match(query),
            successMessage = RESULT_ + MATCH_QUERY_SUCCESS,
            noResultMessage = RESULT_ + MATCH_QUERY_NO_RESULT
        )
    }

    private fun logResultStream(results: Stream<ConceptMap>, successMessage: String, noResultMessage: String) {
        var started = false
        response.log.emptyLine()
        results.peek {
            if (started) return@peek
            response.log.collect(SUCCESS, successMessage)
            started = true
        }.forEach {
            if (hasStopSignal.get()) return@forEach
            response.log.collect(TYPEQL, printConceptMap(it))
        }
        if (!started) response.log.collect(SUCCESS, noResultMessage)
    }

    private fun printConceptMap(conceptMap: ConceptMap?): String {
        return conceptMap.toString() // TODO
    }
}
