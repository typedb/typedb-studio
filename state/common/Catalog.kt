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

package com.vaticle.typedb.studio.state.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

abstract class Catalog<T : Catalog.Item<T>> {

    interface Item<U : Item<U>> {

        val name: String
        val info: String?
        val isExpandable: Boolean

        fun asExpandable(): Expandable<U>

        interface Expandable<V : Item<V>> : Item<V> {

            val isExpanded: Boolean
            val entries: List<V>
            fun toggle()
            fun expand()
            fun collapse()
        }
    }

    abstract val items: List<T>
    abstract fun open(item: T)

    private var selected: T? by mutableStateOf(null)

    fun select(item: T) {
        selected = item
    }

    fun isSelected(item: T): Boolean {
        return selected == item
    }

    fun selectNext(item: T) {
        println("Select next from: $selected")
    }

    fun selectPrevious(item: T) {
        println("Select previous from: $selected")
    }

    fun selectParent(item: T) {
        println("Select parent from: $selected")
    }
}
