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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.common.Property.OS.MACOS
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.ComponentSpacer
import com.vaticle.typedb.studio.view.common.component.Form.Field
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.Submission
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Form.TextInput
import com.vaticle.typedb.studio.view.common.component.Icon
import java.awt.FileDialog
import java.io.File
import javax.swing.JFileChooser
import kotlin.io.path.Path


object ProjectDialog {

    private val WINDOW_WIDTH = 500.dp
    private val WINDOW_HEIGHT = 140.dp

    class ProjectFormState : Form.State {

        var directory: String? by mutableStateOf(GlobalState.project.current?.directory?.absolutePath?.toString())

        override fun isValid(): Boolean {
            return !directory.isNullOrBlank()
        }

        override fun trySubmitIfValid() {
            if (isValid()) trySubmit()
        }

        override fun trySubmit() {
            assert(!directory.isNullOrBlank())
            GlobalState.project.tryOpenDirectory(directory!!)
        }
    }

    @Composable
    fun OpenProject() {
        val formState = remember { ProjectFormState() }
        Dialog(
            title = Label.OPEN_PROJECT_DIRECTORY,
            onCloseRequest = { GlobalState.project.openProjectDialog.close() },
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(WINDOW_WIDTH, WINDOW_HEIGHT)
            )
        ) {
            Submission(state = formState) {
                SelectDirectoryField(formState, window)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    OpenProjectButtons(formState)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun SelectDirectoryField(formState: ProjectFormState, window: ComposeDialog) {
        Field(label = Label.DIRECTORY) {
            Row {
                TextInput(
                    value = formState.directory ?: "",
                    placeholder = Label.PATH_TO_PROJECT,
                    onValueChange = { formState.directory = it },
                    modifier = Modifier.fillMaxHeight().weight(1f),
                )
                ComponentSpacer()
                IconButton(icon = Icon.Code.FOLDER_OPEN, onClick = { launchFileDialog(formState, window) })
            }
        }
    }

    private fun launchFileDialog(formState: ProjectFormState, window: ComposeDialog) {
        when (Property.OS.Current) {
            MACOS -> macOSDialog(formState, window)
            else -> otherOSDialog(formState)
        }
    }

    private fun macOSDialog(formState: ProjectFormState, window: ComposeDialog) {
        val fileDialog = FileDialog(window, Label.OPEN_PROJECT_DIRECTORY, FileDialog.LOAD).apply {
            file = formState.directory
            isMultipleMode = false
            isVisible = true
        }
        if (fileDialog.directory != null) {
            formState.directory = Path(fileDialog.directory).resolve(fileDialog.file).toString()
        }
    }

    private fun otherOSDialog(formState: ProjectFormState) {
        val directoryChooser = JFileChooser().apply {
            formState.directory?.let { currentDirectory = File(it) }
            dialogTitle = Label.OPEN_PROJECT_DIRECTORY
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        val option = directoryChooser.showOpenDialog(null)
        if (option == JFileChooser.APPROVE_OPTION) {
            val directory = directoryChooser.selectedFile
            assert(directory.isDirectory)
            formState.directory = directory.absolutePath
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun OpenProjectButtons(formState: ProjectFormState) {
        TextButton(text = Label.CANCEL, onClick = { GlobalState.project.openProjectDialog.close() })
        ComponentSpacer()
        TextButton(text = Label.OPEN, enabled = formState.isValid(), onClick = { formState.trySubmit() })
    }
}
