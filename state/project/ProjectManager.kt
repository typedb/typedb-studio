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
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FAILED_TO_CREATE_FILE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PATH_NOT_DIRECTORY
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PATH_NOT_EXIST
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PATH_NOT_READABLE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PATH_NOT_WRITABLE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PROJECT_DATA_DIR_PATH_TAKEN
import com.vaticle.typedb.studio.state.notification.NotificationManager
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.io.path.notExists
import mu.KotlinLogging

class ProjectManager(private val notificationMgr: NotificationManager) {

    class CreateItemDialog : DialogManager() {

        var parent: Directory? by mutableStateOf(null)
        var type: ProjectItem.Type? by mutableStateOf(null)
        var onSuccess: (() -> Unit)? by mutableStateOf(null)

        fun open(parent: Directory, type: ProjectItem.Type, onSuccess: () -> Unit) {
            isOpen = true
            this.parent = parent
            this.type = type
            this.onSuccess = onSuccess
        }

        override fun close() {
            isOpen = false
            parent = null
            type = null
            onSuccess = null
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
        const val DATA_DIR_NAME = ".tdbs"
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

    fun tryOpenProject(newDir: String) {
        val dir = Path.of(newDir)
        val dataDir = dir.resolve(DATA_DIR_NAME)
        if (!dir.exists()) notificationMgr.userError(LOGGER, PATH_NOT_EXIST, newDir)
        else if (!dir.isReadable()) notificationMgr.userError(LOGGER, PATH_NOT_READABLE, newDir)
        else if (!dir.isWritable()) notificationMgr.userError(LOGGER, PATH_NOT_WRITABLE, newDir)
        else if (!dir.isDirectory()) notificationMgr.userError(LOGGER, PATH_NOT_DIRECTORY, newDir)
        else if (dataDir.exists() && dataDir.isRegularFile()) {
            notificationMgr.userError(LOGGER, PROJECT_DATA_DIR_PATH_TAKEN, dataDir)
        } else {
            current = Project(dir, notificationMgr)
            openProjectDialog.close()
            if (dataDir.notExists()) dataDir.createDirectory()
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
        tryCreateItem { parent.createFile(newFileName) }
    }

    fun tryCreateDirectory(parent: Directory, newDirectoryName: String) {
        tryCreateItem { parent.createDirectory(newDirectoryName) }
    }

    private fun tryCreateItem(createFn: () -> ProjectItem?) {
        createFn()?.let {
            createItemDialog.onSuccess?.let { fn -> fn() }
            createItemDialog.close()
            onContentChange?.let { fn -> fn() }
        }
    }

    fun tryRename(item: ProjectItem, newName: String) {
        if (item.tryRename(newName)) {
            renameItemDialog.close()
            onContentChange?.let { it() }
        }
    }
}