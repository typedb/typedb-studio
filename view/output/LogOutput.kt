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
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.answer.ConceptMapGroup
import com.vaticle.typedb.client.api.answer.Numeric
import com.vaticle.typedb.client.api.answer.NumericGroup
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.thing.Attribute
import com.vaticle.typedb.client.api.concept.thing.Relation
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.state.common.util.Property
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Message.Type.ERROR
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Message.Type.INFO
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Message.Type.SUCCESS
import com.vaticle.typedb.studio.state.connection.QueryRunner.Response.Message.Type.TYPEQL
import com.vaticle.typedb.studio.state.connection.TransactionState
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form.IconButtonArg
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Tooltip
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.editor.TextEditor
import com.vaticle.typedb.studio.view.highlighter.SyntaxHighlighter
import com.vaticle.typeql.lang.common.TypeQLToken
import com.vaticle.typeql.lang.common.util.Strings
import java.util.stream.Collectors
import mu.KotlinLogging

internal object LogOutput : RunOutput() {

    internal val END_OF_OUTPUT_SPACE = 20.dp

    internal class State constructor(
        internal val editorState: TextEditor.State,
        private val colors: Color.StudioTheme,
        val transactionState: TransactionState
    ) : RunOutput.State() {

        override val name: String = Label.LOG

        companion object {
            private val LOGGER = KotlinLogging.logger {}
        }

        init {
            editorState.onScrollToBottom { editorState.stickToBottom = true }
            editorState.stickToBottom = true
        }

        internal fun jumpToTop() {
            editorState.stickToBottom = false
            editorState.jumpToTop()
        }

        internal fun toggleFinder() {
            editorState.toggleFinder()
        }

        internal fun copyToClipboard() {
            editorState.copyContentToClipboard()
            GlobalState.notification.info(LOGGER, Message.View.TEXT_COPIED_TO_CLIPBOARD)
        }

        internal fun outputFn(message: Response.Message): () -> Unit = {
            editorState.content.addAll(message.text.split("\n").map {
                when (message.type) {
                    INFO -> AnnotatedString(it)
                    SUCCESS, ERROR -> highlightText(message.type, it, colors)
                    TYPEQL -> highlightTypeQL(it)
                }
            })
        }

        internal fun outputFn(numeric: Numeric): () -> Unit = { outputTypeQL(numeric.toString()) }

        internal fun outputFn(conceptMap: ConceptMap): () -> Unit {
            val output = loadToString(conceptMap)
            return { outputTypeQL(output) }
        }

        internal fun outputFn(conceptMapGroup: ConceptMapGroup): () -> Unit {
            val output = loadToString(conceptMapGroup)
            return { outputTypeQL(output) }
        }

        internal fun outputFn(numericGroup: NumericGroup): () -> Unit {
            val output = loadToString(numericGroup)
            return { outputTypeQL(output) }
        }

        private fun outputTypeQL(text: String) {
            editorState.content.addAll(text.split("\n").map { highlightTypeQL(it) })
        }

        private fun highlightTypeQL(text: String) = SyntaxHighlighter.highlight(text, Property.FileType.TYPEQL)

        private fun highlightText(
            type: Response.Message.Type,
            text: String,
            colors: Color.StudioTheme
        ): AnnotatedString {
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

        private fun loadToString(group: NumericGroup): String {
            return loadToString(group.owner()) + " => " + group.numeric().asNumber()
        }

        private fun loadToString(group: ConceptMapGroup): String {
            val str = StringBuilder(loadToString(group.owner()) + " => {\n")
            group.conceptMaps().forEach { str.append(Strings.indent(loadToString(it))) }
            str.append("\n}")
            return str.toString()
        }

        private fun loadToString(conceptMap: ConceptMap): String {
            val content = conceptMap.map().map {
                "$" + it.key + " " + loadToString(it.value) + ";"
            }.stream().collect(Collectors.joining("\n"))

            val str = StringBuilder("{")
            if (content.lines().size > 1) str.append("\n").append(Strings.indent(content)).append("\n")
            else str.append(" ").append(content).append(" ")
            str.append("}")
            return str.toString()
        }

        private fun loadToString(concept: Concept): String {
            return when (concept) {
                is Type -> printType(concept)
                is Thing -> printThing(concept)
                else -> throw IllegalStateException("Unrecognised TypeQL Concept")
            }
        }

        private fun printType(type: Type): String {
            var str = TypeQLToken.Constraint.TYPE.toString() + " " + type.label
            transactionState.transaction?.let {
                type.asRemote(it).supertype?.let {
                    str += " " + TypeQLToken.Constraint.SUB + " " + it.label.scopedName()
                }
            }
            return str
        }

        private fun printThing(thing: Thing): String {
            val str = StringBuilder()
            when (thing) {
                is Attribute<*> -> str.append(Strings.valueToString(thing.value))
                else -> str.append(TypeQLToken.Constraint.IID.toString() + " " + thing.asThing().iid)
            }
            if (thing is Relation) str.append(" ").append(printRolePlayers(thing.asThing().asRelation()))
            str.append(" ").append(TypeQLToken.Constraint.ISA).append(" ")
                .append(thing.asThing().type.label.scopedName())
            return str.toString()
        }

        private fun printRolePlayers(relation: Relation): String {
            val rolePlayers = transactionState.transaction?.let {
                relation.asRemote(it).playersByRoleType.flatMap { (role, players) ->
                    players.map { player -> role.label.name() + ": " + TypeQLToken.Constraint.IID + " " + player.iid }
                }.stream().collect(Collectors.joining(", "))
            } ?: " "
            return "($rolePlayers)"
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
            IconButtonArg(Icon.Code.COPY, tooltip = Tooltip.Arg(Label.COPY_All)) { state.copyToClipboard() },
            IconButtonArg(Icon.Code.MAGNIFYING_GLASS, tooltip = Tooltip.Arg(Label.FIND)) { state.toggleFinder() },
            IconButtonArg(Icon.Code.ARROW_UP_TO_LINE, tooltip = Tooltip.Arg(Label.JUMP_TO_TOP)) { state.jumpToTop() },
            IconButtonArg(
                icon = Icon.Code.ARROW_DOWN_TO_LINE,
                color = { if (state.editorState.stickToBottom) Theme.studio.secondary else Theme.studio.icon },
                tooltip = Tooltip.Arg(Label.JUMP_AND_STICK_TO_BOTTOM)
            ) { state.editorState.stickToBottom = true }
        )
    }
}
