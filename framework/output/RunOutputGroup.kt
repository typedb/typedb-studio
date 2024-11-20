/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.output

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.typedb.studio.framework.material.Tabs
import com.typedb.studio.service.Service
import com.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.typedb.studio.service.common.NotificationService.Companion.launchCompletableFuture
import com.typedb.studio.service.common.StatusService.Key.OUTPUT_RESPONSE_TIME
import com.typedb.studio.service.common.StatusService.Key.QUERY_RESPONSE_TIME
import com.typedb.studio.service.connection.QueryRunner
import com.typedb.studio.service.connection.QueryRunner.Response
import com.typedb.studio.service.connection.QueryRunner.Response.Stream.ConceptMaps.Source.GET
import com.typedb.common.collection.Either
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mu.KotlinLogging

internal class RunOutputGroup constructor(
    private val runner: QueryRunner,
    private val logOutput: LogOutput
) {

    private val graphCount = AtomicInteger(0)
    private val tableCount = AtomicInteger(0)
    internal val outputs: MutableList<RunOutput> = mutableStateListOf(logOutput)
    internal var active: RunOutput by mutableStateOf(logOutput)
    private val serialOutputFutures = LinkedBlockingQueue<Either<CompletableFuture<(() -> Unit)?>, Done>>()
    private val nonSerialOutputFutures = LinkedBlockingQueue<Either<CompletableFuture<Unit?>, Done>>()
    private val coroutines = CoroutineScope(Dispatchers.Default)
    private val futuresLatch = CountDownLatch(2)
    private var endTime: Long? = null
    internal val tabsState = Tabs.Horizontal.State<RunOutput>()

    object Done

    companion object {
        private const val COUNT_DOWN_LATCH_PERIOD_MS: Long = 50
        private val CONSUMER_PERIOD_MS = 33.milliseconds // 30 FPS
        private val LOGGER = KotlinLogging.logger {}

        @Composable
        fun createAndLaunch(runner: QueryRunner) = RunOutputGroup(
            runner = runner, logOutput = LogOutput.create(runner.transactionState)
        ).also { it.launch() }

        private suspend fun <E> LinkedBlockingQueue<E>.takeNonBlocking(periodMS: Long): E {
            while (true) {
                val item = this.poll(1, MILLISECONDS)
                if (item != null) return item else delay(periodMS)
            }
        }
    }

    internal fun launch() {
        runner.onClose { clearStatus() }
        logOutput.start()
        launchResponseConsumer()
        launchSerialOutputConsumer()
        launchNonSerialOutputConsumer()
        launchRunnerConcluder()
    }

    internal fun publishStatus() {
        publishQueryResponseTime()
        publishOutputResponseTime()
    }

    private fun publishQueryResponseTime() = runner.startTime?.let { startTime ->
            val responseTime = (runner.endTime ?: System.currentTimeMillis()) - startTime
            Service.status.publish(QUERY_RESPONSE_TIME, responseTime.milliseconds.toString())
    } ?: Unit

    private fun publishOutputResponseTime() = runner.endTime?.let { queryEndTime ->
            val responseTime = (endTime ?: System.currentTimeMillis()) - queryEndTime
            Service.status.publish(OUTPUT_RESPONSE_TIME, responseTime.milliseconds.toString())
    } ?: Unit

    private fun clearStatus() {
        Service.status.clear(QUERY_RESPONSE_TIME)
        Service.status.clear(OUTPUT_RESPONSE_TIME)
    }

    internal fun isActive(runOutput: RunOutput): Boolean = active == runOutput

    internal fun activate(runOutput: RunOutput) {
        active = runOutput
    }

    private fun collectSerial(outputFn: () -> Unit) = collectSerial(CompletableFuture.completedFuture(outputFn))

    private fun collectSerial(
        outputFnFuture: CompletableFuture<(() -> Unit)?>
    ) = serialOutputFutures.put(Either.first(outputFnFuture))

    private fun collectNonSerial(future: CompletableFuture<Unit?>) = nonSerialOutputFutures.put(Either.first(future))

    private fun launchSerialOutputConsumer() = coroutines.launchAndHandle(Service.notification, LOGGER) {
        do {
            val future = serialOutputFutures.takeNonBlocking(COUNT_DOWN_LATCH_PERIOD_MS)
            if (future.isFirst) future.first().join()?.invoke()
        } while (future.isFirst)
        futuresLatch.countDown()
    }

    private fun launchNonSerialOutputConsumer() = coroutines.launchAndHandle(Service.notification, LOGGER) {
        val futures = mutableListOf<CompletableFuture<Unit?>>()
        do {
            val future = nonSerialOutputFutures.takeNonBlocking(COUNT_DOWN_LATCH_PERIOD_MS)
            if (future.isFirst) futures += future.first()
        } while (future.isFirst)
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        futuresLatch.countDown()
    }

    private fun launchRunnerConcluder() = coroutines.launchAndHandle(Service.notification, LOGGER) {
        while (futuresLatch.count > 0L) {
            delay(COUNT_DOWN_LATCH_PERIOD_MS)
        }
        outputs.filterIsInstance<GraphOutput>().forEach { it.setCompleted() }
        runner.setConsumed()
        logOutput.stop()
        endTime = System.currentTimeMillis()
    }

    private fun launchResponseConsumer() = coroutines.launchAndHandle(Service.notification, LOGGER) {
        do {
            val responses: MutableList<Response> = mutableListOf()
            delay(CONSUMER_PERIOD_MS)
            runner.responses.drainTo(responses)
            if (responses.isNotEmpty()) responses.forEach { consumeResponse(it) }
        } while (responses.lastOrNull() != Response.Done)
        serialOutputFutures.add(Either.second(Done))
        nonSerialOutputFutures.add(Either.second(Done))
    }

    private suspend fun consumeResponse(response: Response) = when (response) {
        is Response.Message -> consumeMessageResponse(response)
        is Response.Value -> consumeValueResponse(response)
        is Response.Stream<*> -> when (response) {
            is Response.Stream.ValueGroups -> consumeValueGroupStreamResponse(response)
            is Response.Stream.ConceptMapGroups -> consumeConceptMapGroupStreamResponse(response)
            is Response.Stream.ConceptMaps -> consumeConceptMapStreamResponse(response)
            is Response.Stream.JSONs -> consumeJSONStreamResponse(response)
        }
        is Response.Done -> {}
    }

    private fun consumeMessageResponse(response: Response.Message) = collectSerial(logOutput.outputFn(response))

    private fun consumeValueResponse(response: Response.Value) = collectSerial(logOutput.outputFn(response.value))

    private suspend fun consumeValueGroupStreamResponse(response: Response.Stream.ValueGroups) {
        consumeStreamResponse(response) {
            collectSerial(
                launchCompletableFuture(
                    Service.notification,
                    LOGGER
                ) { logOutput.outputFn(it) })
        }
    }

    private suspend fun consumeConceptMapGroupStreamResponse(
        response: Response.Stream.ConceptMapGroups
    ) = consumeStreamResponse(response) {
        collectSerial(launchCompletableFuture(Service.notification, LOGGER) { logOutput.outputFn(it) })
    }

    private suspend fun consumeConceptMapStreamResponse(response: Response.Stream.ConceptMaps) {
        val notificationSrv = Service.notification
        // TODO: enable configuration of displaying GraphOutput for INSERT and UPDATE
        val table = if (response.source != GET) null else TableOutput(
            transaction = runner.transactionState, number = tableCount.incrementAndGet()
        ) // TODO: .also { outputs.add(it) }
        val graph =
            if (response.source != GET || !Service.preference.graphOutputEnabled) null else GraphOutput(
                transactionState = runner.transactionState, number = graphCount.incrementAndGet()
            ).also { outputs.add(it); activate(it) }

        consumeStreamResponse(response) {
            collectSerial(launchCompletableFuture(notificationSrv, LOGGER) { logOutput.outputFn(it) })
            table?.let { t -> collectSerial(launchCompletableFuture(notificationSrv, LOGGER) { t.outputFn(it) }) }
            graph?.let { g -> collectNonSerial(launchCompletableFuture(notificationSrv, LOGGER) { g.output(it) }) }
        }
    }

    private suspend fun consumeJSONStreamResponse(response: Response.Stream.JSONs) = consumeStreamResponse(response) {
        collectSerial(launchCompletableFuture(Service.notification, LOGGER) { logOutput.outputFn(it) })
    }

    private suspend fun <T> consumeStreamResponse(
        stream: Response.Stream<T>,
        consumer: (T) -> Unit
    ) {
        val responses: MutableList<Either<T, Response.Done>> = mutableListOf()
        do {
            delay(CONSUMER_PERIOD_MS)
            responses.clear()
            stream.queue.drainTo(responses)
            if (responses.isNotEmpty()) responses.filter { it.isFirst }.forEach { consumer(it.first()) }
        } while (responses.lastOrNull()?.isSecond != true)
    }
}
