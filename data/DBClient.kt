package com.vaticle.typedb.studio.data

import com.vaticle.typedb.client.api.connection.TypeDBClient

interface DBClient {

    val serverAddress: String

    val typeDBClient: TypeDBClient

    fun close()
}
