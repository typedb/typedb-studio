/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.editor

import com.typedb.studio.framework.editor.InputTarget.Cursor
import com.typedb.studio.framework.editor.InputTarget.Cursor.Companion.min
import com.typedb.studio.framework.editor.InputTarget.Selection
import com.typedb.studio.framework.editor.common.GlyphLine
import com.vaticle.typedb.common.collection.Either
import kotlin.streams.toList

internal class TextChange(val operations: List<Operation>) {

    constructor(vararg operations: Operation) : this(operations.asList())

    enum class ReplayType { UNDO, REDO }

    private var target: Either<Cursor, Selection>? = null

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

    fun lines(): IntRange {
        return target().let {
            when {
                it.isFirst -> IntRange(start = it.first().row, endInclusive = it.first().row)
                else -> IntRange(start = it.second().min.row, endInclusive = it.second().max.row)
            }
        }
    }

    fun target(): Either<Cursor, Selection> {
        assert(operations.isNotEmpty())
        if (target != null) return target!!

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
        target = when (summary.start) {
            summary.end -> Either.first(summary.start)
            else -> Either.second(summary)
        }
        return target!!
    }

    sealed class Operation(
        val cursor: Cursor,
        val text: List<GlyphLine>,
        private var selection: Selection? = null
    ) {

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
            val endRow = cursor.row + text.size - 1
            val endCol = when {
                text.size > 1 -> text[text.size - 1].length
                else -> cursor.col + text[0].length
            }
            val newSelection = Selection(cursor, Cursor(endRow, endCol))
            if (cache) selection = newSelection
            return newSelection
        }
    }

    class Insertion constructor(
        cursor: Cursor,
        text: List<GlyphLine>
    ) : Operation(cursor, text) {

        override fun invert(): Deletion {
            return Deletion(cursor, text)
        }

        override fun toString(): String {
            return "Insertion { cursor: ${cursor.label()}, text: $text }"
        }
    }

    // Note that it is not canonical to provide 'selection' as argument, but we provide it for performance
    class Deletion constructor(
        cursor: Cursor,
        text: List<GlyphLine>,
        selection: Selection? = null
    ) : Operation(cursor, text, selection) {

        override fun invert(): Insertion {
            return Insertion(cursor, text)
        }

        override fun toString(): String {
            return "Deletion { selection: ${selection().label()} }"
        }
    }
}
