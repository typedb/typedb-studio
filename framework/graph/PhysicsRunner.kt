/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.framework.graph

import androidx.compose.runtime.withFrameMillis
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.common.util.Message
import kotlinx.coroutines.Job
import mu.KotlinLogging

// TODO: Should extend FixedScheduleRunner
class PhysicsRunner constructor(private val graphArea: GraphArea) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    suspend fun launch() {
        while (true) {
            withFrameMillis {
                return@withFrameMillis if (isReadyToStep()) {
                    graphArea.coroutineScope.launchAndHandle(StudioState.notification, LOGGER) { step() }
                } else Job()
            }.join()
        }
    }

    private fun isReadyToStep(): Boolean {
        return !graphArea.graph.physics.isStepRunning.get()
    }

    private fun step() {
        try {
            graphArea.graphBuilder.dumpTo(graphArea.graph)
            graphArea.graph.physics.step()
        } catch (e: Exception) {
            StudioState.notification.systemError(LOGGER, e, Message.Visualiser.UNEXPECTED_ERROR)
            graphArea.graph.physics.terminate()
        }
    }
}