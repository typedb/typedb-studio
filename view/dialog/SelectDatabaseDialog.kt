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

package com.vaticle.typedb.studio.view.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form

object SelectDatabaseDialog {

    private val WINDOW_WIDTH = 400.dp
    private val WINDOW_HEIGHT = 200.dp

    class DialogState {
        var showDialog by mutableStateOf(false)
    }

    @Composable
    fun rememberState(): DialogState {
        return remember { DialogState() }
    }

    @Composable
    fun Layout(dialogState: DialogState) {
        Dialog(
            title = Label.SELECT_DATABASE,
            onCloseRequest = { dialogState.showDialog = false },
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(WINDOW_WIDTH, WINDOW_HEIGHT)
            )
        ) {
            Form.Submission {
                Form.Field(label = Label.SELECT_DATABASE) { DatabaseDropdown() }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    Form.TextButton(text = Label.CLOSE, onClick = { dialogState.showDialog = false })
                }
            }
        }
    }

    @Composable
    fun DatabaseDropdown(modifier: Modifier = Modifier) {
        Form.Dropdown(
            values = State.connection.current?.databaseList ?: emptyList(),
            selected = State.connection.current?.getDatabase() ?: "",
            onExpand = { State.connection.current?.refreshDatabaseList() },
            onSelection = { State.connection.current?.setDatabase(it) },
            placeholder = Label.SELECT_DATABASE,
            enabled = State.connection.isConnected(),
            modifier = modifier
        )
    }
}