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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.isTypedEvent
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Icon.Code
import com.vaticle.typedb.studio.view.common.component.LazyColumn
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Color.fadeable
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import com.vaticle.typedb.studio.view.editor.InputTarget.Cursor
import com.vaticle.typedb.studio.view.editor.InputTarget.Selection
import com.vaticle.typedb.studio.view.editor.KeyMapping.Command
import com.vaticle.typedb.studio.view.editor.KeyMapping.Companion.CURRENT_KEY_MAPPING
import com.vaticle.typedb.studio.view.editor.TextChange.Type.NATIVE
import com.vaticle.typedb.studio.view.editor.TextChange.Type.REDO
import com.vaticle.typedb.studio.view.editor.TextChange.Type.UNDO
import java.awt.event.MouseEvent.BUTTON1
import java.util.stream.Collectors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

@OptIn(ExperimentalTime::class)
object TextEditor {

    private const val TAB_SIZE = 4
    private const val UNDO_LIMIT = 1_000
    private const val LINE_HEIGHT = 1.56f
    private const val DISABLED_CURSOR_OPACITY = 0.6f
    private val LINE_GAP = 2.dp
    private val AREA_PADDING_HORIZONTAL = 6.dp
    private val DEFAULT_FONT_WIDTH = 12.dp
    private val CURSOR_LINE_PADDING = 0.dp
    private val BLINKING_FREQUENCY = Duration.milliseconds(500)

    class State internal constructor(
        internal val file: File,
        internal val fontBase: TextStyle,
        internal val lineHeight: Dp,
        internal val clipboard: ClipboardManager,
        internal val onClose: () -> Unit,
        coroutineScope: CoroutineScope,
        initDensity: Float,
    ) {
        internal val focusReq = FocusRequester()
        internal val content: SnapshotStateList<String> get() = file.content
        internal val lineCount: Int get() = content.size
        internal val rendering = TextRendering(file.content.size)
        internal val contextMenu = ContextMenu.State()
        internal val verScroller = LazyColumn.createScrollState(lineHeight) { content.size }
        internal var horScroller = ScrollState(0)
        internal val target = InputTarget(
            file, lineHeight, verScroller, horScroller, AREA_PADDING_HORIZONTAL, rendering, coroutineScope, initDensity
        )
        internal var isFocused by mutableStateOf(true)
        internal var width by mutableStateOf(0.dp)
        internal var stateVersion by mutableStateOf(0)
        private var undoStack: ArrayDeque<TextChange> = ArrayDeque()
        private var redoStack: ArrayDeque<TextChange> = ArrayDeque()
        internal var density: Float
            get() = target.density
            set(value) {
                target.density = value
            }

        internal fun updateStatus() {
            target.updateStatus()
        }

        internal fun increaseWidth(newRawWidth: Int) {
            val newWidth = toDP(newRawWidth, density)
            if (newWidth > width) width = newWidth
        }

        internal fun processKeyEvent(event: KeyEvent): Boolean {
            return if (event.isTypedEvent) {
                insertText(event.awtEvent.keyChar.toString())
                true
            } else if (event.type != KeyEventType.KeyDown) false
            else CURRENT_KEY_MAPPING.map(event)?.let { processCommand(it); true } ?: false
        }

        private fun processCommand(command: Command) {
            when (command) {
                Command.MOVE_CURSOR_LEFT_CHAR -> target.moveCursorPrevByChar() // because we only display left to right
                Command.MOVE_CURSOR_RIGHT_CHAR -> target.moveCursorNextByChar() // because we only display left to right
                Command.MOVE_CURSOR_LEFT_WORD -> target.moveCursorPrevByWord() // because we only display left to right
                Command.MOVE_CURSOR_RIGHT_WORD -> target.moveCursorNexBytWord() // because we only display left to right
                Command.MOVE_CURSOR_PREV_PARAGRAPH -> target.moveCursorPrevByParagraph()
                Command.MOVE_CURSOR_NEXT_PARAGRAPH -> target.moveCursorNextByParagraph()
                Command.MOVE_CURSOR_LEFT_LINE -> target.moveCursorToStartOfLine() // because we only display left to right
                Command.MOVE_CURSOR_RIGHT_LINE -> target.moveCursorToEndOfLine() // because we only display left to right
                Command.MOVE_CURSOR_START_LINE -> target.moveCursorToStartOfLine()
                Command.MOVE_CURSOR_END_LINE -> target.moveCursorToEndOfLine()
                Command.MOVE_CURSOR_UP_LINE -> target.moveCursorUpByLine()
                Command.MOVE_CURSOR_DOWN_LINE -> target.moveCursorDownByLine()
                Command.MOVE_CURSOR_UP_PAGE -> target.moveCursorUpByPage()
                Command.MOVE_CURSOR_DOWN_PAGE -> target.moveCursorDownByPage()
                Command.MOVE_CURSOR_HOME -> target.moveCursorToHome()
                Command.MOVE_CURSOR_END -> target.moveCursorToEnd()
                Command.SELECT_LEFT_CHAR -> target.moveCursorPrevByChar(true) // because we only display left to right
                Command.SELECT_RIGHT_CHAR -> target.moveCursorNextByChar(true) // because we only display left to right
                Command.SELECT_LEFT_WORD -> target.moveCursorPrevByWord(true) // because we only display left to right
                Command.SELECT_RIGHT_WORD -> target.moveCursorNexBytWord(true) // because we only display left to right
                Command.SELECT_PREV_PARAGRAPH -> target.moveCursorPrevByParagraph(true)
                Command.SELECT_NEXT_PARAGRAPH -> target.moveCursorNextByParagraph(true)
                Command.SELECT_LEFT_LINE -> target.moveCursorToStartOfLine(true) // because we only display left to right
                Command.SELECT_RIGHT_LINE -> target.moveCursorToEndOfLine(true) // because we only display left to right
                Command.SELECT_START_LINE -> target.moveCursorToStartOfLine(true)
                Command.SELECT_END_LINE -> target.moveCursorToEndOfLine(true)
                Command.SELECT_UP_LINE -> target.moveCursorUpByLine(true)
                Command.SELECT_DOWN_LINE -> target.moveCursorDownByLine(true)
                Command.SELECT_UP_PAGE -> target.moveCursorUpByPage(true)
                Command.SELECT_DOWN_PAGE -> target.moveCursorDownByPage(true)
                Command.SELECT_HOME -> target.moveCursorToHome(true)
                Command.SELECT_END -> target.moveCursorToEnd(true)
                Command.SELECT_ALL -> target.selectAll()
                Command.SELECT_NONE -> target.selectNone()
                Command.DELETE_PREV_CHAR -> deleteSelectionOr { target.moveCursorPrevByChar(true); deleteSelection() }
                Command.DELETE_NEXT_CHAR -> deleteSelectionOr { target.moveCursorNextByChar(true); deleteSelection() }
                Command.DELETE_PREV_WORD -> deleteSelectionOr { target.moveCursorPrevByWord(true); deleteSelection() }
                Command.DELETE_NEXT_WORD -> deleteSelectionOr { target.moveCursorNexBytWord(true); deleteSelection() }
                Command.DELETE_START_LINE -> deleteSelectionOr { target.moveCursorToStartOfLine(true); deleteSelection() }
                Command.DELETE_END_LINE -> deleteSelectionOr { target.moveCursorToEndOfLine(true); deleteSelection() }
                Command.DELETE_TAB -> deleteTab()
                Command.INSERT_TAB -> insertTab()
                Command.INSERT_NEW_LINE -> insertNewLine()
                Command.COPY -> copy()
                Command.PASTE -> paste()
                Command.CUT -> cut()
                Command.UNDO -> undo()
                Command.REDO -> redo()
                Command.CLOSE -> onClose()
                Command.CHARACTER_PALETTE -> {
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
            apply(TextChange(deletionOperation()), NATIVE)
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

        private fun insertText(string: String, newPosition: Either<Cursor, Selection>? = null) {
            assert(string.isNotEmpty())
            val operations = mutableListOf<TextChange.Operation>()
            if (target.selection != null) operations.add(deletionOperation())
            operations.add(TextChange.Insertion(target.selection?.min ?: target.cursor, string))
            apply(TextChange(operations), NATIVE, newPosition)
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
            if (undoStack.isNotEmpty()) apply(undoStack.removeLast(), UNDO)
        }

        private fun redo() {
            if (redoStack.isNotEmpty()) apply(redoStack.removeLast(), REDO)
        }

        private fun apply(change: TextChange, type: TextChange.Type, newPosition: Either<Cursor, Selection>? = null) {

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
            stateVersion++

            when (type) { // TODO: make this async and batch the changes
                NATIVE -> {
                    redoStack.clear()
                    undoStack.addLast(change.invert())
                    while (undoStack.size > UNDO_LIMIT) undoStack.removeFirst()
                }
                UNDO -> redoStack.addLast(change.invert())
                REDO -> undoStack.addLast(change.invert())
            }
        }
    }

    @Composable
    fun createState(file: File, coroutineScope: CoroutineScope, onClose: () -> Unit): State {
        val font = Theme.typography.code1
        val currentDensity = LocalDensity.current
        val lineHeight = with(currentDensity) { font.fontSize.toDp() * LINE_HEIGHT }
        val clipboard = LocalClipboardManager.current
        return State(file, font, lineHeight, clipboard, onClose, coroutineScope, currentDensity.density)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Area(state: State, modifier: Modifier = Modifier) {
        if (state.content.isEmpty()) return
        val density = LocalDensity.current.density
        val fontHeight = with(LocalDensity.current) { (state.lineHeight - LINE_GAP).toSp() * density }
        val fontColor = Theme.colors.onBackground
        val textFont = state.fontBase.copy(color = fontColor, lineHeight = fontHeight)
        val lineNumberFont = state.fontBase.copy(color = fontColor.copy(0.5f), lineHeight = fontHeight)
        var fontWidth by remember { mutableStateOf(DEFAULT_FONT_WIDTH) }

        Box { // We render a number to find out the default width of a digit for the given font
            Text(text = "0", style = lineNumberFont, onTextLayout = { fontWidth = toDP(it.size.width, density) })
            Row(modifier = modifier.onFocusChanged { state.isFocused = it.isFocused; state.updateStatus() }
                .focusRequester(state.focusReq).focusable()
                .onGloballyPositioned { state.density = density }
                .onKeyEvent { state.processKeyEvent(it) }
                .onPointerEvent(Move) { state.target.mayUpdateDragSelection(it.awtEvent.x, it.awtEvent.y) }
                .onPointerEvent(Release) { if (it.awtEvent.button == BUTTON1) state.target.stopDragSelection() }
                .pointerInput(state) { onPointerInput(state) }
            ) {
                LineNumberArea(state, lineNumberFont, fontWidth)
                Separator.Vertical()
                TextArea(state, textFont, fontWidth)
            }
        }

        LaunchedEffect(state) { state.focusReq.requestFocus() }
    }

    @Composable
    private fun LineNumberArea(state: State, font: TextStyle, fontWidth: Dp) {
        val maxDigits = ceil(log10(state.lineCount + 1.0)).toInt()
        val minWidth = fontWidth * maxDigits + AREA_PADDING_HORIZONTAL * 2 + 2.dp
        val lazyColumnState: LazyColumn.State<Int> = LazyColumn.createState(
            items = (0 until state.lineCount).map { it },
            scroller = state.verScroller
        )
        LazyColumn.Area(state = lazyColumnState) { index, _ -> LineNumber(state, index, font, minWidth) }
    }

    @Composable
    private fun LineNumber(state: State, index: Int, font: TextStyle, minWidth: Dp) {
        val isCursor = state.target.cursor.row == index
        val isSelected = state.target.selection?.let { it.min.row <= index && it.max.row >= index } ?: false
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
    private fun TextArea(state: State, font: TextStyle, fontWidth: Dp) {
        val lazyColumnState: LazyColumn.State<String> = LazyColumn.createState(
            items = state.content,
            scroller = state.verScroller
        )

        Box(modifier = Modifier.fillMaxSize()
            .background(Theme.colors.background2)
            .horizontalScroll(state.horScroller)
            .onGloballyPositioned { state.target.updateTextArea(it.boundsInWindow()) }
            .onSizeChanged { state.increaseWidth(it.width) }) {
            ContextMenu.Popup(state.contextMenu) { contextMenuFn(state) }
            LazyColumn.Area(state = lazyColumnState) { index, text -> TextLine(state, index, text, font, fontWidth) }
        }
    }

    @Composable
    private fun TextLine(state: State, index: Int, text: String, font: TextStyle, fontWidth: Dp) {
        val cursor = state.target.cursor
        val selection = state.target.selection
        val bgColor = when {
            cursor.row == index && selection == null -> Theme.colors.primary
            else -> Theme.colors.background2
        }
        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier.background(bgColor)
                .defaultMinSize(minWidth = state.width)
                .height(state.lineHeight)
                .padding(horizontal = AREA_PADDING_HORIZONTAL)
        ) {
            val isRendered = state.rendering.isRendered(index, state.stateVersion)
            if (selection != null && selection.min.row <= index && selection.max.row >= index) {
                if (!isRendered) SelectionHighlighter(state, index, null, text.length, fontWidth)
                else SelectionHighlighter(state, index, state.rendering.get(index), text.length, fontWidth)
            }
            Text(
                text = AnnotatedString(text), style = font,
                modifier = Modifier.onSizeChanged { state.increaseWidth(it.width) },
                onTextLayout = { state.rendering.set(index, it, state.stateVersion) }
            )
            if (cursor.row == index) {
                if (!isRendered) CursorIndicator(state, text, null, font, fontWidth)
                else CursorIndicator(state, text, state.rendering.get(index), font, fontWidth)
            }
        }
    }

    @Composable
    private fun SelectionHighlighter(
        state: State, index: Int, textLayout: TextLayoutResult?, length: Int, fontWidth: Dp
    ) {
        val selection = state.target.selection
        assert(selection != null && selection.min.row <= index && selection.max.row >= index)
        val start = when {
            selection!!.min.row < index -> 0
            else -> selection.min.col
        }
        val end = when {
            selection.max.row > index -> state.content[index].length
            else -> selection.max.col
        }
        var startPos = textLayout?.let { toDP(it.getCursorRect(start).left, state.density) } ?: (fontWidth * start)
        var endPos = textLayout?.let {
            toDP(it.getCursorRect(end.coerceAtMost(it.getLineEnd(0))).right, state.density)
        } ?: (fontWidth * end)
        if (selection.min.row < index) startPos -= AREA_PADDING_HORIZONTAL
        if (selection.max.row > index && length > 0) endPos += AREA_PADDING_HORIZONTAL
        val color = Theme.colors.tertiary.copy(Theme.SELECTION_ALPHA)
        Box(Modifier.offset(x = startPos).width(endPos - startPos).height(state.lineHeight).background(color))
    }

    @OptIn(ExperimentalTime::class)
    @Composable
    private fun CursorIndicator(
        state: State, text: String, textLayout: TextLayoutResult?, font: TextStyle, fontWidth: Dp
    ) {
        val cursor = state.target.cursor
        var visible by remember { mutableStateOf(true) }
        val offsetX = textLayout?.let {
            toDP(it.getCursorRect(cursor.col.coerceAtMost(it.getLineEnd(0))).left, state.density)
        } ?: (fontWidth * cursor.col)
        val width = when {
            cursor.col >= text.length -> fontWidth
            else -> textLayout?.let {
                toDP(it.getBoundingBox(cursor.col).width, state.density)
            } ?: fontWidth
        }
        if (visible || !state.isFocused) {
            Box(
                modifier = Modifier.offset(x = offsetX, y = CURSOR_LINE_PADDING)
                    .width(width).height(state.lineHeight - CURSOR_LINE_PADDING * 2)
                    .background(fadeable(Theme.colors.secondary, !state.isFocused, DISABLED_CURSOR_OPACITY))
            ) {
                Text(
                    text.getOrNull(cursor.col)?.toString() ?: "",
                    Modifier.offset(y = -CURSOR_LINE_PADDING),
                    style = font.copy(Theme.colors.background2)
                )
            }
        }
        if (state.isFocused) LaunchedEffect(cursor) {
            visible = true
            while (true) {
                delay(BLINKING_FREQUENCY)
                visible = !visible
            }
        }
    }

    private suspend fun PointerInputScope.onPointerInput(state: State) {
        state.contextMenu.onPointerInput(
            pointerInputScope = this,
            onSinglePrimaryClick = {
                state.focusReq.requestFocus()
                state.target.startDragSelection()
                state.target.updateCursor(it.x, it.y, it.isShiftDown)
            },
            onDoublePrimaryClick = { state.target.selectWord() },
            onTriplePrimaryClick = { state.target.selectLine() },
            onSecondaryClick = { state.target.updateCursorIfOutOfSelection(it.x, it.y) }
        )
    }

    private fun contextMenuFn(state: State): List<List<ContextMenu.Item>> { // TODO
        val selection = state.target.selection
        val modKey = if (Property.OS.Current == Property.OS.MACOS) Label.CMD else Label.CTRL
        val hasClipboard = !state.clipboard.getText().isNullOrBlank()
        return listOf(
            listOf(
                ContextMenu.Item(Label.CUT, Code.CUT, "$modKey + X", selection != null) { state.cut() },
                ContextMenu.Item(Label.COPY, Code.COPY, "$modKey + C", selection != null) { state.copy() },
                ContextMenu.Item(Label.PASTE, Code.PASTE, "$modKey + V", hasClipboard) { state.paste() }
            ),
            listOf(
                ContextMenu.Item(Label.SAVE, Code.FLOPPY_DISK, "$modKey + S", false) { }, // TODO
                ContextMenu.Item(Label.CLOSE, Code.XMARK, "$modKey + W") { state.onClose() },
            )
        )
    }
}