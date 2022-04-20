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
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.DIRECTORY_HAS_BEEN_MOVED_OUT
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FAILED_TO_CREATE_FILE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FILE_HAS_BEEN_MOVED_OUT
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PATH_NOT_DIRECTORY
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PATH_NOT_EXIST
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PATH_NOT_READABLE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PATH_NOT_WRITABLE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.PROJECT_DATA_DIR_PATH_TAKEN
import com.vaticle.typedb.studio.state.common.Settings
import com.vaticle.typedb.studio.state.notification.NotificationManager
import com.vaticle.typedb.studio.state.resource.Resource
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.io.path.notExists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

class ProjectManager constructor(private val settings: Settings, private val notificationMgr: NotificationManager) {

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

    class ModifyDirectoryDialog : DialogManager() {

        var directory: Directory? by mutableStateOf(null)

        fun open(item: Directory) {
            isOpen = true
            this.directory = item
        }

        override fun close() {
            isOpen = false
            directory = null
        }
    }

    class ModifyFileDialog : DialogManager() {

        var file: File? by mutableStateOf(null)
        var onSuccess: ((Resource) -> Unit)? by mutableStateOf(null)

        internal fun open(file: File, onSuccess: ((Resource) -> Unit)? = null) {
            isOpen = true
            this.file = file
            this.onSuccess = onSuccess
        }

        override fun close() {
            isOpen = false
            file = null
            onSuccess = null
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        const val DATA_DIR_NAME = ".tdbs"
        const val UNSAVED_DATA_DIR_NAME = ".unsaved"
    }

    var current: Project? by mutableStateOf(null)
    var dataDir: Directory? by mutableStateOf(null)
    var unsavedFilesDir: Directory? by mutableStateOf(null)
    var onProjectChange: ((Project) -> Unit)? = null
    var onContentChange: (() -> Unit)? = null
    val openProjectDialog = DialogManager.Base()
    val createItemDialog = CreateItemDialog()
    val moveDirectoryDialog = ModifyDirectoryDialog()
    val renameDirectoryDialog = ModifyDirectoryDialog()
    val renameFileDialog = ModifyFileDialog()
    val saveFileDialog = ModifyFileDialog()
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    fun tryOpenProject(dir: Path): Boolean {
        val dataDirPath = dir.resolve(DATA_DIR_NAME)
        val unsavedFilesDirPath = dataDirPath.resolve(UNSAVED_DATA_DIR_NAME)
        if (!dir.exists()) notificationMgr.userError(LOGGER, PATH_NOT_EXIST, dir)
        else if (!dir.isReadable()) notificationMgr.userError(LOGGER, PATH_NOT_READABLE, dir)
        else if (!dir.isWritable()) notificationMgr.userError(LOGGER, PATH_NOT_WRITABLE, dir)
        else if (!dir.isDirectory()) notificationMgr.userError(LOGGER, PATH_NOT_DIRECTORY, dir)
        else if (dataDirPath.exists() && dataDirPath.isRegularFile()) {
            notificationMgr.userError(LOGGER, PROJECT_DATA_DIR_PATH_TAKEN, dataDirPath)
        } else if (unsavedFilesDirPath.exists() && unsavedFilesDirPath.isRegularFile()) {
            notificationMgr.userError(LOGGER, PROJECT_DATA_DIR_PATH_TAKEN, unsavedFilesDirPath)
        } else {
            initialiseDirectories(dir, dataDirPath, unsavedFilesDirPath)
            onProjectChange?.let { it(current!!) }
            openProjectDialog.close()
            return true
        }
        return false
    }

    private fun initialiseDirectories(dir: Path, dataDirPath: Path, unsavedFilesDirPath: Path) {
        current = Project(dir, settings, this, notificationMgr)
        if (dataDirPath.notExists()) dataDirPath.createDirectory()
        if (unsavedFilesDirPath.notExists()) unsavedFilesDirPath.createDirectory()
        current!!.directory.reloadEntries()
        dataDir = current!!.directory.entries.first { it.name == DATA_DIR_NAME }.asDirectory()
        dataDir!!.reloadEntries()
        unsavedFilesDir = dataDir!!.entries.first { it.name == UNSAVED_DATA_DIR_NAME }.asDirectory()
    }

    fun unsavedFiles(): List<File> {
        unsavedFilesDir?.reloadEntries()
        return unsavedFilesDir?.entries?.filter { it.isFile }?.map { it.asFile() } ?: listOf()
    }

    fun tryCreateUntitledFile(): File? {
        if (current == null) return null
        val newFileName = unsavedFilesDir!!.nextUntitledFileName()
        return try {
            val newFile = unsavedFilesDir!!.createFile(newFileName)
            onContentChange?.let { it() }
            newFile
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_CREATE_FILE, unsavedFilesDir!!.path.resolve(newFileName))
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

    fun tryRenameDirectory(directory: Directory, newName: String) {
        directory.tryRename(newName)?.let {
            renameDirectoryDialog.close()
            onContentChange?.let { fn -> fn() }
        }
    }

    fun tryRenameFile(file: File, newName: String) = coroutineScope.launch {
        file.tryRename(newName)?.let {
            renameFileDialog.onSuccess?.let { fn -> fn(it.asFile()) }
            renameFileDialog.close()
            onContentChange?.let { fn -> fn() }
        }
    }

    fun trySaveFileTo(file: File, newPath: Path, overwrite: Boolean) {
        file.trySaveTo(newPath, overwrite)?.let { newFile ->
            saveFileDialog.onSuccess?.let { it(newFile) }
            saveFileDialog.close()
            onContentChange?.let { it() }
        } ?: if (!newPath.startsWith(current!!.path)) {
            notificationMgr.userWarning(LOGGER, FILE_HAS_BEEN_MOVED_OUT, newPath)
        }
    }

    fun tryMoveDirectory(directory: Directory, newParent: Path) {
        directory.tryMove(newParent)?.let {
            moveDirectoryDialog.close()
            onContentChange?.let { it() }
        } ?: if (!newParent.startsWith(current!!.path)) {
            notificationMgr.userWarning(LOGGER, DIRECTORY_HAS_BEEN_MOVED_OUT, newParent)
        }
    }
}