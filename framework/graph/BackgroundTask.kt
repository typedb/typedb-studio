/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.graph

import androidx.compose.runtime.withFrameMillis

abstract class BackgroundTask(private val runIntervalMs: Int) {
    private var lastRunDoneTime = System.currentTimeMillis()

    suspend fun launch() {
        while (true) {
            withFrameMillis {
                if (System.currentTimeMillis() - lastRunDoneTime > runIntervalMs && canRun()) {
                    run()
                    lastRunDoneTime = System.currentTimeMillis()
                }
            }
        }
    }

    protected open fun canRun(): Boolean {
        return true
    }

    protected abstract fun run()
}
