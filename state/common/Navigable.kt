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

import com.vaticle.typedb.studio.state.common.Message.System.Companion.ILLEGAL_CAST


object Navigable {

    interface Item<T : Item<T>> : Comparable<Item<T>> {

        val name: String
        val parent: ExpandableItem<T>?
        val info: String?
        val isExpandable: Boolean get() = false

        fun asExpandable(): ExpandableItem<T> {
            throw TypeCastException(ILLEGAL_CAST.message(Item::class.simpleName, ExpandableItem::class.simpleName))
        }
    }

    interface ExpandableItem<T : Item<T>> : Item<T> {

        override val isExpandable: Boolean get() = true
        override fun asExpandable(): ExpandableItem<T> {
            return this
        }

        val isContainer: Boolean get() = false
        val entries: List<T>
        fun reloadEntries()
    }

    interface Container<T : Item<T>> : ExpandableItem<T> {

        override val isContainer: Boolean get() = true
    }
}
