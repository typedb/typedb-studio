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

package com.vaticle.typedb.studio.view.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.RECTANGLE_ROUNDED_ALL
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP

object TextToolbar {

    private val INPUT_MAX_WIDTH = 740.dp
    private val INPUT_MIN_WIDTH = 300.dp
    private val INPUT_VERTICAL_PADDING = 4.dp
    private val INPUT_RIGHT_PADDING = 4.dp
    private val INPUT_MIN_HEIGHT = 28.dp
    private val INPUT_MAX_HEIGHT = 120.dp
    private val BUTTON_AREA_WIDTH = 220.dp
    private val BUTTON_HEIGHT = 23.dp
    private val BUTTON_SPACING = 4.dp

    internal class State(private val target: InputTarget, val finder: TextFinder) {

        internal var lineHeight by mutableStateOf(0.dp)
        internal var showFinder by mutableStateOf(false)
        internal var showReplacer by mutableStateOf(false)
        internal var findText by mutableStateOf("")
        internal var findTextLayout: TextLayoutResult? by mutableStateOf(null)
        internal var replaceText by mutableStateOf("")
        internal var replaceTextLayout: TextLayoutResult? by mutableStateOf(null)
        internal var isRegex by mutableStateOf(false)
        internal var isWord by mutableStateOf(false)
        internal var isCaseSensitive by mutableStateOf(false)
        internal val density: Float get() = target.density

        internal fun showFinder() {
            showFinder = true
            showReplacer = false
            if (target.selection != null) findText = target.selectedText()
        }

        internal fun showReplacer() {
            showReplacer = true
            if (target.selection != null) findText = target.selectedText()
        }

        internal fun toolBarHeight(): Dp {
            var height = Separator.WEIGHT + finderInputHeight()
            if (showReplacer) height += replacerInputHeight()
            return height
        }

        private fun textInputWidth(): Dp {
            val findTextWidth = findTextLayout?.let { toDP(it.multiParagraph.width, density) } ?: 0.dp
            val replaceTextWidth = replaceTextLayout?.let { toDP(it.multiParagraph.width, density) } ?: 0.dp
            return max(max(findTextWidth, replaceTextWidth), INPUT_MIN_WIDTH)
        }

        internal fun toolbarMaxWidth(): Dp {
            val otherWidth = INPUT_MIN_HEIGHT + INPUT_RIGHT_PADDING + BUTTON_AREA_WIDTH
            return otherWidth + textInputWidth().coerceIn(INPUT_MIN_WIDTH, INPUT_MAX_WIDTH)
        }

        internal fun finderInputHeight(): Dp {
            val height = lineHeight * findText.split("\n").size + INPUT_VERTICAL_PADDING * 2
            return height.coerceIn(INPUT_MIN_HEIGHT, INPUT_MAX_HEIGHT)
        }

        internal fun replacerInputHeight(): Dp {
            val height = lineHeight * replaceText.split("\n").size + INPUT_VERTICAL_PADDING * 2
            return height.coerceIn(INPUT_MIN_HEIGHT, INPUT_MAX_HEIGHT)
        }

        internal fun toggleCaseSensitive() {
            isCaseSensitive = !isCaseSensitive
        }

        internal fun toggleWord() {
            isWord = !isWord
            if (isWord) isRegex = false
        }

        internal fun toggleRegex() {
            isRegex = !isRegex
            if (isRegex) isWord = false
        }

        internal fun findText(text: String) {
            findText = text
            if (isRegex) finder.findRegex(Regex(findText), isCaseSensitive)
            else if (isWord) finder.findRegex(Regex("\b$findText\b"), isCaseSensitive)
            else finder.findText(text, isCaseSensitive)
        }

        internal fun findNext() {
            finder.findNext()
        }

        internal fun findPrevious() {
            finder.findPrevious()
        }

        internal fun replaceNext() {
            finder.replaceNext(findText)
        }

        internal fun replaceAll() {
            finder.replaceAll(replaceText)
        }
    }

    @Composable
    internal fun Area(state: State) {
        Box {
            // We render a character to find out the default height of a line for the given font
            // TODO: use FinderTextInput TextLayoutResult after: https://github.com/JetBrains/compose-jb/issues/1781
            Text(
                text = "0", style = Theme.typography.body1,
                onTextLayout = { state.lineHeight = Theme.toDP(it.size.height, state.density) }
            )
            // TODO: figure out how to set min width to MIN_WIDTH
            Row(modifier = Modifier.widthIn(max = state.toolbarMaxWidth()).height(state.toolBarHeight())) {
                Column(Modifier.weight(1f)) {
                    FinderTextInput(state)
                    if (state.showReplacer) {
                        Separator.Horizontal()
                        ReplacerTextInput(state)
                    }
                }
                Separator.Vertical()
                Column(Modifier.width(BUTTON_AREA_WIDTH)) {
                    FinderButtons(state)
                    Separator.Horizontal(height = 3.dp, color = Color.Transparent)
                    ReplacerButtons(state)
                }
            }
        }
        Separator.Horizontal()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun FinderTextInput(state: State) {
        TextInput(
            value = state.findText,
            modifier = Modifier.height(state.finderInputHeight()),
            onValueChange = { state.findText(it) },
            onTextLayout = { state.findTextLayout = it },
            icon = Icon.Code.MAGNIFYING_GLASS,
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ReplacerTextInput(state: State) {
        TextInput(
            value = state.replaceText,
            modifier = Modifier.height(state.replacerInputHeight()),
            onValueChange = { state.replaceText = it },
            onTextLayout = { state.replaceTextLayout = it },
            icon = Icon.Code.RIGHT_LEFT,
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun TextInput(
        value: String,
        modifier: Modifier,
        onValueChange: (String) -> Unit,
        onTextLayout: (TextLayoutResult) -> Unit,
        icon: Icon.Code
    ) {
        val focusReq = FocusRequester()
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier.fillMaxWidth()
                .background(Theme.colors.surface)
                .onPointerEvent(Press) { focusReq.requestFocus() }
        ) {
            Box(Modifier.size(INPUT_MIN_HEIGHT)) { Icon.Render(icon, modifier = Modifier.align(Alignment.Center)) }
            Box(modifier = Modifier.fillMaxHeight().weight(1f)
                .padding(vertical = INPUT_VERTICAL_PADDING)
                .horizontalScroll(rememberScrollState())) {
                Row(Modifier.align(alignment = Alignment.CenterStart)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        onTextLayout = onTextLayout,
                        cursorBrush = SolidColor(Theme.colors.secondary),
                        textStyle = Theme.typography.body1.copy(Theme.colors.onSurface),
                        modifier = Modifier.focusRequester(focusReq).defaultMinSize(minWidth = INPUT_MIN_WIDTH)
                    )
                    Spacer(Modifier.width(INPUT_RIGHT_PADDING))
                }
            }
        }
    }

    @Composable
    private fun FinderButtons(state: State) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.height(INPUT_MIN_HEIGHT - 1.dp)) {
            Spacer(Modifier.width(BUTTON_SPACING))
            FinderNextPreviousButtons(state)
            Spacer(Modifier.width(BUTTON_SPACING))
            FinderParameterButtons(state)
            Spacer(Modifier.width(BUTTON_SPACING))
            FinderStatus(state, Modifier.height(BUTTON_HEIGHT).weight(1f))
        }
    }

    @Composable
    private fun FinderNextPreviousButtons(state: State) {
        Row(Modifier.background(Theme.colors.primary, RECTANGLE_ROUNDED_ALL)) {
            FinderButton(Icon.Code.CHEVRON_DOWN, { state.findNext() })
            FinderButton(Icon.Code.CHEVRON_UP, { state.findPrevious() })
        }
    }

    @Composable
    private fun FinderParameterButtons(state: State) {
        Row(Modifier.background(Theme.colors.primary, RECTANGLE_ROUNDED_ALL)) {
            FinderButton(Icon.Code.FONT_CASE, { state.toggleCaseSensitive() }, state.isCaseSensitive)
            FinderButton(Icon.Code.LETTER_W, { state.toggleWord() }, state.isWord)
            FinderButton(Icon.Code.ASTERISK, { state.toggleRegex() }, state.isRegex)
        }
    }

    @Composable
    private fun FinderStatus(state: State, modifier: Modifier) {
        Form.Text(
            value = state.finder.status,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }

    @Composable
    private fun FinderButton(icon: Icon.Code, onClick: () -> Unit, isActive: Boolean = false) {
        Form.IconButton(
            icon = icon,
            onClick = onClick,
            modifier = Modifier.size(BUTTON_HEIGHT),
            iconColor = if (isActive) Theme.colors.secondary else Theme.colors.icon
        )
    }

    @Composable
    private fun ReplacerButtons(state: State) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.height(INPUT_MIN_HEIGHT - 1.dp)) {
            Spacer(Modifier.width(BUTTON_SPACING))
            ReplacerButton(Label.REPLACE) { state.replaceNext() }
            Spacer(Modifier.width(BUTTON_SPACING))
            ReplacerButton(Label.REPLACE_ALL) { state.replaceAll() }
        }
    }

    @Composable
    private fun ReplacerButton(text: String, onClick: () -> Unit) {
        Form.TextButton(
            text = text,
            onClick = onClick,
            modifier = Modifier.height(BUTTON_HEIGHT)
        )
    }
}