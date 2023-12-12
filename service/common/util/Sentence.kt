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

object Sentence {

    private const val ACTION_CANNOT_BE_UNDONE =
        "This action cannot be undone."
    private const val CHANGE_NEEDS_COMMIT =
        "This change will not be persisted until you commit the transaction."
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
    private const val RUNNABLE_FILE_EXTENSIONS =
        "Runnable files are those with the extensions: %s."
    const val BUTTON_ENABLED_WHEN_RUNNING =
        "This button will only be enabled when there is a running query."
    const val BUTTON_ENABLED_WHEN_RUNNABLE =
        "This button will only be enabled when a session is opened to a database, and runnable file is opened and active " +
                "while no other query is running. " + RUNNABLE_FILE_EXTENSIONS
    const val CANNOT_BE_UNDONE =
        "This action cannot be undone."
    const val CHANGE_OVERRIDDEN_OWNED_ATT_TYPE =
        "Change the %s type '%s' owning attribute type '%s' from overriding attribute type '%s'."
    const val CHANGE_OVERRIDDEN_OWNED_ATT_TYPE_TO_SET =
        "Change the %s type '%s' owning attribute type '%s' to override an attribute type."
    const val CHANGE_OVERRIDDEN_PLAYED_ROLE_TYPE =
        "Change the %s type '%s' playing role type '%s' from overriding role type '%s'."
    const val CHANGE_OVERRIDDEN_PLAYED_ROLE_TYPE_TO_SET =
        "Change the %s type '%s' playing role type '%s' to override a role type."
    const val CHANGE_OVERRIDDEN_RELATED_ROLE_TYPE =
        "Change the relation type '%s' relating role type '%s' from overriding role type '%s'."
    const val CHANGE_OVERRIDDEN_RELATED_ROLE_TYPE_TO_SET =
        "Change the relation type '%s' relating role type '%s' to override a role type."
    const val CHANGE_SUPERTYPE =
        "Change the supertype of the %s type '%s'."
    const val CHANGE_TYPE_ABSTRACTNESS =
        "Change this  %s type with label '%s' to abstract or not."
    const val CONFIRM_DATABASE_DELETION =
        "Are you sure you would like to delete the database '%s' and all of its content? $ACTION_CANNOT_BE_UNDONE " +
                "Enter the database name below in order to confirm its deletion."
    const val CONFIRM_DIRECTORY_DELETION =
        "Are you sure you would like to delete this directory and all of its content? $ACTION_CANNOT_BE_UNDONE"
    const val CONFIRM_FILE_DELETION =
        "Are you sure you would like to delete this file with the name '%s'? $ACTION_CANNOT_BE_UNDONE"
    const val CONFIRM_TYPE_DELETION =
        "Are you sure you would like to delete this %s type with the label '%s'? $CHANGE_NEEDS_COMMIT"
    const val CONFIRM_FILE_TYPE_CHANGE_NON_RUNNABLE =
        "You are about to convert this runnable file (%s) to a non-runnable file (%s). " + RUNNABLE_FILE_EXTENSIONS +
                " Are you sure you want to proceed?"
    const val CONFIRM_QUITING_APPLICATION =
        "Are you sure you want to close TypeDB Studio? Unsaved files will still be available when you reopen."
    const val CREATE_DATABASE_BUTTON_DESCRIPTION =
        "Create a new database on your TypeDB Server. This button will only be enabled the provided name is valid, " +
                "i.e. valid syntax and does not already exist."
    const val CREATE_DIRECTORY =
        "Create a new directory under %s."
    const val CREATE_FILE =
        "Create a new file under %s."
    const val CREATE_TYPE =
        "Create a new %s type."
    const val CREATE_TYPE_AS_SUBTYPE_OF =
        "Create a new %s type as a subtype of '%s'."
    const val CREATE_AN_ADDRESS_FOR_CLOUD =
        "Create a new address for connecting to your TypeDB Cloud."
    const val EDITING_TYPES_REQUIREMENT_DESCRIPTION =
        "To add/delete/edit type definitions in the database schema, you must be on a 'schema' session and 'write' transaction."

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
    const val MANAGE_ADDRESSES_MESSAGE =
        "Below is the list of server addresses of your TypeDB Cloud. You can remove them from the list below and/or " +
                "add new ones."
    const val MANAGE_DATABASES_DESCRIPTION =
        "You can manage your databases by adding or deleting databases from the TypeDB Server you are connected to. " +
                BUTTON_ENABLED_WHEN_CONNECTED
    const val MANAGE_DATABASES_MESSAGE =
        "Below is the list of databases on your TypeDB Server. You can delete them individually, or create new ones. " +
                "Additionally, you can export a database's schema."
    const val OUTPUT_RESPONSE_TIME_DESCRIPTION =
        "Duration to collect all answers of the query from the server."
    const val PREFERENCES_GRAPH_OUTPUT_CAPTION =
        "When running a match query, display a graph output."
    const val PREFERENCES_IGNORED_PATHS_CAPTION = "Ignore files matching glob expressions. Separate entries by line."
    const val PREFERENCES_MATCH_QUERY_LIMIT_CAPTION =
        "When running a match query, limit the number of results to this value."
    const val PREFERENCES_TRANSACTION_TIMEOUT_CAPTION =
        "Set the timeout of transactions (in minutes) to this value."
    const val QUERY_RESPONSE_TIME_DESCRIPTION =
        "Duration to collect auxiliary information/concepts to display informative log & graph output."
    const val RENAME_DIRECTORY =
        "Rename the directory at %s."
    const val RENAME_FILE =
        "Rename the file at %s."
    const val RENAME_TYPE =
        "Rename the %s type with label '%s'."
    const val SAVE_CURRENT_FILE_DESCRIPTION =
        "By default, edited text files are automatically saved, except for untitled files that have not been saved. " +
                "This button will only be enabled if the currently opened file needs saving."
    const val SAVE_OR_DELETE_FILE =
        "Would you like to save this file before closing it? Closing it without saving would delete this file and its content."
    const val SCHEMA_EXCEPTIONS_DESCRIPTION =
        "Unresolved schema exceptions that you should resolve before committing the changes in the current transaction."
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
    const val STOP_RUNNING_QUERY_BEFORE_CLOSING_OUTPUT_GROUP_TAB_DESCRIPTION =
        "The running query associated to this output group tab should be stopped before closing this tab."
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
    const val UPDATE_DEFAULT_PASSWORD_FOR_USERNAME =
        "Update default password for username '%s'."
    const val UPDATE_DEFAULT_PASSWORD_INSTRUCTION =
        "Update your initial default password below. The new password must be different, and the repeated password must be identical."
}
