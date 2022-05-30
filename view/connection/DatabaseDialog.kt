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

package com.vaticle.typedb.studio.view.connection

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.material.ActionableList
import com.vaticle.typedb.studio.view.material.Dialog
import com.vaticle.typedb.studio.view.material.Dialog.DIALOG_SPACING
import com.vaticle.typedb.studio.view.material.Form
import com.vaticle.typedb.studio.view.material.Form.Dropdown
import com.vaticle.typedb.studio.view.material.Form.FIELD_HEIGHT
import com.vaticle.typedb.studio.view.material.Form.Field
import com.vaticle.typedb.studio.view.material.Form.FormRowSpacer
import com.vaticle.typedb.studio.view.material.Form.IconButtonArg
import com.vaticle.typedb.studio.view.material.Form.Submission
import com.vaticle.typedb.studio.view.material.Form.TextButton
import com.vaticle.typedb.studio.view.material.Form.TextInput
import com.vaticle.typedb.studio.view.material.Icon
import com.vaticle.typedb.studio.view.material.Tooltip

object DatabaseDialog {

    private val MANAGER_WIDTH = 400.dp
    private val MANAGER_HEIGHT = 500.dp
    private val SELECTOR_WIDTH = 400.dp
    private val SELECTOR_HEIGHT = 200.dp

    private object CreateDatabaseForm : Form.State {

        var name: String by mutableStateOf("")

        override fun cancel() {
            GlobalState.client.manageDatabasesDialog.close()
        }

        override fun isValid(): Boolean {
            return name.isNotBlank()
        }

        override fun trySubmit() {
            assert(name.isNotBlank())
            GlobalState.client.tryCreateDatabase(name) { name = "" }
        }
    }

    @Composable
    fun ManageDatabases() {
        val dialogState = GlobalState.client.manageDatabasesDialog
        Dialog.Layout(dialogState, Label.MANAGE_DATABASES, MANAGER_WIDTH, MANAGER_HEIGHT) {
            Column(Modifier.fillMaxSize()) {
                Form.Text(value = Sentence.MANAGE_DATABASES_MESSAGE, softWrap = true)
                Spacer(Modifier.height(DIALOG_SPACING))
                DeletableDatabaseList(Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(DIALOG_SPACING))
                CreateDatabaseForm()
                Spacer(Modifier.height(DIALOG_SPACING * 2))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        text = Label.REFRESH,
                        leadingIcon = Form.IconArg(Icon.Code.ROTATE)
                    ) { GlobalState.client.refreshDatabaseList() }
                    FormRowSpacer()
                    TextButton(text = Label.CLOSE) { dialogState.close() }
                }
            }
        }
    }

    @Composable
    private fun DeletableDatabaseList(modifier: Modifier) {
        ActionableList.Layout(
            items = GlobalState.client.databaseList,
            modifier = modifier.border(1.dp, Theme.studio.border),
            buttonSide = ActionableList.Side.RIGHT,
            buttonFn = { databaseName ->
                IconButtonArg(
                    icon = Icon.Code.TRASH_CAN,
                    color = { Theme.studio.errorStroke },
                    onClick = {
                        GlobalState.confirmation.submit(
                            title = Label.DELETE_DATABASE,
                            message = Sentence.CONFIRM_DATABASE_DELETION.format(databaseName),
                            verificationValue = databaseName,
                            confirmLabel = Label.DELETE,
                            onConfirm = { GlobalState.client.tryDeleteDatabase(databaseName) }
                        )
                    }
                )
            }
        )
    }

    @Composable
    private fun CreateDatabaseForm() {
        val focusReq = remember { FocusRequester() }
        Submission(CreateDatabaseForm, modifier = Modifier.height(FIELD_HEIGHT), showButtons = false) {
            Row {
                TextInput(
                    value = CreateDatabaseForm.name,
                    placeholder = Label.DATABASE_NAME,
                    onValueChange = { CreateDatabaseForm.name = it },
                    modifier = Modifier.weight(1f).focusRequester(focusReq),
                )
                FormRowSpacer()
                TextButton(
                    text = Label.CREATE,
                    enabled = CreateDatabaseForm.isValid(),
                    tooltip = Tooltip.Arg(
                        title = Label.CREATE_DATABASE,
                        description = Sentence.CREATE_DATABASE_BUTTON_DESCRIPTION
                    )
                ) { CreateDatabaseForm.trySubmit() }
            }
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    fun SelectDatabase() {
        val dialogState = GlobalState.client.selectDBDialog
        val focusReq = remember { FocusRequester() }
        Dialog.Layout(dialogState, Label.SELECT_DATABASE, SELECTOR_WIDTH, SELECTOR_HEIGHT) {
            Column(Modifier.fillMaxSize()) {
                Field(label = Label.SELECT_DATABASE) { DatabaseDropdown(Modifier.fillMaxWidth(), focusReq) }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(text = Label.CLOSE) { dialogState.close() }
                }
            }
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    fun DatabaseDropdown(modifier: Modifier = Modifier, focusReq: FocusRequester? = null, enabled: Boolean = true) {
        Dropdown(
            values = GlobalState.client.databaseList,
            selected = GlobalState.client.session.database,
            onExpand = { GlobalState.client.refreshDatabaseList() },
            onSelection = { GlobalState.client.tryOpenSession(it) },
            placeholder = Label.SELECT_DATABASE,
            enabled = enabled,
            modifier = modifier,
            focusReq = focusReq,
            tooltip = Tooltip.Arg(
                title = Label.SELECT_DATABASE,
                description = Sentence.SELECT_DATABASE_DESCRIPTION
            )
        )
    }
}
