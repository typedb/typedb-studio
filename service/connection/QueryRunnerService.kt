/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.concurrent.LinkedBlockingQueue

class QueryRunnerService {

    var active: QueryRunner? by mutableStateOf(null)
    val launched: SnapshotStateList<QueryRunner> = mutableStateListOf()
    val isRunning: Boolean get() = launched.any { it.isRunning.get() }
    private val saved: SnapshotStateList<QueryRunner> = mutableStateListOf()
    private val onLaunch = LinkedBlockingQueue<() -> Unit>()

    fun clone(): QueryRunnerService {
        val newRunnerSrv = QueryRunnerService()
        newRunnerSrv.active = this.active
        newRunnerSrv.launched.addAll(this.launched)
        newRunnerSrv.onLaunch.addAll(onLaunch)
        return newRunnerSrv
    }

    fun numberOf(runner: QueryRunner): Int {
        return launched.indexOf(runner) + 1
    }

    fun onLaunch(function: () -> Unit) {
        onLaunch.put(function)
    }

    fun isActive(runner: QueryRunner): Boolean {
        return runner == active
    }

    fun activate(runner: QueryRunner) {
        active = runner
    }

    fun isSaved(runner: QueryRunner): Boolean {
        return saved.contains(runner)
    }

    fun save(runner: QueryRunner) {
        saved.add(runner)
    }

    fun launched(runner: QueryRunner) {
        active = runner
        if (launched.isEmpty() || launched.all { saved.contains(it) }) launched.add(runner)
        else launched[launched.indexOf(launched.first { !saved.contains(it) })] = runner
        onLaunch.forEach { it() }
    }

    fun close(runner: QueryRunner) {
        runner.close()
        if (active == runner) {
            val i = launched.indexOf(active)
            active = if (launched.size == 1) null
            else if (i > 0) launched[i - 1]
            else launched[0]
        }
        launched.remove(runner)
        saved.remove(runner)
    }

    fun close() {
        active = null
        launched.forEach { it.close() }
        launched.clear()
        onLaunch.clear()
    }
}
