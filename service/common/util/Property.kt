/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.service.common.util

import java.nio.file.Path
import kotlin.io.path.extension

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

        companion object {
            fun of(name: String): Server? {
                return when (name) {
                    TYPEDB.displayName -> TYPEDB
                    TYPEDB_CLUSTER.displayName -> TYPEDB_CLUSTER
                    else -> null
                }
            }
        }

        override fun toString(): String {
            return displayName
        }
    }

    enum class FileType constructor(
        val extensions: List<String> = emptyList(),
        val commentToken: String = "",
        val isRunnable: Boolean = false
    ) {
        TYPEQL(listOf("tql", "typeql"), "#", true),
        UNKNOWN;

        companion object {

            val RUNNABLE_TYPES get() = values().filter { it.isRunnable }
            val RUNNABLE_EXTENSIONS get() = RUNNABLE_TYPES.flatMap { it.extensions }
            val RUNNABLE_EXTENSIONS_STR get() = RUNNABLE_EXTENSIONS.joinToString(", ") { ".$it" }

            fun of(path: Path): FileType = of(path.extension)
            fun of(extension: String): FileType = when {
                TYPEQL.extensions.contains(extension) -> TYPEQL
                else -> UNKNOWN
            }
        }
    }

    fun serverOf(displayName: String): Server {
        return when (displayName) {
            Server.TYPEDB.displayName -> Server.TYPEDB
            Server.TYPEDB_CLUSTER.displayName -> Server.TYPEDB_CLUSTER
            else -> throw IllegalStateException("Unrecognised TypeDB server type")
        }
    }
}