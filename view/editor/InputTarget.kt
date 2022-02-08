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
import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.status.StatusManager
import com.vaticle.typedb.studio.view.common.component.LazyColumn
import com.vaticle.typedb.studio.view.common.theme.Theme
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.floor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class InputTarget constructor(
    private val content: SnapshotStateList<AnnotatedString>,
    internal val lineHeight: Dp,
    private val horPadding: Dp,
    private val rendering: TextRendering,
    initDensity: Float
) {

    companion object {
        // TODO: is this complete?
        private val WORD_BREAK_CHARS = charArrayOf(',', '.', ':', ';', '=', '(', ')', '{', '}')
    }

    internal data class Cursor(val row: Int, val col: Int) : Comparable<Cursor> {

        companion object {
            fun min(first: Cursor, second: Cursor): Cursor {
                return if (first < second) first else second
            }
        }

        override fun compareTo(other: Cursor): Int {
            return when (this.row) {
                other.row -> this.col.compareTo(other.col)
                else -> this.row.compareTo(other.row)
            }
        }

        fun toSelection(): Selection {
            return Selection(this, this)
        }

        fun label(): String {
            return "${row + 1}:${col + 1}"
        }

        override fun toString(): String {
            return "Cursor (row: $row, col: $col)"
        }

    }

    internal class Selection(val start: Cursor, endInit: Cursor) {
        var end: Cursor by mutableStateOf(endInit)
        val min: Cursor get() = if (start <= end) start else end
        val max: Cursor get() = if (end >= start) end else start
        val isForward: Boolean get() = start <= end
        fun label(): String {
            return "${start.label()} -- ${end.label()}"
        }

        override fun toString(): String {
            val startStatus = if (start == min) "min" else "max"
            val endStatus = if (end == max) "max" else "min"
            return "Selection {start: $start [$startStatus], end: $end[$endStatus]}"
        }
    }

    internal var cursor: Cursor by mutableStateOf(Cursor(0, 0)); private set
    internal var selection: Selection? by mutableStateOf(null); private set
    internal var density: Float by mutableStateOf(initDensity)
    internal val verScroller = LazyColumn.createScrollState(lineHeight) { content.size }
    internal var horScroller = ScrollState(0)
    internal val horScrollerAdapter: ScrollbarAdapter = ScrollbarAdapter(horScroller)
    internal var textWidth by mutableStateOf(0.dp)
    private var mayDragSelect: Boolean by mutableStateOf(false)
    private var textAreaRect: Rect by mutableStateOf(Rect.Zero)
    private val lineCount: Int get() = content.size
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    internal fun mayIncreaseTextWidth(newRawWidth: Int) {
        val newWidth = Theme.toDP(newRawWidth, density)
        if (newWidth > textWidth) textWidth = newWidth
    }

    internal fun resetTextWidth() {
        textWidth = 0.dp
    }

    internal fun updateTextArea(rawRectangle: Rect) {
        textAreaRect = Rect(
            left = Theme.toDP(rawRectangle.left, density).value + horPadding.value,
            top = Theme.toDP(rawRectangle.top, density).value,
            right = Theme.toDP(rawRectangle.right, density).value - horPadding.value,
            bottom = Theme.toDP(rawRectangle.bottom, density).value
        )
    }

    private fun createCursor(x: Int, y: Int): Cursor {
        val relX = x - textAreaRect.left + Theme.toDP(horScroller.value, density).value
        val relY = y - textAreaRect.top + verScroller.offset.value
        val row = floor(relY / lineHeight.value).toInt().coerceIn(0, lineCount - 1)
        val offsetInLine = Offset(relX * density, (relY - (row * lineHeight.value)) * density)
        val col = rendering.get(row)?.getOffsetForPosition(offsetInLine) ?: 0
        return Cursor(row, col)
    }

    internal fun startDragSelection() {
        mayDragSelect = true
    }

    internal fun stopDragSelection() {
        mayDragSelect = false
    }

    internal fun mayUpdateDragSelection(x: Int, y: Int) {
        if (!mayDragSelect) return
        var newCursor = createCursor(x, y)
        val horScrollOffset = Theme.toDP(horScroller.value, density).value
        val lineNumberBorder = textAreaRect.left - horScrollOffset - horPadding.value
        if (x < lineNumberBorder && selection != null && newCursor >= selection!!.start) {
            newCursor = createCursor(x, y + lineHeight.value.toInt())
        }
        if (newCursor != cursor) updateCursor(newCursor, true)
    }

    internal fun updateSelection(newSelection: Selection?, mayScroll: Boolean = true) {
        selection = newSelection
        if (selection != null) updateCursor(selection!!.end, true, mayScroll)
        else updateStatus()
    }

    internal fun updateCursorIfOutOfSelection(x: Int, y: Int) {
        val newCursor = createCursor(x, y)
        if (selection == null || newCursor < selection!!.min || newCursor > selection!!.max) {
            updateCursor(newCursor, false)
        }
    }

    internal fun updateCursor(x: Int, y: Int, isSelecting: Boolean) {
        updateCursor(createCursor(x, y), isSelecting, false)
    }

    internal fun updateCursor(newCursor: Cursor, isSelecting: Boolean, mayScroll: Boolean = true) {
        if (isSelecting) {
            if (selection == null) selection = Selection(cursor, newCursor)
            else selection!!.end = newCursor
        } else selection = null
        cursor = newCursor
        if (mayScroll) mayScrollToCursor()
        updateStatus()
    }

    internal fun updateStatus() {
        GlobalState.status.publish(StatusManager.Key.TEXT_POSITION, selection?.label() ?: cursor.label())
    }

    private fun mayScrollToCursor() {
        fun mayScrollToCoordinate(x: Int, y: Int, padding: Int = 0) {
            val left = textAreaRect.left.toInt() + padding
            val right = textAreaRect.right.toInt() - padding
            val top = textAreaRect.top.toInt() + padding
            val bottom = textAreaRect.bottom.toInt() - padding
            if (x < left) coroutineScope.launch {
                horScroller.scrollTo(horScroller.value + ((x - left) * density).toInt())
            } else if (x > right) coroutineScope.launch {
                horScroller.scrollTo(horScroller.value + ((x - right) * density).toInt())
            }
            if (y < top) verScroller.updateOffsetBy((y - top).dp)
            else if (y > bottom) verScroller.updateOffsetBy((y - bottom).dp)
        }

        val cursorRect = rendering.get(cursor.row)?.let {
            it.getCursorRect(cursor.col.coerceAtMost(it.getLineEnd(0)))
        } ?: Rect(0f, 0f, 0f, 0f)
        val x = textAreaRect.left + Theme.toDP(cursorRect.left - horScroller.value, density).value
        val y = textAreaRect.top + (lineHeight.value * (cursor.row + 0.5f)) - verScroller.offset.value
        mayScrollToCoordinate(x.toInt(), y.toInt(), lineHeight.value.toInt())
    }

    internal fun moveCursorPrevByChar(isSelecting: Boolean = false) {
        if (!isSelecting && selection != null) updateCursor(selection!!.min, false)
        else {
            var newRow = cursor.row
            var newCol = cursor.col - 1
            if (newCol < 0) {
                newRow -= 1
                if (newRow < 0) {
                    newRow = 0
                    newCol = 0
                } else newCol = content[newRow].length
            }
            updateCursor(Cursor(newRow, newCol), isSelecting)
        }
    }

    internal fun moveCursorNextByChar(isSelecting: Boolean = false) {
        if (!isSelecting && selection != null) updateCursor(selection!!.max, false)
        else {
            var newRow = cursor.row
            var newCol = cursor.col + 1
            if (newCol > content[newRow].length) {
                newRow += 1
                if (newRow >= content.size) {
                    newRow = content.size - 1
                    newCol = content[newRow].length
                } else newCol = 0
            }
            updateCursor(Cursor(newRow, newCol), isSelecting)
        }
    }

    private fun wordBoundary(textLayout: TextLayoutResult, col: Int): TextRange {
        // TODO: https://github.com/JetBrains/compose-jb/issues/1762
        //       We can remove this function once the above issue is resolved
        if (content[cursor.row].isEmpty()) return TextRange(0, 0)
        val colSafe = col.coerceIn(0, (content[cursor.row].length - 1).coerceAtLeast(0))
        val boundary = textLayout.getWordBoundary(colSafe)
        val word = textLayout.multiParagraph.intrinsics.annotatedString.text.substring(boundary.start, boundary.end)
        val newStart = word.lastIndexOfAny(WORD_BREAK_CHARS, colSafe - boundary.start)
        val newEnd = word.indexOfAny(WORD_BREAK_CHARS, colSafe - boundary.start)
        return TextRange(
            if (newStart < 0) boundary.start else newStart + boundary.start + 1,
            if (newEnd < 0) boundary.end else newEnd + boundary.start
        )
    }

    internal fun moveCursorPrevByWord(isSelecting: Boolean = false) {
        val newCursor: Cursor = rendering.get(cursor.row)?.let {
            Cursor(cursor.row, getPrevWordOffset(it, cursor.col))
        } ?: Cursor(0, 0)
        updateCursor(newCursor, isSelecting)
    }

    private fun getPrevWordOffset(textLayout: TextLayoutResult, col: Int): Int {
        if (col < 0 || content[cursor.row].isEmpty()) return 0
        val newCol = wordBoundary(textLayout, col).start
        return if (newCol < col) newCol
        else getPrevWordOffset(textLayout, col - 1)
    }

    internal fun moveCursorNexBytWord(isSelecting: Boolean = false) {
        val newCursor: Cursor = rendering.get(cursor.row)?.let {
            Cursor(cursor.row, getNextWordOffset(it, cursor.col))
        } ?: Cursor(0, 0)
        updateCursor(newCursor, isSelecting)
    }

    private fun getNextWordOffset(textLayout: TextLayoutResult, col: Int): Int {
        if (col >= content[cursor.row].length) return content[cursor.row].length
        val newCol = wordBoundary(textLayout, col).end
        return if (newCol > col) newCol
        else getNextWordOffset(textLayout, col + 1)
    }

    internal fun moveCursorPrevByParagraph(isSelecting: Boolean = false) {
        if (cursor.col > 0) moveCursorToStartOfLine(isSelecting) // because we don't wrap text
        else updateCursor(Cursor((cursor.row - 1).coerceAtLeast(0), cursor.col), isSelecting)
    }

    internal fun moveCursorNextByParagraph(isSelecting: Boolean = false) {
        if (cursor.col < content[cursor.row].length) moveCursorToEndOfLine(isSelecting) // because we don't wrap text
        else {
            val newRow = (cursor.row + 1).coerceAtMost(content.size - 1)
            val newCol = cursor.col.coerceAtMost(content[newRow].length)
            updateCursor(Cursor(newRow, newCol), isSelecting)
        }
    }

    internal fun moveCursorToStartOfLine(isSelecting: Boolean = false) {
        updateCursor(Cursor(cursor.row, 0), isSelecting)
    }

    internal fun moveCursorToEndOfLine(isSelecting: Boolean = false) {
        updateCursor(Cursor(cursor.row, content[cursor.row].length), isSelecting)
    }

    internal fun moveCursorUpByLine(isSelecting: Boolean = false) {
        var newRow = cursor.row - 1
        var newCol = cursor.col
        if (newRow < 0) {
            newRow = 0
            newCol = 0
        } else newCol = newCol.coerceAtMost(content[newRow].length)
        updateCursor(Cursor(newRow, newCol), isSelecting)
    }

    internal fun moveCursorDownByLine(isSelecting: Boolean = false) {
        var newRow = cursor.row + 1
        var newCol = cursor.col
        if (newRow >= content.size) {
            newRow = content.size - 1
            newCol = content[newRow].length
        } else newCol = newCol.coerceAtMost(content[newRow].length)
        updateCursor(Cursor(newRow, newCol), isSelecting)
    }

    internal fun moveCursorUpByPage(isSelecting: Boolean = false) {
        val fullyVisibleLines = floor(textAreaRect.height / lineHeight.value).toInt()
        val newRow = (cursor.row - fullyVisibleLines).coerceAtLeast(0)
        val newCol = cursor.col.coerceAtMost(content[newRow].length)
        updateCursor(Cursor(newRow, newCol), isSelecting)
    }

    internal fun moveCursorDownByPage(isSelecting: Boolean = false) {
        val fullyVisibleLines = floor(textAreaRect.height / lineHeight.value).toInt()
        val newRow = (cursor.row + fullyVisibleLines).coerceAtMost(content.size - 1)
        val newCol = cursor.col.coerceAtMost(content[newRow].length)
        updateCursor(Cursor(newRow, newCol), isSelecting)
    }

    internal fun moveCursorToHome(isSelecting: Boolean = false) {
        updateCursor(Cursor(0, 0), isSelecting)
    }

    internal fun moveCursorToEnd(isSelecting: Boolean = false) {
        updateCursor(Cursor(content.size - 1, content.last().length), isSelecting)
    }

    internal fun selectAll() {
        updateSelection(Selection(Cursor(0, 0), Cursor(content.size - 1, content.last().length)), false)
    }

    internal fun selectWord() {
        val boundary = wordBoundary(rendering.get(cursor.row)!!, cursor.col)
        updateSelection(Selection(Cursor(cursor.row, boundary.start), Cursor(cursor.row, boundary.end)))
    }

    internal fun selectLine() {
        updateSelection(Selection(Cursor(cursor.row, 0), Cursor(cursor.row, content[cursor.row].length)))
    }

    internal fun selectNone() {
        updateSelection(null)
    }

    internal fun shiftSelection(selection: Selection, firstLine: Int, secondLine: Int) = Selection(
        Cursor(selection.start.row, (selection.start.col + firstLine).coerceAtLeast(0)),
        Cursor(selection.end.row, (selection.end.col + secondLine).coerceAtLeast(0))
    )

    internal fun expandSelection(selection: Selection): Selection = Selection(
        Cursor(selection.min.row, 0),
        Cursor(selection.max.row, content[selection.max.row].length)
    )

    internal fun selectedText(): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val textList = selectedTextLines()
        textList.forEach {
            builder.append(it)
            if (textList.size > 1) builder.append("\n")
        }
        return builder.toAnnotatedString()
    }

    internal fun selectedTextLines(): List<AnnotatedString> {
        if (selection == null) return listOf(AnnotatedString(""))
        val start = selection!!.min
        val end = selection!!.max
        val list = mutableListOf<AnnotatedString>()
        for (i in start.row..end.row) {
            val line = content[i]
            if (i == start.row && end.row > start.row) list.add(line.subSequence(start.col, line.length))
            else if (i == start.row) list.add(line.subSequence(start.col, end.col))
            else if (i == end.row) list.add(line.subSequence(0, end.col))
            else list.add(line)
        }
        return list
    }
}
