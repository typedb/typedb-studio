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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.common.Label
import com.vaticle.typedb.studio.common.Property.displayableOf
import com.vaticle.typedb.studio.common.component.Form
import com.vaticle.typedb.studio.common.component.Form.Dropdown
import com.vaticle.typedb.studio.service.Service

object ToolbarArea {

    private val HEIGHT = 28.dp

    @Composable
    fun Layout() {
        Row(Modifier.fillMaxWidth().height(HEIGHT)) {
            Row {

            }
            Spacer(Modifier.weight(1f))
            Row {
                DatabaseDropdown()
                if (Service.connection.isDisconnected()) {
                    ConnectionButton()
                } else {
                    ConnectionStatus()
                }
            }
        }
    }

    @Composable
    private fun DatabaseDropdown() {
        Dropdown(
            values = Service.connection.databases.map { displayableOf(it) },
            selected = Service.connection.getDatabase()?.let { displayableOf(it) } ?: displayableOf(""),
            placeholder = Label.SELECT_DATABASE,
            onSelection = { Service.connection.setDatabase(it.displayName) },
            modifier = Modifier.width(120.dp),
            textInputModifier = Modifier.onFocusEvent { Service.connection.refreshDatabases() }
        )
    }

    @Composable
    private fun ConnectionButton() {
        Form.Button(
            text = Label.CONNECT_TO_TYPEDB,
            onClick = { Service.connection.openDialog = true }
        )
    }


    @Composable
    private fun ConnectionStatus() {
        TODO("Not yet implemented")
    }
}
