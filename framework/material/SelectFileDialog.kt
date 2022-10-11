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
import com.vaticle.typedb.studio.state.common.util.Property.OS
import java.awt.FileDialog
import java.io.File
import javax.swing.JFileChooser

object SelectFileDialog {
    fun selectFilePath(parent: ComposeDialog, title: String, selectorOptions: SelectorOptions): String? {
        val file = when (OS.Current) {
            OS.MACOS -> macOSFileSelector(parent, title, selectorOptions)
            else -> otherOSFileSelector(title, selectorOptions)
        }

        return file?.absolutePath
    }

    private fun macOSFileSelector(parent: ComposeDialog, title: String, selectorOptions: SelectorOptions): File? {
        when (selectorOptions) {
            SelectorOptions.FILES_ONLY -> System.setProperty("apple.awt.fileDialogForDirectories", "false")
            SelectorOptions.DIRECTORIES_ONLY -> System.setProperty("apple.awt.fileDialogForDirectories", "true")
        }

        val fileDialog = java.awt.FileDialog(parent, title, FileDialog.LOAD)
        fileDialog.apply {
            isMultipleMode = false
            isVisible = true
        }

        return if (fileDialog.directory == null && fileDialog.file == null) {
            null
        } else {
            File("${fileDialog.directory}/${fileDialog.file}").absoluteFile
        }
    }

    private fun otherOSFileSelector(title: String, selectorOptions: SelectorOptions): File? {
        val selectionMode = selectorOptions.toJFileChooserOptions()
        val fileChooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = selectionMode
        }

        val option = fileChooser.showOpenDialog(null)
        return if (option == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile.absoluteFile
        } else {
            null
        }
    }

    enum class SelectorOptions {
        FILES_ONLY,
        DIRECTORIES_ONLY;

        fun toJFileChooserOptions(): Int {
            return when (this) {
                FILES_ONLY -> JFileChooser.FILES_ONLY
                DIRECTORIES_ONLY -> JFileChooser.DIRECTORIES_ONLY
            }
        }
    }
}