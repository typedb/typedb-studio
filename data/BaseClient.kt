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

package com.vaticle.typedb.studio.data

import com.vaticle.typedb.client.api.connection.TypeDBClient
import com.vaticle.typedb.client.api.connection.TypeDBSession
import com.vaticle.typedb.client.api.connection.database.Database

abstract class BaseClient: DBClient {

    protected abstract val typeDBClient: TypeDBClient
    private val sessions: MutableList<TypeDBSession> = mutableListOf()

    init {
        Runtime.getRuntime().addShutdownHook(Thread { typeDBClient.close() })
    }

    override fun listDatabases(): List<String> = typeDBClient.databases().all().map(Database::name)

    override fun containsDatabase(dbName: String): Boolean = typeDBClient.databases().contains(dbName)

    override fun close() {
        closeAllSessions()
        typeDBClient.close()
    }

    override fun closeAllSessions() {
        sessions.forEach(TypeDBSession::close)
        sessions.clear()
    }

    override fun session(dbName: String, type: TypeDBSession.Type): TypeDBSession {
        typeDBClient.session(dbName, type).let {
            sessions += it
            return it
        }
    }
}
