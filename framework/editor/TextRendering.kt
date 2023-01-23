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
    private var deleted = initDeleted()

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    private fun initResults(initSize: Int): SnapshotStateList<TextLayoutResult?> =
        mutableStateListOf<TextLayoutResult?>().apply { addAll(List(initSize) { null }) }

    private fun initDeleted() = mutableStateMapOf<Int, TextLayoutResult?>()

    fun reinitialize(initSize: Int) {
        results = initResults(initSize)
    }

    fun get(int: Int): TextLayoutResult? = results.getOrNull(int)

    fun set(int: Int, layout: TextLayoutResult) {
        if (int >= results.size) addNew(results.size, int + 1 - results.size)
        results[int] = layout
    }

    fun invalidate(change: TextChange) {
        for (i in change.lines()) {
            if (results.size > i) {
                results[i] = null
            }
        }
    }

    fun removeRange(startInc: Int, endExc: Int) {
        for (i in startInc until endExc) deleted[i] = results[i]
        results.removeRange(startInc, endExc)
    }

    fun addNew(index: Int, size: Int) {
        results.addAll(index, List(size) { deleted.remove(index + it) ?: results.getOrNull(index + it) })
    }
}