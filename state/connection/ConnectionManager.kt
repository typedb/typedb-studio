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
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.state.common.DialogManager
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.UNABLE_TO_CONNECT
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.state.notification.NotificationManager
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class ConnectionManager(private val notificationMgr: NotificationManager) {

    enum class Status { DISCONNECTED, CONNECTED, CONNECTING }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    val connectServerDialog = DialogManager.Base()
    val selectDatabaseDialog = DialogManager.Base()
    var current: Connection? by mutableStateOf(null)
    var status: Status by mutableStateOf(Status.DISCONNECTED)
    val hasSession: Boolean get() = isConnected && current!!.hasOpenSession
    val isInteractiveMode: Boolean get() = isConnected && current!!.isInteractiveMode
    val isScriptMode: Boolean get() = isConnected && current!!.isScriptMode
    val isConnected: Boolean get() = status == Status.CONNECTED
    val isConnecting: Boolean get() = status == Status.CONNECTING
    val isDisconnected: Boolean get() = status == Status.DISCONNECTED

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
            disconnect()
            status = Status.CONNECTING
            try {
                current = Connection(clientConstructor(), newAddress, newUsername, notificationMgr)
                status = Status.CONNECTED
            } catch (e: TypeDBClientException) {
                status = Status.DISCONNECTED
                notificationMgr.userError(LOGGER, UNABLE_TO_CONNECT)
            } catch (e: java.lang.Exception) {
                status = Status.DISCONNECTED
                notificationMgr.systemError(LOGGER, e, UNEXPECTED_ERROR)
            }
        }
    }

    fun disconnect() {
        status = Status.DISCONNECTED
        current?.close()
        current = null
    }
}