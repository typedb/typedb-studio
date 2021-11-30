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
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.CONNECTED
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.CONNECTING
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.DISCONNECTED
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form.Dropdown
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Theme

object Toolbar {

    private val TOOLBAR_HEIGHT = 32.dp
    private val TOOLBAR_SPACING = 4.dp
    private val COMPONENT_HEIGHT = 24.dp
    private val DATABASE_DROPDOWN_WIDTH = 120.dp

    @Composable
    fun Area() {
        Row(
            modifier = Modifier.fillMaxWidth().height(TOOLBAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarSpace()
            ToolbarButton(icon = Icon.Code.FOLDER_OPEN, onClick = { State.project.toggleWindow() })
            ToolbarSpace()
            ToolbarButton(icon = Icon.Code.FLOPPY_DISK, onClick = {})
            ToolbarSpace()
            ToolbarButton(icon = Icon.Code.PLAY, color = Theme.colors.secondary, onClick = {})
            ToolbarSpace()
            ToolbarButton(icon = Icon.Code.STOP, color = Theme.colors.error, onClick = {})
            Spacer(Modifier.weight(1f))
            DatabaseDropdown()
            ToolbarSpace()
            ConnectionButton()
            ToolbarSpace()
        }
    }

    @Composable
    private fun ToolbarSpace() {
        Spacer(Modifier.width(TOOLBAR_SPACING))
    }

    @Composable
    private fun ToolbarButton(icon: Icon.Code, onClick: () -> Unit, color: Color = Theme.colors.icon) {
        IconButton(icon = icon, onClick = onClick, iconColor = color, modifier = Modifier.size(COMPONENT_HEIGHT))
    }

    @Composable
    private fun DatabaseDropdown() {
        Dropdown(
            values = State.connection.current?.databaseList ?: emptyList(),
            selected = State.connection.current?.getDatabase() ?: "",
            onExpand = { State.connection.current?.refreshDatabaseList() },
            onSelection = { State.connection.current?.setDatabase(it) },
            placeholder = Label.SELECT_DATABASE,
            enabled = State.connection.isConnected(),
            modifier = Modifier.height(COMPONENT_HEIGHT).width(width = DATABASE_DROPDOWN_WIDTH)
        )
    }

    @Composable
    private fun ConnectionButton() {
        when (State.connection.status) {
            DISCONNECTED -> ConnectionButton(Label.CONNECT_TO_TYPEDB)
            CONNECTING -> ConnectionButton(Label.CONNECTING)
            CONNECTED -> ConnectionButton(
                (State.connection.current!!.username?.let { "$it@" } ?: "") + State.connection.current!!.address
            )
        }
    }

    @Composable
    private fun ConnectionButton(text: String) {
        TextButton(
            text = text,
            modifier = Modifier.height(COMPONENT_HEIGHT),
            onClick = { State.connection.showWindow = true },
            trailingIcon = Icon.Code.DATABASE,
        )
    }
}
