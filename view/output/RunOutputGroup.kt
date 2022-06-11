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
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchCompletableFuture
import com.vaticle.typedb.studio.state.app.StatusManager.Key.OUTPUT_RESPONSE_TIME
import com.vaticle.typedb.studio.state.app.StatusManager.Key.QUERY_RESPONSE_TIME
import com.vaticle.typedb.studio.state.connection.QueryRunner
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Stream.ConceptMaps.Source.MATCH
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.editor.TextEditor
import com.vaticle.typedb.studio.view.material.Tabs
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mu.KotlinLogging

internal class RunOutputGroup constructor(
    private val runner: QueryRunner,
    textEditorState: TextEditor.State,
    colors: Color.StudioTheme
) {

    private val graphCount = AtomicInteger(0)
    private val tableCount = AtomicInteger(0)
    private val logOutput = LogOutput(textEditorState, runner.transactionState, colors)
    internal val outputs: MutableList<RunOutput> = mutableStateListOf(logOutput)
    internal var active: RunOutput by mutableStateOf(logOutput)
    private val serialOutputFutures = LinkedBlockingQueue<Either<CompletableFuture<(() -> Unit)?>, Done>>()
    private val nonSerialOutputFutures = LinkedBlockingQueue<Either<CompletableFuture<Unit?>, Done>>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val futuresLatch = CountDownLatch(2)
    private var endTime: Long? = null
    internal val tabsState = Tabs.Horizontal.State<RunOutput>()

    object Done

    companion object {
        private const val CONSUMER_PERIOD_MS = 33 // 30 FPS
        private const val COUNT_DOWN_LATCH_PERIOD_MS: Long = 50
        private val LOGGER = KotlinLogging.logger {}

        private suspend fun <E> LinkedBlockingQueue<E>.takeNonBlocking(periodMS: Long): E {
            while (true) {
                val item = this.poll(1, MILLISECONDS)
                if (item != null) return item else delay(periodMS)
            }
        }
    }

    init {
        runner.onClose { clearStatus() }
        consumeResponses()
        printSerialOutput()
        concludeNonSerialOutput()
        concludeRunnerIsConsumed()
    }

    internal fun publishStatus() {
        publishQueryResponseTime()
        publishOutputResponseTime()
    }

    @OptIn(ExperimentalTime::class)
    private fun publishQueryResponseTime() {
        runner.startTime?.let { startTime ->
            val duration = (runner.endTime ?: System.currentTimeMillis()) - startTime
            GlobalState.status.publish(QUERY_RESPONSE_TIME, Duration.milliseconds(duration).toString())
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun publishOutputResponseTime() {
        runner.endTime?.let { queryEndTime ->
            val duration = (endTime ?: System.currentTimeMillis()) - queryEndTime
            GlobalState.status.publish(OUTPUT_RESPONSE_TIME, Duration.milliseconds(duration).toString())
        }
    }

    private fun clearStatus() {
        GlobalState.status.clear(QUERY_RESPONSE_TIME)
        GlobalState.status.clear(OUTPUT_RESPONSE_TIME)
    }

    internal fun isActive(runOutput: RunOutput): Boolean {
        return active == runOutput
    }

    internal fun activate(runOutput: RunOutput) {
        active = runOutput
    }

    private fun concludeRunnerIsConsumed() = coroutineScope.launchAndHandle(GlobalState.notification, LOGGER) {
        while (futuresLatch.count > 0L) {
            delay(COUNT_DOWN_LATCH_PERIOD_MS)
        }
        runner.setConsumed()
        endTime = System.currentTimeMillis()
    }

    private fun concludeNonSerialOutput() = coroutineScope.launchAndHandle(GlobalState.notification, LOGGER) {
        val futures = mutableListOf<CompletableFuture<Unit?>>()
        do {
            val future = nonSerialOutputFutures.takeNonBlocking(COUNT_DOWN_LATCH_PERIOD_MS)
            if (future.isFirst) futures += future.first()
        } while (future.isFirst)
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        futuresLatch.countDown()
    }

    private fun collectNonSerial(future: CompletableFuture<Unit?>) {
        nonSerialOutputFutures.put(Either.first(future))
    }

    private fun printSerialOutput() = coroutineScope.launchAndHandle(GlobalState.notification, LOGGER) {
        do {
            val future = serialOutputFutures.takeNonBlocking(COUNT_DOWN_LATCH_PERIOD_MS)
            if (future.isFirst) future.first().join()?.invoke()
        } while (future.isFirst)
        futuresLatch.countDown()
    }

    private fun collectSerial(outputFn: () -> Unit) {
        collectSerial(CompletableFuture.completedFuture(outputFn))
    }

    private fun collectSerial(outputFnFuture: CompletableFuture<(() -> Unit)?>) {
        serialOutputFutures.put(Either.first(outputFnFuture))
    }

    @OptIn(ExperimentalTime::class)
    @Suppress("BlockingMethodInNonBlockingContext")
    private fun consumeResponses() = coroutineScope.launchAndHandle(GlobalState.notification, LOGGER) {
        do {
            val responses: MutableList<Response> = mutableListOf()
            delay(Duration.Companion.milliseconds(CONSUMER_PERIOD_MS))
            runner.responses.drainTo(responses)
            if (responses.isNotEmpty()) responses.forEach { consumeResponse(it) }
        } while (responses.lastOrNull() != Response.Done)
        serialOutputFutures.put(Either.second(Done))
        nonSerialOutputFutures.put(Either.second(Done))
    }

    private fun consumeResponse(response: Response) {
        when (response) {
            is Response.Message -> collectSerial { logOutput.output(response) }
            is Response.Numeric -> collectSerial { logOutput.output(response.value) }
            is Response.Stream<*> -> when (response) {
                is Response.Stream.NumericGroups -> consumeResponseStream(response) {
                    collectSerial(launchCompletableFuture(GlobalState.notification, LOGGER) { logOutput.outputFn(it) })
                }
                is Response.Stream.ConceptMapGroups -> consumeResponseStream(response) {
                    collectSerial(launchCompletableFuture(GlobalState.notification, LOGGER) { logOutput.outputFn(it) })
                }
                is Response.Stream.ConceptMaps -> {
                    // TODO: enable configuration of displaying GraphOutput for INSERT and UPDATE
                    val table = if (response.source != MATCH) null else TableOutput(
                        transaction = runner.transactionState, number = tableCount.incrementAndGet()
                    ) // TODO: .also { outputs.add(it) }
                    val graph = if (response.source != MATCH) null else GraphOutput(
                        transactionState = runner.transactionState, number = graphCount.incrementAndGet()
                    ).also { outputs.add(it); activate(it) }
                    consumeResponseStream(response, onCompleted = { graph?.setCompleted() }) {
                        collectSerial(launchCompletableFuture(GlobalState.notification, LOGGER) {
                            logOutput.outputFn(it)
                        })
                        table?.let { t ->
                            collectSerial(launchCompletableFuture(GlobalState.notification, LOGGER) { t.outputFn(it) })
                        }
                        graph?.let { g ->
                            collectNonSerial(launchCompletableFuture(GlobalState.notification, LOGGER) { g.output(it) })
                        }
                    }
                }
            }
            is Response.Done -> {}
        }
    }

    private fun <T> consumeResponseStream(
        stream: Response.Stream<T>, onCompleted: (() -> Unit)? = null, output: (T) -> Unit
    ) {
        val responses: MutableList<Either<T, Response.Done>> = mutableListOf()
        do {
            Thread.sleep(CONSUMER_PERIOD_MS.toLong())
            responses.clear()
            stream.queue.drainTo(responses)
            if (responses.isNotEmpty()) responses.filter { it.isFirst }.forEach { output(it.first()) }
        } while (responses.lastOrNull()?.isSecond != true)
        onCompleted?.let { it() }
    }
}
