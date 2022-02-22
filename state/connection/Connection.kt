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
import com.vaticle.typedb.studio.state.common.Message.Connection.Companion.UNABLE_CREATE_SESSION
import com.vaticle.typedb.studio.state.notification.NotificationManager
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

    val config = TransactionConfig(this)
    var isOpen: Boolean by mutableStateOf(true)
    var databaseList: List<String> by mutableStateOf(emptyList()); private set
    var session: TypeDBSession? by mutableStateOf(null); private set
    var transaction: TypeDBTransaction? by mutableStateOf(null); private set
    var mode: Mode by mutableStateOf(Mode.INTERACTIVE)
    val isScriptMode: Boolean get() = mode == Mode.SCRIPT
    val isInteractiveMode: Boolean get() = mode == Mode.INTERACTIVE
    val hasWrites: Boolean get() = false // TODO: implement tx.hasUncommittedWrites

    private var databaseListRefreshedTime = System.currentTimeMillis()

    fun hasSession(): Boolean {
        return session != null && session!!.isOpen
    }

    fun hasTransaction(): Boolean {
        return transaction != null && transaction!!.isOpen
    }

    fun getDatabase(): String? {
        return if (isInteractiveMode) session?.database()?.name() else null
    }

    fun updateTransactionType(type: TypeDBTransaction.Type) {
        if (config.transactionType == type) return
        transaction?.let {
            it.close()
            transaction = null
        }
        config.transactionType = type
    }

    fun updateSessionType(type: TypeDBSession.Type) {
        if (config.sessionType == type) return
        config.sessionType = type
        openSession(getDatabase()!!, type)
    }

    fun openSession(database: String) {
        openSession(database, config.sessionType)
    }

    fun openSession(database: String, type: TypeDBSession.Type) {
        if (session?.database()?.name() == database && session?.type() == type) return
        closeSession()
        try {
            session = client.session(database, type)
        } catch (exception: TypeDBClientException) {
            notificationMgr.userError(LOGGER, UNABLE_CREATE_SESSION, database)
        }
    }

    fun refreshDatabaseList() {
        if (System.currentTimeMillis() - databaseListRefreshedTime < DATABASE_LIST_REFRESH_RATE_MS) return
        client.let { c -> databaseList = c.databases().all().map { d -> d.name() } }
        databaseListRefreshedTime = System.currentTimeMillis()
    }

    private fun closeSession() {
        session?.let { it.close(); session = null }
    }

    internal fun close() {
        isOpen = false
        closeSession()
        client.close()
    }
}
