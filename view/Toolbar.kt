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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import com.vaticle.typedb.studio.view.common.component.Form.RawIconButton
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.component.Tooltip
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.ROUNDED_RECTANGLE
import com.vaticle.typedb.studio.view.common.theme.Theme.TOOLBAR_BUTTON_SIZE
import com.vaticle.typedb.studio.view.common.theme.Theme.TOOLBAR_SEPARATOR_HEIGHT
import com.vaticle.typedb.studio.view.common.theme.Theme.TOOLBAR_SIZE
import com.vaticle.typedb.studio.view.common.theme.Theme.TOOLBAR_SPACING
import com.vaticle.typedb.studio.view.dialog.DatabaseDialog.DatabaseDropdown

object Toolbar {

    @Composable
    fun Layout() {
        Row(
            modifier = Modifier.fillMaxWidth().height(TOOLBAR_SIZE),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Project.Buttons()
            VerticalSeparator()
            InteractionSettings.Buttons()
            VerticalSeparator()
            TransactionControl.Buttons()
            VerticalSeparator()
            Run.Buttons()
            Spacer(Modifier.weight(1f))
            DBConnection.Buttons()
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
        tooltip: Tooltip.Args? = null
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
    private fun ToggleButtonRow(content: @Composable RowScope.() -> Unit) {
        Row(Modifier.height(TOOLBAR_BUTTON_SIZE).background(Theme.colors.primary, ROUNDED_RECTANGLE)) { content() }
    }

    @Composable
    private fun ToggleButton(
        text: String,
        onClick: () -> Unit,
        isActive: Boolean,
        enabled: Boolean,
        tooltip: Tooltip.Args
    ) {
        TextButton(
            text = text,
            onClick = onClick,
            textColor = if (isActive) Theme.colors.secondary else Theme.colors.onPrimary,
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
                tooltip = Tooltip.Args(title = Label.OPEN_PROJECT_DIRECTORY)
            )
        }

        @Composable
        private fun SaveButton() {
            val activePage = GlobalState.resource.active
            ToolbarIconButton(
                icon = Icon.Code.FLOPPY_DISK,
                onClick = { GlobalState.resource.saveAndReopen(activePage!!) },
                enabled = activePage?.hasUnsavedChanges == true || activePage?.isUnsavedResource == true,
                tooltip = Tooltip.Args(
                    title = Label.SAVE_CURRENT_FILE,
                    description = Sentence.SAVE_CURRENT_FILE_DESCRIPTION
                )
            )
        }
    }

    object InteractionSettings {

        @Composable
        internal fun Buttons() {
            val isInteractive = GlobalState.connection.current?.isInteractiveMode ?: false
            ToolbarSpace()
            DatabaseDropdown(Modifier.height(TOOLBAR_BUTTON_SIZE))
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
            val hasOpenTransaction = GlobalState.connection.current?.hasOpenTransaction == true
            ToggleButtonRow {
                ToggleButton(
                    text = schema.name.lowercase(),
                    onClick = { GlobalState.connection.current?.updateSessionType(schema) },
                    isActive = enabled && GlobalState.connection.current?.config?.sessionType == schema,
                    enabled = enabled && GlobalState.connection.hasSession && !hasOpenTransaction,
                    tooltip = Tooltip.Args(
                        title = Label.SCHEMA_SESSION,
                        description = Sentence.SESSION_SCHEMA_DESCRIPTION,
                        url = URL.DOCS_SESSION_SCHEMA
                    )
                )
                ToggleButton(
                    text = data.name.lowercase(),
                    onClick = { GlobalState.connection.current?.updateSessionType(data) },
                    isActive = enabled && GlobalState.connection.current?.config?.sessionType == data,
                    enabled = enabled && GlobalState.connection.hasSession && !hasOpenTransaction,
                    tooltip = Tooltip.Args(
                        title = Label.DATA_SESSION,
                        description = Sentence.SESSION_DATA_DESCRIPTION,
                        url = URL.DOCS_SESSION_DATA
                    )
                )
            }
        }

        @Composable
        private fun TransactionTypeButtons(enabled: Boolean) {
            val write = TypeDBTransaction.Type.WRITE
            val read = TypeDBTransaction.Type.READ
            val hasOpenTx = GlobalState.connection.current?.hasOpenTransaction == true
            ToggleButtonRow {
                ToggleButton(
                    text = write.name.lowercase(),
                    onClick = { GlobalState.connection.current?.updateTransactionType(write) },
                    isActive = enabled && GlobalState.connection.current?.config?.transactionType == write,
                    enabled = enabled && GlobalState.connection.hasSession && !hasOpenTx,
                    tooltip = Tooltip.Args(
                        title = Label.WRITE_TRANSACTION,
                        description = Sentence.TRANSACTION_WRITE_DESCRIPTION,
                        url = URL.DOCS_TRANSACTION_WRITE
                    )
                )
                ToggleButton(
                    text = read.name.lowercase(),
                    onClick = { GlobalState.connection.current?.updateTransactionType(read) },
                    isActive = enabled && GlobalState.connection.current?.config?.transactionType == read,
                    enabled = enabled && GlobalState.connection.hasSession && !hasOpenTx,
                    tooltip = Tooltip.Args(
                        title = Label.READ_TRANSACTION,
                        description = Sentence.TRANSACTION_READ_DESCRIPTION,
                        url = URL.DOCS_TRANSACTION_READ
                    )
                )
            }
        }

        @Composable
        private fun OptionsButtons(enabled: Boolean) {
            val txConfig = GlobalState.connection.current?.config
            val hasOpenTx = GlobalState.connection.current?.hasOpenTransaction == true
            ToggleButtonRow {
                ToggleButton(
                    text = Label.SNAPSHOT.lowercase(),
                    onClick = { txConfig?.toggleSnapshot() },
                    isActive = enabled && txConfig?.snapshot ?: false,
                    enabled = enabled && !hasOpenTx && txConfig?.snapshotEnabled ?: false,
                    tooltip = Tooltip.Args(
                        title = Label.ENABLE_SNAPSHOT,
                        description = Sentence.ENABLE_SNAPSHOT_DESCRIPTION,
                        url = URL.DOCS_ENABLE_SNAPSHOT
                    )
                )
                ToggleButton(
                    text = Label.INFER.lowercase(),
                    onClick = { txConfig?.toggleInfer() },
                    isActive = enabled && txConfig?.infer ?: false,
                    enabled = enabled && !hasOpenTx && txConfig?.inferEnabled ?: false,
                    tooltip = Tooltip.Args(
                        title = Label.ENABLE_INFERENCE,
                        description = Sentence.ENABLE_INFERENCE_DESCRIPTION,
                        url = URL.DOCS_ENABLE_INFERENCE
                    )
                )
                ToggleButton(
                    text = Label.EXPLAIN.lowercase(),
                    onClick = { txConfig?.toggleExplain() },
                    isActive = enabled && txConfig?.explain ?: false,
                    enabled = enabled && !hasOpenTx && txConfig?.explainEnabled ?: false,
                    tooltip = Tooltip.Args(
                        title = Label.ENABLE_INFERENCE_EXPLANATION,
                        description = Sentence.ENABLE_INFERENCE_EXPLANATION_DESCRIPTION,
                        url = URL.DOCS_ENABLE_INFERENCE_EXPLANATION,
                    )
                )
            }
        }
    }

    object TransactionControl {

        @Composable
        internal fun Buttons() {
            val isInteractive = GlobalState.connection.current?.isInteractiveMode == true
            ToolbarSpace()
            StatusIndicator(isInteractive)
            ToolbarSpace()
            CloseButton(isInteractive)
            ToolbarSpace()
            RollbackButton(isInteractive)
            ToolbarSpace()
            CommitButton(isInteractive)
            ToolbarSpace()
        }

        @Composable
        private fun StatusIndicator(enabled: Boolean) {
            val hasOpenTx = GlobalState.connection.current?.hasOpenTransaction == true
            RawIconButton(
                icon = Icon.Code.CIRCLE,
                modifier = Modifier.size(TOOLBAR_BUTTON_SIZE),
                iconColor = if (enabled && hasOpenTx) Theme.colors.secondary else Theme.colors.icon,
                enabled = enabled,
                tooltip = Tooltip.Args(
                    title = Label.TRANSACTION_STATUS,
                    description = Sentence.TRANSACTION_STATUS_DESCRIPTION
                )
            )
        }

        @Composable
        private fun CloseButton(enabled: Boolean) {
            val connection = GlobalState.connection.current
            ToolbarIconButton(
                icon = Icon.Code.XMARK,
                onClick = { connection?.closeTransaction() },
                color = Theme.colors.error,
                enabled = enabled && connection?.hasOpenTransaction == true,
                tooltip = Tooltip.Args(
                    title = Label.CLOSE_TRANSACTION,
                    description = Sentence.TRANSACTION_CLOSE_DESCRIPTION,
                    url = URL.DOCS_TRANSACTION_CLOSE,
                )
            )
        }

        @Composable
        private fun RollbackButton(enabled: Boolean) {
            val connection = GlobalState.connection.current
            ToolbarIconButton(
                icon = Icon.Code.ROTATE_LEFT,
                onClick = { connection?.transaction?.rollback() },
                color = Theme.colors.quaternary2,
                enabled = enabled && connection?.hasOpenTransaction == true && connection.isWrite,
                tooltip = Tooltip.Args(
                    title = Label.ROLLBACK_TRANSACTION,
                    description = Sentence.TRANSACTION_ROLLBACK_DESCRIPTION,
                    url = URL.DOCS_TRANSACTION_ROLLBACK,
                )
            )
        }

        @Composable
        private fun CommitButton(enabled: Boolean) {
            val connection = GlobalState.connection.current
            ToolbarIconButton(
                icon = Icon.Code.CHECK,
                onClick = { GlobalState.connection.current?.transaction?.commit() },
                color = Theme.colors.secondary,
                enabled = enabled && connection?.hasOpenTransaction == true && connection.isWrite,
                tooltip = Tooltip.Args(
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
                enabled = GlobalState.connection.hasSession &&
                        GlobalState.resource.active?.isRunnable == true &&
                        GlobalState.connection.current?.hasRunningCommand != true,
                tooltip = Tooltip.Args(
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
                onClick = { GlobalState.connection.current!!.signalStop() },
                enabled = GlobalState.connection.current?.hasRunningCommand == true,
                tooltip = Tooltip.Args(title = Label.STOP_SIGNAL, description = Sentence.STOP_SIGNAL_DESCRIPTION)
            )
        }
    }

    object DBConnection {

        @Composable
        internal fun Buttons() {
            ToolbarSpace()
            ModeButtons()
            ToolbarSpace()
            ConnectionButton()
            ToolbarSpace()
        }

        @Composable
        private fun ConnectionButton() {
            when (GlobalState.connection.status) {
                DISCONNECTED -> ConnectionButton(Label.CONNECT_TO_TYPEDB)
                CONNECTING -> ConnectionButton(Label.CONNECTING)
                CONNECTED -> ConnectionButton(
                    (GlobalState.connection.current!!.username?.let { "$it@" } ?: "") +
                            GlobalState.connection.current!!.address
                )
            }
        }

        @Composable
        private fun ModeButtons() {
            val interactive = Connection.Mode.INTERACTIVE
            val script = Connection.Mode.SCRIPT
            ToggleButtonRow {
                ToggleButton(
                    text = interactive.name.lowercase(),
                    onClick = { GlobalState.connection.current?.mode = interactive },
                    isActive = GlobalState.connection.isInteractiveMode,
                    enabled = GlobalState.connection.isConnected,
                    tooltip = Tooltip.Args(
                        title = Label.INTERACTIVE_MODE,
                        description = Sentence.INTERACTIVE_MODE_DESCRIPTION,
                        url = URL.DOCS_MODE_INTERACTIVE,
                    )
                )
                ToggleButton(
                    text = script.name.lowercase(),
                    onClick = { GlobalState.connection.current?.mode = script },
                    isActive = GlobalState.connection.isScriptMode,
                    enabled = GlobalState.connection.isConnected,
                    tooltip = Tooltip.Args(
                        title = Label.SCRIPT_MODE,
                        description = Sentence.SCRIPT_MODE_DESCRIPTION,
                        url = URL.DOCS_MODE_SCRIPT,
                    )
                )
            }
        }

        @Composable
        private fun ConnectionButton(text: String) {
            TextButton(
                text = text,
                onClick = { GlobalState.connection.connectServerDialog.open() },
                modifier = Modifier.height(TOOLBAR_BUTTON_SIZE),
                trailingIcon = Icon.Code.DATABASE,
            )
        }
    }
}
