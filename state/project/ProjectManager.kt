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

package com.vaticle.typedb.studio.state.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.state.notification.Error
import com.vaticle.typedb.studio.state.notification.Message.Project.Companion.PATH_NOT_DIRECTORY
import com.vaticle.typedb.studio.state.notification.Message.Project.Companion.PATH_NOT_EXIST
import com.vaticle.typedb.studio.state.notification.Message.Project.Companion.PATH_NOT_READABLE
import com.vaticle.typedb.studio.state.notification.NotificationManager
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import mu.KotlinLogging

class ProjectManager(private val notificationMgr: NotificationManager) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    var current: Project? by mutableStateOf(null)
    var showDialog: Boolean by mutableStateOf(false)

    fun toggleDialog() {
        showDialog = !showDialog
    }

    fun tryOpenDirectory(newDir: String) {
        val path = Path.of(newDir)
        if (!path.exists()) notificationMgr.userError(Error.fromUser(PATH_NOT_EXIST, newDir), LOGGER)
        else if (!path.isReadable()) notificationMgr.userError(Error.fromUser(PATH_NOT_READABLE, newDir), LOGGER)
        else if (!path.isDirectory()) notificationMgr.userError(Error.fromUser(PATH_NOT_DIRECTORY, newDir), LOGGER)
        else {
            current?.close()
            current = Project(path, notificationMgr)
            showDialog = false
        }
    }
}