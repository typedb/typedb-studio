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

package com.vaticle.typedb.studio.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.common.Label
import com.vaticle.typedb.studio.common.component.Form
import com.vaticle.typedb.studio.common.component.Icon
import com.vaticle.typedb.studio.common.theme.Theme
import com.vaticle.typedb.studio.service.Service
import javax.swing.JFileChooser


object ProjectWindow {

    private val WINDOW_WIDTH = 500.dp
    private val WINDOW_HEIGHT = 140.dp

    class FormState(currentDirectory: String?) {

        var directory: String? by mutableStateOf(currentDirectory)

        fun isValid(): Boolean {
            return !directory.isNullOrBlank()
        }

        fun tryOpen() {

        }
    }

    @Composable
    fun Layout() {
        var formState = remember { FormState(Service.project.currentDirectory) }
        Window(
            title = Label.OPEN_PROJECT_DIRECTORY,
            onCloseRequest = { Service.project.showWindow = false },
            state = rememberWindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition.Aligned(Alignment.Center),
                size = WindowSize(WINDOW_WIDTH, WINDOW_HEIGHT)
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Theme.colors.background).padding(Form.SPACING)) {
                Form.FieldGroup {
                    SelectDirectoryField(formState)
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Spacer(modifier = Modifier.weight(1f))
                        OpenProjectButtons(formState)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun SelectDirectoryField(formState: FormState) {
        Form.Field(label = Label.DIRECTORY) {
            Row {
                Form.TextInput(
                    value = formState.directory ?: "",
                    placeholder = Label.PATH_TO_PROJECT,
                    onValueChange = { formState.directory = it },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Form.SPACING))
                Form.IconButton(icon = Icon.Code.FolderOpen, onClick = { launchFileDialog(formState) })
            }
        }
    }

    private fun launchFileDialog(formState: FormState) {
        val directoryChooser = JFileChooser().apply {
            dialogTitle = Label.OPEN_PROJECT_DIRECTORY
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        val option = directoryChooser.showOpenDialog(null)
        if (option == JFileChooser.APPROVE_OPTION) {
            val directory = directoryChooser.selectedFile
            assert(directory.isDirectory)
            formState.directory = directory.absolutePath
            println(formState.directory)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun OpenProjectButtons(formState: FormState) {
        Form.TextButton(text = Label.CANCEL, onClick = { Service.project.showWindow = false })
        Spacer(modifier = Modifier.width(Form.SPACING))
        Form.TextButton(text = Label.OPEN, enabled = formState.isValid(), onClick = { formState.tryOpen() })
    }
}
