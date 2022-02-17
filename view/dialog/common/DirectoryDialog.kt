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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.view.common.component.Form
import java.awt.FileDialog
import java.nio.file.Path
import javax.swing.JFileChooser
import kotlin.io.path.Path

object DirectoryDialog {

    abstract class SelectDirectoryForm(initPath: Path?) : Form.State {

        var selectedPath: Path? by mutableStateOf(initPath)

        override fun isValid(): Boolean {
            return selectedPath != null
        }
    }

    internal fun launch(state: SelectDirectoryForm, parent: ComposeDialog, title: String) {
        when (Property.OS.Current) {
            Property.OS.MACOS -> macOSDialog(state, parent, title)
            else -> otherOSDialog(state, title)
        }
    }

    internal fun launch(state: SelectDirectoryForm, parent: ComposeWindow, title: String) {
        when (Property.OS.Current) {
            Property.OS.MACOS -> macOSDialog(state, parent, title)
            else -> otherOSDialog(state, title)
        }
    }

    private fun macOSDialog(state: SelectDirectoryForm, parent: ComposeDialog, title: String) {
        macOSDialog(state, FileDialog(parent, title, FileDialog.LOAD))
    }

    private fun macOSDialog(state: SelectDirectoryForm, parent: ComposeWindow, title: String) {
        macOSDialog(state, FileDialog(parent, title, FileDialog.LOAD))
    }

    private fun macOSDialog(state: SelectDirectoryForm, fileDialog: FileDialog) {
        fileDialog.apply {
            directory = state.selectedPath?.toString()
            isMultipleMode = false
            isVisible = true
        }
        if (fileDialog.directory != null) state.selectedPath = Path(fileDialog.directory).resolve(fileDialog.file)
        else state.selectedPath = null
    }

    private fun otherOSDialog(state: SelectDirectoryForm, title: String) {
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