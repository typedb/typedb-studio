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

package com.vaticle.typedb.studio.view.page

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.page.Pageable
import com.vaticle.typedb.studio.view.common.KeyMapper
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object PageArea {

    val MIN_WIDTH = 300.dp
    private val TAB_SPACING = 8.dp
    private val TAB_HEIGHT = 28.dp
    private val TAB_UNDERLINE_HEIGHT = 2.dp
    private val TAB_SCROLL_DELTA = 200.dp
    private val ICON_SIZE = 10.sp

    internal class AreaState(private val coroutineScope: CoroutineScope) {
        var density: Float by mutableStateOf(0f)
        val tabsScroller = ScrollState(0)
        var tabsScrollTo: Dp? by mutableStateOf(null)
        var tabsRowMaxWidth by mutableStateOf(4096.dp)
        val cachedOpenedPages: MutableMap<Pageable, Page> = mutableMapOf()
        var cachedActivePage: Pageable? by mutableStateOf(null)

        fun handleKeyEvent(event: KeyEvent): Boolean {
            return if (event.type == KeyEventType.KeyUp) false
            else KeyMapper.CURRENT.map(event)?.let { execute(it) } ?: false
        }

        private fun execute(command: KeyMapper.Command): Boolean {
            return when (command) {
                KeyMapper.Command.NEW_PAGE -> createAndOpenNewFile()
                KeyMapper.Command.CLOSE -> closeActivePage()
                else -> false
            }
        }

        fun initTab(page: Page, rawWidth: Int) {
            if (GlobalState.page.activePage == cachedActivePage) return
            val newTabSize = toDP(rawWidth, density) + Separator.WEIGHT
            if (newTabSize != page.tabSize) page.tabSize = newTabSize
            if (GlobalState.page.activePage == page.state) {
                var start = 0.dp
                var found = false
                cachedOpenedPages.values.forEach { if (it != page && !found) start += it.tabSize else found = true }
                val end = start + page.tabSize
                val scrollerPos = toDP(tabsScroller.value, density)
                if (start + 5.dp < scrollerPos) tabsScrollTo = start
                else if (end - 5.dp > scrollerPos + tabsRowMaxWidth) tabsScrollTo = end - tabsRowMaxWidth
                cachedActivePage = GlobalState.page.activePage
            }
        }

        internal fun scrollTabsBy(dp: Dp) {
            val pos = tabsScroller.value + (dp.value * density).toInt()
            coroutineScope.launch { tabsScroller.animateScrollTo(pos) }
        }

        internal fun createAndOpenNewFile(): Boolean {
            GlobalState.project.tryCreateUntitledFile()?.let { GlobalState.page.open(it) }
            return true
        }

        private fun closeActivePage(): Boolean {
            return GlobalState.page.activePage?.let { close(it) } ?: false
        }

        internal fun removeCache(pageable: Pageable) {
            cachedOpenedPages.remove(pageable)
            if (cachedActivePage == pageable) cachedActivePage = null
        }

        internal fun close(pageable: Pageable): Boolean {
            pageable.execBeforeClose()
            fun closeFn() {
                removeCache(pageable)
                GlobalState.page.close(pageable)
                if (pageable.isUnsavedFile) pageable.delete()
            }
            if (pageable.isUnsaved) {
                GlobalState.confirmation.submit(
                    title = Label.SAVE_OR_DELETE,
                    message = Sentence.SAVE_OR_DELETE_FILE,
                    confirmLabel = Label.SAVE,
                    cancelLabel = Label.DELETE,
                    onCancel = { closeFn() },
                    onConfirm = { pageable.save() },
                )
            } else closeFn()
            return true
        }

        internal fun contextMenuFn(page: Pageable): List<List<ContextMenu.Item>> {
            val modKey = if (Property.OS.Current == Property.OS.MACOS) Label.CMD else Label.CTRL
            val pageMgr = GlobalState.page
            return listOf(
                listOf(
                    ContextMenu.Item(Label.SAVE, Icon.Code.FLOPPY_DISK, "$modKey + S", page.isUnsaved) {
                        pageMgr.saveAndReopen(page)
                    },
                    ContextMenu.Item(Label.CLOSE, Icon.Code.XMARK, "$modKey + W") { close(page) }
                )
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Layout() {
        val density = LocalDensity.current.density
        val coroutineScope = rememberCoroutineScope()
        val state = remember { AreaState(coroutineScope) }
        state.density = density
        val focusReq = FocusRequester()
        fun mayRequestFocus() {
            if (GlobalState.page.openedPages.isEmpty()) focusReq.requestFocus()
        }
        state.cachedOpenedPages.values.forEach { it.resetFocus() }
        Column(
            modifier = Modifier.fillMaxSize().focusRequester(focusReq).focusable()
                .onPointerEvent(Press) { if (it.buttons.isPrimaryPressed) mayRequestFocus() }
                .onKeyEvent { state.handleKeyEvent(it) }
        ) {
            TabArea(state)
            Separator.Horizontal()
            PageLayout(state)
        }
        LaunchedEffect(focusReq) { mayRequestFocus() }
    }

    @Composable
    private fun PageLayout(state: AreaState) {
        GlobalState.page.activePage?.let { state.cachedOpenedPages[it]?.Layout() }
    }

    @Composable
    private fun TabArea(state: AreaState) {
        val scrollState = state.tabsScroller
        fun updateTabsRowMaxWidth(rawAreaWidth: Int) {
            state.tabsRowMaxWidth = toDP(rawAreaWidth, state.density) - TAB_HEIGHT * 3
        }
        Row(Modifier.fillMaxWidth().height(TAB_HEIGHT).onSizeChanged { updateTabsRowMaxWidth(it.width) }) {
            if (scrollState.maxValue > 0) {
                PreviousTabsButton(state)
                Separator.Vertical()
            }
            Row(Modifier.widthIn(max = state.tabsRowMaxWidth).height(TAB_HEIGHT).horizontalScroll(scrollState)) {
                GlobalState.page.openedPages.forEach { pageState ->
                    Tab(state, state.cachedOpenedPages.getOrPut(pageState) {
                        pageState.onClose { state.removeCache(pageState) }
                        Page.of(pageState)
                    })
                }
            }
            if (scrollState.maxValue > 0) {
                if (scrollState.value < scrollState.maxValue) Separator.Vertical()
                NextTabsButton(state)
                Separator.Vertical()
            }
            NewPageButton(state)
            Separator.Vertical()
        }
        LaunchedEffect(state.tabsScrollTo) {
            state.tabsScrollTo?.let {
                scrollState.scrollTo((it.value * state.density).toInt())
                state.tabsScrollTo = null
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Tab(state: AreaState, page: Page) {
        val isActive = GlobalState.page.isActive(page.state)
        val contextMenu = remember { ContextMenu.State() }
        val bgColor = if (isActive) Theme.colors.primary else Theme.colors.background
        val height = if (isActive) TAB_HEIGHT - TAB_UNDERLINE_HEIGHT else TAB_HEIGHT
        var width by remember { mutableStateOf(0.dp) }

        Box {
            ContextMenu.Popup(contextMenu) { state.contextMenuFn(page.state) }
            Column(Modifier.onSizeChanged { state.initTab(page, it.width) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(height)
                        .background(color = bgColor)
                        .pointerHoverIcon(PointerIconDefaults.Hand)
                        .pointerInput(state, page) { onPointerInput(contextMenu, page) }
                        .onSizeChanged { width = toDP(it.width, state.density) }
                ) {
                    Spacer(modifier = Modifier.width(TAB_SPACING))
                    Icon.Render(icon = page.icon.code, color = page.icon.color(), size = ICON_SIZE)
                    Spacer(modifier = Modifier.width(TAB_SPACING))
                    Text(value = tabTitle(page))
                    IconButton(
                        icon = Icon.Code.XMARK,
                        onClick = { state.close(page.state) },
                        modifier = Modifier.size(TAB_HEIGHT),
                        bgColor = Color.Transparent,
                        rounded = false,
                    )
                }
                if (isActive) Separator.Horizontal(TAB_UNDERLINE_HEIGHT, Theme.colors.secondary, Modifier.width(width))
            }
            Separator.Vertical()
        }
    }

    private suspend fun PointerInputScope.onPointerInput(contextMenu: ContextMenu.State, page: Page) {
        contextMenu.onPointerInput(
            pointerInputScope = this,
            onSinglePrimaryPressed = { GlobalState.page.activate(page.state) }
        )
    }

    @Composable
    private fun PreviousTabsButton(state: AreaState) {
        IconButton(
            icon = Icon.Code.CARET_LEFT,
            onClick = { state.scrollTabsBy(-TAB_SCROLL_DELTA) },
            modifier = Modifier.size(TAB_HEIGHT),
            bgColor = Color.Transparent,
            rounded = false,
            enabled = state.tabsScroller.value > 0
        )
    }

    @Composable
    private fun NextTabsButton(state: AreaState) {
        IconButton(
            icon = Icon.Code.CARET_RIGHT,
            onClick = { state.scrollTabsBy(TAB_SCROLL_DELTA) },
            modifier = Modifier.size(TAB_HEIGHT),
            bgColor = Color.Transparent,
            rounded = false,
            enabled = state.tabsScroller.value < state.tabsScroller.maxValue
        )
    }

    @Composable
    private fun NewPageButton(state: AreaState) {
        IconButton(
            icon = Icon.Code.PLUS,
            onClick = { state.createAndOpenNewFile() },
            modifier = Modifier.size(TAB_HEIGHT),
            bgColor = Color.Transparent,
            rounded = false,
            enabled = GlobalState.project.current != null
        )
    }

    @Composable
    private fun tabTitle(page: Page): AnnotatedString {
        return if (page.isWritable) {
            val changeIndicator = " *"
            AnnotatedString(page.name) + when {
                page.state.isUnsaved -> AnnotatedString(changeIndicator)
                else -> AnnotatedString(changeIndicator, SpanStyle(color = Color.Transparent))
            }
        } else {
            val builder = AnnotatedString.Builder()
            val style = SpanStyle(color = Theme.colors.onPrimary.copy(alpha = 0.6f))
            builder.append(page.name)
            builder.pushStyle(style)
            builder.append(" -- (${Label.READ_ONLY.lowercase()})")
            builder.pop()
            builder.toAnnotatedString()
        }
    }
}
