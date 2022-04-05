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
import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.state.common.AtomicBooleanState
import com.vaticle.typedb.studio.state.common.Message
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.FAILED_TO_OPEN_SESSION
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.SESSION_CLOSED_ON_SERVER
import com.vaticle.typedb.studio.state.notification.NotificationManager
import mu.KotlinLogging

class SessionState(
    private val connection: Connection,
    private val notificationMgr: NotificationManager
) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    var type: TypeDBSession.Type by mutableStateOf(TypeDBSession.Type.DATA); internal set
    val isSchema: Boolean get() = type == TypeDBSession.Type.SCHEMA
    val isData: Boolean get() = type == TypeDBSession.Type.DATA
    val isOpen get() = isOpenAtomic.state
    val database: String? get() = _session?.database()?.name()
    var transaction = TransactionState(this, notificationMgr)
    private val isOpenAtomic = AtomicBooleanState(false)
    private var _session: TypeDBSession? by mutableStateOf(null); private set

    internal fun tryOpen(database: String, type: TypeDBSession.Type) {
        if (isOpen && database == database && this.type == type) return
        close()
        try {
            _session = connection.client.session(database, type).apply { onClose { close(SESSION_CLOSED_ON_SERVER) } }
            this.type = type
            isOpenAtomic.set(true)
        } catch (exception: TypeDBClientException) {
            notificationMgr.userError(LOGGER, FAILED_TO_OPEN_SESSION, database)
            isOpenAtomic.set(false)
        }
    }

    internal fun transaction(type: TypeDBTransaction.Type, options: TypeDBOptions): TypeDBTransaction? {
        return _session?.transaction(type, options)
    }

    internal fun close(message: Message? = null, vararg params: Any) {
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            transaction.close()
            _session?.close()
            _session = null
            message?.let { notificationMgr.userError(LOGGER, it, *params) }
        }
    }
}