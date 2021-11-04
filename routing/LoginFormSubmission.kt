package com.vaticle.typedb.studio.routing

import com.vaticle.typedb.studio.data.ClusterClient
import com.vaticle.typedb.studio.data.CoreClient
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.data.DBClient

abstract class LoginFormSubmission(val serverAddress: String, val db: DB, val allDBNames: List<String>) {
    abstract val dbClient: DBClient
    abstract fun toRoute(): LoginRoute
}

class CoreLoginFormSubmission(override val dbClient: CoreClient, db: DB, allDBNames: List<String>):
    LoginFormSubmission(serverAddress = dbClient.serverAddress, db = db, allDBNames = allDBNames) {

    override fun toRoute() = CoreLoginRoute(serverAddress)
}

class ClusterLoginFormSubmission(override val dbClient: ClusterClient, val username: String, val rootCAPath: String,
                                 db: DB, allDBNames: List<String>):
    LoginFormSubmission(serverAddress = dbClient.serverAddress, db = db, allDBNames = allDBNames) {

    override fun toRoute() = ClusterLoginRoute(serverAddress, username, rootCAPath)
}
