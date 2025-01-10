/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.output

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.typedb.driver.api.QueryType
import com.typedb.driver.api.Transaction
import com.typedb.driver.api.answer.ConceptRow
import com.typedb.driver.api.answer.JSON
import com.typedb.driver.api.concept.Concept
import com.typedb.driver.api.concept.instance.Attribute
import com.typedb.driver.api.concept.instance.Entity
import com.typedb.driver.api.concept.instance.Instance
import com.typedb.driver.api.concept.instance.Relation
import com.typedb.driver.api.concept.type.Type
import com.typedb.driver.api.concept.value.Value
import com.typedb.studio.framework.common.Util
import com.typedb.studio.framework.common.theme.Color
import com.typedb.studio.framework.common.theme.Theme
import com.typedb.studio.framework.editor.TextEditor
import com.typedb.studio.framework.material.Form.IconButtonArg
import com.typedb.studio.framework.material.Icon
import com.typedb.studio.framework.material.Tooltip
import com.typedb.studio.service.Service
import com.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.typedb.studio.service.common.util.Label
import com.typedb.studio.service.common.util.Message
import com.typedb.studio.service.common.util.Property
import com.typedb.studio.service.connection.QueryRunner.Response
import com.typedb.studio.service.connection.QueryRunner.Response.Message.Type.ERROR
import com.typedb.studio.service.connection.QueryRunner.Response.Message.Type.INFO
import com.typedb.studio.service.connection.QueryRunner.Response.Message.Type.SUCCESS
import com.typedb.studio.service.connection.QueryRunner.Response.Message.Type.TYPEQL
import com.typedb.studio.service.connection.TransactionState
import com.typeql.lang.common.TypeQLToken
import com.typeql.lang.common.util.Strings
import java.io.PrintStream
import java.util.Arrays
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

    internal fun outputFn(value: Value): () -> Unit = { output(TYPEQL, valueDisplayString(value)) }

    internal fun outputFn(row: ConceptRow): () -> Unit {
        val output = loadToString(row)
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

    private fun loadToString(json: JSON): String = json.toString()

    private fun loadToString(row: ConceptRow): String {
        val columnNames = row.columnNames().collect(Collectors.toList())
        val columnsWidth = columnNames.stream().map { obj: String -> obj.length }
            .max(Comparator.comparingInt { obj: Int -> obj }).orElse(0)
        return conceptRowDisplayString(row, columnNames, columnsWidth)
    }

//    private fun formatVariable(variable: String, concept: Concept): String {
//        val str = StringBuilder()
//        if (concept.isValue)
//            str.append("?").append(variable).append(" = ")
//        else
//            str.append("$").append(variable).append(" ")
//        str.append(loadToString(concept)).append(";")
//        return str.toString()
//    }
//
//    private fun loadToString(concept: Concept): String {
//        return when (concept) {
//            is Type -> printType(concept)
//            is Thing -> printThing(concept)
//            is Value -> printValue(concept)
//            else -> throw IllegalStateException("Unrecognised TypeQL Concept")
//        }
//    }

    private val TABLE_DASHES = 7
    private val TABLE_INDENT = "   "
    private val CONTENT_INDENT = "    "

//    fun info(s: String?) {
//        out!!.println(s)
//    }
//
//    fun error(s: String) {
//        err!!.println(colorError(s))
//    }
//
//    fun value(answer: Value?) {
//        out!!.println(stringifyNumericValue(answer))
//    }

//    private fun stringifyNumericValue(value: Value?): String {
//        return value?.toString() ?: "NaN"
//    }

//    private fun conceptRowDisplayStringHeader(queryType: QueryType, columnsWidth: Int): String {
//        val sb = StringBuilder()
//        sb.append(QUERY_COMPILATION_SUCCESS)
//        sb.append("\n")
//
//        if (queryType.isWrite) {
//            sb.append(QUERY_WRITE_SUCCESS)
//            sb.append(". ")
//        }
//
//        assert(
//            !queryType.isSchema // expected to return another type of answer
//        )
//        sb.append(QUERY_STREAMING_ROWS)
//        sb.append("\n\n")
//
//        if (columnsWidth != 0) {
//            sb.append(lineDashSeparator(columnsWidth))
//        }
//
//        return sb.toString()
//    }

    private fun conceptRowDisplayString(
        conceptRow: ConceptRow, columnNames: List<String>, columnsWidth: Int,
    ): String {
        val content = columnNames
            .stream()
            .map { columnName: String ->
                val concept = conceptRow[columnName]
                val sb = StringBuilder("$")
                sb.append(columnName)
                sb.append(" ".repeat(columnsWidth - columnName.length + 1))
                sb.append("| ")
                sb.append(conceptDisplayString(if (concept.isValue) concept.asValue() else concept))
                sb.toString()
            }.collect(Collectors.joining("\n"))

        val sb = StringBuilder(indent(CONTENT_INDENT, content))
        sb.append("\n")
        sb.append(lineDashSeparator(columnsWidth))
        return sb.toString()
    }

//    private fun conceptDocumentDisplayHeader(queryType: QueryType): String {
//        val sb = java.lang.StringBuilder()
//        sb.append(QUERY_COMPILATION_SUCCESS)
//        sb.append("\n")
//
//        if (queryType.isWrite) {
//            sb.append(QUERY_WRITE_SUCCESS)
//            sb.append(". ")
//        }
//
//        assert(
//            !queryType.isSchema // expected to return another type of answer
//        )
//        sb.append(QUERY_STREAMING_DOCUMENTS)
//        sb.append("\n")
//        return sb.toString()
//    }

    private fun indent(indent: String, string: String): String {
        return Arrays.stream(string.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            .map { s: String -> indent + s }
            .collect(Collectors.joining("\n"))
    }

    private fun lineDashSeparator(additionalDashesNum: Int): String {
        return indent(TABLE_INDENT, "-".repeat(TABLE_DASHES + additionalDashesNum))
    }

    private fun conceptDisplayString(concept: Concept): String {
        if (concept.isValue) return valueDisplayString(concept.asValue())

        val sb = java.lang.StringBuilder()
        if (concept.isType) {
            sb.append(typeDisplayString(concept.asType()))
        } else if (concept.isAttribute) {
            sb.append(attributeDisplayString(concept.asAttribute()))
        } else if (concept.isEntity) {
            sb.append(entityDisplayKeyString(concept.asEntity()))
        } else if (concept.isRelation) {
            sb.append(relationDisplayKeyString(concept.asRelation()))
        }

        if (concept.isInstance) {
            sb.append(" ").append(isaDisplayString(concept.asInstance()))
        }

        return sb.toString()
    }

    private fun valueDisplayString(value: Value): String {
        val rawValue: Any
        if (value.isDouble) rawValue = value.double
        else if (value.isDecimal) rawValue = value.decimal
        else if (value.isBoolean) rawValue = value.boolean
        else if (value.isString) rawValue = value.string
        else if (value.isDate) rawValue = value.date
        else if (value.isDatetime) rawValue = value.datetime
        else if (value.isDatetimeTZ) rawValue = value.datetimeTZ
        else if (value.isDuration) rawValue = value.duration
        else if (value.isStruct) rawValue = "Structs are not supported in log output now"
        else throw IllegalArgumentException()
        return rawValue.toString()
    }

    private fun isaDisplayString(instance: Instance): String {
        return "isa ${instance.type.label}"
    }

    private fun entityDisplayKeyString(entity: Entity): String {
        return "iid ${entity.iid}"
    }

    private fun relationDisplayKeyString(relation: Relation): String {
        return "iid ${relation.iid}"
    }

    private fun typeDisplayString(type: Type): String {
        return "type ${type.label}"
    }

    private fun attributeDisplayString(attribute: Attribute): String {
        return attribute.value.toString()
    }

//    private fun colorKeyword(s: String): String {
//        return AttributedString(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)).toAnsi()
//    }
//
//    private fun colorType(s: String): String {
//        return AttributedString(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA)).toAnsi()
//    }
//
//    private fun colorError(s: String): String {
//        return AttributedString(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)).toAnsi()
//    }
//
//    private fun colorJsonKey(s: String): String {
//        return AttributedString(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)).toAnsi()
//    }

//    private fun printType(type: Type): String {
//        var str = TypeQLToken.Constraint.TYPE.toString() + " " + type.label
//        transactionState.transaction?.let {
//            type.getSupertype(it).resolve()?.let {
//                str += " " + TypeQLToken.Constraint.SUB + " " + it.label.scopedName()
//            }
//        }
//        return str
//    }
//
//    private fun printThing(thing: Thing): String {
//        val str = StringBuilder()
//        when (thing) {
//            is Attribute -> str.append(Strings.valueToString(thing.value))
//            else -> str.append(TypeQLToken.Constraint.IID.toString() + " " + thing.asThing().iid)
//        }
//        if (thing is Relation) str.append(" ").append(printRolePlayers(thing.asThing().asRelation()))
//        str.append(" ").append(TypeQLToken.Constraint.ISA).append(" ")
//            .append(thing.asThing().type.label.scopedName())
//        return str.toString()
//    }
//
//    private fun printRolePlayers(relation: Relation): String {
//        val rolePlayers = transactionState.transaction?.let {
//            relation.getPlayers(it).flatMap { (role, players) ->
//                players.map { player -> role.label.name() + ": " + TypeQLToken.Constraint.IID + " " + player.iid }
//            }.stream().collect(Collectors.joining(", "))
//        } ?: " "
//        return "($rolePlayers)"
//    }
//

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
