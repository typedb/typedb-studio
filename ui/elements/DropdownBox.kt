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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.IconSize.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StudioDropdownBox(
    items: List<String>, text: String, onTextChange: (value: String) -> Unit, modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier, textStyle: TextStyle = StudioTheme.typography.body1,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var textfieldSize by remember { mutableStateOf(Size.Zero)}
    var mouseOverIndex: Int? by remember { mutableStateOf(null) }
    val focusRequester = FocusRequester()

    Column(modifier) {
        Row {
            StudioTextField(
                value = text, onValueChange = { onTextChange(it) },
                modifier = textFieldModifier
                    .fillMaxSize()
                    .focusable()
                    .focusRequester(focusRequester)
                    .clickable {
                        expanded = !expanded
                        focusRequester.requestFocus()
                    }
                    .onKeyEvent {
                        if (it.nativeKeyEvent.id == java.awt.event.KeyEvent.KEY_RELEASED) return@onKeyEvent false
                        when (it.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                expanded = !expanded
                                return@onKeyEvent true
                            }
                            else -> return@onKeyEvent false
                        }
                    }
                    .onGloballyPositioned { coordinates ->
                        // This is used to keep the DropdownMenu the same width as the TextField
                        textfieldSize = coordinates.size.toSize()
                    },
                readOnly = true, textStyle = textStyle, leadingIcon = leadingIcon,
                trailingIcon = { StudioIcon(Icon.CaretDown, size = Size16) },
                pointerHoverIcon = PointerIcon.Hand /*PointerIconDefaults.Hand*/
            )
        }

        Row {
            DropdownMenu(
                expanded = expanded && items.isNotEmpty(), onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(with(LocalDensity.current) { textfieldSize.width.toDp() })
                    .background(StudioTheme.colors.uiElementBackground)
            ) {
                items.forEachIndexed { idx, label ->
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onTextChange(label)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp)
                            .background(if (idx == mouseOverIndex) StudioTheme.colors.primary else StudioTheme.colors.uiElementBackground)
                            .pointerMoveFilter(onExit = {
                                if (mouseOverIndex == idx) mouseOverIndex = null
                                return@pointerMoveFilter false
                            }) /* onEnter = */ {
                                mouseOverIndex = idx
                                return@pointerMoveFilter true
                            }
                    ) {
                        Text(label, style = textStyle, color = if (idx == mouseOverIndex) StudioTheme.colors.onPrimary else StudioTheme.colors.text)
                    }
                }
            }
        }
    }
}
