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

package com.vaticle.typedb.studio.state.common.util

object Sentence {

    private const val ACTION_CANNOT_BE_UNDONE = "This action cannot be undone."
    private const val BUTTON_ENABLED_WHEN_CONNECTED =
        "This button will only be enabled when a connection has been established to a TypeDB server."
    private const val BUTTON_ENABLED_WHEN_SESSION_OPEN =
        "This button will only be enabled when a session is opened to a database."
    private const val BUTTON_ENABLED_WHEN_SNAPSHOT_ENABLED =
        "This button will only be enabled when a transaction is kept alive on a specific 'snapshot' -- " +
                "which could happen by enabling 'snapshot' on a 'read' transaction, or the transaction type is 'write'."
    private const val BUTTON_ENABLED_WHEN_TRANSACTION_IS_READ =
        "This button will only be enabled when there is an open session to a database, and the transaction type is 'read'."
    private const val BUTTON_ENABLED_WHEN_TRANSACTION_IS_WRITE =
        "This button will only be enabled when there is an open session to a database, and the transaction type is 'write'."
    const val BUTTON_ENABLED_WHEN_RUNNING =
        "This button will only be enabled when there is a running query."
    const val BUTTON_ENABLED_WHEN_RUNNABLE =
        "This button will only be enabled when a session is opened to a database, and TypeQL file is opened and active " +
                "while no other query is running. A TypeQL file is a file with .tql or .typeql extension."
    const val CANNOT_BE_UNDONE =
        "This action cannot be undone."
    const val CONFIRM_DATABASE_DELETION =
        "Are you sure you would like to delete the database '%s' and all of its content? $ACTION_CANNOT_BE_UNDONE " +
                "Enter the database name below in order to confirm its deletion."
    const val CONFIRM_DIRECTORY_DELETION =
        "Are you sure you would like to delete this directory and all of its content? $ACTION_CANNOT_BE_UNDONE"
    const val CONFIRM_FILE_DELETION =
        "Are you sure you would like to delete this file? $ACTION_CANNOT_BE_UNDONE"
    const val CONFIRM_QUITING_APPLICATION = "Are you sure you want to close TypeDB Studio? " +
            "Unsaved files will still be available when you reopen."
    const val CREATE_DATABASE_BUTTON_DESCRIPTION =
        "Create a new database on your TypeDB Server. This button will only be enabled the provided name is valid, " +
                "i.e. valid syntax and does not already exist."
    const val CREATE_DIRECTORY =
        "Create a new directory under %s."
    const val CREATE_FILE =
        "Create a new file under %s."
    const val EDITING_TYPES_REQUIREMENT_DESCRIPTION =
        "Functionalities for editing schema types are under development and COMING SOON."
    //TODO: "To edit types in the schema, you need to have a 'schema' session and a 'write' transaction'."
    const val ENABLE_INFERENCE_DESCRIPTION =
        "Enabling inference means that you will get inferred answers in your match query. " +
                BUTTON_ENABLED_WHEN_TRANSACTION_IS_READ
    const val ENABLE_INFERENCE_EXPLANATION_DESCRIPTION =
        "Enabling inference explanation means that any inferred concepts returned by your query will include an explanation " +
                "of how the logical inference was made. This button will only be enabled if 'snapshot' and 'infer' are " +
                "both enabled."
    const val ENABLE_SNAPSHOT_DESCRIPTION =
        "Enabling snapshot means that you will keep a transaction alive on a given snapshot of the data, " +
                "until you either reopen or commit the transaction. In TypeDB Studio, keeping the transaction alive on a " +
                "given snapshot is the de facto behaviour when you are in a 'write' transaction, but not in 'read'. " +
                "Enabling snapshot in a 'read' transaction allows you to query for explanations of inferred concept answers. " +
                "The transaction will be opened on the latest snapshot when the first query is ran. " +
                BUTTON_ENABLED_WHEN_SESSION_OPEN
    const val INTERACTIVE_MODE_DESCRIPTION =
        "Running TypeDB Studio in 'interactive' mode (as opposed to 'script' mode), means that you can interact with a " +
                "TypeDB server interactively. In 'script' mode, you have to declare the user, database, session, and " +
                "transaction that you're connecting in each script you run on TypeDB Server. In 'interactive' mode, you " +
                "can set on these parameters in the toolbar, and perform queries against the TypeDB server with configured " +
                "parameters interactively. " + BUTTON_ENABLED_WHEN_CONNECTED
    const val MANAGE_DATABASES_DESCRIPTION =
        "You can manage your databases by adding or deleting databases from the TypeDB Server you are connected to. " +
                BUTTON_ENABLED_WHEN_CONNECTED
    const val MANAGE_DATABASES_MESSAGE =
        "Below is the list of databases on your TypeDB Server. You can delete them individually, or create new ones. "
    const val OUTPUT_RESPONSE_TIME_DESCRIPTION = "Duration to collect all answers of the query from the server."
    const val QUERY_RESPONSE_TIME_DESCRIPTION =
        "Duration to collect auxiliary information/concepts to display informative log & graph output."
    const val RENAME_DIRECTORY =
        "Rename the directory at %s."
    const val RENAME_FILE =
        "Rename the file at %s."
    const val SAVE_CURRENT_FILE_DESCRIPTION =
        "By default, edited text files are automatically saved, except for untitled files that have not been saved. " +
                "This button will only be enabled if the currently opened file needs saving."
    const val SAVE_OR_DELETE_FILE =
        "Would you like to save this file before closing it? Closing it without saving would delete this file and its content."
    const val SCRIPT_MODE_DESCRIPTION =
        "Running TypeDB Studio in 'script' mode (as opposed to 'interactive' mode), means that you can an end-to-end workflow " +
                "on TypeDB server through a script. In 'interactive' mode, you have to configure the user, database, session, " +
                "and transaction manually for each query you want to perform on the toolbar. In 'script' mode, you can " +
                "declare these parameters in the script, and perform queries against the TypeDB server automatically " +
                "through the script. " + BUTTON_ENABLED_WHEN_CONNECTED
    const val SELECT_DATABASE_DESCRIPTION =
        "Selecting a database will open a session onto that database on the TypeDB server. $BUTTON_ENABLED_WHEN_CONNECTED" // TODO: interactive
    const val SELECT_DIRECTORY_FOR_PROJECT =
        "Select the directory that will serve as the project root directory."
    const val SELECT_PARENT_DIRECTORY_TO_MOVE_UNDER =
        "Select the parent directory in which %s will be moved under."
    const val SESSION_DATA_DESCRIPTION =
        "Data sessions allow you to only modify data in the database, and not the schema. This means inserting and deleting data. " +
                "There is no limitation on performing reads on schema or data. " + BUTTON_ENABLED_WHEN_SESSION_OPEN
    const val SESSION_SCHEMA_DESCRIPTION =
        "Schema sessions allow you to only modify the schema of the database, and not data. This means defining and undefining schema. " +
                "There is no limitation on performing reads on schema or data. " + BUTTON_ENABLED_WHEN_SESSION_OPEN
    const val STOP_SIGNAL_DESCRIPTION =
        "A stop signal allows you to stop the currently running query when the next server response is received. " +
                "To stop the query immediately without waiting for any server response, close the transaction instead. " +
                BUTTON_ENABLED_WHEN_RUNNING
    const val STOP_RUNNING_QUERY_BEFORE_CLOSING_PAGE_DESCRIPTION =
        "The running query associated to this page should be stopped before closing this page."
    const val TRANSACTION_CLOSE_DESCRIPTION =
        "Closing a transaction will close the current transaction, deleting any unsaved writes you've made through it. " +
                "The next transaction will be opened at a newer and latest snapshot. " + BUTTON_ENABLED_WHEN_SNAPSHOT_ENABLED
    const val TRANSACTION_COMMIT_DESCRIPTION =
        "Committing a transaction will persist all unsaved writes that you've made to the database through the transaction. " +
                BUTTON_ENABLED_WHEN_TRANSACTION_IS_WRITE
    const val TRANSACTION_READ_DESCRIPTION =
        "Read transactions allow you to only read data from the database, and not write. For both data and schema sessions, " +
                "this means reading any data and schema, but not inserting, deleting, defining, or undefining. " +
                BUTTON_ENABLED_WHEN_SESSION_OPEN
    const val TRANSACTION_ROLLBACK_DESCRIPTION =
        "Rolling back a transaction will delete all unsaved/uncommitted writes that you've made to the database through " +
                "the transaction, while keeping the same transaction alive. " + BUTTON_ENABLED_WHEN_TRANSACTION_IS_WRITE
    const val TRANSACTION_STATUS_DESCRIPTION =
        "The transaction status indicates whether a transaction is currently opened (when it lights up). In a read transaction, " +
                "a transaction will be kept open if 'snapshot' is enabled. In a write transaction, a transaction is always " +
                "be kept open until you close or commit the transaction."
    const val TRANSACTION_WRITE_DESCRIPTION =
        "Write transactions allow you to write data onto the database, in addition to reading. For data sessions, this " +
                "means inserting and deleting data, in addition to matching. For schema sessions, this means defining and " +
                "undefining schema, in addition to matching. " + BUTTON_ENABLED_WHEN_SESSION_OPEN
    const val TYPE_BROWSER_ONLY_INTERACTIVE =
        "The Type Browser only works in 'interactive' mode."

}