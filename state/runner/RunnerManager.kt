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

package com.vaticle.typedb.studio.state.runner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.util.concurrent.LinkedBlockingDeque

class RunnerManager {

    var activeRunner: Runner? by mutableStateOf(null)
    val runners: SnapshotStateList<Runner> = mutableStateListOf()
    private val onLaunch = LinkedBlockingDeque<(Runner) -> Unit>()

    fun clone(): RunnerManager {
        val newRunnerMgr = RunnerManager()
        newRunnerMgr.activeRunner = this.activeRunner
        newRunnerMgr.runners.addAll(this.runners)
        newRunnerMgr.onLaunch.addAll(onLaunch)
        return newRunnerMgr
    }

    fun numberOf(runner: Runner): Int {
        return runners.indexOf(runner) + 1
    }

    fun onLaunch(function: (Runner) -> Unit) {
        this.onLaunch.push(function)
    }

    fun isActive(runner: Runner): Boolean {
        return runner == activeRunner
    }

    fun activate(runner: Runner) {
        activeRunner = runner
    }

    fun launch(runner: Runner, onComplete: () -> Unit) {
        activeRunner = runner
        if (runners.isEmpty() || runners.all { it.isSaved }) runners.add(runner)
        else runners[runners.indexOf(runners.first { !it.isSaved })] = runner
        onLaunch.forEach { it(runner) }
        runner.onComplete { onComplete() }
        runner.launch()
    }

    fun delete(runner: Runner) {
        if (activeRunner == runner) {
            val i = runners.indexOf(activeRunner)
            activeRunner = if (runners.size == 1) null
            else if (i > 0) runners[i - 1]
            else runners[0]
        }
        runners.remove(runner)
    }

    fun reset() {
        activeRunner = null
        runners.clear()
        onLaunch.clear()
    }
}