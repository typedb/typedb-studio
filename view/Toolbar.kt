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
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.CONNECTED
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.CONNECTING
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.DISCONNECTED
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form.Dropdown
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.dialog.ConnectionDialog.DatabaseDropdown

object Toolbar {

    private val TOOLBAR_HEIGHT = 34.dp
    private val TOOLBAR_SPACING = 5.dp
    private val BUTTON_HEIGHT = 24.dp

    @Composable
    fun Layout() {
        Row(
            modifier = Modifier.fillMaxWidth().height(TOOLBAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Project.Buttons()
            Separator.Vertical()
            TxConfig.Buttons()
            Separator.Vertical()
            QueryRun.Buttons()
            Separator.Vertical()
            TransactionControl.Buttons()
            Separator.Vertical()
            Spacer(Modifier.weight(1f))
            Separator.Vertical()
            DBConnection.Buttons()
        }
    }

    @Composable
    private fun ToolbarSpace() {
        Spacer(Modifier.width(TOOLBAR_SPACING))
    }

    @Composable
    private fun ToolbarButton(
        icon: Icon.Code, onClick: () -> Unit, color: Color = Theme.colors.icon, enabled: Boolean = true
    ) {
        IconButton(
            icon = icon,
            onClick = onClick,
            modifier = Modifier.size(BUTTON_HEIGHT),
            iconColor = color,
            enabled = enabled
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
            ToolbarButton(icon = Icon.Code.FOLDER_OPEN, onClick = { GlobalState.project.openProjectDialog.toggle() })
        }

        @Composable
        private fun SaveButton() {
            ToolbarButton(
                icon = Icon.Code.FLOPPY_DISK,
                onClick = { GlobalState.page.saveAndReopen(GlobalState.page.selectedPage!!) },
                enabled = GlobalState.page.selectedPage?.isUnsaved == true
            )
        }
    }

    object TxConfig {

        @Composable
        internal fun Buttons() {
            ToolbarSpace()
            SessionTypeButton()
            ToolbarSpace()
            TransactionTypeButton()
            ToolbarSpace()
            ConfigToggleButtons()
            ToolbarSpace()
        }

        @Composable
        private fun SessionTypeButton() {
            Dropdown(
                values = TypeDBSession.Type.values().asList(),
                selected = GlobalState.connection.current?.session?.type(),
                displayFn = { it.name.lowercase() },
                onSelection = { GlobalState.connection.current?.updateSessionType(it) },
                placeholder = Label.SESSION_TYPE,
                enabled = GlobalState.connection.hasSession(),
                modifier = Modifier.height(BUTTON_HEIGHT),
            )
        }

        @Composable
        private fun TransactionTypeButton() {
            Dropdown(
                values = TypeDBTransaction.Type.values().asList(),
                selected = GlobalState.connection.current?.config?.transactionType,
                displayFn = { it.name.lowercase() },
                onSelection = { GlobalState.connection.current?.updateTransactionType(it) },
                placeholder = Label.TRANSACTION_TYPE,
                enabled = GlobalState.connection.hasSession(),
                modifier = Modifier.height(BUTTON_HEIGHT),
            )
        }

        @Composable
        private fun ConfigToggleButtons() {
            Row(Modifier.height(BUTTON_HEIGHT).background(Theme.colors.primary, Theme.ROUNDED_RECTANGLE)) {
                ToggleButton(
                    text = Label.KEEP_ALIVE,
                    onClick = { GlobalState.connection.current?.config?.toggleKeepAlive() },
                    isActive = GlobalState.connection.current?.config?.keepAlive ?: false,
                    enabled = GlobalState.connection.current?.config?.keepAliveEnabled ?: false
                )
                ToggleButton(
                    text = Label.INFER,
                    onClick = { GlobalState.connection.current?.config?.toggleInfer() },
                    isActive = GlobalState.connection.current?.config?.infer ?: false,
                    enabled = GlobalState.connection.current?.config?.inferEnabled ?: false
                )
                ToggleButton(
                    text = Label.EXPLAIN,
                    onClick = { GlobalState.connection.current?.config?.toggleExplain() },
                    isActive = GlobalState.connection.current?.config?.explain ?: false,
                    enabled = GlobalState.connection.current?.config?.explainEnabled ?: false
                )
            }
        }

        @Composable
        private fun ToggleButton(text: String, onClick: () -> Unit, isActive: Boolean, enabled: Boolean) {
            TextButton(
                text = text,
                onClick = onClick,
                textColor = if (isActive) Theme.colors.secondary else Theme.colors.onPrimary,
                enabled = enabled
            )
        }
    }

    object QueryRun {

        @Composable
        internal fun Buttons() {
            ToolbarSpace()
            PlayButton()
            ToolbarSpace()
            StopButton()
            ToolbarSpace()
        }

        @Composable
        private fun StopButton() {
            ToolbarButton(icon = Icon.Code.STOP, color = Theme.colors.error, onClick = {})
        }

        @Composable
        private fun PlayButton() {
            ToolbarButton(icon = Icon.Code.PLAY, color = Theme.colors.secondary, onClick = {})
        }
    }

    object TransactionControl {

        @Composable
        internal fun Buttons() {
            ToolbarSpace()
            ReopenButton()
            ToolbarSpace()
            CommitButton()
            ToolbarSpace()
            RollbackButton()
            ToolbarSpace()
        }

        @Composable
        private fun ReopenButton() {
            ToolbarButton(
                icon = Icon.Code.ROTATE,
                onClick = {},
                enabled = GlobalState.connection.current?.config?.keepAlive ?: false
            )
        }

        @Composable
        private fun CommitButton() {
            ToolbarButton(
                icon = Icon.Code.CODE_COMMIT,
                onClick = {},
                enabled = GlobalState.connection.current?.hasWrites ?: false
            )
        }

        @Composable
        private fun RollbackButton() {
            ToolbarButton(
                icon = Icon.Code.CLOCK_ROTATE_LEFT,
                onClick = {},
                enabled = GlobalState.connection.current?.hasWrites ?: false
            )
        }
    }


    object DBConnection {

        @Composable
        internal fun Buttons() {
            ToolbarSpace()
            DatabaseDropdown(Modifier.height(BUTTON_HEIGHT))
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
