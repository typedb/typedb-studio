/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.material

import androidx.compose.ui.awt.ComposeDialog
import com.typedb.studio.service.common.util.Property.OS
import java.awt.FileDialog
import java.io.File
import javax.swing.JFileChooser

object SelectFileDialog {
    data class Result(val selectedPath: String?)

    fun open(parent: ComposeDialog, title: String, selectorOptions: SelectorOptions): Result {
        val file = when (OS.Current) {
            OS.MACOS -> macOSFileSelector(parent, title, selectorOptions)
            else -> otherOSFileSelector(title, selectorOptions)
        }

        return Result(file?.absolutePath)
    }

    private fun macOSFileSelector(parent: ComposeDialog, title: String, selectorOptions: SelectorOptions): File? {
        when (selectorOptions) {
            SelectorOptions.FILES_ONLY -> System.setProperty("apple.awt.fileDialogForDirectories", "false")
            SelectorOptions.DIRECTORIES_ONLY -> System.setProperty("apple.awt.fileDialogForDirectories", "true")
        }

        val fileDialog = FileDialog(parent, title, FileDialog.LOAD)
        fileDialog.apply {
            isMultipleMode = false
            isVisible = true
        }

        // When selecting a directory, fileDialog.file is the selected directory and fileDialog.directory is the rest
        // of the path. Therefore, if fileDialog.file is null, then no directory was selected.
        if (fileDialog.file == null) {
            return null
        }

        return File("${fileDialog.directory}/${fileDialog.file}").absoluteFile
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
