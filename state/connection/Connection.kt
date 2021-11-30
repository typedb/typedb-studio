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
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.state.notification.Error
import com.vaticle.typedb.studio.state.notification.Message.Connection.Companion.UNABLE_CREATE_SESSION
import com.vaticle.typedb.studio.state.notification.Message.Connection.Companion.UNABLE_TO_CONNECT
import com.vaticle.typedb.studio.state.notification.Message.Connection.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.state.notification.Notifier
import java.nio.file.Path
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Connection(private val notifier: Notifier) {

    companion object {
        private const val DATABASE_LIST_REFRESH_RATE_MS = 100
        private val SESSION_TYPE = TypeDBSession.Type.DATA
        private val LOGGER = KotlinLogging.logger {}
    }

    enum class Status { DISCONNECTED, CONNECTED, CONNECTING }

    private var databaseListRefreshedTime = System.currentTimeMillis()
    private var client: TypeDBClient? = null

    var status: Status by mutableStateOf(Status.DISCONNECTED)
    var showWindow: Boolean by mutableStateOf(false)
    var databaseList: List<String> by mutableStateOf(emptyList()); private set
    var address: String? by mutableStateOf(null)
    var username: String? by mutableStateOf(null)
    var session: TypeDBSession? by mutableStateOf(null); private set

    fun isConnected(): Boolean {
        return status == Status.CONNECTED
    }

    fun isDisconnected(): Boolean {
        return status == Status.DISCONNECTED
    }

    fun getDatabase(): String? {
        return session?.database()?.name()
    }

    fun setDatabase(database: String) {
        if (session?.database()?.name() == database) return
        closeSession()
        try {
            this.session = client!!.session(database, SESSION_TYPE)
        } catch (exception: TypeDBClientException) {
            notifier.userError(Error.fromUser(UNABLE_CREATE_SESSION, database), LOGGER)
        }
    }

    fun refreshDatabaseList() {
        if (System.currentTimeMillis() - databaseListRefreshedTime < DATABASE_LIST_REFRESH_RATE_MS) return
        client?.let { c -> databaseList = c.databases().all().map { d -> d.name() } }
        databaseListRefreshedTime = System.currentTimeMillis()
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
    private fun tryConnect(newAddres: String, newUsername: String?, clientConstructor: () -> TypeDBClient) {
        GlobalScope.launch { // We use GlobalScope because ConnectionService lifetime is also global
            status = Status.CONNECTING
            try {
                client = clientConstructor()
                address = newAddres
                username = newUsername
                status = Status.CONNECTED
            } catch (e: TypeDBClientException) {
                status = Status.DISCONNECTED
                notifier.userError(Error.fromUser(UNABLE_TO_CONNECT), LOGGER)
            } catch (e: Exception) {
                status = Status.DISCONNECTED
                notifier.systemError(Error.fromSystem(e, UNEXPECTED_ERROR), LOGGER)
            }
        }
    }

    fun disconnect() {
        status = Status.DISCONNECTED
        closeSession()
        address = null
        username = null
        client!!.close()
        client = null
    }

    private fun closeSession() {
        session?.let { it.close(); session = null }
    }
}
