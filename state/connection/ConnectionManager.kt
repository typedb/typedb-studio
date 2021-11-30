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
import com.vaticle.typedb.studio.state.notification.Error
import com.vaticle.typedb.studio.state.notification.Message
import com.vaticle.typedb.studio.state.notification.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext

class ConnectionManager(val notifier: Notifier) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    enum class Status { DISCONNECTED, CONNECTED, CONNECTING }

    var current: Connection? by mutableStateOf(null)
    var status: Status by mutableStateOf(Status.DISCONNECTED)
    var showWindow: Boolean by mutableStateOf(false)

    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    fun isConnected(): Boolean {
        return status == Status.CONNECTED
    }

    fun isConnecting(): Boolean {
        return status == Status.CONNECTING
    }

    fun isDisconnected(): Boolean {
        return status == Status.DISCONNECTED
    }

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

    @OptIn(DelicateCoroutinesApi::class)
    private fun tryConnect(newAddress: String, newUsername: String?, clientConstructor: () -> TypeDBClient) {
        coroutineScope.launch {
            disconnect()
            status = Status.CONNECTING
            try {
                current = Connection(clientConstructor(), newAddress, newUsername, notifier)
                status = Status.CONNECTED
            } catch (e: TypeDBClientException) {
                status = Status.DISCONNECTED
                notifier.userError(Error.fromUser(Message.Connection.UNABLE_TO_CONNECT), LOGGER)
            } catch (e: Exception) {
                status = Status.DISCONNECTED
                notifier.systemError(Error.fromSystem(e, Message.Connection.UNEXPECTED_ERROR), LOGGER)
            }
        }
    }

    fun disconnect() {
        status = Status.DISCONNECTED
        current?.close()
        current = null
    }
}