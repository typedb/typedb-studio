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

package com.vaticle.typedb.studio.state.app

import java.nio.file.Path
import kotlin.io.path.name

class PreferenceManager(appData: DataManager) {
    private val preferences = appData.preferences
    val preferencesDialog = DialogManager.Base()

    var autoSave: Boolean = Defaults.autoSave
        get() = preferences.autoSave ?: field
        set(value) = run { preferences.autoSave = value }

    var graphOutputEnabled: Boolean = Defaults.graphOutputEnabled
        get() = preferences.graphOutputEnabled ?: field
        set(value) = run { preferences.graphOutputEnabled = value }

    var matchQueryLimit: Long = Defaults.matchQueryLimit
        get() = preferences.matchQueryLimit?.toLong() ?: field
        set(value) = run { preferences.matchQueryLimit = value.toString() }

    var ignoredPaths: List<String> = Defaults.ignoredPaths
        get() = preferences.ignoredPaths ?: field
        set(value) = run { preferences.ignoredPaths = value}

    fun isIgnoredPath(path: Path): Boolean {
        val ignoredPaths = preferences.ignoredPaths ?: Defaults.ignoredPaths
        return ignoredPaths.contains(path.name)
    }

    private object Defaults {
        val autoSave = true
        val graphOutputEnabled = true
        val matchQueryLimit = 1000L
        val ignoredPaths = listOf(".git")
    }
}