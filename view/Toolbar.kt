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
import androidx.compose.ui.unit.dp
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
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.component.Tooltip
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.dialog.DatabaseDialog.DatabaseDropdown

object Toolbar {

    private val TOOLBAR_HEIGHT = 34.dp
    private val TOOLBAR_SPACING = 5.dp
    private val BUTTON_HEIGHT = 24.dp
    private val SEPARATOR_HEIGHT = 20.dp

    @Composable
    fun Layout() {
        Row(
            modifier = Modifier.fillMaxWidth().height(TOOLBAR_HEIGHT),
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
        Separator.Vertical(modifier = Modifier.height(SEPARATOR_HEIGHT))
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
            modifier = Modifier.size(BUTTON_HEIGHT),
            iconColor = if (enabled) color else Theme.colors.icon,
            enabled = enabled,
            tooltip = tooltip
        )
    }

    @Composable
    private fun ToggleButtonRow(content: @Composable RowScope.() -> Unit) {
        Row(Modifier.height(BUTTON_HEIGHT).background(Theme.colors.primary, Theme.ROUNDED_RECTANGLE)) { content() }
    }

    @Composable
    private fun ToggleButton(
        text: String,
        onClick: () -> Unit,
        isActive: Boolean,
        enabled: Boolean,
        tooltip: Tooltip.Args? = null
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
                onClick = { GlobalState.project.openProjectDialog.toggle() })
        }

        @Composable
        private fun SaveButton() {
            ToolbarIconButton(
                icon = Icon.Code.FLOPPY_DISK,
                onClick = { GlobalState.page.saveAndReopen(GlobalState.page.selectedPage!!) },
                enabled = GlobalState.page.selectedPage?.isUnsaved == true
            )
        }
    }

    object InteractionSettings {

        @Composable
        internal fun Buttons() {
            val isInteractive = GlobalState.connection.current?.isInteractiveMode ?: false
            ToolbarSpace()
            DatabaseDropdown(Modifier.height(BUTTON_HEIGHT))
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
            ToggleButtonRow {
                ToggleButton(
                    text = schema.name.lowercase(),
                    onClick = { GlobalState.connection.current?.updateSessionType(schema) },
                    isActive = enabled && GlobalState.connection.current?.config?.sessionType == schema,
                    enabled = enabled && GlobalState.connection.hasSession
                )
                ToggleButton(
                    text = data.name.lowercase(),
                    onClick = { GlobalState.connection.current?.updateSessionType(data) },
                    isActive = enabled && GlobalState.connection.current?.config?.sessionType == data,
                    enabled = enabled && GlobalState.connection.hasSession
                )
            }
        }

        @Composable
        private fun TransactionTypeButtons(enabled: Boolean) {
            val write = TypeDBTransaction.Type.WRITE
            val read = TypeDBTransaction.Type.READ
            ToggleButtonRow {
                ToggleButton(
                    text = write.name.lowercase(),
                    onClick = { GlobalState.connection.current?.updateTransactionType(write) },
                    isActive = enabled && GlobalState.connection.current?.config?.transactionType == write,
                    enabled = enabled && GlobalState.connection.hasSession,
                    tooltip = Tooltip.Args(
                        title = Label.WRITE_TRANSACTION,
                        description = Sentence.TRANSACTION_WRITE_DESCRIPTION,
                        url = URL.DOCS_WRITE_TRANSACTION
                    )
                )
                ToggleButton(
                    text = read.name.lowercase(),
                    onClick = { GlobalState.connection.current?.updateTransactionType(read) },
                    isActive = enabled && GlobalState.connection.current?.config?.transactionType == read,
                    enabled = enabled && GlobalState.connection.hasSession,
                    tooltip = Tooltip.Args(
                        title = Label.READ_TRANSACTION,
                        description = Sentence.TRANSACTION_READ_DESCRIPTION,
                        url = URL.DOCS_READ_TRANSACTION
                    )
                )
            }
        }

        @Composable
        private fun OptionsButtons(enabled: Boolean) {
            ToggleButtonRow {
                ToggleButton(
                    text = Label.SNAPSHOT.lowercase(),
                    onClick = { GlobalState.connection.current?.config?.toggleSnapshot() },
                    isActive = enabled && GlobalState.connection.current?.config?.snapshot ?: false,
                    enabled = enabled && GlobalState.connection.current?.config?.snapshotEnabled ?: false,
                    tooltip = Tooltip.Args(
                        title = Label.ENABLE_SNAPSHOT,
                        description = Sentence.ENABLE_SNAPSHOT_DESCRIPTION,
                        url = URL.DOCS_SNAPSHOT
                    )
                )
                ToggleButton(
                    text = Label.INFER.lowercase(),
                    onClick = { GlobalState.connection.current?.config?.toggleInfer() },
                    isActive = enabled && GlobalState.connection.current?.config?.infer ?: false,
                    enabled = enabled && GlobalState.connection.current?.config?.inferEnabled ?: false,
                    tooltip = Tooltip.Args(
                        title = Label.ENABLE_INFERENCE,
                        description = Sentence.ENABLE_INFERENCE_DESCRIPTION,
                        url = URL.DOCS_INFERENCE
                    )
                )
                ToggleButton(
                    text = Label.EXPLAIN.lowercase(),
                    onClick = { GlobalState.connection.current?.config?.toggleExplain() },
                    isActive = enabled && GlobalState.connection.current?.config?.explain ?: false,
                    enabled = enabled && GlobalState.connection.current?.config?.explainEnabled ?: false,
                    tooltip = Tooltip.Args(
                        title = Label.ENABLE_INFERENCE_EXPLANATION,
                        description = Sentence.ENABLE_INFERENCE_EXPLANATION_DESCRIPTION,
                        url = URL.DOCS_INFERENCE_EXPLANATION,
                    )
                )
            }
        }
    }

    object TransactionControl {

        @Composable
        internal fun Buttons() {
            val isQueryMode = GlobalState.connection.current?.isInteractiveMode ?: false
            ToolbarSpace()
            ReopenButton(isQueryMode)
            ToolbarSpace()
            RollbackButton(isQueryMode)
            ToolbarSpace()
            CommitButton(isQueryMode)
            ToolbarSpace()
        }

        @Composable
        private fun ReopenButton(enabled: Boolean) {
            val isSnapshot = GlobalState.connection.current?.config?.snapshot ?: false
            val hasTransaction = GlobalState.connection.current?.hasSession() ?: false
            // TODO: val hasTransaction = GlobalState.connection.current?.hasTransaction() ?: false
            ToolbarIconButton(
                icon = Icon.Code.ROTATE,
                onClick = {},
                color = Theme.colors.quinary,
                enabled = enabled && isSnapshot && hasTransaction,
                tooltip = Tooltip.Args(
                    title = Label.REOPEN_TRANSACTION,
                    description = Sentence.REOPEN_TRANSACTION_DESCRIPTION,
                    url = URL.DOCS_REOPEN_TRANSACTION,
                )
            )
        }

        @Composable
        private fun RollbackButton(enabled: Boolean) {
            ToolbarIconButton(
                icon = Icon.Code.ROTATE_LEFT,
                onClick = {},
                color = Theme.colors.quaternary2,
                enabled = enabled && GlobalState.connection.current?.hasWrites ?: false,
                tooltip = Tooltip.Args(
                    title = Label.ROLLBACK_TRANSACTION,
                    description = Sentence.ROLLBACK_TRANSACTION_DESCRIPTION,
                    url = URL.DOCS_ROLLBACK_TRANSACTION,
                )
            )
        }

        @Composable
        private fun CommitButton(enabled: Boolean) {
            ToolbarIconButton(
                icon = Icon.Code.CHECK,
                onClick = {},
                color = Theme.colors.secondary,
                enabled = enabled && GlobalState.connection.current?.hasWrites ?: false,
                tooltip = Tooltip.Args(
                    title = Label.COMMIT_TRANSACTION,
                    description = Sentence.COMMIT_TRANSACTION_DESCRIPTION,
                    url = URL.DOCS_COMMIT_TRANSACTION
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
                onClick = {}, // TODO
                enabled = GlobalState.page.selectedPage?.isRunnable == true,
                tooltip = Tooltip.Args(
                    title = if (GlobalState.connection.isScriptMode) Label.RUN_SCRIPT else Label.RUN_QUERY,
                    description = Sentence.BUTTON_ENABLED_WHEN_RUNNABLE_PAGE
                )
            )
        }

        @Composable
        private fun StopButton() {
            ToolbarIconButton(
                icon = Icon.Code.STOP,
                color = Theme.colors.error,
                onClick = {},
                enabled = GlobalState.page.selectedPage?.isRunnable == true, // TODO: replace with isRunning() when ready
                tooltip = Tooltip.Args(
                    title = Label.STOP,
                    description = Sentence.BUTTON_ENABLED_WHEN_RUNNABLE_PAGE // TODO: replace once isRunning() is ready
                )
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
                        url = URL.DOCS_INTERACTIVE_MODE,
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
                        url = URL.DOCS_SCRIPT_MODE,
                    )
                )
            }
        }

        @Composable
        private fun ConnectionButton(text: String) {
            TextButton(
                text = text,
                onClick = { GlobalState.connection.connectServerDialog.open() },
                modifier = Modifier.height(BUTTON_HEIGHT),
                trailingIcon = Icon.Code.DATABASE,
            )
        }
    }
}
