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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.project.ProjectItem.Type.DIRECTORY
import com.vaticle.typedb.studio.state.project.ProjectItem.Type.FILE
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.ComponentSpacer
import com.vaticle.typedb.studio.view.common.component.Form.Field
import com.vaticle.typedb.studio.view.common.component.Form.Submission
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Form.TextInput
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Tooltip
import java.awt.FileDialog
import javax.swing.JFileChooser
import kotlin.io.path.Path
import mu.KotlinLogging


object ProjectDialog {

    private class ProjectItemForm constructor(
        initField: String,
        val onCancel: () -> Unit,
        val onSubmit: (String) -> Unit
    ) : Form.State {

        var field: String by mutableStateOf(initField)

        override fun isValid(): Boolean {
            return field.isNotBlank()
        }

        override fun trySubmit() {
            assert(field.isNotBlank())
            onSubmit(field)
        }
    }

    private val DIRECTORY_DIALOG_WIDTH = 500.dp
    private val DIRECTORY_DIALOG_HEIGHT = 200.dp
    private val PROJECT_ITEM_NAMING_WINDOW_WIDTH = 500.dp
    private val PROJECT_ITEM_NAMING_WINDOW_HEIGHT = 200.dp
    private val LOGGER = KotlinLogging.logger {}

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ProjectDialogButtons(form: ProjectItemForm, submitLabel: String) {
        TextButton(text = Label.CANCEL, onClick = { form.onCancel() })
        ComponentSpacer()
        TextButton(text = submitLabel, onClick = { form.trySubmit() }, enabled = form.isValid())
    }
    
    @Composable
    fun OpenProject() {
        val state = ProjectItemForm(
            initField = GlobalState.project.current?.directory?.path?.toString() ?: "",
            onCancel = { GlobalState.project.openProjectDialog.close() },
            onSubmit = {
                val previous = GlobalState.project.current
                if (GlobalState.project.tryOpenProject(Path(it))) {
                    if (previous != GlobalState.project.current) {
                        GlobalState.page.closeAll()
                        GlobalState.project.unsavedFiles().forEach { GlobalState.page.open(it) }
                    }
                }
            }
        )
        SelectDirectoryDialog(
            state = state,
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
            state = state,
            title = Label.MOVE_DIRECTORY,
            message = Sentence.SELECT_PARENT_DIRECTORY_TO_MOVE_UNDER.format(directory.path),
            submitLabel = Label.MOVE
        )
    }

    @Composable
    private fun SelectDirectoryDialog(state: ProjectItemForm, title: String, message: String, submitLabel: String) {
        Dialog(
            title = title,
            onCloseRequest = { state.onCancel },
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(DIRECTORY_DIALOG_WIDTH, DIRECTORY_DIALOG_HEIGHT)
            )
        ) {
            Submission(state = state) {
                Form.Text(value = message, softWrap = true)
                SelectDirectoryField(state, window, title)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    this@ProjectDialog.ProjectDialogButtons(state, submitLabel)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun SelectDirectoryField(state: ProjectItemForm, window: ComposeDialog, title: String) {
        Field(label = Label.DIRECTORY) {
            Row {
                TextInput(
                    value = state.field,
                    placeholder = Label.PATH_OF_DIRECTORY,
                    onValueChange = { state.field = it },
                    modifier = Modifier.fillMaxHeight().weight(1f),
                )
                ComponentSpacer()
                Form.IconButton(
                    icon = Icon.Code.FOLDER_OPEN,
                    onClick = { launchDirectorySelector(state, window, title) },
                    tooltip = Tooltip.Args(Label.OPEN_PROJECT_DIRECTORY)
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
        val parent = GlobalState.project.createItemDialog.parent!!
        val form = remember {
            ProjectItemForm(
                initField = parent.nexUntitledDirName(),
                onCancel = { GlobalState.project.createItemDialog.close() },
                onSubmit = { GlobalState.project.tryCreateDirectory(parent, it) }
            )
        }
        ProjectItemNamingDialog(
            form = form,
            title = Label.CREATE_DIRECTORY,
            message = Sentence.CREATE_DIRECTORY.format(parent),
            submitLabel = Label.CREATE,
        )
    }

    @Composable
    private fun CreateFile() {
        val parent = GlobalState.project.createItemDialog.parent!!
        val form = remember {
            ProjectItemForm(
                initField = parent.nextUntitledFileName(),
                onCancel = { GlobalState.project.createItemDialog.close() },
                onSubmit = { GlobalState.project.tryCreateFile(parent, it) }
            )
        }
        ProjectItemNamingDialog(
            form = form,
            title = Label.CREATE_FILE,
            message = Sentence.CREATE_FILE.format(parent),
            submitLabel = Label.CREATE,
        )
    }

    @Composable
    fun RenameDirectory() {
        val directory = GlobalState.project.renameDirectoryDialog.directory!!
        val form = remember {
            ProjectItemForm(
                initField = directory.name,
                onCancel = { GlobalState.project.renameDirectoryDialog.close() },
                onSubmit = { GlobalState.project.tryRenameDirectory(directory, it) }
            )
        }
        ProjectItemNamingDialog(
            form = form,
            title = Label.RENAME_DIRECTORY,
            message = Sentence.RENAME_DIRECTORY.format(directory),
            submitLabel = Label.RENAME,
        )
    }

    @Composable
    fun RenameFile() {
        val file = GlobalState.project.renameFileDialog.file!!
        val form = remember {
            ProjectItemForm(
                initField = file.name,
                onCancel = { GlobalState.project.renameFileDialog.close() },
                onSubmit = { GlobalState.project.tryRenameFile(file, it) }
            )
        }
        ProjectItemNamingDialog(
            form = form,
            title = Label.RENAME_FILE,
            message = Sentence.RENAME_FILE.format(file),
            submitLabel = Label.RENAME,
        )
    }

    @Composable
    private fun ProjectItemNamingDialog(
        form: ProjectItemForm, title: String, message: String, submitLabel: String
    ) {
        Dialog(
            title = title,
            onCloseRequest = form.onCancel,
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(PROJECT_ITEM_NAMING_WINDOW_WIDTH, PROJECT_ITEM_NAMING_WINDOW_HEIGHT)
            )
        ) {
            Submission(state = form) {
                Form.Text(value = message, softWrap = true)
                ProjectItemNamingField(form.field) { form.field = it }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    this@ProjectDialog.ProjectDialogButtons(form, submitLabel)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ProjectItemNamingField(text: String, onChange: (String) -> Unit) {
        val focusReq = FocusRequester()
        Field(label = Label.FILE_NAME) {
            TextInput(
                value = text,
                placeholder = "",
                onValueChange = onChange,
                modifier = Modifier.fillMaxHeight().focusRequester(focusReq),
            )
        }
        LaunchedEffect(Unit) { focusReq.requestFocus() }
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
