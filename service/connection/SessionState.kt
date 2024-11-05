/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.typedb.studio.service.common.NotificationService
import com.typedb.studio.service.common.PreferenceService
import com.typedb.studio.service.common.atomic.AtomicBooleanState
import com.typedb.studio.service.common.util.Message
import com.typedb.studio.service.common.util.Message.Connection.Companion.FAILED_TO_OPEN_SESSION
import com.typedb.studio.service.common.util.Message.Connection.Companion.SESSION_CLOSED_ON_SERVER
import com.typedb.studio.service.common.util.Message.Connection.Companion.SESSION_REOPENED
import com.vaticle.typedb.driver.api.TypeDBOptions
import com.vaticle.typedb.driver.api.TypeDBSession
import com.vaticle.typedb.driver.api.TypeDBTransaction
import com.vaticle.typedb.driver.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.driver.common.exception.TypeDBDriverException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import mu.KotlinLogging

class SessionState constructor(
    internal val driver: DriverState,
    private val notificationSrv: NotificationService,
    preferenceSrv: PreferenceService
) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    var type: TypeDBSession.Type by mutableStateOf(TypeDBSession.Type.DATA); internal set
    val isSchema: Boolean get() = type == TypeDBSession.Type.SCHEMA
    val isData: Boolean get() = type == TypeDBSession.Type.DATA
    val isOpen get() = isOpenAtomic.state
    var database: String? by mutableStateOf(null)
    val transaction = TransactionState(this, notificationSrv, preferenceSrv)
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
            _session.set(driver.session(database, type))
            if (_session.get()?.isOpen == true) {
                this.type = type
                opened(database)
                val session = _session.get()!!
                session.onClose { closed(SESSION_CLOSED_ON_SERVER) }
                session.onReopen { opened(database, SESSION_REOPENED) }
            } else isOpenAtomic.set(false)
        } catch (exception: TypeDBDriverException) {
            notificationSrv.userError(LOGGER, FAILED_TO_OPEN_SESSION, type, database)
            isOpenAtomic.set(false)
        }
    }

    private fun opened(database: String, message: Message? = null, vararg params: Any) {
        this.database = database
        isOpenAtomic.set(true)
        message?.let { notificationSrv.userWarning(LOGGER, it, *params) }
        onOpen.forEach { it() }
    }

    fun typeSchema(): String? = database?.let { driver.tryFetchTypeSchema(it) }

    fun transaction(type: TypeDBTransaction.Type = READ, options: TypeDBOptions? = null): TypeDBTransaction? {
        return if (!isOpenAtomic.state) null
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

    internal fun close() {
        if (!isResetting.get() && isOpenAtomic.compareAndSet(expected = true, new = false)) {
            reset()
            closed()
        }
    }

    private fun closed(message: Message? = null, vararg params: Any) {
        if (!isResetting.get() && isOpenAtomic.compareAndSet(expected = true, new = false)) {
            database = null
            transaction.close()
            message?.let { notificationSrv.userError(LOGGER, it, *params) }
            onClose.forEach { it() }
        }
    }
}
