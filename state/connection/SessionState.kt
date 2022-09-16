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

package com.vaticle.typedb.studio.state.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.common.atomic.AtomicBooleanState
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.FAILED_TO_OPEN_SESSION
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.SESSION_CLOSED_ON_SERVER
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import mu.KotlinLogging

class SessionState constructor(
    internal val client: ClientState,
    private val notificationMgr: NotificationManager
) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    var type: TypeDBSession.Type by mutableStateOf(TypeDBSession.Type.DATA); internal set
    val isSchema: Boolean get() = type == TypeDBSession.Type.SCHEMA
    val isData: Boolean get() = type == TypeDBSession.Type.DATA
    val isOpen get() = isOpenAtomic.state
    var database: String? by mutableStateOf(null)
    val transaction = TransactionState(this, notificationMgr)
    private var _session = AtomicReference<TypeDBSession>(null)
    private val isOpenAtomic = AtomicBooleanState(false)
    private val isResetting = AtomicBoolean(false)
    private val onOpen = LinkedBlockingQueue<() -> Unit>()
    private val onClose = LinkedBlockingQueue<() -> Unit>()

    fun onOpen(function: () -> Unit) = onOpen.put(function)
    fun onClose(function: () -> Unit) = onClose.put(function)

    internal fun tryOpen(database: String, type: TypeDBSession.Type) {
        if (isOpen && this.database == database && this.type == type) return
        if (this.database != database) close() else reset()
        try {
            _session.set(client.session(database, type)?.apply { onClose { close(SESSION_CLOSED_ON_SERVER) } })
            if (_session.get()?.isOpen == true) {
                this.database = database
                this.type = type
                isOpenAtomic.set(true)
                onOpen.forEach { it() }
            } else isOpenAtomic.set(false)
        } catch (exception: TypeDBClientException) {
            notificationMgr.userError(LOGGER, FAILED_TO_OPEN_SESSION, type, database)
            isOpenAtomic.set(false)
        }
    }

    fun typeSchema(): String? = _session.get()?.database()?.typeSchema()

    fun transaction(type: TypeDBTransaction.Type = READ, options: TypeDBOptions? = null): TypeDBTransaction? {
        return if (!isOpenAtomic.atomic.get()) null
        else if (options != null) _session.get()?.transaction(type, options)
        else _session.get()?.transaction(type)
    }

    private fun reset() {
        if (isResetting.compareAndSet(false, true)) {
            transaction.close()
            _session.get()?.close()
            _session.set(null)
            database = null
        }
        isResetting.set(false)
    }

    internal fun close(message: Message? = null, vararg params: Any) {
        if (!isResetting.get() && isOpenAtomic.compareAndSet(expected = true, new = false)) {
            onClose.forEach { it() }
            reset()
            message?.let { notificationMgr.userError(LOGGER, it, *params) }
        }
    }
}