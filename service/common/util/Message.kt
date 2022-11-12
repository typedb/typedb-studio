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
            System; Framework; Connection; Project; Schema
        }
    }

    class System(codeNumber: Int, messageBody: String) :
        Message(CODE_PREFIX, codeNumber, MESSAGE_PREFIX, messageBody) {

        companion object {
            private const val CODE_PREFIX = "SYS"
            private const val MESSAGE_PREFIX = "TypeDB Studio System"

            val UNEXPECTED_ERROR_IN_COROUTINE = System(1, "Unexpected error occurred with a coroutine: %s")
            val ILLEGAL_CAST = System(2, "Illegal cast of %s to %s")
            val DATA_DIR_NOT_WRITABLE = System(3, "Does not have write permission to Application Data Directory: %s")
            val UNEXPECTED_ERROR_APP_DATA_DIR =
                System(4, "Unexpected exception occurred while setting up Application Data Directory: %s")
            val APP_DATA_DIR_DISABLED = System(5, "Application properties and logging may be disabled.")
        }
    }

    class Framework(codeNumber: Int, messageBody: String) :
        Message(CODE_PREFIX, codeNumber, MESSAGE_PREFIX, messageBody) {

        companion object {
            private const val CODE_PREFIX = "VIW"
            private const val MESSAGE_PREFIX = "TypeDB Studio View"

            val UNEXPECTED_ERROR =
                Framework(1, "Unexpected error occurred with TypeDB Studio view library.")
            val EXPAND_LIMIT_REACHED =
                Framework(
                    2, "%s navigator reached the recommended limit of expanded items (%s). " +
                            "Automated expansion beyond the limit is disabled to improve performance."
                )
            val TEXT_COPIED_TO_CLIPBOARD =
                Framework(3, "Selected text has been successfully copied to the OS clipboard.")
        }
    }

    class Connection(codeNumber: Int, messageBody: String) :
        Message(CODE_PREFIX, codeNumber, MESSAGE_PREFIX, messageBody) {

        companion object {
            private const val CODE_PREFIX = "CNX"
            private const val MESSAGE_PREFIX = "TypeDB Connection"

            val UNEXPECTED_ERROR =
                Connection(1, "Unexpected error occurred with the connection to TypeDB server.")
            val UNABLE_TO_CONNECT =
                Connection(2, "Failed to connect to TypeDB server with the provided address and credentials.")
            val FAILED_TO_OPEN_SESSION =
                Connection(3, "Failed to establish '%s' session to database '%s'.")
            val FAILED_TO_OPEN_TRANSACTION =
                Connection(4, "Failed to open transaction: %s")
            val FAILED_TO_RUN_QUERY =
                Connection(5, "Failed to run query: %s")
            val SESSION_IS_CLOSED =
                Connection(6, "Session is closed on TypeDB Studio.")
            val SESSION_CLOSED_ON_SERVER =
                Connection(7, "Session was closed on TypeDB Server.")
            val TRANSACTION_CLOSED_ON_SERVER =
                Connection(8, "Transaction was closed due to: %s.")
            val TRANSACTION_CLOSED_IN_QUERY =
                Connection(9, "Transaction was closed due to an error in the query.")
            val TRANSACTION_ROLLBACK =
                Connection(
                    10,
                    "Transaction has been rolled back to the opened snapshot, and all uncommitted writes have been deleted."
                )
            val TRANSACTION_COMMIT_SUCCESSFULLY =
                Connection(
                    11,
                    "Transaction has been successfully committed and closed -- all writes have been persisted."
                )
            val TRANSACTION_COMMIT_FAILED =
                Connection(12, "Transaction failed to commit: %s")
            val FAILED_TO_DELETE_DATABASE =
                Connection(13, "Failed to delete database '%s' due to: %s.")
            val FAILED_TO_CREATE_DATABASE =
                Connection(14, "Failed to create database '%s' due to: %s.")
            val FAILED_TO_CREATE_DATABASE_DUE_TO_DUPLICATE =
                Connection(15, "Failed to create database '%s' due to duplicate.")
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
            val PATH_NO_LONGER_EXIST =
                Project(3, "Project path '%s' no longer exists or is not longer readable.")
            val PATH_NOT_READABLE =
                Project(4, "Project path '%s' is not readable.")
            val PATH_NOT_WRITABLE =
                Project(5, "Project path '%s' is not writable.")
            val PATH_NOT_DIRECTORY =
                Project(6, "Project path '%s' is not a directory.")
            val PROJECT_DATA_DIR_PATH_TAKEN =
                Project(7, "Project data directory cannot be created due to clashing file: %s.")
            val FILE_NOT_READABLE =
                Project(8, "File '%s' is not readable.")
            val FILE_NOT_WRITABLE =
                Project(9, "File %s is not writable, and you are currently in READ-ONLY mode.")
            val DIRECTORY_NOT_DELETABLE =
                Project(10, "Directory %s is not deletable.")
            val FILE_NOT_DELETABLE =
                Project(11, "File %s is not deletable.")
            val FAILED_TO_MOVE_DIRECTORY_TO_SAME_LOCATION =
                Project(
                    12,
                    "The path %s is where the directory currently resides in. Choose a different parent or cancel."
                )
            val FAILED_TO_CREATE_OR_RENAME_DIRECTORY_DUE_TO_DUPLICATE =
                Project(13, "Failed to create or rename directory to %s, as it already exists.")
            val FAILED_TO_MOVE_DIRECTORY_AS_PATH_NOT_EXIST =
                Project(14, "The path %s in which you want to move the directory into, does not exist.")
            val FAILED_TO_MOVE_DIRECTORY_DUE_TO_DUPLICATE =
                Project(15, "Failed to move directory to %s, as it already exists.")
            val FAILED_TO_CREATE_DIRECTORY =
                Project(16, "Failed to create new directory at %s, due to: %s.")
            val FAILED_TO_MOVE_DIRECTORY =
                Project(17, "Failed to move directory to %s, due to: %s.")
            val FAILED_TO_RENAME_DIRECTORY =
                Project(18, "Failed to rename file to %s, due to: %s.")
            val FAILED_TO_CREATE_OR_RENAME_FILE_DUE_TO_DUPLICATE =
                Project(19, "Failed to create or rename file to %s, as it already exists.")
            val FAILED_TO_CREATE_FILE =
                Project(20, "Failed to create new file at %s, due to: %s.")
            val FAILED_TO_SAVE_FILE =
                Project(21, "Failed to save file to new location: %s.")
            val FAILED_TO_RENAME_FILE =
                Project(22, "Failed to rename file to %s, due to: %s.")
            val DIRECTORY_HAS_BEEN_MOVED_OUT =
                Project(23, "Directory has been moved to a location outside of project: %s.")
            val FILE_HAS_BEEN_MOVED_OUT =
                Project(24, "File has been moved to a location outside of project: %s.")
            val FILE_CONTENT_CHANGED_ON_DISK: Message =
                Project(25, "Content of file %s on the filesystem has changed, and has been reloaded in Studio.")
            val FILE_PERMISSION_CHANGED_ON_DISK: Message =
                Project(26, "Permission of file %s on the filesystem has changed, and has been updated in Studio.")
        }
    }

    class Schema(codeNumber: Int, messageBody: String) :
        Message(CODE_PREFIX, codeNumber, MESSAGE_PREFIX, messageBody) {

        companion object {
            private const val CODE_PREFIX = "SCH"
            private const val MESSAGE_PREFIX = "TypeDB Studio Schema"

            val UNEXPECTED_ERROR =
                Schema(1, "Unexpected error occurred with database schema management/operation.")
            val FAILED_TO_OPEN_READ_TX =
                Schema(2, "Failed to open read transaction for schema manager.")
            val FAILED_TO_OPEN_WRITE_TX =
                Schema(3, "Failed to open write transaction for schema manager.")
            val FAILED_TO_LOAD_TYPE =
                Schema(4, "Failed to load type properties due to: %s.")
            val FAILED_TO_DELETE_TYPE =
                Schema(5, "Failed to delete '%s' type due to: %s.")
            val FAILED_TO_CREATE_TYPE_DUE_TO_DUPLICATE =
                Schema(6, "Failed to create %s type with label '%s', as the label is already used.")
            val FAILED_TO_CREATE_TYPE =
                Schema(7, "Failed to create %s type with label '%s' due to: %s.")
            val FAILED_TO_RENAME_TYPE =
                Schema(8, "Failed to rename %s type '%s' to '%s' due to: %s.")
            val FAILED_TO_CHANGE_SUPERTYPE =
                Schema(9, "Failed to change supertype of %s type '%s' due to: %s.")
            val FAILED_TO_CHANGE_ABSTRACT =
                Schema(10, "Failed to change %s type to abstract / not abstract due to: %s.")
            val FAILED_TO_DEFINE_OWN_ATTRIBUTE_TYPE =
                Schema(11, "Failed to define %s type '%s' to own attribute type '%s' due to: %s.")
            val FAILED_TO_UNDEFINE_OWNS_ATT_TYPE =
                Schema(12, "Failed to undefine %s type '%s' from owning attribute type '%s' due to: %s.")
            val FAILED_TO_DEFINE_PLAY_ROLE_TYPE =
                Schema(13, "Failed to define %s type '%s' to play role type '%s' due to: %s.")
            val FAILED_TO_UNDEFINE_PLAYS_ROLE_TYPE =
                Schema(14, "Failed to undefine %s type '%s' from playing role type '%s' due to: %s.")
            val FAILED_TO_DEFINE_RELATE_ROLE_TYPE =
                Schema(15, "Failed to define relation type '%s' to relate role type '%s' due to: %s.")
        }
    }

    class Visualiser(codeNumber: Int, messageBody: String) :
        Message(CODE_PREFIX, codeNumber, MESSAGE_PREFIX, messageBody) {

        companion object {
            private const val CODE_PREFIX = "VIS"
            private const val MESSAGE_PREFIX = "TypeDB Studio Graph Visualiser"

            val UNEXPECTED_ERROR =
                Visualiser(1, "Unexpected error occurred in the graph visualiser.")
            val FULLY_EXPLAINED =
                Visualiser(2, "This concept has been fully explained.")
        }
    }
}
