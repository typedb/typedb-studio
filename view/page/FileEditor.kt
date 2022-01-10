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

package com.vaticle.typedb.studio.view.page

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP

object FileEditor {

    private const val LINE_HEIGHT = 1.5f
    private val AREA_PADDING_HORIZONTAL = 6.dp
    private val AREA_PADDING_VERTICAL = 0.dp
    private val LINE_NUMBER_MIN_WIDTH = 40.dp
    private val FONT_WIDTH = 14

    @Composable
    fun createState(content: String, onChange: (String) -> Unit): State {
        val font = Theme.typography.code1
        val currentDensity = LocalDensity.current
        val lineHeightDP = with(currentDensity) { font.fontSize.toDp() * LINE_HEIGHT }
        val lineHeightSP = with(currentDensity) { lineHeightDP.toSp() * currentDensity.density }
        val lineCount = content.split("\n").size
        val contentHeight = State.calcContentHeight(lineCount, lineHeightDP)
        val fontStyle = font.copy(lineHeight = lineHeightSP)
        return State(content, lineCount, contentHeight, onChange, fontStyle, lineHeightDP, currentDensity.density)
    }

    class State internal constructor(
        initContent: String,
        initLineCount: Int,
        initContentHeight: Dp,
        internal val onChange: (String) -> Unit,
        internal val font: TextStyle,
        internal val lineHeight: Dp,
        internal val pixelDensity: Float,
    ) {
        internal var cursorOffset by mutableStateOf(0.dp)
        internal var lineCount by mutableStateOf(initLineCount)
        private var currentLineIndex by mutableStateOf(0)
        internal val currentLineOffset get() = lineHeight * currentLineIndex + AREA_PADDING_VERTICAL
        internal var content: TextFieldValue by mutableStateOf(highlight(initContent))
        internal var layout: TextLayoutResult? by mutableStateOf(null)
        internal var editorHeight: Dp by mutableStateOf(initContentHeight); private set
        private var contentHeight: Dp by mutableStateOf(initContentHeight)
        private var areaHeight: Dp by mutableStateOf(0.dp)

        companion object {
            internal fun calcContentHeight(lineCount: Int, lineHeight: Dp): Dp {
                return lineHeight * lineCount + AREA_PADDING_VERTICAL * 2
            }
        }

        private fun updateCurrentLineAndCursor() {
            val cursorOffsetRaw = when {
                content.selection.end < content.text.length -> layout?.getBoundingBox(content.selection.end)?.left
                else -> when {
                    content.text.endsWith("\n") -> 0
                    else -> layout?.getBoundingBox(content.selection.end - 1)?.right
                }
            } ?: 0
            cursorOffset = toDP(cursorOffsetRaw, pixelDensity)
            currentLineIndex = layout?.getLineForOffset(content.selection.end) ?: 0
        }

        private fun mayUpdateDisplayHeight() {
            if (areaHeight > contentHeight && areaHeight != editorHeight) editorHeight = areaHeight
            else if (contentHeight >= areaHeight && contentHeight != editorHeight) editorHeight = contentHeight
        }

        internal fun updateContent(newContent: TextFieldValue) {
            val oldText = content.text
            onChange(newContent.text)
            content = newContent
            if (oldText == newContent.text) updateCurrentLineAndCursor()
            // else, text have changed and updateLayout() will be called
        }

        internal fun updateLayout(newLayout: TextLayoutResult) {
            layout = newLayout
            updateCurrentLineAndCursor()
            if (lineCount == newLayout.lineCount) return

            lineCount = newLayout.lineCount
            contentHeight = calcContentHeight(lineCount, lineHeight)
            mayUpdateDisplayHeight()
        }

        internal fun updateAreaHeight(newHeight: Dp) {
            if (areaHeight != newHeight) {
                areaHeight = newHeight
                mayUpdateDisplayHeight()
            }
        }
    }

    @Composable
    fun Area(state: State, modifier: Modifier = Modifier) {
        val pixD = LocalDensity.current.density
        val scrollState = rememberScrollState()
        Row(modifier = modifier
            .onSizeChanged { state.updateAreaHeight(toDP(it.height, pixD)) }
            .verticalScroll(scrollState)) {
            LineNumbers(state)
            Separator.Vertical(modifier = Modifier.height(state.editorHeight))
            TextArea(state)
        }
    }

    @Composable
    private fun LineNumbers(editorState: State) {
        val fontStyle = editorState.font.copy(Theme.colors.onBackground.copy(0.5f))
        var actualWidth by remember { mutableStateOf(LINE_NUMBER_MIN_WIDTH) }
        Box(modifier = Modifier.background(Theme.colors.background)) {
            CurrentLineHighlighter(editorState, actualWidth)
            Column(
                modifier = Modifier.height(editorState.editorHeight)
                    .defaultMinSize(minWidth = LINE_NUMBER_MIN_WIDTH)
                    .padding(AREA_PADDING_HORIZONTAL, AREA_PADDING_VERTICAL)
                    .onSizeChanged { actualWidth = toDP(it.width, editorState.pixelDensity) },
                horizontalAlignment = Alignment.End
            ) { for (i in 1..editorState.lineCount) Text(text = i.toString(), style = fontStyle) }
        }
    }

    @Composable
    private fun TextArea(editorState: State) {
        var minWidth by remember { mutableStateOf(4096.dp) }
        var actualWidth by remember { mutableStateOf(4096.dp) }
        val focusReq = FocusRequester()
        Box(modifier = Modifier.fillMaxSize()
            .background(Theme.colors.background2)
            .onSizeChanged { minWidth = toDP(it.width, editorState.pixelDensity) }
            .horizontalScroll(rememberScrollState())) {
            CurrentLineHighlighter(editorState, actualWidth)
            Cursor(editorState)
            BasicTextField(
                value = editorState.content,
                onValueChange = { editorState.updateContent(it) },
                onTextLayout = { editorState.updateLayout(it) },
                cursorBrush = SolidColor(Color.Transparent),
                textStyle = editorState.font.copy(Theme.colors.onBackground),
                modifier = Modifier.focusRequester(focusReq)
                    .height(editorState.editorHeight)
                    .defaultMinSize(minWidth = minWidth)
                    .padding(horizontal = AREA_PADDING_HORIZONTAL, vertical = AREA_PADDING_VERTICAL)
                    .onSizeChanged { actualWidth = toDP(it.width, editorState.pixelDensity) }
            )
        }
        LaunchedEffect(editorState) { focusReq.requestFocus() }
    }

    @Composable
    private fun CurrentLineHighlighter(editorState: State, actualWidth: Dp) {
        Box(
            modifier = Modifier.offset(y = editorState.currentLineOffset)
                .height(editorState.lineHeight + 2.dp)
                .width(actualWidth + AREA_PADDING_HORIZONTAL * 2)
                .background(Theme.colors.primary)
        )
    }

    @Composable
    private fun Cursor(editorState: State) {
        val x = editorState.cursorOffset + AREA_PADDING_HORIZONTAL
        val y = editorState.currentLineOffset + 2.dp
        Box(
            modifier = Modifier.offset(x, y)
                .height(editorState.lineHeight - 2.dp)
                .width(toDP(FONT_WIDTH, editorState.pixelDensity))
                .background(Theme.colors.secondary)
        )
    }

    private fun highlight(content: String): TextFieldValue {
        return TextFieldValue(AnnotatedString(content)) // TODO
    }
}