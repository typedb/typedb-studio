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
import java.util.concurrent.LinkedBlockingDeque

class RunnerManager {

    private val onLaunch = LinkedBlockingDeque<(Runner) -> Unit>()
    private var lastRunner: Runner? by mutableStateOf(null)
    private val savedRunners: MutableList<Runner> = mutableStateListOf()
    var activeRunner: Runner? by mutableStateOf(null)
    val runners: List<Runner> get() = savedRunners + (lastRunner?.let { listOf(it) } ?: listOf())


    fun numberOf(runner: Runner): Int {
        return if (savedRunners.contains(runner)) savedRunners.indexOf(runner) + 1
        else if (lastRunner == runner) savedRunners.size + 1
        else throw IllegalStateException()
    }

    fun onLaunch(function: (Runner) -> Unit) {
        this.onLaunch.push(function)
    }

    fun isActive(runner: Runner): Boolean {
        return runner == activeRunner
    }

    fun isSaved(runner: Runner): Boolean {
        return savedRunners.contains(runner)
    }

    fun activate(runner: Runner) {
        activeRunner = runner
    }

    fun launch(runner: Runner, onComplete: () -> Unit) {
        lastRunner = runner
        activeRunner = runner
        onLaunch.forEach { it(runner) }
        runner.launch(onComplete)
    }

    fun save(runner: Runner) {
        if (savedRunners.contains(runner)) return
        savedRunners.add(runner)
        if (lastRunner == runner) lastRunner = null
    }

    fun delete(runner: Runner) {
        if (lastRunner == runner) lastRunner = null
        if (activeRunner == runner) activeRunner = null
        savedRunners.remove(runner)
    }

    fun reset() {
        lastRunner = null
        activeRunner = null
        savedRunners.clear()
        onLaunch.clear()
    }
}