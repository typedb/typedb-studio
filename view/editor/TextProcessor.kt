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
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.view.editor.InputTarget.Cursor
import com.vaticle.typedb.studio.view.editor.InputTarget.Selection
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.CHARACTER_PALETTE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.CLOSE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.COPY
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.CUT
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.DELETE_END_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.DELETE_NEXT_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.DELETE_NEXT_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.DELETE_PREV_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.DELETE_PREV_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.DELETE_START_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.DELETE_TAB
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.INSERT_NEW_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.INSERT_TAB
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_DOWN_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_DOWN_PAGE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_END
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_END_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_HOME
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_LEFT_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_LEFT_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_LEFT_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_NEXT_PARAGRAPH
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_PREV_PARAGRAPH
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_RIGHT_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_RIGHT_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_RIGHT_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_START_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_UP_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.MOVE_CURSOR_UP_PAGE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.PASTE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.REDO
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_ALL
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_DOWN_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_DOWN_PAGE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_END
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_END_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_HOME
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_LEFT_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_LEFT_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_LEFT_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_NEXT_PARAGRAPH
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_NONE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_PREV_PARAGRAPH
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_RIGHT_CHAR
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_RIGHT_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_RIGHT_WORD
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_START_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_UP_LINE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.SELECT_UP_PAGE
import com.vaticle.typedb.studio.view.editor.KeyMapper.Command.UNDO
import com.vaticle.typedb.studio.view.editor.TextChange.Deletion
import com.vaticle.typedb.studio.view.editor.TextChange.Insertion
import com.vaticle.typedb.studio.view.editor.TextChange.Type
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class TextProcessor(
    private val file: File,
    private val rendering: TextRendering,
    private val target: InputTarget,
    internal val clipboard: ClipboardManager,
    internal val onClose: () -> Unit
) {

    @OptIn(ExperimentalTime::class)
    companion object {
        private const val TAB_SIZE = 4
        private const val UNDO_LIMIT = 1_000
        private val CHANGE_BATCH_DELAY = Duration.milliseconds(500)
    }

    internal val content: SnapshotStateList<String> get() = file.content
    internal var version by mutableStateOf(0)
    private var undoStack: ArrayDeque<TextChange> = ArrayDeque()
    private var redoStack: ArrayDeque<TextChange> = ArrayDeque()
    private var changeQueue: BlockingQueue<TextChange> = LinkedBlockingQueue()
    private var changeCount: AtomicInteger = AtomicInteger(0)
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    internal fun process(command: KeyMapper.Command) {
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
            DELETE_TAB -> deleteTab()
            INSERT_TAB -> insertTab()
            INSERT_NEW_LINE -> insertNewLine()
            CUT -> cut()
            COPY -> copy()
            PASTE -> paste()
            UNDO -> undo()
            REDO -> redo()
            CLOSE -> onClose()
            CHARACTER_PALETTE -> {
                // TODO: https://github.com/JetBrains/compose-jb/issues/1754
                // androidx.compose.foundation.text.showCharacterPalette()
            }
        }
    }

    internal fun cut() {
        if (target.selection == null) return
        copy()
        deleteSelection()
    }

    internal fun copy() {
        if (target.selection == null) return
        clipboard.setText(AnnotatedString(target.selectedText()))
    }

    internal fun paste() {
        clipboard.getText()?.let { if (it.text.isNotEmpty()) insertText(it.text) }
    }

    private fun indent(string: String, spaces: Int): String {
        return string.split("\n").stream().map {
            if (spaces > 0) " ".repeat(spaces) + it
            else if (spaces < 0) it.removePrefix(" ".repeat((-spaces).coerceAtMost(prefixSpaces(it))))
            else it
        }.collect(Collectors.joining("\n"))
    }

    private fun prefixSpaces(line: String): Int {
        for (it in line.indices) if (line[it] != ' ') return it
        return line.length
    }

    private fun deletionOperation(): Deletion {
        assert(target.selection != null)
        return Deletion(target.selection!!.min, target.selectedText(), target.selection)
    }

    private fun deleteSelectionOr(elseFn: () -> Unit) {
        if (target.selection != null) deleteSelection()
        else elseFn()
    }

    private fun deleteSelection() {
        if (target.selection == null) return
        apply(TextChange(deletionOperation()), Type.ORIGINAL)
    }

    private fun deleteTab() {
        val oldSelection = target.selection
        val oldCursor = target.cursor
        val newSelection = oldSelection?.let { target.expandSelection(it) }
            ?: target.expandSelection(oldCursor.toSelection())
        target.updateSelection(newSelection)
        val oldText = target.selectedText()
        val newText = indent(oldText, -TAB_SIZE)
        val oldTextLines = oldText.split("\n")
        val newTextLines = newText.split("\n")
        val firstLineShift = newTextLines.first().length - oldTextLines.first().length
        val lastLineShift = newTextLines.last().length - oldTextLines.last().length
        val newPosition: Either<Cursor, Selection> = oldSelection?.let {
            val startCursorShift = if (it.isForward) firstLineShift else lastLineShift
            val endCursorShift = if (it.isForward) lastLineShift else firstLineShift
            Either.second(target.shiftSelection(it, startCursorShift, endCursorShift))
        } ?: Either.first(Cursor(oldCursor.row, (oldCursor.col + firstLineShift).coerceAtLeast(0)))
        insertText(newText, newPosition)
    }

    private fun insertTab() {
        val selection = target.selection
        val cursor = target.cursor
        if (selection == null) insertText(" ".repeat(TAB_SIZE - prefixSpaces(content[cursor.row]) % TAB_SIZE))
        else {
            val newSelection = target.shiftSelection(selection, TAB_SIZE, TAB_SIZE)
            target.updateSelection(target.expandSelection(selection))
            insertText(indent(target.selectedText(), TAB_SIZE), Either.second(newSelection))
        }
    }

    private fun insertNewLine() {
        val line = content[target.cursor.row]
        val tabs = floor(prefixSpaces(line).toDouble() / TAB_SIZE).toInt()
        insertText("\n" + " ".repeat(TAB_SIZE * tabs))
    }

    internal fun insertText(string: String, newPosition: Either<Cursor, Selection>? = null) {
        assert(string.isNotEmpty())
        val operations = mutableListOf<TextChange.Operation>()
        if (target.selection != null) operations.add(deletionOperation())
        operations.add(Insertion(target.selection?.min ?: target.cursor, string))
        apply(TextChange(operations), Type.ORIGINAL, newPosition)
    }

    private fun undo() {
        drainAndBatchOriginalChanges()
        if (undoStack.isNotEmpty()) apply(undoStack.removeLast(), Type.UNDO)
    }

    private fun redo() {
        if (redoStack.isNotEmpty()) apply(redoStack.removeLast(), Type.REDO)
    }

    private fun apply(change: TextChange, type: Type, newPosition: Either<Cursor, Selection>? = null) {
        change.operations.forEach {
            when (it) {
                is Deletion -> applyDeletion(it)
                is Insertion -> applyInsertion(it, type)
            }
        }
        if (newPosition != null) when {
            newPosition.isFirst -> target.updateCursor(newPosition.first(), false)
            newPosition.isSecond -> target.updateSelection(newPosition.second())
        }
        version++
        recordChange(change, type)
    }

    private fun applyDeletion(deletion: Deletion) {
        val start = deletion.selection().min
        val end = deletion.selection().max
        val prefix = content[start.row].substring(0, start.col)
        val suffix = content[end.row].substring(end.col)
        content[start.row] = prefix + suffix
        if (end.row > start.row) {
            rendering.removeRange(start.row + 1, end.row + 1)
            content.removeRange(start.row + 1, end.row + 1)
        }
        target.updateCursor(deletion.selection().min, false)
    }

    private fun applyInsertion(insertion: Insertion, type: Type) {
        val cursor = insertion.cursor
        val prefix = content[cursor.row].substring(0, cursor.col)
        val suffix = content[cursor.row].substring(cursor.col)
        val texts = insertion.text.split("\n").toMutableList()
        texts[0] = prefix + texts[0]
        texts[texts.size - 1] = texts[texts.size - 1] + suffix

        content[cursor.row] = texts[0]
        if (texts.size > 1) {
            content.addAll(cursor.row + 1, texts.subList(1, texts.size))
            rendering.addNew(cursor.row + 1, texts.size - 1)
        }
        when (type) {
            Type.UNDO, Type.REDO -> target.updateSelection(insertion.selection())
            else -> target.updateCursor(insertion.selection().max, false)
        }
    }

    private fun recordChange(change: TextChange, type: Type) {
        when (type) {
            Type.ORIGINAL -> queueOriginalChange(change)
            Type.UNDO -> redoStack.addLast(change.invert())
            Type.REDO -> undoStack.addLast(change.invert())
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun queueOriginalChange(change: TextChange) {
        redoStack.clear()
        changeQueue.put(change)
        changeCount.incrementAndGet()
        coroutineScope.launch {
            delay(CHANGE_BATCH_DELAY)
            if (changeCount.decrementAndGet() <= 0) drainAndBatchOriginalChanges()
            if (changeCount.get() < 0) changeCount.set(0)
        }
    }

    @Synchronized
    private fun drainAndBatchOriginalChanges() {
        if (changeQueue.isNotEmpty()) {
            val changes = mutableListOf<TextChange>()
            changeQueue.drainTo(changes)
            undoStack.addLast(TextChange.merge(changes).invert())
            while (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
        }
    }
}