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

package com.vaticle.typedb.studio.view.common

object Sentence {

    private const val BUTTON_ENABLED_ON_WRITE_TRANSACTION =
        "This button will only be enabled when there is an open session to a database, and a 'write' transaction type."
    private const val BUTTON_ENABLED_ON_SNAPSHOT =
        "This button will only be enabled when a transaction is kept alive on a specific 'snapshot' -- " +
                "which could happen by enabling 'snapshot' on a 'read' transaction, or when on a 'write' transaction."
    const val BUTTON_ENABLED_WHEN_CONNECTED =
        "This button will only be enabled when a connection has been established to a TypeDB server."
    const val CANNOT_BE_UNDONE =
        "This action cannot be undone."
    const val COMMIT_TRANSACTION_DESCRIPTION =
        "Committing a transaction will persist all unsaved writes that you've made to the database through the transaction. " +
                BUTTON_ENABLED_ON_WRITE_TRANSACTION
    const val CONFIRM_DIRECTORY_DELETION =
        "Are you sure you would like to delete this directory and all of its content?"
    const val CONFIRM_FILE_DELETION =
        "Are you sure you would like to delete this file?"
    const val CREATE_DIRECTORY =
        "Create a new directory under %s."
    const val CREATE_FILE =
        "Create a new file under %s."
    const val RENAME_DIRECTORY =
        "Rename the directory at %s."
    const val RENAME_FILE =
        "Rename the file at %s."
    const val REOPEN_TRANSACTION_DESCRIPTION =
        "Reopening a transaction will close the current transaction (deleting any unsaved writes you've made through it), " +
                "and open a new transaction at the latest snapshot of the database. " + BUTTON_ENABLED_ON_SNAPSHOT
    const val ROLLBACK_TRANSACTION_DESCRIPTION =
        "Rolling back a transaction will delete all unsaved writes that you've made to the database through the transaction, " +
                "while keeping the same transaction alive. " + BUTTON_ENABLED_ON_WRITE_TRANSACTION
    const val SAVE_OR_DELETE_FILE =
        "Would you like to save this file before closing it? Closing it without saving would delete this file and its content."
    const val SELECT_DIRECTORY_FOR_PROJECT =
        "Select the directory that will serve as the project root directory."
    const val SELECT_PARENT_DIRECTORY_TO_MOVE_UNDER =
        "Select the parent directory in which %s will be moved under."
    const val TYPE_BROWSER_ONLY_INTERACTIVE =
        "The Type Browser only works in 'interactive' mode."

}