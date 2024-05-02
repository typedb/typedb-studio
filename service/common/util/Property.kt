/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.service.common.util

import java.nio.file.Path
import kotlin.io.path.extension

object Property {
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
        TYPEDB_CORE("TypeDB Core"),
        TYPEDB_CLOUD("TypeDB Cloud");

        companion object {
            fun of(name: String): Server? {
                return when (name) {
                    TYPEDB_CORE.displayName -> TYPEDB_CORE
                    TYPEDB_CLOUD.displayName -> TYPEDB_CLOUD
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
            Server.TYPEDB_CORE.displayName -> Server.TYPEDB_CORE
            Server.TYPEDB_CLOUD.displayName -> Server.TYPEDB_CLOUD
            else -> throw IllegalStateException("Unrecognised TypeDB server type")
        }
    }
}
