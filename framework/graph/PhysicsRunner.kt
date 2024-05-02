/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
            graphArea.graphBuilder.dumpTo(graphArea.graph)
            graphArea.graph.physics.step()
        } catch (e: Exception) {
            Service.notification.systemError(LOGGER, e, UNEXPECTED_ERROR)
            graphArea.graph.physics.terminate()
        }
    }
}
