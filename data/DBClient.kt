package com.vaticle.typedb.studio.data

import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.connection.TypeDBClient

class DBClient(val serverAddress: String) {

    internal val typeDBClient: TypeDBClient = TypeDB.coreClient(serverAddress)

    init {
        Runtime.getRuntime().addShutdownHook(Thread { typeDBClient.close() })
    }

    fun close() {
        typeDBClient.close()
    }
}
