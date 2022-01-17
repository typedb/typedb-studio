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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP

object TextEditor3 {

    private const val LINE_HEIGHT = 1.5f
    private val AREA_PADDING_HORIZONTAL = 6.dp
    private val AREA_PADDING_VERTICAL = 0.dp

    @Composable
    fun createState(file: File): State {
        val fontDefault = Theme.typography.code1
        val currentDensity = LocalDensity.current
        val lineHeightDP = with(currentDensity) { fontDefault.fontSize.toDp() * LINE_HEIGHT }
        val lineHeightSP = with(currentDensity) { lineHeightDP.toSp() * currentDensity.density }
        val fontStyle = fontDefault.copy(color = Theme.colors.onBackground, lineHeight = lineHeightSP)
        return State(file, fontStyle, lineHeightDP, currentDensity.density)
    }

    class State internal constructor(val file: File, val font: TextStyle, val lineHeight: Dp, val density: Float) {
        val content: MutableList<String> get() = file.content
        var lineCount: Int by mutableStateOf(file.content.size)
        val contentHeight: Dp by mutableStateOf(lineHeight * lineCount)
    }

    @Composable
    fun Area(state: State, modifier: Modifier) {
        val lineNumberScroller = rememberScrollState()
        val textAreaScroller = rememberScrollState()
        Row(modifier = modifier) {
            LineNumbers(state, lineNumberScroller, textAreaScroller)
            Separator.Vertical()
            TextArea(state, lineNumberScroller, textAreaScroller, state.density)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun LineNumbers(editorState: State, lineNumberScroller: ScrollState, textAreaScroller: ScrollState) {
        val font = editorState.font.copy(Theme.colors.onBackground.copy(0.5f))
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.background(Theme.colors.background)
                .padding(AREA_PADDING_HORIZONTAL, AREA_PADDING_VERTICAL)
                .verticalScroll(state = lineNumberScroller)
                .mouseScrollFilter { event, bounds ->
                    println(lineNumberScroller.value); false
                }
        ) { for (i in 1..editorState.lineCount) Text(text = i.toString(), style = font) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun TextArea(
        editorState: State,
        lineNumberScroller: ScrollState,
        textAreaScroller: ScrollState,
        density: Float
    ) {
        var minWidth by remember { mutableStateOf(4096.dp) }
        fun mayUpdateMinWidth(newWidth: Dp) {
            if (newWidth > minWidth) minWidth = newWidth
        }

        Box(modifier = Modifier.fillMaxSize()
            .background(Theme.colors.background2)
            .horizontalScroll(rememberScrollState())
            .onSizeChanged { mayUpdateMinWidth(Theme.toDP(it.width, density)) }) {
            Column(
                modifier = Modifier.defaultMinSize(minWidth = minWidth)
                    .padding(AREA_PADDING_HORIZONTAL, AREA_PADDING_VERTICAL)
                    .verticalScroll(textAreaScroller)
            ) {
                editorState.content.forEach {
                    Text(
                        text = it,
                        style = editorState.font,
                        modifier = Modifier.onSizeChanged { mayUpdateMinWidth(toDP(it.width, density)) }
                    )
                }
            }
        }
    }
}