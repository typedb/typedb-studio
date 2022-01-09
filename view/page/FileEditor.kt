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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
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
    private val AREA_PADDING_VERTICAL = 3.dp
    private val LINE_NUMBER_MIN_WIDTH = 40.dp

    private fun calcContentHeight(content: String, lineHeight: Dp): Dp {
        return calcContentHeight(content.split("\n").size, lineHeight)
    }

    private fun calcContentHeight(lineCount: Int, lineHeight: Dp): Dp {
        return lineHeight * lineCount + AREA_PADDING_VERTICAL * 2
    }

    @Composable
    fun createState(content: String, onChange: (String) -> Unit): State {
        val font = Theme.typography.code1
        val currentDensity = LocalDensity.current
        val lineHeightDP = with(currentDensity) { font.fontSize.toDp() * LINE_HEIGHT }
        val lineHeightSP = with(currentDensity) { lineHeightDP.toSp() * currentDensity.density }
        return State(content, content.split("\n").size, onChange, font.copy(lineHeight = lineHeightSP), lineHeightDP)
    }

    class State internal constructor(
        initContent: String, initLineCount: Int,
        val onChange: (String) -> Unit, val font: TextStyle,
        private val lineHeight: Dp,
    ) {
        internal var value: TextFieldValue by mutableStateOf(highlight(initContent))
        internal var lineCount by mutableStateOf(initLineCount)
        internal var editorHeight: Dp by mutableStateOf(lineHeight * initLineCount); private set
        private var contentHeight: Dp by mutableStateOf(lineHeight * initLineCount)
        private var areaHeight: Dp by mutableStateOf(0.dp)

        internal fun updateValue(newValue: TextFieldValue) {
            onChange(newValue.text)
            value = newValue
            val oldLineCount = lineCount
            val newLineCount = newValue.text.split("\n").size
            if (oldLineCount != newLineCount) {
                lineCount = newLineCount
                contentHeight = calcContentHeight(lineCount, lineHeight)
                mayUpdateDisplayHeight()
            }
        }

        internal fun updateAreaHeight(newHeight: Dp) {
            if (areaHeight != newHeight) {
                areaHeight = newHeight
                mayUpdateDisplayHeight()
            }
        }

        private fun mayUpdateDisplayHeight() {
            if (areaHeight > contentHeight && areaHeight != editorHeight) editorHeight = areaHeight
            else if (contentHeight >= areaHeight && contentHeight != editorHeight) editorHeight = contentHeight
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
    private fun LineNumbers(state: State) {
        val fontStyle = state.font.copy(Theme.colors.onBackground.copy(0.5f))
        Column(
            modifier = Modifier.height(state.editorHeight)
                .defaultMinSize(minWidth = LINE_NUMBER_MIN_WIDTH)
                .padding(AREA_PADDING_HORIZONTAL, AREA_PADDING_VERTICAL)
                .background(Theme.colors.background),
            horizontalAlignment = Alignment.End
        ) { for (i in 1..state.lineCount) Text(text = i.toString(), style = fontStyle) }
    }

    @Composable
    private fun TextArea(editorState: State) {
        val pixD = LocalDensity.current.density
        var minWidth by remember { mutableStateOf(4096.dp) }
        Box(modifier = Modifier.fillMaxSize()
            .background(Theme.colors.background2)
            .onSizeChanged { minWidth = toDP(it.width, pixD) }
            .horizontalScroll(rememberScrollState())) {
            BasicTextField(
                value = editorState.value,
                onValueChange = { editorState.updateValue(it) },
                cursorBrush = SolidColor(Theme.colors.secondary),
                textStyle = editorState.font.copy(Theme.colors.onBackground),
                modifier = Modifier.height(editorState.editorHeight)
                    .defaultMinSize(minWidth = minWidth)
                    .padding(horizontal = AREA_PADDING_HORIZONTAL, vertical = AREA_PADDING_VERTICAL)
            )
        }
    }

    private fun highlight(content: String): TextFieldValue {
        return TextFieldValue(AnnotatedString(content)) // TODO
    }
}