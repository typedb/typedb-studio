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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.TextInput
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP

object TextToolbar {

    private val INPUT_MAX_WIDTH = 740.dp
    private val INPUT_MIN_WIDTH = 300.dp
    private val INPUT_MIN_HEIGHT = 28.dp
    private val INPUT_MAX_HEIGHT = 120.dp
    private val BUTTON_AREA_WIDTH = 160.dp
    private val BUTTON_HEIGHT = 24.dp
    private val BUTTON_SPACING = 4.dp

    internal class State(private val target: InputTarget, val finder: TextFinder) {

        internal var lineHeight by mutableStateOf(0.dp)
        internal var showFinder by mutableStateOf(false)
        internal var showReplacer by mutableStateOf(false)
        internal var findText by mutableStateOf("")
        internal var findTextLayout: TextLayoutResult? by mutableStateOf(null)
        internal var findTextInputWidth by mutableStateOf(0.dp)
        internal var replaceText by mutableStateOf("")
        internal val isRegex by mutableStateOf(false)
        internal val isWord by mutableStateOf(false)
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

        internal fun toolbarMaxWidth(): Dp {
            val findTextWidth = findTextLayout?.let { toDP(it.size.width, this.density) } ?: 0.dp
            return BUTTON_AREA_WIDTH + findTextWidth.coerceIn(INPUT_MIN_WIDTH, INPUT_MAX_WIDTH)
        }

        internal fun finderInputHeight(): Dp {
            return (lineHeight * findText.split("\n").size).coerceIn(INPUT_MIN_HEIGHT, INPUT_MAX_HEIGHT)
        }

        internal fun replacerInputHeight(): Dp {
            return (lineHeight * replaceText.split("\n").size).coerceIn(INPUT_MIN_HEIGHT, INPUT_MAX_HEIGHT)
        }

        internal fun toggleCaseSensitive() {
            isCaseSensitive = !isCaseSensitive
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
                Buttons(state)
            }
        }
        Separator.Horizontal()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun FinderTextInput(state: State) {
        TextInput(
            value = state.findText,
            placeholder = Label.FIND,
            onValueChange = { state.findText(it) },
            leadingIcon = Icon.Code.MAGNIFYING_GLASS,
            onTextLayout = { state.findTextLayout = it },
            singleLine = false,
            shape = null,
            border = null,
            modifier = Modifier.height(state.finderInputHeight())
                .onSizeChanged { state.findTextInputWidth = toDP(it.width, state.density) },
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ReplacerTextInput(state: State) {
        TextInput(
            value = state.replaceText,
            placeholder = Label.REPLACE,
            onValueChange = { state.replaceText = it },
            leadingIcon = Icon.Code.RIGHT_LEFT,
            singleLine = false,
            shape = null,
            border = null,
            modifier = Modifier.height(state.replacerInputHeight()),
        )
    }

    @Composable
    private fun Buttons(state: State) {
        Row(modifier = Modifier.offset(x = -INPUT_MIN_HEIGHT - Separator.WEIGHT)) {
            FinderToggles(state)
            Separator.Vertical()
            Column(Modifier.width(BUTTON_AREA_WIDTH)) {
                FinderButtons(state)
                ReplacerButtons(state)
            }
        }
    }

    @Composable
    private fun FinderToggles(state: State) {
        Form.IconButton(
            Icon.Code.FONT_CASE,
            onClick = { state.toggleCaseSensitive() },
            modifier = Modifier.size(INPUT_MIN_HEIGHT),
            iconColor = if (state.isCaseSensitive) Theme.colors.secondary else Theme.colors.icon,
            bgColor = Theme.colors.surface,
            rounded = false
        )
    }

    @Composable
    private fun FinderButtons(state: State) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(INPUT_MIN_HEIGHT)
        ) {
            Spacer(Modifier.width(BUTTON_SPACING))
            FindNextButton(state)
            FindPreviousButton(state)
            Spacer(Modifier.width(BUTTON_SPACING))
            FinderStatus(state, Modifier.weight(1f))
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
    private fun FindNextButton(state: State) {
        Form.IconButton(
            icon = Icon.Code.CHEVRON_DOWN,
            onClick = { state.findNext() },
            modifier = Modifier.size(INPUT_MIN_HEIGHT),
            bgColor = Color.Transparent,
            rounded = false
        )
    }

    @Composable
    private fun FindPreviousButton(state: State) {
        Form.IconButton(
            icon = Icon.Code.CHEVRON_UP,
            onClick = { state.findPrevious() },
            modifier = Modifier.size(INPUT_MIN_HEIGHT),
            bgColor = Color.Transparent,
            rounded = false
        )
    }

    @Composable
    private fun ReplacerButtons(state: State) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.height(INPUT_MIN_HEIGHT)
        ) {
            Spacer(Modifier.width(BUTTON_SPACING))
            ReplaceNextButton(state)
            Spacer(Modifier.width(BUTTON_SPACING))
            ReplaceAllButton(state)
        }
    }

    @Composable
    private fun ReplaceNextButton(state: State) {
        Form.TextButton(
            text = Label.REPLACE,
            onClick = { state.replaceNext() },
            modifier = Modifier.height(BUTTON_HEIGHT)
        )
    }

    @Composable
    private fun ReplaceAllButton(state: State) {
        Form.TextButton(
            text = Label.REPLACE_ALL,
            onClick = { state.replaceAll() },
            modifier = Modifier.height(BUTTON_HEIGHT)
        )
    }
}