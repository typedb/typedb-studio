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
import com.vaticle.typedb.client.api.connection.TypeDBClient
import com.vaticle.typedb.client.api.connection.TypeDBSession
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import java.lang.Exception

class ConnectionService {

    enum class Status { DISCONNECTED, CONNECTED, CONNECTING }

    private val sessionType = TypeDBSession.Type.DATA
    private var _database: String? by mutableStateOf(null)

    var client: TypeDBClient? = null
    var status: Status by mutableStateOf(Status.DISCONNECTED)
    var openDialog: Boolean by mutableStateOf(false)
    var databases: List<String> by mutableStateOf(listOf("database 1", "database 2", "database 3")); private set
    var session: TypeDBSession? by mutableStateOf(null); private set

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
            // TODO: StudioState.error.reportUserError(e) {}
        } catch (e: Exception) {
            status = Status.DISCONNECTED

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