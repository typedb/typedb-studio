package com.vaticle.typedb.studio.data

import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.connection.TypeDBClient
import com.vaticle.typedb.client.api.connection.TypeDBCredential
import java.nio.file.Path

class ClusterClient(override val serverAddress: String, val username: String, password: String, rootCAPath: String?): BaseClient() {

    override val typeDBClient: TypeDBClient = if (rootCAPath.isNullOrBlank()) {
        TypeDB.clusterClient(serverAddress, TypeDBCredential(username, password, false))
    } else {
        TypeDB.clusterClient(serverAddress, TypeDBCredential(username, password, true, Path.of(rootCAPath)))
    }
}
