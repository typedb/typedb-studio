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

package com.vaticle.typedb.studio.view.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FILE_CONTENT_CHANGED_ON_DISK
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FILE_PERMISSION_CHANGED_ON_DISK
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Scrollbar
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Color.fadeable
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.editor.InputTarget.Selection
import com.vaticle.typedb.studio.view.editor.TextProcessor.Companion.normaliseWhiteSpace
import com.vaticle.typedb.studio.view.highlighter.SyntaxHighlighter.highlight
import java.awt.event.MouseEvent.BUTTON1
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import mu.KotlinLogging

@OptIn(ExperimentalTime::class)
object TextEditor {

    private const val LINE_HEIGHT = 1.56f
    private const val DISABLED_CURSOR_OPACITY = 0.6f
    private val LINE_GAP = 4.dp
    private val AREA_PADDING_HOR = 6.dp
    private val RIGHT_PADDING = 32.dp
    private val DEFAULT_FONT_WIDTH = 7.dp
    private val CURSOR_LINE_PADDING = 0.dp
    private val BLINKING_FREQUENCY = Duration.milliseconds(500)
    private val END_OF_FILE_SPACE = 100.dp
    private val MAX_LINE_MIN_WIDTH: Dp = 100_000.dp // we need this cause Compose can't render components too large
    private val LOGGER = KotlinLogging.logger {}

    @Composable
    fun createState(file: File, bottomSpace: Dp = END_OF_FILE_SPACE): State {
        val editor = createState(
            content = SnapshotStateList<AnnotatedString>().apply {
                val content = file.readContent().map { normaliseWhiteSpace(it) }
                addAll(highlight(content, file.fileType))
            },
            bottomSpace = bottomSpace,
            processorFn = when {
                !file.isWritable -> { _, _, _, _ -> TextProcessor.ReadOnly(file) }
                else -> { content, rendering, finder, target ->
                    TextProcessor.Writable(
                        file = file,
                        content = content,
                        rendering = rendering,
                        finder = finder,
                        target = target,
                        onChangeStart = { file.isChanged() },
                        onChangeEnd = { file.content(it) }
                    )
                }
            }
        )
        file.beforeRun { editor.processor.drainChanges() }
        file.beforeSave { editor.processor.drainChanges() }
        file.beforeClose { editor.processor.drainChanges() }
        file.onClose { editor.clearStatus() }
        onChangeFromDisk(file, editor)
        return editor
    }

    @Composable
    fun createState(bottomSpace: Dp): State {
        return createState(
            content = SnapshotStateList(),
            bottomSpace = bottomSpace
        ) { _, _, _, _ -> TextProcessor.ReadOnly() }
    }

    @Composable
    private fun createState(
        content: SnapshotStateList<AnnotatedString>,
        bottomSpace: Dp,
        processorFn: (
            content: SnapshotStateList<AnnotatedString>,
            rendering: TextRendering, finder: TextFinder, target: InputTarget
        ) -> TextProcessor
    ): State {
        val font = Theme.typography.code1
        val currentDensity = LocalDensity.current
        val lineHeight = with(currentDensity) { font.fontSize.toDp() * LINE_HEIGHT }
        val clipboard = LocalClipboardManager.current
        val rendering = TextRendering(content.size)
        val finder = TextFinder(content)
        val target = InputTarget(content, rendering, AREA_PADDING_HOR, lineHeight, bottomSpace, currentDensity.density)
        val processor = processorFn(content, rendering, finder, target)
        val toolbar = TextToolbar.State(finder, target, processor)
        val handler = EventHandler(target, toolbar, clipboard, processor)
        return State(content, font, rendering, finder, target, toolbar, handler, processor)
    }

    private fun onChangeFromDisk(file: File, editor: State) {
        fun reinitialiseContent(file: File) {
            val newContent = file.readContent().map { normaliseWhiteSpace(it) }
            editor.content.clear()
            editor.content.addAll(highlight(newContent, file.fileType))
            editor.rendering.reinitialize(editor.content.size)
        }

        file.onDiskChangeContent {
            reinitialiseContent(it)
            editor.processor.reset()
            GlobalState.notification.userWarning(LOGGER, FILE_CONTENT_CHANGED_ON_DISK, it.path)
        }

        file.onDiskChangePermission {
            reinitialiseContent(it)
            val newProcessor = when {
                !it.isWritable -> TextProcessor.ReadOnly(it)
                else -> TextProcessor.Writable(
                    file = it,
                    content = editor.content,
                    rendering = editor.rendering,
                    finder = editor.finder,
                    target = editor.target,
                    onChangeStart = { it.isChanged() },
                    onChangeEnd = { lines -> it.content(lines) }
                )
            }
            editor.toolbar.processor = newProcessor
            editor.handler.processor = newProcessor
            editor.processor = newProcessor
            GlobalState.notification.userWarning(LOGGER, FILE_PERMISSION_CHANGED_ON_DISK, it.path)
        }
    }

    class State internal constructor(
        val content: SnapshotStateList<AnnotatedString>,
        internal val font: TextStyle,
        internal val rendering: TextRendering,
        internal val finder: TextFinder,
        internal val target: InputTarget,
        internal val toolbar: TextToolbar.State,
        internal val handler: EventHandler,
        initProcessor: TextProcessor,
    ) {
        val focusReq = FocusRequester()
        var stickToBottom
            get() = target.stickToBottom
            set(value) {
                target.stickToBottom = value
            }
        internal val contextMenu = ContextMenu.State()
        internal var areaWidth by mutableStateOf(0.dp)
        internal var isFocused by mutableStateOf(true)
        internal var processor by mutableStateOf(initProcessor)
        internal val lineCount get() = content.size
        internal val lineHeight get() = target.lineHeight
        internal val isWritable get() = processor.isWritable
        internal val showToolbar get() = toolbar.showToolbar

        internal var density: Float
            get() = target.density
            set(value) {
                target.density = value
            }

        internal fun updateAreaWidth(newWidth: Int) {
            areaWidth = toDP(newWidth, density)
        }

        internal fun updateStatus() {
            target.updateStatus()
        }

        internal fun clearStatus() {
            target.clearStatus()
        }

        fun updateFile(file: File) {
            processor.updateFile(file)
        }

        fun jumpToTop() {
            target.verScroller.scrollToTop()
            target.moveCursorToStart(isSelecting = false, mayScroll = false)
        }

        fun onScrollToBottom(function: () -> Unit) {
            target.verScroller.onScrollToBottom(function)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Layout(state: State, modifier: Modifier = Modifier, showLine: Boolean = true, onScroll: () -> Unit = {}) {
        if (state.content.isEmpty()) return
        val textScale = GlobalState.editor.scale
        val density = LocalDensity.current.density
        val fontSize = ((state.font.fontSize.value * textScale * 100).roundToInt() / 100f).sp
        val fontStyle = state.font.copy(color = Theme.colors.onBackground, fontSize = fontSize)
        var fontWidthUnscaled by remember { mutableStateOf(DEFAULT_FONT_WIDTH) }
        val fontWidth = fontWidthUnscaled * textScale
        val lineGap = LINE_GAP * textScale

        Box { // We render a number to find out the default width of a digit for the given font
            Text("0", style = state.font, onTextLayout = { fontWidthUnscaled = toDP(it.size.width, density) })
            Column {
                if (state.showToolbar) {
                    TextToolbar.Area(state.toolbar, Modifier.onPreviewKeyEvent { state.handler.handleToolbarEvent(it) })
                }
                Row(modifier = modifier.onFocusChanged { state.isFocused = it.isFocused }
                    .focusRequester(state.focusReq).focusable()
                    .onGloballyPositioned { state.density = density }
                    .onKeyEvent { state.handler.handleEditorEvent(it) }
                    .onPointerEvent(Move) { state.target.dragSelection(it.awtEvent.x, it.awtEvent.y) }
                    .onPointerEvent(Release) { if (it.awtEvent.button == BUTTON1) state.target.stopDragSelection() }
                    .pointerInput(state) { onPointerInput(state) }
                ) {
                    if (showLine) {
                        LineNumberArea(state, fontStyle, fontWidth, lineGap, onScroll)
                        Separator.Vertical()
                    }
                    TextArea(state, fontStyle, fontWidth, lineGap, showLine, onScroll)
                }
            }
        }

        LaunchedEffect(state, state.showToolbar) {
            if (!state.showToolbar && state.isWritable) {
                state.updateStatus()
                state.focusReq.requestFocus()
                state.isFocused = true
            } else state.isFocused = false
        }
    }

    @Composable
    private fun LineNumberArea(state: State, font: TextStyle, fontWidth: Dp, lineGap: Dp, onScroll: () -> Unit) {
        val maxDigits = ceil(log10(state.lineCount + 1.0)).toInt()
        val minWidth = fontWidth * maxDigits + AREA_PADDING_HOR * 2 + 2.dp
        val lazyColumnState: LazyLines.State<Int> = LazyLines.createState(
            lines = (0 until state.lineCount).map { it },
            scroller = state.target.verScroller
        )
        LazyLines.Area(state = lazyColumnState, onScroll = onScroll) { index, _ ->
            LineNumber(state, index, font, minWidth, lineGap)
        }
    }

    @Composable
    private fun LineNumber(state: State, index: Int, font: TextStyle, minWidth: Dp, lineGap: Dp) {
        val isCursor = state.target.cursor.row == index
        val isSelected = state.target.selection?.let { it.min.row <= index && it.max.row >= index } ?: false
        val bgColor = if (isCursor || isSelected) Theme.colors.primary else Theme.colors.background1
        val fontAlpha = if (isCursor || isSelected) 0.9f else 0.5f
        Column(
            modifier = Modifier.background(bgColor)
                .defaultMinSize(minWidth = minWidth)
                .height(state.lineHeight)
                .padding(horizontal = AREA_PADDING_HOR)
        ) {
            Spacer(Modifier.height(lineGap))
            Text(text = (index + 1).toString(), style = font.copy(font.color.copy(alpha = fontAlpha)))
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun TextArea(
        state: State, font: TextStyle, fontWidth: Dp, lineGap: Dp, showLine: Boolean, onScroll: () -> Unit
    ) {
        val lazyColumnState = LazyLines.createState(state.content, state.target.verScroller)
        Box(modifier = Modifier.onGloballyPositioned {
            state.updateAreaWidth(it.size.width)
            state.target.updateTextArea(it.boundsInWindow())
        }) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Theme.colors.background0)
                    .horizontalScroll(state.target.horScroller)
                    .pointerHoverIcon(PointerIconDefaults.Text)
            ) {
                ContextMenu.Popup(state.contextMenu) { state.handler.contextMenuFn() }
                LazyLines.Area(state = lazyColumnState, onScroll = onScroll) { index, text ->
                    TextLine(state, index, text, font, fontWidth, lineGap, showLine)
                }
            }
            Scrollbar.Vertical(state.target.verScroller, Modifier.align(Alignment.CenterEnd))
            Scrollbar.Horizontal(state.target.horScrollerAdapter, Modifier.align(Alignment.BottomCenter))
        }
    }

    @Composable
    private fun TextLine(
        state: State, index: Int, text: AnnotatedString, font: TextStyle, fontWidth: Dp, lineGap: Dp, showLine: Boolean
    ) {
        val cursor = state.target.cursor
        val selection = state.target.selection
        val bgColor = when {
            showLine && cursor.row == index && selection == null -> Theme.colors.primary
            else -> Theme.colors.background0
        }
        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier.background(bgColor)
                .defaultMinSize(minWidth = state.target.textWidth.coerceIn(state.areaWidth, MAX_LINE_MIN_WIDTH))
                .height(state.lineHeight)
                .padding(horizontal = AREA_PADDING_HOR)
        ) {
            val isRenderedUpToDate = state.rendering.hasVersion(index, state.processor.version)
            val textLayout = if (isRenderedUpToDate) state.rendering.get(index) else null
            val findColor = Theme.colors.quaternary2.copy(Theme.FIND_SELECTION_ALPHA)
            state.finder.matches(index).forEach {
                Selection(state, it, index, textLayout, findColor, text.length, fontWidth)
            }
            if (selection != null && selection.min.row <= index && selection.max.row >= index) {
                val color = Theme.colors.tertiary.copy(Theme.TARGET_SELECTION_ALPHA)
                Selection(state, selection, index, textLayout, color, text.length, fontWidth)
            }
            Row {
                Column {
                    Spacer(Modifier.height(lineGap))
                    Text(
                        text = text, style = font,
                        modifier = Modifier.onSizeChanged { state.target.mayIncreaseTextWidth(it.width) },
                        onTextLayout = { state.rendering.set(index, it, state.processor.version) }
                    )
                }
                Spacer(Modifier.width(RIGHT_PADDING))
            }
            if (cursor.row == index) Cursor(state, text, textLayout, font, fontWidth, lineGap)
        }
    }

    @Composable
    private fun Selection(
        state: State, selection: Selection, index: Int,
        textLayout: TextLayoutResult?, color: Color,
        length: Int, fontWidth: Dp
    ) {
        assert(selection.min.row <= index && selection.max.row >= index)
        val start = if (selection.min.row < index) 0 else selection.min.col
        val end = if (selection.max.row > index) state.content[index].length else selection.max.col
        var startPos = textLayout?.let { toDP(it.getCursorRect(start).left, state.density) } ?: (fontWidth * start)
        var endPos = textLayout?.let {
            toDP(it.getCursorRect(end.coerceAtMost(it.getLineEnd(0))).right, state.density)
        } ?: (fontWidth * end)
        if (selection.min.row < index) startPos -= AREA_PADDING_HOR
        if (selection.max.row > index && length > 0) endPos += AREA_PADDING_HOR
        Box(Modifier.offset(x = startPos).width(endPos - startPos).height(state.lineHeight).background(color))
    }

    @OptIn(ExperimentalTime::class)
    @Composable
    private fun Cursor(
        state: State, text: AnnotatedString, textLayout: TextLayoutResult?, font: TextStyle, fontWidth: Dp, lineGap: Dp
    ) {
        val cursor = state.target.cursor
        var visible by remember { mutableStateOf(true) }
        val offsetX = textLayout?.let {
            toDP(it.getCursorRect(cursor.col.coerceAtMost(it.getLineEnd(0))).left, state.density)
        } ?: (fontWidth * cursor.col)
        val width = when {
            cursor.col >= text.length -> fontWidth
            else -> textLayout?.let {
                toDP(it.getBoundingBox(cursor.col).width, state.density)
            } ?: fontWidth
        }
        if (visible || !state.isFocused) {
            Column(
                modifier = Modifier.offset(x = offsetX, y = CURSOR_LINE_PADDING)
                    .width(width).height(state.lineHeight - CURSOR_LINE_PADDING * 2)
                    .background(fadeable(Theme.colors.secondary, !state.isFocused, DISABLED_CURSOR_OPACITY))
            ) {
                Spacer(Modifier.height(lineGap))
                Text(
                    text = AnnotatedString.Builder(
                        if (cursor.col >= text.length) AnnotatedString("")
                        else text.subSequence(cursor.col, cursor.col + 1)
                    ).also { it.addStyle(SpanStyle(Theme.colors.background0), 0, 1) }.toAnnotatedString(),
                    modifier = Modifier.offset(y = -CURSOR_LINE_PADDING),
                    style = font
                )
            }
        }
        if (state.isFocused) LaunchedEffect(cursor) {
            visible = true
            while (true) {
                delay(BLINKING_FREQUENCY)
                visible = !visible
            }
        }
    }

    private suspend fun PointerInputScope.onPointerInput(state: State) {
        state.contextMenu.onPointerInput(
            pointerInputScope = this,
            onSinglePrimaryPressed = {
                state.focusReq.requestFocus()
                state.target.mayUpdateCursor(it.x, it.y, it.isShiftDown)
            },
            onDoublePrimaryPressed = { state.target.maySelectWord(it.x) },
            onTriplePrimaryPressed = { state.target.maySelectLine(it.x) },
            onSecondaryClick = { state.target.updateCursorIfOutOfSelection(it.x, it.y) }
        )
    }
}