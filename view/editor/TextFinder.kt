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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.studio.state.project.File

internal class TextFinder(private val file: File, private val target: InputTarget) {

    internal val content: SnapshotStateList<String> get() = file.content
    internal var showFinder by mutableStateOf(false)
    internal var showReplacer by mutableStateOf(false)
    internal var findText by mutableStateOf("")
    internal var replaceText by mutableStateOf("")
    internal var isCaseSensitive by mutableStateOf(false)
    internal val density: Float get() = target.density
    internal val status: String get() = "11 / 23462" // TODO

    internal fun showFinder() {
        showFinder = true
        showReplacer = false
        if (target.selection != null) findText = target.selectedText()
    }

    internal fun showReplacer() {
        showReplacer = true
        if (target.selection != null) findText = target.selectedText()
    }

    fun toggleCaseSensitive() {
        isCaseSensitive = !isCaseSensitive
    }
}