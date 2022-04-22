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
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.resource.Runner
import com.vaticle.typedb.studio.state.resource.Runner.Response.Type.ERROR
import com.vaticle.typedb.studio.state.resource.Runner.Response.Type.INFO
import com.vaticle.typedb.studio.state.resource.Runner.Response.Type.SUCCESS
import com.vaticle.typedb.studio.state.resource.Runner.Response.Type.TYPEQL
import com.vaticle.typedb.studio.view.common.component.Form.IconButtonArg
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.editor.TextEditor
import com.vaticle.typedb.studio.view.highlighter.SyntaxHighlighter

internal object LogOutput : RunOutput() {

    internal val END_OF_OUTPUT_SPACE = 20.dp

    internal class State(internal val editorState: TextEditor.State, private val colors: Color.Theme) :
        RunOutput.State() {

        init {
            editorState.onScrollToBottom { editorState.stickToBottom = true }
            editorState.stickToBottom = true
        }

        fun jumpToTop() {
            editorState.stickToBottom = false
            editorState.jumpToTop()
        }

        fun collect(responses: List<Runner.Response>) {
            editorState.content.addAll(responses.flatMap { response ->
                response.text.split("\n").map { line ->
                    format(response.type, line, colors)
                }
            })
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    internal fun Layout(state: State) {
        super.Layout(buttons(state)) { modifier ->
            TextEditor.Layout(
                state = state.editorState,
                modifier = modifier.onPointerEvent(Press) { state.editorState.stickToBottom = false },
                showLine = false,
                onScroll = { state.editorState.stickToBottom = false }
            )
        }
    }

    private fun buttons(state: State): List<IconButtonArg> {
        return listOf(
            IconButtonArg(Icon.Code.ARROW_UP_TO_LINE) { state.jumpToTop() },
            IconButtonArg(
                icon = Icon.Code.ARROW_DOWN_TO_LINE,
                color = { if (state.editorState.stickToBottom) Theme.colors.secondary else Theme.colors.icon }
            ) { state.editorState.stickToBottom = true }
        )
    }

    internal fun format(type: Runner.Response.Type, text: String, colors: Color.Theme): AnnotatedString {
        return when (type) {
            INFO -> AnnotatedString(text)
            SUCCESS, ERROR -> highlightText(type, text, colors)
            TYPEQL -> SyntaxHighlighter.highlight(text, Property.FileType.TYPEQL)
        }
    }

    private fun highlightText(type: Runner.Response.Type, text: String, colors: Color.Theme): AnnotatedString {
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
