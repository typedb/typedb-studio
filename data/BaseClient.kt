package com.vaticle.typedb.studio.data

abstract class BaseClient: DBClient {

    init {
        Runtime.getRuntime().addShutdownHook(Thread { typeDBClient.close() })
    }

    override fun close() {
        typeDBClient.close()
    }
}
