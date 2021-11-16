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

package com.vaticle.typedb.studio.common

object Property {

    const val DEFAULT_SERVER_ADDRESS: String = "localhost:1729"

    interface Displayable {
        val displayName: String
    }

    enum class OS(override val displayName: String): Displayable {
        WINDOWS("Windows"),
        MAC("MacOS"),
        LINUX("Linux"),
    }

    enum class Server(override val displayName: String): Displayable {
        TYPEDB("TypeDB"),
        TYPEDB_CLUSTER("TypeDB Cluster");
    }

    fun displayableOf(displayName: String): Displayable {
        return object: Displayable { override val displayName = displayName }
    }

    fun serverOf(displayName: String): Server {
        return when (displayName) {
            Server.TYPEDB.displayName -> Server.TYPEDB
            Server.TYPEDB_CLUSTER.displayName -> Server.TYPEDB_CLUSTER
            else -> throw IllegalStateException()
        }
    }
}