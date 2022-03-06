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

class OutputManager {

    internal val log: RunnerOutput.Log = RunnerOutput.Log()
    internal val graphs: MutableList<RunnerOutput.Graph> = mutableStateListOf(RunnerOutput.Graph(), RunnerOutput.Graph()) // TODO: null
    internal val tables: MutableList<RunnerOutput.Table> = mutableStateListOf(RunnerOutput.Table()) // TODO: null
    val hasMultipleGraphs get() = graphs.size > 1
    val hasMultipleTables get() = tables.size > 1
    var active: RunnerOutput by mutableStateOf(log)
    val outputs: List<RunnerOutput> get() = listOf(log, *graphs.toTypedArray(), *tables.toTypedArray())

    fun isActive(output: RunnerOutput): Boolean {
        return active == output
    }

    fun activate(output: RunnerOutput) {
        active = output
    }

    fun numberOf(output: RunnerOutput.Graph): Int {
        return graphs.indexOf(output) + 1
    }

    fun numberOf(output: RunnerOutput.Table): Int {
        return tables.indexOf(output) + 1
    }
}