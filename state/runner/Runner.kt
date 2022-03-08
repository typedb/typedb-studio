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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
        const val DEFINE_QUERY_SUCCESS = "Defined Types Successfully."
        const val UNDEFINE_QUERY_SUCCESS = "Undefined Types Successfully."
        const val DELETE_QUERY_SUCCESS = "Deleted Things Successfully."
        const val INSERTED_QUERY_SUCCESS = "Inserted Things Successfully:"
        const val UPDATED_QUERY_SUCCESS = "Updated Things Successfully:"
        const val MATCH_QUERY_SUCCESS = "Matched Things Successfully:"
    }

    var isSaved by mutableStateOf(false)
    val response = ResponseManager()
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)
    private val onComplete = LinkedBlockingDeque<(Runner) -> Unit>()

    fun save() {
        isSaved = true
    }

    fun onComplete(function: (Runner) -> Unit) {
        onComplete.push(function)
    }

    internal fun launch() {
        coroutineScope.launch {
            try {
                runQueries(TypeQL.parseQueries<TypeQLQuery>(queries).toList())
            } catch (e: Exception) {
                response.log.emptyLine()
                response.log.collect(ERROR, ERROR_ + e.message)
            } finally {
                onComplete.forEach { it(this@Runner) }
            }
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
                else -> throw IllegalStateException()
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
            successMessage = RESULT_ + INSERTED_QUERY_SUCCESS
        )
    }

    private fun runUpdateQuery(query: TypeQLUpdate) {
        response.log.emptyLine()
        response.log.collect(INFO, RUNNING_ + UPDATE_QUERY)
        response.log.collect(TYPEQL, query.toString())
        logResultStream(
            results = transaction.query().update(query),
            successMessage = RESULT_ + UPDATED_QUERY_SUCCESS
        )
    }

    private fun runMatchQuery(query: TypeQLMatch) {
        response.log.emptyLine()
        response.log.collect(INFO, RUNNING_ + MATCH_QUERY)
        response.log.collect(TYPEQL, query.toString())
        logResultStream(
            results = transaction.query().match(query),
            successMessage = RESULT_ + MATCH_QUERY_SUCCESS
        )
    }

    private fun logResultStream(results: Stream<ConceptMap>, successMessage: String) {
        var started = false
        results.peek {
            if (started) return@peek
            response.log.emptyLine()
            response.log.collect(SUCCESS, successMessage)
            started = true
        }.forEach {
            if (hasStopSignal.get()) return@forEach
            response.log.collect(TYPEQL, printConceptMap(it))
        }
    }

    private fun printConceptMap(conceptMap: ConceptMap?): String {
        return conceptMap.toString() // TODO
    }
}
