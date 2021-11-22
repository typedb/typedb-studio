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
import com.vaticle.typedb.studio.common.notification.Message.Connection.Companion.UNABLE_CREATE_SESSION
import com.vaticle.typedb.studio.common.notification.Message.Connection.Companion.UNABLE_TO_CONNECT
import com.vaticle.typedb.studio.common.notification.Message.Connection.Companion.UNEXPECTED_ERROR
import mu.KotlinLogging

class ConnectionService {

    companion object {
        private const val DATABASE_LIST_REFRESH_RATE_MS = 100
        private val SESSION_TYPE = TypeDBSession.Type.DATA
        private val LOGGER = KotlinLogging.logger {}
    }

    enum class Status { DISCONNECTED, CONNECTED, CONNECTING }

    private var databaseListRefreshedTime = System.currentTimeMillis()

    var client: TypeDBClient? = null
    var status: Status by mutableStateOf(Status.DISCONNECTED)
    var openDialog: Boolean by mutableStateOf(false)
    var databaseList: List<String> by mutableStateOf(listOf()); private set
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
            Service.notifier.userError(Error.fromUser(UNABLE_CREATE_SESSION, database), LOGGER)
        }
    }

    fun refreshDatabaseList() {
        if (System.currentTimeMillis() - databaseListRefreshedTime < DATABASE_LIST_REFRESH_RATE_MS) return
        client?.let { c -> databaseList = c.databases().all().map { d -> d.name() } }
        databaseListRefreshedTime = System.currentTimeMillis()
    }

    fun tryConnectToTypeDB(address: String) {
        status = Status.CONNECTING
        try {
            client = TypeDB.coreClient(address)
            status = Status.CONNECTED
        } catch (e: TypeDBClientException) {
            status = Status.DISCONNECTED
            Service.notifier.userError(Error.fromUser(UNABLE_TO_CONNECT), LOGGER)
        } catch (e: Exception) {
            status = Status.DISCONNECTED
            Service.notifier.systemError(Error.fromSystem(e, UNEXPECTED_ERROR), LOGGER)
        }
    }

    fun tryConnectToTypeDBCluster(address: String, username: String, password: String, caPath: String) {
        status = Status.CONNECTING
        TODO("Not yet implemented")
    }

    fun disconnect() {
        status = Status.DISCONNECTED
        closeSession()
        client!!.close()
        client = null
    }

    private fun closeSession() {
        session?.let { it.close(); session = null }
    }
}