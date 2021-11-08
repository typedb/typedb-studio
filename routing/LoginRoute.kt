package com.vaticle.typedb.studio.routing

abstract class LoginRoute(val serverAddress: String): Route {

    class Core(serverAddress: String = "127.0.0.1:1729"): LoginRoute(serverAddress = serverAddress)

    class Cluster(serverAddress: String = "127.0.0.1:11729", val username: String = "", val rootCAPath: String = ""):
        LoginRoute(serverAddress = serverAddress)
}
