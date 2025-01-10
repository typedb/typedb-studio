/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.module.user

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.typedb.studio.framework.material.Dialog
import com.typedb.studio.framework.material.Form
import com.typedb.studio.service.Service
import com.typedb.studio.service.common.util.Label
import com.typedb.studio.service.common.util.Sentence.UPDATE_DEFAULT_PASSWORD_FOR_USERNAME
import com.typedb.studio.service.common.util.Sentence.UPDATE_DEFAULT_PASSWORD_INSTRUCTION

object UpdatePasswordDialog {

    private val WIDTH = 500.dp
    private val HEIGHT = 300.dp

    private val state by mutableStateOf(UpdateDefaultPasswordForm())

    private class UpdateDefaultPasswordForm : Form.State() {
        var newPassword: String by mutableStateOf("")
        var repeatPassword: String by mutableStateOf("")

        override fun cancel() {
            Service.user.updatePasswordDialog.close()
            newPassword = ""
        }
        override fun submit() {
            assert(isValid())
            Service.user.tryUpdatePassword(newPassword) {
                Service.user.updatePasswordDialog.close()
                newPassword = ""
            }
        }

        override fun isValid() = newPassword.isNotEmpty() && repeatPassword == newPassword
    }

    @Composable
    fun MayShowDialogs() {
        if (Service.user.updatePasswordDialog.isOpen) UpdateDefaultPassword()
    }

    @Composable
    private fun UpdateDefaultPassword() = Dialog.Layout(
        state = Service.driver.connectServerDialog,
        title = UPDATE_DEFAULT_PASSWORD_FOR_USERNAME.format(Service.data.connection.username).removeSuffix("."),
        width = WIDTH,
        height = HEIGHT
    ) {
        Form.Submission(state = state, modifier = Modifier.fillMaxSize(), showButtons = true) {
            Form.Text(value = UPDATE_DEFAULT_PASSWORD_INSTRUCTION, softWrap = true)
            NewPasswordFormField(state)
            RepeatPasswordFormField(state)
        }
    }

    @Composable
    private fun NewPasswordFormField(state: UpdateDefaultPasswordForm) = Form.Field(label = Label.NEW_PASSWORD) {
        Form.TextInput(
            value = state.newPassword,
            onValueChange = { state.newPassword = it },
            isPassword = true,
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Composable
    private fun RepeatPasswordFormField(state: UpdateDefaultPasswordForm) = Form.Field(label = Label.REPEAT_PASSWORD) {
        Form.TextInput(
            value = state.repeatPassword,
            onValueChange = { state.repeatPassword = it },
            isPassword = true,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
