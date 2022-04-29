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

package com.vaticle.typedb.studio.view

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
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.connection.ClientState
import com.vaticle.typedb.studio.state.connection.ClientState.Status.CONNECTED
import com.vaticle.typedb.studio.state.connection.ClientState.Status.CONNECTING
import com.vaticle.typedb.studio.state.connection.ClientState.Status.DISCONNECTED
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.URL
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.LoadingIndicator
import com.vaticle.typedb.studio.view.common.component.Form.RawIconButton
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Form.TextButtonArg
import com.vaticle.typedb.studio.view.common.component.Form.TextButtonRow
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.component.Tooltip
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.TOOLBAR_BUTTON_SIZE
import com.vaticle.typedb.studio.view.common.theme.Theme.TOOLBAR_SEPARATOR_HEIGHT
import com.vaticle.typedb.studio.view.common.theme.Theme.TOOLBAR_SIZE
import com.vaticle.typedb.studio.view.common.theme.Theme.TOOLBAR_SPACING
import com.vaticle.typedb.studio.view.dialog.DatabaseDialog.DatabaseDropdown

object Toolbar {

    private val isConnected get() = GlobalState.client.isConnected
    private val isScript get() = GlobalState.client.isScriptMode
    private val isInteractive get() = GlobalState.client.isInteractiveMode
    private val hasOpenSession get() = GlobalState.client.isConnected && GlobalState.client.session.isOpen
    private val hasOpenTx get() = GlobalState.client.session.transaction.isOpen
    private val isSchemaSession get() = GlobalState.client.session.isSchema
    private val isDataSession get() = GlobalState.client.session.isData
    private val isReadTransaction get() = GlobalState.client.session.transaction.isRead
    private val isWriteTransaction get() = GlobalState.client.session.transaction.isWrite
    private val isSnapshotActivated get() = GlobalState.client.session.transaction.snapshot.activated
    private val isSnapshotEnabled get() = GlobalState.client.session.transaction.snapshot.enabled
    private val isInferActivated get() = GlobalState.client.session.transaction.infer.activated
    private val isInferEnabled get() = GlobalState.client.session.transaction.infer.enabled
    private val isExplainActivated get() = GlobalState.client.session.transaction.explain.activated
    private val isExplainEnabled get() = GlobalState.client.session.transaction.explain.enabled
    private val isReadyToRunQuery get() = GlobalState.client.isReadyToRunQuery
    private val hasRunnablePage get() = GlobalState.resource.active?.isRunnable == true
    private val hasRunningQuery get() = GlobalState.client.hasRunningQuery
    private val hasRunningCommand get() = GlobalState.client.hasRunningCommand

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
        icon: Icon.Code,
        onClick: () -> Unit,
        color: Color = Theme.colors.icon,
        enabled: Boolean = true,
        tooltip: Tooltip.Arg? = null
    ) {
        IconButton(
            icon = icon,
            onClick = onClick,
            modifier = Modifier.size(TOOLBAR_BUTTON_SIZE),
            iconColor = if (enabled) color else Theme.colors.icon,
            enabled = enabled,
            tooltip = tooltip
        )
    }

    @Composable
    private fun toggleButtonColor(isActive: Boolean): Color {
        return if (isActive) Theme.colors.secondary else Theme.colors.onPrimary
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
                icon = Icon.Code.FOLDER_OPEN,
                onClick = { GlobalState.project.openProjectDialog.toggle() },
                tooltip = Tooltip.Arg(title = Label.OPEN_PROJECT_DIRECTORY)
            )
        }

        @Composable
        private fun SaveButton() {
            val activePage = GlobalState.resource.active
            ToolbarIconButton(
                icon = Icon.Code.FLOPPY_DISK,
                onClick = { GlobalState.resource.saveAndReopen(activePage!!) },
                enabled = activePage?.hasUnsavedChanges == true || activePage?.isUnsavedResource == true,
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
            internal fun Buttons(enabled: Boolean) {
                val dbManagerEnabled = enabled && !hasOpenTx
                ToolbarRow {
                    ManageDatabasesButton(dbManagerEnabled)
                    DatabaseDropdown(Modifier.height(TOOLBAR_BUTTON_SIZE), enabled = dbManagerEnabled)
                }
            }

            @Composable
            private fun ManageDatabasesButton(enabled: Boolean) {
                ToolbarIconButton(
                    icon = Icon.Code.DATABASE,
                    onClick = {
                        GlobalState.client.refreshDatabaseList()
                        GlobalState.client.manageDatabasesDialog.open()
                    },
                    enabled = enabled,
                    tooltip = Tooltip.Arg(
                        title = Label.MANAGE_DATABASES,
                        description = Sentence.MANAGE_DATABASES_DESCRIPTION,
                    )
                )
            }
        }

        object Transaction {

            @Composable
            internal fun Buttons(enabled: Boolean) {
                val txButtonsEnabled = enabled && hasOpenSession
                ToolbarRow {
                    Config.Buttons(txButtonsEnabled)
                    VerticalSeparator()
                    Controller.Buttons(txButtonsEnabled)
                }
            }

            object Config {

                @Composable
                internal fun Buttons(enabled: Boolean) {
                    val configEnabled = enabled && !hasOpenTx
                    ToolbarRow {
                        SessionTypeButton(configEnabled)
                        TransactionTypeButtons(configEnabled)
                        OptionsButtons(configEnabled)
                    }
                }

                @Composable
                private fun SessionTypeButton(enabled: Boolean) {
                    val schema = TypeDBSession.Type.SCHEMA
                    val data = TypeDBSession.Type.DATA
                    TextButtonRow(
                        height = TOOLBAR_BUTTON_SIZE,
                        buttons = listOf(
                            TextButtonArg(
                                text = schema.name.lowercase(),
                                onClick = { GlobalState.client.tryUpdateSessionType(schema) },
                                color = { toggleButtonColor(isSchemaSession) },
                                enabled = enabled,
                                tooltip = Tooltip.Arg(
                                    title = Label.SCHEMA_SESSION,
                                    description = Sentence.SESSION_SCHEMA_DESCRIPTION,
                                    url = URL.DOCS_SESSION_SCHEMA
                                )
                            ),
                            TextButtonArg(
                                text = data.name.lowercase(),
                                onClick = { GlobalState.client.tryUpdateSessionType(data) },
                                color = { toggleButtonColor(isDataSession) },
                                enabled = enabled,
                                tooltip = Tooltip.Arg(
                                    title = Label.DATA_SESSION,
                                    description = Sentence.SESSION_DATA_DESCRIPTION,
                                    url = URL.DOCS_SESSION_DATA
                                )
                            )
                        )
                    )
                }

                @Composable
                private fun TransactionTypeButtons(enabled: Boolean) {
                    val write = TypeDBTransaction.Type.WRITE
                    val read = TypeDBTransaction.Type.READ
                    TextButtonRow(
                        height = TOOLBAR_BUTTON_SIZE,
                        buttons = listOf(
                            TextButtonArg(
                                text = write.name.lowercase(),
                                onClick = { GlobalState.client.tryUpdateTransactionType(write) },
                                color = { toggleButtonColor(isWriteTransaction) },
                                enabled = enabled,
                                tooltip = Tooltip.Arg(
                                    title = Label.WRITE_TRANSACTION,
                                    description = Sentence.TRANSACTION_WRITE_DESCRIPTION,
                                    url = URL.DOCS_TRANSACTION_WRITE
                                )
                            ),
                            TextButtonArg(
                                text = read.name.lowercase(),
                                onClick = { GlobalState.client.tryUpdateTransactionType(read) },
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

                @Composable
                private fun OptionsButtons(enabled: Boolean) {
                    TextButtonRow(
                        height = TOOLBAR_BUTTON_SIZE,
                        buttons = listOf(
                            TextButtonArg(
                                text = Label.SNAPSHOT.lowercase(),
                                onClick = { GlobalState.client.session.transaction.snapshot.toggle() },
                                color = { toggleButtonColor(isSnapshotActivated) },
                                enabled = enabled && isSnapshotEnabled,
                                tooltip = Tooltip.Arg(
                                    title = Label.ENABLE_SNAPSHOT,
                                    description = Sentence.ENABLE_SNAPSHOT_DESCRIPTION,
                                    url = URL.DOCS_ENABLE_SNAPSHOT
                                )
                            ),
                            TextButtonArg(
                                text = Label.INFER.lowercase(),
                                onClick = { GlobalState.client.session.transaction.infer.toggle() },
                                color = { toggleButtonColor(isInferActivated) },
                                enabled = enabled && isInferEnabled,
                                tooltip = Tooltip.Arg(
                                    title = Label.ENABLE_INFERENCE,
                                    description = Sentence.ENABLE_INFERENCE_DESCRIPTION,
                                    url = URL.DOCS_ENABLE_INFERENCE
                                )
                            ),
                            TextButtonArg(
                                text = Label.EXPLAIN.lowercase(),
                                onClick = { GlobalState.client.session.transaction.explain.toggle() },
                                color = { toggleButtonColor(isExplainActivated) },
                                enabled = enabled && isExplainEnabled,
                                tooltip = Tooltip.Arg(
                                    title = Label.ENABLE_INFERENCE_EXPLANATION,
                                    description = Sentence.ENABLE_INFERENCE_EXPLANATION_DESCRIPTION,
                                    url = URL.DOCS_ENABLE_INFERENCE_EXPLANATION,
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
                        icon = Icon.Code.CIRCLE,
                        modifier = Modifier.size(TOOLBAR_BUTTON_SIZE),
                        iconColor = if (isInteractive && hasOpenTx) Theme.colors.secondary else Theme.colors.icon,
                        enabled = isInteractive && hasOpenSession,
                        tooltip = Tooltip.Arg(
                            title = Label.TRANSACTION_STATUS,
                            description = Sentence.TRANSACTION_STATUS_DESCRIPTION
                        )
                    )
                }

                @Composable
                private fun CloseButton(enabled: Boolean) {
                    ToolbarIconButton(
                        icon = Icon.Code.XMARK,
                        onClick = { GlobalState.client.closeTransaction() },
                        color = Theme.colors.error,
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
                        icon = Icon.Code.ROTATE_LEFT,
                        onClick = { GlobalState.client.rollbackTransaction() },
                        color = Theme.colors.quaternary2,
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
                        icon = Icon.Code.CHECK,
                        onClick = { GlobalState.client.commitTransaction() },
                        color = Theme.colors.secondary,
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
                PlayButton()
                StopButton()
            }
        }

        @Composable
        private fun PlayButton() {
            ToolbarIconButton(
                icon = Icon.Code.PLAY,
                color = Theme.colors.secondary,
                onClick = {
                    GlobalState.resource.active?.let {
                        if (it.isRunnable) GlobalState.resource.openAndMayRun(it.asRunnable())
                    }
                },
                enabled = isReadyToRunQuery && hasRunnablePage,
                tooltip = Tooltip.Arg(
                    title = if (isInteractive) Label.RUN_QUERY else Label.RUN_SCRIPT,
                    description = Sentence.BUTTON_ENABLED_WHEN_RUNNABLE
                )
            )
        }

        @Composable
        private fun StopButton() {
            ToolbarIconButton(
                icon = Icon.Code.BOLT,
                color = Theme.colors.error,
                onClick = { GlobalState.client.sendStopSignal() },
                enabled = hasRunningQuery && !hasRunningCommand,
                tooltip = Tooltip.Arg(title = Label.STOP_SIGNAL, description = Sentence.STOP_SIGNAL_DESCRIPTION)
            )
        }
    }

    object Major {

        private val connectionName
            get() = (GlobalState.client.username?.let { "$it@" } ?: "") + GlobalState.client.address

        @Composable
        internal fun Buttons() {
            ToolbarRow {
                // TODO: ModeButtons()
                ConnectionButton()
            }
        }

        @Composable
        private fun ModeButtons() {
            val interactive = ClientState.Mode.INTERACTIVE
            val script = ClientState.Mode.SCRIPT
            TextButtonRow(
                height = TOOLBAR_BUTTON_SIZE,
                buttons = listOf(
                    TextButtonArg(
                        text = interactive.name.lowercase(),
                        onClick = { GlobalState.client.mode = interactive },
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
                        onClick = { GlobalState.client.mode = script },
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
            when (GlobalState.client.status) {
                DISCONNECTED -> ConnectionButton(Label.CONNECT_TO_TYPEDB)
                CONNECTING -> ConnectionButton(Label.CONNECTING)
                CONNECTED -> ConnectionButton(connectionName)
            }
        }

        @Composable
        private fun ConnectionButton(text: String) {
            TextButton(
                text = text,
                onClick = { GlobalState.client.connectServerDialog.open() },
                modifier = Modifier.height(TOOLBAR_BUTTON_SIZE),
                trailingIcon = Icon.Code.SERVER,
                tooltip = Tooltip.Arg(title = Label.CONNECT_TO_TYPEDB)
            )
        }
    }
}
