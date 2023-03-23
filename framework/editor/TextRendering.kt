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

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.TextLayoutResult
import com.vaticle.typedb.studio.framework.editor.common.GlyphLine
import mu.KotlinLogging

/**
 * This class is a wrapper over [TextLayoutResult] which is produced after
 * [androidx.compose.material.Text] completes "rendering" and is saved when the
 * onTextLayout() callback function called. This class allows us to recycle
 * [TextLayoutResult] after they get deleted, or initialise a text line with a
 * subsequent line's [TextLayoutResult] in case they are identical. We initialise
 * a line with a subsequent line's [TextLayoutResult] as it is possible that they
 * are identical and Compose may not recompose that Text and simply reuse the
 * previously identical text line on that same position.
 */
internal class TextRendering {

    private var results = initResults(0)

    private fun initResults(initSize: Int): SnapshotStateList<TextLayoutResult?> =
        mutableStateListOf<TextLayoutResult?>().apply { addAll(List(initSize) { null }) }

    fun reinitialize(initSize: Int) {
        results = initResults(initSize)
    }

    fun get(int: Int): TextLayoutResult? = results.getOrNull(int)

    fun set(int: Int, layout: TextLayoutResult) {
        if (int >= results.size) addNewLines(results.size, int + 1 - results.size)
        results[int] = layout
    }

    fun invalidate(change: TextChange) {
        change.operations.forEach {
            val start = it.selection().min.row
            val end = it.selection().max.row.coerceIn(start, results.size - 1)
            val lines = start .. end
            val lineTextPairs = it.text.zip(lines)
            lineTextPairs.forEach { (text, line) ->
                results[line]?.let { textLayout ->
                    if (it.cursor.col != GlyphLine(textLayout.layoutInput.text).length || !text.isEmpty()) {
                        results[line] = null
                    }
                }
            }
        }
    }

    fun removeRange(startInc: Int, endExc: Int) {
        results.removeRange(startInc, endExc)
    }

    fun addNewLines(index: Int, size: Int) {
        results.addAll(index, List(size) { null })
    }
}