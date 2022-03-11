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

package com.vaticle.typedb.studio.view.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.DialogManager
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.project.Directory
import com.vaticle.typedb.studio.state.project.ProjectItem.Type.DIRECTORY
import com.vaticle.typedb.studio.state.project.ProjectItem.Type.FILE
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.Field
import com.vaticle.typedb.studio.view.common.component.Form.FormRowSpacer
import com.vaticle.typedb.studio.view.common.component.Form.Submission
import com.vaticle.typedb.studio.view.common.component.Form.TextInput
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Tooltip
import java.awt.FileDialog
import javax.swing.JFileChooser
import kotlin.io.path.Path
import mu.KotlinLogging


object ProjectDialog {

    private class ProjectItemForm constructor(
        initField: String, val onCancel: () -> Unit, val onSubmit: (String) -> Unit
    ) : Form.State {

        var field: String by mutableStateOf(initField)

        override fun cancel() {
            onCancel()
        }

        override fun isValid(): Boolean {
            return field.isNotBlank()
        }

        override fun trySubmit() {
            assert(field.isNotBlank())
            onSubmit(field)
        }
    }

    private val SELECT_DIR_WIDTH = 500.dp
    private val SELECT_DIR_HEIGHT = 200.dp
    private val NAMING_WIDTH = 500.dp
    private val NAMING_HEIGHT = 200.dp
    private val LOGGER = KotlinLogging.logger {}

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ProjectDialogButtons(form: ProjectItemForm, submitLabel: String) {

    }

    @Composable
    fun OpenProject() {
        val formState = ProjectItemForm(
            initField = GlobalState.project.current?.directory?.path?.toString() ?: "",
            onCancel = { GlobalState.project.openProjectDialog.close() },
            onSubmit = {
                val previous = GlobalState.project.current
                if (GlobalState.project.tryOpenProject(Path(it))) {
                    if (previous != GlobalState.project.current) {
                        GlobalState.resource.closeAll()
                        GlobalState.project.unsavedFiles().forEach { GlobalState.resource.open(it) }
                    }
                }
            }
        )
        SelectDirectoryDialog(
            dialogState = GlobalState.project.openProjectDialog,
            formState = formState,
            title = Label.OPEN_PROJECT_DIRECTORY,
            message = Sentence.SELECT_DIRECTORY_FOR_PROJECT,
            submitLabel = Label.OPEN
        )
    }

    @Composable
    fun MoveDirectory() {
        val directory = GlobalState.project.moveDirectoryDialog.directory!!
        val state = ProjectItemForm(
            initField = directory.path.parent.toString(),
            onCancel = { GlobalState.project.moveDirectoryDialog.close() },
            onSubmit = { GlobalState.project.tryMoveDirectory(directory, Path(it)) }
        )
        SelectDirectoryDialog(
            dialogState = GlobalState.project.moveDirectoryDialog,
            formState = state,
            title = Label.MOVE_DIRECTORY,
            message = Sentence.SELECT_PARENT_DIRECTORY_TO_MOVE_UNDER.format(directory.path),
            submitLabel = Label.MOVE
        )
    }

    @Composable
    private fun SelectDirectoryDialog(
        dialogState: DialogManager, formState: ProjectItemForm, title: String, message: String, submitLabel: String
    ) {
        val focusReq = FocusRequester()
        Dialog.Layout(dialogState, focusReq, title, SELECT_DIR_WIDTH, SELECT_DIR_HEIGHT) {
            Submission(state = formState, submitLabel = submitLabel) {
                Form.Text(value = message, softWrap = true)
                SelectDirectoryField(formState, window, title, focusReq)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun SelectDirectoryField(
        state: ProjectItemForm,
        window: ComposeDialog,
        title: String,
        focusReq: FocusRequester
    ) {
        Field(label = Label.DIRECTORY) {
            Row {
                TextInput(
                    value = state.field,
                    placeholder = Label.PATH_OF_DIRECTORY,
                    onValueChange = { state.field = it },
                    modifier = Modifier.fillMaxHeight().weight(1f).focusRequester(focusReq),
                )
                FormRowSpacer()
                Form.IconButton(
                    icon = Icon.Code.FOLDER_OPEN,
                    onClick = { launchDirectorySelector(state, window, title) },
                    tooltip = Tooltip.Arg(Label.OPEN_PROJECT_DIRECTORY)
                )
            }
        }
    }

    private fun launchDirectorySelector(state: ProjectItemForm, parent: ComposeDialog, title: String) {
        when (Property.OS.Current) {
            Property.OS.MACOS -> macOSDirectorySelector(state, parent, title)
            else -> otherOSDirectorySelector(state, title)
        }
    }

    private fun macOSDirectorySelector(state: ProjectItemForm, parent: ComposeDialog, title: String) {
        val fileDialog = FileDialog(parent, title, FileDialog.LOAD)
        fileDialog.apply {
            directory = state.field
            isMultipleMode = false
            isVisible = true
        }
        fileDialog.directory?.let { state.field = Path(it).resolve(fileDialog.file).toString() }
    }

    private fun otherOSDirectorySelector(state: ProjectItemForm, title: String) {
        val directoryChooser = JFileChooser().apply {
            currentDirectory = Path(state.field).toFile()
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        val option = directoryChooser.showOpenDialog(null)
        if (option == JFileChooser.APPROVE_OPTION) {
            val directory = directoryChooser.selectedFile
            assert(directory.isDirectory)
            state.field = directory.absolutePath
        }
    }

    @Composable
    fun CreateProjectItem() {
        when (GlobalState.project.createItemDialog.type!!) {
            DIRECTORY -> CreateDirectory()
            FILE -> CreateFile()
        }
    }

    @Composable
    private fun CreateDirectory() {
        CreateItem(Label.CREATE_DIRECTORY, Sentence.CREATE_DIRECTORY, { it.nextUntitledDirName() }) { parent, name ->
            GlobalState.project.tryCreateDirectory(parent, name)
        }
    }

    @Composable
    private fun CreateFile() {
        CreateItem(Label.CREATE_FILE, Sentence.CREATE_FILE, { it.nextUntitledFileName() }) { parent, name ->
            GlobalState.project.tryCreateFile(parent, name)
        }
    }

    @Composable
    private fun CreateItem(
        title: String, message: String, initNameFn: (Directory) -> String, onSubmit: (Directory, String) -> Unit
    ) {
        val dialogState = GlobalState.project.createItemDialog
        val parent = dialogState.parent!!
        val formState = remember {
            ProjectItemForm(
                initField = initNameFn(parent),
                onCancel = { dialogState.close() },
                onSubmit = { onSubmit(parent, it) }
            )
        }
        ProjectItemNamingDialog(dialogState, formState, title, message.format(parent), Label.CREATE)
    }

    @Composable
    fun RenameDirectory() {
        val dialogState = GlobalState.project.renameDirectoryDialog
        val directory = dialogState.directory!!
        val message = Sentence.RENAME_DIRECTORY.format(directory)
        val formState = remember {
            ProjectItemForm(
                initField = directory.name,
                onCancel = { dialogState.close() },
                onSubmit = { GlobalState.project.tryRenameDirectory(directory, it) }
            )
        }
        ProjectItemNamingDialog(dialogState, formState, Label.RENAME_DIRECTORY, message, Label.RENAME)
    }

    @Composable
    fun RenameFile() {
        val dialogState = GlobalState.project.renameFileDialog
        val file = dialogState.file!!
        val message = Sentence.RENAME_FILE.format(file)
        val formState = remember {
            ProjectItemForm(
                initField = file.name,
                onCancel = { dialogState.close() },
                onSubmit = { GlobalState.project.tryRenameFile(file, it) }
            )
        }
        ProjectItemNamingDialog(dialogState, formState, Label.RENAME_FILE, message, Label.RENAME)
    }

    @Composable
    private fun ProjectItemNamingDialog(
        dialogState: DialogManager, formState: ProjectItemForm, title: String, message: String, submitLabel: String
    ) {
        val focusReq = FocusRequester()
        Dialog.Layout(dialogState, focusReq, title, NAMING_WIDTH, NAMING_HEIGHT) {
            Submission(state = formState, submitLabel = submitLabel) {
                Form.Text(value = message, softWrap = true)
                ProjectItemNamingField(formState.field, focusReq) { formState.field = it }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ProjectItemNamingField(text: String, focusReq: FocusRequester, onChange: (String) -> Unit) {
        Field(label = Label.FILE_NAME) {
            TextInput(
                value = text,
                placeholder = "",
                onValueChange = onChange,
                modifier = Modifier.fillMaxHeight().focusRequester(focusReq),
            )
        }
    }

    @Composable
    fun SaveFile(window: ComposeWindow) {
        val projectFile = GlobalState.project.saveFileDialog.file!!
        val fileDialog = FileDialog(window, Label.SAVE_FILE, FileDialog.SAVE).apply {
            directory = GlobalState.project.current?.path.toString()
            file = projectFile.name
            isMultipleMode = false
            isVisible = true
        }
        fileDialog.directory?.let {
            val newPath = Path(it).resolve(fileDialog.file)
            GlobalState.project.trySaveFileTo(projectFile, newPath, true)
        } ?: GlobalState.project.saveFileDialog.close()
    }
}
