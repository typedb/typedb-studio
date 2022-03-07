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

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.TextLayoutResult

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
internal class TextRendering(initSize: Int) {

    private var results = initResults(initSize)
    private var versions = initVersions(initSize)
    private var deleted = initDeleted()

    private fun initResults(initSize: Int): SnapshotStateList<TextLayoutResult?> =
        mutableStateListOf<TextLayoutResult?>().apply { addAll(List(initSize) { null }) }

    private fun initVersions(initSize: Int): SnapshotStateList<Int> =
        mutableStateListOf<Int>().apply { addAll(List(initSize) { 0 }) }

    private fun initDeleted() = mutableStateMapOf<Int, TextLayoutResult?>()

    fun reinitialize(initSize: Int) {
        results = initResults(initSize)
        versions = initVersions(initSize)
        deleted = initDeleted()
    }

    fun get(int: Int): TextLayoutResult? = results[int]

    fun set(int: Int, layout: TextLayoutResult, version: Int) {
        if (int >= results.size) addNew(int, int + 1 - results.size)
        results[int] = layout
        versions[int] = version
    }

    fun hasVersion(int: Int, version: Int): Boolean {
        return if (int >= 0 && int < versions.size) versions[int] == version else false
    }

    fun removeRange(startInc: Int, endExc: Int) {
        for (i in startInc until endExc) deleted[i] = results[i]
        results.removeRange(startInc, endExc)
        versions.removeRange(startInc, endExc)
    }

    fun addNew(index: Int, size: Int) {
        versions.addAll(index, List(size) { 0 })
        results.addAll(index, List(size) { deleted.remove(index + it) ?: results.getOrNull(index + it) })
    }
}