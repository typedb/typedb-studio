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
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.common.api.TransactionState
import com.vaticle.typedb.studio.state.common.atomic.AtomicBooleanState
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.FAILED_TO_OPEN_TRANSACTION
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.FAILED_TO_RUN_QUERY
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.TRANSACTION_CLOSED_IN_QUERY
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.TRANSACTION_CLOSED_ON_SERVER
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.TRANSACTION_COMMIT
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.TRANSACTION_ROLLBACK
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.state.resource.Runner
import java.util.concurrent.atomic.AtomicBoolean
import mu.KotlinLogging

class TransactionStateImpl constructor(
    private val session: SessionStateImpl,
    private val notificationMgr: NotificationManager
) : TransactionState {

    companion object {
        internal const val ONE_HOUR_IN_MILLS = 60 * 60 * 1_000
        private val LOGGER = KotlinLogging.logger {}
    }

    class ConfigState constructor(
        private val activatedFn: (value: Boolean) -> Boolean,
        private val enabledFn: () -> Boolean,
    ) {
        private var value by mutableStateOf(false)
        val activated get() = activatedFn(value)
        val enabled get() = enabledFn()

        fun toggle() {
            value = !value
        }
    }

    var type by mutableStateOf(TypeDBTransaction.Type.READ); internal set
    val isRead get() = type.isRead
    val isWrite get() = type.isWrite
    val isOpen get() = isOpenAtomic.state
    val hasRunningQuery get() = hasRunningQueryAtomic.state
    private val stopSignal = AtomicBoolean(false)
    private var hasRunningQueryAtomic = AtomicBooleanState(false)
    private val isOpenAtomic = AtomicBooleanState(false)
    private var _transaction: TypeDBTransaction? by mutableStateOf(null)

    val snapshot = ConfigState(
        activatedFn = { it || type.isWrite },
        enabledFn = { session.isOpen && !type.isWrite }
    )
    val infer = ConfigState(
        activatedFn = { it && !type.isWrite },
        enabledFn = { session.isOpen && !type.isWrite }
    )
    val explain = ConfigState(
        activatedFn = { it && infer.activated && snapshot.activated },
        enabledFn = { session.isOpen && infer.activated && snapshot.activated }
    )

    internal fun sendStopSignal() {
        stopSignal.set(true)
    }

    private fun tryOpen() {
        if (isOpen) return
        try {
            val options = TypeDBOptions.core().infer(infer.activated)
                .explain(infer.activated).transactionTimeoutMillis(ONE_HOUR_IN_MILLS)
            _transaction = session.transaction(type, options)!!.apply {
                onClose { close(TRANSACTION_CLOSED_ON_SERVER, it?.message ?: "Unknown") }
            }
            isOpenAtomic.set(true)
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_OPEN_TRANSACTION, e.message ?: "Unknown")
            isOpenAtomic.set(false)
            hasRunningQueryAtomic.set(false)
        }
    }

    internal fun runQuery(resource: Resource.Runnable, content: String) {
        if (hasRunningQueryAtomic.compareAndSet(expected = false, new = true)) {
            try {
                stopSignal.set(false)
                tryOpen()
                if (isOpen) resource.runner.launch(Runner(_transaction!!, content, stopSignal)) {
                    if (!snapshot.activated) close()
                    else if (!isOpen) close(TRANSACTION_CLOSED_IN_QUERY)
                    stopSignal.set(false)
                    hasRunningQueryAtomic.set(false)
                }
            } catch (e: Exception) {
                notificationMgr.userError(LOGGER, FAILED_TO_RUN_QUERY, e.message ?: e)
                hasRunningQueryAtomic.set(false)
            }
        }
    }

    internal fun commit() {
        sendStopSignal()
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            _transaction?.commit()
            _transaction = null
            if (session.type == TypeDBSession.Type.SCHEMA) {
                session.resetSchemaReadTx()
                session.onSchemaWrite?.let { it() }
            }
            notificationMgr.info(LOGGER, TRANSACTION_COMMIT)
        }
    }

    internal fun rollback() {
        sendStopSignal()
        _transaction?.rollback()
        notificationMgr.userWarning(LOGGER, TRANSACTION_ROLLBACK)
    }

    internal fun close(message: Message? = null, vararg params: Any) {
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            sendStopSignal()
            _transaction?.close()
            _transaction = null
            hasRunningQueryAtomic.set(false)
            message?.let { notificationMgr.userError(LOGGER, it, *params) }
        }
    }
}