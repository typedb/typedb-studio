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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Icon.Code.CARET_DOWN
import com.vaticle.typedb.studio.view.common.theme.Color.fadeable
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.ROUNDED_CORNER_SHAPE
import com.vaticle.typedb.studio.view.common.theme.Theme.RoundedCorners
import com.vaticle.typedb.studio.view.common.theme.Theme.rectangleIndication
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import java.awt.event.KeyEvent.KEY_RELEASED
import java.net.URL
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object Form {

    val FIELD_HEIGHT = 28.dp
    internal val BORDER_WIDTH = 1.dp
    private const val LABEL_WEIGHT = 1f
    private const val INPUT_WEIGHT = 3f
    private val INNER_SPACING = 10.dp
    private val FIELD_SPACING = 12.dp
    private val TRAILING_ICON_SIZE = 12.dp
    private val TEXT_BUTTON_PADDING = 8.dp
    private val MULTILINE_INPUT_PADDING = 4.dp
    private val LOADING_SPINNER_SIZE = 14.dp
    private val LOADING_SPINNER_STROKE_WIDTH = 2.dp
    private val ICON_SPACING = 6.dp
    private val DEFAULT_BORDER = Border(BORDER_WIDTH, ROUNDED_CORNER_SHAPE)

    private val RowScope.LABEL_MODIFIER: Modifier get() = Modifier.weight(LABEL_WEIGHT)
    private val RowScope.INPUT_MODIFIER: Modifier get() = Modifier.weight(INPUT_WEIGHT).height(FIELD_HEIGHT)

    data class Border(val width: Dp, val shape: Shape, val color: @Composable () -> Color = { Theme.colors.border })
    data class IconArg(val code: Icon.Code, val color: @Composable () -> Color = { Theme.colors.icon })

    data class IconButtonArg(
        val icon: Icon.Code,
        val hoverIcon: Icon.Code? = null,
        val color: @Composable () -> Color = { Theme.colors.icon },
        val hoverColor: @Composable (() -> Color)? = null,
        val disabledColor: @Composable (() -> Color)? = null,
        val enabled: Boolean = true,
        val tooltip: Tooltip.Arg? = null,
        val onClick: () -> Unit
    )

    data class TextButtonArg(
        val text: String,
        val color: @Composable () -> Color = { Theme.colors.icon },
        val enabled: Boolean = true,
        val tooltip: Tooltip.Arg? = null,
        val onClick: () -> Unit
    )

    interface State {
        fun cancel()
        fun isValid(): Boolean
        fun trySubmit()
        fun trySubmitIfValid() {
            if (isValid()) trySubmit()
        }
    }

    @Composable
    fun Field(label: String, fieldInput: @Composable () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value = label, modifier = LABEL_MODIFIER)
            Box(modifier = INPUT_MODIFIER) { fieldInput() }
        }
    }

    @Composable
    fun Submission(
        state: State,
        modifier: Modifier,
        showButtons: Boolean = true,
        submitLabel: String = Label.SUBMIT,
        content: @Composable (ColumnScope.() -> Unit)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(FIELD_SPACING),
            modifier = modifier.onKeyEvent {
                onKeyEvent(event = it, onEnter = { state.trySubmitIfValid() })
            }
        ) {
            content()
            if (showButtons) {
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(text = Label.CANCEL, onClick = { state.cancel() })
                    FormRowSpacer()
                    TextButton(text = submitLabel, onClick = { state.trySubmit() }, enabled = state.isValid())
                }
            }
        }
    }

    @Composable
    fun FormRowSpacer() {
        Spacer(modifier = Modifier.width(INNER_SPACING))
    }

    @Composable
    fun Text(
        value: String,
        textStyle: TextStyle = Theme.typography.body1,
        fontStyle: FontStyle? = null,
        fontWeight: FontWeight? = null,
        textDecoration: TextDecoration? = null,
        color: Color = Theme.colors.onPrimary,
        alpha: Float? = null,
        align: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = false,
        enabled: Boolean = true,
    ) {
        Text(
            AnnotatedString(value), textStyle, fontStyle, fontWeight, textDecoration,
            color, alpha, align, modifier, overflow, softWrap, enabled
        )
    }

    @Composable
    fun Text(
        value: AnnotatedString,
        textStyle: TextStyle = Theme.typography.body1,
        fontStyle: FontStyle? = null,
        fontWeight: FontWeight? = null,
        textDecoration: TextDecoration? = null,
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
            style = textStyle,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            textDecoration = textDecoration,
            color = fadeable(alpha?.let { color.copy(alpha = alpha) } ?: color, !enabled),
            modifier = modifier,
            overflow = overflow,
            softWrap = softWrap,
            textAlign = align,
        )
    }

    @Composable
    fun TextURL(url: URL, text: String? = null) {
        val uriHandler = LocalUriHandler.current
        TextClickable(
            text = text ?: url.toString(),
            onClick = { uriHandler.openUri(url.toString()) }
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun TextClickable(text: String, onClick: (Int) -> Unit) {
        ClickableText(
            text = AnnotatedString(text),
            modifier = Modifier.pointerHoverIcon(PointerIconDefaults.Hand),
            style = Theme.typography.body1.copy(color = Theme.colors.secondary),
            onClick = onClick
        )
    }

    @Composable
    fun LoadingIndicator(modifier: Modifier) {
        Box(modifier.size(FIELD_HEIGHT), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(LOADING_SPINNER_SIZE),
                color = Theme.colors.secondary,
                strokeWidth = LOADING_SPINNER_STROKE_WIDTH
            )
        }
    }

    @Composable
    fun TextButtonRow(height: Dp, bgColor: Color = Theme.colors.primary, buttons: List<TextButtonArg>) {
        @Composable
        fun TextButton(button: TextButtonArg, roundedCorners: RoundedCorners) {
            TextButton(
                text = button.text,
                onClick = button.onClick,
                modifier = Modifier.height(height),
                bgColor = bgColor,
                textColor = button.color(),
                roundedCorners = roundedCorners,
                tooltip = button.tooltip,
                enabled = button.enabled,
            )
        }

        buttons.forEachIndexed { i, button ->
            when (i) {
                0 -> TextButton(button, RoundedCorners.LEFT)
                buttons.size - 1 -> TextButton(button, RoundedCorners.RIGHT)
                else -> TextButton(button, RoundedCorners.NONE)
            }
        }
    }

    @Composable
    fun IconButtonRow(size: Dp, bgColor: Color = Theme.colors.primary, buttons: List<IconButtonArg>) {
        @Composable
        fun IconButton(button: IconButtonArg, roundedCorners: RoundedCorners) {
            IconButton(
                icon = button.icon,
                hoverIcon = button.hoverIcon,
                onClick = button.onClick,
                modifier = Modifier.size(size),
                bgColor = bgColor,
                iconColor = button.color(),
                iconHoverColor = button.hoverColor?.invoke(),
                disabledColor = button.disabledColor?.invoke(),
                roundedCorners = roundedCorners,
                enabled = true,
                tooltip = button.tooltip,
            )
        }

        buttons.forEachIndexed { i, button ->
            when (i) {
                0 -> IconButton(button, RoundedCorners.LEFT)
                buttons.size - 1 -> IconButton(button, RoundedCorners.RIGHT)
                else -> IconButton(button, RoundedCorners.NONE)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun TextButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        textColor: Color = Theme.colors.onPrimary,
        bgColor: Color = Theme.colors.primary,
        focusReq: FocusRequester? = null,
        leadingIcon: Icon.Code? = null,
        trailingIcon: Icon.Code? = null,
        iconColor: Color = Theme.colors.icon,
        roundedCorners: RoundedCorners = RoundedCorners.ALL,
        enabled: Boolean = true,
        tooltip: Tooltip.Arg? = null,
    ) {
        @Composable
        fun Spacer() = Spacer(Modifier.width(TEXT_BUTTON_PADDING))
        BoxButton(
            onClick = onClick, bgColor = bgColor, focusReq = focusReq,
            roundedCorners = roundedCorners, enabled = enabled, tooltip = tooltip
        ) {
            Row(modifier.height(FIELD_HEIGHT), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer()
                    leadingIcon?.let {
                        Box(Modifier.size(TRAILING_ICON_SIZE), Alignment.Center) { Icon.Render(it, iconColor) }
                        Spacer()
                    }
                    Text(text, textStyle = Theme.typography.body1, color = fadeable(textColor, !enabled))
                    Spacer()
                }
                trailingIcon?.let {
                    Row {
                        Box(Modifier.size(TRAILING_ICON_SIZE), Alignment.Center) { Icon.Render(it, iconColor) }
                        Spacer()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun RawIconButton(
        icon: Icon.Code,
        onClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier,
        iconColor: Color = Theme.colors.icon,
        enabled: Boolean = true,
        tooltip: Tooltip.Arg? = null,
    ) {
        val tooltipState: Tooltip.State? = remember { if (tooltip != null) Tooltip.State(tooltip) else null }
        val mod = onClick?.let {
            modifier.pointerHoverIcon(if (enabled) PointerIconDefaults.Hand else PointerIconDefaults.Default)
        } ?: modifier
        Box(
            contentAlignment = Alignment.Center,
            modifier = mod.height(FIELD_HEIGHT).onPointerEvent(Press) {
                if (it.buttons.isPrimaryPressed) {
                    tooltipState?.hideOnTargetHover()
                    onClick?.let { c -> c() }
                }
            }.pointerMoveFilter(
                onEnter = { tooltipState?.mayShowOnTargetHover(); false },
                onExit = { tooltipState?.mayHideOnTargetExit(); false }
            )
        ) {
            Icon.Render(icon = icon, color = iconColor, enabled = enabled)
            tooltipState?.let { Tooltip.Popup(it) }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun IconButton(
        icon: Icon.Code,
        hoverIcon: Icon.Code? = null,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        focusReq: FocusRequester? = null,
        iconColor: Color = Theme.colors.icon,
        iconHoverColor: Color? = null,
        disabledColor: Color? = null,
        bgColor: Color = Theme.colors.primary,
        roundedCorners: RoundedCorners = RoundedCorners.ALL,
        enabled: Boolean = true,
        tooltip: Tooltip.Arg? = null,
    ) {
        var isHover by remember { mutableStateOf(false) }
        BoxButton(
            onClick = onClick,
            bgColor = bgColor,
            roundedCorners = roundedCorners,
            enabled = enabled,
            tooltip = tooltip,
            focusReq = focusReq,
            modifier = modifier.size(FIELD_HEIGHT).pointerMoveFilter(
                onEnter = { isHover = true; false },
                onExit = { isHover = false; false }
            ),
        ) {
            Icon.Render(
                icon = if (hoverIcon != null && isHover) hoverIcon else icon,
                color = if (iconHoverColor != null && isHover) iconHoverColor else iconColor,
                disabledColor = disabledColor, enabled = enabled
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun BoxButton(
        onClick: () -> Unit,
        bgColor: Color = Theme.colors.primary,
        modifier: Modifier = Modifier,
        focusReq: FocusRequester? = null,
        roundedCorners: RoundedCorners = RoundedCorners.ALL,
        enabled: Boolean = true,
        tooltip: Tooltip.Arg? = null,
        content: @Composable BoxScope.() -> Unit
    ) {
        val density = LocalDensity.current.density
        val mod = if (focusReq != null) modifier.focusRequester(focusReq) else modifier
        val tooltipState: Tooltip.State? = remember { if (tooltip != null) Tooltip.State(tooltip) else null }
        val hoverIndication = rectangleIndication(Theme.colors.indicationBase, density, roundedCorners)
        CompositionLocalProvider(LocalIndication provides hoverIndication) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = mod.background(fadeable(bgColor, !enabled), roundedCorners.shape(density))
                    .clickable(enabled = enabled) { tooltipState?.hideOnTargetHover(); onClick() }
                    .pointerHoverIcon(icon = if (enabled) PointerIconDefaults.Hand else PointerIconDefaults.Default)
                    .pointerMoveFilter(
                        onEnter = { tooltipState?.mayShowOnTargetHover(); false },
                        onExit = { tooltipState?.mayHideOnTargetExit(); false }
                    )
            ) {
                content()
                tooltipState?.let { Tooltip.Popup(it) }
            }
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
                .background(color = fadeable(Theme.colors.surface, !enabled), ROUNDED_CORNER_SHAPE)
                .border(BORDER_WIDTH, SolidColor(fadeable(Theme.colors.border, !enabled)), ROUNDED_CORNER_SHAPE)
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
        shape: Shape? = ROUNDED_CORNER_SHAPE,
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
        internal var horScroller: ScrollState = ScrollState(0)
        private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

        fun reset() {
            boxWidth = 0.dp
        }

        internal fun updateValue(newValue: TextFieldValue) {
            val oldText = value.text
            value = newValue
            if (oldText == newValue.text) mayScrollHorizontally()
            // else, text have changed and updateLayout() will be called
        }

        internal fun updateLayout(newLayout: TextLayoutResult, value: TextFieldValue) {
            if (this.value != value) this.value = value
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
        focusReq: FocusRequester = FocusRequester(),
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
                            onTextLayout = { state.updateLayout(it, value); onTextLayout(it) },
                            cursorBrush = SolidColor(Theme.colors.secondary),
                            textStyle = Theme.typography.body1.copy(Theme.colors.onSurface),
                            modifier = Modifier.focusRequester(focusReq)
                                .defaultMinSize(minWidth = state.boxWidth - MULTILINE_INPUT_PADDING)
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
        selected: T?,
        displayFn: (T) -> String = { it.toString() },
        onExpand: (() -> Unit)? = null,
        onSelection: (value: T) -> Unit,
        placeholder: String = "",
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
        focusReq: FocusRequester? = null,
        tooltip: Tooltip.Arg? = null,
    ) {

        class DropdownState {
            var expanded by mutableStateOf(false)
            var isButtonHover by mutableStateOf(false)
            var mouseIndex: Int? by mutableStateOf(null)
            var width: Dp by mutableStateOf(0.dp)

            fun toggle() {
                expanded = !expanded
                if (expanded && onExpand != null) onExpand()
            }

            fun select(value: T) {
                onSelection(value); expanded = false
            }

            fun mouseOutFrom(index: Int) {
                if (mouseIndex == index) mouseIndex = null
            }

            fun mouseInTo(index: Int) {
                mouseIndex = index
            }
        }

        val pixelDensity = LocalDensity.current.density
        val state = remember { DropdownState() }
        Box {
            TextButton(
                text = selected?.let { displayFn(it).ifBlank { placeholder } } ?: placeholder,
                onClick = { state.toggle() },
                textColor = Theme.colors.onPrimary,
                trailingIcon = CARET_DOWN,
                focusReq = focusReq,
                enabled = enabled,
                tooltip = tooltip,
                modifier = modifier.onSizeChanged { state.width = toDP(it.width, pixelDensity) }.pointerMoveFilter(
                    onEnter = { state.isButtonHover = true; false },
                    onExit = { state.isButtonHover = false; false }
                ),
            )
            DropdownMenu(
                expanded = state.expanded,
                onDismissRequest = { if (!state.isButtonHover) state.expanded = false },
                modifier = Modifier.background(Theme.colors.surface)
                    .defaultMinSize(minWidth = state.width)
                    .border(BORDER_WIDTH, Theme.colors.border, ROUNDED_CORNER_SHAPE) // TODO: how to make not rounded?
            ) {
                val padding = PaddingValues(horizontal = 0.dp)
                val itemModifier = Modifier.height(FIELD_HEIGHT)
                if (values.isEmpty()) DropdownMenuItem({}, itemModifier.background(Theme.colors.surface)) {
                    Row {
                        Spacer(Modifier.width(TEXT_BUTTON_PADDING))
                        Text(value = "(${Label.NONE})")
                    }
                } else values.forEachIndexed { i, value ->
                    val color = if (value == selected) Theme.colors.secondary else Theme.colors.onSurface
                    DropdownMenuItem(
                        onClick = { state.select(value) }, contentPadding = padding,
                        modifier = itemModifier
                            .background(if (i == state.mouseIndex) Theme.colors.primary else Theme.colors.surface)
                            .pointerHoverIcon(icon = PointerIconDefaults.Hand)
                            .pointerMoveFilter(
                                onExit = { state.mouseOutFrom(i); false },
                                onEnter = { state.mouseInTo(i); false }
                            ),
                    ) {
                        Row {
                            Spacer(Modifier.width(TEXT_BUTTON_PADDING))
                            Text(value = displayFn(value), color = color)
                        }
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