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

package com.vaticle.typedb.studio.state.status

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap

class StatusManager {

    data class PrioritisedKey(val key: String, val priority: Int) : Comparable<PrioritisedKey> {
        override fun compareTo(other: PrioritisedKey): Int {
            return this.priority.compareTo(other.priority)
        }
    }

    var prioritisedKeys: List<PrioritisedKey> by mutableStateOf(listOf())
    val statuses: SnapshotStateMap<String, String> = mutableStateMapOf()
    var loadingStatus: String by mutableStateOf("")

    fun register(key: String, priority: Int) {
        deregister(key)
        val newKeys = prioritisedKeys + PrioritisedKey(key, priority)
        prioritisedKeys = newKeys.sortedBy { it.priority }
    }

    fun deregister(key: String) {
        statuses.remove(key)
        prioritisedKeys = prioritisedKeys.filter { it.key != key }
    }

    fun publish(key: String, status: String) {
        assert(prioritisedKeys.any { it.key == key })
        statuses[key] = status
    }

    fun clear(key: String) {
        statuses.remove(key)
    }

    fun publishLoading(status: String) {
        loadingStatus = status
    }

    fun clearLoading() {
        loadingStatus = ""
    }
}
