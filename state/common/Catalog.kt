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

import java.util.LinkedList

interface Catalog<T : Catalog.Item<T>> {

    interface Item<U : Item<U>> {

        val name: String
        val parent: U?
        val info: String?
        var focusFn: (() -> Unit)?
        val isExpandable: Boolean get() = false

        fun asExpandable(): Expandable<U> {
            throw TypeCastException("Illegal cast of Catalog.Item to Catalog.Item.Expandable")
        }

        interface Expandable<V : Item<V>> : Item<V> {

            val isExpanded: Boolean
            val entries: List<V>
            override val isExpandable: Boolean get() = true

            override fun asExpandable(): Expandable<V> {
                return this
            }

            fun toggle(isExpanded: Boolean)

            fun toggle() {
                toggle(!isExpanded)
            }

            fun expand() {
                toggle(true)
            }

            fun collapse() {
                toggle(false)
            }
        }
    }

    companion object {
        const val MAX_ITEM_EXPANDED = 512
    }

    val entries: List<T>
    var selected: T?

    fun open(item: T)

    fun expand(onExpandLimitReached: () -> Unit) {
        val isWithinLimits = toggle(true)
        if (!isWithinLimits) onExpandLimitReached()
    }

    fun collapse() {
        toggle(false)
    }

    private fun toggle(isExpanded: Boolean): Boolean {
        var i = 0
        val queue: LinkedList<Item.Expandable<T>> = LinkedList(entries.filterIsInstance<Item.Expandable<T>>())

        while (queue.isNotEmpty() && i < MAX_ITEM_EXPANDED) {
            val item = queue.pop()
            item.toggle(isExpanded)
            if (isExpanded) {
                i += item.entries.count()
                queue.addAll(item.entries.filterIsInstance<Item.Expandable<T>>())
            } else {
                queue.addAll(item.entries.filterIsInstance<Item.Expandable<T>>().filter { it.isExpanded })
            }
        }
        return queue.isEmpty()
    }

    fun select(item: T) {
        selected = item
        item.focusFn?.let { it() }
    }

    fun isSelected(item: T): Boolean {
        return selected == item
    }

    fun selectNext(item: T) {
        println("Select next from: $selected") // TODO
    }

    fun selectPrevious(item: T) {
        println("Select previous from: $selected") // TODO
    }

    fun selectParent(item: T) {
        item.parent?.let { select(it) }
    }
}
