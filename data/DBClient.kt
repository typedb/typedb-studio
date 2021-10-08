package com.vaticle.typedb.studio.data

import com.vaticle.typedb.client.api.connection.TypeDBOptions
import com.vaticle.typedb.client.api.connection.TypeDBSession

interface DBClient {

    val serverAddress: String

    fun listDatabases(): List<String>

    fun containsDatabase(dbName: String): Boolean

    fun session(dbName: String, type: TypeDBSession.Type): TypeDBSession

    fun close()

    fun closeAllSessions()
}
