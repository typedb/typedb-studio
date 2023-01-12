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
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.vaticle.typedb.studio.service.common.util.Message.Visualiser.Companion.UNEXPECTED_ERROR
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
                    graphArea.coroutines.launchAndHandle(Service.notification, LOGGER) { step() }
                } else Job()
            }.join()
        }
    }

    private fun isReadyToStep(): Boolean {
        return !graphArea.graph.physics.isStepRunning.get()
    }

    private fun step() {
        try {
            graphArea.graphState.dumpTo(graphArea.graph)
            graphArea.graph.physics.step()
        } catch (e: Exception) {
            Service.notification.systemError(LOGGER, e, UNEXPECTED_ERROR)
            graphArea.graph.physics.terminate()
        }
    }
}