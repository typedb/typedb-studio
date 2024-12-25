/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.module

import com.typedb.driver.api.Transaction as TypeDBTransaction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.typedb.studio.framework.common.URL
import com.typedb.studio.framework.common.Util.mayTruncate
import com.typedb.studio.framework.common.theme.Theme
import com.typedb.studio.framework.common.theme.Theme.TOOLBAR_BUTTON_SIZE
import com.typedb.studio.framework.common.theme.Theme.TOOLBAR_SEPARATOR_HEIGHT
import com.typedb.studio.framework.common.theme.Theme.TOOLBAR_SIZE
import com.typedb.studio.framework.common.theme.Theme.TOOLBAR_SPACING
import com.typedb.studio.framework.material.Form
import com.typedb.studio.framework.material.Form.IconButton
import com.typedb.studio.framework.material.Form.LoadingIndicator
import com.typedb.studio.framework.material.Form.RawIconButton
import com.typedb.studio.framework.material.Form.TextButton
import com.typedb.studio.framework.material.Form.TextButtonArg
import com.typedb.studio.framework.material.Form.TextButtonRow
import com.typedb.studio.framework.material.Form.toggleButtonColor
import com.typedb.studio.framework.material.Icon
import com.typedb.studio.framework.material.Separator
import com.typedb.studio.framework.material.Tooltip
import com.typedb.studio.module.connection.DatabaseDialog.DatabaseDropdown
import com.typedb.studio.service.Service
import com.typedb.studio.service.common.util.Label
import com.typedb.studio.service.common.util.Property.FileType.Companion.RUNNABLE_EXTENSIONS_STR
import com.typedb.studio.service.common.util.Sentence
import com.typedb.studio.service.connection.DriverState
import com.typedb.studio.service.connection.DriverState.Status.CONNECTED
import com.typedb.studio.service.connection.DriverState.Status.CONNECTING
import com.typedb.studio.service.connection.DriverState.Status.DISCONNECTED

object Toolbar {

    private const val CONNECTION_NAME_LENGTH_LIMIT = 50

    private val isConnected get() = Service.driver.isConnected
    private val isScript get() = Service.driver.isScriptMode
    private val isInteractive get() = Service.driver.isInteractiveMode
    private val hasOpenTx get() = Service.driver.transaction.isOpen
    private val isSchemaTransaction get() = Service.driver.transaction.isSchema
    private val isWriteTransaction get() = Service.driver.transaction.isWrite
    private val isReadTransaction get() = Service.driver.transaction.isRead
    private val isReadyToRunQuery get() = Service.driver.isReadyToRunQuery
    private val hasRunnablePage get() = Service.pages.active?.isRunnable == true
    private val hasRunningQuery get() = Service.driver.hasRunningQuery
    private val hasRunningCommand get() = Service.driver.hasRunningCommand /*|| Service.schema.hasRunningCommand*/ // TODO
    private val hasStopSignal get() = Service.driver.transaction.hasStopSignal

    @Composable
    fun Layout() {
        Row(
            modifier = Modifier.fillMaxWidth().height(TOOLBAR_SIZE).padding(horizontal = TOOLBAR_SPACING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarRow {
                Project.Buttons()
                VerticalSeparator()
                Database.Buttons()
                VerticalSeparator()
                Run.Buttons()
            }
            Spacer(Modifier.weight(1f))
            Major.Buttons()
        }
    }

    @Composable
    private fun VerticalSeparator() {
        Separator.Vertical(modifier = Modifier.height(TOOLBAR_SEPARATOR_HEIGHT))
    }

    @Composable
    private fun ToolbarRow(content: @Composable RowScope.() -> Unit) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(TOOLBAR_SPACING),
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
    }

    @Composable
    private fun ToolbarIconButton(
        icon: Icon,
        onClick: () -> Unit,
        color: Color = Theme.studio.icon,
        enabled: Boolean = true,
        tooltip: Tooltip.Arg? = null
    ) {
        IconButton(
            icon = icon,
            modifier = Modifier.size(TOOLBAR_BUTTON_SIZE),
            iconColor = if (enabled) color else Theme.studio.icon,
            enabled = enabled,
            tooltip = tooltip,
            onClick = onClick
        )
    }

    object Project {

        @Composable
        internal fun Buttons() {
            ToolbarRow {
                OpenProjectButton()
                SaveButton()
            }
        }

        @Composable
        private fun OpenProjectButton() {
            ToolbarIconButton(
                icon = Icon.FOLDER_OPEN,
                onClick = { Service.project.openProjectDialog.open() },
                tooltip = Tooltip.Arg(title = Label.OPEN_PROJECT_DIRECTORY)
            )
        }

        @Composable
        private fun SaveButton() {
            val activePage = Service.pages.active
            ToolbarIconButton(
                icon = Icon.SAVE,
                onClick = { activePage?.initiateSave() },
                enabled = activePage?.hasUnsavedChanges == true || activePage?.isUnsavedPageable == true,
                tooltip = Tooltip.Arg(
                    title = Label.SAVE_CURRENT_FILE,
                    description = Sentence.SAVE_CURRENT_FILE_DESCRIPTION
                )
            )
        }
    }

    object Database {

        @Composable
        fun Buttons() {
            val dbButtonsEnabled = isConnected && isInteractive && !hasRunningCommand
            ToolbarRow {
                Manager.Buttons(dbButtonsEnabled)
                VerticalSeparator()
                Transaction.Buttons(dbButtonsEnabled)
            }
        }

        object Manager {

            @Composable
            internal fun Buttons(enabled: Boolean) = ToolbarRow {
                ManageDatabasesButton(enabled && !hasOpenTx)
                DatabaseDropdown(Modifier.height(TOOLBAR_BUTTON_SIZE), enabled = enabled && !hasOpenTx)
            }

            @Composable
            private fun ManageDatabasesButton(enabled: Boolean) = ToolbarIconButton(
                icon = Icon.DATABASE,
                onClick = {
                    Service.driver.refreshDatabaseList()
                    Service.driver.manageDatabasesDialog.open()
                },
                enabled = enabled,
                tooltip = Tooltip.Arg(
                    title = Label.MANAGE_DATABASES,
                    description = Sentence.MANAGE_DATABASES_DESCRIPTION,
                )
            )
        }

        object Transaction {

            @Composable
            internal fun Buttons(enabled: Boolean) = ToolbarRow {
                Config.Buttons(enabled)
                VerticalSeparator()
                Controller.Buttons(enabled)
            }

            object Config {

                @Composable
                internal fun Buttons(enabled: Boolean) {
                    val configEnabled = enabled && !hasOpenTx
                    ToolbarRow {
                        TransactionTypeButtons(configEnabled)
                    }
                }

                @Composable
                private fun TransactionTypeButtons(enabled: Boolean) {
                    val schema = TypeDBTransaction.Type.SCHEMA
                    val write = TypeDBTransaction.Type.WRITE
                    val read = TypeDBTransaction.Type.READ
                    TextButtonRow(
                        height = TOOLBAR_BUTTON_SIZE,
                        buttons = listOf(
                            TextButtonArg(
                                text = Label.SCHEMA.lowercase(),
                                onClick = { Service.driver.tryUpdateTransactionType(schema) },
                                color = { toggleButtonColor(isSchemaTransaction) },
                                enabled = enabled,
                                tooltip = Tooltip.Arg(
                                    title = Label.SCHEMA_TRANSACTION,
                                    description = Sentence.TRANSACTION_SCHEMA_DESCRIPTION,
                                    url = URL.DOCS_TRANSACTION_SCHEMA
                                )
                            ),
                            TextButtonArg(
                                text = Label.WRITE.lowercase(),
                                onClick = { Service.driver.tryUpdateTransactionType(write) },
                                color = { toggleButtonColor(isWriteTransaction) },
                                enabled = enabled,
                                tooltip = Tooltip.Arg(
                                    title = Label.WRITE_TRANSACTION,
                                    description = Sentence.TRANSACTION_WRITE_DESCRIPTION,
                                    url = URL.DOCS_TRANSACTION_WRITE
                                )
                            ),
                            TextButtonArg(
                                text = Label.READ.lowercase(),
                                onClick = { Service.driver.tryUpdateTransactionType(read) },
                                color = { toggleButtonColor(isReadTransaction) },
                                enabled = enabled,
                                tooltip = Tooltip.Arg(
                                    title = Label.READ_TRANSACTION,
                                    description = Sentence.TRANSACTION_READ_DESCRIPTION,
                                    url = URL.DOCS_TRANSACTION_READ
                                )
                            )
                        )
                    )
                }
            }

            object Controller {

                @Composable
                internal fun Buttons(enabled: Boolean) {
                    val controlsEnabled = enabled && hasOpenTx
                    ToolbarRow {
                        StatusIndicator()
                        CloseButton(controlsEnabled)
                        RollbackButton(controlsEnabled)
                        CommitButton(controlsEnabled)
                    }
                }

                @Composable
                private fun StatusIndicator() {
                    if (hasRunningCommand || hasRunningQuery) LoadingIndicator(Modifier.size(TOOLBAR_BUTTON_SIZE))
                    else OnlineIndicator()
                }

                @Composable
                private fun OnlineIndicator() {
                    RawIconButton(
                        icon = Icon.ONLINE,
                        modifier = Modifier.size(TOOLBAR_BUTTON_SIZE),
                        iconColor = if (isInteractive && hasOpenTx) Theme.studio.secondary else Theme.studio.icon,
                        enabled = isInteractive && isConnected,
                        tooltip = Tooltip.Arg(
                            title = Label.TRANSACTION_STATUS,
                            description = Sentence.TRANSACTION_STATUS_DESCRIPTION
                        )
                    )
                }

                @Composable
                private fun CloseButton(enabled: Boolean) {
                    ToolbarIconButton(
                        icon = Icon.CLOSE,
                        onClick = { Service.driver.closeTransactionAsync() },
                        color = Theme.studio.errorStroke,
                        enabled = enabled,
                        tooltip = Tooltip.Arg(
                            title = Label.CLOSE_TRANSACTION,
                            description = Sentence.TRANSACTION_CLOSE_DESCRIPTION,
                            url = URL.DOCS_TRANSACTION_CLOSE,
                        )
                    )
                }

                @Composable
                private fun RollbackButton(enabled: Boolean) {
                    ToolbarIconButton(
                        icon = Icon.ROLLBACK,
                        onClick = { Service.driver.rollbackTransaction() },
                        color = Theme.studio.warningStroke,
                        enabled = enabled && isWriteTransaction,
                        tooltip = Tooltip.Arg(
                            title = Label.ROLLBACK_TRANSACTION,
                            description = Sentence.TRANSACTION_ROLLBACK_DESCRIPTION,
                            url = URL.DOCS_TRANSACTION_ROLLBACK,
                        )
                    )
                }

                @Composable
                private fun CommitButton(enabled: Boolean) {
                    ToolbarIconButton(
                        icon = Icon.COMMIT,
                        onClick = { Service.driver.commitTransaction() },
                        color = Theme.studio.secondary,
                        enabled = enabled && isWriteTransaction,
                        tooltip = Tooltip.Arg(
                            title = Label.COMMIT_TRANSACTION,
                            description = Sentence.TRANSACTION_COMMIT_DESCRIPTION,
                            url = URL.DOCS_TRANSACTION_COMMIT
                        )
                    )
                }
            }
        }
    }

    object Run {

        @Composable
        internal fun Buttons() {
            ToolbarRow {
                RunButton()
                StopButton()
            }
        }

        @Composable
        private fun RunButton() {
            ToolbarIconButton(
                icon = Icon.RUN,
                color = Theme.studio.secondary,
                onClick = {
                    Service.pages.active?.let {
                        if (it.isRunnable) it.asRunnable().mayOpenAndRun()
                    }
                },
                enabled = isReadyToRunQuery && hasRunnablePage,
                tooltip = Tooltip.Arg(
                    title = if (isInteractive) Label.RUN_QUERY else Label.RUN_SCRIPT,
                    description = Sentence.BUTTON_ENABLED_WHEN_RUNNABLE.format(RUNNABLE_EXTENSIONS_STR)
                )
            )
        }

        @Composable
        private fun StopButton() {
            ToolbarIconButton(
                icon = Icon.STOP,
                color = Theme.studio.errorStroke,
                onClick = { Service.driver.sendStopSignal() },
                enabled = hasRunningQuery && !hasStopSignal,
                tooltip = Tooltip.Arg(title = Label.STOP_SIGNAL, description = Sentence.STOP_SIGNAL_DESCRIPTION)
            )
        }
    }

    object Major {

        @Composable
        internal fun Buttons() {
            ToolbarRow {
                // TODO: ModeButtons()
                ConnectionButton()
                OpenPreferencesDialogButton()
            }
        }

        @Composable
        private fun ModeButtons() {
            val interactive = DriverState.Mode.INTERACTIVE
            val script = DriverState.Mode.SCRIPT
            TextButtonRow(
                height = TOOLBAR_BUTTON_SIZE,
                buttons = listOf(
                    TextButtonArg(
                        text = interactive.name.lowercase(),
                        onClick = { Service.driver.mode = interactive },
                        color = { toggleButtonColor(isActive = isConnected && isInteractive) },
                        enabled = isConnected,
                        tooltip = Tooltip.Arg(
                            title = Label.INTERACTIVE_MODE,
                            description = Sentence.INTERACTIVE_MODE_DESCRIPTION,
                            url = URL.DOCS_MODE_INTERACTIVE,
                        )
                    ),
                    TextButtonArg(
                        text = script.name.lowercase(),
                        onClick = { Service.driver.mode = script },
                        color = { toggleButtonColor(isActive = isConnected && isScript) },
                        enabled = isConnected,
                        tooltip = Tooltip.Arg(
                            title = Label.SCRIPT_MODE,
                            description = Sentence.SCRIPT_MODE_DESCRIPTION,
                            url = URL.DOCS_MODE_SCRIPT,
                        )
                    )
                )
            )
        }

        @Composable
        private fun ConnectionButton() {
            when (Service.driver.status) {
                DISCONNECTED -> ConnectionButton(Label.CONNECT_TO_TYPEDB)
                CONNECTING -> ConnectionButton(Label.CONNECTING)
                CONNECTED -> ConnectionButton(mayTruncate(Service.driver.connectionName!!, CONNECTION_NAME_LENGTH_LIMIT))
            }
        }

        @Composable
        private fun ConnectionButton(text: String) {
            TextButton(
                text = text,
                modifier = Modifier.height(TOOLBAR_BUTTON_SIZE),
                leadingIcon = Form.IconArg(Icon.CONNECT_TO_TYPEDB),
                tooltip = Tooltip.Arg(title = Label.CONNECT_TO_TYPEDB)
            ) { Service.driver.connectServerDialog.open() }
        }

        @Composable
        private fun OpenPreferencesDialogButton() {
            ToolbarIconButton(
                icon = Icon.PREFERENCES,
                onClick = { Service.preference.preferencesDialog.toggle() },
                tooltip = Tooltip.Arg(title = Label.OPEN_PREFERENCES)
            )
        }
    }
}
