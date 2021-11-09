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

package com.vaticle.typedb.studio.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.studio.data.ClusterClient
import com.vaticle.typedb.studio.data.CoreClient
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.data.DBClient
import com.vaticle.typedb.studio.login.ServerSoftware.*
import com.vaticle.typedb.studio.routing.LoginFormSubmission
import com.vaticle.typedb.studio.routing.LoginRoute
import mu.KotlinLogging.logger

class LoginScreenState(serverSoftware: ServerSoftware = CORE, serverAddress: String = "127.0.0.1:1729",
                       username: String = "", password: String = "", rootCAPath: String = "", dbFieldText: String = "",
                       dbClient: DBClient? = null, db: DB? = null, allDBNames: List<String> = listOf()) {

    var serverSoftware: ServerSoftware by mutableStateOf(serverSoftware)
    var serverAddress: String by mutableStateOf(serverAddress)
    var username by mutableStateOf(username)
    var password by mutableStateOf(password)
    var rootCAPath: String by mutableStateOf(rootCAPath)
    var dbFieldText by mutableStateOf(dbFieldText)
    var dbClient: DBClient? by mutableStateOf(dbClient)
    val allDBNames: SnapshotStateList<String> = mutableStateListOf<String>().let { it += allDBNames; return@let it }
    var db: DB? by mutableStateOf(db)
    val databaseSelected: Boolean
    get() = db != null

    /**
     * If a Client is currently open, close it, handling and logging any errors that occur.
     */
    fun closeClient() {
        dbClient = null
        db = null
        try {
            dbClient?.close()
        } catch (e: Exception) {
            log.warn(e) { "Failed to close client" }
        }
    }

    fun clearDBList() {
        allDBNames.clear()
        dbFieldText = ""
    }

    fun asSubmission(): LoginFormSubmission {
        val dbClientSnapshot = requireNotNull(dbClient)
        val dbSnapshot = requireNotNull(db)

        return when (serverSoftware) {
            CORE -> {
                if (dbClientSnapshot !is CoreClient) {
                    throw IllegalStateException("Core login form expected DBClient of type CoreClient, but was ${dbClientSnapshot.javaClass}")
                }
                LoginFormSubmission.Core(dbClient = dbClientSnapshot, db = dbSnapshot, allDBNames = allDBNames)
            }
            CLUSTER -> {
                if (dbClientSnapshot !is ClusterClient) {
                    throw IllegalStateException("Cluster login form expected DBClient of type ClusterClient, but was ${dbClientSnapshot.javaClass}")
                }
                LoginFormSubmission.Cluster(dbClient = dbClientSnapshot, username = username, rootCAPath = rootCAPath,
                    db = dbSnapshot, allDBNames = allDBNames)
            }
        }
    }

    companion object {
        val log = logger {}
    }
}

fun loginScreenStateOf(routeData: LoginRoute) = when (routeData) {
    is LoginRoute.Cluster -> LoginScreenState(serverSoftware = CLUSTER, serverAddress = routeData.serverAddress,
        username = routeData.username, rootCAPath = routeData.rootCAPath)
    else -> LoginScreenState(serverSoftware = CORE, serverAddress = routeData.serverAddress)
}

enum class ServerSoftware(val displayName: String) {
    CORE(displayName = "TypeDB"),
    CLUSTER(displayName = "TypeDB Cluster"),
}
