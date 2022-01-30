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

import java.util.stream.Collectors

internal data class TextChange(val operations: List<Operation>) {

    constructor(vararg operations: Operation) : this(operations.asList())

    enum class Type { ORIGINAL, UNDO, REDO }

    companion object {
        fun merge(changes: List<TextChange>): TextChange {
            return TextChange(changes.flatMap { it.operations }.toList())
        }
    }

    fun invert(): TextChange {
        return TextChange(operations.reversed().map { it.invert() })
    }

    override fun toString(): String {
        return "TextChange {\n" + operations.stream().map { it.toString() }.collect(Collectors.joining(",\n")) + "\n}"
    }

    sealed class Operation(
        val cursor: InputTarget.Cursor, val text: String, private var selection: InputTarget.Selection? = null
    ) {

        init {
            assert(selection == null || selection == selection(false))
        }

        abstract fun invert(): Operation

        fun selection(): InputTarget.Selection {
            return selection(true)
        }

        private fun selection(cache: Boolean): InputTarget.Selection {
            selection?.let { return it }
            assert(text.isNotEmpty())
            val texts = text.split("\n")
            val endRow = cursor.row + texts.size - 1
            val endCol = when {
                texts.size > 1 -> texts[texts.size - 1].length
                else -> cursor.col + texts[0].length
            }
            val newSelection = InputTarget.Selection(cursor, InputTarget.Cursor(endRow, endCol))
            if (cache) selection = newSelection
            return newSelection
        }

    }

    class Insertion(cursor: InputTarget.Cursor, text: String) : Operation(cursor, text) {
        override fun invert(): Deletion {
            return Deletion(cursor, text)
        }

        override fun toString(): String {
            return "Insertion { cursor: ${cursor.label()}, text: $text }"
        }
    }

    // Note that it is not canonical to provide 'selection' as argument, but we provide it for performance
    class Deletion(cursor: InputTarget.Cursor, text: String, selection: InputTarget.Selection? = null) :
        Operation(cursor, text, selection) {
        override fun invert(): Insertion {
            return Insertion(cursor, text)
        }

        override fun toString(): String {
            return "Deletion { selection: ${selection().label()} }"
        }
    }
}