package com.vaticle.typedb.studio.visualiser

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.withFrameNanos
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.data.GraphData
import com.vaticle.typedb.studio.data.QueryResponseStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging.logger
import java.util.concurrent.CompletionException

suspend fun runSimulation(simulation: TypeDBForceSimulation, dataStream: QueryResponseStream,
                          snackbarHostState: SnackbarHostState, snackbarCoroutineScope: CoroutineScope) {
    val log = logger {}

    fun fetchNewData() {
        if (dataStream.isEmpty()) return
        dataStream.lastFetchedNanos = System.nanoTime()
        val response: Either<GraphData, Exception> = dataStream.drain()
        if (response.isFirst) {
            val graphData: GraphData = response.first()
            simulation.addVertices(graphData.vertices.map(VertexState.Companion::of))
            simulation.addEdges(graphData.edges.map(EdgeState.Companion::of))
            simulation.addVertexExplanations(graphData.explanationVertices.map(VertexExplanationState.Companion::of))
            if (simulation.alpha() < 0.3) simulation.alpha(0.3)
        } else {
            // TODO: Add some kind of indicator that results may be incomplete
            val e: Throwable = when {
                response.second() is CompletionException -> response.second().cause ?: response.second()
                else -> response.second()
            }
            snackbarCoroutineScope.launch {
                log.debug(e) { "Query error" }
                snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
            }
        }
    }

    while (true) {
        withFrameNanos {
            if (!simulation.isStarted || System.nanoTime() - simulation.lastTickStartNanos < 1.667e7) /* 60 FPS */ {
                return@withFrameNanos
            }
            simulation.lastTickStartNanos = System.nanoTime()

            try {
                if (System.nanoTime() - dataStream.lastFetchedNanos > 5e7 /* 50ms */) fetchNewData()
                if (simulation.isEmpty() || simulation.alpha() < simulation.alphaMin()) return@withFrameNanos
                simulation.tick()
            } catch (e: Exception) {
                snackbarCoroutineScope.launch {
                    log.error(e) { "Error in simulation runner coroutine" }
                    snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
                }
            }
        }
    }
}
