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
