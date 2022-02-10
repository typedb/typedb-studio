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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import com.vaticle.typedb.studio.state.common.Property.OS

internal interface KeyMapper {

    fun map(event: KeyEvent): Command?

    companion object {

        internal val CURRENT: KeyMapper = when (OS.Current) {
            OS.MACOS -> MacOSKeyMapper
            else -> DefaultKeyMapper
        }
    }

    sealed interface Command

    enum class WindowCommand : Command {
        FIND,
        REPLACE,
        CLOSE,
        ESCAPE,
    }

    enum class EditorCommand(val editsText: Boolean) : Command { // TODO: do we need 'editsText' boolean?
        MOVE_CURSOR_LEFT_CHAR(false),
        MOVE_CURSOR_RIGHT_CHAR(false),
        MOVE_CURSOR_RIGHT_WORD(false),
        MOVE_CURSOR_LEFT_WORD(false),
        MOVE_CURSOR_PREV_PARAGRAPH(false),
        MOVE_CURSOR_NEXT_PARAGRAPH(false),

        MOVE_CURSOR_LEFT_LINE(false),
        MOVE_CURSOR_RIGHT_LINE(false),
        MOVE_CURSOR_START_LINE(false),
        MOVE_CURSOR_END_LINE(false),

        MOVE_CURSOR_UP_LINE(false),
        MOVE_CURSOR_DOWN_LINE(false),
        MOVE_CURSOR_UP_PAGE(false),
        MOVE_CURSOR_DOWN_PAGE(false),
        MOVE_CURSOR_HOME(false),
        MOVE_CURSOR_END(false),

        SELECT_NONE(false),
        SELECT_ALL(false),

        SELECT_LEFT_CHAR(false),
        SELECT_RIGHT_CHAR(false),
        SELECT_LEFT_WORD(false),
        SELECT_RIGHT_WORD(false),
        SELECT_NEXT_PARAGRAPH(false),
        SELECT_PREV_PARAGRAPH(false),

        SELECT_LEFT_LINE(false),
        SELECT_RIGHT_LINE(false),
        SELECT_START_LINE(false),
        SELECT_END_LINE(false),

        SELECT_UP_LINE(false),
        SELECT_DOWN_LINE(false),
        SELECT_UP_PAGE(false),
        SELECT_DOWN_PAGE(false),
        SELECT_HOME(false),
        SELECT_END(false),

        ENTER(true),
        ENTER_SHIFT(true),
        ENTER_SHIFT_MOD(false),
        TAB(true),
        TAB_SHIFT(true),

        DELETE_PREV_CHAR(true),
        DELETE_NEXT_CHAR(true),
        DELETE_PREV_WORD(true),
        DELETE_NEXT_WORD(true),
        DELETE_START_LINE(true),
        DELETE_END_LINE(true),

        COPY(false),
        PASTE(true),
        CUT(true),

        UNDO(true),
        REDO(true),

        EMOJI_WINDOW(false),
    }

    private object Keys {
        val A: Key = Key(java.awt.event.KeyEvent.VK_A)
        val B: Key = Key(java.awt.event.KeyEvent.VK_B)
        val D: Key = Key(java.awt.event.KeyEvent.VK_D)
        val C: Key = Key(java.awt.event.KeyEvent.VK_C)
        val E: Key = Key(java.awt.event.KeyEvent.VK_E)
        val F: Key = Key(java.awt.event.KeyEvent.VK_F)
        val H: Key = Key(java.awt.event.KeyEvent.VK_H)
        val K: Key = Key(java.awt.event.KeyEvent.VK_K)
        val N: Key = Key(java.awt.event.KeyEvent.VK_N)
        val O: Key = Key(java.awt.event.KeyEvent.VK_O)
        val P: Key = Key(java.awt.event.KeyEvent.VK_P)
        val R: Key = Key(java.awt.event.KeyEvent.VK_R)
        val V: Key = Key(java.awt.event.KeyEvent.VK_V)
        val W: Key = Key(java.awt.event.KeyEvent.VK_W)
        val X: Key = Key(java.awt.event.KeyEvent.VK_X)
        val Z: Key = Key(java.awt.event.KeyEvent.VK_Z)
        val Backslash: Key = Key(java.awt.event.KeyEvent.VK_BACK_SLASH)
        val DirectionLeft: Key = Key(java.awt.event.KeyEvent.VK_LEFT)
        val DirectionRight: Key = Key(java.awt.event.KeyEvent.VK_RIGHT)
        val DirectionUp: Key = Key(java.awt.event.KeyEvent.VK_UP)
        val DirectionDown: Key = Key(java.awt.event.KeyEvent.VK_DOWN)
        val PageUp: Key = Key(java.awt.event.KeyEvent.VK_PAGE_UP)
        val PageDown: Key = Key(java.awt.event.KeyEvent.VK_PAGE_DOWN)
        val MoveHome: Key = Key(java.awt.event.KeyEvent.VK_HOME)
        val MoveEnd: Key = Key(java.awt.event.KeyEvent.VK_END)
        val Insert: Key = Key(java.awt.event.KeyEvent.VK_INSERT)
        val Enter: Key = Key(java.awt.event.KeyEvent.VK_ENTER)
        val Backspace: Key = Key(java.awt.event.KeyEvent.VK_BACK_SPACE)
        val Delete: Key = Key(java.awt.event.KeyEvent.VK_DELETE)
        val Paste: Key = Key(java.awt.event.KeyEvent.VK_PASTE)
        val Cut: Key = Key(java.awt.event.KeyEvent.VK_CUT)
        val Copy: Key = Key(java.awt.event.KeyEvent.VK_COPY)
        val Tab: Key = Key(java.awt.event.KeyEvent.VK_TAB)
        val Space: Key = Key(java.awt.event.KeyEvent.VK_SPACE)
        val Escape: Key = Key(java.awt.event.KeyEvent.VK_ESCAPE)
    }

    private object CommonKeyMapper {
        fun map(event: KeyEvent, shortcutModifier: (KeyEvent) -> Boolean): Command? {
            return when {
                shortcutModifier(event) && event.isShiftPressed ->
                    when (event.key) {
                        Keys.Z -> EditorCommand.REDO
                        Keys.Enter -> EditorCommand.ENTER_SHIFT_MOD
                        else -> null
                    }
                shortcutModifier(event) ->
                    when (event.key) {
                        Keys.C, Keys.Insert -> EditorCommand.COPY
                        Keys.V -> EditorCommand.PASTE
                        Keys.X -> EditorCommand.CUT
                        Keys.A -> EditorCommand.SELECT_ALL
                        Keys.Z -> EditorCommand.UNDO
                        Keys.F -> WindowCommand.FIND
                        Keys.R -> WindowCommand.REPLACE
                        Keys.W -> WindowCommand.CLOSE
                        else -> null
                    }
                event.isCtrlPressed -> null
                event.isShiftPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> EditorCommand.SELECT_LEFT_CHAR
                        Keys.DirectionRight -> EditorCommand.SELECT_RIGHT_CHAR
                        Keys.DirectionUp -> EditorCommand.SELECT_UP_LINE
                        Keys.DirectionDown -> EditorCommand.SELECT_DOWN_LINE
                        Keys.PageUp -> EditorCommand.SELECT_UP_PAGE
                        Keys.PageDown -> EditorCommand.SELECT_DOWN_PAGE
                        Keys.MoveHome -> EditorCommand.SELECT_START_LINE
                        Keys.MoveEnd -> EditorCommand.SELECT_END_LINE
                        Keys.Insert -> EditorCommand.PASTE
                        Keys.Tab -> EditorCommand.TAB_SHIFT
                        Keys.Enter -> EditorCommand.ENTER_SHIFT
                        else -> null
                    }
                else ->
                    when (event.key) {
                        Keys.DirectionLeft -> EditorCommand.MOVE_CURSOR_LEFT_CHAR
                        Keys.DirectionRight -> EditorCommand.MOVE_CURSOR_RIGHT_CHAR
                        Keys.DirectionUp -> EditorCommand.MOVE_CURSOR_UP_LINE
                        Keys.DirectionDown -> EditorCommand.MOVE_CURSOR_DOWN_LINE
                        Keys.PageUp -> EditorCommand.MOVE_CURSOR_UP_PAGE
                        Keys.PageDown -> EditorCommand.MOVE_CURSOR_DOWN_PAGE
                        Keys.MoveHome -> EditorCommand.MOVE_CURSOR_START_LINE
                        Keys.MoveEnd -> EditorCommand.MOVE_CURSOR_END_LINE
                        Keys.Enter -> EditorCommand.ENTER
                        Keys.Backspace -> EditorCommand.DELETE_PREV_CHAR
                        Keys.Delete -> EditorCommand.DELETE_NEXT_CHAR
                        Keys.Paste -> EditorCommand.PASTE
                        Keys.Cut -> EditorCommand.CUT
                        Keys.Tab -> EditorCommand.TAB
                        Keys.Copy -> EditorCommand.COPY
                        Keys.Escape -> WindowCommand.ESCAPE
                        else -> null
                    }
            }
        }
    }

    object DefaultKeyMapper : KeyMapper {
        override fun map(event: KeyEvent): Command? {
            return when {
                event.isShiftPressed && event.isCtrlPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> EditorCommand.SELECT_LEFT_WORD
                        Keys.DirectionRight -> EditorCommand.SELECT_RIGHT_WORD
                        Keys.DirectionUp -> EditorCommand.SELECT_PREV_PARAGRAPH
                        Keys.DirectionDown -> EditorCommand.SELECT_NEXT_PARAGRAPH
                        else -> null
                    }
                event.isCtrlPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> EditorCommand.MOVE_CURSOR_LEFT_WORD
                        Keys.DirectionRight -> EditorCommand.MOVE_CURSOR_RIGHT_WORD
                        Keys.DirectionUp -> EditorCommand.MOVE_CURSOR_PREV_PARAGRAPH
                        Keys.DirectionDown -> EditorCommand.MOVE_CURSOR_NEXT_PARAGRAPH
                        Keys.H -> EditorCommand.DELETE_PREV_CHAR
                        Keys.Delete -> EditorCommand.DELETE_NEXT_WORD
                        Keys.Backspace -> EditorCommand.DELETE_PREV_WORD
                        Keys.Backslash -> EditorCommand.SELECT_NONE
                        else -> null
                    }
                event.isShiftPressed ->
                    when (event.key) {
                        Keys.MoveHome -> EditorCommand.SELECT_HOME
                        Keys.MoveEnd -> EditorCommand.SELECT_END
                        else -> null
                    }
                else -> null
            } ?: CommonKeyMapper.map(event, KeyEvent::isCtrlPressed)
        }
    }

    object MacOSKeyMapper : KeyMapper {
        override fun map(event: KeyEvent): Command? {
            return when {
                event.isMetaPressed && event.isCtrlPressed ->
                    when (event.key) {
                        Keys.Space -> EditorCommand.EMOJI_WINDOW
                        else -> null
                    }
                event.isShiftPressed && event.isAltPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> EditorCommand.SELECT_LEFT_WORD
                        Keys.DirectionRight -> EditorCommand.SELECT_RIGHT_WORD
                        Keys.DirectionUp -> EditorCommand.SELECT_PREV_PARAGRAPH
                        Keys.DirectionDown -> EditorCommand.SELECT_NEXT_PARAGRAPH
                        else -> null
                    }
                event.isShiftPressed && event.isMetaPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> EditorCommand.SELECT_LEFT_LINE
                        Keys.DirectionRight -> EditorCommand.SELECT_RIGHT_LINE
                        Keys.DirectionUp -> EditorCommand.SELECT_HOME
                        Keys.DirectionDown -> EditorCommand.SELECT_END
                        else -> null
                    }

                event.isMetaPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> EditorCommand.MOVE_CURSOR_LEFT_LINE
                        Keys.DirectionRight -> EditorCommand.MOVE_CURSOR_RIGHT_LINE
                        Keys.DirectionUp -> EditorCommand.MOVE_CURSOR_HOME
                        Keys.DirectionDown -> EditorCommand.MOVE_CURSOR_END
                        Keys.Backspace -> EditorCommand.DELETE_START_LINE
                        else -> null
                    }

                // Emacs-like shortcuts
                event.isCtrlPressed && event.isShiftPressed && event.isAltPressed -> {
                    when (event.key) {
                        Keys.F -> EditorCommand.SELECT_RIGHT_WORD
                        Keys.B -> EditorCommand.SELECT_LEFT_WORD
                        else -> null
                    }
                }
                event.isCtrlPressed && event.isAltPressed -> {
                    when (event.key) {
                        Keys.F -> EditorCommand.MOVE_CURSOR_RIGHT_WORD
                        Keys.B -> EditorCommand.MOVE_CURSOR_LEFT_WORD
                        else -> null
                    }
                }
                event.isCtrlPressed && event.isShiftPressed -> {
                    when (event.key) {
                        Keys.F -> EditorCommand.SELECT_RIGHT_CHAR
                        Keys.B -> EditorCommand.SELECT_LEFT_CHAR
                        Keys.P -> EditorCommand.SELECT_UP_LINE
                        Keys.N -> EditorCommand.SELECT_DOWN_LINE
                        Keys.A -> EditorCommand.SELECT_START_LINE
                        Keys.E -> EditorCommand.SELECT_END_LINE
                        else -> null
                    }
                }
                event.isCtrlPressed -> {
                    when (event.key) {
                        Keys.F -> EditorCommand.MOVE_CURSOR_LEFT_CHAR
                        Keys.B -> EditorCommand.MOVE_CURSOR_RIGHT_CHAR
                        Keys.P -> EditorCommand.MOVE_CURSOR_UP_LINE
                        Keys.N -> EditorCommand.MOVE_CURSOR_DOWN_LINE
                        Keys.A -> EditorCommand.MOVE_CURSOR_START_LINE
                        Keys.E -> EditorCommand.MOVE_CURSOR_END_LINE
                        Keys.H -> EditorCommand.DELETE_PREV_CHAR
                        Keys.D -> EditorCommand.DELETE_NEXT_CHAR
                        Keys.K -> EditorCommand.DELETE_END_LINE
                        Keys.O -> EditorCommand.ENTER
                        else -> null
                    }
                }
                // end of emacs-like shortcuts

                event.isShiftPressed ->
                    when (event.key) {
                        Keys.MoveHome -> EditorCommand.SELECT_HOME
                        Keys.MoveEnd -> EditorCommand.SELECT_END
                        else -> null
                    }
                event.isAltPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> EditorCommand.MOVE_CURSOR_LEFT_WORD
                        Keys.DirectionRight -> EditorCommand.MOVE_CURSOR_RIGHT_WORD
                        Keys.DirectionUp -> EditorCommand.MOVE_CURSOR_PREV_PARAGRAPH
                        Keys.DirectionDown -> EditorCommand.MOVE_CURSOR_NEXT_PARAGRAPH
                        Keys.Delete -> EditorCommand.DELETE_NEXT_WORD
                        Keys.Backspace -> EditorCommand.DELETE_PREV_WORD
                        else -> null
                    }
                else -> null
            } ?: CommonKeyMapper.map(event, KeyEvent::isMetaPressed)
        }
    }
}