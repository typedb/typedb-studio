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

package com.vaticle.typedb.studio.view.common.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Icon.Code.CARET_DOWN
import com.vaticle.typedb.studio.view.common.theme.Color.fadeable
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.RECTANGLE_ROUNDED_ALL
import com.vaticle.typedb.studio.view.common.theme.Theme.rectangleIndication
import com.vaticle.typedb.studio.view.common.theme.Theme.roundedIndication
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import java.awt.event.KeyEvent.KEY_RELEASED
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object Form {

    private const val LABEL_WEIGHT = 1f
    private const val INPUT_WEIGHT = 3f
    private val OUTER_SPACING = 16.dp
    private val INNER_SPACING = 10.dp
    private val FIELD_SPACING = 12.dp
    private val FIELD_HEIGHT = 28.dp
    private val TEXT_BUTTON_PADDING = 8.dp
    private val MULTILINE_INPUT_PADDING = 4.dp
    private val MULTILINE_INPUT_MIN_WIDTH = 100.dp
    private val ICON_SPACING = 6.dp
    internal val BORDER_WIDTH = 1.dp
    private val DEFAULT_BORDER = Border(BORDER_WIDTH, RECTANGLE_ROUNDED_ALL)

    private val RowScope.LABEL_MODIFIER: Modifier get() = Modifier.weight(LABEL_WEIGHT)
    private val RowScope.INPUT_MODIFIER: Modifier get() = Modifier.weight(INPUT_WEIGHT).height(FIELD_HEIGHT)

    data class ButtonArgs(val icon: Icon.Code, val onClick: () -> Unit)
    data class IconArgs(val code: Icon.Code, val color: @Composable () -> Color = { Theme.colors.icon })
    data class Border(val width: Dp, val shape: Shape, val color: @Composable () -> Color = { Theme.colors.border })

    interface State {
        fun isValid(): Boolean
        fun trySubmit()
        fun trySubmitIfValid()
    }

    @Composable
    fun Field(label: String, fieldInput: @Composable () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value = label, modifier = LABEL_MODIFIER)
            Box(modifier = INPUT_MODIFIER) { fieldInput() }
        }
    }

    @Composable
    fun Submission(state: State? = null, content: @Composable() (ColumnScope.() -> Unit)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(FIELD_SPACING),
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.colors.background)
                .padding(OUTER_SPACING)
                .onKeyEvent { onKeyEvent(event = it, onEnter = { state?.trySubmitIfValid() }) }
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
        alpha: Float? = null,
        align: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = false,
        enabled: Boolean = true,
    ) {
        androidx.compose.material.Text(
            text = value,
            style = style,
            color = fadeable(alpha?.let { color.copy(alpha = alpha) } ?: color, !enabled),
            modifier = modifier,
            overflow = overflow,
            softWrap = softWrap,
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
        bgColor: Color = Theme.colors.primary,
        leadingIcon: Icon.Code? = null,
        trailingIcon: Icon.Code? = null,
        iconColor: Color = Theme.colors.icon,
        enabled: Boolean = true,
        wideMode: Boolean = false,
    ) {
        BoxButton(onClick = onClick, color = bgColor, modifier = modifier, enabled = enabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = TEXT_BUTTON_PADDING)
            ) {
                @Composable
                fun iconSpacing() {
                    if (wideMode) Spacer(Modifier.weight(1f)) else Spacer(Modifier.width(TEXT_BUTTON_PADDING))
                }
                leadingIcon?.let {
                    Icon.Render(icon = it, color = iconColor)
                    iconSpacing()
                }
                Text(text, style = Theme.typography.body1, color = fadeable(textColor, !enabled))
                trailingIcon?.let {
                    iconSpacing()
                    Icon.Render(icon = it, color = iconColor)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun RawClickableIcon(
        icon: Icon.Code,
        onClick: () -> Unit,
        modifier: Modifier,
        iconColor: Color = Theme.colors.icon,
        enabled: Boolean = true
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.height(FIELD_HEIGHT)
                .pointerHoverIcon(icon = PointerIconDefaults.Hand)
                .onPointerEvent(PointerEventType.Press) { if (it.buttons.isPrimaryPressed) onClick() }
        ) { Icon.Render(icon = icon, color = iconColor, enabled = enabled) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun IconButton(
        icon: Icon.Code,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        iconColor: Color = Theme.colors.icon,
        bgColor: Color = Theme.colors.primary,
        rounded: Boolean = true,
        enabled: Boolean = true
    ) {
        BoxButton(
            onClick = onClick,
            color = bgColor,
            modifier = modifier.size(FIELD_HEIGHT),
            rounded = rounded,
            enabled = enabled
        ) { Icon.Render(icon = icon, color = iconColor, enabled = enabled) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun BoxButton(
        onClick: () -> Unit,
        color: Color = Theme.colors.primary,
        modifier: Modifier = Modifier,
        rounded: Boolean = true,
        enabled: Boolean = true,
        content: @Composable BoxScope.() -> Unit
    ) {
        val hoverIndication = when {
            rounded -> roundedIndication(Theme.colors.indicationBase, LocalDensity.current.density)
            else -> rectangleIndication(Theme.colors.indicationBase)
        }
        CompositionLocalProvider(
            LocalIndication provides hoverIndication
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .height(FIELD_HEIGHT)
                    .background(fadeable(color, !enabled), if (rounded) RECTANGLE_ROUNDED_ALL else RectangleShape)
                    .pointerHoverIcon(icon = PointerIconDefaults.Hand)
                    .clickable(enabled = enabled) { onClick() }
            ) { content() }
        }
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
                .border(BORDER_WIDTH, SolidColor(fadeable(Theme.colors.border, !enabled)), RECTANGLE_ROUNDED_ALL)
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
        singleLine: Boolean = true,
        readOnly: Boolean = false,
        enabled: Boolean = true,
        isPassword: Boolean = false,
        modifier: Modifier = Modifier,
        textStyle: TextStyle = Theme.typography.body1,
        pointerHoverIcon: PointerIcon = PointerIconDefaults.Text,
        onTextLayout: (TextLayoutResult) -> Unit = {},
        shape: Shape? = RECTANGLE_ROUNDED_ALL,
        border: Border? = DEFAULT_BORDER,
        trailingIcon: Icon.Code? = null,
        leadingIcon: Icon.Code? = null
    ) {
        val mod = border?.let {
            modifier.border(border.width, fadeable(border.color(), !enabled), border.shape)
        } ?: modifier

        BasicTextField(
            modifier = mod.pointerHoverIcon(pointerHoverIcon)
                .background(fadeable(Theme.colors.surface, !enabled), shape ?: RectangleShape),
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = singleLine,
            enabled = enabled,
            cursorBrush = SolidColor(Theme.colors.secondary),
            textStyle = textStyle.copy(color = fadeable(Theme.colors.onSurface, !enabled)),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            onTextLayout = onTextLayout,
            decorationBox = { innerTextField ->
                Row(
                    Modifier.padding(horizontal = MULTILINE_INPUT_PADDING),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon?.let {
                        Icon.Render(icon = it)
                        Spacer(Modifier.width(ICON_SPACING))
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        innerTextField()
                        if (value.isEmpty()) Text(value = placeholder, color = fadeable(Theme.colors.onSurface, true))
                    }
                    trailingIcon?.let {
                        Spacer(Modifier.width(ICON_SPACING))
                        Icon.Render(icon = it)
                    }
                }
            },
        )
    }

    @Composable
    private fun rememberMultilineTextInputState(): MultilineTextInputState {
        val density = LocalDensity.current.density
        return remember { MultilineTextInputState(density) }
    }

    class MultilineTextInputState(initDensity: Float) {

        internal var value by mutableStateOf(TextFieldValue(""))
        internal var layout: TextLayoutResult? by mutableStateOf(null)
        internal var density by mutableStateOf(initDensity)
        internal var boxWidth by mutableStateOf(0.dp)
        internal val horScroller: ScrollState = ScrollState(0)
        private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

        internal fun updateValue(newValue: TextFieldValue) {
            val oldText = value.text
            value = newValue
            if (oldText == newValue.text) mayScrollHorizontally()
            // else, text have changed and updateLayout() will be called
        }

        internal fun updateLayout(newLayout: TextLayoutResult) {
            layout = newLayout
            mayScrollHorizontally()
        }

        private fun mayScrollHorizontally() {
            val cursorOffset = toDP(layout?.getCursorRect(value.selection.end)?.left ?: 0f, density)
            val scrollOffset = toDP(horScroller.value, density)
            val viewPadding = MULTILINE_INPUT_PADDING * 2
            if (boxWidth + scrollOffset - viewPadding < cursorOffset) {
                val scrollTo = (cursorOffset - boxWidth + viewPadding).value.toInt()
                coroutineScope.launch { horScroller.scrollTo((scrollTo * density).toInt()) }
            } else if (scrollOffset + viewPadding > cursorOffset) {
                val scrollTo = (cursorOffset - viewPadding).value.toInt()
                coroutineScope.launch { horScroller.scrollTo((scrollTo * density).toInt()) }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun MultilineTextInput(
        state: MultilineTextInputState = rememberMultilineTextInputState(),
        value: TextFieldValue,
        modifier: Modifier,
        icon: Icon.Code? = null,
        focusRequester: FocusRequester = FocusRequester(),
        onValueChange: (TextFieldValue) -> Unit,
        onTextLayout: (TextLayoutResult) -> Unit
    ) {
        val density = LocalDensity.current.density
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier.fillMaxWidth()
                .background(Theme.colors.surface)
                .onSizeChanged { state.density = density }
                .pointerHoverIcon(PointerIconDefaults.Text)
                .onPointerEvent(PointerEventType.Press) { focusRequester.requestFocus() }
        ) {
            icon?.let {
                Box(Modifier.size(FIELD_HEIGHT)) { Icon.Render(icon = it, modifier = Modifier.align(Alignment.Center)) }
            } ?: Spacer(Modifier.width(MULTILINE_INPUT_PADDING))
            Box(Modifier.weight(1f).onSizeChanged { state.boxWidth = toDP(it.width, state.density) }) {
                Box(
                    modifier = Modifier.fillMaxHeight()
                        .padding(vertical = MULTILINE_INPUT_PADDING)
                        .horizontalScroll(state.horScroller)
                ) {
                    Row(Modifier.align(alignment = Alignment.CenterStart)) {
                        BasicTextField(
                            value = value,
                            onValueChange = { state.updateValue(it); onValueChange(it) },
                            onTextLayout = { state.updateLayout(it); onTextLayout(it) },
                            cursorBrush = SolidColor(Theme.colors.secondary),
                            textStyle = Theme.typography.body1.copy(Theme.colors.onSurface),
                            modifier = Modifier.focusRequester(focusRequester)
                                .defaultMinSize(minWidth = MULTILINE_INPUT_MIN_WIDTH)
                        )
                        Spacer(Modifier.width(MULTILINE_INPUT_PADDING))
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun <T : Any> Dropdown(
        values: List<T>,
        selected: T,
        onExpand: (() -> Unit)? = null,
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
                if (expanded && onExpand != null) onExpand()
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
                textColor = fadeable(Theme.colors.onPrimary, selected.toString().isBlank()),
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
                val padding = PaddingValues(horizontal = MULTILINE_INPUT_PADDING)
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