/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.output

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.driver.api.answer.ConceptMap
import com.vaticle.typedb.driver.api.answer.ConceptMapGroup
import com.vaticle.typedb.driver.api.answer.JSON
import com.vaticle.typedb.driver.api.answer.ValueGroup
import com.vaticle.typedb.driver.api.concept.Concept
import com.vaticle.typedb.driver.api.concept.thing.Attribute
import com.vaticle.typedb.driver.api.concept.thing.Relation
import com.vaticle.typedb.driver.api.concept.thing.Thing
import com.vaticle.typedb.driver.api.concept.type.Type
import com.vaticle.typedb.driver.api.concept.value.Value
import com.vaticle.typedb.studio.framework.common.Util
import com.vaticle.typedb.studio.framework.common.theme.Color
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.editor.TextEditor
import com.vaticle.typedb.studio.framework.material.Form.IconButtonArg
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Tooltip
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Message
import com.vaticle.typedb.studio.service.common.util.Property
import com.vaticle.typedb.studio.service.connection.QueryRunner.Response
import com.vaticle.typedb.studio.service.connection.QueryRunner.Response.Message.Type.ERROR
import com.vaticle.typedb.studio.service.connection.QueryRunner.Response.Message.Type.INFO
import com.vaticle.typedb.studio.service.connection.QueryRunner.Response.Message.Type.SUCCESS
import com.vaticle.typedb.studio.service.connection.QueryRunner.Response.Message.Type.TYPEQL
import com.vaticle.typedb.studio.service.connection.TransactionState
import com.vaticle.typeql.lang.common.TypeQLToken
import com.vaticle.typeql.lang.common.util.Strings
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mu.KotlinLogging

internal class LogOutput constructor(
    private val editorState: TextEditor.State,
    private val transactionState: TransactionState,
    private val colors: Color.StudioTheme
) : RunOutput() {

    companion object {
        private val END_OF_OUTPUT_SPACE = 20.dp
        private val RUNNING_INDICATOR_DELAY = 3.seconds
        private val LOGGER = KotlinLogging.logger {}

        @Composable
        fun create(transactionState: TransactionState): LogOutput {
            return LogOutput(TextEditor.createState(END_OF_OUTPUT_SPACE), transactionState, Theme.studio)
        }
    }

    private val isCollecting = AtomicBoolean(false)
    private val lastOutputTime = AtomicLong(System.currentTimeMillis())
    private val coroutines = CoroutineScope(Dispatchers.Default)

    override val name: String = Label.LOG
    override val icon: Icon = Icon.TEXT_LEFT_ALIGN
    override val buttons: List<IconButtonArg> = listOf(
        IconButtonArg(Icon.COPY, tooltip = Tooltip.Arg(Label.COPY_All)) { copyToClipboard() },
        IconButtonArg(Icon.FIND, tooltip = Tooltip.Arg(Label.FIND)) { toggleFinder() },
        IconButtonArg(Icon.JUMP_TO_TOP, tooltip = Tooltip.Arg(Label.JUMP_TO_TOP)) { jumpToTop() },
        IconButtonArg(
            icon = Icon.JUMP_TO_BOTTOM,
            color = { if (editorState.stickToBottom) Theme.studio.secondary else Theme.studio.icon },
            tooltip = Tooltip.Arg(Label.JUMP_AND_STICK_TO_BOTTOM)
        ) { editorState.stickToBottom = true }
    )

    init {
        editorState.onScrollToBottom { editorState.stickToBottom = true }
        editorState.stickToBottom = true
    }

    internal fun start() {
        isCollecting.set(true)
        launchRunningIndicator()
    }

    internal fun stop() {
        isCollecting.set(false)
    }

    private fun jumpToTop() {
        editorState.stickToBottom = false
        editorState.jumpToTop()
    }

    private fun toggleFinder() {
        editorState.toggleFinder()
    }

    private fun copyToClipboard() {
        editorState.copyContentToClipboard()
        Service.notification.info(LOGGER, Message.Framework.TEXT_COPIED_TO_CLIPBOARD)
    }

    private fun launchRunningIndicator() = coroutines.launchAndHandle(Service.notification, LOGGER) {
        var duration = RUNNING_INDICATOR_DELAY
        while (isCollecting.get()) {
            delay(duration)
            if (!isCollecting.get()) return@launchAndHandle
            val timeSinceLastResponse = System.currentTimeMillis() - lastOutputTime.get()
            if (timeSinceLastResponse >= RUNNING_INDICATOR_DELAY.inWholeMilliseconds) {
                output(INFO, Util.ELLIPSES)
                duration = RUNNING_INDICATOR_DELAY
            } else {
                duration = RUNNING_INDICATOR_DELAY - timeSinceLastResponse.milliseconds
            }
        }
    }

    internal fun outputFn(message: Response.Message): () -> Unit = { output(message.type, message.text) }

    internal fun outputFn(value: Value?): () -> Unit = { output(TYPEQL, printValue(value)) }

    internal fun outputFn(conceptMap: ConceptMap): () -> Unit {
        val output = loadToString(conceptMap)
        return { output(TYPEQL, output) }
    }

    internal fun outputFn(conceptMapGroup: ConceptMapGroup): () -> Unit {
        val output = loadToString(conceptMapGroup)
        return { output(TYPEQL, output) }
    }

    internal fun outputFn(valueGroup: ValueGroup): () -> Unit {
        val output = loadToString(valueGroup)
        return { output(TYPEQL, output) }
    }

    internal fun outputFn(json: JSON): () -> Unit {
        val output = loadToString(json)
        return { output(TYPEQL, output) }
    }

    private fun output(type: Response.Message.Type, text: String) {
        when (type) {
            INFO -> editorState.addContent(text)
            SUCCESS, ERROR -> editorState.addContent(text) { highlightText(type, it, colors) }
            TYPEQL -> editorState.addContent(text, Property.FileType.TYPEQL)
        }
        lastOutputTime.set(System.currentTimeMillis())
    }

    private fun highlightText(type: Response.Message.Type, text: String, colors: Color.StudioTheme): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val style = SpanStyle(
            color = when (type) {
                SUCCESS -> colors.secondary
                ERROR -> colors.errorStroke
                else -> throw IllegalArgumentException()
            }
        )
        builder.pushStyle(style)
        builder.append(text)
        builder.pop()
        return builder.toAnnotatedString()
    }

    private fun loadToString(group: ValueGroup): String {
        return loadToString(group.owner()) + " => " + group.value().toString()
    }

    private fun loadToString(json: JSON): String = json.toString()

    private fun loadToString(group: ConceptMapGroup): String {
        val str = StringBuilder(loadToString(group.owner()) + " => {\n")
        group.conceptMaps().forEach { str.append(Strings.indent(loadToString(it))) }
        str.append("\n}")
        return str.toString()
    }

    private fun loadToString(conceptMap: ConceptMap): String {
        val content = conceptMap.variables().map {
            formatVariable(it, conceptMap.get(it))
        }.collect(Collectors.joining("\n"))

        val str = StringBuilder("{")
        if (content.lines().size > 1) str.append("\n").append(Strings.indent(content)).append("\n")
        else str.append(" ").append(content).append(" ")
        str.append("}")
        return str.toString()
    }

    private fun formatVariable(variable: String, concept: Concept): String {
        val str = StringBuilder()
        if (concept.isValue)
            str.append("?").append(variable).append(" = ")
        else
            str.append("$").append(variable).append(" ")
        str.append(loadToString(concept)).append(";")
        return str.toString()
    }

    private fun loadToString(concept: Concept): String {
        return when (concept) {
            is Type -> printType(concept)
            is Thing -> printThing(concept)
            is Value -> printValue(concept)
            else -> throw IllegalStateException("Unrecognised TypeQL Concept")
        }
    }

    private fun printType(type: Type): String {
        var str = TypeQLToken.Constraint.TYPE.toString() + " " + type.label
        transactionState.transaction?.let {
            type.getSupertype(it).resolve()?.let {
                str += " " + TypeQLToken.Constraint.SUB + " " + it.label.scopedName()
            }
        }
        return str
    }

    private fun printThing(thing: Thing): String {
        val str = StringBuilder()
        when (thing) {
            is Attribute -> str.append(Strings.valueToString(thing.value))
            else -> str.append(TypeQLToken.Constraint.IID.toString() + " " + thing.asThing().iid)
        }
        if (thing is Relation) str.append(" ").append(printRolePlayers(thing.asThing().asRelation()))
        str.append(" ").append(TypeQLToken.Constraint.ISA).append(" ")
            .append(thing.asThing().type.label.scopedName())
        return str.toString()
    }

    private fun printRolePlayers(relation: Relation): String {
        val rolePlayers = transactionState.transaction?.let {
            relation.getPlayers(it).flatMap { (role, players) ->
                players.map { player -> role.label.name() + ": " + TypeQLToken.Constraint.IID + " " + player.iid }
            }.stream().collect(Collectors.joining(", "))
        } ?: " "
        return "($rolePlayers)"
    }

    private fun printValue(value: Value?) = value?.let { Strings.valueToString(value) } ?: "NaN"

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun content(modifier: Modifier) {
        TextEditor.Layout(
            state = editorState,
            modifier = modifier.onPointerEvent(Press) { editorState.stickToBottom = false },
            showLine = false,
            onScroll = { editorState.stickToBottom = false }
        )
    }
}
