/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.module.connection

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
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.ActionableList
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.Dropdown
import com.vaticle.typedb.studio.framework.material.Form.FIELD_HEIGHT
import com.vaticle.typedb.studio.framework.material.Form.Field
import com.vaticle.typedb.studio.framework.material.Form.IconButtonArg
import com.vaticle.typedb.studio.framework.material.Form.RowSpacer
import com.vaticle.typedb.studio.framework.material.Form.Submission
import com.vaticle.typedb.studio.framework.material.Form.TextButton
import com.vaticle.typedb.studio.framework.material.Form.TextInput
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Tooltip
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Sentence

object DatabaseDialog {

    private val MANAGER_WIDTH = 400.dp
    private val MANAGER_HEIGHT = 500.dp
    private val SELECTOR_WIDTH = 400.dp
    private val SELECTOR_HEIGHT = 200.dp

    private object CreateDatabaseForm : Form.State() {
        var name: String by mutableStateOf("")
        override fun cancel() = Service.client.manageDatabasesDialog.close()
        override fun isValid(): Boolean = name.isNotBlank()
        override fun submit() {
            assert(isValid())
            Service.client.tryCreateDatabase(name) { name = "" }
        }
    }

    @Composable
    fun MayShowDialogs() {
        if (Service.client.manageDatabasesDialog.isOpen) ManageDatabases()
        if (Service.client.selectDBDialog.isOpen) SelectDatabase()
    }

    @Composable
    private fun ManageDatabases() = Dialog.Layout(
        state = Service.client.manageDatabasesDialog,
        title = Label.MANAGE_DATABASES,
        width = MANAGER_WIDTH,
        height = MANAGER_HEIGHT
    ) {
        Column(Modifier.fillMaxSize()) {
            Form.Text(value = Sentence.MANAGE_DATABASES_MESSAGE, softWrap = true)
            Spacer(Modifier.height(Theme.DIALOG_PADDING))
            ManageDatabaseList(Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(Theme.DIALOG_PADDING))
            CreateDatabaseForm()
            Spacer(Modifier.height(Theme.DIALOG_PADDING * 2))
            Row(verticalAlignment = Alignment.Bottom) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(text = Label.REFRESH, leadingIcon = Form.IconArg(Icon.REFRESH)) {
                    Service.client.refreshDatabaseList()
                }
                RowSpacer()
                TextButton(text = Label.CLOSE) { Service.client.manageDatabasesDialog.close() }
            }
        }
    }

    @Composable
    private fun ManageDatabaseList(modifier: Modifier) = ActionableList.Layout(
        items = Service.client.databaseList,
        modifier = modifier.border(1.dp, Theme.studio.border),
        buttonSide = ActionableList.Side.RIGHT,
        buttonsFn = { databaseName ->
            listOf(
                IconButtonArg(
                    icon = Icon.EXPORT,
                    enabled = Service.project.current != null && !Service.schema.hasRunningCommand,
                    tooltip = Tooltip.Arg(title = Label.EXPORT_SCHEMA)
                ) {
                    Service.client.fetchSchema(databaseName).let { schema ->
                        Service.project.tryCreateUntitledFile()?.let { file ->
                            file.content(schema ?: "")
                            file.tryOpen()
                        }
                    }
                },
                IconButtonArg(
                    icon = Icon.DELETE,
                    color = { Theme.studio.errorStroke }
                ) {
                    Service.confirmation.submit(
                        title = Label.DELETE_DATABASE,
                        message = Sentence.CONFIRM_DATABASE_DELETION.format(databaseName),
                        verificationValue = databaseName,
                        confirmLabel = Label.DELETE,
                        onConfirm = { Service.client.tryDeleteDatabase(databaseName) }
                    )
                }
            )
        }
    )

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
                RowSpacer()
                TextButton(
                    text = Label.CREATE,
                    enabled = CreateDatabaseForm.isValid(),
                    tooltip = Tooltip.Arg(
                        title = Label.CREATE_DATABASE,
                        description = Sentence.CREATE_DATABASE_BUTTON_DESCRIPTION
                    )
                ) { CreateDatabaseForm.submit() }
            }
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun SelectDatabase() {
        val dialogState = Service.client.selectDBDialog
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
    fun DatabaseDropdown(
        modifier: Modifier = Modifier,
        focusReq: FocusRequester? = null,
        enabled: Boolean = true
    ) = Dropdown(
        values = Service.client.databaseList,
        selected = Service.client.session.database,
        onSelection = { it?.let { Service.client.tryOpenSession(it) } ?: Service.client.closeSession() },
        onExpand = { Service.client.refreshDatabaseList() },
        placeholder = Label.DATABASE.lowercase(),
        modifier = modifier,
        allowNone = true,
        enabled = enabled,
        focusReq = focusReq,
        tooltip = Tooltip.Arg(
            title = Label.SELECT_DATABASE,
            description = Sentence.SELECT_DATABASE_DESCRIPTION
        )
    )
}
