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

package com.vaticle.typedb.studio.state.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.state.app.ConfirmationManager
import com.vaticle.typedb.studio.state.app.DataManager
import com.vaticle.typedb.studio.state.app.DialogManager
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_CREATE_FILE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PATH_NOT_DIRECTORY
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PATH_NOT_EXIST
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PATH_NOT_READABLE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PATH_NOT_WRITABLE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PROJECT_DATA_DIR_PATH_TAKEN
import com.vaticle.typedb.studio.state.common.util.PreferenceManager
import com.vaticle.typedb.studio.state.connection.ClientState
import com.vaticle.typedb.studio.state.page.PageManager
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.io.path.notExists
import mu.KotlinLogging

class ProjectManager(
    internal val preference: PreferenceManager,
    internal val appData: DataManager,
    internal val notification: NotificationManager,
    internal val confirmation: ConfirmationManager,
    internal val client: ClientState,
    internal val pages: PageManager
) {

    class CreatePathDialog : DialogManager() {

        var parent: DirectoryState? by mutableStateOf(null)
        var type: PathState.Type? by mutableStateOf(null)
        var onSuccess: (() -> Unit)? by mutableStateOf(null)

        internal fun open(parent: DirectoryState, type: PathState.Type, onSuccess: () -> Unit) {
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

        var directory: DirectoryState? by mutableStateOf(null)

        internal fun open(directory: DirectoryState) {
            isOpen = true
            this.directory = directory
        }

        override fun close() {
            isOpen = false
            directory = null
        }
    }

    class ModifyFileDialog : DialogManager() {

        var file: FileState? by mutableStateOf(null)
        var onSuccess: ((FileState) -> Unit)? by mutableStateOf(null)

        internal fun open(file: FileState, onSuccess: ((FileState) -> Unit)? = null) {
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
        const val DATA_DIR_NAME = ".typedb-studio"
        const val UNSAVED_DATA_DIR_NAME = ".unsaved"
    }

    var current: Project? by mutableStateOf(null)
    var dataDir: DirectoryState? by mutableStateOf(null)
    var unsavedFilesDir: DirectoryState? by mutableStateOf(null)
    val openProjectDialog = DialogManager.Base()
    val createPathDialog = CreatePathDialog()
    val moveDirectoryDialog = ModifyDirectoryDialog()
    val renameDirectoryDialog = ModifyDirectoryDialog()
    val renameFileDialog = ModifyFileDialog()
    val saveFileDialog = ModifyFileDialog()
    private val onProjectChange = LinkedBlockingQueue<(Project) -> Unit>()
    private val onContentChange = LinkedBlockingQueue<() -> Unit>()
    private val onClose = LinkedBlockingQueue<() -> Unit>()

    fun onProjectChange(function: (Project) -> Unit) = onProjectChange.put(function)
    fun onContentChange(function: () -> Unit) = onContentChange.put(function)
    fun onClose(function: () -> Unit) = onClose.put(function)

    fun tryOpenProject(newPath: Path) {
        if (current?.path == newPath) return
        val dataDirPath = newPath.resolve(DATA_DIR_NAME)
        val unsavedFilesDirPath = dataDirPath.resolve(UNSAVED_DATA_DIR_NAME)
        if (!newPath.exists()) notification.userError(LOGGER, PATH_NOT_EXIST, newPath)
        else if (!newPath.isReadable()) notification.userError(LOGGER, PATH_NOT_READABLE, newPath)
        else if (!newPath.isWritable()) notification.userError(LOGGER, PATH_NOT_WRITABLE, newPath)
        else if (!newPath.isDirectory()) notification.userError(LOGGER, PATH_NOT_DIRECTORY, newPath)
        else if (dataDirPath.exists() && dataDirPath.isRegularFile()) {
            notification.userError(LOGGER, PROJECT_DATA_DIR_PATH_TAKEN, dataDirPath)
        } else if (unsavedFilesDirPath.exists() && unsavedFilesDirPath.isRegularFile()) {
            notification.userError(LOGGER, PROJECT_DATA_DIR_PATH_TAKEN, unsavedFilesDirPath)
        } else {
            current?.close()
            initialiseProject(newPath, dataDirPath, unsavedFilesDirPath)
            execProjectChange()
            openProjectDialog.close()
            unsavedFiles().forEach { it.tryOpen() }
            appData.project.path = current!!.path
        }
    }

    private fun initialiseProject(dir: Path, dataDirPath: Path, unsavedFilesDirPath: Path) {
        current = Project(dir, this).also { it.open() }
        if (dataDirPath.notExists()) dataDirPath.createDirectory()
        if (unsavedFilesDirPath.notExists()) unsavedFilesDirPath.createDirectory()
        current!!.directory.reloadEntries()
        dataDir = current!!.directory.entries.first { it.name == DATA_DIR_NAME }.asDirectory()
        dataDir!!.reloadEntries()
        unsavedFilesDir = dataDir!!.entries.first { it.name == UNSAVED_DATA_DIR_NAME }.asDirectory()
    }

    fun unsavedFiles(): List<FileState> {
        unsavedFilesDir?.reloadEntries()
        return unsavedFilesDir?.entries?.filter { it.isFile }?.map { it.asFile() } ?: listOf()
    }

    fun tryCreateUntitledFile(): FileState? {
        if (current == null) return null
        val newFileName = unsavedFilesDir!!.nextUntitledFileName()
        return try {
            val newFile = unsavedFilesDir!!.tryCreateFile(newFileName)
            execContentChange()
            newFile
        } catch (e: Exception) {
            notification.userError(LOGGER, FAILED_TO_CREATE_FILE, unsavedFilesDir!!.path.resolve(newFileName))
            null
        }
    }

    fun execProjectChange() = onProjectChange.forEach { it(current!!) }

    fun execContentChange() = onContentChange.forEach { it() }

    fun close(project: Project) {
        project.close()
        onClose.forEach { it() }
        if (current == project) current = null
    }
}