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

package com.vaticle.typedb.studio.view.common

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Property.OS
import java.awt.event.KeyEvent.KEY_LOCATION_NUMPAD

interface KeyMapper {

    val modKey: String

    fun map(event: KeyEvent): Command?

    companion object {

        val CURRENT: KeyMapper = when (OS.Current) {
            OS.MACOS -> MacOSKeyMapper
            else -> DefaultKeyMapper
        }
    }

    enum class Command {
        MOVE_CHAR_LEFT,
        MOVE_CHAR_RIGHT,
        MOVE_WORD_RIGHT,
        MOVE_WORD_LEFT,
        MOVE_PARAGRAPH_PREV,
        MOVE_PARAGRAPH_NEXT,
        MOVE_LINE_LEFT,
        MOVE_LINE_RIGHT,
        MOVE_LINE_START,
        MOVE_LINE_END,
        MOVE_LINE_UP,
        MOVE_LINE_DOWN,
        MOVE_PAGE_UP,
        MOVE_PAGE_DOWN,
        MOVE_HOME,
        MOVE_END,

        SELECT_NONE,
        SELECT_ALL,
        SELECT_CHAR_LEFT,
        SELECT_CHAR_RIGHT,
        SELECT_WORD_LEFT,
        SELECT_WORD_RIGHT,
        SELECT_PARAGRAPH_NEXT,
        SELECT_PARAGRAPH_PREV,
        SELECT_LINE_LEFT,
        SELECT_LINE_RIGHT,
        SELECT_LINE_START,
        SELECT_LINE_END,
        SELECT_LINE_UP,
        SELECT_LINE_DOWN,
        SELECT_PAGE_UP,
        SELECT_PAGE_DOWN,
        SELECT_HOME,
        SELECT_END,

        DELETE_CHAR_PREV,
        DELETE_CHAR_NEXT,
        DELETE_WORD_PREV,
        DELETE_WORD_NEXT,
        DELETE_LINE_START,
        DELETE_LINE_END,

        ENTER,
        ENTER_SHIFT,
        TAB,
        TAB_SHIFT,
        TOGGLE_COMMENT,
        MOD_ENTER,
        MOD_ENTER_SHIFT,
        CTRL_TAB,
        CTRL_TAB_SHIFT,

        COPY,
        PASTE,
        CUT,

        DUPLICATE,
        REORDER_LINES_UP,
        REORDER_LINES_DOWN,

        UNDO,
        REDO,

        TEXT_SIZE_INCREASE,
        TEXT_SIZE_DECREASE,
        TEXT_SIZE_RESET,

        FIND,
        REPLACE,
        NEW_PAGE,
        ESCAPE,
        SAVE,
        CLOSE,
        QUIT,

        EMOJI_WINDOW,
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
        val Q: Key = Key(java.awt.event.KeyEvent.VK_Q)
        val R: Key = Key(java.awt.event.KeyEvent.VK_R)
        val S: Key = Key(java.awt.event.KeyEvent.VK_S)
        val T: Key = Key(java.awt.event.KeyEvent.VK_T)
        val V: Key = Key(java.awt.event.KeyEvent.VK_V)
        val W: Key = Key(java.awt.event.KeyEvent.VK_W)
        val X: Key = Key(java.awt.event.KeyEvent.VK_X)
        val Z: Key = Key(java.awt.event.KeyEvent.VK_Z)
        val Slash: Key = Key(java.awt.event.KeyEvent.VK_SLASH)
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
        val EnterNumPad: Key = Key(java.awt.event.KeyEvent.VK_ENTER, KEY_LOCATION_NUMPAD)
        val Backspace: Key = Key(java.awt.event.KeyEvent.VK_BACK_SPACE)
        val Delete: Key = Key(java.awt.event.KeyEvent.VK_DELETE)
        val Paste: Key = Key(java.awt.event.KeyEvent.VK_PASTE)
        val Cut: Key = Key(java.awt.event.KeyEvent.VK_CUT)
        val Copy: Key = Key(java.awt.event.KeyEvent.VK_COPY)
        val Tab: Key = Key(java.awt.event.KeyEvent.VK_TAB)
        val Space: Key = Key(java.awt.event.KeyEvent.VK_SPACE)
        val Escape: Key = Key(java.awt.event.KeyEvent.VK_ESCAPE)
        val Zero: Key = Key(java.awt.event.KeyEvent.VK_0)
        val Minus: Key = Key(java.awt.event.KeyEvent.VK_MINUS)
        val Equals: Key = Key(java.awt.event.KeyEvent.VK_EQUALS)
    }

    private object CommonKeyMapper {
        fun map(event: KeyEvent, shortcutModifier: (KeyEvent) -> Boolean): Command? {
            return when {
                shortcutModifier(event) && event.isShiftPressed -> when (event.key) {
                    Keys.Z -> Command.REDO
                    Keys.Enter, Keys.EnterNumPad -> Command.MOD_ENTER_SHIFT
                    Keys.DirectionLeft -> Command.SELECT_LINE_LEFT
                    Keys.DirectionRight -> Command.SELECT_LINE_RIGHT
                    Keys.DirectionUp -> Command.REORDER_LINES_UP
                    Keys.DirectionDown -> Command.REORDER_LINES_DOWN
                    else -> null
                }
                shortcutModifier(event) -> when (event.key) {
                    Keys.C, Keys.Insert -> Command.COPY
                    Keys.V -> Command.PASTE
                    Keys.X -> Command.CUT
                    Keys.D -> Command.DUPLICATE
                    Keys.A -> Command.SELECT_ALL
                    Keys.Z -> Command.UNDO
                    Keys.F -> Command.FIND
                    Keys.S -> Command.SAVE
                    Keys.W -> Command.CLOSE
                    Keys.N, Keys.T -> Command.NEW_PAGE
                    Keys.Enter, Keys.EnterNumPad -> Command.MOD_ENTER
                    Keys.Slash -> Command.TOGGLE_COMMENT
                    Keys.Equals -> Command.TEXT_SIZE_INCREASE
                    Keys.Minus -> Command.TEXT_SIZE_DECREASE
                    Keys.Zero -> Command.TEXT_SIZE_RESET
                    else -> null
                }
                event.isCtrlPressed && event.isShiftPressed -> when (event.key) {
                    Keys.Tab -> Command.CTRL_TAB_SHIFT
                    else -> null
                }
                event.isCtrlPressed -> when (event.key) {
                    Keys.Tab -> Command.CTRL_TAB
                    else -> null
                }
                event.isAltPressed && event.isShiftPressed -> when (event.key) {
                    Keys.DirectionLeft -> Command.SELECT_WORD_LEFT
                    Keys.DirectionRight -> Command.SELECT_WORD_RIGHT
                    Keys.DirectionUp -> Command.SELECT_PARAGRAPH_PREV
                    Keys.DirectionDown -> Command.SELECT_PARAGRAPH_NEXT
                    else -> null
                }
                event.isShiftPressed -> when (event.key) {
                    Keys.DirectionLeft -> Command.SELECT_CHAR_LEFT
                    Keys.DirectionRight -> Command.SELECT_CHAR_RIGHT
                    Keys.DirectionUp -> Command.SELECT_LINE_UP
                    Keys.DirectionDown -> Command.SELECT_LINE_DOWN
                    Keys.PageUp -> Command.SELECT_PAGE_UP
                    Keys.PageDown -> Command.SELECT_PAGE_DOWN
                    Keys.MoveHome -> Command.SELECT_HOME
                    Keys.MoveEnd -> Command.SELECT_END
                    Keys.Insert -> Command.PASTE
                    Keys.Tab -> Command.TAB_SHIFT
                    Keys.Enter, Keys.EnterNumPad -> Command.ENTER_SHIFT
                    else -> null
                }
                else -> when (event.key) {
                    Keys.DirectionLeft -> Command.MOVE_CHAR_LEFT
                    Keys.DirectionRight -> Command.MOVE_CHAR_RIGHT
                    Keys.DirectionUp -> Command.MOVE_LINE_UP
                    Keys.DirectionDown -> Command.MOVE_LINE_DOWN
                    Keys.PageUp -> Command.MOVE_PAGE_UP
                    Keys.PageDown -> Command.MOVE_PAGE_DOWN
                    Keys.MoveHome -> Command.MOVE_LINE_START
                    Keys.MoveEnd -> Command.MOVE_LINE_END
                    Keys.Enter, Keys.EnterNumPad -> Command.ENTER
                    Keys.Backspace -> Command.DELETE_CHAR_PREV
                    Keys.Delete -> Command.DELETE_CHAR_NEXT
                    Keys.Paste -> Command.PASTE
                    Keys.Cut -> Command.CUT
                    Keys.Tab -> Command.TAB
                    Keys.Copy -> Command.COPY
                    Keys.Escape -> Command.ESCAPE
                    else -> null
                }
            }
        }
    }

    object DefaultKeyMapper : KeyMapper {

        override val modKey: String = Label.CTRL

        override fun map(event: KeyEvent): Command? {
            return when {
                event.isCtrlPressed && event.isShiftPressed -> when (event.key) {
                    // no unique keys to filter, but necessary to route event to CommonKeyMapper
                    else -> null
                }
                event.isCtrlPressed -> when (event.key) {
                    Keys.DirectionLeft -> Command.MOVE_WORD_LEFT
                    Keys.DirectionRight -> Command.MOVE_WORD_RIGHT
                    Keys.DirectionUp -> Command.MOVE_PARAGRAPH_PREV
                    Keys.DirectionDown -> Command.MOVE_PARAGRAPH_NEXT
                    Keys.Delete -> Command.DELETE_WORD_NEXT
                    Keys.Backspace -> Command.DELETE_WORD_PREV
                    Keys.Backslash -> Command.SELECT_NONE
                    Keys.H -> Command.REPLACE
                    else -> null
                }
                else -> null
            } ?: CommonKeyMapper.map(event, KeyEvent::isCtrlPressed)
        }
    }

    object MacOSKeyMapper : KeyMapper {

        override val modKey: String = Label.CMD

        override fun map(event: KeyEvent): Command? {
            return when {
                event.isMetaPressed && event.isCtrlPressed -> when (event.key) {
                    Keys.Space -> Command.EMOJI_WINDOW
                    else -> null
                }
                event.isMetaPressed && event.isShiftPressed -> when (event.key) {
                    // no unique keys to filter, but necessary to route event to CommonKeyMapper
                    else -> null
                }
                event.isMetaPressed -> when (event.key) {
                    Keys.DirectionLeft -> Command.MOVE_LINE_LEFT
                    Keys.DirectionRight -> Command.MOVE_LINE_RIGHT
                    Keys.DirectionUp -> Command.MOVE_HOME
                    Keys.DirectionDown -> Command.MOVE_END
                    Keys.Backspace -> Command.DELETE_LINE_START
                    Keys.Q -> Command.QUIT
                    else -> null
                }
                // Emacs-like shortcuts
                event.isCtrlPressed && event.isAltPressed && event.isShiftPressed -> when (event.key) {
                    Keys.F -> Command.SELECT_WORD_RIGHT
                    Keys.B -> Command.SELECT_WORD_LEFT
                    else -> null
                }
                event.isCtrlPressed && event.isAltPressed -> when (event.key) {
                    Keys.F -> Command.MOVE_WORD_RIGHT
                    Keys.B -> Command.MOVE_WORD_LEFT
                    else -> null
                }
                event.isCtrlPressed && event.isShiftPressed -> when (event.key) {
                    Keys.F -> Command.SELECT_CHAR_RIGHT
                    Keys.B -> Command.SELECT_CHAR_LEFT
                    Keys.P -> Command.SELECT_LINE_UP
                    Keys.N -> Command.SELECT_LINE_DOWN
                    Keys.A -> Command.SELECT_LINE_START
                    Keys.E -> Command.SELECT_LINE_END
                    else -> null
                }
                event.isCtrlPressed -> when (event.key) {
                    Keys.F -> Command.MOVE_CHAR_LEFT
                    Keys.B -> Command.MOVE_CHAR_RIGHT
                    Keys.P -> Command.MOVE_LINE_UP
                    Keys.N -> Command.MOVE_LINE_DOWN
                    Keys.A -> Command.MOVE_LINE_START
                    Keys.E -> Command.MOVE_LINE_END
                    Keys.H -> Command.DELETE_CHAR_PREV
                    Keys.D -> Command.DELETE_CHAR_NEXT
                    Keys.K -> Command.DELETE_LINE_END
                    Keys.O -> Command.ENTER
                    Keys.R -> Command.REPLACE
                    else -> null
                }
                event.isAltPressed && event.isShiftPressed -> when (event.key) {
                    // no unique keys to filter, but necessary to route event to CommonKeyMapper
                    else -> null
                }
                event.isAltPressed -> when (event.key) {
                    Keys.DirectionLeft -> Command.MOVE_WORD_LEFT
                    Keys.DirectionRight -> Command.MOVE_WORD_RIGHT
                    Keys.DirectionUp -> Command.MOVE_PARAGRAPH_PREV
                    Keys.DirectionDown -> Command.MOVE_PARAGRAPH_NEXT
                    Keys.Delete -> Command.DELETE_WORD_NEXT
                    Keys.Backspace -> Command.DELETE_WORD_PREV
                    else -> null
                }
                else -> null
            } ?: CommonKeyMapper.map(event, KeyEvent::isMetaPressed)
        }
    }
}