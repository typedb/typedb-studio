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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
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
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.LazyColumn
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import java.awt.event.MouseEvent.BUTTON1
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay

object TextEditor2 {

    private const val LINE_HEIGHT = 1.56f
    private val LINE_GAP = 2.dp
    private val AREA_PADDING_HORIZONTAL = 6.dp
    private val DEFAULT_FONT_WIDTH = 12.dp
    private val CURSOR_LINE_PADDING = 2.dp

    internal data class Coordinate(val x: Int, val y: Int)

    internal data class Cursor(val row: Int, val col: Int) : Comparable<Cursor> {
        override fun compareTo(other: Cursor): Int {
            return when (this.row) {
                other.row -> this.col.compareTo(other.col)
                else -> this.row.compareTo(other.row)
            }
        }

        override fun toString(): String {
            return "Cursor (row: $row, col: $col)"
        }
    }

    internal class Selection(val start: Cursor, endInit: Cursor) {
        var end: Cursor by mutableStateOf(endInit)
        val min: Cursor get() = if (start < end) start else end
        val max: Cursor get() = if (end > start) end else start

        override fun toString(): String {
            val startStatus = if (start == min) "min" else "max"
            val endStatus = if (end == max) "max" else "min"
            return "Selection {start: $start [$startStatus], end: $end[$endStatus]}"
        }
    }

    class State internal constructor(
        internal val file: File, internal val fontBase: TextStyle, internal val lineHeight: Dp
    ) {
        internal val content: SnapshotStateList<String> get() = file.content
        internal val textLayouts: MutableList<TextLayoutResult?> = MutableList(file.content.size) { null }
        internal var lineCount: Int by mutableStateOf(file.content.size)
        internal var cursor: Cursor by mutableStateOf(Cursor(0, 0))
        internal var selection: Selection? by mutableStateOf(null)
        private var isSelecting: Boolean by mutableStateOf(false)
        internal var textAreaCoord: Coordinate by mutableStateOf(Coordinate(0, 0))
        internal val scroller = LazyColumn.createScrollState(lineHeight, lineCount)

        private fun createCursor(x: Int, y: Int, density: Float): Cursor {
            val relX = x - textAreaCoord.x
            val relY = y - textAreaCoord.y + scroller.offset.value
            val row = min(floor(relY / lineHeight.value).toInt(), lineCount - 1)
            val offsetInLine = Offset(relX.toFloat() * density, (relY - (row * lineHeight.value)) * density)
            val col = textLayouts[row]?.getOffsetForPosition(offsetInLine) ?: 0
            return Cursor(row, col)
        }

        internal fun updateTextAreaCoord(rawPosition: Offset, density: Float) {
            textAreaCoord = Coordinate(
                x = toDP(rawPosition.x, density).value.toInt() + AREA_PADDING_HORIZONTAL.value.toInt(),
                y = toDP(rawPosition.y, density).value.toInt()
            )
        }

        internal fun updateCursor(x: Int, y: Int, density: Float) {
            cursor = createCursor(x, y, density)
            selection = null
        }

        internal fun updateCursorIfOutOfSelection(x: Int, y: Int, density: Float) {
            val newCursor = createCursor(x, y, density)
            if (selection == null || newCursor < selection!!.min || newCursor > selection!!.max) {
                cursor = newCursor
                selection = null
            }
        }

        internal fun startSelection() {
            isSelecting = true
        }

        internal fun endSelection() {
            isSelecting = false
        }

        internal fun updateSelection(x: Int, y: Int, density: Float) {
            if (isSelecting) {
                val newCursor = createCursor(x, y, density)
                if (newCursor != cursor) {
                    if (selection == null) {
                        selection = Selection(cursor, newCursor)
                    } else {
                        selection!!.end = newCursor
                        cursor = newCursor
                    }
                }
            }
        }
    }

    @Composable
    fun createState(file: File): State {
        val font = Theme.typography.code1
        val currentDensity = LocalDensity.current
        val lineHeight = with(currentDensity) { font.fontSize.toDp() * LINE_HEIGHT }
        return State(file, font, lineHeight)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Area(state: State, modifier: Modifier = Modifier) {
        val density = LocalDensity.current.density
        val fontHeight = with(LocalDensity.current) { (state.lineHeight - LINE_GAP).toSp() * density }
        val fontColor = Theme.colors.onBackground
        val textFont = state.fontBase.copy(color = fontColor, lineHeight = fontHeight)
        val lineNumberFont = state.fontBase.copy(color = fontColor.copy(0.5f), lineHeight = fontHeight)
        var fontWidth by remember { mutableStateOf(DEFAULT_FONT_WIDTH) }
        val contextMenuState = remember { ContextMenu.State() }

        Box { // We render a number to find out the default width of a digit for the given font
            Text(text = "0", style = lineNumberFont, onTextLayout = { fontWidth = toDP(it.size.width, density) })
            Row(modifier = modifier) {
                LineNumberArea(state, lineNumberFont, fontWidth)
                Separator.Vertical()
                TextArea(state, contextMenuState, textFont, fontWidth, density)
            }
        }
    }

    @Composable
    private fun LineNumberArea(state: State, font: TextStyle, fontWidth: Dp) {
        val minWidth = fontWidth * ceil(log10(state.lineCount.toDouble())).toInt() + AREA_PADDING_HORIZONTAL * 2 + 2.dp
        val lazyColumnState: LazyColumn.State<Int> = LazyColumn.createState(
            items = (0 until state.lineCount).map { it },
            scroller = state.scroller
        )
        LazyColumn.Area(state = lazyColumnState) { index, _ -> LineNumber(state, index, font, minWidth) }
    }

    @Composable
    private fun LineNumber(state: State, index: Int, font: TextStyle, minWidth: Dp) {
        val isCursor = state.cursor.row == index
        val isSelected = state.selection?.let { it.min.row <= index && it.max.row >= index } ?: false
        val bgColor = if (isCursor || isSelected) Theme.colors.primary else Theme.colors.background
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
    private fun TextArea(
        state: State, contextMenuState: ContextMenu.State, font: TextStyle, fontWidth: Dp, density: Float
    ) {
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
            .onPointerEvent(PointerEventType.Press) { if (it.awtEvent.button == BUTTON1) state.startSelection() }
            .onPointerEvent(PointerEventType.Move) { state.updateSelection(it.awtEvent.x, it.awtEvent.y, density) }
            .onPointerEvent(PointerEventType.Release) { if (it.awtEvent.button == BUTTON1) state.endSelection() }
            .pointerInput(Unit) { onPointerInput(state, contextMenuState) }
            .onSizeChanged { mayUpdateMinWidth(it.width) }) {
            ContextMenu.Popup(contextMenuState) { contextMenuFn(state) }
            LazyColumn.Area(state = lazyColumnState) { index, text ->
                TextLine(state, index, text, font, fontWidth, minWidth, density) { mayUpdateMinWidth(it) }
            }
        }
    }

    @Composable
    private fun TextLine(
        state: State, index: Int, text: String,
        font: TextStyle, fontWidth: Dp, minWidth: Dp, density: Float,
        onSizeChanged: (Int) -> Unit
    ) {
        val bgColor = when {
            state.cursor.row == index && state.selection == null -> Theme.colors.primary
            else -> Theme.colors.background2
        }

        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier.background(bgColor)
                .defaultMinSize(minWidth = minWidth)
                .height(state.lineHeight)
                .padding(horizontal = AREA_PADDING_HORIZONTAL)
        ) {
            if (state.selection != null && state.selection!!.min.row <= index && state.selection!!.max.row >= index) {
                SelectionHighlighter(state, index, density)
            }
            Text(
                text = AnnotatedString(text), style = font,
                modifier = Modifier.onSizeChanged { onSizeChanged(it.width) },
                onTextLayout = { state.textLayouts[index] = it }
            )
            if (state.cursor.row == index && state.textLayouts[index] != null) {
                CursorIndicator(state, text, font, fontWidth, density)
            }
        }
    }

    @Composable
    private fun SelectionHighlighter(state: State, index: Int, density: Float) {
        assert(state.selection != null && state.selection!!.min.row <= index && state.selection!!.max.row >= index)
        val start = when {
            state.selection!!.min.row < index -> 0
            else -> state.selection!!.min.col // state.selection!!.min.row == index
        }
        val end = when {
            state.selection!!.max.row > index -> state.content[index].length
            else -> state.selection!!.max.col
        }
        var startPos = toDP(state.textLayouts[index]!!.getCursorRect(start).left, density)
        var endPos = toDP(state.textLayouts[index]!!.getCursorRect(end).right, density)
        if (state.selection!!.min.row < index) startPos -= AREA_PADDING_HORIZONTAL
        if (state.selection!!.max.row > index) endPos += DEFAULT_FONT_WIDTH
        val width = endPos - startPos

        val color = Theme.colors.tertiary.copy(Theme.SELECTION_ALPHA)
        Box(Modifier.offset(x = startPos).width(width).height(state.lineHeight).background(color))
    }

    @Composable
    private fun CursorIndicator(state: State, text: String, font: TextStyle, fontWidth: Dp, density: Float) {
        val offsetX = toDP(state.textLayouts[state.cursor.row]!!.getCursorRect(state.cursor.col).left, density)
        val width = when {
            state.cursor.col >= text.length -> fontWidth
            else -> toDP(state.textLayouts[state.cursor.row]!!.getBoundingBox(state.cursor.col).width, density)
        }
        BlinkingCursorIndicator(state, text.getOrNull(state.cursor.col)?.toString() ?: "", offsetX, width, font)
    }

    @OptIn(ExperimentalTime::class)
    @Composable
    private fun BlinkingCursorIndicator(state: State, char: String, offsetX: Dp, width: Dp, font: TextStyle) {
        var visible by remember { mutableStateOf(true) }
        if (visible) {
            Box(
                modifier = Modifier.offset(x = offsetX, y = CURSOR_LINE_PADDING)
                    .width(width).height(state.lineHeight - CURSOR_LINE_PADDING * 2)
                    .background(Theme.colors.secondary)
            ) { Text(char, Modifier.offset(y = -CURSOR_LINE_PADDING), style = font.copy(Theme.colors.background2)) }
        }
        LaunchedEffect(state.cursor) {
            visible = true
            while (true) {
                delay(Duration.Companion.milliseconds(500))
                visible = !visible
            }
        }
    }

    private suspend fun PointerInputScope.onPointerInput(state: State, contextMenuState: ContextMenu.State) {
        contextMenuState.onPointerInput(
            pointerInputScope = this,
            onSinglePrimaryClick = { state.updateCursor(it.x, it.y, density) },
            onDoublePrimaryClick = { }, // TODO
            onTriplePrimaryClick = { }, // TODO
            onSecondaryClick = { state.updateCursorIfOutOfSelection(it.x, it.y, density) }
        )
    }

    private fun contextMenuFn(state: State): List<ContextMenu.Item> { // TODO
        return listOf(
            ContextMenu.Item(Label.PASTE, Icon.Code.PASTE) {}
        )
    }
}