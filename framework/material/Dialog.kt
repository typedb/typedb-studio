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

package com.vaticle.typedb.studio.framework.material

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
import com.vaticle.typedb.studio.framework.common.KeyMapper
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.service.common.util.DialogState

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
    ) = Dialog(
        title = title, onCloseRequest = { state.close() }, state = rememberDialogState(
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(width, height)
        )
    ) {
        Box(Modifier.background(Theme.studio.backgroundMedium).padding(padding)
            .onKeyEvent { handleKeyEvent(it, state) }) {
            content()
        }
    }
}