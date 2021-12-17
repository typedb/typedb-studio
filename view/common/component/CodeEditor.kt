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

package com.vaticle.typedb.studio.view.common.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.state.common.Message.View.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.util.SwingUtil.toSwingColor
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import mu.KotlinLogging
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextArea
import org.fife.ui.rtextarea.RTextScrollPane

var swingComponent: JComponent? = null

@Composable
fun FileEditor(code: String, editorID: String, onChange: (code: String) -> Unit, modifier: Modifier = Modifier) {

    val LOGGER = KotlinLogging.logger {}
    val textArea = remember { RSyntaxTextArea() }
    var documentListener: DocumentListener? by remember { mutableStateOf(null) }

    fun createDocumentListener(): DocumentListener {
        return object : DocumentListener {
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
            font = Theme.typography.code1SwingFont
            syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_NONE // TODO: Add TypeQL syntax style
            background = Theme.colors.background2.toSwingColor()
            foreground = Theme.colors.onBackground.toSwingColor()
            caretColor = Theme.colors.secondary.toSwingColor()
            currentLineHighlightColor = Theme.colors.surface.toSwingColor()
            antiAliasingEnabled = true
            popupMenu.background = Theme.colors.surface.toSwingColor()
            popupMenu.subElements.forEach { element ->
                element.component.apply {
                    background = Theme.colors.surface.toSwingColor()
                    foreground = Theme.colors.onSurface.toSwingColor()
                    font = Theme.typography.body1SwingFont
                    if (this is RTextArea) markAllHighlightColor = Theme.colors.border.toSwingColor()
                }
            }
            documentListener = createDocumentListener()
            document.addDocumentListener(documentListener)
        }

        val scrollPane = RTextScrollPane(textArea).apply {
            verticalScrollBar.preferredSize = Dimension(0, 0)
            horizontalScrollBar.preferredSize = Dimension(0, 0)
            border = BorderFactory.createEmptyBorder()
            background = Theme.colors.border.toSwingColor()
            gutter.borderColor = Theme.colors.border.toSwingColor()
        }
        swingComponent = scrollPane
    }

    Box(modifier = modifier) {
        SwingPanel(
            background = Theme.colors.background2,
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
        try {
            // Temporarily "switch off" the document listener so that setText doesn't emit a change event
            documentListener?.let { textArea.document.removeDocumentListener(it) }
            textArea.text = code
            documentListener = createDocumentListener()
            textArea.document.addDocumentListener(documentListener)
        } catch (e: Exception) {
            State.notification.systemError(LOGGER, e, UNEXPECTED_ERROR)
        }

        onDispose {
            // We need to deallocate this reference, otherwise after logging out and back in, we'll still be attached
            // to the old, no longer visible instance of RSyntaxTextArea
            swingComponent = null
        }
    }
}
