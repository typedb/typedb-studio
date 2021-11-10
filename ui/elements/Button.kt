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

package com.vaticle.typedb.studio.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import java.awt.event.KeyEvent.KEY_RELEASED

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StudioButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val focusManager = LocalFocusManager.current
    val backgroundColor = if (enabled) StudioTheme.colors.primary else StudioTheme.colors.uiElementBackground
    val textColor = if (enabled) StudioTheme.colors.onPrimary else StudioTheme.colors.text.copy(alpha = .25f)

    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = modifier
            .height(28.dp)
            .background(backgroundColor, RoundedCornerShape(CornerSize(4.dp)))
            .focusable(enabled = enabled)
            .clickable(enabled = enabled) { onClick() }
            .onKeyEvent { event: KeyEvent ->
                if (event.nativeKeyEvent.id == KEY_RELEASED) return@onKeyEvent false
                if (!enabled) return@onKeyEvent false
                when (event.key) {
                    Key.Tab -> {
                        focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Up else FocusDirection.Down)
                        return@onKeyEvent true
                    }
                    Key.Enter, Key.NumPadEnter -> {
                        onClick()
                        return@onKeyEvent true
                    }
                    else -> return@onKeyEvent false
                }
            }
    ) {
        Spacer(Modifier.width(8.dp))
        Text(text, style = StudioTheme.typography.body1, fontWeight = FontWeight.SemiBold, color = textColor)
        Spacer(Modifier.width(8.dp))
    }
}
