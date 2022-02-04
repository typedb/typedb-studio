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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.MultilineTextInput
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.RECTANGLE_ROUNDED_ALL
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import com.vaticle.typedb.studio.view.editor.TextProcessor.Companion.CHANGE_BATCH_DELAY
import com.vaticle.typedb.studio.view.editor.TextToolbar.State.InputType.FINDER
import com.vaticle.typedb.studio.view.editor.TextToolbar.State.InputType.REPLACER
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    internal class State(private val target: InputTarget, private val finder: TextFinder) {

        enum class InputType { FINDER, REPLACER }

        internal var lineHeight by mutableStateOf(0.dp)
        internal var showFinder by mutableStateOf(false)
        internal var showReplacer by mutableStateOf(false)
        internal var findText by mutableStateOf(TextFieldValue(""))
        internal var findTextLayout: TextLayoutResult? by mutableStateOf(null)
        internal val findTextHorScroller = Form.MultilineTextInputState(target.density)
        internal val replaceTextHorScroller = Form.MultilineTextInputState(target.density)
        internal var replaceText by mutableStateOf(TextFieldValue(""))
        internal var replaceTextLayout: TextLayoutResult? by mutableStateOf(null)
        internal var isRegex by mutableStateOf(false)
        internal var isWord by mutableStateOf(false)
        internal var isCaseSensitive by mutableStateOf(false)
        internal val hasFindMatches: Boolean get() = finder.hasMatches
        internal val status: String get() = finder.status()
        internal val density: Float get() = target.density
        private var changeCount: AtomicInteger = AtomicInteger(0)
        private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

        internal fun showFinder() {
            showFinder = true
            showReplacer = false
            if (target.selection != null) findText = TextFieldValue(target.selectedText())
        }

        internal fun showReplacer() {
            showReplacer = true
            if (target.selection != null) findText = TextFieldValue(target.selectedText())
        }

        internal fun toolbarHeight(): Dp {
            var height = finderInputHeight()
            if (showReplacer) height += replacerInputHeight() + Separator.WEIGHT
            return height
        }

        internal fun toolbarButtonAreaHeight(): Dp {
            var height = INPUT_MIN_HEIGHT
            if (showReplacer) height += INPUT_MIN_HEIGHT + Separator.WEIGHT
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
            val height = lineHeight * findText.text.split("\n").size + INPUT_VERTICAL_PADDING * 2
            return height.coerceIn(INPUT_MIN_HEIGHT, INPUT_MAX_HEIGHT)
        }

        internal fun replacerInputHeight(): Dp {
            val height = lineHeight * replaceText.text.split("\n").size + INPUT_VERTICAL_PADDING * 2
            return height.coerceIn(INPUT_MIN_HEIGHT, INPUT_MAX_HEIGHT)
        }

        internal fun handle(event: KeyEvent, focusManager: FocusManager, inputType: InputType): Boolean {
            return if (event.type == KeyEventType.KeyUp) false
            else KeyMapper.CURRENT.map(event)?.let {
                when (it) {
                    is KeyMapper.EditorCommand -> execute(it, focusManager, inputType)
                    else -> false
                }
            } ?: false
        }

        private fun execute(
            command: KeyMapper.EditorCommand,
            focusManager: FocusManager,
            inputType: InputType
        ): Boolean {
            return when (command) {
                KeyMapper.EditorCommand.TAB -> {
                    moveFocusNext(focusManager)
                    true
                }
                KeyMapper.EditorCommand.ENTER_SHIFT -> {
                    insertNewLine(inputType)
                    true
                }
                KeyMapper.EditorCommand.ENTER -> {
                    onEnter(inputType)
                    true
                }
                else -> false
            }
        }

        private fun moveFocusNext(focusManager: FocusManager) {
            focusManager.moveFocus(FocusDirection.Next)
        }

        private fun insertNewLine(inputType: InputType) {
            val textFieldValue = when (inputType) {
                FINDER -> findText
                REPLACER -> replaceText
            }
            val noSelectionField = when {
                textFieldValue.selection.collapsed -> textFieldValue
                else -> deleteSelection(textFieldValue)
            }
            val cursor = noSelectionField.selection.end
            val newText = noSelectionField.text.substring(0, cursor) + "\n" + noSelectionField.text.substring(cursor)
            val newTextFieldValue = TextFieldValue(newText, TextRange(cursor + 1))
            when (inputType) {
                FINDER -> updateFindText(newTextFieldValue)
                REPLACER -> replaceText = newTextFieldValue
            }
        }

        private fun onEnter(inputType: InputType) {
            when (inputType) {
                FINDER -> findNext()
                REPLACER -> replaceCurrent()
            }
        }

        private fun deleteSelection(field: TextFieldValue): TextFieldValue {
            val text = field.text.removeRange(field.selection.min, field.selection.max)
            return TextFieldValue(text, TextRange(field.selection.min))
        }

        internal fun updateContent() {
            finder.updateContent()
        }

        internal fun toggleCaseSensitive() {
            isCaseSensitive = !isCaseSensitive
            if (findText.text.isNotEmpty()) findText()
        }

        internal fun toggleWord() {
            isWord = !isWord
            if (isWord) isRegex = false
            if (findText.text.isNotEmpty()) findText()
        }

        internal fun toggleRegex() {
            isRegex = !isRegex
            if (isRegex) isWord = false
            if (findText.text.isNotEmpty()) findText()
        }

        internal fun updateFindText(text: TextFieldValue) {
            findText = text
            if (findText.text.isNotEmpty()) delayedFindText()
            else finder.reset()
        }

        @OptIn(ExperimentalTime::class)
        private fun delayedFindText() {
            changeCount.incrementAndGet()
            coroutineScope.launch {
                delay(CHANGE_BATCH_DELAY)
                if (changeCount.decrementAndGet() == 0 && findText.text.isNotEmpty()) {
                    findText()
                }
            }
        }

        private fun findText() {
            if (isRegex) finder.findRegex(findText.text, isCaseSensitive)
            else if (isWord) finder.findWord(findText.text, isCaseSensitive)
            else finder.findText(findText.text, isCaseSensitive)
        }

        internal fun findNext() {
            if (finder.hasMatches) finder.findNext()
        }

        internal fun findPrevious() {
            if (finder.hasMatches) finder.findPrevious()
        }

        internal fun replaceCurrent() {
            if (finder.hasMatches) finder.replaceCurrent(replaceText.text)
        }

        internal fun replaceAll() {
            if (finder.hasMatches) finder.replaceAll(replaceText.text)
        }
    }

    @Composable
    internal fun Area(state: State, modifier: Modifier = Modifier) {
        val findTextFocusReq = FocusRequester()
        Box {
            ComputeFontHeight(state)
            // TODO: figure out how to set min width to MIN_WIDTH
            Row(modifier = modifier.widthIn(max = state.toolbarMaxWidth()).height(state.toolbarHeight())) {
                TextInputs(state, Modifier.weight(1f), findTextFocusReq)
                Separator.Vertical()
                ToolbarButtons(state)
            }
        }
        Separator.Horizontal()
        LaunchedEffect(state, state.showReplacer) { findTextFocusReq.requestFocus() }
    }

    @Composable
    private fun ComputeFontHeight(state: State) {
        // We render a character to find out the default height of a line for the given font
        // TODO: use FinderTextInput TextLayoutResult after: https://github.com/JetBrains/compose-jb/issues/1781
        Text(
            text = "0", style = Theme.typography.body1,
            onTextLayout = { state.lineHeight = toDP(it.size.height, state.density) }
        )
    }

    @Composable
    private fun TextInputs(state: State, modifier: Modifier, focusReq: FocusRequester) {
        Column(modifier) {
            FinderTextInput(state, focusReq)
            if (state.showReplacer) {
                Separator.Horizontal()
                ReplacerTextInput(state)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun FinderTextInput(state: State, focusReq: FocusRequester) {
        val focusManager = LocalFocusManager.current
        MultilineTextInput(
            state = state.findTextHorScroller,
            value = state.findText,
            icon = Icon.Code.MAGNIFYING_GLASS,
            focusRequester = focusReq,
            onValueChange = { state.updateFindText(it) },
            onTextLayout = { state.findTextLayout = it },
            modifier = Modifier.height(state.finderInputHeight())
                .onFocusEvent { state.updateContent() }
                .onPreviewKeyEvent { state.handle(it, focusManager, FINDER) },
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ReplacerTextInput(state: State) {
        val focusManager = LocalFocusManager.current
        MultilineTextInput(
            state = state.replaceTextHorScroller,
            value = state.replaceText,
            icon = Icon.Code.RIGHT_LEFT,
            onValueChange = { state.replaceText = it },
            onTextLayout = { state.replaceTextLayout = it },
            modifier = Modifier.height(state.replacerInputHeight())
                .onPreviewKeyEvent { state.handle(it, focusManager, REPLACER) },
        )
    }

    @Composable
    private fun ToolbarButtons(state: State) {
        Column(Modifier.width(BUTTON_AREA_WIDTH).height(state.toolbarButtonAreaHeight())) {
            Spacer(Modifier.weight(1f))
            FinderButtons(state)
            Spacer(Modifier.weight(1f))
            if (state.showReplacer) {
                ReplacerButtons(state)
                Spacer(Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun FinderButtons(state: State) {
        Row(Modifier.height(BUTTON_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(BUTTON_SPACING))
            FinderNextPreviousButtons(state)
            Spacer(Modifier.width(BUTTON_SPACING))
            FinderParameterButtons(state)
            Spacer(Modifier.width(BUTTON_SPACING))
            if (state.findText.text.isNotEmpty()) {
                Spacer(Modifier.width(BUTTON_SPACING))
                FinderStatus(state, Modifier.weight(1f))
            }
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
            value = state.status,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
            color = if (state.hasFindMatches) Theme.colors.secondary else Theme.colors.error
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
        Row(Modifier.height(BUTTON_HEIGHT)) {
            Spacer(Modifier.width(BUTTON_SPACING))
            ReplacerButton(Label.REPLACE) { state.replaceCurrent() }
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