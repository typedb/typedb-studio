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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.common.Label
import com.vaticle.typedb.studio.common.Property.displayableOf
import com.vaticle.typedb.studio.common.component.Form.Button
import com.vaticle.typedb.studio.common.component.Form.Dropdown
import com.vaticle.typedb.studio.common.component.Icon
import com.vaticle.typedb.studio.common.theme.Theme
import com.vaticle.typedb.studio.common.util.IconUtil
import com.vaticle.typedb.studio.service.ConnectionService
import com.vaticle.typedb.studio.service.ConnectionService.Status.CONNECTED
import com.vaticle.typedb.studio.service.ConnectionService.Status.CONNECTING
import com.vaticle.typedb.studio.service.ConnectionService.Status.DISCONNECTED
import com.vaticle.typedb.studio.service.Service

object ToolbarArea {

    private val TOOLBAR_HEIGHT = 30.dp
    private val TOOLBAR_COMPONENT_HEIGHT = 25.dp
    private val DATABASE_DROPDOWN_WIDTH = 120.dp

    @Composable
    fun Layout() {
        Row(
            modifier = Modifier.fillMaxWidth().height(TOOLBAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OpenFileButton()
            SaveFileButton()
            PlayFileButton()
            Spacer(Modifier.weight(1f))
            DatabaseDropdown()
            Spacer(Modifier.width(4.dp))
            when (Service.connection.status) {
                DISCONNECTED -> ConnectionButton()
                CONNECTING -> ConnectionButton() // TODO: replace with connecting status
                CONNECTED -> ConnectionStatus()
            }
            DatabaseIcon()
        }
    }

    @Composable
    private fun OpenFileButton() {
        Spacer(Modifier.width(10.dp))
        Icon.Render(
            icon = Icon.Set.FolderOpen,
            size = Icon.Size.Size14,
            modifier = Modifier.clickable { }
        )
    }

    @Composable
    private fun SaveFileButton() {
        Spacer(Modifier.width(12.dp))
        Icon.Render(
            icon = Icon.Set.FloppyDisk,
            size = Icon.Size.Size12,
            modifier = Modifier.clickable { }
        )
    }

    @Composable
    private fun PlayFileButton() {
        Spacer(Modifier.width(8.dp))
        Icon.Render(
            icon = Icon.Set.Play,
            size = Icon.Size.Size16,
            color = Theme.colors.secondary,
            modifier = Modifier.clickable { }
        )
    }

    @Composable
    private fun DatabaseDropdown() {
        Dropdown(
            values = Service.connection.databaseList.map { displayableOf(it) },
            selected = Service.connection.getDatabase()?.let { displayableOf(it) } ?: displayableOf(""),
            placeholder = Label.SELECT_DATABASE,
            onSelection = { Service.connection.setDatabase(it.displayName) },
            modifier = Modifier.height(TOOLBAR_COMPONENT_HEIGHT).width(width = DATABASE_DROPDOWN_WIDTH),
            textInputModifier = Modifier.onFocusEvent { Service.connection.refreshDatabaseList() },
            enabled = Service.connection.isConnected()
        )
    }

    @Composable
    private fun ConnectionButton() {
        Button(
            text = Label.CONNECT_TO_TYPEDB,
            modifier = Modifier.height(TOOLBAR_COMPONENT_HEIGHT),
            onClick = { Service.connection.showWindow = true }
        )
    }

    @Composable
    private fun ConnectionStatus() {
        Button(
            text = (Service.connection.username?.let { "$it@" } ?: "" ) + Service.connection.address!!,
            modifier = Modifier.height(TOOLBAR_COMPONENT_HEIGHT),
            onClick = { Service.connection.showWindow = true }
        )
    }


    @Composable
    private fun DatabaseIcon() {
        Spacer(Modifier.width(8.dp))
        IconUtil.DatabaseIcon() // TODO: do we really need this? should we perhaps find a better icon?
        Spacer(Modifier.width(8.dp))
    }
}
