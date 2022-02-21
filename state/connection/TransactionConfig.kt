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

package com.vaticle.typedb.studio.state.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction

class TransactionConfig(private val connection: Connection) {

    var sessionType: TypeDBSession.Type by mutableStateOf(TypeDBSession.Type.DATA); internal set
    var transactionType: TypeDBTransaction.Type by mutableStateOf(TypeDBTransaction.Type.READ); internal set

    val keepAlive: Boolean get() = _keepAlive || transactionType.isWrite
    val keepAliveEnabled: Boolean get() = connection.hasSession() && !transactionType.isWrite
    private var _keepAlive: Boolean by mutableStateOf(false)

    val infer: Boolean get() = _infer && !transactionType.isWrite
    val inferEnabled: Boolean get() = connection.hasSession() && !transactionType.isWrite
    private var _infer: Boolean by mutableStateOf(false)

    val explain: Boolean get() = _explain && infer
    val explainEnabled: Boolean get() = connection.hasSession() && infer
    private var _explain: Boolean by mutableStateOf(false)

    fun toggleKeepAlive() {
        _keepAlive = !_keepAlive
    }

    fun toggleInfer() {
        _infer = !_infer
    }

    fun toggleExplain() {
        _explain = !_explain
    }
}