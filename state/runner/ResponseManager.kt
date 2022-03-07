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

class ResponseManager {

    internal val log: Response.Log = Response.Log()
    internal val graphs: MutableList<Response.Graph> =
        mutableStateListOf(Response.Graph(), Response.Graph()) // TODO: null
    internal val tables: MutableList<Response.Table> = mutableStateListOf(Response.Table()) // TODO: null
    val hasMultipleGraphs get() = graphs.size > 1
    val hasMultipleTables get() = tables.size > 1
    var active: Response by mutableStateOf(log)
    val responses: List<Response> get() = listOf(log, *graphs.toTypedArray(), *tables.toTypedArray())

    fun isActive(response: Response): Boolean {
        return active == response
    }

    fun activate(response: Response) {
        active = response
    }

    fun numberOf(response: Response.Graph): Int {
        return graphs.indexOf(response) + 1
    }

    fun numberOf(response: Response.Table): Int {
        return tables.indexOf(response) + 1
    }
}