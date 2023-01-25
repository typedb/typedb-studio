/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.framework.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.framework.editor.InputTarget.Companion.prefixSpaces
import com.vaticle.typedb.studio.framework.editor.InputTarget.Cursor
import com.vaticle.typedb.studio.framework.editor.InputTarget.Selection
import com.vaticle.typedb.studio.framework.editor.TextChange.Deletion
import com.vaticle.typedb.studio.framework.editor.TextChange.Insertion
import com.vaticle.typedb.studio.framework.editor.TextChange.ReplayType
import com.vaticle.typedb.studio.framework.editor.common.GlyphLine
import com.vaticle.typedb.studio.framework.editor.common.GlyphLine.Companion.toGlyphLines
import com.vaticle.typedb.studio.framework.editor.highlighter.SyntaxHighlighter
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Message.Project.Companion.FILE_NOT_WRITABLE
import com.vaticle.typedb.studio.service.common.util.Property
import com.vaticle.typedb.studio.service.project.FileState
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

internal interface TextProcessor {

    val isWritable: Boolean
    val file: FileState?

    fun replaceCurrentFound(text: String)
    fun replaceAllFound(text: String)
    fun insertText(text: String): Insertion?
    fun insertNewLine()
    fun duplicate()
    fun reorderLinesUp()
    fun reorderLinesDown()
    fun deleteSelection()
    fun toggleComment()
    fun indentTab()
    fun outdentTab()
    fun undo()
    fun redo()
    fun drainChanges()
    fun clearHistory()
    fun updateFile(file: FileState)

    companion object {
        internal const val TAB_SIZE = 4
        internal const val TYPING_WINDOW_MILLIS = 400
        private const val UNDO_LIMIT = 1_000
        private val LOGGER = KotlinLogging.logger {}

        fun normaliseWhiteSpace(string: String): String {
            return string.replace("\t", " ".repeat(TAB_SIZE)).replace("\u00a0", " ")
        }
    }

    class ReadOnly constructor(override var file: FileState? = null) : TextProcessor {

        override val isWritable: Boolean = false
        private var lastTyped by mutableStateOf(System.currentTimeMillis())

        override fun replaceCurrentFound(text: String) = mayDisplayWarning()
        override fun replaceAllFound(text: String) = mayDisplayWarning()
        override fun insertText(text: String): Insertion? = displayWarningOnStartTyping()
        override fun insertNewLine() = mayDisplayWarning()
        override fun duplicate() = mayDisplayWarning()
        override fun reorderLinesUp() = mayDisplayWarning()
        override fun reorderLinesDown() = mayDisplayWarning()
        override fun deleteSelection() = mayDisplayWarning()
        override fun toggleComment() = mayDisplayWarning()
        override fun indentTab() = mayDisplayWarning()
        override fun outdentTab() = mayDisplayWarning()
        override fun undo() = mayDisplayWarning()
        override fun redo() = mayDisplayWarning()
        override fun drainChanges() {}
        override fun clearHistory() {}
        override fun updateFile(file: FileState) {
            this.file = file
        }

        private fun mayDisplayWarning() = file?.path?.let {
            Service.notification.userWarning(LOGGER, FILE_NOT_WRITABLE, it)
        } ?: Unit

        private fun displayWarningOnStartTyping(): Insertion? {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTyped > TYPING_WINDOW_MILLIS) mayDisplayWarning()
            lastTyped = currentTime
            return null
        }
    }

    class Writable constructor(
        override var file: FileState,
        private val content: SnapshotStateList<GlyphLine>,
        private val rendering: TextRendering,
        private val finder: TextFinder,
        private val target: InputTarget,
        private var onChangeStart: () -> Unit,
        private var onChangeEnd: (List<String>) -> Unit,
    ) : TextProcessor {

        override val isWritable: Boolean = true
        private val fileType: Property.FileType get() = file.fileType
        private var undoStack: ArrayDeque<TextChange> = ArrayDeque()
        private var redoStack: ArrayDeque<TextChange> = ArrayDeque()
        private var changeQueue: BlockingQueue<TextChange> = LinkedBlockingQueue()
        private var changeCount: AtomicInteger = AtomicInteger(0)
        private val coroutines = CoroutineScope(Dispatchers.Default)

        override fun clearHistory() {
            undoStack.clear()
            redoStack.clear()
            changeQueue.clear()
            changeCount.set(0)
        }

        override fun updateFile(file: FileState) {
            this.file = file
            onChangeStart = { file.isChanged() }
            onChangeEnd = { file.content(it) }
        }

        override fun replaceCurrentFound(text: String) {
            if (!finder.hasMatches) return
            val oldPosition = finder.position
            if (finder.findCurrent() != target.selection) target.updateSelection(
                finder.findCurrent(),
                mayScroll = false
            )
            insertText(text, recomputeFinder = true)
            finder.trySetPosition(oldPosition)
            target.updateSelection(finder.findCurrent())
        }

        override fun replaceAllFound(text: String) {
            if (!finder.hasMatches) return
            var next: Selection?
            target.updateCursor(Cursor(0, 0), isSelecting = false, mayScroll = false)
            while (finder.recomputeNextMatch(target.cursor).let { next = it; next != null }) {
                target.updateSelection(next)
                insertText(text, recomputeFinder = false)
            }
            finder.mayRecomputeAllMatches()
        }

        private fun indent(strings: List<GlyphLine>, spaces: Int): List<GlyphLine> {
            return strings.map {
                if (spaces > 0) GlyphLine(" ".repeat(spaces) + it.annotatedString)
                else if (spaces < 0) it.subSequenceSafely((-spaces).coerceAtMost(prefixSpaces(it)), it.length)
                else it
            }
        }

        private fun deletionOperation(): Deletion {
            assert(target.selection != null)
            return Deletion(target.selection!!.min, target.selectedTextLines(), target.selection)
        }

        override fun deleteSelection() {
            if (target.selection == null) return
            val change = TextChange(deletionOperation())
            applyChange(change)
            queueChangeStack(change)
        }

        override fun toggleComment() {
            if (fileType == Property.FileType.UNKNOWN) return
            val oldSelection = target.selection
            val oldCursor = target.cursor

            target.updateSelection(target.selectionOfLineContent(oldSelection ?: oldCursor.toSelection()))
            val textLines = target.selectedTextLines()
            val commentToken = fileType.commentToken

            fun commentSelection(oldLines: List<GlyphLine>) = oldLines.map { GlyphLine(commentToken + it.annotatedString) }
            fun uncommentSelection(oldLines: List<GlyphLine>) = oldLines.map {
                if (it.isEmpty()) it
                else it.indexOf(commentToken).let { index ->
                    it.subSequenceSafely(0, index) + it.subSequenceSafely(index + commentToken.length, it.length)
                }
            }

            fun newPosition(sign: Int): Either<Cursor, Selection> {
                val shift = sign * commentToken.length
                return oldSelection?.let {
                    Either.second(target.selectionShiftedBy(it, shift, shift))
                } ?: Either.first(Cursor(oldCursor.row, oldCursor.col + shift))
            }

            val isComment = textLines.all {
                val text = it.annotatedString.text.trim(); text.isEmpty() || text.startsWith(commentToken)
            }

            insertText(if (isComment) uncommentSelection(textLines) else commentSelection(textLines))
            target.updatePosition(newPosition(if (isComment) -1 else 1))
        }

        override fun outdentTab() {
            val oldSelection = target.selection
            val oldCursor = target.cursor
            target.updateSelection(target.selectionOfLineContent(oldSelection ?: oldCursor.toSelection()))
            val oldTextLines = target.selectedTextLines()
            val newTextLines = indent(oldTextLines, -TAB_SIZE)
            val firstLineShift = newTextLines.first().length - oldTextLines.first().length
            val lastLineShift = newTextLines.last().length - oldTextLines.last().length
            val newPosition: Either<Cursor, Selection> = oldSelection?.let {
                val startShift = if (it.isForward) firstLineShift else lastLineShift
                val endShift = if (it.isForward) lastLineShift else firstLineShift
                Either.second(target.selectionShiftedBy(it, startShift, endShift))
            } ?: Either.first(Cursor(oldCursor.row, (oldCursor.col + firstLineShift).coerceAtLeast(0)))
            insertText(newTextLines)
            target.updatePosition(newPosition)
        }

        override fun indentTab() {
            val oldSelection = target.selection
            val cursor = target.cursor
            if (oldSelection == null) insertText(" ".repeat(TAB_SIZE - prefixSpaces(content[cursor.row]) % TAB_SIZE))
            else {
                val newSelection = target.selectionShiftedBy(oldSelection, TAB_SIZE, TAB_SIZE)
                target.updateSelection(target.selectionOfLineContent(oldSelection))
                insertText(indent(target.selectedTextLines(), TAB_SIZE))
                target.updateSelection(newSelection)
            }
        }

        override fun insertNewLine() {
            val lineText = content[target.cursor.row]
            val tabs = floor(prefixSpaces(lineText).coerceAtMost(target.cursor.col).toDouble() / TAB_SIZE).toInt()
            insertText("\n" + " ".repeat(TAB_SIZE * tabs))
        }

        override fun duplicate() {
            if (target.selection == null) duplicateLine() else duplicateSelection()
        }

        private fun duplicateLine() {
            val oldCursor = target.cursor
            target.updateSelection(target.selectionOfLineAndBreak(target.cursor))
            val lineAndBreak = target.selectedTextLines()
            target.updateCursor(target.selection!!.max, false)
            if (lineAndBreak.size == 1) insertNewLine()
            insertText(lineAndBreak)
            target.updateCursor(Cursor(oldCursor.row + 1, oldCursor.col), false)
        }

        private fun duplicateSelection() {
            val selection = target.selectedText().toString()
            target.updateCursor(target.selection!!.max, false)
            insertText(selection)?.let { target.updateSelection(it.selection()) }
        }

        override fun reorderLinesUp() {
            val minRow = target.selection?.min?.row ?: target.cursor.row
            if (minRow == 0) return

            val oldPosition: Either<Cursor, Selection> =
                target.selection?.let { Either.second(it) } ?: Either.first(target.cursor)

            target.updateSelection(target.selectionOfPreviousLineAndBreak(minRow))
            val lineAndBreak = target.selectedTextLines()
            deleteSelection()

            val newPosition: Either<Cursor, Selection> = oldPosition.apply(
                { Either.first(Cursor(it.row - 1, it.col)) },
                { Either.second(Selection(Cursor(it.start.row - 1, it.start.col), Cursor(it.end.row - 1, it.end.col))) }
            )
            val maxRow = newPosition.apply({ it.row }, { it.max.row })
            if (maxRow < content.size - 1) {
                target.updateCursor(Cursor(maxRow + 1, 0), false)
                insertText(lineAndBreak)
            } else {
                target.updateCursor(Cursor(maxRow, content[maxRow].length), false)
                insertText("\n")
                insertText(lineAndBreak.first())
            }
            target.updatePosition(newPosition)
        }

        override fun reorderLinesDown() {
            val minRow = target.selection?.min?.row ?: target.cursor.row
            val maxRow = target.selection?.max?.row ?: target.cursor.row
            if (maxRow == content.size - 1) return

            val oldPosition: Either<Cursor, Selection> =
                target.selection?.let { Either.second(it) } ?: Either.first(target.cursor)

            target.updateSelection(target.selectionOfNextBreakAndLine(maxRow))
            val breakAndLine = target.selectedTextLines()
            deleteSelection()

            if (minRow > 0) {
                target.updateCursor(Cursor(minRow - 1, content[minRow - 1].length), false)
                insertText(breakAndLine)
            } else {
                target.updateCursor(Cursor(minRow, 0), false)
                insertText(breakAndLine.last())
                insertText("\n")
            }

            val newPosition: Either<Cursor, Selection> = oldPosition.apply(
                { Either.first(Cursor(it.row + 1, it.col)) },
                { Either.second(Selection(Cursor(it.start.row + 1, it.start.col), Cursor(it.end.row + 1, it.end.col))) }
            )
            target.updatePosition(newPosition)
        }

        override fun insertText(text: String): Insertion? {
            return insertText(text, recomputeFinder = true)
        }

        private fun insertText(text: GlyphLine): Insertion? {
            return insertText(listOf(text))
        }

        private fun insertText(text: String, recomputeFinder: Boolean): Insertion? {
            val glyphLines = text.toGlyphLines()
            return insertText(glyphLines, recomputeFinder)
        }

        private fun insertText(strings: List<GlyphLine>, recomputeFinder: Boolean = true): Insertion? {
            val operations = mutableListOf<TextChange.Operation>()
            if (target.selection != null) operations.add(deletionOperation())
            val insertion: Insertion?
            if (strings.isNotEmpty()) {
                insertion = Insertion(target.selection?.min ?: target.cursor, strings)
                operations.add(insertion)
            } else insertion = null
            val change = TextChange(operations)
            applyChange(change, recomputeFinder)
            queueChangeStack(change)
            return insertion
        }

        override fun undo() {
            drainAndBatchChanges(isFinalChange = false)
            if (undoStack.isNotEmpty()) applyReplay(undoStack.removeLast(), ReplayType.UNDO)
        }

        override fun redo() {
            if (redoStack.isNotEmpty()) applyReplay(redoStack.removeLast(), ReplayType.REDO)
        }

        private fun applyReplay(change: TextChange, replayType: ReplayType) {
            applyChange(change)
            target.updatePosition(change.target())
            when (replayType) {
                ReplayType.UNDO -> redoStack.addLast(change.invert())
                ReplayType.REDO -> undoStack.addLast(change.invert())
            }
            callOnChangeEnd()
        }

        private fun applyChange(change: TextChange, recomputeFinder: Boolean = true) {
            onChangeStart()
            rendering.invalidate(change)
            change.operations.forEach {
                when (it) {
                    is Deletion -> applyDeletion(it)
                    is Insertion -> applyInsertion(it)
                }
            }
            target.resetTextWidth()
            if (recomputeFinder) finder.mayRecomputeAllMatches()
        }

        private fun applyDeletion(deletion: Deletion) {
            val start = deletion.selection().min
            val end = deletion.selection().max
            val prefix = content[start.row].subSequenceSafely(0, start.col)
            val suffix = content[end.row].subSequenceSafely(end.col, content[end.row].length)
            content[start.row] = prefix + suffix
            if (end.row > start.row) {
                rendering.removeRange(start.row + 1, end.row + 1)
                content.removeRange(start.row + 1, end.row + 1)
            }
            target.updateCursor(deletion.selection().min, false)
        }

        private fun applyInsertion(insertion: Insertion) {
            // TODO: investigate how it is possible for subSequence() to throw IndexOutOfBounds without coercion
            val cursor = insertion.cursor
            val prefix = content[cursor.row].subSequenceSafely(0, cursor.col)
            val suffix = content[cursor.row].let { it.subSequenceSafely(cursor.col, it.length) }
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

        private fun queueChangeStack(change: TextChange) {
            redoStack.clear()
            changeQueue.put(change)
            changeCount.incrementAndGet()
            coroutines.launch {
                delay(TYPING_WINDOW_MILLIS.milliseconds)
                if (changeCount.decrementAndGet() == 0) drainAndBatchChanges(isFinalChange = true)
            }
        }

        @Synchronized
        private fun drainAndBatchChanges(isFinalChange: Boolean): TextChange? {
            var batchedChanges: TextChange? = null
            if (changeQueue.isNotEmpty()) {
                val changes = mutableListOf<TextChange>()
                changeQueue.drainTo(changes)
                batchedChanges = TextChange.merge(changes)
                undoStack.addLast(batchedChanges.invert())
                while (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
                if (isFinalChange) callOnChangeEnd()
                highlight(batchedChanges.lines())
            }
            return batchedChanges
        }

        private fun highlight(lines: IntRange) {
            lines.forEach { content[it] = SyntaxHighlighter.highlight(content[it].annotatedString.text, fileType) }
        }

        private fun callOnChangeEnd() {
            onChangeEnd(content.map { it.annotatedString.text })
        }

        override fun drainChanges() {
            drainAndBatchChanges(isFinalChange = true)
        }
    }
}
