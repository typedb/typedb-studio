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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture

internal class RunOutputGroup constructor(
    private val runner: QueryRunner,
    textEditorState: TextEditor.State,
    colors: Color.Theme,
    private val coroutineScope: CoroutineScope
) {

    private val graphCount = AtomicInteger(0)
    private val tableCount = AtomicInteger(0)
    private val log = LogOutput.State(textEditorState, colors, runner.transaction)
    internal val outputs: MutableList<RunOutput.State> = mutableStateListOf(log)
    internal var active: RunOutput.State by mutableStateOf(log)
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

    @OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
    private fun consumeResponses() = coroutineScope.launch {
        val responses: MutableList<Response> = mutableListOf()
        do {
            delay(Duration.Companion.milliseconds(CONSUMER_PERIOD_MS))
            responses.clear()
            runner.responses.drainTo(responses)
            if (responses.isNotEmpty()) withContext(Dispatchers.Default) { responses.forEach { output(it) } }
        } while (responses.lastOrNull() != Response.Done)
        runner.isConsumed()
    }

    @OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
    private fun <T> consumeStream(
        stream: Response.Stream<T>, onCompleted: (() -> Unit)? = null, output: (T) -> Unit
    ) {
        val responses: MutableList<Either<T, Response.Done>> = mutableListOf()
        val tasks: MutableList<CompletableFuture<*>> = mutableListOf()
        do {
            Thread.sleep(CONSUMER_PERIOD_MS.toLong())
            responses.clear()
            stream.queue.drainTo(responses)
            if (responses.isNotEmpty()) responses.filter { it.isFirst }.forEach { tasks += CompletableFuture.supplyAsync { output(it.first()) } }
        } while (responses.lastOrNull()?.isSecond != true)
        CompletableFuture.allOf(*tasks.toTypedArray()).join()
        onCompleted?.let { it() }
    }

    private fun output(response: Response) {
        when (response) {
            is Response.Message -> log.output(response)
            is Response.Numeric -> log.output(response.value)
            is Response.Stream<*> -> when (response) {
                is Response.Stream.NumericGroups -> consumeStream(response) { log.output(it) }
                is Response.Stream.ConceptMapGroups -> consumeStream(response) { log.output(it) }
                is Response.Stream.ConceptMaps -> {
                    val table = TableOutput.State(
                        transaction = runner.transaction, number = tableCount.incrementAndGet()
                    ).also { outputs.add(it) }
                    val graph = GraphOutput.State(
                        transaction = runner.transaction, number = graphCount.incrementAndGet()
                    ).also { outputs.add(it); activate(it) }
                    consumeStream(response, onCompleted = { graph.onQueryCompleted() }) {
                        log.output(it) // TODO: investigate freezing on a large query
                        table.output(it)
                        graph.output(it)
                    }
                }
            }
            is Response.Done -> {}
        }
    }
}
