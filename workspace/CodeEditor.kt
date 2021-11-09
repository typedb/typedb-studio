package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.toSwingColor
import com.vaticle.typedb.studio.diagnostics.withErrorHandling
import mu.KotlinLogging.logger
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextArea
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

var swingComponent: JComponent? = null

@Composable
fun CodeEditor(code: String, editorID: String, onChange: (code: String) -> Unit, font: java.awt.Font,
               modifier: Modifier = Modifier, snackbarHostState: SnackbarHostState) {
    val log = logger {}
    val snackbarCoroutineScope = rememberCoroutineScope()
    val textArea = remember { RSyntaxTextArea() }
    var documentListener: DocumentListener? by remember { mutableStateOf(null) }

    fun createDocumentListener(): DocumentListener {
        return object: DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                onChange(textArea.text)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                onChange(textArea.text)
            }

            override fun changedUpdate(e: DocumentEvent?) {
                // ignored
            }
        }
    }

    if (swingComponent == null) {
        // TODO: we need to not use 'if (X == null) initialise X'. It's ugly and it makes it impossible to use try-catch
        textArea.apply {
            text = code
            this.font = font
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
            documentListener = createDocumentListener()
            document.addDocumentListener(documentListener)
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
                val panel = JPanel()
                try {
                    panel.apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        add(swingComponent)
                    }
                } catch (e: Exception) {
                    // TODO: ideally we should use our default error handler and log the error properly, but this creates
                    //       a bug where code editing breaks: see #515
                    println(e)
                }
                panel
            },
        )
    }

    DisposableEffect(editorID) {
        withErrorHandling({ "An error occurred initialising CodeEditor" }, log, snackbarHostState, snackbarCoroutineScope) {
            // Temporarily "switch off" the document listener so that setText doesn't emit a change event
            documentListener?.let { textArea.document.removeDocumentListener(it) }
            textArea.text = code
            documentListener = createDocumentListener()
            textArea.document.addDocumentListener(documentListener)
        }

        onDispose {
            // We need to deallocate this reference, otherwise after logging out and back in, we'll still be attached
            // to the old, no longer visible instance of RSyntaxTextArea
            swingComponent = null
        }
    }
}
