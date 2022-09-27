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

package com.vaticle.typedb.studio.framework.material

import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import com.vaticle.typedb.studio.state.common.util.Property
import java.awt.FileDialog
import java.io.File
import java.util.Optional
import javax.swing.JFileChooser

object FileSelectorDialog {
    fun selectFilePath(parent: ComposeDialog, title: String, selectorOptions: SelectorOptions): String {
        val file = when (Property.OS.Current) {
            Property.OS.MACOS -> macOSFileSelector(parent, title, selectorOptions)
            else -> otherOSFileSelector(title, selectorOptions)
        }

        return if (file.isEmpty) "" else file.get().absolutePath
    }

    private fun macOSFileSelector(parent: ComposeDialog, title: String, selectorOptions: SelectorOptions): Optional<File> {
        when (selectorOptions) {
            SelectorOptions.FILES -> System.setProperty("apple.awt.fileDialogForDirectories", "false")
            SelectorOptions.DIRECTORIES -> System.setProperty("apple.awt.fileDialogForDirectories", "true")
        }

        val fileDialog = FileDialog(parent, title, FileDialog.LOAD)
        fileDialog.apply {
            isMultipleMode = false
            isVisible = true
        }

        return if (fileDialog.file == null) {
            Optional.empty()
        } else {
            Optional.of(File("${fileDialog.directory}/${fileDialog.file}"))
        }
    }

    private fun otherOSFileSelector(title: String, selectorOptions: SelectorOptions): Optional<File> {
        val selectionMode = when (selectorOptions) {
            SelectorOptions.FILES -> JFileChooser.FILES_ONLY
            SelectorOptions.DIRECTORIES -> JFileChooser.DIRECTORIES_ONLY
        }

        val fileChooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = selectionMode
        }

        val option = fileChooser.showOpenDialog(null)
        return if (option == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            assert(file.isFile)
            Optional.of(file.absoluteFile)
        } else {
            Optional.empty()
        }
    }

    enum class SelectorOptions {
        FILES,
        DIRECTORIES,
    }
}