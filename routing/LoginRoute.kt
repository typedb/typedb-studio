package com.vaticle.typedb.studio.routing

abstract class LoginRoute(val serverAddress: String): Route

class CoreLoginRoute(serverAddress: String = "127.0.0.1:1729"): LoginRoute(serverAddress = serverAddress)

class ClusterLoginRoute(serverAddress: String = "127.0.0.1:11729", val username: String = "", val rootCAPath: String = ""):
    LoginRoute(serverAddress = serverAddress)
