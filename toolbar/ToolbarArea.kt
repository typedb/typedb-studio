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

package com.vaticle.typedb.studio.toolbar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.common.Label
import com.vaticle.typedb.studio.common.component.Form
import com.vaticle.typedb.studio.common.component.Icon
import com.vaticle.typedb.studio.common.theme.Theme
import com.vaticle.typedb.studio.service.ConnectionService.Status.CONNECTED
import com.vaticle.typedb.studio.service.ConnectionService.Status.CONNECTING
import com.vaticle.typedb.studio.service.ConnectionService.Status.DISCONNECTED
import com.vaticle.typedb.studio.service.Service

object ToolbarArea {

    private val TOOLBAR_HEIGHT = 32.dp
    private val TOOLBAR_SPACING = 4.dp
    private val COMPONENT_HEIGHT = 24.dp
    private val DATABASE_DROPDOWN_WIDTH = 120.dp

    @Composable
    fun Layout() {
        Row(
            modifier = Modifier.fillMaxWidth().height(TOOLBAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarSpace()
            ToolbarButton(icon = Icon.Code.FOLDER_OPEN, onClick = { Service.project.toggleWindow() })
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
        Form.IconButton(icon = icon, onClick = onClick, color = color, modifier = Modifier.size(COMPONENT_HEIGHT))
    }

    @Composable
    private fun DatabaseDropdown() {
        Form.Dropdown(
            values = Service.connection.databaseList,
            selected = Service.connection.getDatabase() ?: "",
            onSelection = { Service.connection.setDatabase(it) },
            placeholder = Label.SELECT_DATABASE,
            enabled = Service.connection.isConnected(),
            modifier = Modifier.height(COMPONENT_HEIGHT).width(width = DATABASE_DROPDOWN_WIDTH)
                .onFocusChanged { if (it.isFocused) Service.connection.refreshDatabaseList() }
        )
    }

    @Composable
    private fun ConnectionButton() {
        when (Service.connection.status) {
            DISCONNECTED -> ConnectionButton(Label.CONNECT_TO_TYPEDB)
            CONNECTING -> ConnectionButton(Label.CONNECTING)
            CONNECTED -> ConnectionButton(
                (Service.connection.username?.let { "$it@" } ?: "") + Service.connection.address!!
            )
        }
    }

    @Composable
    private fun ConnectionButton(text: String) {
        Form.TextButton(
            text = text,
            modifier = Modifier.height(COMPONENT_HEIGHT),
            onClick = { Service.connection.showWindow = true },
            trailingIcon = Icon.Code.DATABASE,
        )
    }
}
