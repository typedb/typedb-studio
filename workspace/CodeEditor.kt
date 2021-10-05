package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.toSwingColor
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextArea
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.MenuElement
import javax.swing.plaf.basic.BasicMenuItemUI

var swingComponent: JComponent? = null

@Composable
fun CodeEditor(code: String, onChange: (value: String) -> Unit, font: Font, modifier: Modifier = Modifier) {
    if (swingComponent == null) {
        val textArea = RSyntaxTextArea().apply {
            text = code
            syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_NONE // TODO: Add TypeQL syntax style
            background = StudioTheme.colors.editorBackground.toSwingColor()
            foreground = StudioTheme.colors.text.toSwingColor()
            caretColor = StudioTheme.colors.primary.toSwingColor()
            currentLineHighlightColor = StudioTheme.colors.background.toSwingColor()
            antiAliasingEnabled = true
            popupMenu.background = StudioTheme.colors.uiElementBackground.toSwingColor()
            popupMenu.subElements.forEach { element ->
                element.component.apply {
                    background = StudioTheme.colors.uiElementBackground.toSwingColor()
                    foreground = StudioTheme.colors.text.toSwingColor()
                    this.font = StudioTheme.typography.codeEditorContextMenuSwing
                    if (this is RTextArea) {
                        this.markAllHighlightColor = StudioTheme.colors.uiElementBorder.toSwingColor()
                    }
                }
            }
            addCaretListener { onChange(text) }
        }

        val scrollPane = RTextScrollPane(textArea).apply {
            verticalScrollBar.preferredSize = Dimension(0, 0)
            horizontalScrollBar.preferredSize = Dimension(0, 0)
            border = BorderFactory.createEmptyBorder()
            background = StudioTheme.colors.uiElementBorder.toSwingColor()
            gutter.borderColor = StudioTheme.colors.uiElementBorder.toSwingColor()
        }
        swingComponent = scrollPane
    }

    Box(modifier = modifier) {
        SwingPanel(background = StudioTheme.colors.editorBackground,
            factory = {
                JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    add(swingComponent)
                }
            },
        )
    }
}
