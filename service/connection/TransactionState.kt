/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.typedb.driver.api.Transaction
import com.typedb.studio.service.common.NotificationService
import com.typedb.studio.service.common.PreferenceService
import com.typedb.studio.service.common.atomic.AtomicBooleanState
import com.typedb.studio.service.common.util.Message
import com.typedb.studio.service.common.util.Message.Companion.UNKNOWN
import com.typedb.studio.service.common.util.Message.Connection.Companion.CONNECTION_NOT_EXIST
import com.typedb.studio.service.common.util.Message.Connection.Companion.DATABASE_NOT_SELECTED
import com.typedb.studio.service.common.util.Message.Connection.Companion.FAILED_TO_OPEN_TRANSACTION
import com.typedb.studio.service.common.util.Message.Connection.Companion.FAILED_TO_RUN_QUERY
import com.typedb.studio.service.common.util.Message.Connection.Companion.TRANSACTION_CLOSED_IN_QUERY
import com.typedb.studio.service.common.util.Message.Connection.Companion.TRANSACTION_CLOSED_ON_SERVER
import com.typedb.studio.service.common.util.Message.Connection.Companion.TRANSACTION_COMMIT_FAILED
import com.typedb.studio.service.common.util.Message.Connection.Companion.TRANSACTION_COMMIT_SUCCESSFULLY
import com.typedb.studio.service.common.util.Message.Connection.Companion.TRANSACTION_ROLLBACK
import java.util.concurrent.LinkedBlockingQueue
import mu.KotlinLogging

class TransactionState(
    private val driver: DriverState,
    private val notificationSrv: NotificationService,
    private val preferenceSrv: PreferenceService
) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    var database: String? by mutableStateOf(null)
    var type by mutableStateOf(Transaction.Type.READ); internal set
    val isRead get() = type.isRead
    val isWrite get() = type.isWrite
    val isSchema get() = type.isSchema
    val isOpen get() = isOpenAtomic.state
    val hasStopSignal get() = hasStopSignalAtomic.state
    val hasRunningQuery get() = hasRunningQueryAtomic.state
    private val onSchemaWriteReset = LinkedBlockingQueue<() -> Unit>()
    private val hasStopSignalAtomic = AtomicBooleanState(false)
    private var hasRunningQueryAtomic = AtomicBooleanState(false)
    private val isOpenAtomic = AtomicBooleanState(false)
    private var _transaction: Transaction? by mutableStateOf(null)
    val transaction get() = _transaction

    fun onSchemaWriteReset(function: () -> Unit) = onSchemaWriteReset.put(function)

    internal fun sendStopSignal() = hasStopSignalAtomic.set(true)

    fun trySelectDatabase(database: String) {
        if (this.database == database) return
        else if (isOpen) close()
        this.database = database
    }

    fun tryOpen(): Transaction? = database?.let {
        if (isOpen) _transaction
        else try {
            driver.tryGet()?.let {
                it.transaction(database!!, type)!!.apply {
                    onClose { close(TRANSACTION_CLOSED_ON_SERVER, it?.message ?: UNKNOWN) }
                }.also {
                    isOpenAtomic.set(true)
                    _transaction = it
                }
            } ?: let {
                notificationSrv.userError(LOGGER, CONNECTION_NOT_EXIST)
                null
            }
        } catch (e: Exception) {
            notificationSrv.userError(LOGGER, FAILED_TO_OPEN_TRANSACTION, e.message ?: UNKNOWN)
            isOpenAtomic.set(false)
            hasRunningQueryAtomic.set(false)
            null
        }
    } ?: let {
        notificationSrv.userError(LOGGER, DATABASE_NOT_SELECTED)
        null
    }

    internal fun runQuery(content: String): QueryRunner? =
        if (hasRunningQueryAtomic.compareAndSet(expected = false, new = true)) try {
            hasStopSignalAtomic.set(false)
            tryOpen()?.let {
                QueryRunner(this, notificationSrv, preferenceSrv, content) {
                    if (!isOpen) close(TRANSACTION_CLOSED_IN_QUERY)
                    hasStopSignalAtomic.set(false)
                    hasRunningQueryAtomic.set(false)
                    mayExecOnSchemaWriteReset()
                }.also { it.launch() }
            }
        } catch (e: Exception) {
            notificationSrv.userError(LOGGER, FAILED_TO_RUN_QUERY, e.message ?: e)
            hasRunningQueryAtomic.set(false)
            null
        } else null

    private fun mayExecOnSchemaWriteReset() {
        if (isSchema) onSchemaWriteReset.forEach { it() }
    }

    internal fun commit() {
        sendStopSignal()
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            try {
                _transaction?.commit()
                _transaction = null
                notificationSrv.info(LOGGER, TRANSACTION_COMMIT_SUCCESSFULLY)
            } catch (e: Exception) {
                notificationSrv.userError(LOGGER, TRANSACTION_COMMIT_FAILED, e.message ?: e)
            } finally {
                mayExecOnSchemaWriteReset()
            }
        }
    }

    internal fun rollback() {
        sendStopSignal()
        _transaction?.rollback()
        notificationSrv.userWarning(LOGGER, TRANSACTION_ROLLBACK)
        mayExecOnSchemaWriteReset()
    }

    internal fun close(message: Message? = null, vararg params: Any) {
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            sendStopSignal()
            _transaction?.close()
            _transaction = null
            hasRunningQueryAtomic.set(false)
            message?.let { notificationSrv.userError(LOGGER, it, *params) }
            mayExecOnSchemaWriteReset()
        }
    }
}
