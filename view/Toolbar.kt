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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.connection.Connection
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.CONNECTED
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.CONNECTING
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.DISCONNECTED
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

    private val isConnected get() = GlobalState.connection.isConnected
    private val isInteractive get() = GlobalState.connection.current?.isInteractiveMode == true
    private val hasOpenSession get() = GlobalState.connection.hasOpenSession
    private val hasOpenTx get() = GlobalState.connection.current?.hasOpenTransaction == true
    private val isSchema get() = GlobalState.connection.current?.config?.sessionType == TypeDBSession.Type.SCHEMA
    private val isData get() = GlobalState.connection.current?.config?.sessionType == TypeDBSession.Type.DATA
    private val isRead get() = GlobalState.connection.current?.config?.transactionType == TypeDBTransaction.Type.READ
    private val isWrite get() = GlobalState.connection.current?.config?.transactionType == TypeDBTransaction.Type.WRITE
    private val isSnapshot get() = GlobalState.connection.current?.config?.snapshot == true
    private val isSnapshotEnabled get() = GlobalState.connection.current?.config?.snapshotEnabled == true
    private val isInfer get() = GlobalState.connection.current?.config?.infer == true
    private val isInferEnabled get() = GlobalState.connection.current?.config?.inferEnabled == true
    private val isExplain get() = GlobalState.connection.current?.config?.explain == true
    private val isExplainEnabled get() = GlobalState.connection.current?.config?.explainEnabled == true
    private val isCommitting get() = GlobalState.connection.current?.isCommitting == true
    private val hasRunnable get() = GlobalState.resource.active?.isRunnable == true
    private val hasRunningCommand get() = GlobalState.connection.current?.hasRunningCommand == true

    @Composable
    fun Layout() {
        Row(
            modifier = Modifier.fillMaxWidth().height(TOOLBAR_SIZE),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Project.Buttons()
            VerticalSeparator()
            Database.Buttons()
            VerticalSeparator()
            TransactionConfig.Buttons()
            VerticalSeparator()
            TransactionControl.Buttons()
            VerticalSeparator()
            Run.Buttons()
            Spacer(Modifier.weight(1f))
            Major.Buttons()
        }
    }

    @Composable
    private fun VerticalSeparator() {
        Separator.Vertical(modifier = Modifier.height(TOOLBAR_SEPARATOR_HEIGHT))
    }

    @Composable
    private fun ToolbarSpace() {
        Spacer(Modifier.width(TOOLBAR_SPACING))
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
    private fun ToggleButton(
        text: String,
        onClick: () -> Unit,
        isActive: Boolean,
        enabled: Boolean,
        tooltip: Tooltip.Arg
    ): TextButtonArg {
        return TextButtonArg(
            text = text,
            onClick = onClick,
            color = { if (isActive) Theme.colors.secondary else Theme.colors.onPrimary },
            enabled = enabled,
            tooltip = tooltip
        )
    }

    object Project {

        @Composable
        internal fun Buttons() {
            ToolbarSpace()
            OpenProjectButton()
            ToolbarSpace()
            SaveButton()
            ToolbarSpace()
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
            ToolbarSpace()
            ManageDatabasesButton()
            ToolbarSpace()
            DatabaseDropdown(Modifier.height(TOOLBAR_BUTTON_SIZE))
            ToolbarSpace()
        }

        @Composable
        private fun ManageDatabasesButton() {
            ToolbarIconButton(
                icon = Icon.Code.DATABASE,
                onClick = {
                    GlobalState.connection.current!!.refreshDatabaseList()
                    GlobalState.connection.manageDatabasesDialog.open()
                },
                enabled = isConnected,
                tooltip = Tooltip.Arg(
                    title = Label.MANAGE_DATABASES,
                    description = Sentence.MANAGE_DATABASES_DESCRIPTION,
                )
            )
        }
    }

    object TransactionConfig {

        @Composable
        internal fun Buttons() {
            val isInteractive = GlobalState.connection.current?.isInteractiveMode == true
            ToolbarSpace()
            SessionTypeButton(isInteractive)
            ToolbarSpace()
            TransactionTypeButtons(isInteractive)
            ToolbarSpace()
            OptionsButtons(isInteractive)
            ToolbarSpace()
        }

        @Composable
        private fun SessionTypeButton(enabled: Boolean) {
            val schema = TypeDBSession.Type.SCHEMA
            val data = TypeDBSession.Type.DATA
            TextButtonRow(
                height = TOOLBAR_BUTTON_SIZE,
                buttons = listOf(
                    ToggleButton(
                        text = schema.name.lowercase(),
                        onClick = { GlobalState.connection.current?.updateSessionType(schema) },
                        isActive = enabled && isSchema,
                        enabled = enabled && hasOpenSession && !hasOpenTx,
                        tooltip = Tooltip.Arg(
                            title = Label.SCHEMA_SESSION,
                            description = Sentence.SESSION_SCHEMA_DESCRIPTION,
                            url = URL.DOCS_SESSION_SCHEMA
                        )
                    ),
                    ToggleButton(
                        text = data.name.lowercase(),
                        onClick = { GlobalState.connection.current?.updateSessionType(data) },
                        isActive = enabled && isData,
                        enabled = enabled && hasOpenSession && !hasOpenTx,
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
                    ToggleButton(
                        text = write.name.lowercase(),
                        onClick = { GlobalState.connection.current?.updateTransactionType(write) },
                        isActive = enabled && isWrite,
                        enabled = enabled && hasOpenSession && !hasOpenTx,
                        tooltip = Tooltip.Arg(
                            title = Label.WRITE_TRANSACTION,
                            description = Sentence.TRANSACTION_WRITE_DESCRIPTION,
                            url = URL.DOCS_TRANSACTION_WRITE
                        )
                    ),
                    ToggleButton(
                        text = read.name.lowercase(),
                        onClick = { GlobalState.connection.current?.updateTransactionType(read) },
                        isActive = enabled && isRead,
                        enabled = enabled && hasOpenSession && !hasOpenTx,
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
                    ToggleButton(
                        text = Label.SNAPSHOT.lowercase(),
                        onClick = { GlobalState.connection.current?.config?.toggleSnapshot() },
                        isActive = enabled && isSnapshot,
                        enabled = enabled && !hasOpenTx && isSnapshotEnabled,
                        tooltip = Tooltip.Arg(
                            title = Label.ENABLE_SNAPSHOT,
                            description = Sentence.ENABLE_SNAPSHOT_DESCRIPTION,
                            url = URL.DOCS_ENABLE_SNAPSHOT
                        )
                    ),
                    ToggleButton(
                        text = Label.INFER.lowercase(),
                        onClick = { GlobalState.connection.current?.config?.toggleInfer() },
                        isActive = enabled && isInfer,
                        enabled = enabled && !hasOpenTx && isInferEnabled,
                        tooltip = Tooltip.Arg(
                            title = Label.ENABLE_INFERENCE,
                            description = Sentence.ENABLE_INFERENCE_DESCRIPTION,
                            url = URL.DOCS_ENABLE_INFERENCE
                        )
                    ),
                    ToggleButton(
                        text = Label.EXPLAIN.lowercase(),
                        onClick = { GlobalState.connection.current?.config?.toggleExplain() },
                        isActive = enabled && isExplain,
                        enabled = enabled && !hasOpenTx && isExplainEnabled,
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

    object TransactionControl {

        @Composable
        internal fun Buttons() {
            ToolbarSpace()
            StatusIndicator()
            ToolbarSpace()
            CloseButton()
            ToolbarSpace()
            RollbackButton()
            ToolbarSpace()
            CommitButton()
            ToolbarSpace()
        }

        @Composable
        private fun StatusIndicator() {
            if (isCommitting) LoadingIndicator(Modifier.size(TOOLBAR_BUTTON_SIZE)) else OnlineIndicator()
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
        private fun CloseButton() {
            ToolbarIconButton(
                icon = Icon.Code.XMARK,
                onClick = { GlobalState.connection.current?.closeTransaction() },
                color = Theme.colors.error,
                enabled = isInteractive && !isCommitting && hasOpenTx,
                tooltip = Tooltip.Arg(
                    title = Label.CLOSE_TRANSACTION,
                    description = Sentence.TRANSACTION_CLOSE_DESCRIPTION,
                    url = URL.DOCS_TRANSACTION_CLOSE,
                )
            )
        }

        @Composable
        private fun RollbackButton() {
            ToolbarIconButton(
                icon = Icon.Code.ROTATE_LEFT,
                onClick = { GlobalState.connection.current?.rollbackTransaction() },
                color = Theme.colors.quaternary2,
                enabled = isInteractive && !isCommitting && hasOpenTx && GlobalState.connection.current!!.isWrite,
                tooltip = Tooltip.Arg(
                    title = Label.ROLLBACK_TRANSACTION,
                    description = Sentence.TRANSACTION_ROLLBACK_DESCRIPTION,
                    url = URL.DOCS_TRANSACTION_ROLLBACK,
                )
            )
        }

        @Composable
        private fun CommitButton() {
            ToolbarIconButton(
                icon = Icon.Code.CHECK,
                onClick = { GlobalState.connection.current?.commitTransaction() },
                color = Theme.colors.secondary,
                enabled = isInteractive && !isCommitting && hasOpenTx && GlobalState.connection.current!!.isWrite,
                tooltip = Tooltip.Arg(
                    title = Label.COMMIT_TRANSACTION,
                    description = Sentence.TRANSACTION_COMMIT_DESCRIPTION,
                    url = URL.DOCS_TRANSACTION_COMMIT
                )
            )
        }
    }

    object Run {

        @Composable
        internal fun Buttons() {
            ToolbarSpace()
            PlayButton()
            ToolbarSpace()
            StopButton()
            ToolbarSpace()
        }

        @Composable
        private fun PlayButton() {
            ToolbarIconButton(
                icon = Icon.Code.PLAY,
                color = Theme.colors.secondary,
                onClick = { GlobalState.connection.current?.run(GlobalState.resource.active!!) },
                enabled = hasOpenSession && hasRunnable && !hasRunningCommand && !isCommitting,
                tooltip = Tooltip.Arg(
                    title = if (GlobalState.connection.isScriptMode) Label.RUN_SCRIPT else Label.RUN_QUERY,
                    description = Sentence.BUTTON_ENABLED_WHEN_RUNNABLE
                )
            )
        }

        @Composable
        private fun StopButton() {
            ToolbarIconButton(
                icon = Icon.Code.BOLT,
                color = Theme.colors.error,
                onClick = { GlobalState.connection.current!!.sendStopSignal() },
                enabled = hasRunningCommand && !isCommitting,
                tooltip = Tooltip.Arg(title = Label.STOP_SIGNAL, description = Sentence.STOP_SIGNAL_DESCRIPTION)
            )
        }
    }

    object Major {

        private val connectionName
            get() = (GlobalState.connection.current!!.username?.let { "$it@" } ?: "") +
                    GlobalState.connection.current!!.address

        @Composable
        internal fun Buttons() {
//            ToolbarSpace()
//            ModeButtons() // TODO
            ToolbarSpace()
            ConnectionButton()
            ToolbarSpace()
        }

        @Composable
        private fun ConnectionButton() {
            when (GlobalState.connection.status) {
                DISCONNECTED -> ConnectionButton(Label.CONNECT_TO_TYPEDB)
                CONNECTING -> ConnectionButton(Label.CONNECTING)
                CONNECTED -> ConnectionButton(connectionName)
            }
        }

        @Composable
        private fun ModeButtons() {
            val interactive = Connection.Mode.INTERACTIVE
            val script = Connection.Mode.SCRIPT
            TextButtonRow(
                height = TOOLBAR_BUTTON_SIZE,
                buttons = listOf(
                    ToggleButton(
                        text = interactive.name.lowercase(),
                        onClick = { GlobalState.connection.current?.mode = interactive },
                        isActive = GlobalState.connection.isInteractiveMode,
                        enabled = isConnected,
                        tooltip = Tooltip.Arg(
                            title = Label.INTERACTIVE_MODE,
                            description = Sentence.INTERACTIVE_MODE_DESCRIPTION,
                            url = URL.DOCS_MODE_INTERACTIVE,
                        )
                    ),
                    ToggleButton(
                        text = script.name.lowercase(),
                        onClick = { GlobalState.connection.current?.mode = script },
                        isActive = GlobalState.connection.isScriptMode,
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
        private fun ConnectionButton(text: String) {
            TextButton(
                text = text,
                onClick = { GlobalState.connection.connectServerDialog.open() },
                modifier = Modifier.height(TOOLBAR_BUTTON_SIZE),
                trailingIcon = Icon.Code.SERVER,
            )
        }
    }
}
