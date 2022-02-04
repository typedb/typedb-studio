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

import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.view.editor.InputTarget.Cursor
import com.vaticle.typedb.studio.view.editor.InputTarget.Cursor.Companion.min
import com.vaticle.typedb.studio.view.editor.InputTarget.Selection
import kotlin.streams.toList

internal data class TextChange(val operations: List<Operation>) {

    constructor(vararg operations: Operation) : this(operations.asList())

    enum class ReplayType { UNDO, REDO }

    companion object {
        fun merge(changes: List<TextChange>): TextChange {
            return TextChange(changes.flatMap { it.operations }.toList())
        }
    }

    fun invert(): TextChange {
        return TextChange(operations.reversed().map { it.invert() })
    }

    override fun toString(): String {
        val operations = operations.stream().map { it.toString() }.toList().toMutableList()
        operations.add(0, "TextChange {")
        operations.add("}")

        return when {
            operations.size > 3 -> operations.joinToString(separator = "\n")
            else -> operations.joinToString(separator = " ")
        }
    }

    fun summaryTarget(): Either<Cursor, Selection> {
        assert(operations.isNotEmpty())
        var summary = when (val first = operations.first()) {
            is Deletion -> Selection(first.selection().min, first.selection().min)
            is Insertion -> first.selection()
        }
        operations.stream().skip(1).forEach { operation ->
            val selection = operation.selection()
            if (selection.start != summary.end && selection.end != summary.start) {
                val cursor = operations.last().selection().end
                summary = Selection(cursor, cursor)
                return@forEach
            } else summary = when (operation) {
                is Deletion -> Selection(min(summary.start, selection.min), selection.min)
                is Insertion -> Selection(summary.start, selection.end)
            }
        }
        return when (summary.start) {
            summary.end -> Either.first(summary.start)
            else -> Either.second(summary)
        }
    }

    sealed class Operation(val cursor: Cursor, val text: String, private var selection: Selection? = null) {

        init {
            assert(selection == null || selection == selection(false))
        }

        abstract fun invert(): Operation

        fun selection(): Selection {
            return selection(true)
        }

        private fun selection(cache: Boolean): Selection {
            selection?.let { return it }
            assert(text.isNotEmpty())
            val texts = text.split("\n")
            val endRow = cursor.row + texts.size - 1
            val endCol = when {
                texts.size > 1 -> texts[texts.size - 1].length
                else -> cursor.col + texts[0].length
            }
            val newSelection = Selection(cursor, Cursor(endRow, endCol))
            if (cache) selection = newSelection
            return newSelection
        }

    }

    class Insertion(cursor: Cursor, text: String) : Operation(cursor, text) {
        override fun invert(): Deletion {
            return Deletion(cursor, text)
        }

        override fun toString(): String {
            return "Insertion { cursor: ${cursor.label()}, text: $text }"
        }
    }

    // Note that it is not canonical to provide 'selection' as argument, but we provide it for performance
    class Deletion(cursor: Cursor, text: String, selection: Selection? = null) :
        Operation(cursor, text, selection) {
        override fun invert(): Insertion {
            return Insertion(cursor, text)
        }

        override fun toString(): String {
            return "Deletion { selection: ${selection().label()} }"
        }
    }
}