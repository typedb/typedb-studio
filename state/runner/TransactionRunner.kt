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

import androidx.compose.runtime.mutableStateListOf
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.studio.state.runner.TransactionRunner.LogOutput.Type.ERROR
import com.vaticle.typedb.studio.state.runner.TransactionRunner.LogOutput.Type.INFO
import com.vaticle.typedb.studio.state.runner.TransactionRunner.LogOutput.Type.SUCCESS
import com.vaticle.typedb.studio.state.runner.TransactionRunner.LogOutput.Type.TYPEQL
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLDefine
import com.vaticle.typeql.lang.query.TypeQLDelete
import com.vaticle.typeql.lang.query.TypeQLInsert
import com.vaticle.typeql.lang.query.TypeQLMatch
import com.vaticle.typeql.lang.query.TypeQLQuery
import com.vaticle.typeql.lang.query.TypeQLUndefine
import com.vaticle.typeql.lang.query.TypeQLUpdate
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.streams.toList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TransactionRunner constructor(private val transaction: TypeDBTransaction, private val queries: String) {

    data class LogOutput(val type: Type, val text: String) {

        enum class Type { INFO, SUCCESS, ERROR, TYPEQL }
    }

    companion object {
        const val RUNNING_DEFINE_QUERY = "Running Define Query:"
        const val RUNNING_UNDEFINE_QUERY = "Running Undefine Query:"
        const val RUNNING_INSERT_QUERY = "Running Insert Query:"
        const val RUNNING_DELETE_QUERY = "Running Delete Query:"
        const val RUNNING_UPDATE_QUERY = "Running Update Query:"
        const val RUNNING_MATCH_QUERY = "Running Match Query:"
        const val RESULT_ = "Result> "
        const val ERROR_ = "Error> "
        const val DEFINE_QUERY_SUCCESS = "Defined Types Successfully."
        const val UNDEFINE_QUERY_SUCCESS = "Undefined Types Successfully."
        const val DELETE_QUERY_SUCCESS = "Deleted Things Successfully."
        const val INSERTED_QUERY_SUCCESS = "Inserted Things Successfully:"
        const val UPDATED_QUERY_SUCCESS = "Updated Things Successfully:"
        const val MATCH_QUERY_SUCCESS = "Matched Things Successfully:"
    }

    private val _logOutput: MutableList<LogOutput> = mutableStateListOf()
    private val _graphOutput: MutableList<GraphOutput> = mutableStateListOf()
    private val _tableOutput: MutableList<TableOutput> = mutableStateListOf()
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)
    val logOutput: List<LogOutput> = _logOutput
    val graphOutput: List<GraphOutput> = _graphOutput
    val tableOutput: List<TableOutput> = _tableOutput

    private fun logOutput(type: LogOutput.Type, text: String) {
        _logOutput.add(LogOutput(type, text))
    }

    fun launch(onComplete: () -> Unit) {
        coroutineScope.launch {
            try {
                runQueries(TypeQL.parseQueries<TypeQLQuery>(queries).toList())
            } catch (e: Exception) {
                _logOutput.add(LogOutput(ERROR, e.message ?: e.toString()))
            } finally {
                onComplete()
            }
        }
    }

    private fun runQueries(queries: List<TypeQLQuery>) {
        queries.forEach { query ->
            val success = when (query) {
                is TypeQLDefine -> runDefineQuery(query)
                is TypeQLUndefine -> runUndefineQuery(query)
                is TypeQLDelete -> runDeleteQuery(query)
                is TypeQLInsert -> runInsertQuery(query)
                is TypeQLUpdate -> runUpdateQuery(query)
                is TypeQLMatch -> runMatchQuery(query)
                else -> throw IllegalStateException()
            }
            logOutput(INFO, "")
            if (!success) return
        }
    }

    private fun runDefineQuery(query: TypeQLDefine): Boolean {
        return runLoggedQuery {
            logOutput(INFO, RUNNING_DEFINE_QUERY)
            logOutput(TYPEQL, query.toString())
            transaction.query().define(query).get()
            logOutput(SUCCESS, RESULT_ + DEFINE_QUERY_SUCCESS)
        }
    }

    private fun runUndefineQuery(query: TypeQLUndefine): Boolean {
        return runLoggedQuery {
            logOutput(INFO, RUNNING_UNDEFINE_QUERY)
            logOutput(TYPEQL, query.toString())
            transaction.query().undefine(query).get()
            logOutput(SUCCESS, RESULT_ + UNDEFINE_QUERY_SUCCESS)
        }
    }

    private fun runDeleteQuery(query: TypeQLDelete): Boolean {
        return runLoggedQuery {
            logOutput(INFO, RUNNING_DELETE_QUERY)
            logOutput(TYPEQL, query.toString())
            transaction.query().delete(query).get()
            logOutput(SUCCESS, RESULT_ + DELETE_QUERY_SUCCESS)
        }
    }

    private fun runInsertQuery(query: TypeQLInsert): Boolean {
        return runLoggedQuery {
            logOutput(INFO, RUNNING_INSERT_QUERY)
            logOutput(TYPEQL, query.toString())
            val result = transaction.query().insert(query)
            logOutput(SUCCESS, RESULT_ + INSERTED_QUERY_SUCCESS)
            result.forEach { logOutput(TYPEQL, printConceptMap(it)) }
        }
    }

    private fun runUpdateQuery(query: TypeQLUpdate): Boolean {
        return runLoggedQuery {
            logOutput(INFO, RUNNING_UPDATE_QUERY)
            logOutput(TYPEQL, query.toString())
            val result = transaction.query().update(query)
            logOutput(SUCCESS, RESULT_ + UPDATED_QUERY_SUCCESS)
            result.forEach { logOutput(TYPEQL, printConceptMap(it)) }
        }
    }

    private fun runMatchQuery(query: TypeQLMatch): Boolean {
        return runLoggedQuery {
            logOutput(INFO, RUNNING_MATCH_QUERY)
            logOutput(TYPEQL, query.toString())
            val result = transaction.query().match(query)
            logOutput(SUCCESS, RESULT_ + MATCH_QUERY_SUCCESS)
            result.forEach { logOutput(TYPEQL, printConceptMap(it)) }
        }
    }

    private fun runLoggedQuery(function: () -> Unit): Boolean {
        return try {
            function()
            true
        } catch (e: Exception) {
            logOutput(ERROR, ERROR_ + e.message)
            false
        }
    }

    private fun printConceptMap(conceptMap: ConceptMap?): String {
        return conceptMap.toString() // TODO
    }
}
