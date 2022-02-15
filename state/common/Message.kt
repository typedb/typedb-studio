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

import com.vaticle.typedb.common.exception.ErrorMessage

// TODO: if you add a new nested class to ErrorMessage, please make sure to update ErrorMessage.loadClasses()
abstract class Message(codePrefix: String, codeNumber: Int, messagePrefix: String, messageBody: String) :
    ErrorMessage(codePrefix, codeNumber, messagePrefix, messageBody) {

    companion object {

        const val UNKNOWN: String = "Unknown"

        /**
         * This method ensures all nested classes are initialised eagerly in start of runtime,
         * by simply calling that nested class, as shown below. Please make sure to update this
         * function with every new nested class added into ErrorMessage.
         */
        fun loadClasses() {
            System; View; Connection; Project
        }
    }

    class System(codeNumber: Int, messageBody: String) :
        Message(CODE_PREFIX, codeNumber, MESSAGE_PREFIX, messageBody) {

        companion object {
            private const val CODE_PREFIX = "SYS"
            private const val MESSAGE_PREFIX = "TypeDB Studio System"

            val ILLEGAL_CAST = System(1, "Illegal cast of %s to %s")
        }
    }

    class View(codeNumber: Int, messageBody: String) :
        Message(CODE_PREFIX, codeNumber, MESSAGE_PREFIX, messageBody) {

        companion object {
            private const val CODE_PREFIX = "VIW"
            private const val MESSAGE_PREFIX = "TypeDB Studio View"

            val UNEXPECTED_ERROR =
                View(1, "Unexpected error occurred with TypeDB Studio view library.")
            val EXPAND_LIMIT_REACHED =
                View(
                    2, "%s navigator reached the recommended limit of expanded items (%s). " +
                            "Automated expansion beyond the limit is disabled to improve performance."
                )
        }
    }

    class Connection(codeNumber: Int, messageBody: String) :
        Message(CODE_PREFIX, codeNumber, MESSAGE_PREFIX, messageBody) {

        companion object {
            private const val CODE_PREFIX = "CON"
            private const val MESSAGE_PREFIX = "TypeDB Connection"

            val UNEXPECTED_ERROR =
                Connection(1, "Unexpected error occurred with the connection to TypeDB server.")
            val UNABLE_TO_CONNECT =
                Connection(2, "Unable to connect to TypeDB server with the provided address and credentials.")
            val UNABLE_CREATE_SESSION =
                Connection(3, "Unable to establish session to database '%s'.")
        }
    }

    class Project(codeNumber: Int, messageBody: String) :
        Message(CODE_PREFIX, codeNumber, MESSAGE_PREFIX, messageBody) {

        companion object {
            private const val CODE_PREFIX = "PRJ"
            private const val MESSAGE_PREFIX = "TypeDB Studio Project"

            val UNEXPECTED_ERROR =
                Project(1, "Unexpected error occurred with the project directory file system.")
            val PATH_NOT_EXIST =
                Project(2, "Project path '%s' does not exist.")
            val PATH_NOT_READABLE =
                Project(3, "Project path '%s' is not readable.")
            val PATH_NOT_DIRECTORY =
                Project(4, "Project path '%s' is not a directory.")
            val PROJECT_CLOSED =
                Project(5, "Project path '%s' has been closed.")
            val FILE_NOT_READABLE =
                Project(6, "File '%s' is not readable.")
            val FILE_NOT_WRITABLE =
                Project(7, "File %s is not writable, and you are currently in READ-ONLY mode.")
            val DIRECTORY_NOT_DELETABLE =
                Project(8, "Directory %s is not deletable.")
            val FILE_NOT_DELETABLE =
                Project(9, "File %s is not deletable.")
            val FAILED_TO_CREATE_OR_RENAME_FILE_TO_DUPLICATE =
                Project(10, "Failed to create or rename file to %s, as it already exists.")
            val FAILED_TO_CREATE_DIRECTORY =
                Project(11, "Failed to create new directory at %s.")
            val FAILED_TO_CREATE_FILE =
                Project(12, "Failed to create new file at %s.")
            val FAILED_TO_RENAME_FILE =
                Project(13, "Failed to rename file to %s.")
            val FILE_CONTENT_CHANGED_ON_DISK: Message =
                Project(14, "Content of file %s on the filesystem has changed, and has been reloaded in Studio.")
            val FILE_PERMISSION_CHANGED_ON_DISK: Message =
                Project(15, "Permission of file %s on the filesystem has changed, and has been updated in Studio.")
        }
    }
}
