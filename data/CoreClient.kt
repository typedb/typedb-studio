package com.vaticle.typedb.studio.data

import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.connection.TypeDBClient

class CoreClient(override val serverAddress: String): BaseClient() {

    override val typeDBClient: TypeDBClient = TypeDB.coreClient(serverAddress)
}
