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
import java.util.stream.Collectors
import kotlin.math.floor

internal class TextProcessor(
    private val file: File,
    private val rendering: TextRendering,
    private val target: InputTarget,
    internal val clipboard: ClipboardManager,
    internal val onClose: () -> Unit
) {

    companion object {
        private const val TAB_SIZE = 4
        private const val UNDO_LIMIT = 1_000
    }

    internal val content: SnapshotStateList<String> get() = file.content
    internal var version by mutableStateOf(0)
    private var undoStack: ArrayDeque<TextChange> = ArrayDeque()
    private var redoStack: ArrayDeque<TextChange> = ArrayDeque()

    internal fun process(command: KeyMapping.Command) {
        when (command) {
            KeyMapping.Command.MOVE_CURSOR_LEFT_CHAR -> target.moveCursorPrevByChar() // because we only display left to right
            KeyMapping.Command.MOVE_CURSOR_RIGHT_CHAR -> target.moveCursorNextByChar() // because we only display left to right
            KeyMapping.Command.MOVE_CURSOR_LEFT_WORD -> target.moveCursorPrevByWord() // because we only display left to right
            KeyMapping.Command.MOVE_CURSOR_RIGHT_WORD -> target.moveCursorNexBytWord() // because we only display left to right
            KeyMapping.Command.MOVE_CURSOR_PREV_PARAGRAPH -> target.moveCursorPrevByParagraph()
            KeyMapping.Command.MOVE_CURSOR_NEXT_PARAGRAPH -> target.moveCursorNextByParagraph()
            KeyMapping.Command.MOVE_CURSOR_LEFT_LINE -> target.moveCursorToStartOfLine() // because we only display left to right
            KeyMapping.Command.MOVE_CURSOR_RIGHT_LINE -> target.moveCursorToEndOfLine() // because we only display left to right
            KeyMapping.Command.MOVE_CURSOR_START_LINE -> target.moveCursorToStartOfLine()
            KeyMapping.Command.MOVE_CURSOR_END_LINE -> target.moveCursorToEndOfLine()
            KeyMapping.Command.MOVE_CURSOR_UP_LINE -> target.moveCursorUpByLine()
            KeyMapping.Command.MOVE_CURSOR_DOWN_LINE -> target.moveCursorDownByLine()
            KeyMapping.Command.MOVE_CURSOR_UP_PAGE -> target.moveCursorUpByPage()
            KeyMapping.Command.MOVE_CURSOR_DOWN_PAGE -> target.moveCursorDownByPage()
            KeyMapping.Command.MOVE_CURSOR_HOME -> target.moveCursorToHome()
            KeyMapping.Command.MOVE_CURSOR_END -> target.moveCursorToEnd()
            KeyMapping.Command.SELECT_LEFT_CHAR -> target.moveCursorPrevByChar(true) // because we only display left to right
            KeyMapping.Command.SELECT_RIGHT_CHAR -> target.moveCursorNextByChar(true) // because we only display left to right
            KeyMapping.Command.SELECT_LEFT_WORD -> target.moveCursorPrevByWord(true) // because we only display left to right
            KeyMapping.Command.SELECT_RIGHT_WORD -> target.moveCursorNexBytWord(true) // because we only display left to right
            KeyMapping.Command.SELECT_PREV_PARAGRAPH -> target.moveCursorPrevByParagraph(true)
            KeyMapping.Command.SELECT_NEXT_PARAGRAPH -> target.moveCursorNextByParagraph(true)
            KeyMapping.Command.SELECT_LEFT_LINE -> target.moveCursorToStartOfLine(true) // because we only display left to right
            KeyMapping.Command.SELECT_RIGHT_LINE -> target.moveCursorToEndOfLine(true) // because we only display left to right
            KeyMapping.Command.SELECT_START_LINE -> target.moveCursorToStartOfLine(true)
            KeyMapping.Command.SELECT_END_LINE -> target.moveCursorToEndOfLine(true)
            KeyMapping.Command.SELECT_UP_LINE -> target.moveCursorUpByLine(true)
            KeyMapping.Command.SELECT_DOWN_LINE -> target.moveCursorDownByLine(true)
            KeyMapping.Command.SELECT_UP_PAGE -> target.moveCursorUpByPage(true)
            KeyMapping.Command.SELECT_DOWN_PAGE -> target.moveCursorDownByPage(true)
            KeyMapping.Command.SELECT_HOME -> target.moveCursorToHome(true)
            KeyMapping.Command.SELECT_END -> target.moveCursorToEnd(true)
            KeyMapping.Command.SELECT_ALL -> target.selectAll()
            KeyMapping.Command.SELECT_NONE -> target.selectNone()
            KeyMapping.Command.DELETE_PREV_CHAR -> deleteSelectionOr { target.moveCursorPrevByChar(true); deleteSelection() }
            KeyMapping.Command.DELETE_NEXT_CHAR -> deleteSelectionOr { target.moveCursorNextByChar(true); deleteSelection() }
            KeyMapping.Command.DELETE_PREV_WORD -> deleteSelectionOr { target.moveCursorPrevByWord(true); deleteSelection() }
            KeyMapping.Command.DELETE_NEXT_WORD -> deleteSelectionOr { target.moveCursorNexBytWord(true); deleteSelection() }
            KeyMapping.Command.DELETE_START_LINE -> deleteSelectionOr { target.moveCursorToStartOfLine(true); deleteSelection() }
            KeyMapping.Command.DELETE_END_LINE -> deleteSelectionOr { target.moveCursorToEndOfLine(true); deleteSelection() }
            KeyMapping.Command.DELETE_TAB -> deleteTab()
            KeyMapping.Command.INSERT_TAB -> insertTab()
            KeyMapping.Command.INSERT_NEW_LINE -> insertNewLine()
            KeyMapping.Command.COPY -> copy()
            KeyMapping.Command.PASTE -> paste()
            KeyMapping.Command.CUT -> cut()
            KeyMapping.Command.UNDO -> undo()
            KeyMapping.Command.REDO -> redo()
            KeyMapping.Command.CLOSE -> onClose()
            KeyMapping.Command.CHARACTER_PALETTE -> {
                // TODO: https://github.com/JetBrains/compose-jb/issues/1754
                // androidx.compose.foundation.text.showCharacterPalette()
            }
        }
    }

    private fun deletionOperation(): TextChange.Deletion {
        return TextChange.Deletion(target.selection!!.min, target.selectedText(), target.selection)
    }

    private fun deleteSelectionOr(elseFn: () -> Unit) {
        if (target.selection != null) deleteSelection()
        else elseFn()
    }

    private fun deleteSelection() {
        if (target.selection == null) return
        apply(TextChange(deletionOperation()), TextChange.Type.NATIVE)
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
        val newPosition: Either<InputTarget.Cursor, InputTarget.Selection> = oldSelection?.let {
            val startCursorShift = if (it.isForward) firstLineShift else lastLineShift
            val endCursorShift = if (it.isForward) lastLineShift else firstLineShift
            Either.second(target.shiftSelection(it, startCursorShift, endCursorShift))
        } ?: Either.first(InputTarget.Cursor(oldCursor.row, (oldCursor.col + firstLineShift).coerceAtLeast(0)))
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

    internal fun insertText(string: String, newPosition: Either<InputTarget.Cursor, InputTarget.Selection>? = null) {
        assert(string.isNotEmpty())
        val operations = mutableListOf<TextChange.Operation>()
        if (target.selection != null) operations.add(deletionOperation())
        operations.add(TextChange.Insertion(target.selection?.min ?: target.cursor, string))
        apply(TextChange(operations), TextChange.Type.NATIVE, newPosition)
    }

    internal fun copy() {
        if (target.selection == null) return
        clipboard.setText(AnnotatedString(target.selectedText()))
    }

    internal fun paste() {
        clipboard.getText()?.let { if (it.text.isNotEmpty()) insertText(it.text) }
    }

    internal fun cut() {
        if (target.selection == null) return
        copy()
        deleteSelection()
    }

    private fun undo() {
        if (undoStack.isNotEmpty()) apply(undoStack.removeLast(), TextChange.Type.UNDO)
    }

    private fun redo() {
        if (redoStack.isNotEmpty()) apply(redoStack.removeLast(), TextChange.Type.REDO)
    }

    private fun apply(
        change: TextChange,
        type: TextChange.Type,
        newPosition: Either<InputTarget.Cursor, InputTarget.Selection>? = null
    ) {

        fun applyDeletion(deletion: TextChange.Deletion) {
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

        fun applyInsertion(insertion: TextChange.Insertion) {
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
            target.updateCursor(insertion.selection().max, false)
        }

        change.operations.forEach {
            when (it) {
                is TextChange.Deletion -> applyDeletion(it)
                is TextChange.Insertion -> applyInsertion(it)
            }
        }
        if (newPosition != null) when {
            newPosition.isFirst -> target.updateCursor(newPosition.first(), false)
            newPosition.isSecond -> target.updateSelection(newPosition.second())
        }
        version++

        when (type) { // TODO: make this async and batch the changes
            TextChange.Type.NATIVE -> {
                redoStack.clear()
                undoStack.addLast(change.invert())
                while (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
            }
            TextChange.Type.UNDO -> redoStack.addLast(change.invert())
            TextChange.Type.REDO -> undoStack.addLast(change.invert())
        }
    }
}