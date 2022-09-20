package com.vaticle.typedb.studio.framework.material

import androidx.compose.ui.awt.ComposeDialog
import com.vaticle.typedb.studio.state.common.util.Property
import java.awt.FileDialog
import java.io.File
import java.util.Optional
import javax.swing.JFileChooser

object FileSelectorDialog {
    fun selectFile(parent: ComposeDialog, title: String, selectorOptions: SelectorOptions): Optional<File> {
        return when (Property.OS.Current) {
            Property.OS.MACOS -> macOSFileSelector(parent, title, selectorOptions)
            else -> otherOSFileSelector(title, selectorOptions)
        }
    }

    private fun macOSFileSelector(parent: ComposeDialog, title: String, selectorOptions: SelectorOptions): Optional<File> {
        val fileDialog = FileDialog(parent, title, FileDialog.LOAD)
        fileDialog.apply {
            isMultipleMode = false
            isVisible = true
        }
        return (Optional.of(File(fileDialog.file)))
    }

    private fun otherOSFileSelector(title: String, selectorOptions: SelectorOptions): Optional<File> {
        val selectionMode = when (selectorOptions) {
            SelectorOptions.FILES -> JFileChooser.FILES_ONLY
            SelectorOptions.DIRECTORIES -> JFileChooser.DIRECTORIES_ONLY
            SelectorOptions.FILES_AND_DIRECTORIES -> JFileChooser.FILES_AND_DIRECTORIES
        }

        val fileChooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = selectionMode
        }

        val option = fileChooser.showOpenDialog(null)
        if (option == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            assert(file.isFile)
            return Optional.of(file.absoluteFile)
        } else {
            return Optional.empty()
        }
    }

    enum class SelectorOptions {
        FILES,
        DIRECTORIES,
        FILES_AND_DIRECTORIES
    }
}