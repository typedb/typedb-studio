package com.vaticle.typedb.studio.module.connection

interface ConnectionConfiguration {
    companion object {
        private const val SEPARATOR: String = "|"
        private const val CLUSTER_ID: String = "cluster"
        private const val CORE_ID: String = "core"
    }

    class Cluster(val addresses: Set<String>, val username: String, val password: String,
                  val tls: Boolean, val certPath: String): ConnectionConfiguration

    class Core(val address: String) : ConnectionConfiguration

    fun fromString(configString: String): ConnectionConfiguration {
        val config: List<String> = configString.split(SEPARATOR)

        return if (config[0] == CLUSTER_ID) {
            ConnectionConfiguration.Cluster(config[1].split(",").toSet(), config[2], config[3], config[4].toBooleanStrictOrNull()!!, config[5])
        } else {
            ConnectionConfiguration.Core(config[1])
        }
    }

    fun toString(config: ConnectionConfiguration): String {
        return when (config) {
            is Cluster -> {
                listOf(CLUSTER_ID, config.addresses.joinToString(","), config.username, config.password, config.tls, config.certPath).joinToString(SEPARATOR)
            }
            is Core -> {
                listOf(CORE_ID, config.address).joinToString(SEPARATOR)
            }
            else -> {
                throw RuntimeException("No such connection configuration type.")
            }
        }
    }
}