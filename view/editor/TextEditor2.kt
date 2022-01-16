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
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollOrientation.Vertical
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP

object TextEditor2 {

    private const val LINE_HEIGHT = 1.5f
    private val AREA_PADDING_HORIZONTAL = 6.dp
    private val AREA_PADDING_VERTICAL = 0.dp

    @Composable
    fun createState(file: File): State {
        val fontDefault = Theme.typography.code1
        val currentDensity = LocalDensity.current
        val lineHeightDP = with(currentDensity) { fontDefault.fontSize.toDp() * LINE_HEIGHT }
        val lineHeightSP = with(currentDensity) { lineHeightDP.toSp() * currentDensity.density }
        val fontStyle = fontDefault.copy(lineHeight = lineHeightSP)
        return State(file, fontStyle, lineHeightDP, currentDensity.density)
    }

    class State internal constructor(val file: File, val font: TextStyle, val lineHeight: Dp, val density: Float) {
        val lineCount: Int get() = file.content.size
        val content: MutableList<String> get() = file.content
        val contentHeight: Dp = lineHeight * lineCount
        var contentWidth: Dp by mutableStateOf(0.dp)
        var scrollOffsetY: Dp by mutableStateOf(0.dp)

        @OptIn(ExperimentalComposeUiApi::class)
        fun updateScrollOffset(event: MouseScrollEvent, bounds: IntSize) {
            if (event.delta is MouseScrollUnit.Line && event.orientation == Vertical) {
                val delta = lineHeight * (event.delta as MouseScrollUnit.Line).value
                val min = toDP(bounds.height, density) - contentHeight
                scrollOffsetY = (scrollOffsetY - delta).coerceIn(min, 0.dp)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Area(state: State, modifier: Modifier = Modifier) {
        Row(modifier = modifier.clipToBounds()
            .mouseScrollFilter { event, bounds -> state.updateScrollOffset(event, bounds); true }) {
            LineNumbers(state)
            Separator.Vertical()
            TextArea(state, state.density)
        }
    }

    @Composable
    private fun LineNumbers(editorState: State) {
        val font = editorState.font.copy(Theme.colors.onBackground.copy(0.5f))
        Box(
            modifier = Modifier.offset(y = editorState.scrollOffsetY)
                .height(editorState.contentHeight)
                .border(1.dp, Color.Red, RectangleShape)
        ) {
            Column(modifier = Modifier.background(Theme.colors.background)) {
                for (i in 1..editorState.lineCount) Text(text = i.toString(), style = font)
            }
        }
    }

    @Composable
    private fun TextArea(editorState: State, density: Float) {
        Box(modifier = Modifier.offset(y = editorState.scrollOffsetY)
            .height(editorState.contentHeight)
            .background(Theme.colors.background2)
            .horizontalScroll(rememberScrollState())
            .border(1.dp, Color.Yellow, RectangleShape)
            .onSizeChanged { editorState.contentWidth = toDP(it.width, density) }) {
            Column(modifier = Modifier) {
                for (i in 0 until editorState.lineCount) TextLine(editorState, i)
            }
        }
    }

    @Composable
    private fun TextLine(editorState: State, line: Int) {
        Text(
            text = AnnotatedString(editorState.content[line]),
            style = editorState.font.copy(Theme.colors.onBackground)
        )
    }
}