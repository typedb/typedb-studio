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
import java.lang.IllegalStateException

class RunnerManager {

    private var lastRunner: TransactionRunner? by mutableStateOf(null)
    private var activeRunner: TransactionRunner? by mutableStateOf(null)
    private val savedRunners: MutableList<TransactionRunner> = mutableStateListOf()
    val runners: List<TransactionRunner> get() = savedRunners + (activeRunner?.let { listOf(it) } ?: listOf())


    fun indexOf(runner: TransactionRunner): Int {
        return if (savedRunners.contains(runner)) savedRunners.indexOf(runner)
        else if (lastRunner == runner) savedRunners.size + 1
        else throw IllegalStateException()
    }

    fun isActive(runner: TransactionRunner): Boolean {
        return runner == activeRunner
    }

    fun activate(runner: TransactionRunner) {
        activeRunner = runner
    }

    fun register(newRunner: TransactionRunner) {
        lastRunner = newRunner
        activeRunner = newRunner
    }

    fun saveLast() {
        lastRunner?.let { savedRunners.add(it) }
        lastRunner = null
    }

    fun delete(runner: TransactionRunner) {
        if (lastRunner == runner) lastRunner = null
        if (activeRunner == runner) activeRunner = null
        savedRunners.remove(runner)
    }
}