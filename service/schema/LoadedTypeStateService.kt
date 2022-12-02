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

class LoadedTypeStateService {
    private val loadedTypeState = ConcurrentHashMap<String, MutableList<LoadedTypeState>>()

    init {
        reset()
    }

    fun reset() {
        loadedTypeState.clear()
    }

    fun contains(key: String, value: LoadedTypeState): Boolean {
        return loadedTypeState[key]?.contains(value) ?: false
    }

    fun append(key: String, value: LoadedTypeState) {
        val loadedKeys = loadedTypeState[key] ?: mutableListOf()
        loadedKeys.add(value)
        loadedTypeState[key] = loadedKeys
    }

    enum class LoadedTypeState {
        OwnerTypes,
        OwnedAttributeTypes,
        PlayedRoleTypes,
        PlayerTypes,
        RelatedRoleTypes,
    }
}