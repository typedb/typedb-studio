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

package com.vaticle.typedb.studio.viewer.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.viewer.common.Label
import com.vaticle.typedb.studio.viewer.common.component.Icon.Code.CARET_DOWN
import com.vaticle.typedb.studio.viewer.common.theme.Color.fadeable
import com.vaticle.typedb.studio.viewer.common.theme.Theme
import com.vaticle.typedb.studio.viewer.common.theme.Theme.toDP
import java.awt.event.KeyEvent.KEY_RELEASED

object Form {

    private const val LABEL_WEIGHT = 1f
    private const val INPUT_WEIGHT = 3f
    private val OUTER_SPACING = 16.dp
    private val INNER_SPACING = 10.dp
    private val FIELD_SPACING = 12.dp
    private val FIELD_HEIGHT = 28.dp
    private val BORDER_WIDTH = 1.dp
    private val CONTENT_PADDING = 8.dp
    private val ICON_SPACING = 4.dp
    private val ROUNDED_RECTANGLE = RoundedCornerShape(4.dp)

    private val RowScope.LABEL_MODIFIER: Modifier get() = Modifier.weight(LABEL_WEIGHT)
    private val RowScope.INPUT_MODIFIER: Modifier get() = Modifier.weight(INPUT_WEIGHT).height(FIELD_HEIGHT)

    @Composable
    fun Field(label: String, fieldInput: @Composable () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value = label, modifier = LABEL_MODIFIER)
            Column(modifier = INPUT_MODIFIER) { fieldInput() }
        }
    }

    @Composable
    fun Content(onSubmit: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
        Column(
            verticalArrangement = Arrangement.spacedBy(FIELD_SPACING),
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.colors.background)
                .padding(OUTER_SPACING)
                .onKeyEvent { onKeyEvent(event = it, onEnter = onSubmit) }
        ) { content() }
    }

    @Composable
    fun ComponentSpacer() {
        Spacer(modifier = Modifier.width(INNER_SPACING))
    }

    @Composable
    fun Text(
        value: String,
        style: TextStyle = Theme.typography.body1,
        color: Color = Theme.colors.onPrimary,
        align: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier
    ) {
        androidx.compose.material.Text(
            text = value,
            style = style,
            color = color,
            modifier = modifier,
            textAlign = align,
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun TextButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        textColor: Color = Theme.colors.onPrimary,
        trailingIcon: Icon.Code? = null,
        iconColor: Color = Theme.colors.icon,
        enabled: Boolean = true,
        wideMode: Boolean = false,
    ) {
        BoxButton(onClick = onClick, modifier = modifier, enabled = enabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = CONTENT_PADDING)
            ) {
                Text(text, style = Theme.typography.body1, color = fadeable(textColor, !enabled))
                if (wideMode) Spacer(Modifier.weight(1f))
                trailingIcon?.let {
                    Spacer(Modifier.width(CONTENT_PADDING))
                    Icon.Render(icon = it, color = iconColor)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun IconButton(
        icon: Icon.Code,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        color: Color = Theme.colors.icon,
        enabled: Boolean = true
    ) {
        BoxButton(
            onClick = onClick,
            modifier = modifier.size(FIELD_HEIGHT),
            enabled = enabled
        ) {
            Icon.Render(icon = icon, color = color, enabled = enabled)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun BoxButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable BoxScope.() -> Unit
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .height(FIELD_HEIGHT)
                .background(fadeable(Theme.colors.primary, !enabled), ROUNDED_RECTANGLE)
                .pointerHoverIcon(icon = PointerIconDefaults.Hand)
                .clickable(enabled = enabled) { onClick() }
                .onKeyEvent { onKeyEvent(event = it, enabled = enabled, onEnter = onClick) }
        ) {
            content()
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun TextSelectable(
        value: String,
        style: TextStyle = Theme.typography.body1,
        color: Color = Theme.colors.onSurface,
        modifier: Modifier = Modifier
    ) {
        BasicTextField(
            modifier = modifier.pointerHoverIcon(icon = PointerIconDefaults.Hand),
            value = value,
            onValueChange = {},
            readOnly = true,
            cursorBrush = SolidColor(Theme.colors.secondary),
            textStyle = style.copy(color = color)
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun TextInput(
        value: String,
        placeholder: String,
        onValueChange: (String) -> Unit,
        maxLines: Int = 1,
        singleLine: Boolean = true,
        readOnly: Boolean = false,
        enabled: Boolean = true,
        isPassword: Boolean = false,
        modifier: Modifier = Modifier,
        textStyle: TextStyle = Theme.typography.body1,
        pointerHoverIcon: PointerIcon = PointerIconDefaults.Text,
        trailingIcon: (@Composable () -> Unit)? = null,
        leadingIcon: (@Composable () -> Unit)? = null
    ) {
        val borderBrush = SolidColor(fadeable(Theme.colors.border, !enabled))
        BasicTextField(
            modifier = modifier
                .background(fadeable(Theme.colors.surface, !enabled), ROUNDED_RECTANGLE)
                .border(width = BORDER_WIDTH, brush = borderBrush, shape = ROUNDED_RECTANGLE)
                .pointerHoverIcon(pointerHoverIcon)
                .onPreviewKeyEvent { onKeyEvent(event = it, enabled = enabled) },
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            cursorBrush = SolidColor(Theme.colors.secondary),
            textStyle = textStyle.copy(color = fadeable(Theme.colors.onSurface, !enabled)),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { innerTextField ->
                Row(modifier.padding(horizontal = CONTENT_PADDING), verticalAlignment = Alignment.CenterVertically) {
                    leadingIcon?.let { leadingIcon(); Spacer(Modifier.width(ICON_SPACING)) }
                    Box(Modifier.fillMaxHeight().weight(1f), contentAlignment = Alignment.CenterStart) {
                        innerTextField()
                        if (value.isEmpty()) Text(value = placeholder, color = fadeable(Theme.colors.onSurface, true))
                    }
                    trailingIcon?.let {
                        Spacer(Modifier.width(ICON_SPACING))
                        trailingIcon()
                    }
                }
            },
        )
    }

    @Composable
    fun Checkbox(
        value: Boolean,
        onChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        Checkbox(
            checked = value,
            onCheckedChange = onChange,
            modifier = modifier.size(FIELD_HEIGHT)
                .background(color = fadeable(Theme.colors.surface, !enabled))
                .border(BORDER_WIDTH, SolidColor(fadeable(Theme.colors.border, !enabled)), ROUNDED_RECTANGLE)
                .onKeyEvent { onKeyEvent(event = it, onSpace = { onChange(!value) }) },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = fadeable(Theme.colors.icon, !enabled),
                uncheckedColor = fadeable(Theme.colors.surface, !enabled),
                disabledColor = fadeable(Theme.colors.surface, !enabled)
            )
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun <T : Any> Dropdown(
        values: List<T>,
        selected: T,
        onRefresh: (() -> Unit)? = null,
        onSelection: (value: T) -> Unit,
        placeholder: String = "",
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
    ) {

        class DropdownState {
            var expanded by mutableStateOf(false)
            var mouseIndex: Int? by mutableStateOf(null)
            var width: Dp by mutableStateOf(0.dp)

            fun toggle() {
                expanded = !expanded
                if (expanded && onRefresh != null) onRefresh()
            }

            fun select(value: T) {
                onSelection(value); expanded = false
            }

            fun mouseOutFrom(index: Int): Boolean {
                if (mouseIndex == index) mouseIndex = null; return false
            }

            fun mouseInTo(index: Int): Boolean {
                mouseIndex = index; return true
            }
        }

        val pixelDensity = LocalDensity.current.density
        val state = remember { DropdownState() }
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.onSizeChanged { state.width = toDP(it.width, pixelDensity) }
        ) {
            TextButton(
                text = selected.toString().ifBlank { placeholder },
                onClick = { state.toggle() },
                trailingIcon = CARET_DOWN,
                enabled = enabled,
                wideMode = true,
            )
            DropdownMenu(
                expanded = state.expanded,
                onDismissRequest = { state.expanded = false },
                modifier = Modifier.background(Theme.colors.surface).defaultMinSize(minWidth = state.width)
            ) {
                val padding = PaddingValues(horizontal = CONTENT_PADDING)
                val itemModifier = Modifier.height(FIELD_HEIGHT)
                if (values.isEmpty()) DropdownMenuItem(
                    onClick = {}, contentPadding = padding,
                    modifier = itemModifier.background(Theme.colors.surface)
                ) {
                    Text(value = "(${Label.NONE})")
                } else values.forEachIndexed { i, value ->
                    DropdownMenuItem(
                        onClick = { state.select(value) }, contentPadding = padding, modifier = itemModifier
                            .background(if (i == state.mouseIndex) Theme.colors.primary else Theme.colors.surface)
                            .pointerMoveFilter(onExit = { state.mouseOutFrom(i) }, onEnter = { state.mouseInTo(i) })
                            .pointerHoverIcon(icon = PointerIconDefaults.Hand)
                    ) {
                        val isSelected = value == selected
                        val color = if (isSelected) Theme.colors.secondary else Theme.colors.onSurface
                        Text(value = value.toString(), color = color)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun onKeyEvent(
        event: KeyEvent,
        enabled: Boolean = true,
        onEnter: (() -> Unit)? = null,
        onSpace: (() -> Unit)? = null
    ): Boolean {
        return when {
            event.awtEvent.id == KEY_RELEASED -> false
            !enabled -> false
            else -> when (event.key) {
                Key.Enter, Key.NumPadEnter -> onEnter?.let { it(); true } ?: false
                Key.Spacebar -> onSpace?.let { it(); true } ?: false
                else -> false
            }
        }
    }
}