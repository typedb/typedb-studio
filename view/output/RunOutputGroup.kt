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

package com.vaticle.typedb.studio.view.output

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.state.connection.QueryRunner
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response
import com.vaticle.typedb.studio.view.common.component.Tabs
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.editor.TextEditor
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class RunOutputGroup constructor(
    private val runner: QueryRunner,
    textEditorState: TextEditor.State,
    colors: Color.Theme,
    private val coroutineScope: CoroutineScope
) {

    internal val log = LogOutput.State(textEditorState, colors, runner.transaction)
    internal val graphs: MutableList<GraphOutput.State> = mutableStateListOf()
    internal val tables: MutableList<TableOutput.State> = mutableStateListOf()
    internal val hasMultipleGraphs get() = graphs.size > 1
    internal val hasMultipleTables get() = tables.size > 1
    internal var active: RunOutput.State by mutableStateOf(log)
    internal val outputs: List<RunOutput.State> get() = listOf(log, *graphs.toTypedArray(), *tables.toTypedArray())
    internal val tabsState = Tabs.State<RunOutput.State>(coroutineScope)

    companion object {
        private const val CONSUMER_PERIOD_MS = 33 // 30 FPS
    }

    init {
        consumeResponses()
    }

    internal fun isActive(runOutput: RunOutput.State): Boolean {
        return active == runOutput
    }

    internal fun activate(runOutput: RunOutput.State) {
        active = runOutput
    }

    internal fun numberOf(graph: GraphOutput.State): Int {
        return graphs.indexOf(graph) + 1
    }

    internal fun numberOf(table: TableOutput.State): Int {
        return tables.indexOf(table) + 1
    }

    @OptIn(ExperimentalTime::class)
    private fun consumeResponses() = coroutineScope.launch {
        val responses: MutableList<Response> = mutableListOf()
        do {
            delay(Duration.Companion.milliseconds(CONSUMER_PERIOD_MS))
            responses.clear()
            runner.responses.drainTo(responses)
            if (responses.isNotEmpty()) responses.forEach { output(it) }
        } while (responses.lastOrNull() != Response.Done)
        runner.isConsumed()
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun <T> consumeStream(stream: Response.Stream<T>, output: (T) -> Unit) {
        val responses: MutableList<Either<T, Response.Done>> = mutableListOf()
        do {
            delay(Duration.Companion.milliseconds(CONSUMER_PERIOD_MS))
            responses.clear()
            stream.queue.drainTo(responses)
            if (responses.isNotEmpty()) responses.filter { it.isFirst }.forEach { output(it.first()) }
        } while (responses.lastOrNull()?.isSecond != true)
    }

    private suspend fun output(response: Response) {
        when (response) {
            is Response.Message -> log.output(response)
            is Response.Numeric -> log.output(response.value)
            is Response.Stream<*> -> when (response) {
                is Response.Stream.NumericGroups -> consumeStream(response) { log.output(it) }
                is Response.Stream.ConceptMapGroups -> consumeStream(response) { log.output(it) }
                is Response.Stream.ConceptMaps -> {
                    val graph = GraphOutput.State(runner.transaction).also { graphs.add(it) }
                    val table = TableOutput.State(runner.transaction).also { tables.add(it) }
                    consumeStream(response) {
                        log.output(it)
                        graph.output(it)
                        table.collect(it)
                    }
                }
            }
            is Response.Done -> {}
        }
    }
}
