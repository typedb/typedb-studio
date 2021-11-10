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

package com.vaticle.typedb.studio.visualiser

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.withFrameNanos
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.data.GraphData
import com.vaticle.typedb.studio.data.QueryResponseStream
import com.vaticle.typedb.studio.diagnostics.ErrorHandler
import com.vaticle.typedb.studio.diagnostics.LogLevel
import com.vaticle.typedb.studio.diagnostics.withUnexpectedErrorHandling
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging.logger
import java.util.concurrent.CompletionException

suspend fun runSimulation(
    simulation: TypeDBForceSimulation, dataStream: QueryResponseStream,
    snackbarHostState: SnackbarHostState, snackbarCoroutineScope: CoroutineScope
) {
    val log = logger {}
    val errorHandler = ErrorHandler(log, snackbarHostState, snackbarCoroutineScope)

    fun fetchNewData() {
        if (dataStream.isEmpty()) return
        dataStream.lastFetchedNanos = System.nanoTime()
        val response: Either<GraphData, Exception> = dataStream.drain()
        if (response.isFirst) {
            val graphData: GraphData = response.first()
            simulation.addVertices(graphData.vertices.map(::vertexStateOf))
            simulation.addEdges(graphData.edges.map(::edgeStateOf))
            simulation.addVertexExplanations(graphData.explanationVertices.map(::vertexExplanationStateOf))
            if (simulation.alpha() < 0.3) simulation.alpha(0.3)
        } else {
            // TODO: Add some kind of indicator that results may be incomplete
            val e: Throwable = when {
                response.second() is CompletionException -> response.second().cause ?: response.second()
                else -> response.second()
            }
            errorHandler.handleError(e, { "Query error" }, LogLevel.DEBUG)
        }
    }

    while (true) {
        withFrameNanos {
            if (!simulation.isStarted || System.nanoTime() - simulation.lastTickStartNanos < 1.667e7) /* 60 FPS */ {
                return@withFrameNanos
            }
            simulation.lastTickStartNanos = System.nanoTime()

            withUnexpectedErrorHandling(errorHandler, { "Unexpected error in simulation runner" }) {
                if (System.nanoTime() - dataStream.lastFetchedNanos > 5e7 /* 50ms */) fetchNewData()
                if (simulation.isEmpty() || simulation.alpha() < simulation.alphaMin()) return@withUnexpectedErrorHandling
                simulation.tick()
            }
        }
    }
}
