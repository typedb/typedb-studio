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

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.isTypedEvent
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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Form.TextInput
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.LazyColumn
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Color.fadeable
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.SCROLLBAR_END_PADDING
import com.vaticle.typedb.studio.view.common.theme.Theme.SCROLLBAR_LONG_PADDING
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import com.vaticle.typedb.studio.view.editor.KeyMapper.EditorCommand
import com.vaticle.typedb.studio.view.editor.KeyMapper.GenericCommand
import com.vaticle.typedb.studio.view.editor.KeyMapper.WindowCommand
import java.awt.event.MouseEvent.BUTTON1
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay

@OptIn(ExperimentalTime::class)
object TextEditor {

    private const val LINE_HEIGHT = 1.56f
    private const val DISABLED_CURSOR_OPACITY = 0.6f
    private val LINE_GAP = 2.dp
    private val AREA_PADDING_HOR = 6.dp
    private val TOOLBAR_MAX_WIDTH = 500.dp
    private val TOOLBAR_MIN_WIDTH = 260.dp
    private val TOOLBAR_ROW_HEIGHT = 28.dp
    private val TOOLBAR_BUTTON_AREA_WIDTH = 160.dp
    private val TOOLBAR_BUTTON_HEIGHT = 24.dp
    private val TOOLBAR_BUTTON_SPACING = 4.dp
    private val DEFAULT_FONT_WIDTH = 12.dp
    private val CURSOR_LINE_PADDING = 0.dp
    private val BLINKING_FREQUENCY = Duration.milliseconds(500)

    @Composable
    fun createState(file: File, onClose: () -> Unit): State {
        val font = Theme.typography.code1
        val currentDensity = LocalDensity.current
        val lineHeight = with(currentDensity) { font.fontSize.toDp() * LINE_HEIGHT }
        val clipboard = LocalClipboardManager.current
        val rendering = TextRendering(file.content.size)
        val target = InputTarget(file, lineHeight, AREA_PADDING_HOR, rendering, currentDensity.density)
        val processor = TextProcessor(file, rendering, target, clipboard)
        return State(file, font, rendering, target, processor, onClose)
    }

    class State internal constructor(
        internal val file: File,
        internal val font: TextStyle,
        internal val rendering: TextRendering,
        internal val target: InputTarget,
        internal val processor: TextProcessor,
        internal val onClose: () -> Unit,
    ) {
        internal val contextMenu = ContextMenu.State()
        internal val content: SnapshotStateList<String> get() = file.content
        internal val lineCount: Int get() = content.size
        internal var isFocused by mutableStateOf(true)
        internal val focusReq = FocusRequester()
        internal val lineHeight get() = target.lineHeight
        internal var areaWidth by mutableStateOf(0.dp)
        internal var showFinder by mutableStateOf(false)
        internal var showReplacer by mutableStateOf(false)
        internal var showToolbar
            get() = showFinder || showReplacer
            set(value) {
                showFinder = value
                showReplacer = value
            }
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

        internal fun process(event: KeyEvent): Boolean {
            return when {
                event.isTypedEvent -> processor.insertText(event.awtEvent.keyChar.toString())
                event.type != KeyEventType.KeyDown -> false
                else -> KeyMapper.CURRENT.map(event)?.let {
                    when (it) {
                        is EditorCommand -> processor.process(it)
                        is WindowCommand -> process(it)
                        is GenericCommand -> processor.process(it) || process(it)
                    }
                } ?: false
            }
        }

        private fun process(command: WindowCommand): Boolean {
            when (command) {
                WindowCommand.FIND -> showFinder()
                WindowCommand.REPLACE -> showReplacer()
                WindowCommand.CLOSE -> onClose()
            }
            return true
        }

        private fun process(command: GenericCommand): Boolean {
            return when (command) {
                GenericCommand.ESCAPE -> hideToolbar()
            }
        }

        internal fun showFinder() {
            showFinder = true
            showReplacer = false
        }

        internal fun showReplacer() {
            showReplacer = true
        }

        private fun hideToolbar(): Boolean {
            return if (showToolbar) {
                showToolbar = false
                true
            } else false
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Area(state: State, modifier: Modifier = Modifier) {
        if (state.content.isEmpty()) return
        val density = LocalDensity.current.density
        val fontHeight = with(LocalDensity.current) { (state.lineHeight - LINE_GAP).toSp() * density }
        val fontColor = Theme.colors.onBackground
        val textFont = state.font.copy(color = fontColor, lineHeight = fontHeight)
        val lineNumberFont = state.font.copy(color = fontColor.copy(0.5f), lineHeight = fontHeight)
        var fontWidth by remember { mutableStateOf(DEFAULT_FONT_WIDTH) }

        Box { // We render a number to find out the default width of a digit for the given font
            Text(text = "0", style = lineNumberFont, onTextLayout = { fontWidth = toDP(it.size.width, density) })
            Column {
                if (state.showToolbar) Toolbar(state)
                Row(modifier = modifier.onFocusChanged { state.isFocused = it.isFocused; state.updateStatus() }
                    .focusRequester(state.focusReq).focusable()
                    .onGloballyPositioned { state.density = density }
                    .onKeyEvent { state.process(it) }
                    .onPointerEvent(Move) { state.target.mayUpdateDragSelection(it.awtEvent.x, it.awtEvent.y) }
                    .onPointerEvent(Release) { if (it.awtEvent.button == BUTTON1) state.target.stopDragSelection() }
                    .pointerInput(state) { onPointerInput(state) }
                ) {
                    LineNumberArea(state, lineNumberFont, fontWidth)
                    Separator.Vertical()
                    TextArea(state, textFont, fontWidth)
                }
            }
        }

        LaunchedEffect(state) { state.focusReq.requestFocus() }
    }

    @Composable
    private fun Toolbar(state: State) {
        Column {
            Finder(state)
            if (state.showReplacer) {
                var inputTextWidth by remember { mutableStateOf(0.dp) }
                ToolbarLineSeparator(inputTextWidth)
                Replacer(state) { inputTextWidth = it }
            }
            Separator.Horizontal()
        }
    }

    @Composable
    private fun ToolbarLineSeparator(inputTextWidth: Dp) {
        Spacer(Modifier.height(Separator.WEIGHT).width(inputTextWidth).background(Theme.colors.border))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Finder(state: State) {
        Row(Modifier.height(TOOLBAR_ROW_HEIGHT).width(TOOLBAR_MAX_WIDTH)) {
            TextInput(
                value = "",
                placeholder = Label.FIND,
                onValueChange = {},
                leadingIcon = Icon.Code.MAGNIFYING_GLASS,
                shape = null,
                border = null,
                modifier = Modifier.weight(1f),
                // TODO: figure out how to set min width to TOOLBAR_MIN_WIDTH
            )
            Separator.Vertical()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(TOOLBAR_ROW_HEIGHT).width(TOOLBAR_BUTTON_AREA_WIDTH)
            ) {
                Spacer(Modifier.width(TOOLBAR_BUTTON_SPACING))
                IconButton(Icon.Code.CHEVRON_DOWN, {}, bgColor = Color.Transparent, rounded = false)
                IconButton(Icon.Code.CHEVRON_UP, {}, bgColor = Color.Transparent, rounded = false)
                Spacer(Modifier.width(TOOLBAR_BUTTON_SPACING))
                Text(
                    value = "11 / 23462",
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Replacer(state: State, onResizeInputText: (Dp) -> Unit) {
        Row(Modifier.height(TOOLBAR_ROW_HEIGHT).width(TOOLBAR_MAX_WIDTH)) {
            TextInput(
                value = "",
                placeholder = Label.REPLACE,
                onValueChange = {},
                leadingIcon = Icon.Code.RIGHT_LEFT,
                shape = null,
                border = null,
                modifier = Modifier.weight(1f).onSizeChanged { onResizeInputText(toDP(it.width, state.density)) },
                // TODO: figure out how to set min width to TOOLBAR_MIN_WIDTH
            )
            Separator.Vertical()
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.height(TOOLBAR_ROW_HEIGHT).width(TOOLBAR_BUTTON_AREA_WIDTH)
            ) {
                Spacer(Modifier.width(TOOLBAR_BUTTON_SPACING))
                Form.TextButton(Label.REPLACE, {}, Modifier.height(TOOLBAR_BUTTON_HEIGHT))
                Spacer(Modifier.width(TOOLBAR_BUTTON_SPACING))
                Form.TextButton(Label.REPLACE_ALL, {}, Modifier.height(TOOLBAR_BUTTON_HEIGHT))
            }
        }
    }

    @Composable
    private fun LineNumberArea(state: State, font: TextStyle, fontWidth: Dp) {
        val maxDigits = ceil(log10(state.lineCount + 1.0)).toInt()
        val minWidth = fontWidth * maxDigits + AREA_PADDING_HOR * 2 + 2.dp
        val lazyColumnState: LazyColumn.State<Int> = LazyColumn.createState(
            items = (0 until state.lineCount).map { it },
            scroller = state.target.verScroller
        )
        LazyColumn.Area(state = lazyColumnState) { index, _ -> LineNumber(state, index, font, minWidth) }
    }

    @Composable
    private fun LineNumber(state: State, index: Int, font: TextStyle, minWidth: Dp) {
        val isCursor = state.target.cursor.row == index
        val isSelected = state.target.selection?.let { it.min.row <= index && it.max.row >= index } ?: false
        val bgColor = if (isCursor || isSelected) Theme.colors.primary else Theme.colors.background
        Box(
            contentAlignment = Alignment.TopEnd,
            modifier = Modifier.background(bgColor)
                .defaultMinSize(minWidth = minWidth)
                .height(state.lineHeight)
                .padding(horizontal = AREA_PADDING_HOR)
        ) { Text(text = (index + 1).toString(), style = font) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun TextArea(state: State, font: TextStyle, fontWidth: Dp) {
        val lazyColumnState: LazyColumn.State<String> = LazyColumn.createState(state.content, state.target.verScroller)

        Box(modifier = Modifier.onGloballyPositioned {
            state.updateAreaWidth(it.size.width)
            state.target.updateTextArea(it.boundsInWindow())
        }) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Theme.colors.background2)
                    .horizontalScroll(state.target.horScroller)
            ) {
                ContextMenu.Popup(state.contextMenu) { contextMenuFn(state) }
                LazyColumn.Area(state = lazyColumnState) { index, text ->
                    TextLine(state, index, text, font, fontWidth)
                }
            }
            VerticalScrollbar(
                adapter = state.target.verScroller,
                modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd)
                    .padding(SCROLLBAR_LONG_PADDING, SCROLLBAR_END_PADDING)
            )
            HorizontalScrollbar(
                adapter = state.target.horScrollerAdapter,
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .padding(SCROLLBAR_END_PADDING, SCROLLBAR_LONG_PADDING)
            )
        }
    }

    @Composable
    private fun TextLine(state: State, index: Int, text: String, font: TextStyle, fontWidth: Dp) {
        val cursor = state.target.cursor
        val selection = state.target.selection
        val bgColor = when {
            cursor.row == index && selection == null -> Theme.colors.primary
            else -> Theme.colors.background2
        }
        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier.background(bgColor)
                .defaultMinSize(minWidth = max(state.target.textWidth, state.areaWidth))
                .height(state.lineHeight)
                .padding(horizontal = AREA_PADDING_HOR)
        ) {
            val isRendered = state.rendering.isRendered(index, state.processor.version)
            if (selection != null && selection.min.row <= index && selection.max.row >= index) {
                if (!isRendered) Selection(state, index, null, text.length, fontWidth)
                else Selection(state, index, state.rendering.get(index), text.length, fontWidth)
            }
            Text(
                text = AnnotatedString(text), style = font,
                modifier = Modifier.onSizeChanged { state.target.mayIncreaseTextWidth(it.width) },
                onTextLayout = { state.rendering.set(index, it, state.processor.version) }
            )
            if (cursor.row == index) {
                if (!isRendered) Cursor(state, text, null, font, fontWidth)
                else Cursor(state, text, state.rendering.get(index), font, fontWidth)
            }
        }
    }

    @Composable
    private fun Selection(state: State, index: Int, textLayout: TextLayoutResult?, length: Int, fontWidth: Dp) {
        val selection = state.target.selection
        assert(selection != null && selection.min.row <= index && selection.max.row >= index)
        val start = when {
            selection!!.min.row < index -> 0
            else -> selection.min.col
        }
        val end = when {
            selection.max.row > index -> state.content[index].length
            else -> selection.max.col
        }
        var startPos = textLayout?.let { toDP(it.getCursorRect(start).left, state.density) } ?: (fontWidth * start)
        var endPos = textLayout?.let {
            toDP(it.getCursorRect(end.coerceAtMost(it.getLineEnd(0))).right, state.density)
        } ?: (fontWidth * end)
        if (selection.min.row < index) startPos -= AREA_PADDING_HOR
        if (selection.max.row > index && length > 0) endPos += AREA_PADDING_HOR
        val color = Theme.colors.tertiary.copy(Theme.SELECTION_ALPHA)
        Box(Modifier.offset(x = startPos).width(endPos - startPos).height(state.lineHeight).background(color))
    }

    @OptIn(ExperimentalTime::class)
    @Composable
    private fun Cursor(state: State, text: String, textLayout: TextLayoutResult?, font: TextStyle, fontWidth: Dp) {
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
            Box(
                modifier = Modifier.offset(x = offsetX, y = CURSOR_LINE_PADDING)
                    .width(width).height(state.lineHeight - CURSOR_LINE_PADDING * 2)
                    .background(fadeable(Theme.colors.secondary, !state.isFocused, DISABLED_CURSOR_OPACITY))
            ) {
                Text(
                    text.getOrNull(cursor.col)?.toString() ?: "",
                    Modifier.offset(y = -CURSOR_LINE_PADDING),
                    style = font.copy(Theme.colors.background2)
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
                state.target.startDragSelection()
                state.target.updateCursor(it.x, it.y, it.isShiftDown)
            },
            onDoublePrimaryPressed = { state.target.selectWord() },
            onTriplePrimaryPressed = { state.target.selectLine() },
            onSecondaryClick = { state.target.updateCursorIfOutOfSelection(it.x, it.y) }
        )
    }

    private fun contextMenuFn(state: State): List<List<ContextMenu.Item>> { // TODO
        val selection = state.target.selection
        val modKey = if (Property.OS.Current == Property.OS.MACOS) Label.CMD else Label.CTRL
        val hasClipboard = !state.processor.clipboard.getText().isNullOrBlank()
        return listOf(
            listOf(
                ContextMenu.Item(Label.CUT, Icon.Code.CUT, "$modKey + X", selection != null) {
                    state.processor.cut()
                },
                ContextMenu.Item(Label.COPY, Icon.Code.COPY, "$modKey + C", selection != null) {
                    state.processor.copy()
                },
                ContextMenu.Item(Label.PASTE, Icon.Code.PASTE, "$modKey + V", hasClipboard) {
                    state.processor.paste()
                }
            ),
            listOf(
                ContextMenu.Item(Label.FIND, Icon.Code.MAGNIFYING_GLASS, "$modKey + F") {
                    state.showFinder()
                },
                ContextMenu.Item(Label.REPLACE, Icon.Code.RIGHT_LEFT, "$modKey + R") {
                    state.showReplacer()
                }
            ),
            listOf(
                ContextMenu.Item(Label.SAVE, Icon.Code.FLOPPY_DISK, "$modKey + S", false) { }, // TODO
                ContextMenu.Item(Label.CLOSE, Icon.Code.XMARK, "$modKey + W") {
                    state.onClose()
                },
            )
        )
    }
}