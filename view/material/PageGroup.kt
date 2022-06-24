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

package com.vaticle.typedb.studio.view.material

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.page.Pageable
import com.vaticle.typedb.studio.view.common.KeyMapper
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.material.Form.IconButtonArg

object PageGroup {

    val MIN_WIDTH = 300.dp

    internal class State {

        private val openedPages: MutableMap<Pageable, Page> = mutableMapOf()
        internal val tabsState = Tabs.Horizontal.State<Pageable>()

        fun handleKeyEvent(event: KeyEvent, onNewPage: () -> Unit): Boolean {
            return if (event.type == KeyEventType.KeyUp) false
            else KeyMapper.CURRENT.map(event)?.let { execute(it, onNewPage) } ?: false
        }

        private fun execute(command: KeyMapper.Command, onNewPage: () -> Unit): Boolean {
            return when (command) {
                KeyMapper.Command.SAVE -> saveActivePage()
                KeyMapper.Command.CLOSE -> closeActivePage()
                KeyMapper.Command.CTRL_TAB -> showNextPage()
                KeyMapper.Command.CTRL_TAB_SHIFT -> showPreviousPage()
                KeyMapper.Command.NEW_PAGE -> {
                    onNewPage()
                    true
                }
                else -> false
            }
        }

        @Composable
        internal fun openedPage(pageable: Pageable, createPageFn: @Composable (Pageable) -> Page): Page {
            return openedPages.getOrPut(pageable) {
                val page = createPageFn(pageable)
                pageable.onClose { openedPages.remove(it) }
                pageable.onReopen {
                    page.updatePageable(it)
                    openedPages[it] = page
                }
                page
            }
        }

        private fun saveActivePage(): Boolean {
            StudioState.pages.active?.initiateSave()
            return true
        }

        private fun showNextPage(): Boolean {
            StudioState.pages.next.activate()
            return true
        }

        private fun showPreviousPage(): Boolean {
            StudioState.pages.previous.activate()
            return true
        }

        private fun closeActivePage(): Boolean {
            return StudioState.pages.active?.let { close(it) } ?: false
        }

        internal fun close(pageable: Pageable, stopRunner: Boolean = false): Boolean {
            pageable.execBeforeClose()
            fun closeFn() {
                openedPages.remove(pageable)
                pageable.close()
                if (pageable.isUnsavedPageable) pageable.delete()
            }
            if (pageable.isRunnable && pageable.asRunnable().isRunning && !stopRunner) {
                StudioState.confirmation.submit(
                    title = Label.QUERY_IS_RUNNING,
                    message = Sentence.STOP_RUNNING_QUERY_BEFORE_CLOSING_PAGE_DESCRIPTION,
                    cancelLabel = Label.OK,
                )
            } else if (pageable.needSaving) {
                StudioState.confirmation.submit(
                    title = Label.SAVE_OR_DELETE,
                    message = Sentence.SAVE_OR_DELETE_FILE,
                    confirmLabel = Label.SAVE,
                    rejectLabel = Label.DELETE,
                    onReject = { closeFn() },
                    onConfirm = { pageable.initiateSave(reopen = false) }
                )
            } else closeFn()
            return true
        }

        internal fun contextMenuFn(pageable: Pageable): List<List<ContextMenu.Item>> {
            return listOf(
                listOf(
                    saveMenuItem(pageable),
                    closeMenuItem(pageable)
                )
            )
        }

        private fun closeMenuItem(pageable: Pageable) = ContextMenu.Item(
            label = Label.CLOSE,
            icon = Icon.Code.XMARK,
            info = "${KeyMapper.CURRENT.modKey} + W"
        ) { close(pageable) }

        private fun saveMenuItem(pageable: Pageable) = ContextMenu.Item(
            label = Label.SAVE,
            icon = Icon.Code.FLOPPY_DISK,
            info = "${KeyMapper.CURRENT.modKey} + S",
            enabled = pageable.hasUnsavedChanges || pageable.isUnsavedPageable
        ) { pageable.initiateSave() }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Layout(enabled: Boolean, onNewPage: () -> Unit, createPageFn: @Composable (Pageable) -> Page) {
        val state = remember { State() }
        val focusReq = remember { FocusRequester() }
        fun mayRequestFocus() {
            if (StudioState.pages.opened.isEmpty()) focusReq.requestFocus()
        }
        Column(
            modifier = Modifier.fillMaxSize().focusRequester(focusReq).focusable()
                .onPointerEvent(Press) { if (it.buttons.isPrimaryPressed) mayRequestFocus() }
                .onKeyEvent { state.handleKeyEvent(it, onNewPage) }
        ) {
            Tabs.Horizontal.Layout(
                state = state.tabsState,
                tabs = StudioState.pages.opened,
                iconFn = { state.openedPage(it, createPageFn).icon },
                labelFn = { tabLabel(it) },
                isActiveFn = { StudioState.pages.active == it },
                onClick = { it.activate() },
                contextMenuFn = { state.contextMenuFn(it) },
                closeButtonFn = { IconButtonArg(icon = Icon.Code.XMARK) { state.close(it) } },
                trailingTabButtonFn = null,
                buttons = listOf(IconButtonArg(Icon.Code.PLUS, enabled = enabled) { onNewPage() })
            )
            Separator.Horizontal()
            StudioState.pages.active?.let { state.openedPage(it, createPageFn).Layout() }
        }
        LaunchedEffect(focusReq) { mayRequestFocus() }
    }

    @Composable
    private fun tabLabel(pageable: Pageable): AnnotatedString {
        return if (pageable.isWritable) {
            val changedIndicator = " *"
            AnnotatedString(pageable.name) + when {
                pageable.needSaving -> AnnotatedString(changedIndicator)
                else -> AnnotatedString(changedIndicator, SpanStyle(color = Color.Transparent))
            }
        } else {
            val builder = AnnotatedString.Builder()
            val style = SpanStyle(color = Theme.studio.onPrimary.copy(alpha = 0.6f))
            builder.append(pageable.name)
            builder.pushStyle(style)
            builder.append(" -- (${Label.READ_ONLY.lowercase()})")
            builder.pop()
            builder.toAnnotatedString()
        }
    }

    abstract class Page {

        companion object {
            private val CONTENT_MIN_HEIGHT = 64.dp
            private val RUN_PANEL_MIN_HEIGHT = 64.dp
        }

        private var frameState: Frame.FrameState? by mutableStateOf(null)
        protected abstract val hasSecondary: Boolean
        abstract val icon: Form.IconArg

        abstract fun updatePageable(pageable: Pageable)

        @Composable
        abstract fun PrimaryContent()

        @Composable
        protected open fun SecondaryContent(paneState: Frame.PaneState) {
        }

        @Composable
        private fun frameState(): Frame.FrameState {
            if (frameState == null) {
                frameState = Frame.createFrameState(
                    separator = Frame.SeparatorArgs(Separator.WEIGHT),
                    Frame.Pane(
                        id = Page::class.java.canonicalName + ".primary",
                        order = 1,
                        minSize = CONTENT_MIN_HEIGHT,
                        initSize = Either.second(1f)
                    ) { PrimaryContent() },
                    Frame.Pane(
                        id = Page::class.java.canonicalName + ".secondary",
                        order = 2,
                        minSize = RUN_PANEL_MIN_HEIGHT,
                        initSize = Either.first(Theme.PANEL_BAR_HEIGHT),
                        initFreeze = true
                    ) { paneState -> SecondaryContent(paneState) }
                )
            }
            return frameState!!
        }

        @Composable
        fun Layout() {
            key(this) {
                if (!hasSecondary) PrimaryContent()
                else Frame.Column(
                    state = frameState(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
