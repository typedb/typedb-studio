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

package com.vaticle.typedb.studio.state.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.concurrent.LinkedBlockingQueue

class RunnerManager {

    var active: QueryRunner? by mutableStateOf(null)
    val launched: SnapshotStateList<QueryRunner> = mutableStateListOf()
    private val saved: SnapshotStateList<QueryRunner> = mutableStateListOf()
    private val onLaunch = LinkedBlockingQueue<(QueryRunner) -> Unit>()

    fun clone(): RunnerManager {
        val newRunnerMgr = RunnerManager()
        newRunnerMgr.active = this.active
        newRunnerMgr.launched.addAll(this.launched)
        newRunnerMgr.onLaunch.addAll(onLaunch)
        return newRunnerMgr
    }

    fun numberOf(runner: QueryRunner): Int {
        return launched.indexOf(runner) + 1
    }

    fun onLaunch(function: (QueryRunner) -> Unit) {
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

    fun launch(runner: QueryRunner) {
        active = runner
        if (launched.isEmpty() || launched.all { saved.contains(it) }) launched.add(runner)
        else launched[launched.indexOf(launched.first { !saved.contains(it) })] = runner
        runner.launch()
        onLaunch.forEach { it(runner) }
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
    }

    fun close() {
        active = null
        launched.forEach { it.close() }
        launched.clear()
        onLaunch.clear()
    }
}