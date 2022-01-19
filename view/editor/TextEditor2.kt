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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.view.common.component.LazyColumn
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import java.awt.event.MouseEvent
import kotlin.math.floor
import kotlin.math.min

object TextEditor2 {

    private const val LINE_HEIGHT = 1.56f
    private val LINE_GAP = 2.dp
    private val AREA_PADDING_HORIZONTAL = 6.dp

    @Composable
    fun createState(file: File): State {
        val font = Theme.typography.code1
        val currentDensity = LocalDensity.current
        val lineHeight = with(currentDensity) { font.fontSize.toDp() * LINE_HEIGHT }
        return State(file, font, lineHeight)
    }

    internal data class Coordinate(val x: Int, val y: Int)
    internal data class Cursor(val row: Int, val col: Int)
    internal data class Selection(val start: Cursor, val end: Cursor)

    class State internal constructor(
        internal val file: File, internal val fontBase: TextStyle, internal val lineHeight: Dp
    ) {
        internal val content: SnapshotStateList<String> get() = file.content
        internal val textLayouts: MutableList<TextLayoutResult?> = MutableList(file.content.size) { null }
        internal var lineCount: Int by mutableStateOf(file.content.size)
        internal var cursor: Cursor by mutableStateOf(Cursor(0, 0))
        internal var selection: Selection? by mutableStateOf(null)
        internal var textAreaCoord: Coordinate by mutableStateOf(Coordinate(0, 0))
        internal val scroller = LazyColumn.createScrollState(lineHeight, lineCount)

        internal fun isCurrentLine(index: Int): Boolean {
            return cursor.row == index
        }

        internal fun updateTextAreaCoord(rawPosition: Offset, density: Float) {
            textAreaCoord = Coordinate(
                x = toDP(rawPosition.x, density).value.toInt() + AREA_PADDING_HORIZONTAL.value.toInt(),
                y = toDP(rawPosition.y, density).value.toInt()
            )
        }

        internal fun updateCursor(x: Int, y: Int, density: Float) {
            val relX = x - textAreaCoord.x
            val relY = y - textAreaCoord.y + scroller.offset.value
            val row = min(floor(relY / lineHeight.value).toInt(), lineCount - 1)
            val offsetInLine = Offset(relX.toFloat() * density, (relY - (row * lineHeight.value)) * density)
            val col = textLayouts[row]?.getOffsetForPosition(offsetInLine) ?: 0
            cursor = Cursor(row, col)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Area(state: State, modifier: Modifier = Modifier) {
        val density = LocalDensity.current.density
        val fontHeight = with (LocalDensity.current) { (state.lineHeight - LINE_GAP).toSp() * density }
        val fontColor = Theme.colors.onBackground
        val textFont = state.fontBase.copy(color = fontColor, lineHeight = fontHeight)
        val lineNumberFont = state.fontBase.copy(color = fontColor.copy(0.5f), lineHeight = fontHeight)

        Row(modifier = modifier) {
            LineNumberArea(state, lineNumberFont, density)
            Separator.Vertical()
            TextArea(state, textFont, density)
        }
    }

    @Composable
    private fun LineNumberArea(state: State, font: TextStyle, density: Float) {
        var minWidth by remember { mutableStateOf(0.dp) }
        val lazyColumnState: LazyColumn.State<Int> = LazyColumn.createState(
            items = (0 until state.lineCount).map { it },
            scroller = state.scroller
        )
        Box {
            Text( // We render the longest line number to find out the width
                text = state.lineCount.toString(), style = font,
                onTextLayout = {
                    minWidth = toDP(it.size.width, density) + 2.dp + AREA_PADDING_HORIZONTAL * 2
                }
            )
            LazyColumn.Area(state = lazyColumnState) { index, _ -> LineNumber(state, index, font, minWidth) }
        }
    }

    @Composable
    private fun LineNumber(state: State, index: Int, font: TextStyle, minWidth: Dp) {
        val bgColor = if (state.isCurrentLine(index)) Theme.colors.primary else Theme.colors.background
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier.background(bgColor)
                .defaultMinSize(minWidth = minWidth)
                .height(state.lineHeight)
                .padding(horizontal = AREA_PADDING_HORIZONTAL)
        ) { Text(text = (index + 1).toString(), style = font) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun TextArea(state: State, font: TextStyle, density: Float) {
        var minWidth by remember { mutableStateOf(0.dp) }
        val lazyColumnState: LazyColumn.State<String> = LazyColumn.createState(
            items = state.content,
            scroller = state.scroller
        )

        fun mayUpdateMinWidth(newRawWidth: Int) {
            val newWidth = toDP(newRawWidth, density)
            if (newWidth > minWidth) minWidth = newWidth
        }

        Box(modifier = Modifier.fillMaxSize()
            .background(Theme.colors.background2)
            .horizontalScroll(rememberScrollState())
            .onGloballyPositioned { state.updateTextAreaCoord(it.positionInWindow(), density) }
            .onPointerEvent(PointerEventType.Press) { onPointerEvent(state, it.awtEvent, density) }
            .onSizeChanged { mayUpdateMinWidth(it.width) }) {
            LazyColumn.Area(
                state = lazyColumnState,
                modifier = Modifier,
            ) { index, text -> TextLine(state, index, text, font, minWidth) { mayUpdateMinWidth(it) } }
        }
    }

    @Composable
    private fun TextLine(state: State, index: Int, text: String, font: TextStyle, minWidth: Dp, onSizeChanged: (Int) -> Unit) {
        val bgColor = if (state.isCurrentLine(index)) Theme.colors.primary else Theme.colors.background2
        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier.background(bgColor)
                .defaultMinSize(minWidth = minWidth)
                .height(state.lineHeight)
                .padding(horizontal = AREA_PADDING_HORIZONTAL)
        ) {
            Text(
                text = AnnotatedString(text), style = font,
                modifier = Modifier.onSizeChanged { onSizeChanged(it.width) },
                onTextLayout = { state.textLayouts[index] = it }
            )
        }
    }

    private fun onPointerEvent(state: State, event: MouseEvent, density: Float) {
        when (event.button) {
            MouseEvent.BUTTON1 -> when (event.clickCount) {
                1 -> state.updateCursor(event.x, event.y, density)
                2 -> {}
            }
            MouseEvent.BUTTON3 -> {}
        }
    }
}