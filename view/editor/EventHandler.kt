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

import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ClipboardManager
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Icon
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
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand.ENTER_SHIFT_MOD
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
import com.vaticle.typedb.studio.view.editor.KeyMapper.WindowCommand

internal class EventHandler(
    private val target: InputTarget,
    private val processor: TextProcessor,
    private val toolbar: TextToolbar.State,
    private val clipboard: ClipboardManager
) {

    internal var onClose: (() -> Unit)? = null

    internal fun handleEditorEvent(event: KeyEvent): Boolean {
        return if (event.type == KeyEventType.KeyUp) false
        else when {
            event.isTypedEvent -> processor.insertText(event.awtEvent.keyChar.toString())
            else -> KeyMapper.CURRENT.map(event)?.let {
                when (it) {
                    is WindowCommand -> execute(it)
                    is EditorCommand -> execute(it)
                }
            } ?: false
        }
    }

    internal fun handleToolbarEvent(event: KeyEvent): Boolean {
        return if (event.type == KeyEventType.KeyUp) false
        else KeyMapper.CURRENT.map(event)?.let {
            when (it) {
                is WindowCommand -> execute(it)
                else -> false
            }
        } ?: false
    }

    private fun execute(command: WindowCommand): Boolean {
        when (command) {
            WindowCommand.FIND -> toolbar.showFinder()
            WindowCommand.REPLACE -> toolbar.showReplacer()
            WindowCommand.CLOSE -> onClose?.let { it() }
            WindowCommand.ESCAPE -> target.selection?.let { target.selectNone() } ?: hideToolbar()
        }
        return true
    }

    private fun execute(command: EditorCommand): Boolean {
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
            DELETE_PREV_CHAR -> deleteSelectionOr { target.moveCursorPrevByChar(true); processor.deleteSelection() }
            DELETE_NEXT_CHAR -> deleteSelectionOr { target.moveCursorNextByChar(true); processor.deleteSelection() }
            DELETE_PREV_WORD -> deleteSelectionOr { target.moveCursorPrevByWord(true); processor.deleteSelection() }
            DELETE_NEXT_WORD -> deleteSelectionOr { target.moveCursorNexBytWord(true); processor.deleteSelection() }
            DELETE_START_LINE -> deleteSelectionOr { target.moveCursorToStartOfLine(true); processor.deleteSelection() }
            DELETE_END_LINE -> deleteSelectionOr { target.moveCursorToEndOfLine(true); processor.deleteSelection() }
            TAB -> processor.indentTab()
            TAB_SHIFT -> processor.outdentTab()
            ENTER, ENTER_SHIFT, ENTER_SHIFT_MOD -> processor.insertNewLine()
            CUT -> cut()
            COPY -> copy()
            PASTE -> paste()
            UNDO -> processor.undo()
            REDO -> processor.redo()
            EMOJI_WINDOW -> {
                // TODO: https://github.com/JetBrains/compose-jb/issues/1754
                // androidx.compose.foundation.text.showCharacterPalette()
            }
        }
        return true
    }

    private fun deleteSelectionOr(elseFn: () -> Unit) {
        if (target.selection != null) processor.deleteSelection()
        else elseFn()
    }

    private fun cut() {
        if (target.selection == null) return
        copy()
        processor.deleteSelection()
    }

    private fun copy() {
        if (target.selection == null) return
        clipboard.setText(target.selectedText())
    }

    private fun paste() {
        clipboard.getText()?.let { if (it.text.isNotEmpty()) processor.insertText(it.text) }
    }

    private fun hideToolbar(): Boolean {
        return if (toolbar.showToolbar) {
            toolbar.hide()
            true
        } else false
    }

    internal fun contextMenuFn(): List<List<ContextMenu.Item>> {
        val selection = target.selection
        val modKey = if (Property.OS.Current == Property.OS.MACOS) Label.CMD else Label.CTRL
        val hasClipboard = !clipboard.getText().isNullOrBlank()
        return listOf(
            listOf(
                ContextMenu.Item(Label.CUT, Icon.Code.CUT, "$modKey + X", selection != null) { cut() },
                ContextMenu.Item(Label.COPY, Icon.Code.COPY, "$modKey + C", selection != null) { copy() },
                ContextMenu.Item(Label.PASTE, Icon.Code.PASTE, "$modKey + V", hasClipboard) { paste() }
            ),
            listOf(
                ContextMenu.Item(Label.FIND, Icon.Code.MAGNIFYING_GLASS, "$modKey + F") { toolbar.showFinder() },
                ContextMenu.Item(Label.REPLACE, Icon.Code.RIGHT_LEFT, "$modKey + R") { toolbar.showReplacer() }
            ),
            listOf(
                ContextMenu.Item(Label.SAVE, Icon.Code.FLOPPY_DISK, "$modKey + S", false) { }, // TODO
                ContextMenu.Item(Label.CLOSE, Icon.Code.XMARK, "$modKey + W") { onClose?.let { it() } },
            )
        )
    }
}