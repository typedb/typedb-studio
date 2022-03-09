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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.vaticle.typedb.studio.state.common.DialogManager
import com.vaticle.typedb.studio.view.common.KeyMapper

object Dialog {

    private fun handleKeyEvent(event: KeyEvent, state: DialogManager): Boolean {
        return if (event.type == KeyEventType.KeyUp) false
        else KeyMapper.CURRENT.map(event)?.let { executeCommand(it, state) } ?: false
    }

    private fun executeCommand(command: KeyMapper.Command, state: DialogManager): Boolean {
        return when (command) {
            KeyMapper.Command.ESCAPE -> {
                state.close()
                true
            }
            else -> false
        }
    }

    @Composable
    fun Layout(
        state: DialogManager, title: String, width: Dp, height: Dp, onClose: () -> Unit,
        content: @Composable DialogWindowScope.() -> Unit
    ) {
        val focusReq = FocusRequester()
        Dialog(
            title = title, onCloseRequest = onClose, state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(width, height)
            )
        ) {
            Box(Modifier.focusRequester(focusReq).focusable().onKeyEvent { handleKeyEvent(it, state) }) {
                content()
            }
        }
        LaunchedEffect(Unit) { focusReq.requestFocus() }
    }
}