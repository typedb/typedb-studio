/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.service.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap

class StatusService {

    /**
     * The order of keys defined in this enum determine the order in which they
     * get displayed on the status bar.
     */
    enum class Key {
        TEXT_CURSOR_POSITION,
        OUTPUT_RESPONSE_TIME,
        QUERY_RESPONSE_TIME,
    }

    val statuses: SnapshotStateMap<Key, String> = mutableStateMapOf()
    var loadingStatus: String by mutableStateOf("")

    fun publish(key: Key, status: String) {
        statuses[key] = status
    }

    fun clear(key: Key) {
        statuses.remove(key)
    }

    fun publishLoading(status: String) {
        loadingStatus = status
    }

    fun clearLoading() {
        loadingStatus = ""
    }
}
