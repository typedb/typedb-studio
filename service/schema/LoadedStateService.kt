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

package com.vaticle.typedb.studio.service.schema

import java.util.concurrent.ConcurrentHashMap

class LoadedStateService {
    val loadedState = ConcurrentHashMap<LoadedTypeState, MutableList<String>>()

    init {
        reset()
    }

    fun reset() {
        loadedState.clear()
        for (name in LoadedTypeState.values()) {
            loadedState[name] = mutableListOf()
        }
    }

    fun get(key: LoadedTypeState): List<String> {
        return loadedState[key]!!
    }

    fun set(key: LoadedTypeState, value: MutableList<String>) {
        loadedState[key] = value
    }

    fun contains(type: LoadedTypeState, value: String): Boolean {
        return loadedState[type]!!.contains(value)
    }

    fun append(key: LoadedTypeState, value: String) {
        val loadedList = loadedState[key]!!
        loadedList.add(value)
        loadedState[key] = loadedList
    }

    enum class LoadedTypeState {
        OwnerTypes,
        OwnedAttributeTypes,
        PlayedRoleTypes,
        PlayerTypes,
        RelatedRoleTypes,
    }
}