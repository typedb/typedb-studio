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
import androidx.compose.ui.text.TextLayoutResult

internal class TextLayout(initSize: Int) {

    private val layouts = mutableStateListOf<TextLayoutResult?>().apply { addAll(List(initSize) { null }) }
    private val versions = mutableStateListOf<Int>().apply { addAll(List(initSize) { 0 }) }
    private val deleted = mutableStateMapOf<Int, TextLayoutResult?>()

    fun get(int: Int): TextLayoutResult? = layouts[int]

    fun set(int: Int, layout: TextLayoutResult, version: Int) {
        layouts[int] = layout
        versions[int] = version
    }

    fun isRendered(int: Int, version: Int): Boolean {
        return versions[int] == version
    }

    fun removeRange(startInc: Int, endExc: Int) {
        for (i in startInc until endExc) deleted[i] = layouts[i]
        layouts.removeRange(startInc, endExc)
        versions.removeRange(startInc, endExc)
    }

    fun addNew(index: Int, size: Int) {
        versions.addAll(index, List(size) { 0 })
        layouts.addAll(index, List(size) { deleted.remove(index + it) ?: layouts.getOrNull(index + it) })
    }
}