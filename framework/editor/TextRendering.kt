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
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Message.Framework.Companion.UNEXPECTED_ERROR
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
    private var versions = initVersions(0)
    private var deleted = initDeleted()

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

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

    fun get(int: Int): TextLayoutResult? = results.getOrNull(int)

    fun set(int: Int, layout: TextLayoutResult, version: Int) {
        if (int >= results.size) addNew(results.size, int + 1 - results.size)
        results[int] = layout
        versions[int] = version
    }

    fun hasVersion(int: Int, version: Int): Boolean {
        return try {
            if (int >= 0 && int < versions.size) versions[int] == version else false
        } catch (e: Exception) {
            // TODO: Find out why there could be an exception here at all. Last error was:
            // java.lang.IllegalStateException: Reading a state that was created after the snapshot was taken or in a snapshot that has not yet been applied
            // ...
            // at androidx.compose.runtime.snapshots.SnapshotStateList.size(SnapshotStateList.kt:33)
            // at com.vaticle.typedb.studio.framework.editor.TextRendering.hasVersion(TextRendering.kt:65)
            // ...
            Service.notification.systemError(LOGGER, e, UNEXPECTED_ERROR)
            false
        }
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