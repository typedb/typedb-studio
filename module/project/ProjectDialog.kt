/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.module.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.typedb.studio.framework.material.Dialog
import com.typedb.studio.framework.material.Form
import com.typedb.studio.framework.material.Form.Field
import com.typedb.studio.framework.material.Form.Submission
import com.typedb.studio.framework.material.Form.Text
import com.typedb.studio.framework.material.Form.TextInput
import com.typedb.studio.framework.material.Icon
import com.typedb.studio.framework.material.SelectFileDialog
import com.typedb.studio.framework.material.SelectFileDialog.SelectorOptions
import com.typedb.studio.framework.material.Tooltip
import com.typedb.studio.service.Service
import com.typedb.studio.service.Service.notification
import com.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.typedb.studio.service.common.util.DialogState
import com.typedb.studio.service.common.util.Label
import com.typedb.studio.service.common.util.Sentence
import com.typedb.studio.service.project.DirectoryState
import com.typedb.studio.service.project.PathState.Type.DIRECTORY
import com.typedb.studio.service.project.PathState.Type.FILE
import java.awt.FileDialog
import kotlin.io.path.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mu.KotlinLogging

object ProjectDialog {

    private class PathForm constructor(
        initField: String,
        val isValid: ((String) -> Boolean)? = null,
        val onCancel: () -> Unit,
        val onSubmit: (String) -> Unit
    ) : Form.State() {

        var field: String by mutableStateOf(initField)

        override fun cancel() = onCancel()
        override fun isValid(): Boolean = field.isNotBlank() && isValid?.invoke(field) ?: true

        override fun submit() {
            assert(field.isNotBlank())
            onSubmit(field)
        }
    }

    private val DIALOG_WIDTH = 500.dp
    private val DIALOG_HEIGHT = 200.dp
    private val coroutines = CoroutineScope(Dispatchers.Default)
    private val LOGGER = KotlinLogging.logger {}

    @Composable
    fun MayShowDialogs(window: ComposeWindow) {
        if (Service.project.createPathDialog.isOpen) CreatePath()
        if (Service.project.openProjectDialog.isOpen) OpenProject()
        if (Service.project.moveDirectoryDialog.isOpen) MoveDirectory()
        if (Service.project.renameDirectoryDialog.isOpen) RenameDirectory()
        if (Service.project.renameFileDialog.isOpen) RenameFile()
        if (Service.project.saveFileDialog.isOpen) SaveFile(window)
    }

    @Composable
    private fun OpenProject() {
        val formState = PathForm(
            initField = Service.data.project.path?.toString() ?: "",
            isValid = { it != Service.project.current?.path.toString() },
            onCancel = { Service.project.openProjectDialog.close() },
            onSubmit = { Service.project.tryOpenProject(Path(it)) }
        )
        SelectDirectoryDialog(
            dialog = Service.project.openProjectDialog,
            formState = formState,
            title = Label.OPEN_PROJECT_DIRECTORY,
            message = Sentence.SELECT_DIRECTORY_FOR_PROJECT,
            submitLabel = Label.OPEN
        )
    }

    @Composable
    private fun MoveDirectory() {
        val directory = Service.project.moveDirectoryDialog.directory!!
        val state = PathForm(
            initField = directory.path.parent.toString(),
            onCancel = { Service.project.moveDirectoryDialog.close() },
            onSubmit = { directory.tryMoveTo(Path(it)) }
        )
        SelectDirectoryDialog(
            dialog = Service.project.moveDirectoryDialog,
            formState = state,
            title = Label.MOVE_DIRECTORY,
            message = Sentence.SELECT_PARENT_DIRECTORY_TO_MOVE_UNDER.format(directory.path),
            submitLabel = Label.MOVE
        )
    }

    @Composable
    private fun SelectDirectoryDialog(
        dialog: DialogState,
        formState: PathForm,
        title: String,
        message: String,
        submitLabel: String
    ) {
        Dialog.Layout(dialog, title, DIALOG_WIDTH, DIALOG_HEIGHT) {
            Submission(state = formState, modifier = Modifier.fillMaxSize(), submitLabel = submitLabel) {
                Text(value = message, softWrap = true)
                SelectDirectoryField(formState, window, title)
            }
        }
    }

    @Composable
    private fun SelectDirectoryField(state: PathForm, window: ComposeDialog, title: String) {
        val focusReq = remember { FocusRequester() }
        Field(label = Label.DIRECTORY) {
            TextInput(
                value = state.field,
                placeholder = Label.PATH_OF_DIRECTORY,
                onValueChange = { state.field = it },
                modifier = Modifier.weight(1f).focusRequester(focusReq),
            )
            Form.IconButton(
                icon = Icon.FOLDER_OPEN,
                tooltip = Tooltip.Arg(title)
            ) {
                val (selectedDirectoryPath) = SelectFileDialog.open(
                    window, title, SelectorOptions.DIRECTORIES_ONLY
                )
                if (selectedDirectoryPath != null) {
                    state.field = selectedDirectoryPath
                }
            }
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun CreatePath() {
        when (Service.project.createPathDialog.type!!) {
            DIRECTORY -> CreateDirectory()
            FILE -> CreateFile()
        }
    }

    @Composable
    private fun CreateDirectory() {
        CreateItem(
            title = Label.CREATE_DIRECTORY,
            messageTemplate = Sentence.CREATE_DIRECTORY,
            initNameFn = { it.nextUntitledDirName() }
        ) { parent, name -> parent.tryCreateDirectory(name) }
    }

    @Composable
    private fun CreateFile() {
        CreateItem(
            title = Label.CREATE_FILE,
            messageTemplate = Sentence.CREATE_FILE,
            initNameFn = { it.nextUntitledFileName() }
        ) { parent, name -> parent.tryCreateFile(name) }
    }

    @Composable
    private fun CreateItem(
        title: String,
        messageTemplate: String,
        initNameFn: (DirectoryState) -> String,
        onSubmit: (DirectoryState, String) -> Unit
    ) {
        val dialogState = Service.project.createPathDialog
        val parent = dialogState.parent!!
        val formState = remember {
            PathForm(
                initField = initNameFn(parent),
                onCancel = { dialogState.close() },
                onSubmit = { onSubmit(parent, it) }
            )
        }
        PathNamingDialog(dialogState, formState, title, messageTemplate.format(parent), Label.CREATE)
    }

    @Composable
    private fun RenameDirectory() {
        val dialogState = Service.project.renameDirectoryDialog
        val directory = dialogState.directory!!
        val message = Sentence.RENAME_DIRECTORY.format(directory)
        val formState = remember {
            PathForm(
                initField = directory.name,
                isValid = { it != directory.name },
                onCancel = { dialogState.close() },
                onSubmit = { directory.tryRename(it) }
            )
        }
        PathNamingDialog(dialogState, formState, Label.RENAME_DIRECTORY, message, Label.RENAME)
    }

    @Composable
    private fun RenameFile() {
        val dialogState = Service.project.renameFileDialog
        val file = dialogState.file!!
        val message = Sentence.RENAME_FILE.format(file)
        val formState = remember {
            PathForm(
                initField = file.name,
                isValid = { it != file.name },
                onCancel = { dialogState.close() },
                onSubmit = { file.tryRename(it) }
            )
        }
        PathNamingDialog(dialogState, formState, Label.RENAME_FILE, message, Label.RENAME)
    }

    @Composable
    private fun PathNamingDialog(
        dialog: DialogState, formState: PathForm,
        title: String, message: String, submitLabel: String
    ) {
        Dialog.Layout(dialog, title, DIALOG_WIDTH, DIALOG_HEIGHT) {
            Submission(state = formState, modifier = Modifier.fillMaxSize(), submitLabel = submitLabel) {
                Text(value = message, softWrap = true)
                PathNamingField(formState.field) { formState.field = it }
            }
        }
    }

    @Composable
    private fun PathNamingField(text: String, onChange: (String) -> Unit) {
        val focusReq = remember { FocusRequester() }
        Field(label = Label.FILE_NAME) {
            TextInput(
                value = text,
                placeholder = "",
                onValueChange = onChange,
                modifier = Modifier.focusRequester(focusReq),
            )
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun SaveFile(window: ComposeWindow) = coroutines.launchAndHandle(notification, LOGGER) {
        val projectFile = Service.project.saveFileDialog.file!!
        val fileDialog = FileDialog(window, Label.SAVE_FILE, FileDialog.SAVE).apply {
            directory = Service.project.current?.path.toString()
            file = projectFile.name
            isMultipleMode = false
            isVisible = true
        }
        fileDialog.directory?.let {
            val newPath = Path(it).resolve(fileDialog.file)
            projectFile.trySave(newPath, true)
        } ?: Service.project.saveFileDialog.close()
    }
}
