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

package com.vaticle.typedb.studio.module.project

import androidx.compose.foundation.layout.Row
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
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.SelectFileDialog.SelectorOptions
import com.vaticle.typedb.studio.framework.material.SelectFileDialog.selectFilePath
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.Field
import com.vaticle.typedb.studio.framework.material.Form.RowSpacer
import com.vaticle.typedb.studio.framework.material.Form.Submission
import com.vaticle.typedb.studio.framework.material.Form.Text
import com.vaticle.typedb.studio.framework.material.Form.TextInput
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Tooltip
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.StudioState.notification
import com.vaticle.typedb.studio.state.app.DialogManager
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.project.DirectoryState
import com.vaticle.typedb.studio.state.project.PathState.Type.DIRECTORY
import com.vaticle.typedb.studio.state.project.PathState.Type.FILE
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
    ) : Form.State {

        var field: String by mutableStateOf(initField)

        override fun cancel() = onCancel()
        override fun isValid(): Boolean = field.isNotBlank() && isValid?.invoke(field) ?: true

        override fun trySubmit() {
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
        if (StudioState.project.createPathDialog.isOpen) CreatePath()
        if (StudioState.project.openProjectDialog.isOpen) OpenProject()
        if (StudioState.project.moveDirectoryDialog.isOpen) MoveDirectory()
        if (StudioState.project.renameDirectoryDialog.isOpen) RenameDirectory()
        if (StudioState.project.renameFileDialog.isOpen) RenameFile()
        if (StudioState.project.saveFileDialog.isOpen) SaveFile(window)
    }

    @Composable
    private fun OpenProject() {
        val formState = PathForm(
            initField = StudioState.appData.project.path?.toString() ?: "",
            isValid = { it != StudioState.project.current?.path.toString() },
            onCancel = { StudioState.project.openProjectDialog.close() },
            onSubmit = { StudioState.project.tryOpenProject(Path(it)) }
        )
        SelectDirectoryDialog(
            dialogState = StudioState.project.openProjectDialog,
            formState = formState,
            title = Label.OPEN_PROJECT_DIRECTORY,
            message = Sentence.SELECT_DIRECTORY_FOR_PROJECT,
            submitLabel = Label.OPEN
        )
    }

    @Composable
    private fun MoveDirectory() {
        val directory = StudioState.project.moveDirectoryDialog.directory!!
        val state = PathForm(
            initField = directory.path.parent.toString(),
            onCancel = { StudioState.project.moveDirectoryDialog.close() },
            onSubmit = { directory.tryMoveTo(Path(it)) }
        )
        SelectDirectoryDialog(
            dialogState = StudioState.project.moveDirectoryDialog,
            formState = state,
            title = Label.MOVE_DIRECTORY,
            message = Sentence.SELECT_PARENT_DIRECTORY_TO_MOVE_UNDER.format(directory.path),
            submitLabel = Label.MOVE
        )
    }

    @Composable
    private fun SelectDirectoryDialog(
        dialogState: DialogManager, formState: PathForm, title: String, message: String, submitLabel: String
    ) {
        Dialog.Layout(dialogState, title, DIALOG_WIDTH, DIALOG_HEIGHT) {
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
            Row {
                TextInput(
                    value = state.field,
                    placeholder = Label.PATH_OF_DIRECTORY,
                    onValueChange = { state.field = it },
                    modifier = Modifier.weight(1f).focusRequester(focusReq),
                )
                RowSpacer()
                Form.IconButton(
                    icon = Icon.FOLDER_OPEN,
                    tooltip = Tooltip.Arg(Label.OPEN_PROJECT_DIRECTORY)
                ) {
                    val selectedDirectoryPath = selectFilePath(window, Label.OPEN_PROJECT_DIRECTORY, SelectorOptions.DIRECTORIES_ONLY)
                    if (selectedDirectoryPath != null) {
                        state.field = selectedDirectoryPath
                    }
                }
            }
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun CreatePath() {
        when (StudioState.project.createPathDialog.type!!) {
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
        val dialogState = StudioState.project.createPathDialog
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
        val dialogState = StudioState.project.renameDirectoryDialog
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
        val dialogState = StudioState.project.renameFileDialog
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
        dialogState: DialogManager, formState: PathForm,
        title: String, message: String, submitLabel: String
    ) {
        Dialog.Layout(dialogState, title, DIALOG_WIDTH, DIALOG_HEIGHT) {
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
        val projectFile = StudioState.project.saveFileDialog.file!!
        val fileDialog = FileDialog(window, Label.SAVE_FILE, FileDialog.SAVE).apply {
            directory = StudioState.project.current?.path.toString()
            file = projectFile.name
            isMultipleMode = false
            isVisible = true
        }
        fileDialog.directory?.let {
            val newPath = Path(it).resolve(fileDialog.file)
            projectFile.trySave(newPath, true)
        } ?: StudioState.project.saveFileDialog.close()
    }
}
