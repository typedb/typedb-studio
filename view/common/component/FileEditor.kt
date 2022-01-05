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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Typography
import com.vaticle.typedb.studio.view.common.util.SwingUtil.toSwingColor
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextArea
import org.fife.ui.rtextarea.RTextScrollPane

object FileEditor {

    @Composable
    fun Area(content: String, onChange: (code: String) -> Unit, modifier: Modifier = Modifier) {
        val colors = Theme.colors
        val typography = Theme.typography
        SwingPanel(Theme.colors.background2, { textArea(content, colors, typography, onChange) }, modifier)
    }

    private fun textArea(
        content: String, colors: Color.Theme, typography: Typography.Theme, onChange: (code: String) -> Unit
    ): RTextScrollPane {
        val textArea = RSyntaxTextArea().apply {
            text = content
            font = typography.code1SwingFont
            syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_NONE // TODO: Add TypeQL syntax style
            background = colors.background2.toSwingColor()
            foreground = colors.onBackground.toSwingColor()
            caretColor = colors.secondary.toSwingColor()
            currentLineHighlightColor = colors.surface.toSwingColor()
            antiAliasingEnabled = true
            popupMenu.background = colors.surface.toSwingColor()
            popupMenu.subElements.forEach { element ->
                element.component.apply {
                    background = colors.surface.toSwingColor()
                    foreground = colors.onSurface.toSwingColor()
                    font = typography.body1SwingFont
                    if (this is RTextArea) markAllHighlightColor = colors.border.toSwingColor()
                }
            }
            document.addDocumentListener(createDocumentListener { onChange(text) })
        }

        return RTextScrollPane(textArea).apply {
            verticalScrollBar.preferredSize = Dimension(0, 0)
            horizontalScrollBar.preferredSize = Dimension(0, 0)
            border = BorderFactory.createEmptyBorder()
            background = colors.border.toSwingColor()
            gutter.borderColor = colors.border.toSwingColor()
        }
    }

    private fun createDocumentListener(onUpdate: () -> Unit): DocumentListener {
        return object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                onUpdate()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                onUpdate()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                // ignored
            }
        }
    }
}
