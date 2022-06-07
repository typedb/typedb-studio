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

package com.vaticle.typedb.studio.view.material

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.material.Dialog.DIALOG_SPACING
import com.vaticle.typedb.studio.view.material.Form.FormRowSpacer
import com.vaticle.typedb.studio.view.material.Form.TextButton

object ConfirmationDialog {

    // The height has to be increased manually when we have longer messages to display
    private val HEIGHT = 220.dp
    private val WIDTH = 500.dp

    internal class State : Form.State {

        var verificationInput by mutableStateOf("")
        val hasReject get() = GlobalState.confirmation.hasReject
        val hasConfirm get() = GlobalState.confirmation.hasConfirm
        val cancelLabel get() = GlobalState.confirmation.cancelLabel
        val rejectLabel get() = GlobalState.confirmation.rejectLabel
        val confirmLabel get() = GlobalState.confirmation.confirmLabel

        fun reject() {
            GlobalState.confirmation.reject()
        }

        override fun cancel() {
            GlobalState.confirmation.close()
        }

        override fun isValid(): Boolean {
            val verificationValue = GlobalState.confirmation.verificationValue
            return verificationValue == null || verificationValue == verificationInput
        }

        override fun trySubmit() {
            GlobalState.confirmation.confirm()
        }
    }

    @Composable
    fun Layout() {
        val dialogState = GlobalState.confirmation
        val formState = remember { State() }
        val focusReq = remember { FocusRequester() }
        Dialog.Layout(dialogState, dialogState.title!!, WIDTH, HEIGHT) {
            Form.Submission(formState, showButtons = false) {
                Column(Modifier.fillMaxSize()) {
                    dialogState.message?.let { Form.Text(value = it, softWrap = true) }
                    dialogState.verificationValue?.let {
                        Spacer(Modifier.height(DIALOG_SPACING))
                        VerificationInputForm(formState, focusReq)
                    }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.defaultMinSize(minHeight = Form.FIELD_HEIGHT), verticalAlignment = Alignment.Bottom) {
                        Spacer(modifier = Modifier.weight(1f))
                        ConfirmationButtons(formState, if (dialogState.verificationValue == null) focusReq else null)
                    }
                }
            }
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun VerificationInputForm(formState: State, focusReq: FocusRequester) {
        Form.TextInput(
            value = formState.verificationInput,
            placeholder = Label.DATABASE_NAME,
            onValueChange = { formState.verificationInput = it },
            fontColor = Theme.studio.errorStroke,
            modifier = Modifier.focusRequester(focusReq).fillMaxWidth()
                .border(1.dp, Theme.studio.errorStroke, Form.DEFAULT_BORDER.shape),
        )
    }

    @Composable
    private fun ConfirmationButtons(formState: State, focusReq: FocusRequester?) {
        TextButton(text = formState.cancelLabel ?: Label.CANCEL, focusReq = focusReq) { formState.cancel() }
        FormRowSpacer()
        if (formState.hasReject) {
            TextButton(text = formState.rejectLabel ?: "") { formState.reject() }
            FormRowSpacer()
        }
        if (formState.hasConfirm) TextButton(
            text = formState.confirmLabel ?: Label.CONFIRM,
            enabled = formState.isValid()
        ) { formState.trySubmitIfValid() }
    }
}