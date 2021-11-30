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

package com.vaticle.typedb.studio.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form.ComponentSpacer
import com.vaticle.typedb.studio.view.common.component.Form.Field
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.Submission
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Form.TextInput
import com.vaticle.typedb.studio.view.common.component.Icon
import javax.swing.JFileChooser


object Project {

    private val WINDOW_WIDTH = 500.dp
    private val WINDOW_HEIGHT = 140.dp

    class FormState(currentDirectory: String?) {

        var directory: String? by mutableStateOf(currentDirectory)

        fun isValid(): Boolean {
            return !directory.isNullOrBlank()
        }

        fun trySubmitIfValid() {
            if (isValid()) trySubmit()
        }

        fun trySubmit() {
            assert(!directory.isNullOrBlank())
            State.project.tryOpenDirectory(directory!!)
        }
    }

    @Composable
    fun Window() {
        val formState = remember { FormState(State.project.directory?.toString()) }
        Window(
            title = Label.OPEN_PROJECT_DIRECTORY,
            onCloseRequest = { State.project.showWindow = false },
            state = rememberWindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(WINDOW_WIDTH, WINDOW_HEIGHT)
            )
        ) {
            Submission(onSubmit = { formState.trySubmitIfValid() }) {
                SelectDirectoryField(formState)
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
    private fun SelectDirectoryField(formState: FormState) {
        Field(label = Label.DIRECTORY) {
            Row {
                TextInput(
                    value = formState.directory ?: "",
                    placeholder = Label.PATH_TO_PROJECT,
                    onValueChange = { formState.directory = it },
                    modifier = Modifier.weight(1f),
                )
                ComponentSpacer()
                IconButton(icon = Icon.Code.FOLDER_OPEN, onClick = { launchFileDialog(formState) })
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
        TextButton(text = Label.CANCEL, onClick = { State.project.showWindow = false })
        ComponentSpacer()
        TextButton(text = Label.OPEN, enabled = formState.isValid(), onClick = { formState.trySubmit() })
    }
}
