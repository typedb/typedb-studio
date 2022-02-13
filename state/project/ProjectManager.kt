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
import com.vaticle.typedb.studio.state.common.DialogManager
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FAILED_TO_CREATE_DIRECTORY
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FAILED_TO_CREATE_FILE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PATH_NOT_DIRECTORY
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PATH_NOT_EXIST
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PATH_NOT_READABLE
import com.vaticle.typedb.studio.state.notification.NotificationManager
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import mu.KotlinLogging

class ProjectManager(private val notificationMgr: NotificationManager) {

    class CreateItemDialog : DialogManager() {

        var parent: Directory? by mutableStateOf(null)
        var type: ProjectItem.Type? by mutableStateOf(null)

        fun open(parent: Directory, type: ProjectItem.Type) {
            isOpen = true
            this.parent = parent
            this.type = type
        }

        override fun close() {
            isOpen = false
            parent = null
            type = null
        }
    }

    class RenameItemDialog : DialogManager() {

        var item: ProjectItem? by mutableStateOf(null)

        fun open(item: ProjectItem) {
            isOpen = true
            this.item = item
        }

        override fun close() {
            isOpen = false
            item = null
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    var _current: Project? by mutableStateOf(null)
    var current: Project?
        get() = _current
        set(value) {
            _current = value
            onProjectChange?.let { it(_current!!) }
        }
    var onProjectChange: ((Project) -> Unit)? = null
    var onContentChange: (() -> Unit)? = null
    val openProjectDialog = DialogManager.Base()
    val createItemDialog = CreateItemDialog()
    val renameItemDialog = RenameItemDialog()

    fun tryOpenDirectory(newDir: String) {
        val path = Path.of(newDir)
        if (!path.exists()) notificationMgr.userError(LOGGER, PATH_NOT_EXIST, newDir)
        else if (!path.isReadable()) notificationMgr.userError(LOGGER, PATH_NOT_READABLE, newDir)
        else if (!path.isDirectory()) notificationMgr.userError(LOGGER, PATH_NOT_DIRECTORY, newDir)
        else {
            current = Project(path, notificationMgr)
            openProjectDialog.close()
        }
    }

    fun tryCreateFile(): File? {
        val root = current!!.directory
        val newFileName = root.nextUntitledFileName()
        return try {
            val newFile = root.createFile(newFileName)
            onContentChange?.let { it() }
            newFile
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_CREATE_FILE, root.path.resolve(newFileName))
            null
        }
    }

    fun tryCreateFile(parent: Directory, newFileName: String) {
        try {
            parent.createFile(newFileName)
            createItemDialog.close()
            onContentChange?.let { it() }
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_CREATE_FILE, parent.path.resolve(newFileName))
        }
    }

    fun tryCreateDirectory(parent: Directory, newDirectoryName: String) {
        try {
            parent.createDirectory(newDirectoryName)
            createItemDialog.close()
            onContentChange?.let { it() }
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_CREATE_DIRECTORY, parent.path.resolve(newDirectoryName))
        }
    }
}