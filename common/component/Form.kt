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

package com.vaticle.typedb.studio.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.common.theme.Theme

object Form {

    private const val PLACEHOLDER_OPACITY = 0.3f
    private const val LABEL_WEIGHT = 2f
    private const val INPUT_WEIGHT = 3f
    private val FIELD_SPACING = 12.dp
    private val FIELD_HEIGHT = 28.dp
    private val ICON_SPACING = 4.dp

    val ColumnScope.LABEL_MODIFIER: Modifier get() = Modifier.weight(LABEL_WEIGHT).height(FIELD_HEIGHT)
    val ColumnScope.INPUT_MODIFIER: Modifier get() = Modifier.weight(INPUT_WEIGHT).height(FIELD_HEIGHT)

    @Composable
    fun Field(content: @Composable () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }

    @Composable
    fun FieldGroup(content: @Composable () -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(FIELD_SPACING)) {
            content()
        }
    }


    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun TextInput(
        value: String,
        placeholder: String,
        onValueChange: (String) -> Unit,
        textStyle: TextStyle,
        modifier: Modifier = Modifier,
        maxLines: Int = 1,
        singleLine: Boolean = true,
        readOnly: Boolean = false,
        enabled: Boolean = true,
        isPassword: Boolean = false,
        pointerHoverIcon: PointerIcon = PointerIcon.Text, // TODO: #516 PointerIconDefaults.Text
        trailingIcon: (@Composable () -> Unit)? = null,
        leadingIcon: (@Composable () -> Unit)? = null
    ) {
        val focusManager = LocalFocusManager.current // for @Composable to be called in lambda
        BasicTextField(
            modifier = modifier
                .background(MaterialTheme.colors.surface, MaterialTheme.shapes.small)
                .border(1.dp, SolidColor(Theme.colors.uiElementBorder), MaterialTheme.shapes.small)
                .pointerIcon(pointerHoverIcon) // TODO: #516 .pointerHoverIcon(pointerHoverIcon, overrideDescendants = true)
                .onPreviewKeyEvent { handleTabKeyEvent(it, focusManager) },
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            textStyle = textStyle.copy(color = MaterialTheme.colors.onSurface),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { innerTextField ->
                Row(modifier.padding(horizontal = ICON_SPACING), verticalAlignment = Alignment.CenterVertically) {
                    leadingIcon?.let { leadingIcon(); Spacer(Modifier.width(ICON_SPACING)) }
                    Box(modifier.offset(y = ICON_SPACING).weight(1f)) {
                        if (value.isNotEmpty()) innerTextField()
                        else Text(
                            text = placeholder,
                            style = textStyle.copy(color = MaterialTheme.colors.onSurface.copy(alpha = PLACEHOLDER_OPACITY))
                        )
                    }
                    trailingIcon?.let { Spacer(Modifier.width(ICON_SPACING)); trailingIcon() }
                }
            },
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun handleTabKeyEvent(event: KeyEvent, focusManager: FocusManager): Boolean {
        if (event.nativeKeyEvent.id == java.awt.event.KeyEvent.KEY_RELEASED) return true
        else when (event.key) {
            Key.Tab -> {
                focusManager.moveFocus(
                    when {
                        event.isShiftPressed -> FocusDirection.Up
                        else -> FocusDirection.Down
                    }
                )
                return true
            }
            else -> return false
        }
    }
}