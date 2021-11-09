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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.StudioTextFieldVariant.*
import java.awt.Cursor

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StudioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    variant: StudioTextFieldVariant = OUTLINED,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    readOnly: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    placeholderText: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    // TODO: Currently pointerHoverIcon has no effect - see https://github.com/JetBrains/compose-jb/issues/1315
    pointerHoverIcon: PointerIcon = PointerIconDefaults.Text,
    textStyle: TextStyle) {

    val focusManager = LocalFocusManager.current
    var basicTextFieldModifier = modifier

    if (variant == OUTLINED) {
        basicTextFieldModifier = basicTextFieldModifier
            .background(MaterialTheme.colors.surface, MaterialTheme.shapes.small)
            .border(1.dp, SolidColor(StudioTheme.colors.uiElementBorder), MaterialTheme.shapes.small)
    }

    BasicTextField(modifier = basicTextFieldModifier.pointerHoverIcon(pointerHoverIcon, overrideDescendants = true)
        .onPreviewKeyEvent { event: KeyEvent ->
            if (event.nativeKeyEvent.id == java.awt.event.KeyEvent.KEY_RELEASED) return@onPreviewKeyEvent true
            when (event.key) {
                Key.Tab -> {
                    focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Up else FocusDirection.Down)
                    return@onPreviewKeyEvent true
                }
                else -> return@onPreviewKeyEvent false
            }
        },
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        textStyle = textStyle.copy(color = MaterialTheme.colors.onSurface),
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Row(modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(Modifier.width(4.dp))
                }
                Box(modifier.offset(y = if (variant == UNDECORATED) 0.dp else 4.dp).weight(1f)) {
                    if (value.isEmpty()) Text(
                        placeholderText,
                        style = textStyle.copy(color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
                    )
                    innerTextField()
                }
                if (trailingIcon != null) {
                    Spacer(Modifier.width(4.dp))
                    trailingIcon()
                }
            }
        }
    )
}

enum class StudioTextFieldVariant {
    OUTLINED,
    UNDECORATED
}
