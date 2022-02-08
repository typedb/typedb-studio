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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.common.Property.FileType.TYPEQL
import com.vaticle.typedb.studio.view.editor.InputTarget.Cursor
import com.vaticle.typedb.studio.view.editor.InputTarget.Selection
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.COPY
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.CUT
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.DELETE_END_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.DELETE_NEXT_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.DELETE_NEXT_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.DELETE_PREV_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.DELETE_PREV_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.DELETE_START_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.EMOJI_WINDOW
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.ENTER
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.ENTER_SHIFT
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_DOWN_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_DOWN_PAGE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_END
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_END_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_HOME
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_LEFT_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_LEFT_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_LEFT_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_NEXT_PARAGRAPH
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_PREV_PARAGRAPH
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_RIGHT_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_RIGHT_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_RIGHT_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_START_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_UP_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.MOVE_CURSOR_UP_PAGE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.PASTE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.REDO
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_ALL
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_DOWN_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_DOWN_PAGE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_END
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_END_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_HOME
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_LEFT_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_LEFT_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_LEFT_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_NEXT_PARAGRAPH
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_NONE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_PREV_PARAGRAPH
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_RIGHT_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_RIGHT_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_RIGHT_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_START_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_UP_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.SELECT_UP_PAGE
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.TAB
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.TAB_SHIFT
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.UNDO
import com.vaticle.typedb.studio.view.editor.KeyMapper.GenericCommand
import com.vaticle.typedb.studio.view.editor.KeyMapper.GenericCommand.ESCAPE
import com.vaticle.typedb.studio.view.editor.TextChange.Deletion
import com.vaticle.typedb.studio.view.editor.TextChange.Insertion
import com.vaticle.typedb.studio.view.editor.TextChange.ReplayType
import com.vaticle.typedb.studio.view.typeql.TypeQLHighlighter
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class TextProcessor(
    private val content: SnapshotStateList<AnnotatedString>,
    private val fileType: Property.FileType,
    private val rendering: TextRendering,
    private val finder: TextFinder,
    private val target: InputTarget,
    internal val clipboard: ClipboardManager,
) {

    @OptIn(ExperimentalTime::class)
    companion object {
        private const val TAB_SIZE = 4
        private const val UNDO_LIMIT = 1_000
        internal val CHANGE_BATCH_DELAY = Duration.milliseconds(400)

        fun annotate(strings: List<String>, fileType: Property.FileType): List<AnnotatedString> {
            return strings.map { annotate(it, fileType) }
        }

        private fun annotate(string: String, fileType: Property.FileType): AnnotatedString {
            return when (fileType) {
                TYPEQL -> TypeQLHighlighter.annotate(string)
                else -> AnnotatedString(string)
            }
        }
    }

    internal var version by mutableStateOf(0)
    private var undoStack: ArrayDeque<TextChange> = ArrayDeque()
    private var redoStack: ArrayDeque<TextChange> = ArrayDeque()
    private var changeQueue: BlockingQueue<TextChange> = LinkedBlockingQueue()
    private var changeCount: AtomicInteger = AtomicInteger(0)
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    internal fun execute(command: GenericCommand): Boolean {
        return when (command) {
            ESCAPE -> target.selection?.let { target.selectNone(); true } ?: false
        }
    }

    internal fun execute(command: EditorCommand): Boolean {
        when (command) {
            MOVE_CURSOR_LEFT_CHAR -> target.moveCursorPrevByChar() // because we only display left to right
            MOVE_CURSOR_RIGHT_CHAR -> target.moveCursorNextByChar() // because we only display left to right
            MOVE_CURSOR_LEFT_WORD -> target.moveCursorPrevByWord() // because we only display left to right
            MOVE_CURSOR_RIGHT_WORD -> target.moveCursorNexBytWord() // because we only display left to right
            MOVE_CURSOR_PREV_PARAGRAPH -> target.moveCursorPrevByParagraph()
            MOVE_CURSOR_NEXT_PARAGRAPH -> target.moveCursorNextByParagraph()
            MOVE_CURSOR_LEFT_LINE -> target.moveCursorToStartOfLine() // because we only display left to right
            MOVE_CURSOR_RIGHT_LINE -> target.moveCursorToEndOfLine() // because we only display left to right
            MOVE_CURSOR_START_LINE -> target.moveCursorToStartOfLine()
            MOVE_CURSOR_END_LINE -> target.moveCursorToEndOfLine()
            MOVE_CURSOR_UP_LINE -> target.moveCursorUpByLine()
            MOVE_CURSOR_DOWN_LINE -> target.moveCursorDownByLine()
            MOVE_CURSOR_UP_PAGE -> target.moveCursorUpByPage()
            MOVE_CURSOR_DOWN_PAGE -> target.moveCursorDownByPage()
            MOVE_CURSOR_HOME -> target.moveCursorToHome()
            MOVE_CURSOR_END -> target.moveCursorToEnd()
            SELECT_LEFT_CHAR -> target.moveCursorPrevByChar(true) // because we only display left to right
            SELECT_RIGHT_CHAR -> target.moveCursorNextByChar(true) // because we only display left to right
            SELECT_LEFT_WORD -> target.moveCursorPrevByWord(true) // because we only display left to right
            SELECT_RIGHT_WORD -> target.moveCursorNexBytWord(true) // because we only display left to right
            SELECT_PREV_PARAGRAPH -> target.moveCursorPrevByParagraph(true)
            SELECT_NEXT_PARAGRAPH -> target.moveCursorNextByParagraph(true)
            SELECT_LEFT_LINE -> target.moveCursorToStartOfLine(true) // because we only display left to right
            SELECT_RIGHT_LINE -> target.moveCursorToEndOfLine(true) // because we only display left to right
            SELECT_START_LINE -> target.moveCursorToStartOfLine(true)
            SELECT_END_LINE -> target.moveCursorToEndOfLine(true)
            SELECT_UP_LINE -> target.moveCursorUpByLine(true)
            SELECT_DOWN_LINE -> target.moveCursorDownByLine(true)
            SELECT_UP_PAGE -> target.moveCursorUpByPage(true)
            SELECT_DOWN_PAGE -> target.moveCursorDownByPage(true)
            SELECT_HOME -> target.moveCursorToHome(true)
            SELECT_END -> target.moveCursorToEnd(true)
            SELECT_ALL -> target.selectAll()
            SELECT_NONE -> target.selectNone()
            DELETE_PREV_CHAR -> deleteSelectionOr { target.moveCursorPrevByChar(true); deleteSelection() }
            DELETE_NEXT_CHAR -> deleteSelectionOr { target.moveCursorNextByChar(true); deleteSelection() }
            DELETE_PREV_WORD -> deleteSelectionOr { target.moveCursorPrevByWord(true); deleteSelection() }
            DELETE_NEXT_WORD -> deleteSelectionOr { target.moveCursorNexBytWord(true); deleteSelection() }
            DELETE_START_LINE -> deleteSelectionOr { target.moveCursorToStartOfLine(true); deleteSelection() }
            DELETE_END_LINE -> deleteSelectionOr { target.moveCursorToEndOfLine(true); deleteSelection() }
            TAB -> indentTab()
            TAB_SHIFT -> outdentTab()
            ENTER, ENTER_SHIFT -> insertNewLine()
            CUT -> cut()
            COPY -> copy()
            PASTE -> paste()
            UNDO -> undo()
            REDO -> redo()
            EMOJI_WINDOW -> {
                // TODO: https://github.com/JetBrains/compose-jb/issues/1754
                // androidx.compose.foundation.text.showCharacterPalette()
            }
        }
        return true
    }

    internal fun cut() {
        if (target.selection == null) return
        copy()
        deleteSelection()
    }

    internal fun copy() {
        if (target.selection == null) return
        clipboard.setText(target.selectedText())
    }

    internal fun paste() {
        clipboard.getText()?.let { if (it.text.isNotEmpty()) insertText(it.text) }
    }

    internal fun replaceCurrentFound(text: String) {
        if (!finder.hasMatches) return
        val oldPosition = finder.position
        if (finder.findCurrent() != target.selection) target.updateSelection(finder.findCurrent(), mayScroll = false)
        insertText(text, recomputeFinder = true)
        finder.trySetPosition(oldPosition)
        target.updateSelection(finder.findCurrent())
    }

    internal fun replaceAllFound(text: String) {
        if (!finder.hasMatches) return
        var next: Selection?
        target.updateCursor(Cursor(0, 0), isSelecting = false, mayScroll = false)
        while (finder.recomputeNextMatch(target.cursor).let { next = it; next != null }) {
            target.updateSelection(next)
            insertText(text, recomputeFinder = false)
        }
        finder.mayRecomputeAllMatches()
    }

    private fun indent(strings: List<AnnotatedString>, spaces: Int): List<AnnotatedString> {
        return strings.map {
            if (spaces > 0) AnnotatedString(" ".repeat(spaces)) + it
            else if (spaces < 0) it.subSequence(spaces.coerceAtMost(prefixSpaces(it)), it.length)
            else it
        }
    }

    private fun prefixSpaces(line: AnnotatedString): Int {
        for (it in line.indices) if (line[it] != ' ') return it
        return line.length
    }

    private fun deletionOperation(): Deletion {
        assert(target.selection != null)
        return Deletion(target.selection!!.min, target.selectedTextLines(), target.selection)
    }

    private fun deleteSelectionOr(elseFn: () -> Unit) {
        if (target.selection != null) deleteSelection()
        else elseFn()
    }

    private fun deleteSelection() {
        if (target.selection == null) return
        applyOriginal(TextChange(deletionOperation()))
    }

    private fun outdentTab() {
        val oldSelection = target.selection
        val oldCursor = target.cursor
        val newSelection = oldSelection?.let { target.expandSelection(it) }
            ?: target.expandSelection(oldCursor.toSelection())
        target.updateSelection(newSelection)
        val oldTextLines = target.selectedTextLines()
        val newTextLines = indent(oldTextLines, -TAB_SIZE)
        val firstLineShift = newTextLines.first().length - oldTextLines.first().length
        val lastLineShift = newTextLines.last().length - oldTextLines.last().length
        val newPosition: Either<Cursor, Selection> = oldSelection?.let {
            val startCursorShift = if (it.isForward) firstLineShift else lastLineShift
            val endCursorShift = if (it.isForward) lastLineShift else firstLineShift
            Either.second(target.shiftSelection(it, startCursorShift, endCursorShift))
        } ?: Either.first(Cursor(oldCursor.row, (oldCursor.col + firstLineShift).coerceAtLeast(0)))
        insertText(newTextLines, newPosition)
    }

    private fun indentTab() {
        val selection = target.selection
        val cursor = target.cursor
        if (selection == null) insertText(" ".repeat(TAB_SIZE - prefixSpaces(content[cursor.row]) % TAB_SIZE))
        else {
            val newSelection = target.shiftSelection(selection, TAB_SIZE, TAB_SIZE)
            target.updateSelection(target.expandSelection(selection))
            insertText(indent(target.selectedTextLines(), TAB_SIZE), Either.second(newSelection))
        }
    }

    private fun insertNewLine() {
        val line = content[target.cursor.row]
        val tabs = floor(prefixSpaces(line).toDouble() / TAB_SIZE).toInt()
        insertText("\n" + " ".repeat(TAB_SIZE * tabs))
    }

    private fun asAnnotatedLines(string: String): List<AnnotatedString> {
        return if (string.isEmpty()) listOf() else string.split("\n").map { AnnotatedString(it) }
    }

    internal fun insertText(string: String): Boolean {
        insertText(asAnnotatedLines(string), newPosition = null)
        return true
    }

    private fun insertText(string: String, recomputeFinder: Boolean) {
        insertText(asAnnotatedLines(string), newPosition = null, recomputeFinder)
    }

    private fun insertText(
        strings: List<AnnotatedString>,
        newPosition: Either<Cursor, Selection>?,
        recomputeFinder: Boolean = true
    ) {
        val operations = mutableListOf<TextChange.Operation>()
        if (target.selection != null) operations.add(deletionOperation())
        if (strings.isNotEmpty()) operations.add(Insertion(target.selection?.min ?: target.cursor, strings))
        applyOriginal(TextChange(operations), newPosition, recomputeFinder)
    }

    private fun undo() {
        drainAndBatchOriginalChanges()
        if (undoStack.isNotEmpty()) applyReplay(undoStack.removeLast(), ReplayType.UNDO)
    }

    private fun redo() {
        if (redoStack.isNotEmpty()) applyReplay(redoStack.removeLast(), ReplayType.REDO)
    }

    private fun applyOriginal(
        change: TextChange, newPosition: Either<Cursor, Selection>? = null, recomputeFinder: Boolean = true
    ) {
        assert(newPosition == null || !recomputeFinder)
        applyChange(change, recomputeFinder)
        if (newPosition != null) when {
            newPosition.isFirst -> target.updateCursor(newPosition.first(), false)
            newPosition.isSecond -> target.updateSelection(newPosition.second())
        }
        queueChangeAndReannotation(change)
    }

    private fun applyReplay(change: TextChange, replayType: ReplayType) {
        applyChange(change)
        val newTarget = change.target()
        when {
            newTarget.isFirst -> target.updateCursor(newTarget.first(), false)
            newTarget.isSecond -> target.updateSelection(newTarget.second())
        }
        when (replayType) {
            ReplayType.UNDO -> redoStack.addLast(change.invert())
            ReplayType.REDO -> undoStack.addLast(change.invert())
        }
    }

    private fun applyChange(change: TextChange, recomputeFinder: Boolean = true) {
        change.operations.forEach {
            when (it) {
                is Deletion -> applyDeletion(it)
                is Insertion -> applyInsertion(it)
            }
        }
        version++
        target.resetTextWidth()
        if (recomputeFinder) finder.mayRecomputeAllMatches()
    }

    private fun applyDeletion(deletion: Deletion) {
        val start = deletion.selection().min
        val end = deletion.selection().max
        val prefix = content[start.row].subSequence(0, start.col)
        val suffix = content[end.row].subSequence(end.col, content[end.row].length)
        content[start.row] = prefix + suffix
        if (end.row > start.row) {
            rendering.removeRange(start.row + 1, end.row + 1)
            content.removeRange(start.row + 1, end.row + 1)
        }
        target.updateCursor(deletion.selection().min, false)
    }

    private fun applyInsertion(insertion: Insertion) {
        val cursor = insertion.cursor
        val prefix = content[cursor.row].subSequence(0, cursor.col)
        val suffix = content[cursor.row].subSequence(cursor.col, content[cursor.row].length)
        val texts = insertion.text.toMutableList()
        texts[0] = prefix + texts[0]
        texts[texts.size - 1] = texts[texts.size - 1] + suffix

        content[cursor.row] = texts[0]
        if (texts.size > 1) {
            content.addAll(cursor.row + 1, texts.subList(1, texts.size))
            rendering.addNew(cursor.row + 1, texts.size - 1)
        }
        target.updateCursor(insertion.selection().max, false)
    }

    @OptIn(ExperimentalTime::class)
    private fun queueChangeAndReannotation(change: TextChange) {
        redoStack.clear()
        changeQueue.put(change)
        changeCount.incrementAndGet()
        coroutineScope.launch {
            delay(CHANGE_BATCH_DELAY)
            if (changeCount.decrementAndGet() == 0) {
                val changes = drainAndBatchOriginalChanges()
                changes?.let { reannotate(it.lines()) }
            }
        }
    }

    @Synchronized
    private fun drainAndBatchOriginalChanges(): TextChange? {
        var batchedChanges: TextChange? = null
        if (changeQueue.isNotEmpty()) {
            val changes = mutableListOf<TextChange>()
            changeQueue.drainTo(changes)
            batchedChanges = TextChange.merge(changes)
            undoStack.addLast(batchedChanges.invert())
            while (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
        }
        return batchedChanges
    }

    private fun reannotate(lines: IntRange) {
        lines.forEach { content[it] = annotate(content[it].text, fileType) }
    }
}