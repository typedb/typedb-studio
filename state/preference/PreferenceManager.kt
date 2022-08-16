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

package com.vaticle.typedb.studio.state.preference

import com.vaticle.typedb.studio.state.app.DialogManager
import java.nio.file.Path
import kotlin.io.path.name

class PreferenceManager {

    companion object {
        private const val AUTO_SAVE_DEFAULT = true
        private val IGNORED_PATHS_DEFAULT = listOf(".git")
    }

    var autosave: Boolean = AUTO_SAVE_DEFAULT
    var ignoredPaths: List<String> = IGNORED_PATHS_DEFAULT
    val openPreferenceDialog = DialogManager.Base()

    fun isIgnoredPath(path: Path): Boolean {
        return ignoredPaths.contains(path.name)
    }
}