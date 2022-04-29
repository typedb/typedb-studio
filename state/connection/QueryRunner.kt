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

package com.vaticle.typedb.studio.state.connection

import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.answer.ConceptMapGroup
import com.vaticle.typedb.client.api.answer.NumericGroup
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.thing.Attribute
import com.vaticle.typedb.client.api.concept.thing.Relation
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Type.ERROR
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Type.INFO
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Type.SUCCESS
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Type.TYPEQL
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.common.TypeQLToken.Constraint.IID
import com.vaticle.typeql.lang.common.TypeQLToken.Constraint.ISA
import com.vaticle.typeql.lang.common.TypeQLToken.Constraint.SUB
import com.vaticle.typeql.lang.common.TypeQLToken.Constraint.TYPE
import com.vaticle.typeql.lang.common.util.Strings.indent
import com.vaticle.typeql.lang.common.util.Strings.valueToString
import com.vaticle.typeql.lang.query.TypeQLDefine
import com.vaticle.typeql.lang.query.TypeQLDelete
import com.vaticle.typeql.lang.query.TypeQLInsert
import com.vaticle.typeql.lang.query.TypeQLMatch
import com.vaticle.typeql.lang.query.TypeQLQuery
import com.vaticle.typeql.lang.query.TypeQLUndefine
import com.vaticle.typeql.lang.query.TypeQLUpdate
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors.joining
import java.util.stream.Stream
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.streams.toList
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTime::class)
class QueryRunner constructor(
    private val transaction: TypeDBTransaction,
    private val queries: String,
    private val hasStopSignal: AtomicBoolean,
    private val onComplete: () -> Unit
) {

    companion object {
        const val RESULT_ = "## Result> "
        const val ERROR_ = "## Error> "
        const val RUNNING_ = "## Running> "
        const val COMPLETED = "## Completed"
        const val DEFINE_QUERY = "Define Query:"
        const val DEFINE_QUERY_SUCCESS = "Define query successfully defined new types in the schema."
        const val UNDEFINE_QUERY = "Undefine Query:"
        const val UNDEFINE_QUERY_SUCCESS = "Undefine query successfully undefined types in the schema."
        const val DELETE_QUERY = "Delete Query:"
        const val DELETE_QUERY_SUCCESS = "Delete query successfully deleted things from the database."
        const val INSERT_QUERY = "Insert Query:"
        const val INSERT_QUERY_SUCCESS = "Insert query successfully inserted new things to the database:"
        const val INSERT_QUERY_NO_RESULT = "Insert query did not insert any new thing to the database."
        const val UPDATE_QUERY = "Update Query:"
        const val UPDATE_QUERY_SUCCESS = "Update query successfully updated things in the databases:"
        const val UPDATE_QUERY_NO_RESULT = "Update query did not update any thing in the databases."
        const val MATCH_QUERY = "Match Query:"
        const val MATCH_QUERY_SUCCESS = "Match query successfully matched concepts in the database:"
        const val MATCH_QUERY_NO_RESULT = "Match query did not match any concepts in the database."
        const val MATCH_AGGREGATE_QUERY = "Match Aggregate Query:"
        const val MATCH_GROUP_QUERY = "Match Group Query:"
        const val MATCH_GROUP_QUERY_SUCCESS = "Match Group query successfully matched concept groups in the database:"
        const val MATCH_GROUP_QUERY_NO_RESULT = "Match Group query did not match any concept groups in the database."
        const val MATCH_GROUP_AGGREGATE_QUERY = "Match Group Aggregate Query:"
        const val MATCH_GROUP_AGGREGATE_QUERY_SUCCESS =
            "Match Group Aggregate query successfully aggregated matched concept groups in the database:"
        const val MATCH_GROUP_AGGREGATE_QUERY_NO_RESULT =
            "Match Group Aggregate query did not match any concept groups to aggregate in the database."

        private val RUNNING_INDICATOR_DELAY = Duration.Companion.seconds(3)
    }

    object Done

    data class Response(val type: Type, val text: String) {
        enum class Type { INFO, SUCCESS, ERROR, TYPEQL }
    }

    val responses = LinkedBlockingQueue<Either<Response, Done>>()
    val conceptMapStreams = LinkedBlockingQueue<Either<LinkedBlockingQueue<Either<ConceptMap, Done>>, Done>>()
    private val isRunning = AtomicBoolean(false)
    private val lastResponse = AtomicLong(0)
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    fun launch() {
        isRunning.set(true)
        coroutineScope.launch { runningQueryIndicator() }
        coroutineScope.launch { runQueries() }
    }

    private fun collectEmptyLine() {
        collectResponse(INFO, "")
    }

    private fun collectResponse(type: Response.Type, string: String) {
        responses.put(Either.first(Response(type, string)))
        lastResponse.set(System.currentTimeMillis())
    }

    private suspend fun runningQueryIndicator() {
        var duration = RUNNING_INDICATOR_DELAY
        while (isRunning.get()) {
            delay(duration)
            if (!isRunning.get()) break
            val sinceLastResponse = System.currentTimeMillis() - lastResponse.get()
            if (sinceLastResponse >= RUNNING_INDICATOR_DELAY.inWholeMilliseconds) {
                collectResponse(INFO, "...")
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
            collectEmptyLine()
            collectResponse(ERROR, ERROR_ + e.message)
        } finally {
            responses.put(Either.second(Done))
            isRunning.set(false)
            onComplete()
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
            printerFn = { printConceptMap(it) },
        ) { transaction.query().insert(query) }
    }

    private fun runUpdateQuery(query: TypeQLUpdate) {
        runStreamingQuery(
            name = UPDATE_QUERY,
            successMsg = UPDATE_QUERY_SUCCESS,
            noResultMsg = UPDATE_QUERY_NO_RESULT,
            queryStr = query.toString(),
            printerFn = { printConceptMap(it) }
        ) { transaction.query().update(query) }
    }

    private fun runMatchQuery(query: TypeQLMatch) {
        runStreamingQuery(
            name = MATCH_QUERY,
            successMsg = MATCH_QUERY_SUCCESS,
            noResultMsg = MATCH_QUERY_NO_RESULT,
            queryStr = query.toString(),
            printerFn = { printConceptMap(it) }
        ) { transaction.query().match(query) }
    }

    private fun runMatchAggregateQuery(query: TypeQLMatch.Aggregate) {
        printQueryStart(MATCH_AGGREGATE_QUERY, query.toString())
        val result = transaction.query().match(query).get()
        collectEmptyLine()
        collectResponse(SUCCESS, RESULT_ + result)
    }

    private fun runMatchGroupQuery(query: TypeQLMatch.Group) {
        runStreamingQuery(
            name = MATCH_GROUP_QUERY,
            successMsg = MATCH_GROUP_QUERY_SUCCESS,
            noResultMsg = MATCH_GROUP_QUERY_NO_RESULT,
            queryStr = query.toString(),
            printerFn = { printConceptMapGroup(it) }
        ) { transaction.query().match(query) }
    }

    private fun runMatchGroupAggregateQuery(query: TypeQLMatch.Group.Aggregate) {
        runStreamingQuery(
            name = MATCH_GROUP_AGGREGATE_QUERY,
            successMsg = MATCH_GROUP_AGGREGATE_QUERY_SUCCESS,
            noResultMsg = MATCH_GROUP_AGGREGATE_QUERY_NO_RESULT,
            queryStr = query.toString(),
            printerFn = { printNumericGroup(it) }
        ) { transaction.query().match(query) }
    }

    private fun runUnitQuery(name: String, successMsg: String, queryStr: String, queryFn: () -> Unit) {
        printQueryStart(name, queryStr)
        queryFn()
        collectEmptyLine()
        collectResponse(SUCCESS, RESULT_ + successMsg)
    }

    private fun <T : Any> runStreamingQuery(
        name: String,
        successMsg: String,
        noResultMsg: String,
        queryStr: String,
        printerFn: (T) -> String,
        queryFn: () -> Stream<T>
    ) {
        printQueryStart(name, queryStr)
        consumeResultStream(queryFn(), RESULT_ + successMsg, RESULT_ + noResultMsg, printerFn)
    }

    private fun printQueryStart(name: String, queryStr: String) {
        collectEmptyLine()
        collectResponse(INFO, RUNNING_ + name)
        collectResponse(TYPEQL, queryStr)
    }

    private fun <T : Any> consumeResultStream(
        results: Stream<T>,
        successMessage: String,
        noResultMessage: String, printerFn: (T) -> String
    ) {
        var started = false
        val conceptMapStream = LinkedBlockingQueue<Either<ConceptMap, Done>>()
        collectEmptyLine()
        results.peek {
            if (started) return@peek
            if (it is ConceptMap) conceptMapStreams.put(Either.first(conceptMapStream))
            collectResponse(SUCCESS, successMessage)
            started = true
        }.forEach {
            if (hasStopSignal.get()) return@forEach
            collectResponse(TYPEQL, printerFn(it))
            if (it is ConceptMap) conceptMapStream.put(Either.first(it))
        }
        if (started) {
            collectEmptyLine()
            collectResponse(SUCCESS, COMPLETED)
            conceptMapStream.put(Either.second(Done))
        } else collectResponse(SUCCESS, noResultMessage)
    }

    private fun printNumericGroup(group: NumericGroup): String {
        return printConcept(group.owner()) + " => " + group.numeric().asNumber()
    }

    private fun printConceptMapGroup(group: ConceptMapGroup): String {
        val str = StringBuilder(printConcept(group.owner()) + " => {\n")
        group.conceptMaps().forEach { str.append(indent(printConceptMap(it))) }
        str.append("\n}")
        return str.toString()
    }

    private fun printConceptMap(conceptMap: ConceptMap): String {
        val content = conceptMap.map().map {
            "$" + it.key + " " + printConcept(it.value) + ";"
        }.stream().collect(joining("\n"))

        val str = StringBuilder("{")
        if (content.lines().size > 1) str.append("\n").append(indent(content)).append("\n")
        else str.append(" ").append(content).append(" ")
        str.append("}")
        return str.toString()
    }

    private fun printConcept(concept: Concept): String {
        return when (concept) {
            is Type -> printType(concept)
            is Thing -> printThing(concept)
            else -> throw IllegalStateException("Unrecognised TypeQL Concept")
        }
    }

    private fun printType(type: Type): String {
        var str = TYPE.toString() + " " + type.label
        type.asRemote(transaction).supertype?.let { str += " " + SUB + " " + it.label.scopedName() }
        return str
    }

    private fun printThing(thing: Thing): String {
        val str = StringBuilder()
        when (thing) {
            is Attribute<*> -> str.append(valueToString(thing.value))
            else -> str.append(IID.toString() + " " + thing.asThing().iid)
        }
        if (thing is Relation) str.append(" ").append(printRolePlayers(thing.asThing().asRelation()))
        str.append(" ").append(ISA).append(" ").append(thing.asThing().type.label.scopedName())
        return str.toString()
    }

    private fun printRolePlayers(relation: Relation): String {
        val rolePlayers = relation.asRemote(transaction).playersByRoleType.flatMap { (role, players) ->
            players.map { player -> role.label.name() + ": " + IID + " " + player.iid }
        }.stream().collect(joining(", "))
        return "($rolePlayers)"
    }
}
