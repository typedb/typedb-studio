/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.typedb.studio.service.common.ConfirmationService
import com.typedb.studio.service.common.DataService
import com.typedb.studio.service.common.NotificationService
import com.typedb.studio.service.common.PreferenceService
import com.typedb.studio.service.common.util.DialogState
import com.typedb.studio.service.common.util.Message.Project.Companion.FAILED_TO_CREATE_FILE
import com.typedb.studio.service.common.util.Message.Project.Companion.PATH_NOT_DIRECTORY
import com.typedb.studio.service.common.util.Message.Project.Companion.PATH_NOT_EXIST
import com.typedb.studio.service.common.util.Message.Project.Companion.PATH_NOT_READABLE
import com.typedb.studio.service.common.util.Message.Project.Companion.PATH_NOT_WRITABLE
import com.typedb.studio.service.common.util.Message.Project.Companion.PROJECT_DATA_DIR_PATH_TAKEN
import com.typedb.studio.service.connection.DriverState
import com.typedb.studio.service.page.PageService
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

class ProjectService constructor(
    internal val preference: PreferenceService,
    internal val appData: DataService,
    internal val notification: NotificationService,
    internal val confirmation: ConfirmationService,
    internal val driver: DriverState,
    internal val pages: PageService
) {

    class CreatePathDialogState : DialogState() {

        var parent: DirectoryState? by mutableStateOf(null); private set
        var type: PathState.Type? by mutableStateOf(null); private set
        var onSuccess: (() -> Unit)? by mutableStateOf(null); private set

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

    class ModifyDirectoryDialogState : DialogState() {

        var directory: DirectoryState? by mutableStateOf(null); private set

        internal fun open(directory: DirectoryState) {
            isOpen = true
            this.directory = directory
        }

        override fun close() {
            isOpen = false
            directory = null
        }
    }

    class ModifyFileDialogState : DialogState() {

        var file: FileState? by mutableStateOf(null); private set
        var onSuccess: ((FileState) -> Unit)? by mutableStateOf(null); private set

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

    var current: Project? by mutableStateOf(null); private set
    var dataDir: DirectoryState? by mutableStateOf(null); private set
    var unsavedFilesDir: DirectoryState? by mutableStateOf(null); private set
    val openProjectDialog = DialogState.Base()
    val createPathDialog = CreatePathDialogState()
    val moveDirectoryDialog = ModifyDirectoryDialogState()
    val renameDirectoryDialog = ModifyDirectoryDialogState()
    val renameFileDialog = ModifyFileDialogState()
    val saveFileDialog = ModifyFileDialogState()
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
