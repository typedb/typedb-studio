package com.vaticle.typedb.studio.db

import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.connection.TypeDBClient

class DBServer(val serverAddress: String) {

    val client: TypeDBClient = TypeDB.coreClient(serverAddress) // TODO: we need to call client.close
}
