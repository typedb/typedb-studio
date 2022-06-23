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
import com.vaticle.typedb.studio.state.app.ConfirmationManager
import com.vaticle.typedb.studio.state.app.DialogManager
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.DIRECTORY_HAS_BEEN_MOVED_OUT
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_CREATE_FILE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FILE_HAS_BEEN_MOVED_OUT
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PATH_NOT_DIRECTORY
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PATH_NOT_EXIST
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PATH_NOT_READABLE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PATH_NOT_WRITABLE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PROJECT_DATA_DIR_PATH_TAKEN
import com.vaticle.typedb.studio.state.common.util.PreferenceManager
import com.vaticle.typedb.studio.state.common.util.Property
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.connection.ClientState
import com.vaticle.typedb.studio.state.page.PageManager
import java.nio.file.Path
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
    internal val notification: NotificationManager,
    internal val confirmation: ConfirmationManager,
    internal val client: ClientState,
    internal val resource: PageManager
) {

    class CreateItemDialog : DialogManager() {

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

        internal fun open(item: DirectoryState) {
            isOpen = true
            this.directory = item
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
    var onProjectChange: ((Project) -> Unit)? = null
    var onContentChange: (() -> Unit)? = null
    val openProjectDialog = DialogManager.Base()
    val createItemDialog = CreateItemDialog()
    val moveDirectoryDialog = ModifyDirectoryDialog()
    val renameDirectoryDialog = ModifyDirectoryDialog()
    val renameFileDialog = ModifyFileDialog()
    val saveFileDialog = ModifyFileDialog()

    fun tryOpenProject(dir: Path): Boolean {
        val dataDirPath = dir.resolve(DATA_DIR_NAME)
        val unsavedFilesDirPath = dataDirPath.resolve(UNSAVED_DATA_DIR_NAME)
        if (!dir.exists()) notification.userError(LOGGER, PATH_NOT_EXIST, dir)
        else if (!dir.isReadable()) notification.userError(LOGGER, PATH_NOT_READABLE, dir)
        else if (!dir.isWritable()) notification.userError(LOGGER, PATH_NOT_WRITABLE, dir)
        else if (!dir.isDirectory()) notification.userError(LOGGER, PATH_NOT_DIRECTORY, dir)
        else if (dataDirPath.exists() && dataDirPath.isRegularFile()) {
            notification.userError(LOGGER, PROJECT_DATA_DIR_PATH_TAKEN, dataDirPath)
        } else if (unsavedFilesDirPath.exists() && unsavedFilesDirPath.isRegularFile()) {
            notification.userError(LOGGER, PROJECT_DATA_DIR_PATH_TAKEN, unsavedFilesDirPath)
        } else {
            initialiseDirectories(dir, dataDirPath, unsavedFilesDirPath)
            onProjectChange?.let { it(current!!) }
            openProjectDialog.close()
            return true
        }
        return false
    }

    private fun initialiseDirectories(dir: Path, dataDirPath: Path, unsavedFilesDirPath: Path) {
        current = Project(dir, this, preference, notification)
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
            val newFile = unsavedFilesDir!!.createFile(newFileName)
            onContentChange?.let { it() }
            newFile
        } catch (e: Exception) {
            notification.userError(LOGGER, FAILED_TO_CREATE_FILE, unsavedFilesDir!!.path.resolve(newFileName))
            null
        }
    }

    fun tryCreateFile(parent: DirectoryState, newFileName: String) {
        tryCreateItem { parent.createFile(newFileName) }
    }

    fun tryCreateDirectory(parent: DirectoryState, newDirectoryName: String) {
        tryCreateItem { parent.createDirectory(newDirectoryName) }
    }

    private fun tryCreateItem(createFn: () -> PathState?) {
        createFn()?.let {
            createItemDialog.onSuccess?.let { fn -> fn() }
            createItemDialog.close()
            onContentChange?.let { fn -> fn() }
        }
    }

    fun tryRenameDirectory(directory: DirectoryState, newName: String) {
        directory.tryRename(newName)?.let {
            renameDirectoryDialog.close()
            onContentChange?.let { it() }
        }
    }

    fun tryMoveDirectory(directory: DirectoryState, newParent: Path) {
        directory.tryMove(newParent)?.let {
            moveDirectoryDialog.close()
            onContentChange?.let { it() }
        } ?: if (!newParent.startsWith(current!!.path)) {
            notification.userWarning(LOGGER, DIRECTORY_HAS_BEEN_MOVED_OUT, newParent)
        }
    }

    fun tryRenameFile(file: FileState, newName: String) {
        mayConfirmFileTypeChange(file, file.path.resolveSibling(newName), renameFileDialog) { onSuccess ->
            file.tryRename(newName)?.let { newFile ->
                onSuccess?.let { it(newFile) }
                onContentChange?.let { it() }
            }
        }
    }

    fun trySaveFileTo(file: FileState, newPath: Path, overwrite: Boolean) {
        mayConfirmFileTypeChange(file, newPath, saveFileDialog) { onSuccess ->
            file.trySaveTo(newPath, overwrite)?.let { newFile ->
                onSuccess?.let { it(newFile) }
                onContentChange?.let { it() }
            } ?: if (!newPath.startsWith(current!!.path)) {
                notification.userWarning(LOGGER, FILE_HAS_BEEN_MOVED_OUT, newPath)
            }
        }
    }

    private fun mayConfirmFileTypeChange(
        file: FileState, newPath: Path, dialog: ModifyFileDialog,
        confirmedModifyFileFn: (onSuccess: ((FileState) -> Unit)?) -> Unit
    ) {
        if (file.isRunnable && !Property.FileType.of(newPath).isRunnable) {
            // we need to record dialog.onSuccess before dialog.close() which clears it
            val onSuccess = dialog.onSuccess
            dialog.close()
            confirmation.submit(
                title = Label.CONVERT_FILE_TYPE,
                message = Sentence.CONFIRM_FILE_TYPE_CHANGE_NON_RUNNABLE.format(
                    file.name, newPath.fileName,
                    Property.FileType.RUNNABLE_EXTENSIONS_STR
                ),
                onConfirm = { confirmedModifyFileFn(onSuccess) }
            )
        } else confirmedModifyFileFn { newFile ->
            dialog.onSuccess?.let { it(newFile) }
            dialog.close()
        }
    }
}