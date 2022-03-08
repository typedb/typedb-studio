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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.AnnotatedString

sealed interface Response {

    class Log : Response {

        data class Entry(val type: Type, val text: String) {
            enum class Type { INFO, SUCCESS, ERROR, TYPEQL }
        }

        val lines: SnapshotStateList<AnnotatedString> = mutableStateListOf()
        var formatter: ((Entry) -> AnnotatedString) = { entry -> AnnotatedString(entry.text) }

        internal fun collect(type: Entry.Type, text: String) {
            text.split("\n").forEach { lines.add(formatter(Entry(type, it))) }
        }

        fun emptyLine() {
            lines.add(AnnotatedString(""))
        }
    }

    class Graph : Response {
        // TODO
    }

    class Table : Response {
        // TODO
    }
}