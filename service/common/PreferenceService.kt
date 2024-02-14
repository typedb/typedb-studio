/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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

    var getQueryLimit: Long = Defaults.getQueryLimit
        get() = preferences.getQueryLimit ?: field
        set(value) = run { preferences.getQueryLimit = value }

    var transactionTimeoutMins: Long = Defaults.transactionTimeoutMins
        get() = preferences.transactionTimeoutMins ?: field
        set(value) = run { preferences.transactionTimeoutMins = value }

    var ignoredPaths: List<String> = Defaults.ignoredPaths
        get() = preferences.ignoredPaths ?: field
        set(value) = run { preferences.ignoredPaths = value }

    var diagnosticsReportingEnabled: Boolean = Defaults.diagnosticsReportingEnabled
        get() = preferences.diagnosticsReportingEnabled ?: field
        set(value) = run { preferences.diagnosticsReportingEnabled = value }

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
        val getQueryLimit = 1000L
        val ignoredPaths = listOf(".git")
        val transactionTimeoutMins = 60L
        val diagnosticsReportingEnabled = true
    }
}
