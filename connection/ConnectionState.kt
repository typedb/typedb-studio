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

package com.vaticle.typedb.studio.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.session.ClusterClient
import com.vaticle.typedb.studio.session.CoreClient
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.session.DBClient
import com.vaticle.typedb.studio.diagnostics.ErrorReporter
import com.vaticle.typedb.studio.connection.ServerSoftware.CLUSTER
import com.vaticle.typedb.studio.connection.ServerSoftware.CORE
import com.vaticle.typedb.studio.routing.ConnectionFormSubmission
import com.vaticle.typedb.studio.routing.ConnectionRoute
import com.vaticle.typedb.studio.routing.Router
import com.vaticle.typedb.studio.routing.WorkspaceRoute
import java.util.concurrent.CompletableFuture
import mu.KotlinLogging.logger

// TODO: This enum needs to move to the right domain
enum class ServerSoftware(val displayName: String) {
    CORE(displayName = "TypeDB"),
    CLUSTER(displayName = "TypeDB Cluster"),
}

fun connectionScreenStateOf(
    routeData: ConnectionRoute,
    errorReporter: ErrorReporter,
    databasesLastLoadedFromAddress: String?,
    databasesLastLoadedAtMillis: Long?,
    loadingDatabases: Boolean
) = when (routeData) {
    is ConnectionRoute.Cluster -> ConnectionState(
        serverSoftware = CLUSTER, serverAddress = routeData.serverAddress,
        databasesLastLoadedFromAddress = databasesLastLoadedFromAddress,
        username = routeData.username, rootCAPath = routeData.rootCAPath,
        databasesLastLoadedAtMillis = databasesLastLoadedAtMillis,
        loadingDatabases = loadingDatabases,
        errorReporter = errorReporter
    )
    else -> ConnectionState(
        serverSoftware = CORE,
        serverAddress = routeData.serverAddress,
        databasesLastLoadedFromAddress = databasesLastLoadedFromAddress,
        databasesLastLoadedAtMillis = databasesLastLoadedAtMillis,
        loadingDatabases = loadingDatabases,
        errorReporter = errorReporter
    )
}

class ConnectionState(
    serverSoftware: ServerSoftware = CORE,
    serverAddress: String = "127.0.0.1:1729",
    username: String = "",
    password: String = "",
    rootCAPath: String = "",
    dbFieldText: String = "",
    dbClient: DBClient? = null,
    db: DB? = null,
    allDBNames: List<String> = listOf(),
    var databasesLastLoadedFromAddress: String?,
    var databasesLastLoadedAtMillis: Long?,
    var loadingDatabases: Boolean,
    val errorReporter: ErrorReporter
) {
    var serverSoftware: ServerSoftware by mutableStateOf(serverSoftware)
    var serverAddress: String by mutableStateOf(serverAddress)
    var username by mutableStateOf(username)
    var password by mutableStateOf(password)
    var rootCAPath: String by mutableStateOf(rootCAPath)
    var dbFieldText by mutableStateOf(dbFieldText)
    var dbClient: DBClient? by mutableStateOf(dbClient)
    val allDBNames: SnapshotStateList<String> = mutableStateListOf<String>().let { it += allDBNames; return@let it }
    var db: DB? by mutableStateOf(db)
    val databaseSelected: Boolean get() = db != null

    companion object {
        val LOG = logger {}
    }

    /**
     * If a Client is currently open, close it, handling and logging any errors that occur.
     */
    fun closeClient() {
        dbClient = null
        db = null
        try {
            dbClient?.close()
        } catch (e: Exception) {
            LOG.warn(e) { "Failed to close client" }
        }
    }

    fun clearDBList() {
        allDBNames.clear()
        dbFieldText = ""
    }

    fun asSubmission(): ConnectionFormSubmission {
        val dbClientSnapshot = requireNotNull(dbClient)
        val dbSnapshot = requireNotNull(db)

        return when (serverSoftware) {
            CORE -> {
                if (dbClientSnapshot !is CoreClient) {
                    throw IllegalStateException("Core connection form expected DBClient of type CoreClient, but was ${dbClientSnapshot.javaClass}")
                }
                ConnectionFormSubmission.Core(dbClient = dbClientSnapshot, db = dbSnapshot, allDBNames = allDBNames)
            }
            CLUSTER -> {
                if (dbClientSnapshot !is ClusterClient) {
                    throw IllegalStateException("Cluster connection form expected DBClient of type ClusterClient, but was ${dbClientSnapshot.javaClass}")
                }
                ConnectionFormSubmission.Cluster(
                    dbClient = dbClientSnapshot, username = username, rootCAPath = rootCAPath,
                    db = dbSnapshot, allDBNames = allDBNames
                )
            }
        }
    }

    fun selectServerSoftware(software: ServerSoftware) {
        if (serverSoftware == software) return
        serverSoftware = software
        clearDBList()
        closeClient()
        databasesLastLoadedFromAddress = null
        databasesLastLoadedAtMillis = null
    }

    fun populateDBListAsync() {
        CompletableFuture.supplyAsync {
            try {
                val client = when (serverSoftware) {
                    CORE -> CoreClient(serverAddress)
                    CLUSTER -> ClusterClient(serverAddress, username, password, rootCAPath)
                }
                dbClient = client
                allDBNames.let { dbNames ->
                    dbNames += client.listDatabases()
                    if (dbNames.isEmpty()) dbFieldText = "This server has no databases"
                    else if (!databaseSelected) dbFieldText = "Select a database"
                }
            } catch (e: Exception) {
                databasesLastLoadedFromAddress = null
                dbFieldText = "Failed to load databases"
                if (e is TypeDBClientException) {
                    errorReporter.reportTypeDBClientError(e) { "Failed to load databases at address ${serverAddress}" }
                } else {
                    errorReporter.reportIDEError(e)
                }
            } finally {
                loadingDatabases = false
            }
        }
    }

    fun onDatabaseDropdown() {
        if (loadingDatabases) return
        val lastLoadedMillis = databasesLastLoadedAtMillis
        if (serverAddress == databasesLastLoadedFromAddress
            && lastLoadedMillis != null && System.currentTimeMillis() - lastLoadedMillis < 2000
        ) return

        loadingDatabases = true
        databasesLastLoadedFromAddress = serverAddress
        databasesLastLoadedAtMillis = System.currentTimeMillis()
        closeClient()
        if (serverAddress.isNotBlank()) {
            if (!databaseSelected) dbFieldText = "Loading databases..."
            allDBNames.clear()
            populateDBListAsync()
        }
    }

    fun onDatabaseSelected(dbName: String) {
        dbClient.let {
            if (it != null) {
                dbFieldText = dbName
                db = DB(it, dbName)
            } else {
                db = null
                clearDBList()
            }
        }
    }

    fun onSubmit() {
        try {
            val submission = asSubmission()
            Router.navigateTo(WorkspaceRoute(submission))
        } catch (e: Exception) {
            errorReporter.reportIDEError(e)
        }
    }
}
