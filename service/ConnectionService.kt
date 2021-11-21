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

package com.vaticle.typedb.studio.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.TypeDBClient
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.common.notification.Error
import com.vaticle.typedb.studio.common.notification.Message.Connection.Companion.UNABLE_TO_CONNECT
import com.vaticle.typedb.studio.common.notification.Message.Connection.Companion.UNEXPECTED_ERROR
import mu.KotlinLogging

class ConnectionService {

    companion object {
        val LOG = KotlinLogging.logger {}
    }

    enum class Status { DISCONNECTED, CONNECTED, CONNECTING }

    private val sessionType = TypeDBSession.Type.DATA
    private var _database: String? by mutableStateOf(null)

    var client: TypeDBClient? = null
    var status: Status by mutableStateOf(Status.DISCONNECTED)
    var openDialog: Boolean by mutableStateOf(false)
    var databases: List<String> by mutableStateOf(emptyList()); private set
    var session: TypeDBSession? by mutableStateOf(null); private set

    fun isConnected(): Boolean {
        return status == Status.CONNECTED
    }

    fun isDisconnected(): Boolean {
        return status == Status.DISCONNECTED
    }

    fun getDatabase(): String? {
        return _database
    }

    fun setDatabase(database: String) {
        _database = database
        session?.close()
        this.session = client!!.session(this._database, sessionType)
    }

    fun refreshDatabases() {
        client?.let { c -> databases = c.databases().all().map { d -> d.name() } }
    }

    fun tryConnectToTypeDB(address: String) {
        status = Status.CONNECTING
        try {
            client = TypeDB.coreClient(address)
            status = Status.CONNECTED
        } catch (e: TypeDBClientException) {
            status = Status.DISCONNECTED
            Service.notifier.userError(Error.fromUser(UNABLE_TO_CONNECT), LOG)
        } catch (e: Exception) {
            status = Status.DISCONNECTED
            Service.notifier.systemError(Error.fromSystem(e, UNEXPECTED_ERROR), LOG)
        }
    }

    fun tryConnectToTypeDBCluster(
        address: String, username: String, password: String, caPath: String
    ) {
        status = Status.CONNECTING
        TODO("Not yet implemented")
    }

    fun disconnect() {
        status = Status.DISCONNECTED
        TODO("Not yet implemented")
    }
}