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

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.component.ActionList
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.ButtonArg
import com.vaticle.typedb.studio.view.common.component.Form.Dropdown
import com.vaticle.typedb.studio.view.common.component.Form.Field
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Tooltip
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.dialog.Dialog.DIALOG_SPACING

object DatabaseDialog {

    private val MANAGER_WIDTH = 400.dp
    private val MANAGER_HEIGHT = 400.dp
    private val SELECTOR_WIDTH = 400.dp
    private val SELECTOR_HEIGHT = 200.dp

    @Composable
    fun ManageDatabases() {
        val dialogState = GlobalState.connection.manageDatabasesDialog
        val focusReq = FocusRequester()
        Dialog.Layout(dialogState, Label.MANAGE_DATABASES, MANAGER_WIDTH, MANAGER_HEIGHT, focusReq) {
            Column(Modifier.fillMaxSize()) {
                Form.Text(value = Sentence.MANAGE_DATABASES_MESSAGE, softWrap = true)
                Spacer(Modifier.height(DIALOG_SPACING))
                ActionList.Layout(
                    items = GlobalState.connection.current!!.databaseList,
                    settingSide = ActionList.Side.RIGHT,
                    modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, Theme.colors.border),
                    buttonFn = {
                        ButtonArg(
                            icon = Icon.Code.TRASH_CAN,
                            onClick = {
                                GlobalState.confirmation.submit(
                                    title = Label.DELETE_DATABASE,
                                    message = Sentence.CONFIRM_DATABASE_DELETION.format(it),
                                    onConfirm = { GlobalState.connection.current!!.deleteDatabase(it) }
                                )
                            }
                        )
                    }
                )
                Spacer(Modifier.height(DIALOG_SPACING).focusRequester(focusReq).focusable()) // TODO: temporary focus
            }
        }
    }

    @Composable
    fun SelectDatabase() {
        val dialogState = GlobalState.connection.selectDatabaseDialog
        val focusReq = FocusRequester()
        Dialog.Layout(dialogState, Label.SELECT_DATABASE, SELECTOR_WIDTH, SELECTOR_HEIGHT, focusReq) {
            Column(Modifier.fillMaxSize()) {
                Field(label = Label.SELECT_DATABASE) {
                    DatabaseDropdown(Modifier.fillMaxWidth().focusRequester(focusReq))
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(text = Label.CLOSE, onClick = { dialogState.close() })
                }
            }
        }
    }

    @Composable
    fun DatabaseDropdown(modifier: Modifier = Modifier) {
        Dropdown(
            values = GlobalState.connection.current?.databaseList ?: emptyList(),
            selected = GlobalState.connection.current?.database,
            onExpand = { GlobalState.connection.current?.refreshDatabaseList() },
            onSelection = { GlobalState.connection.current?.openSession(it) },
            placeholder = Label.SELECT_DATABASE,
            enabled = GlobalState.connection.isInteractiveMode,
            modifier = modifier,
            tooltip = Tooltip.Arg(
                title = Label.SELECT_DATABASE,
                description = Sentence.SELECT_DATABASE_DESCRIPTION
            )
        )
    }
}
