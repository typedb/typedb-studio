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

import androidx.compose.runtime.mutableStateListOf

sealed interface RunnerOutput {

    class Log: RunnerOutput {

        data class Text(val type: Type, val text: String) {
            enum class Type { INFO, SUCCESS, ERROR, TYPEQL }
        }

        private val lines: MutableList<Text> = mutableStateListOf()

        internal fun append(type: Text.Type, text: String) {
            lines.add(Text(type, text))
        }
    }

    class Graph: RunnerOutput {
        // TODO
    }

    class Table: RunnerOutput {
        // TODO
    }
}