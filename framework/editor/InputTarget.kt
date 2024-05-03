/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.editor

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
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.framework.common.Util.getCursorRectSafely
import com.vaticle.typedb.studio.framework.common.Util.toDP
import com.vaticle.typedb.studio.framework.editor.common.GlyphLine
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.StatusService.Key.TEXT_CURSOR_POSITION
import kotlin.math.floor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class InputTarget constructor(
    private val content: SnapshotStateList<GlyphLine>,
    private val rendering: TextRendering,
    private val horPadding: Dp,
    lineHeightUnscaled: Dp,
    bottomSpace: Dp,
    initDensity: Float
) {

    companion object {
        // TODO: is this complete?
        private val WORD_BREAK_CHARS = charArrayOf(',', '.', ':', ';', '=', '(', ')', '{', '}')

        fun prefixSpaces(line: GlyphLine): Int {
            for (it in line.annotatedString.indices) if (line.annotatedString[it] != ' ') return it
            return line.length
        }

        fun suffixSpaces(line: GlyphLine): Int {
            for (it in line.annotatedString.indices.reversed()) if (line.annotatedString[it] != ' ') return line.length - 1 - it
            return line.length
        }
    }

    internal data class Cursor constructor(val row: Int, val col: Int, val lastCol: Int = col) : Comparable<Cursor> {

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

    internal class Selection constructor(val start: Cursor, endInit: Cursor) {
        var end: Cursor by mutableStateOf(endInit)
        val min: Cursor get() = if (start <= end) start else end
        val max: Cursor get() = if (end >= start) end else start
        val isForward: Boolean get() = start <= end
        fun label(): String {
            return "${start.label()} to ${end.label()}"
        }

        companion object {
            fun coverage(start: Selection, end: Selection): Selection {
                return if (start.min <= end.min && start.max >= end.max) start
                else if (end.min <= start.min && end.max >= start.max) end
                else if (start.min < end.min) Selection(start.min, end.max)
                else if (end.min < start.min) Selection(start.max, end.min)
                else throw IllegalStateException("Invalid selection coverage logic")
            }
        }

        override fun toString(): String {
            val startStatus = if (start == min) "min" else "max"
            val endStatus = if (end == max) "max" else "min"
            return "Selection {start: $start [$startStatus], end: $end [$endStatus]}"
        }
    }

    internal val cursor: Cursor get() = if (!stickToBottom) _cursor else end
    internal var selection: Selection? by mutableStateOf(null); private set
    internal var density: Float by mutableStateOf(initDensity)
    internal val verScroller = LazyLines.createScrollState(lineHeightUnscaled, bottomSpace) { content.size }
    internal var horScroller = ScrollState(0)
    internal val horScrollerAdapter = ScrollbarAdapter(horScroller)
    internal var textWidth by mutableStateOf(0.dp)
    internal val lineHeight: Dp get() = verScroller.lineHeight
    internal var stickToBottom
        get() = verScroller.stickToBottom
        set(value) {
            verScroller.stickToBottom = value
        }

    private var _cursor: Cursor by mutableStateOf(Cursor(0, 0)); private set
    private var mayDragSelectByChar: Boolean by mutableStateOf(false)
    private var mayDragSelectByWord: Boolean by mutableStateOf(false)
    private var mayDragSelectByLine: Boolean by mutableStateOf(false)
    private var mayDragSelectByLineNumber: Boolean by mutableStateOf(false)
    private var selectionDragStart: Selection? by mutableStateOf(null)
    private var textAreaBounds: Rect by mutableStateOf(Rect.Zero)
    private val lineNumberBorder: Float get() = textAreaBounds.left - horPadding.value
    private val lineCount: Int get() = content.size
    private val end: Cursor get() = Cursor(content.size - 1, content.last().length)
    private val coroutines = CoroutineScope(Dispatchers.Default)

    internal fun mayIncreaseTextWidth(newRawWidth: Int) {
        val newWidth = toDP(newRawWidth, density)
        if (newWidth > textWidth) textWidth = newWidth
    }

    internal fun resetTextWidth() {
        textWidth = 0.dp
    }

    internal fun updateBounds(rawRectangle: Rect) {
        textAreaBounds = Rect(
            left = toDP(rawRectangle.left, density).value + horPadding.value,
            top = toDP(rawRectangle.top, density).value,
            right = toDP(rawRectangle.right, density).value - horPadding.value,
            bottom = toDP(rawRectangle.bottom, density).value
        )
    }

    private fun createCursor(x: Int, y: Int): Cursor {
        val relX = x - textAreaBounds.left + toDP(horScroller.value, density).value
        val relY = y - textAreaBounds.top + verScroller.offset.value
        val row = floor(relY / lineHeight.value).toInt().coerceIn(0, lineCount - 1)
        val offsetInLine = Offset(relX * density, (relY - (row * lineHeight.value)) * density)
        val charOffset = rendering.get(row)?.getOffsetForPosition(offsetInLine) ?: 0
        val col = content[row].charToGlyphOffset(charOffset)
        return Cursor(row, col)
    }

    internal fun stopDragSelection() {
        mayDragSelectByChar = false
        mayDragSelectByWord = false
        mayDragSelectByLine = false
        mayDragSelectByLineNumber = false
        selectionDragStart = null
    }

    internal fun dragSelection(x: Int, y: Int) {
        if (mayDragSelectByChar) dragSelectionByChar(x, y)
        else if (mayDragSelectByWord) dragSelectionByWord(x, y)
        else if (mayDragSelectByLine) dragSelectionByLine(x, y)
        else if (mayDragSelectByLineNumber) dragSelectionByLineNumber(x, y)
    }

    private fun dragSelectionByChar(x: Int, y: Int) {
        val newCursor = createCursor(x, y)
        if (newCursor != cursor) updateCursor(newCursor, true)
    }

    private fun dragSelectionByWord(x: Int, y: Int) = selectionOfWord(createCursor(x, y))?.let {
        updateSelection(Selection.coverage(selectionDragStart!!, it))
    }

    private fun dragSelectionByLine(x: Int, y: Int) = updateSelection(
        Selection.coverage(selectionDragStart!!, selectionOfLineAndBreak(createCursor(x, y)))
    )

    private fun dragSelectionByLineNumber(x: Int, y: Int) = updateCursor(
        createCursor(x, y + lineHeight.value.toInt()), true
    )

    internal fun updatePosition(newPosition: Either<Cursor, Selection>) = newPosition.apply(
        { updateCursor(it, false) },
        { updateSelection(it) }
    )

    internal fun updateSelection(newSelection: Selection?, mayScroll: Boolean = true) {
        selection = newSelection
        if (selection != null) updateCursor(selection!!.end, true, mayScroll)
        else publishStatus()
    }

    internal fun updateCursorIfOutOfSelection(x: Int, y: Int) {
        val newCursor = createCursor(x, y)
        if (selection == null || newCursor < selection!!.min || newCursor > selection!!.max) {
            updateCursor(newCursor, false)
        }
    }

    internal fun mayUpdateCursor(x: Int, y: Int, isSelecting: Boolean) {
        if (x <= textAreaBounds.right) {
            updateCursor(createCursor(x, y), isSelecting, false)
            if (x > lineNumberBorder) mayDragSelectByChar = true
            else mayDragSelectByLineNumber = true
        }
    }

    internal fun updateCursor(newCursor: Cursor, isSelecting: Boolean, mayScroll: Boolean = true) {
        if (isSelecting) {
            if (selection == null) selection = Selection(cursor, newCursor)
            else selection!!.end = newCursor
        } else selection = null
        _cursor = newCursor
        if (mayScroll) mayScrollToCursor()
        publishStatus()
    }

    internal fun publishStatus() = Service.status.publish(
        key = TEXT_CURSOR_POSITION, status = selection?.label() ?: cursor.label()
    )

    internal fun clearStatus() = Service.status.clear(TEXT_CURSOR_POSITION)

    private fun mayScrollToCursor() {
        fun mayScrollToCoordinate(x: Int, y: Int, padding: Dp = 0.dp) {
            val left = textAreaBounds.left.toInt() + padding.value.toInt()
            val right = textAreaBounds.right.toInt() - padding.value.toInt()
            val top = textAreaBounds.top.toInt()
            val bottom = textAreaBounds.bottom.toInt()
            if (x < left) coroutines.launch {
                horScroller.scrollTo(horScroller.value + ((x - left) * density).toInt())
            } else if (x > right) coroutines.launch {
                horScroller.scrollTo(horScroller.value + ((x - right) * density).toInt())
            }
            if (y <= top) verScroller.updateOffsetBy((y - top).dp - padding)
            else if (y >= bottom) verScroller.updateOffsetBy((y - bottom).dp + padding)
        }
        val cursorRect = rendering.get(cursor.row)?.getCursorRectSafely(content[cursor.row].glyphToCharOffset(cursor.col)) ?: Rect(0f, 0f, 0f, 0f)
        val x = textAreaBounds.left + toDP(cursorRect.left - horScroller.value, density).value
        val y = textAreaBounds.top + (lineHeight.value * (cursor.row + 0.5f)) - verScroller.offset.value
        mayScrollToCoordinate(x.toInt(), y.toInt(), lineHeight)
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

    private fun wordBoundary(textLayout: TextLayoutResult, row: Int, col: Int): TextRange {
        // TODO: https://github.com/JetBrains/compose-jb/issues/1762
        //       We can remove this function once the above issue is resolved
        if (content[row].isEmpty()) return TextRange(0, 0)
        val colSafe = col.coerceIn(0, (content[row].length - 1).coerceAtLeast(0))
        val boundary = textLayout.getWordBoundary(colSafe)
        val start = content[row].charToGlyphOffset(boundary.start)
        val end = content[row].charToGlyphOffset(boundary.end)
        val word = content[row].subSequenceSafely(start, end).annotatedString
        val newStart = word.lastIndexOfAny(WORD_BREAK_CHARS, colSafe - start)
        val newEnd = word.indexOfAny(WORD_BREAK_CHARS, colSafe - start)
        return TextRange(
            if (newStart < 0) start else newStart + start + 1,
            if (newEnd < 0) end else newEnd + start
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
        val newCol = wordBoundary(textLayout, cursor.row, col).start
        return if (newCol < col) newCol else getPrevWordOffset(textLayout, col - 1)
    }

    internal fun moveCursorNextByWord(isSelecting: Boolean = false) {
        val newCursor: Cursor = rendering.get(cursor.row)?.let {
            Cursor(cursor.row, getNextWordOffset(it, cursor.col))
        } ?: Cursor(0, 0)
        updateCursor(newCursor, isSelecting)
    }

    private fun getNextWordOffset(textLayout: TextLayoutResult, col: Int): Int {
        if (col >= content[cursor.row].length) return content[cursor.row].length
        val newCol = wordBoundary(textLayout, cursor.row, col).end
        return if (newCol > col) newCol else getNextWordOffset(textLayout, col + 1)
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
        val startOfText = prefixSpaces(content[cursor.row])
        val newCol = if (cursor.col > startOfText) startOfText else 0
        updateCursor(Cursor(cursor.row, newCol), isSelecting)
    }

    internal fun moveCursorToEndOfLine(isSelecting: Boolean = false) {
        val length = content[cursor.row].length
        val endOfText = length - suffixSpaces(content[cursor.row])
        val newCol = if (cursor.col < endOfText) endOfText else length
        updateCursor(Cursor(cursor.row, newCol), isSelecting)
    }

    internal fun moveCursorUpByLine(isSelecting: Boolean = false) {
        if (cursor.row != 0) {
            val newRow = cursor.row - 1
            val newCol = cursor.lastCol.coerceAtMost(content[newRow].length)
            updateCursor(Cursor(newRow, newCol, lastCol = cursor.lastCol), isSelecting)
        }
    }

    internal fun moveCursorDownByLine(isSelecting: Boolean = false) {
        if (cursor.row != content.size - 1) {
            val newRow = cursor.row + 1
            val newCol = cursor.lastCol.coerceAtMost(content[newRow].length)
            updateCursor(Cursor(newRow, newCol, lastCol = cursor.lastCol), isSelecting)
        }
    }

    internal fun moveCursorUpByPage(isSelecting: Boolean = false) {
        val fullyVisibleLines = floor(textAreaBounds.height / lineHeight.value).toInt()
        val newRow = (cursor.row - fullyVisibleLines).coerceAtLeast(0)
        val newCol = cursor.col.coerceAtMost(content[newRow].length)
        updateCursor(Cursor(newRow, newCol), isSelecting)
    }

    internal fun moveCursorDownByPage(isSelecting: Boolean = false) {
        val fullyVisibleLines = floor(textAreaBounds.height / lineHeight.value).toInt()
        val newRow = (cursor.row + fullyVisibleLines).coerceAtMost(content.size - 1)
        val newCol = cursor.col.coerceAtMost(content[newRow].length)
        updateCursor(Cursor(newRow, newCol), isSelecting)
    }

    internal fun moveCursorToStartOfContent(isSelecting: Boolean = false, mayScroll: Boolean = true) = updateCursor(
        Cursor(0, 0), isSelecting, mayScroll
    )

    internal fun moveCursorToEndOfContent(isSelecting: Boolean = false, mayScroll: Boolean = true) = updateCursor(
        end, isSelecting, mayScroll
    )

    internal fun selectAll() = updateSelection(
        Selection(Cursor(0, 0), Cursor(content.size - 1, content.last().length)), false
    )

    internal fun selectNone() = updateSelection(null)

    internal fun maySelectWord(x: Int) {
        if (x > lineNumberBorder) {
            selectWord()
            selectionDragStart = selection
            mayDragSelectByWord = true
        }
    }

    private fun selectWord() = updateSelection(selectionOfWord(cursor), mayScroll = false)
    private fun selectLineAndBreak() = updateSelection(selectionOfLineAndBreak(cursor))

    internal fun maySelectLineAndBreak(x: Int) {
        if (x > lineNumberBorder) {
            selectLineAndBreak()
            selectionDragStart = selection
            mayDragSelectByLine = true
        }
    }

    private fun selectionOfWord(cursor: Cursor): Selection? = rendering.get(cursor.row)?.let {
        val boundary = wordBoundary(it, cursor.row, cursor.col)
        Selection(Cursor(cursor.row, boundary.start), Cursor(cursor.row, boundary.end))
    }

    internal fun selectionOfLineAndBreak(cursor: Cursor): Selection {
        val endCursor = if (cursor.row < content.size - 1) Cursor(cursor.row + 1, 0)
        else Cursor(cursor.row, content[cursor.row].length)
        return Selection(Cursor(cursor.row, 0), endCursor)
    }

    internal fun selectionOfLineContent(selection: Selection) = Selection(
        Cursor(selection.min.row, 0),
        Cursor(selection.max.row, content[selection.max.row].length)
    )

    internal fun selectionOfPreviousLineAndBreak(row: Int): Selection? = when {
        row < 1 -> null
        else -> Selection(Cursor(row - 1, 0), Cursor(row, 0))
    }

    internal fun selectionOfNextBreakAndLine(row: Int): Selection? = when {
        row > content.size - 2 -> null
        else -> Selection(Cursor(row, content[row].length), Cursor(row + 1, content[row + 1].length))
    }

    internal fun selectionShiftedBy(selection: Selection, startShift: Int, endShift: Int) = Selection(
        Cursor(selection.start.row, (selection.start.col + startShift).coerceAtLeast(0)),
        Cursor(selection.end.row, (selection.end.col + endShift).coerceAtLeast(0))
    )

    internal fun selectedText(): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val textList = selectedTextLines()
        textList.forEachIndexed { i, text ->
            builder.append(text.annotatedString)
            if (textList.size > 1 && i < textList.size - 1) builder.append("\n")
        }
        return builder.toAnnotatedString()
    }

    internal fun selectedTextLines(): List<GlyphLine> {
        if (selection == null) return listOf(GlyphLine(""))
        val start = selection!!.min
        val end = selection!!.max
        val list = mutableListOf<GlyphLine>()
        for (i in start.row..end.row) {
            val line = content[i]
            if (i == start.row && end.row > start.row) list.add(line.subSequenceSafely(start.col, line.length))
            else if (i == start.row) list.add(line.subSequenceSafely(start.col, end.col))
            else if (i == end.row) list.add(line.subSequenceSafely(0, end.col))
            else list.add(line)
        }
        return list
    }
}
