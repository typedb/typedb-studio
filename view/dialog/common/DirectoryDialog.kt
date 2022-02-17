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

import androidx.compose.ui.awt.ComposeDialog
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import java.awt.FileDialog
import java.io.File
import javax.swing.JFileChooser
import kotlin.io.path.Path

object DirectoryDialog {

    abstract class SelectDirectoryForm : Form.State {

        abstract var directory: String?

        override fun isValid(): Boolean {
            return !directory.isNullOrBlank()
        }
    }

    internal fun launch(state: SelectDirectoryForm, parent: ComposeDialog) {
        when (Property.OS.Current) {
            Property.OS.MACOS -> macOSDialog(state, parent)
            else -> otherOSDialog(state)
        }
    }

    private fun macOSDialog(state: SelectDirectoryForm, parent: ComposeDialog) {
        val fileDialog = FileDialog(parent, Label.OPEN_PROJECT_DIRECTORY, FileDialog.LOAD).apply {
            file = state.directory
            isMultipleMode = false
            isVisible = true
        }
        fileDialog.directory?.let {
            state.directory = Path(it).resolve(fileDialog.file).toString()
        }
    }

    private fun otherOSDialog(state: SelectDirectoryForm) {
        val directoryChooser = JFileChooser().apply {
            state.directory?.let { currentDirectory = File(it) }
            dialogTitle = Label.OPEN_PROJECT_DIRECTORY
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        val option = directoryChooser.showOpenDialog(null)
        if (option == JFileChooser.APPROVE_OPTION) {
            val directory = directoryChooser.selectedFile
            assert(directory.isDirectory)
            state.directory = directory.absolutePath
        }
    }
}