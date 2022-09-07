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
    val openPreferenceDialog = DialogManager.Base()

    val autoSave: Boolean
        get() = preferences.autoSave

    val graphOutputEnabled: Boolean
        get() = preferences.graphOutput

    val queryLimit: Long
        get() = preferences.limit.toLong()

    fun isIgnoredPath(path: Path): Boolean {
        return preferences.ignoredPaths.contains(path.name)
    }
}