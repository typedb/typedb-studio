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

package com.vaticle.typedb.studio.routing

import com.vaticle.typedb.studio.session.ClusterClient
import com.vaticle.typedb.studio.session.CoreClient
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.session.DBClient

abstract class ConnectionFormSubmission(val serverAddress: String, val db: DB, val allDBNames: List<String>) {
    abstract val dbClient: DBClient
    abstract fun toRoute(): ConnectionRoute

    class Core(override val dbClient: CoreClient, db: DB, allDBNames: List<String>) :
        ConnectionFormSubmission(serverAddress = dbClient.serverAddress, db = db, allDBNames = allDBNames) {
        override fun toRoute() = ConnectionRoute.Core(serverAddress)
    }

    class Cluster(
        override val dbClient: ClusterClient, private val username: String,
        val rootCAPath: String, db: DB, allDBNames: List<String>
    ) : ConnectionFormSubmission(serverAddress = dbClient.serverAddress, db = db, allDBNames = allDBNames) {
        override fun toRoute() = ConnectionRoute.Cluster(serverAddress, username, rootCAPath)
    }
}
