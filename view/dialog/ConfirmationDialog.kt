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
import com.vaticle.typedb.studio.state.ConfirmationManager
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.FormRowSpacer
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.dialog.Dialog.DIALOG_SPACING

object ConfirmationDialog {

    // The height has to be increased manually when we have longer messages to display
    private val HEIGHT = 220.dp
    private val WIDTH = 500.dp

    object State : Form.State {

        var verificationInput by mutableStateOf("")

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
        val focusReq = remember { FocusRequester() }
        Dialog.Layout(dialogState, dialogState.title!!, WIDTH, HEIGHT) {
            Column(Modifier.fillMaxSize()) {
                dialogState.message?.let { Form.Text(value = it, softWrap = true) }
                dialogState.verificationValue?.let {
                    Spacer(Modifier.height(DIALOG_SPACING))
                    VerificationInputForm(focusReq)
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.defaultMinSize(minHeight = Form.FIELD_HEIGHT), verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    ConfirmationButtons(if (dialogState.verificationValue == null) focusReq else null, dialogState)
                }
            }
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun VerificationInputForm(focusReq: FocusRequester) {
        Form.Submission(State) {
            Form.TextInput(
                value = State.verificationInput,
                placeholder = Label.DATABASE_NAME,
                onValueChange = { State.verificationInput = it },
                modifier = Modifier.fillMaxWidth().height(Form.FIELD_HEIGHT).focusRequester(focusReq),
            )
        }
    }

    @Composable
    private fun ConfirmationButtons(focusReq: FocusRequester?, dialogState: ConfirmationManager) {
        TextButton(text = Label.CANCEL, focusReq = focusReq, onClick = { State.cancel() })
        FormRowSpacer()
        if (dialogState.hasReject) {
            TextButton(text = dialogState.rejectLabel ?: "", onClick = { dialogState.reject() })
            FormRowSpacer()
        }
        TextButton(
            text = dialogState.confirmLabel ?: Label.CONFIRM,
            onClick = { State.trySubmitIfValid() },
            enabled = State.isValid()
        )
    }
}