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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.FormRowSpacer
import com.vaticle.typedb.studio.view.common.component.Form.TextButton

object ConfirmationDialog {

    // The height has to be increased manually when we have longer messages to display
    private val HEIGHT = 180.dp
    private val WIDTH = 500.dp

    @Composable
    fun Layout() {
        val dialogState = GlobalState.confirmation
        val focusReq = FocusRequester()
        Dialog.Layout(dialogState, dialogState.title!!, WIDTH, HEIGHT) {
            Column(Modifier.fillMaxSize()) {
                dialogState.message?.let { Form.Text(value = it, softWrap = true) }
                Spacer(Modifier.weight(1f))
                Row(Modifier.defaultMinSize(minHeight = Form.FIELD_HEIGHT), verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(text = Label.CANCEL, focusReq = focusReq, onClick = { dialogState.close() })
                    FormRowSpacer()
                    if (dialogState.hasReject) {
                        TextButton(text = dialogState.rejectLabel ?: "", onClick = { dialogState.reject() })
                        FormRowSpacer()
                    }
                    TextButton(text = dialogState.confirmLabel ?: Label.CONFIRM, onClick = { dialogState.confirm() })
                }
            }
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }
}