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
import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.TypeDBClient
import com.vaticle.typedb.client.api.TypeDBCredential
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBSession.Type.DATA
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.state.app.DialogManager
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.common.AtomicBooleanState
import com.vaticle.typedb.studio.state.common.AtomicIntegerState
import com.vaticle.typedb.studio.state.common.AtomicReferenceState
import com.vaticle.typedb.studio.state.common.Message
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.FAILED_TO_CREATE_DATABASE
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.FAILED_TO_CREATE_DATABASE_DUE_TO_DUPLICATE
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.FAILED_TO_DELETE_DATABASE
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.UNABLE_TO_CONNECT
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.state.connection.ClientState.Status.CONNECTED
import com.vaticle.typedb.studio.state.connection.ClientState.Status.CONNECTING
import com.vaticle.typedb.studio.state.connection.ClientState.Status.DISCONNECTED
import com.vaticle.typedb.studio.state.resource.Resource
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class ClientState constructor(private val notificationMgr: NotificationManager) {

    enum class Status { DISCONNECTED, CONNECTED, CONNECTING }
    enum class Mode { SCRIPT, INTERACTIVE }

    companion object {
        private const val DATABASE_LIST_REFRESH_RATE_MS = 100
        private val LOGGER = KotlinLogging.logger {}
    }

    val connectServerDialog = DialogManager.Base()
    val selectDatabaseDialog = DialogManager.Base()
    val manageDatabasesDialog = DialogManager.Base()
    val status: Status get() = statusAtomic.state
    val isConnected: Boolean get() = status == CONNECTED
    val isConnecting: Boolean get() = status == CONNECTING
    val isDisconnected: Boolean get() = status == DISCONNECTED
    var address: String? by mutableStateOf(null)
    var username: String? by mutableStateOf(null)
    var mode: Mode by mutableStateOf(Mode.INTERACTIVE)
    val isScriptMode: Boolean get() = mode == Mode.SCRIPT
    val isInteractiveMode: Boolean get() = mode == Mode.INTERACTIVE
    val hasRunningQuery get() = session.transaction.hasRunningQuery
    val hasRunningCommand get() = hasRunningCommandAtomic.state || runningClosingCommands.state > 0
    val isReadyToRunQuery get() = session.isOpen && !hasRunningQuery && !hasRunningCommand
    var databaseList: List<String> by mutableStateOf(emptyList()); private set
    val session = SessionState(this, notificationMgr)
    private val statusAtomic = AtomicReferenceState<Status>(DISCONNECTED)
    private var _client: TypeDBClient? by mutableStateOf(null)
    private var hasRunningCommandAtomic = AtomicBooleanState(false)
    private var runningClosingCommands = AtomicIntegerState(0)
    private var databaseListRefreshedTime = System.currentTimeMillis()
    private val asyncDepth = AtomicInteger(0)

    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    fun tryConnectToTypeDB(address: String) {
        tryConnect(address, null) { TypeDB.coreClient(address) }
    }

    fun tryConnectToTypeDBCluster(address: String, username: String, password: String, tlsEnabled: Boolean) {
        tryConnectToTypeDBCluster(address, username, TypeDBCredential(username, password, tlsEnabled))
    }

    fun tryConnectToTypeDBCluster(address: String, username: String, password: String, caPath: String) {
        tryConnectToTypeDBCluster(address, username, TypeDBCredential(username, password, Path.of(caPath)))
    }

    private fun tryConnectToTypeDBCluster(address: String, username: String, credentials: TypeDBCredential) {
        tryConnect(address, username) { TypeDB.clusterClient(address, credentials) }
    }

    private fun tryConnect(newAddress: String, newUsername: String?, clientConstructor: () -> TypeDBClient) {
        coroutineScope.launch {
            this@ClientState.close()
            statusAtomic.set(CONNECTING)
            try {
                address = newAddress
                username = newUsername
                _client = clientConstructor()
                statusAtomic.set(CONNECTED)
            } catch (e: TypeDBClientException) {
                statusAtomic.set(DISCONNECTED)
                notificationMgr.userError(LOGGER, UNABLE_TO_CONNECT)
            } catch (e: java.lang.Exception) {
                statusAtomic.set(DISCONNECTED)
                notificationMgr.systemError(LOGGER, e, UNEXPECTED_ERROR)
            }
        }
    }

    private fun runAsyncCommand(function: () -> Unit) {
        val depth = asyncDepth.incrementAndGet()
        assert(depth == 1) { "You should not call runAsyncCommand nested in each other" }
        if (hasRunningCommandAtomic.compareAndSet(expected = false, new = true)) {
            coroutineScope.launch {
                try {
                    function()
                } catch (e: Exception) {
                    notificationMgr.systemError(LOGGER, e, UNEXPECTED_ERROR)
                } finally {
                    hasRunningCommandAtomic.set(false)
                    asyncDepth.decrementAndGet()
                }
            }
        }
    }

    private fun runAsyncClosingCommand(function: () -> Unit) {
        val depth = runningClosingCommands.incrementAndGet()
        assert(depth == 1) { "You should not call runAsyncClosingCommand nested in each other" }
        coroutineScope.launch {
            try {
                function()
            } catch (e: Exception) {
                notificationMgr.systemError(LOGGER, e, UNEXPECTED_ERROR)
            } finally {
                runningClosingCommands.decrementAndGet()
            }
        }
    }

    fun sendStopSignal() {
        session.transaction.sendStopSignal()
    }

    fun tryUpdateTransactionType(type: TypeDBTransaction.Type) = runAsyncCommand {
        if (session.transaction.type == type) return@runAsyncCommand
        session.transaction.close()
        session.transaction.type = type
    }

    fun tryUpdateSessionType(type: TypeDBSession.Type) {
        if (session.type == type) return
        tryOpenSession(session.database!!, type)
    }

    fun tryOpenSession(database: String) = tryOpenSession(database, session.type)

    private fun tryOpenSession(database: String, type: TypeDBSession.Type) = runAsyncCommand {
        session.tryOpen(database, type)
    }

    fun refreshDatabaseList() = runAsyncCommand { refreshDatabaseListFn() }

    private fun refreshDatabaseListFn() {
        if (System.currentTimeMillis() - databaseListRefreshedTime < DATABASE_LIST_REFRESH_RATE_MS) return
        _client?.let { c -> databaseList = c.databases().all().map { d -> d.name() }.sorted() }
        databaseListRefreshedTime = System.currentTimeMillis()
    }

    fun mayRun(resource: Resource.Runnable, content: String = resource.runContent) {
        if (!isReadyToRunQuery) return
        if (isScriptMode) runScript(resource, content)
        else if (isInteractiveMode) runQuery(resource, content)
        else throw IllegalStateException("Unrecognised TypeDB Studio run mode")
    }

    private fun runScript(resource: Resource.Runnable, content: String = resource.runContent) = runAsyncCommand {
        // TODO
    }

    private fun runQuery(resource: Resource.Runnable, content: String = resource.runContent) = runAsyncCommand {
        session.transaction.runQuery(resource, content)
    }

    fun tryCreateDatabase(database: String, onSuccess: () -> Unit) = runAsyncCommand {
        refreshDatabaseListFn()
        if (!databaseList.contains(database)) {
            try {
                _client?.databases()?.create(database)
                refreshDatabaseListFn()
                onSuccess()
            } catch (e: Exception) {
                notificationMgr.userError(LOGGER, FAILED_TO_CREATE_DATABASE, database, e.message ?: e.toString())
            }
        } else notificationMgr.userError(LOGGER, FAILED_TO_CREATE_DATABASE_DUE_TO_DUPLICATE, database)
    }

    fun tryDeleteDatabase(database: String) = runAsyncCommand {
        try {
            if (session.database == database) session.close()
            _client?.databases()?.get(database)?.delete()
            refreshDatabaseListFn()
        } catch (e: Exception) {
            notificationMgr.userWarning(LOGGER, FAILED_TO_DELETE_DATABASE, database, e.message ?: e.toString())
        }
    }

    fun session(database: String, type: TypeDBSession.Type = DATA): TypeDBSession? {
        return _client?.session(database, type)
    }

    fun commitTransaction() = runAsyncCommand { session.transaction.commit() }

    fun rollbackTransaction() = runAsyncCommand { session.transaction.rollback() }

    fun closeTransaction(message: Message? = null, vararg params: Any) = runAsyncClosingCommand {
        session.transaction.close(message, *params)
    }

    fun close() = runAsyncClosingCommand {
        if (statusAtomic.compareAndSet(expected = CONNECTED, new = DISCONNECTED)) {
            session.close()
            _client?.close()
            _client = null
        }
    }
}