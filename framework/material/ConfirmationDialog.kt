/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.material

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
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Form.RowSpacer
import com.vaticle.typedb.studio.framework.material.Form.TextButton
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label

object ConfirmationDialog {

    // The height has to be increased manually when we have longer messages to display
    private val HEIGHT = 220.dp
    private val WIDTH = 500.dp

    internal class State : Form.State() {

        var verificationInput by mutableStateOf("")
        val hasReject get() = Service.confirmation.hasReject
        val hasConfirm get() = Service.confirmation.hasConfirm
        val cancelLabel get() = Service.confirmation.cancelLabel
        val rejectLabel get() = Service.confirmation.rejectLabel
        val confirmLabel get() = Service.confirmation.confirmLabel

        fun reject() = Service.confirmation.reject()
        override fun cancel() = Service.confirmation.close()
        override fun submit() = Service.confirmation.confirm()

        override fun isValid(): Boolean {
            val verificationValue = Service.confirmation.verificationValue
            return verificationValue == null || verificationValue == verificationInput
        }
    }

    @Composable
    fun MayShowDialog() {
        if (Service.confirmation.isOpen) Layout()
    }

    @Composable
    private fun Layout() {
        val dialogState = Service.confirmation
        val formState = remember { State() }
        val focusReq = remember { FocusRequester() }
        Dialog.Layout(dialogState, dialogState.title!!, WIDTH, HEIGHT) {
            Form.Submission(formState, showButtons = false) {
                Column(Modifier.fillMaxSize()) {
                    dialogState.message?.let { Form.Text(value = it, softWrap = true) }
                    dialogState.verificationValue?.let {
                        Spacer(Modifier.height(Theme.DIALOG_PADDING))
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
        RowSpacer()
        if (formState.hasReject) {
            TextButton(text = formState.rejectLabel ?: "") { formState.reject() }
            RowSpacer()
        }
        if (formState.hasConfirm) TextButton(
            text = formState.confirmLabel ?: Label.CONFIRM,
            enabled = formState.isValid()
        ) { formState.submitIfValid() }
    }
}
