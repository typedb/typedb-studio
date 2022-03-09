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

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.vaticle.typedb.studio.state.ConfirmationManager
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.view.common.KeyMapper
import com.vaticle.typedb.studio.view.common.KeyMapper.Command.ESCAPE
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.theme.Theme

object ConfirmationDialog {

    // The height has to be increased manually when we have longer messages to display
    private val WINDOW_HEIGHT = 140.dp
    private val WINDOW_WIDTH = 500.dp

    private fun handleKeyEvent(event: KeyEvent, state: ConfirmationManager): Boolean {
        return if (event.type == KeyEventType.KeyUp) false
        else KeyMapper.CURRENT.map(event)?.let { executeCommand(it, state) } ?: false
    }

    private fun executeCommand(command: KeyMapper.Command, state: ConfirmationManager): Boolean {
        return when (command) {
            ESCAPE -> {
                state.close()
                true
            }
            else -> false
        }
    }

    @Composable
    fun Layout() {
        val state = GlobalState.confirmation
        val focusReq = FocusRequester()
        Dialog(
            title = state.title!!,
            onCloseRequest = { state.close() },
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(WINDOW_WIDTH, WINDOW_HEIGHT)
            )
        ) {
            Column(
                Modifier.fillMaxSize()
                    .background(Theme.colors.background)
                    .padding(Theme.DIALOG_PADDING)
                    .focusRequester(focusReq).focusable()
                    .onKeyEvent { handleKeyEvent(it, state) }
            ) {
                state.message?.let { Form.Text(value = it, softWrap = true) }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    Form.TextButton(text = Label.CANCEL, onClick = { state.close() })
                    Form.ComponentSpacer()
                    if (state.hasReject) {
                        Form.TextButton(text = state.rejectLabel ?: "", onClick = { state.reject() })
                        Form.ComponentSpacer()
                    }
                    Form.TextButton(text = state.confirmLabel ?: Label.CONFIRM, onClick = { state.confirm() })
                }
            }
        }
        LaunchedEffect(Unit) { focusReq.requestFocus() }
    }
}