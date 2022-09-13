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
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.app.PreferenceManager
import com.vaticle.typedb.studio.state.common.atomic.AtomicBooleanState
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.state.common.util.Message.Companion.UNKNOWN
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.FAILED_TO_OPEN_TRANSACTION
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.FAILED_TO_RUN_QUERY
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.TRANSACTION_CLOSED_IN_QUERY
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.TRANSACTION_CLOSED_ON_SERVER
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.TRANSACTION_COMMIT_FAILED
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.TRANSACTION_COMMIT_SUCCESSFULLY
import com.vaticle.typedb.studio.state.common.util.Message.Connection.Companion.TRANSACTION_ROLLBACK
import java.util.concurrent.LinkedBlockingQueue
import mu.KotlinLogging

class TransactionState constructor(
    private val session: SessionState,
    private val notificationMgr: NotificationManager,
    private val preferenceMgr: PreferenceManager
) {

    companion object {
        const val ONE_HOUR_IN_MILLS = 60 * 60 * 1_000
        private val LOGGER = KotlinLogging.logger {}
    }

    class ConfigState constructor(
        private val valueFn: (value: Boolean) -> Boolean,
        private val enabledFn: () -> Boolean
    ) {
        private var _value by mutableStateOf(false)
        val value get() = valueFn(_value)
        val enabled get() = enabledFn()

        fun toggle() {
            _value = !_value
        }
    }

    var type by mutableStateOf(TypeDBTransaction.Type.READ); internal set
    val isRead get() = type.isRead
    val isWrite get() = type.isWrite
    val isOpen get() = isOpenAtomic.state
    val hasStopSignal get() = hasStopSignalAtomic.state
    val hasRunningQuery get() = hasRunningQueryAtomic.state
    private val onSchemaWriteReset = LinkedBlockingQueue<() -> Unit>()
    internal val hasStopSignalAtomic = AtomicBooleanState(false)
    private var hasRunningQueryAtomic = AtomicBooleanState(false)
    private val isOpenAtomic = AtomicBooleanState(false)
    private var _transaction: TypeDBTransaction? by mutableStateOf(null)
    val transaction get() = _transaction

    val snapshot = ConfigState(
        valueFn = { it || type.isWrite },
        enabledFn = { session.isOpen && !type.isWrite }
    )
    val infer = ConfigState(
        valueFn = { it && !type.isWrite },
        enabledFn = { session.isOpen && !type.isWrite }
    )
    val explain = ConfigState(
        valueFn = { it && infer.value && snapshot.value },
        enabledFn = { session.isOpen && infer.value && snapshot.value }
    )

    fun onSchemaWriteReset(function: () -> Unit) = onSchemaWriteReset.put(function)

    internal fun sendStopSignal() = hasStopSignalAtomic.set(true)

    fun tryOpen(): TypeDBTransaction? {
        return if (!session.isOpen) {
            notificationMgr.userError(LOGGER, Message.Connection.SESSION_IS_CLOSED)
            null
        } else if (isOpen) _transaction
        else try {
            val options = typeDBOptions()
                .infer(infer.value)
                .explain(infer.value)
                .transactionTimeoutMillis(ONE_HOUR_IN_MILLS)
            session.transaction(type, options)!!.apply {
                onClose { close(TRANSACTION_CLOSED_ON_SERVER, it?.message ?: UNKNOWN) }
            }.also {
                isOpenAtomic.set(true)
                _transaction = it
            }
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_OPEN_TRANSACTION, e.message ?: UNKNOWN)
            isOpenAtomic.set(false)
            hasRunningQueryAtomic.set(false)
            null
        }
    }

    internal fun runQuery(content: String): QueryRunner? {
        return if (hasRunningQueryAtomic.compareAndSet(expected = false, new = true)) try {
            hasStopSignalAtomic.set(false)
            tryOpen()?.let {
                QueryRunner(this, notificationMgr, preferenceMgr, content) {
                    if (!snapshot.value) close()
                    else if (!isOpen) close(TRANSACTION_CLOSED_IN_QUERY)
                    hasStopSignalAtomic.set(false)
                    hasRunningQueryAtomic.set(false)
                }.also { it.launch() }
            }
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_RUN_QUERY, e.message ?: e)
            hasRunningQueryAtomic.set(false)
            null
        } else null
    }

    internal fun commit() {
        sendStopSignal()
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            try {
                _transaction?.commit()
                _transaction = null
                notificationMgr.info(LOGGER, TRANSACTION_COMMIT_SUCCESSFULLY)
                if (session.isSchema && isWrite) onSchemaWriteReset.forEach { it() }
            } catch (e: Exception) {
                notificationMgr.userError(LOGGER, TRANSACTION_COMMIT_FAILED, e.message ?: e)
            }
        }
    }

    internal fun rollback() {
        sendStopSignal()
        _transaction?.rollback()
        notificationMgr.userWarning(LOGGER, TRANSACTION_ROLLBACK)
        if (session.isSchema && isWrite) onSchemaWriteReset.forEach { it() }
    }

    internal fun close(message: Message? = null, vararg params: Any) {
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            sendStopSignal()
            _transaction?.close()
            _transaction = null
            hasRunningQueryAtomic.set(false)
            message?.let { notificationMgr.userError(LOGGER, it, *params) }
            if (session.isSchema && isWrite) onSchemaWriteReset.forEach { it() }
        }
    }

    internal fun typeDBOptions(): TypeDBOptions {
        return if (session.client.isCluster) TypeDBOptions.cluster() else TypeDBOptions.core()
    }
}
