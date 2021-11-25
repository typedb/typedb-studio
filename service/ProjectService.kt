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

package com.vaticle.typedb.studio.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.common.notification.Error
import com.vaticle.typedb.studio.common.notification.Message.Project.Companion.PATH_NOT_EXIST
import com.vaticle.typedb.studio.common.notification.Message.Project.Companion.PATH_NOT_READABLE
import java.nio.file.Path
import kotlin.io.path.isReadable
import kotlin.io.path.notExists
import mu.KotlinLogging

class ProjectService {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    // TODO: initialise from user data
    var pastPaths: Set<Path> by mutableStateOf(emptySet())
    var currentPath: Path? by mutableStateOf(null)
    var showWindow: Boolean by mutableStateOf(false)

    fun toggleWindow() {
        showWindow = !showWindow
    }

    fun tryOpen(directory: String) {
        val path = Path.of(directory)
        if (path.notExists()) Service.notifier.userError(Error.fromUser(PATH_NOT_EXIST, directory), LOGGER)
        else if (!path.isReadable()) Service.notifier.userError(Error.fromUser(PATH_NOT_READABLE, directory), LOGGER)
        else {
            currentPath = path
            pastPaths = pastPaths + path
            showWindow = false
        }
    }
}
