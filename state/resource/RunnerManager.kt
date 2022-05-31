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

package com.vaticle.typedb.studio.state.resource

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.studio.state.connection.QueryRunner
import java.util.concurrent.LinkedBlockingQueue

class RunnerManager {

    var activeRunner: QueryRunner? by mutableStateOf(null)
    val runners: SnapshotStateList<QueryRunner> = mutableStateListOf()
    private val saved: SnapshotStateList<QueryRunner> = mutableStateListOf()
    private val onLaunch = LinkedBlockingQueue<(QueryRunner) -> Unit>()

    fun clone(): RunnerManager {
        val newRunnerMgr = RunnerManager()
        newRunnerMgr.activeRunner = this.activeRunner
        newRunnerMgr.runners.addAll(this.runners)
        newRunnerMgr.onLaunch.addAll(onLaunch)
        return newRunnerMgr
    }

    fun numberOf(runner: QueryRunner): Int {
        return runners.indexOf(runner) + 1
    }

    fun onLaunch(function: (QueryRunner) -> Unit) {
        onLaunch.put(function)
    }

    fun isActive(runner: QueryRunner): Boolean {
        return runner == activeRunner
    }

    fun activate(runner: QueryRunner) {
        activeRunner = runner
    }

    fun isSaved(runner: QueryRunner): Boolean {
        return saved.contains(runner)
    }

    fun save(runner: QueryRunner) {
        saved.add(runner)
    }

    internal fun launch(runner: QueryRunner) {
        activeRunner = runner
        if (runners.isEmpty() || runners.all { saved.contains(it) }) runners.add(runner)
        else runners[runners.indexOf(runners.first { !saved.contains(it) })] = runner
        runner.launch()
        onLaunch.forEach { it(runner) }
    }

    fun close(runner: QueryRunner) {
        runner.close()
        if (activeRunner == runner) {
            val i = runners.indexOf(activeRunner)
            activeRunner = if (runners.size == 1) null
            else if (i > 0) runners[i - 1]
            else runners[0]
        }
        runners.remove(runner)
    }

    fun close() {
        activeRunner = null
        runners.forEach { it.close() }
        runners.clear()
        onLaunch.clear()
    }
}