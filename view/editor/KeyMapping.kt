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

internal interface KeyMapping {

    fun map(event: KeyEvent): Command?

    companion object {

        internal val CURRENT_KEY_MAPPING: KeyMapping = when (OS.Current) {
            OS.MACOS -> MacOSKeyMapping
            else -> DefaultKeyMapping
        }
    }

    enum class Command(val editsText: Boolean) { // TODO: do we need 'editsText' boolean?
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

        INSERT_NEW_LINE(true),
        INSERT_TAB(true),

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

        CHARACTER_PALETTE(false)
    }

    private enum class OS {
        LINUX,
        WINDOWS,
        MACOS,
        UNKNOWN;

        companion object {
            val Current: OS by lazy {
                val name = System.getProperty("os.name")
                when {
                    name?.startsWith("Linux") == true -> LINUX
                    name?.startsWith("Win") == true -> WINDOWS
                    name == "Mac OS X" -> MACOS
                    else -> UNKNOWN
                }
            }
        }
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
        val V: Key = Key(java.awt.event.KeyEvent.VK_V)
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

    private object CommonKeyMapping {
        fun map(event: KeyEvent, shortcutModifier: (KeyEvent) -> Boolean): Command? {
            return when {
                shortcutModifier(event) && event.isShiftPressed ->
                    when (event.key) {
                        Keys.Z -> Command.REDO
                        else -> null
                    }
                shortcutModifier(event) ->
                    when (event.key) {
                        Keys.C, Keys.Insert -> Command.COPY
                        Keys.V -> Command.PASTE
                        Keys.X -> Command.CUT
                        Keys.A -> Command.SELECT_ALL
                        Keys.Z -> Command.UNDO
                        else -> null
                    }
                event.isCtrlPressed -> null
                event.isShiftPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> Command.SELECT_LEFT_CHAR
                        Keys.DirectionRight -> Command.SELECT_RIGHT_CHAR
                        Keys.DirectionUp -> Command.SELECT_UP_LINE
                        Keys.DirectionDown -> Command.SELECT_DOWN_LINE
                        Keys.PageUp -> Command.SELECT_UP_PAGE
                        Keys.PageDown -> Command.SELECT_DOWN_PAGE
                        Keys.MoveHome -> Command.SELECT_START_LINE
                        Keys.MoveEnd -> Command.SELECT_END_LINE
                        Keys.Insert -> Command.PASTE
                        else -> null
                    }
                else ->
                    when (event.key) {
                        Keys.DirectionLeft -> Command.MOVE_CURSOR_LEFT_CHAR
                        Keys.DirectionRight -> Command.MOVE_CURSOR_RIGHT_CHAR
                        Keys.DirectionUp -> Command.MOVE_CURSOR_UP_LINE
                        Keys.DirectionDown -> Command.MOVE_CURSOR_DOWN_LINE
                        Keys.PageUp -> Command.MOVE_CURSOR_UP_PAGE
                        Keys.PageDown -> Command.MOVE_CURSOR_DOWN_PAGE
                        Keys.MoveHome -> Command.MOVE_CURSOR_START_LINE
                        Keys.MoveEnd -> Command.MOVE_CURSOR_END_LINE
                        Keys.Enter -> Command.INSERT_NEW_LINE
                        Keys.Backspace -> Command.DELETE_PREV_CHAR
                        Keys.Delete -> Command.DELETE_NEXT_CHAR
                        Keys.Paste -> Command.PASTE
                        Keys.Cut -> Command.CUT
                        Keys.Tab -> Command.INSERT_TAB
                        Keys.Escape -> Command.SELECT_NONE
                        Keys.Copy -> Command.COPY
                        else -> null
                    }
            }
        }
    }

    object DefaultKeyMapping : KeyMapping {
        override fun map(event: KeyEvent): Command? {
            return when {
                event.isShiftPressed && event.isCtrlPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> Command.SELECT_LEFT_WORD
                        Keys.DirectionRight -> Command.SELECT_RIGHT_WORD
                        Keys.DirectionUp -> Command.SELECT_PREV_PARAGRAPH
                        Keys.DirectionDown -> Command.SELECT_NEXT_PARAGRAPH
                        else -> null
                    }
                event.isCtrlPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> Command.MOVE_CURSOR_LEFT_WORD
                        Keys.DirectionRight -> Command.MOVE_CURSOR_RIGHT_WORD
                        Keys.DirectionUp -> Command.MOVE_CURSOR_PREV_PARAGRAPH
                        Keys.DirectionDown -> Command.MOVE_CURSOR_NEXT_PARAGRAPH
                        Keys.H -> Command.DELETE_PREV_CHAR
                        Keys.Delete -> Command.DELETE_NEXT_WORD
                        Keys.Backspace -> Command.DELETE_PREV_WORD
                        Keys.Backslash -> Command.SELECT_NONE
                        else -> null
                    }
                event.isShiftPressed ->
                    when (event.key) {
                        Keys.MoveHome -> Command.SELECT_HOME
                        Keys.MoveEnd -> Command.SELECT_END
                        else -> null
                    }
                else -> null
            } ?: CommonKeyMapping.map(event, KeyEvent::isCtrlPressed)
        }
    }

    object MacOSKeyMapping : KeyMapping {
        override fun map(event: KeyEvent): Command? {
            return when {
                event.isMetaPressed && event.isCtrlPressed ->
                    when (event.key) {
                        Keys.Space -> Command.CHARACTER_PALETTE
                        else -> null
                    }
                event.isShiftPressed && event.isAltPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> Command.SELECT_LEFT_WORD
                        Keys.DirectionRight -> Command.SELECT_RIGHT_WORD
                        Keys.DirectionUp -> Command.SELECT_PREV_PARAGRAPH
                        Keys.DirectionDown -> Command.SELECT_NEXT_PARAGRAPH
                        else -> null
                    }
                event.isShiftPressed && event.isMetaPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> Command.SELECT_LEFT_LINE
                        Keys.DirectionRight -> Command.SELECT_RIGHT_LINE
                        Keys.DirectionUp -> Command.SELECT_HOME
                        Keys.DirectionDown -> Command.SELECT_END
                        else -> null
                    }

                event.isMetaPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> Command.MOVE_CURSOR_LEFT_LINE
                        Keys.DirectionRight -> Command.MOVE_CURSOR_RIGHT_LINE
                        Keys.DirectionUp -> Command.MOVE_CURSOR_HOME
                        Keys.DirectionDown -> Command.MOVE_CURSOR_END
                        Keys.Backspace -> Command.DELETE_START_LINE
                        else -> null
                    }

                // Emacs-like shortcuts
                event.isCtrlPressed && event.isShiftPressed && event.isAltPressed -> {
                    when (event.key) {
                        Keys.F -> Command.SELECT_RIGHT_WORD
                        Keys.B -> Command.SELECT_LEFT_WORD
                        else -> null
                    }
                }
                event.isCtrlPressed && event.isAltPressed -> {
                    when (event.key) {
                        Keys.F -> Command.MOVE_CURSOR_RIGHT_WORD
                        Keys.B -> Command.MOVE_CURSOR_LEFT_WORD
                        else -> null
                    }
                }
                event.isCtrlPressed && event.isShiftPressed -> {
                    when (event.key) {
                        Keys.F -> Command.SELECT_RIGHT_CHAR
                        Keys.B -> Command.SELECT_LEFT_CHAR
                        Keys.P -> Command.SELECT_UP_LINE
                        Keys.N -> Command.SELECT_DOWN_LINE
                        Keys.A -> Command.SELECT_START_LINE
                        Keys.E -> Command.SELECT_END_LINE
                        else -> null
                    }
                }
                event.isCtrlPressed -> {
                    when (event.key) {
                        Keys.F -> Command.MOVE_CURSOR_LEFT_CHAR
                        Keys.B -> Command.MOVE_CURSOR_RIGHT_CHAR
                        Keys.P -> Command.MOVE_CURSOR_UP_LINE
                        Keys.N -> Command.MOVE_CURSOR_DOWN_LINE
                        Keys.A -> Command.MOVE_CURSOR_START_LINE
                        Keys.E -> Command.MOVE_CURSOR_END_LINE
                        Keys.H -> Command.DELETE_PREV_CHAR
                        Keys.D -> Command.DELETE_NEXT_CHAR
                        Keys.K -> Command.DELETE_END_LINE
                        Keys.O -> Command.INSERT_NEW_LINE
                        else -> null
                    }
                }
                // end of emacs-like shortcuts

                event.isShiftPressed ->
                    when (event.key) {
                        Keys.MoveHome -> Command.SELECT_HOME
                        Keys.MoveEnd -> Command.SELECT_END
                        else -> null
                    }
                event.isAltPressed ->
                    when (event.key) {
                        Keys.DirectionLeft -> Command.MOVE_CURSOR_LEFT_WORD
                        Keys.DirectionRight -> Command.MOVE_CURSOR_RIGHT_WORD
                        Keys.DirectionUp -> Command.MOVE_CURSOR_PREV_PARAGRAPH
                        Keys.DirectionDown -> Command.MOVE_CURSOR_NEXT_PARAGRAPH
                        Keys.Delete -> Command.DELETE_NEXT_WORD
                        Keys.Backspace -> Command.DELETE_PREV_WORD
                        else -> null
                    }
                else -> null
            } ?: CommonKeyMapping.map(event, KeyEvent::isMetaPressed)
        }
    }
}