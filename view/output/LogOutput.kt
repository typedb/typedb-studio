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

package com.vaticle.typedb.studio.view.output

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.runner.Response
import com.vaticle.typedb.studio.state.runner.Response.Log.Entry.Type.ERROR
import com.vaticle.typedb.studio.state.runner.Response.Log.Entry.Type.INFO
import com.vaticle.typedb.studio.state.runner.Response.Log.Entry.Type.SUCCESS
import com.vaticle.typedb.studio.state.runner.Response.Log.Entry.Type.TYPEQL
import com.vaticle.typedb.studio.view.common.component.Form.ButtonArg
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.editor.TextEditor
import com.vaticle.typedb.studio.view.highlighter.SyntaxHighlighter

internal object LogOutput : RunOutput() {

    private val END_OF_OUPTUT_SPACE = 20.dp

    internal class State(internal val editorState: TextEditor.State) : RunOutput.State() {

        init {
            editorState.stickToBottom = true
        }

        fun jumpToTop() {
            editorState.stickToBottom = false
            editorState.jumpToTop()
        }

        fun jumpAndStickToBottom() {
            editorState.jumpToBottom()
            editorState.stickToBottom = true
        }

    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    internal fun Layout(state: State) {
        super.Layout(buttons(state)) { modifier ->
            TextEditor.Layout(
                state = state.editorState,
                bottomSpace = END_OF_OUPTUT_SPACE,
                showLine = false,
                modifier = modifier.onPointerEvent(Press) { state.editorState.stickToBottom = false }
                    .mouseScrollFilter { _, _ -> state.editorState.stickToBottom = false; false }
            )
        }
    }

    private fun buttons(state: State): List<ButtonArg> {
        return listOf(
            ButtonArg(Icon.Code.ARROW_UP_TO_LINE) { state.jumpToTop() },
            ButtonArg(
                icon = Icon.Code.ARROW_DOWN_TO_LINE,
                color = { if (state.editorState.stickToBottom) Theme.colors.secondary else Theme.colors.icon }
            ) { state.jumpAndStickToBottom() }
        )
    }

    internal fun format(entry: Response.Log.Entry, colors: Color.Theme): AnnotatedString {
        return when (entry.type) {
            INFO -> AnnotatedString(entry.text)
            SUCCESS, ERROR -> highlightText(entry.type, entry.text, colors)
            TYPEQL -> SyntaxHighlighter.highlight(entry.text, Property.FileType.TYPEQL)
        }
    }

    private fun highlightText(type: Response.Log.Entry.Type, text: String, colors: Color.Theme): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val style = SpanStyle(
            color = when (type) {
                SUCCESS -> colors.secondary
                ERROR -> colors.error2
                else -> throw IllegalArgumentException()
            }
        )
        builder.pushStyle(style)
        builder.append(text)
        builder.pop()
        return builder.toAnnotatedString()
    }
}
