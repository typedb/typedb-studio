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
import com.vaticle.typedb.client.api.TypeDBClient
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.state.common.AtomicBooleanState
import com.vaticle.typedb.studio.state.common.AtomicIntegerState
import com.vaticle.typedb.studio.state.common.Message
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.FAILED_TO_CREATE_DATABASE
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.FAILED_TO_CREATE_DATABASE_DUE_TO_DUPLICATE
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.FAILED_TO_DELETE_DATABASE
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.FAILED_TO_OPEN_SESSION
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.FAILED_TO_OPEN_TRANSACTION
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.FAILED_TO_RUN_QUERY
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.TRANSACTION_CLOSED_IN_QUERY
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.TRANSACTION_ROLLBACK
import com.vaticle.typedb.studio.state.notification.NotificationManager
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.state.runner.Runner
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Connection internal constructor(
    private val client: TypeDBClient,
    val address: String,
    val username: String?,
    private val notificationMgr: NotificationManager
) {

    enum class Mode { SCRIPT, INTERACTIVE }

    companion object {
        private const val DATABASE_LIST_REFRESH_RATE_MS = 100
        private val LOGGER = KotlinLogging.logger {}
    }

    val isScriptMode: Boolean get() = mode == Mode.SCRIPT
    val isInteractiveMode: Boolean get() = mode == Mode.INTERACTIVE
    val isSchemaSession: Boolean get() = config.sessionType == TypeDBSession.Type.SCHEMA
    val isDataSession: Boolean get() = config.sessionType == TypeDBSession.Type.DATA
    val isReadTransaction: Boolean get() = config.transactionType.isRead
    val isWriteTransaction: Boolean get() = config.transactionType.isWrite
    val hasOpenSession: Boolean get() = session != null && session!!.isOpen
    val hasRunningQuery get() = hasRunningQueryAtomic.state
    val hasRunningCommand get() = hasRunningCommandAtomic.state || runningClosingCommands.state > 0
    val isReadyToRunQuery get() = hasOpenSession && !hasRunningQuery && !hasRunningCommand
    val database: String? get() = if (isInteractiveMode) session?.database()?.name() else null
    val config = TransactionConfig(this)
    var mode: Mode by mutableStateOf(Mode.INTERACTIVE)
    var isOpen = AtomicBooleanState(true)
    var databaseList: List<String> by mutableStateOf(emptyList()); private set
    var session: TypeDBSession? by mutableStateOf(null); private set
    var hasOpenTransaction: Boolean by mutableStateOf(false)
    var hasRunningQueryAtomic = AtomicBooleanState(false)
    var hasRunningCommandAtomic = AtomicBooleanState(false)
    var runningClosingCommands = AtomicIntegerState(0)
    private val hasStopSignal = AtomicBoolean(false)
    private var transaction: TypeDBTransaction? by mutableStateOf(null); private set
    private var databaseListRefreshedTime = System.currentTimeMillis()
    private val asyncDepth = AtomicInteger(0)
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    fun sendStopSignal() {
        hasStopSignal.set(true)
    }

    private fun runAsyncCommand(function: () -> Unit) {
        val depth = asyncDepth.incrementAndGet()
        assert(depth == 1) { "You should not call runAsyncCommand nested in each other" }
        if (hasRunningCommandAtomic.compareAndSet(expected = false, new = true)) {
            coroutineScope.launch {
                function()
                hasRunningCommandAtomic.set(false)
                asyncDepth.decrementAndGet()
            }
        }
    }

    private fun runAsyncClosingCommand(function: () -> Unit) {
        val depth = asyncDepth.incrementAndGet()
        assert(depth == 1) { "You should not call runAsyncCommand nested in each other" }
        runningClosingCommands.increment()
        coroutineScope.launch {
            function()
            runningClosingCommands.decrement()
            asyncDepth.decrementAndGet()
        }
    }

    fun tryUpdateTransactionType(type: TypeDBTransaction.Type) = runAsyncCommand {
        if (config.transactionType == type) return@runAsyncCommand
        closeTransactionFn()
        config.transactionType = type
    }

    fun tryUpdateSessionType(type: TypeDBSession.Type) {
        if (config.sessionType == type) return
        tryOpenSession(database!!, type)
    }

    fun tryOpenSession(database: String) {
        tryOpenSession(database, config.sessionType)
    }

    fun tryOpenSession(database: String, type: TypeDBSession.Type) = runAsyncCommand {
        if (hasOpenSession && session?.database()?.name() == database && session?.type() == type) return@runAsyncCommand
        closeSession()
        try {
            session = client.session(database, type)
            config.sessionType = type
        } catch (exception: TypeDBClientException) {
            notificationMgr.userError(LOGGER, FAILED_TO_OPEN_SESSION, database)
        }
    }

    fun refreshDatabaseList() = runAsyncCommand { refreshDatabaseListFn() }

    private fun refreshDatabaseListFn() {
        if (System.currentTimeMillis() - databaseListRefreshedTime < DATABASE_LIST_REFRESH_RATE_MS) return
        client.let { c -> databaseList = c.databases().all().map { d -> d.name() }.sorted() }
        databaseListRefreshedTime = System.currentTimeMillis()
    }

    fun mayRun(resource: Resource, content: String = resource.runContent) {
        if (!isReadyToRunQuery) return
        if (isScriptMode) runScript(resource, content)
        else if (isInteractiveMode) runQuery(resource, content)
        else throw IllegalStateException()
    }

    private fun runScript(resource: Resource, script: String = resource.runContent) = runAsyncCommand {
        // TODO
    }

    private fun runQuery(resource: Resource, queries: String = resource.runContent) = runAsyncCommand {
        if (hasRunningQueryAtomic.compareAndSet(expected = false, new = true)) {
            try {
                hasStopSignal.set(false)
                mayOpenTransaction()
                resource.runner.launch(Runner(transaction!!, queries, hasStopSignal)) {
                    if (!config.snapshotSelected) closeTransactionFn()
                    else if (!transaction!!.isOpen) closeTransactionFn(TRANSACTION_CLOSED_IN_QUERY)
                    hasStopSignal.set(false)
                    hasRunningQueryAtomic.set(false)
                }
            } catch (e: Exception) {
                notificationMgr.userError(LOGGER, FAILED_TO_RUN_QUERY, e.message ?: e)
                hasRunningQueryAtomic.set(false)
            }
        }
    }

    private fun mayOpenTransaction() {
        if (hasOpenTransaction) return
        try {
            transaction = session!!.transaction(config.transactionType, config.toTypeDBOptions())
            hasOpenTransaction = true
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_OPEN_TRANSACTION)
            hasOpenTransaction = false
        }
    }

    fun tryCreateDatabase(database: String, onSuccess: () -> Unit) = runAsyncCommand {
        refreshDatabaseListFn()
        if (!databaseList.contains(database)) {
            try {
                client.databases().create(database)
                refreshDatabaseListFn()
                onSuccess()
            } catch (e: Exception) {
                notificationMgr.userError(LOGGER, FAILED_TO_CREATE_DATABASE, database, e.message ?: e.toString())
            }
        } else notificationMgr.userError(LOGGER, FAILED_TO_CREATE_DATABASE_DUE_TO_DUPLICATE, database)
    }

    fun tryDeleteDatabase(database: String) = runAsyncCommand {
        try {
            if (this.database == database) closeSession()
            client.databases().get(database).delete()
            refreshDatabaseListFn()
        } catch (e: Exception) {
            notificationMgr.userWarning(LOGGER, FAILED_TO_DELETE_DATABASE, database, e.message ?: e.toString())
        }
    }

    fun commitTransaction() = runAsyncCommand {
        sendStopSignal()
        transaction?.commit()
        transaction = null
        hasOpenTransaction = false
        notificationMgr.info(LOGGER, Message.Connection.TRANSACTION_COMMIT)
    }

    fun rollbackTransaction() = runAsyncCommand {
        sendStopSignal()
        transaction?.rollback()
        notificationMgr.userWarning(LOGGER, TRANSACTION_ROLLBACK)
    }

    fun closeTransaction(message: Message? = null, vararg params: Any) = runAsyncClosingCommand {
        closeTransactionFn(message, params)
    }

    private fun closeTransactionFn(message: Message? = null, vararg params: Any) {
        if (transaction == null) return
        sendStopSignal()
        transaction?.close()
        transaction = null
        hasOpenTransaction = false
        message?.let { notificationMgr.userError(LOGGER, message, params) }
    }

    private fun closeSession() { // not async, always called from other async functions
        closeTransactionFn()
        session?.let { it.close(); session = null }
    }

    fun close() = runAsyncClosingCommand {
        if (isOpen.compareAndSet(expected = true, new = false)) {
            closeSession()
            client.close()
        }
    }
}
