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

interface Catalog<T : Catalog.Item<T>> {

    val items: List<T>

    fun open(item: T)
    fun select(item: T)
    fun selectNext()
    fun selectPrevious()
    fun isSelected(item: T): Boolean

    interface Item<U : Item<U>> {

        val name: String
        val info: String?
        val isExpandable: Boolean

        fun asExpandable(): Expandable<U>

        interface Expandable<V : Item<V>> : Item<V> {

            val isExpanded: Boolean
            val entries: List<V>
            fun toggle()
        }
    }
}
