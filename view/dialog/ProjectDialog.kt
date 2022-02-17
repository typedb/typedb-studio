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
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.project.ProjectItem
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
import com.vaticle.typedb.studio.view.dialog.common.DirectoryDialog
import com.vaticle.typedb.studio.view.dialog.common.DirectoryDialog.SelectDirectoryForm
import java.awt.FileDialog
import kotlin.io.path.Path
import mu.KotlinLogging


object ProjectDialog {

    private val PROJECT_ITEM_NAMING_WINDOW_WIDTH = 500.dp
    private val PROJECT_ITEM_NAMING_WINDOW_HEIGHT = 200.dp
    private val LOGGER = KotlinLogging.logger {}

    @Composable
    fun OpenProject() {
        val state = SelectDirectoryForm(
            initPath = GlobalState.project.current?.directory?.path,
            onCancel = { GlobalState.project.openProjectDialog.close() },
            onSubmit = {
                val previous = GlobalState.project.current
                if (GlobalState.project.tryOpenProject(it)) {
                    if (previous != GlobalState.project.current) {
                        GlobalState.page.closeAll()
                        GlobalState.project.unsavedFiles().forEach { GlobalState.page.open(it) }
                    }
                }
            }
        )
        DirectoryDialog.Layout(
            state = state,
            title = Label.OPEN_PROJECT_DIRECTORY,
            message = Sentence.SELECT_DIRECTORY_FOR_PROJECT,
            submitLabel = Label.OPEN
        )
    }

    private class ProjectItemNamingForm constructor(
        initName: String,
        val onCancel: () -> Unit,
        val onSubmit: (String) -> Unit
    ) : Form.State {

        var newName: String by mutableStateOf(initName)

        override fun isValid(): Boolean {
            return newName.isNotBlank()
        }

        override fun trySubmit() {
            assert(newName.isNotBlank())
            onSubmit(newName)
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
            ProjectItemNamingForm(
                initName = parent.nexUntitledDirName(),
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
            ProjectItemNamingForm(
                initName = parent.nextUntitledFileName(),
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
    fun RenameProjectItem() {
        val item = GlobalState.project.renameItemDialog.item!!
        val form = remember {
            ProjectItemNamingForm(
                initName = item.name,
                onCancel = { GlobalState.project.renameItemDialog.close() },
                onSubmit = { GlobalState.project.tryRename(item, it) }
            )
        }
        when (GlobalState.project.renameItemDialog.item!!.projectItemType) {
            DIRECTORY -> RenameDirectory(form, item)
            FILE -> RenameFile(form, item)
        }
    }

    @Composable
    private fun RenameDirectory(form: ProjectItemNamingForm, item: ProjectItem) {
        ProjectItemNamingDialog(
            form = form,
            title = Label.RENAME_DIRECTORY,
            message = Sentence.RENAME_DIRECTORY.format(item),
            submitLabel = Label.RENAME,
        )
    }

    @Composable
    private fun RenameFile(form: ProjectItemNamingForm, item: ProjectItem) {
        ProjectItemNamingDialog(
            form = form,
            title = Label.RENAME_FILE,
            message = Sentence.RENAME_FILE.format(item),
            submitLabel = Label.RENAME,
        )
    }

    @Composable
    private fun ProjectItemNamingDialog(
        form: ProjectItemNamingForm, title: String, message: String, submitLabel: String
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
                ProjectItemNamingField(form.newName) { form.newName = it }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    ProjectItemNamingButtons(form, submitLabel, form.onCancel)
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

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ProjectItemNamingButtons(form: Form.State, submitLabel: String, onCancel: () -> Unit) {
        TextButton(text = Label.CANCEL, onClick = onCancel)
        ComponentSpacer()
        TextButton(text = submitLabel, enabled = form.isValid(), onClick = { form.trySubmit() })
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

    @Composable
    fun MoveDirectory() {
        val directory = GlobalState.project.moveDirectoryDialog.item!!
        val state = SelectDirectoryForm(
            initPath = directory.path.parent,
            onCancel = { GlobalState.project.moveDirectoryDialog.close() },
            onSubmit = { GlobalState.project.tryMoveDirectory(directory, it) }
        )
        DirectoryDialog.Layout(
            state = state,
            title = Label.MOVE_DIRECTORY,
            message = Sentence.SELECT_PARENT_DIRECTORY_TO_MOVE_UNDER.format(directory.path),
            submitLabel = Label.MOVE
        )
    }
}
