/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.typedb.studio.framework.common.KeyMapper
import com.typedb.studio.framework.common.theme.Theme
import com.typedb.studio.service.common.util.DialogState
import androidx.compose.ui.window.DialogState as ComposeDialogState

object Dialog {

    val DIALOG_SPACING = 16.dp

    private fun handleKeyEvent(event: KeyEvent, state: DialogState): Boolean = when (event.type) {
        KeyEventType.KeyUp -> false
        else -> KeyMapper.CURRENT.map(event)?.let { executeCommand(it, state) } ?: false
    }

    private fun executeCommand(command: KeyMapper.Command, state: DialogState): Boolean = when (command) {
        KeyMapper.Command.ESCAPE -> {
            state.close()
            true
        }
        else -> false
    }

    @Composable
    fun Layout(
        state: DialogState, title: String, width: Dp, height: Dp,
        padding: Dp = Theme.DIALOG_PADDING,
        content: @Composable DialogWindowScope.() -> Unit
    ) = Layout(state, title, padding,
        rememberDialogState(
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(width, height)
        ),
        content
    )

    @Composable
    fun Layout(
        state: DialogState, title: String,
        padding: Dp = Theme.DIALOG_PADDING,
        composeDialogState: ComposeDialogState,
        content: @Composable DialogWindowScope.() -> Unit
    ) = Dialog(
        title = title, onCloseRequest = { state.close() }, state = composeDialogState
    ) {
        Box(Modifier.background(Theme.studio.backgroundMedium).padding(padding)
            .onKeyEvent { handleKeyEvent(it, state) }) {
            content()
        }
    }
}
