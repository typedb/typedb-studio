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

package com.vaticle.typedb.studio.view.material

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
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.view.common.Context.LocalTitleBarHeight
import com.vaticle.typedb.studio.view.common.Context.LocalWindow
import com.vaticle.typedb.studio.view.common.Util.isMouseHover
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.Util.toRectDP
import com.vaticle.typedb.studio.view.common.theme.Color.fadeable
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.ROUNDED_CORNER_SHAPE
import com.vaticle.typedb.studio.view.common.theme.Theme.RoundedCorners
import com.vaticle.typedb.studio.view.common.theme.Theme.rectangleIndication
import com.vaticle.typedb.studio.view.material.Icon.Code.CARET_DOWN
import java.awt.event.KeyEvent.KEY_PRESSED
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Form {

    val FIELD_HEIGHT = 28.dp
    val BORDER_WIDTH = 1.dp
    val DEFAULT_BORDER = Border(BORDER_WIDTH, ROUNDED_CORNER_SHAPE)
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

    private val RowScope.LABEL_MODIFIER: Modifier get() = Modifier.weight(LABEL_WEIGHT)
    private val RowScope.INPUT_MODIFIER: Modifier get() = Modifier.weight(INPUT_WEIGHT).height(FIELD_HEIGHT)

    data class Border(
        val width: Dp,
        val shape: Shape,
        val color: @Composable () -> Color = { Theme.studio.border }
    )

    data class IconArg(val code: Icon.Code, val color: @Composable () -> Color = { Theme.studio.icon })

    data class IconButtonArg(
        val icon: Icon.Code,
        val hoverIcon: Icon.Code? = null,
        val color: @Composable () -> Color = { Theme.studio.icon },
        val hoverColor: @Composable (() -> Color)? = null,
        val disabledColor: @Composable (() -> Color)? = null,
        val enabled: Boolean = true,
        val tooltip: Tooltip.Arg? = null,
        val onClick: () -> Unit
    )

    data class TextButtonArg(
        val text: String,
        val color: @Composable () -> Color = { Theme.studio.icon },
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
        modifier: Modifier = Modifier,
        showButtons: Boolean = true,
        submitLabel: String = Label.SUBMIT,
        content: @Composable (ColumnScope.() -> Unit)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(FIELD_SPACING),
            modifier = modifier.onKeyEvent {
                onKeyEventHandler(event = it, onEnter = { state.trySubmitIfValid() })
            }
        ) {
            content()
            if (showButtons) {
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(text = Label.CANCEL) { state.cancel() }
                    FormRowSpacer()
                    TextButton(text = submitLabel, enabled = state.isValid()) { state.trySubmit() }
                }
            }
        }
    }

    @Composable
    fun FormRowSpacer() {
        Spacer(modifier = Modifier.width(INNER_SPACING))
    }

    @Composable
    fun ButtonSpacer() = Spacer(Modifier.width(TEXT_BUTTON_PADDING))

    @Composable
    fun Text(
        value: String,
        textStyle: TextStyle = Theme.typography.body1,
        fontStyle: FontStyle? = null,
        fontWeight: FontWeight? = null,
        textDecoration: TextDecoration? = null,
        color: Color = Theme.studio.onPrimary,
        alpha: Float? = null,
        align: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = false,
        enabled: Boolean = true,
        onTextLayout: (TextLayoutResult) -> Unit = {},
    ) {
        Text(
            AnnotatedString(value), textStyle, fontStyle, fontWeight, textDecoration,
            color, alpha, align, modifier, overflow, softWrap, enabled, onTextLayout
        )
    }

    @Composable
    fun Text(
        value: AnnotatedString,
        textStyle: TextStyle = Theme.typography.body1,
        fontStyle: FontStyle? = null,
        fontWeight: FontWeight? = null,
        textDecoration: TextDecoration? = null,
        color: Color = Theme.studio.onPrimary,
        alpha: Float? = null,
        align: TextAlign = TextAlign.Start,
        modifier: Modifier = Modifier,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = false,
        enabled: Boolean = true,
        onTextLayout: (TextLayoutResult) -> Unit = {},
    ) {
        androidx.compose.material.Text(
            text = value,
            style = textStyle,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            textDecoration = textDecoration,
            color = fadeable(alpha?.let { color.copy(alpha = it) } ?: color, !enabled),
            modifier = modifier,
            overflow = overflow,
            softWrap = softWrap,
            textAlign = align,
            onTextLayout = onTextLayout
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun SelectableText(
        value: String,
        style: TextStyle = Theme.typography.body1,
        color: Color = Theme.studio.onSurface,
        singleLine: Boolean = false,
        modifier: Modifier = Modifier,
        onTextLayout: (TextLayoutResult) -> Unit = {},
    ) {
        BasicTextField(
            modifier = modifier.pointerHoverIcon(icon = PointerIconDefaults.Hand),
            value = value,
            onValueChange = {},
            readOnly = true,
            cursorBrush = SolidColor(Theme.studio.secondary),
            singleLine = singleLine,
            textStyle = style.copy(color = color),
            onTextLayout = onTextLayout
        )
    }

    @Composable
    fun URLText(url: URL, text: String? = null) {
        val uriHandler = LocalUriHandler.current
        ClickableText(
            value = text ?: url.toString(),
            color = Theme.studio.secondary,
            onClick = { uriHandler.openUri(url.toString()) }
        )
    }

    @Composable
    fun ClickableText(
        value: String,
        color: Color = Theme.studio.onPrimary,
        hoverColor: Color = Theme.studio.secondary,
        onClick: (Int) -> Unit
    ) { ClickableText(AnnotatedString(value), color, hoverColor, onClick) }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun ClickableText(
        value: AnnotatedString,
        color: Color = Theme.studio.onPrimary,
        hoverColor: Color = Theme.studio.secondary,
        onClick: (Int) -> Unit
    ) {
        var isHover by remember { mutableStateOf(false) }
        ClickableText(
            text = value,
            modifier = Modifier.pointerHoverIcon(PointerIconDefaults.Hand).pointerMoveFilter(
                onEnter = { isHover = true; false },
                onExit = { isHover = false; false }
            ),
            style = Theme.typography.body1.copy(if (isHover) hoverColor else color),
            onClick = onClick
        )
    }

    @Composable
    fun TextBox(
        text: String,
        modifier: Modifier = Modifier,
        textColor: Color = Theme.studio.onPrimary,
        bgColor: Color = Theme.studio.primary,
        trailingIcon: IconArg? = null,
        leadingIcon: IconArg? = null,
        roundedCorners: RoundedCorners = RoundedCorners.ALL
    ) { TextBox(AnnotatedString(text), modifier, textColor, bgColor, trailingIcon, leadingIcon, roundedCorners) }

    @Composable
    fun TextBox(
        text: AnnotatedString,
        modifier: Modifier = Modifier,
        textColor: Color = Theme.studio.onPrimary,
        bgColor: Color = Theme.studio.primary,
        trailingIcon: IconArg? = null,
        leadingIcon: IconArg? = null,
        roundedCorners: RoundedCorners = RoundedCorners.ALL
    ) {
        val density = LocalDensity.current.density
        Row(
            modifier = modifier.height(FIELD_HEIGHT).background(bgColor, roundedCorners.shape(density)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                ButtonSpacer()
                Box(Modifier.size(TRAILING_ICON_SIZE), Alignment.Center) { Icon.Render(it.code, it.color()) }
            }
            ButtonSpacer()
            Text(text, textStyle = Theme.typography.body1, color = textColor)
            ButtonSpacer()
            trailingIcon?.let {
                Box(Modifier.size(TRAILING_ICON_SIZE), Alignment.Center) { Icon.Render(it.code, it.color()) }
                ButtonSpacer()
            }
        }
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
        fontColor: Color = Theme.studio.onSurface,
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
            modifier = mod.height(FIELD_HEIGHT).pointerHoverIcon(pointerHoverIcon)
                .background(fadeable(Theme.studio.surface, !enabled), shape ?: RectangleShape),
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = singleLine,
            enabled = enabled,
            cursorBrush = SolidColor(Theme.studio.secondary),
            textStyle = textStyle.copy(color = fadeable(fontColor, !enabled)),
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
                        if (value.isEmpty()) Text(
                            value = Label.E_G_ + " " + placeholder,
                            textStyle = textStyle.copy(fontStyle = FontStyle.Italic),
                            color = fadeable(fontColor, true)
                        )
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
        internal var horScroller = ScrollState(0)
        private val coroutineScope = CoroutineScope(Dispatchers.Default)

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
        focusReq: FocusRequester = remember { FocusRequester() },
        onValueChange: (TextFieldValue) -> Unit,
        onTextLayout: (TextLayoutResult) -> Unit
    ) {
        val density = LocalDensity.current.density
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier.fillMaxWidth()
                .background(Theme.studio.surface)
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
                            cursorBrush = SolidColor(Theme.studio.secondary),
                            textStyle = Theme.typography.body1.copy(Theme.studio.onSurface),
                            modifier = Modifier.focusRequester(focusReq)
                                .defaultMinSize(minWidth = state.boxWidth - MULTILINE_INPUT_PADDING)
                        )
                        Spacer(Modifier.width(MULTILINE_INPUT_PADDING))
                    }
                }
            }
        }
    }

    @Composable
    fun LoadingIndicator(modifier: Modifier) {
        Box(modifier.size(FIELD_HEIGHT), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(LOADING_SPINNER_SIZE),
                color = Theme.studio.secondary,
                strokeWidth = LOADING_SPINNER_STROKE_WIDTH
            )
        }
    }

    @Composable
    fun toggleButtonColor(isActive: Boolean): Color {
        return if (isActive) Theme.studio.secondary else Theme.studio.onPrimary
    }

    @Composable
    fun TextButtonRow(
        buttons: List<TextButtonArg>,
        height: Dp = FIELD_HEIGHT,
        bgColor: Color = Theme.studio.primary
    ) {
        @Composable
        fun TextButton(button: TextButtonArg, roundedCorners: RoundedCorners) {
            TextButton(
                text = button.text,
                modifier = Modifier.height(height),
                textColor = button.color(),
                bgColor = bgColor,
                roundedCorners = roundedCorners,
                enabled = button.enabled,
                tooltip = button.tooltip,
                onClick = button.onClick,
            )
        }

        Row {
            buttons.forEachIndexed { i, button ->
                when (i) {
                    0 -> TextButton(button, RoundedCorners.LEFT)
                    buttons.size - 1 -> TextButton(button, RoundedCorners.RIGHT)
                    else -> TextButton(button, RoundedCorners.NONE)
                }
            }
        }
    }

    @Composable
    fun IconButtonRow(size: Dp, bgColor: Color = Theme.studio.primary, buttons: List<IconButtonArg>) {
        @Composable
        fun IconButton(button: IconButtonArg, roundedCorners: RoundedCorners) {
            IconButton(
                icon = button.icon,
                hoverIcon = button.hoverIcon,
                modifier = Modifier.size(size),
                iconColor = button.color(),
                iconHoverColor = button.hoverColor?.invoke(),
                disabledColor = button.disabledColor?.invoke(),
                bgColor = bgColor,
                roundedCorners = roundedCorners,
                enabled = true,
                tooltip = button.tooltip,
                onClick = button.onClick,
            )
        }

        Row {
            buttons.forEachIndexed { i, button ->
                when (i) {
                    0 -> IconButton(button, RoundedCorners.LEFT)
                    buttons.size - 1 -> IconButton(button, RoundedCorners.RIGHT)
                    else -> IconButton(button, RoundedCorners.NONE)
                }
            }
        }
    }

    @Composable
    fun TextButton(
        text: String,
        modifier: Modifier = Modifier,
        textColor: Color = Theme.studio.onPrimary,
        bgColor: Color = Theme.studio.primary,
        focusReq: FocusRequester? = null,
        leadingIcon: IconArg? = null,
        trailingIcon: IconArg? = null,
        roundedCorners: RoundedCorners = RoundedCorners.ALL,
        enabled: Boolean = true,
        tooltip: Tooltip.Arg? = null,
        onClick: () -> Unit,
    ) {
        TextButton(
            AnnotatedString(text), modifier, textColor, bgColor, focusReq, leadingIcon, trailingIcon,
            roundedCorners, enabled, tooltip, onClick
        )
    }

    @Composable
    fun TextButton(
        text: AnnotatedString,
        modifier: Modifier = Modifier,
        textColor: Color = Theme.studio.onPrimary,
        bgColor: Color = Theme.studio.primary,
        focusReq: FocusRequester? = null,
        leadingIcon: IconArg? = null,
        trailingIcon: IconArg? = null,
        roundedCorners: RoundedCorners = RoundedCorners.ALL,
        enabled: Boolean = true,
        tooltip: Tooltip.Arg? = null,
        onClick: () -> Unit,
    ) {
        BoxButton(
            bgColor = bgColor, focusReq = focusReq, roundedCorners = roundedCorners,
            enabled = enabled, tooltip = tooltip, onClick = onClick
        ) {
            Row(modifier.height(FIELD_HEIGHT), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    leadingIcon?.let {
                        ButtonSpacer()
                        Box(Modifier.size(TRAILING_ICON_SIZE), Alignment.Center) {
                            Icon.Render(it.code, fadeable(it.color(), !enabled))
                        }
                    }
                    ButtonSpacer()
                    Text(text, textStyle = Theme.typography.body1, color = fadeable(textColor, !enabled))
                    ButtonSpacer()
                }
                trailingIcon?.let {
                    Row {
                        Box(Modifier.size(TRAILING_ICON_SIZE), Alignment.Center) {
                            Icon.Render(it.code, fadeable(it.color(), !enabled))
                        }
                        ButtonSpacer()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun RawIconButton(
        icon: Icon.Code,
        modifier: Modifier = Modifier,
        iconColor: Color = Theme.studio.icon,
        enabled: Boolean = true,
        tooltip: Tooltip.Arg? = null,
        onClick: (() -> Unit)? = null,
    ) {
        val density = LocalDensity.current.density
        val tooltipState: Tooltip.State? = remember { if (tooltip != null) Tooltip.State(tooltip) else null }
        var area: Rect? by remember { mutableStateOf(null) }
        val window = LocalWindow.current!!
        val titleBarHeight = LocalTitleBarHeight.current
        val mod = onClick?.let {
            modifier.pointerHoverIcon(if (enabled) PointerIconDefaults.Hand else PointerIconDefaults.Default)
        } ?: modifier

        fun mayShowOnTargetHover() {
            if (area?.let { isMouseHover(it, window, titleBarHeight) } == true) tooltipState?.mayShowOnTargetHover()
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = mod.height(FIELD_HEIGHT)
                .onGloballyPositioned { area = toRectDP(it.boundsInWindow(), density) }
                .onPointerEvent(Press) {
                    if (it.buttons.isPrimaryPressed) {
                        tooltipState?.hideOnTargetClicked()
                        onClick?.let { c -> c() }
                    }
                }.pointerMoveFilter(
                    onEnter = { mayShowOnTargetHover(); false },
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
        modifier: Modifier = Modifier,
        focusReq: FocusRequester? = null,
        iconColor: Color = Theme.studio.icon,
        iconHoverColor: Color? = null,
        disabledColor: Color? = null,
        bgColor: Color = Theme.studio.primary,
        roundedCorners: RoundedCorners = RoundedCorners.ALL,
        enabled: Boolean = true,
        tooltip: Tooltip.Arg? = null,
        onClick: () -> Unit,
    ) {
        var isHover by remember { mutableStateOf(false) }
        BoxButton(
            modifier = modifier.size(FIELD_HEIGHT).pointerMoveFilter(
                onEnter = { isHover = true; false },
                onExit = { isHover = false; false }
            ),
            bgColor = bgColor, focusReq = focusReq, roundedCorners = roundedCorners,
            enabled = enabled, tooltip = tooltip, onClick = onClick,
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
        bgColor: Color = Theme.studio.primary,
        modifier: Modifier = Modifier,
        focusReq: FocusRequester? = null,
        roundedCorners: RoundedCorners = RoundedCorners.ALL,
        enabled: Boolean = true,
        tooltip: Tooltip.Arg? = null,
        onClick: () -> Unit,
        content: @Composable (BoxScope.() -> Unit)
    ) {
        val density = LocalDensity.current.density
        val mod = if (focusReq != null) modifier.focusRequester(focusReq) else modifier
        val tooltipState: Tooltip.State? = remember { if (tooltip != null) Tooltip.State(tooltip) else null }
        val hoverIndication = rectangleIndication(Theme.studio.indicationBase, density, roundedCorners)
        var area: Rect? by remember { mutableStateOf(null) }
        val window = LocalWindow.current!!
        val titleBarHeight = LocalTitleBarHeight.current

        fun mayShowOnTargetHover() {
            if (area?.let { isMouseHover(it, window, titleBarHeight) } == true) tooltipState?.mayShowOnTargetHover()
        }

        CompositionLocalProvider(LocalIndication provides hoverIndication) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = mod.background(fadeable(bgColor, !enabled), roundedCorners.shape(density))
                    .onGloballyPositioned { area = toRectDP(it.boundsInWindow(), density) }
                    .clickable(enabled = enabled) { tooltipState?.hideOnTargetClicked(); onClick() }
                    .pointerHoverIcon(icon = if (enabled) PointerIconDefaults.Hand else PointerIconDefaults.Default)
                    .pointerMoveFilter(
                        onEnter = { mayShowOnTargetHover(); false },
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
        modifier: Modifier = Modifier,
        size: Dp = FIELD_HEIGHT,
        enabled: Boolean = true,
        onChange: ((Boolean) -> Unit)? = null
    ) {
        fun Modifier.mayRegisterOnKeyEvent(): Modifier {
            onChange?.let { fn -> this.onKeyEvent { onKeyEventHandler(event = it, onSpace = { fn(!value) }) } }
            return this
        }

        Checkbox(
            checked = value,
            onCheckedChange = onChange,
            modifier = modifier.size(size)
                .background(color = fadeable(Theme.studio.surface, !enabled), ROUNDED_CORNER_SHAPE)
                .border(BORDER_WIDTH, SolidColor(fadeable(Theme.studio.border, !enabled)), ROUNDED_CORNER_SHAPE)
                .mayRegisterOnKeyEvent(),
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = fadeable(Theme.studio.icon, !enabled),
                uncheckedColor = fadeable(Theme.studio.surface, !enabled),
                disabledColor = fadeable(Theme.studio.surface, !enabled)
            )
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun <T : Any> Dropdown(
        values: List<T>,
        selected: T?,
        displayFn: @Composable (T) -> AnnotatedString = { AnnotatedString(it.toString()) },
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
        val placeholderAnnStr = AnnotatedString(placeholder)
        val itemPadding = PaddingValues(horizontal = TEXT_BUTTON_PADDING)
        Box {
            TextButton(
                text = selected?.let { displayFn(it).ifBlank { placeholderAnnStr } } ?: placeholderAnnStr,
                modifier = modifier.onSizeChanged { state.width = toDP(it.width, pixelDensity) }.pointerMoveFilter(
                    onEnter = { state.isButtonHover = true; false },
                    onExit = { state.isButtonHover = false; false }
                ),
                textColor = Theme.studio.onPrimary,
                focusReq = focusReq,
                trailingIcon = IconArg(CARET_DOWN),
                enabled = enabled,
                tooltip = tooltip,
            ) { state.toggle() }
            DropdownMenu(
                expanded = state.expanded,
                onDismissRequest = { if (!state.isButtonHover) state.expanded = false },
                modifier = Modifier.background(Theme.studio.surface)
                    .defaultMinSize(minWidth = state.width)
                    .border(BORDER_WIDTH, Theme.studio.border, ROUNDED_CORNER_SHAPE) // TODO: how to make not rounded?
            ) {
                val itemModifier = Modifier.height(FIELD_HEIGHT)
                if (values.isEmpty()) DropdownMenuItem({}, itemModifier.background(Theme.studio.surface)) {
                    Row { Text(value = "(${Label.NONE})") }
                } else values.forEachIndexed { i, value ->
                    val color = if (value == selected) Theme.studio.secondary else Theme.studio.onSurface
                    DropdownMenuItem(
                        onClick = { state.select(value) }, contentPadding = itemPadding,
                        modifier = itemModifier
                            .background(if (i == state.mouseIndex) Theme.studio.primary else Theme.studio.surface)
                            .pointerHoverIcon(icon = PointerIconDefaults.Hand)
                            .pointerMoveFilter(
                                onExit = { state.mouseOutFrom(i); false },
                                onEnter = { state.mouseInTo(i); false }
                            ),
                    ) { Row { Text(value = displayFn(value), color = color) } }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun onKeyEventHandler(
        event: KeyEvent,
        enabled: Boolean = true,
        onEnter: (() -> Unit)? = null,
        onSpace: (() -> Unit)? = null
    ): Boolean {
        return when {
            event.awtEvent.id == KEY_PRESSED -> false
            !enabled -> false
            else -> when (event.key) {
                Key.Enter, Key.NumPadEnter -> onEnter?.let { it(); true } ?: false
                Key.Spacebar -> onSpace?.let { it(); true } ?: false
                else -> false
            }
        }
    }
}