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
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.CONNECTED
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.CONNECTING
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.DISCONNECTED
import com.vaticle.typedb.studio.view.common.Label
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
    private val DATABASE_DROPDOWN_WIDTH = 120.dp

    @Composable
    fun Layout() {
        Row(
            modifier = Modifier.fillMaxWidth().height(TOOLBAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Project.Buttons()
            Separator.Vertical()
            Query.Buttons()
            Separator.Vertical()
            Spacer(Modifier.weight(1f))
            Separator.Vertical()
            Connection.Buttons()
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

    object Query {

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

    object Connection {

        @Composable
        internal fun Buttons() {
            ToolbarSpace()
            DatabaseDropdown(Modifier.height(BUTTON_HEIGHT).width(DATABASE_DROPDOWN_WIDTH))
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
                    (GlobalState.connection.current!!.username?.let { "$it@" }
                        ?: "") + GlobalState.connection.current!!.address
                )
            }
        }

        @Composable
        private fun ConnectionButton(text: String) {
            TextButton(
                text = text,
                modifier = Modifier.height(BUTTON_HEIGHT),
                onClick = { GlobalState.connection.connectServerDialog.open() },
                trailingIcon = Icon.Code.DATABASE,
            )
        }
    }
}
