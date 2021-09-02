package com.vaticle.typedb.studio.visualiser

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.data.GraphData
import com.vaticle.typedb.studio.data.QueryResponseStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import java.util.concurrent.CompletionException

suspend fun simulationRunnerCoroutine(simulation: TypeDBForceSimulation, dataStream: QueryResponseStream,
                                      snackbarHostState: SnackbarHostState, snackbarCoroutineScope: CoroutineScope) {
    while (true) {
        withFrameNanos {
            if (!simulation.isStarted
                || System.nanoTime() - simulation.lastTickStartNanos < 1.667e7) // 16.667ms = 60 FPS
            {
//                println("isEmpty=${simulation.isEmpty()};alpha=${simulation.alpha()};alphaMin=${simulation.alphaMin()}")
                return@withFrameNanos
            }

            simulation.lastTickStartNanos = System.nanoTime()

            if (!dataStream.isEmpty() &&
                System.nanoTime() - dataStream.lastFetchedNanos > 5e7) { // 50ms

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
                    val error: Throwable = when {
                        response.second() is CompletionException -> response.second().cause.let {
                            when (it) { null -> response.second() else -> it }
                        }
                        else -> response.second()
                    }
                    snackbarCoroutineScope.launch {
                        println(error.stackTraceToString())
                        snackbarHostState.showSnackbar(error.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
                    }
                }
            }

            if (simulation.isEmpty() || simulation.alpha() < simulation.alphaMin()) return@withFrameNanos

            simulation.tick()

            simulation.data.vertices.forEach {
                val node = simulation.nodes()[it.id]
                    ?: throw IllegalStateException("Received bad simulation data: no entry received for vertex ID ${it.id}!")
                it.position = Offset(node.x().toFloat(), node.y().toFloat())
            }
            val verticesByID: Map<Int, VertexState> = simulation.data.vertices.associateBy { it.id }
            simulation.data.edges.forEach {
                it.sourcePosition = verticesByID[it.sourceID]!!.position
                it.targetPosition = verticesByID[it.targetID]!!.position
            }
        }
    }
}
