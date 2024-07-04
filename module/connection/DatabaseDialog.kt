/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
        override fun cancel() = Service.driver.manageDatabasesDialog.close()
        override fun isValid(): Boolean = name.isNotBlank()
        override fun submit() {
            assert(isValid())
            Service.driver.tryCreateDatabase(name) { name = "" }
        }
    }

    @Composable
    fun MayShowDialogs() {
        if (Service.driver.manageDatabasesDialog.isOpen) ManageDatabases()
        if (Service.driver.selectDBDialog.isOpen) SelectDatabase()
    }

    @Composable
    private fun ManageDatabases() = Dialog.Layout(
        state = Service.driver.manageDatabasesDialog,
        title = Label.MANAGE_DATABASES,
        width = MANAGER_WIDTH,
        height = MANAGER_HEIGHT
    ) {
        Column(Modifier.fillMaxSize()) {
            Form.Text(value = Sentence.MANAGE_DATABASES_MESSAGE, softWrap = true)
            Spacer(Modifier.height(Theme.DIALOG_PADDING))
            ManageableDatabaseList(Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(Theme.DIALOG_PADDING))
            CreateDatabaseForm()
            Spacer(Modifier.height(Theme.DIALOG_PADDING * 2))
            Row(verticalAlignment = Alignment.Bottom) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(text = Label.REFRESH, leadingIcon = Form.IconArg(Icon.REFRESH)) {
                    Service.driver.refreshDatabaseList()
                }
                RowSpacer()
                TextButton(text = Label.CLOSE) { Service.driver.manageDatabasesDialog.close() }
            }
        }
    }

    @Composable
    private fun ManageableDatabaseList(modifier: Modifier) = ActionableList.Layout(
        items = Service.driver.databaseList,
        modifier = modifier.border(1.dp, Theme.studio.border),
        buttonsSide = ActionableList.Side.RIGHT
    ) { databaseName ->
        listOf(
            IconButtonArg(
                icon = Icon.DELETE,
                color = { Theme.studio.errorStroke }
            ) {
                Service.confirmation.submit(
                    title = Label.DELETE_DATABASE,
                    message = Sentence.CONFIRM_DATABASE_DELETION.format(databaseName),
                    verificationValue = databaseName,
                    confirmLabel = Label.DELETE,
                    onConfirm = { Service.driver.tryDeleteDatabase(databaseName) }
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
        val dialogState = Service.driver.selectDBDialog
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
        values = Service.driver.databaseList,
        selected = Service.driver.session.database,
        onSelection = { it?.let { Service.driver.tryOpenSession(it) } ?: Service.driver.closeSession() },
        onExpand = { Service.driver.refreshDatabaseList() },
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
