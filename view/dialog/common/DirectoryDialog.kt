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

package com.vaticle.typedb.studio.view.dialog.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Icon
import java.awt.FileDialog
import java.nio.file.Path
import javax.swing.JFileChooser
import kotlin.io.path.Path

object DirectoryDialog {

    class SelectDirectoryForm(
        initPath: Path?,
        internal val onCancel: () -> Unit,
        internal val onSubmit: (Path) -> Unit
    ) : Form.State {

        var selectedPath: Path? by mutableStateOf(initPath)

        override fun isValid(): Boolean {
            return selectedPath != null
        }

        override fun trySubmit() {
            assert(isValid())
            onSubmit(selectedPath!!)
        }
    }

    private val DIRECTORY_DIALOG_WIDTH = 500.dp
    private val DIRECTORY_DIALOG_HEIGHT = 200.dp

    @Composable
    fun Layout(state: SelectDirectoryForm, title: String, message: String, submitLabel: String) {
        Dialog(
            title = title,
            onCloseRequest = { state.onCancel },
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(DIRECTORY_DIALOG_WIDTH, DIRECTORY_DIALOG_HEIGHT)
            )
        ) {
            Form.Submission(state = state) {
                Form.Text(value = message, softWrap = true)
                SelectDirectoryField(state, window, title)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    SelectDirectoryButtons(state, submitLabel)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun SelectDirectoryField(state: SelectDirectoryForm, window: ComposeDialog, title: String) {
        Form.Field(label = Label.DIRECTORY) {
            Row {
                Form.TextInput(
                    value = state.selectedPath?.toString() ?: "",
                    placeholder = Label.PATH_OF_DIRECTORY,
                    onValueChange = { state.selectedPath = Path(it) },
                    modifier = Modifier.fillMaxHeight().weight(1f),
                )
                Form.ComponentSpacer()
                Form.IconButton(
                    icon = Icon.Code.FOLDER_OPEN,
                    onClick = { launchDirectorySelector(state, window, title) }
                )
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun SelectDirectoryButtons(state: SelectDirectoryForm, submitLabel: String) {
        Form.TextButton(text = Label.CANCEL, onClick = { state.onCancel() })
        Form.ComponentSpacer()
        Form.TextButton(text = submitLabel, enabled = state.isValid(), onClick = { state.trySubmit() })
    }

    private fun launchDirectorySelector(state: SelectDirectoryForm, parent: ComposeDialog, title: String) {
        when (Property.OS.Current) {
            Property.OS.MACOS -> macOSDirectorySelector(state, parent, title)
            else -> otherOSDirectorySelector(state, title)
        }
    }

    private fun macOSDirectorySelector(state: SelectDirectoryForm, parent: ComposeDialog, title: String) {
        val fileDialog = FileDialog(parent, title, FileDialog.LOAD)
        fileDialog.apply {
            directory = state.selectedPath?.toString()
            isMultipleMode = false
            isVisible = true
        }
        fileDialog.directory?.let { state.selectedPath = Path(it).resolve(fileDialog.file) }
    }

    private fun otherOSDirectorySelector(state: SelectDirectoryForm, title: String) {
        val directoryChooser = JFileChooser().apply {
            state.selectedPath?.let { currentDirectory = it.toFile() }
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        val option = directoryChooser.showOpenDialog(null)
        if (option == JFileChooser.APPROVE_OPTION) {
            val directory = directoryChooser.selectedFile
            assert(directory.isDirectory)
            state.selectedPath = Path(directory.absolutePath)
        }
    }
}