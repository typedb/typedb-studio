/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.state.common

object Property {

    const val DEFAULT_SERVER_ADDRESS: String = "localhost:1729"

    enum class OS(val displayName: String) {
        WINDOWS("Windows"),
        MACOS("MacOS"),
        LINUX("Linux");

        override fun toString(): String {
            return displayName
        }

        companion object {
            val Current: OS by lazy {
                val name = System.getProperty("os.name")
                when {
                    name.startsWith("Mac") -> MACOS
                    name.startsWith("Win") -> WINDOWS
                    else -> LINUX
                }
            }
        }
    }

    enum class Server(val displayName: String) {
        TYPEDB("TypeDB"),
        TYPEDB_CLUSTER("TypeDB Cluster");

        override fun toString(): String {
            return displayName
        }
    }

    enum class File(vararg extensionStrs: String) {
        TYPEQL("tql", "typeql");

        val extensions = extensionStrs.toSet()
    }


    fun serverOf(displayName: String): Server {
        return when (displayName) {
            Server.TYPEDB.displayName -> Server.TYPEDB
            Server.TYPEDB_CLUSTER.displayName -> Server.TYPEDB_CLUSTER
            else -> throw IllegalStateException()
        }
    }
}