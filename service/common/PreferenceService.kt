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

package com.vaticle.typedb.studio.service.common

import com.vaticle.typedb.studio.service.common.util.DialogState
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.relativeTo

class PreferenceService(dataSrv: DataService) {
    private val preferences = dataSrv.preferences
    val preferencesDialog = DialogState.Base()

    var autoSave: Boolean = Defaults.autoSave
        get() = preferences.autoSave ?: field
        set(value) = run { preferences.autoSave = value }

    var graphOutputEnabled: Boolean = Defaults.graphOutputEnabled
        get() = preferences.graphOutputEnabled ?: field
        set(value) = run { preferences.graphOutputEnabled = value }

    var matchQueryLimit: Long = Defaults.matchQueryLimit
        get() = preferences.matchQueryLimit?.toLong() ?: field
        set(value) = run { preferences.matchQueryLimit = value.toString() }

    var transactionTimeoutMins: Long = Defaults.transactionTimeoutMins
        get() = preferences.transactionTimeoutMins?.toLong() ?: field
        set(value) = run { preferences.transactionTimeoutMins = value.toString() }

    var ignoredPaths: List<String> = Defaults.ignoredPaths
        get() = preferences.ignoredPaths ?: field
        set(value) = run { preferences.ignoredPaths = value }

    fun isIgnoredPath(path: Path): Boolean {
        val ignoredPaths = preferences.ignoredPaths ?: Defaults.ignoredPaths
        val relativePath = path.relativeTo(path.parent)
        for (ignored in ignoredPaths) {
            val pathMatcher = FileSystems.getDefault().getPathMatcher("glob:$ignored")
            if (pathMatcher.matches(relativePath)) return true
        }
        return false
    }

    private object Defaults {
        val autoSave = true
        val graphOutputEnabled = true
        val matchQueryLimit = 1000L
        val ignoredPaths = listOf(".git")
        val transactionTimeoutMins = 5L
    }
}