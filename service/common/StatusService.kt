/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.service.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.vaticle.typedb.studio.service.common.StatusService.Status.Type.INFO

class StatusService {

    /**
     * The order of keys defined in this enum determine the order in which they
     * get displayed on the status bar, from right to left.
     */
    enum class Key {
        TEXT_CURSOR_POSITION,
        OUTPUT_RESPONSE_TIME,
        QUERY_RESPONSE_TIME,
        SCHEMA_EXCEPTIONS,
    }

    data class Status(val key: Key, val message: String, val type: Type, val onClick: (() -> Unit)? = null) {
        enum class Type { INFO, WARNING, ERROR }
    }

    val statuses: SnapshotStateMap<Key, Status> = mutableStateMapOf()
    var loadingStatus: String by mutableStateOf("")

    fun publish(key: Key, status: String, type: Status.Type = INFO, onClick: (() -> Unit)? = null) {
        statuses[key] = Status(key, status, type, onClick)
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
