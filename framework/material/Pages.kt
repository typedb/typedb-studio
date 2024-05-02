/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.material

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
import com.vaticle.typedb.studio.framework.common.KeyMapper
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Form.IconButtonArg
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Sentence
import com.vaticle.typedb.studio.service.page.Pageable

object Pages {

    val MIN_WIDTH = 300.dp

    internal class State {

        private val openedPages: MutableMap<Pageable, Page> = mutableMapOf()
        internal val tabsState = Tabs.Horizontal.State<Pageable>()

        fun handleKeyEvent(event: KeyEvent): Boolean = when (event.type) {
            KeyEventType.KeyUp -> false
            else -> KeyMapper.CURRENT.map(event)?.let { execute(it) } ?: false
        }

        private fun execute(command: KeyMapper.Command): Boolean = when (command) {
            KeyMapper.Command.SAVE -> maySaveActivePage()
            KeyMapper.Command.CLOSE -> mayCloseActivePage()
            KeyMapper.Command.CTRL_TAB -> mayShowNextPage()
            KeyMapper.Command.CTRL_TAB_SHIFT -> mayShowPreviousPage()
            else -> false
        }

        @Composable
        internal fun openedPage(
            pageable: Pageable, createPageFn: @Composable (Pageable) -> Page
        ) = openedPages.getOrPut(pageable) {
            val page = createPageFn(pageable)
            pageable.onClose { openedPages.remove(it) }
            pageable.onReopen {
                page.updatePageable(it)
                openedPages[it] = page
            }
            page
        }

        private fun maySaveActivePage(): Boolean = Service.pages.active?.let {
            it.initiateSave()
            true
        } ?: false

        private fun mayShowNextPage(): Boolean = if (Service.pages.opened.size > 1) {
            Service.pages.next.activate()
            true
        } else false

        private fun mayShowPreviousPage(): Boolean = if (Service.pages.opened.size > 1) {
            Service.pages.previous.activate()
            true
        } else false

        private fun mayCloseActivePage(): Boolean = Service.pages.active?.let { close(it) } ?: false

        internal fun close(pageable: Pageable): Boolean {
            pageable.execBeforeClose()
            fun closeFn() {
                openedPages.remove(pageable)
                pageable.close()
                if (pageable.isUnsavedPageable) pageable.tryDelete()
            }
            if (pageable.isRunnable && pageable.asRunnable().isRunning) Service.confirmation.submit(
                title = Label.QUERY_IS_RUNNING,
                message = Sentence.STOP_RUNNING_QUERY_BEFORE_CLOSING_PAGE_DESCRIPTION,
                cancelLabel = Label.OK,
            ) else if (pageable.needSaving) Service.confirmation.submit(
                title = Label.SAVE_OR_DELETE,
                message = Sentence.SAVE_OR_DELETE_FILE,
                confirmLabel = Label.SAVE,
                rejectLabel = Label.DELETE,
                onReject = { closeFn() },
                onConfirm = { pageable.initiateSave(reopen = false) }
            ) else closeFn()
            return true
        }

        internal fun contextMenuFn(pageable: Pageable) = listOf(
            listOf(
                saveMenuItem(pageable),
                closeMenuItem(pageable)
            )
        )

        private fun closeMenuItem(pageable: Pageable) = ContextMenu.Item(
            label = Label.CLOSE,
            icon = Icon.CLOSE,
            info = "${KeyMapper.CURRENT.modKey} + W"
        ) { close(pageable) }

        private fun saveMenuItem(pageable: Pageable) = ContextMenu.Item(
            label = Label.SAVE,
            icon = Icon.SAVE,
            info = "${KeyMapper.CURRENT.modKey} + S",
            enabled = pageable.hasUnsavedChanges || pageable.isUnsavedPageable
        ) { pageable.initiateSave() }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Layout(enabled: Boolean, createPageFn: @Composable (Pageable) -> Page) {
        val state = remember { State() }
        val focusReq = remember { FocusRequester() }
        fun mayRequestFocus() {
            if (Service.pages.opened.isEmpty()) focusReq.requestFocus()
        }
        Column(
            modifier = Modifier.fillMaxSize().focusRequester(focusReq).focusable()
                .onPointerEvent(Press) { if (it.buttons.isPrimaryPressed) mayRequestFocus() }
                .onKeyEvent { state.handleKeyEvent(it) }
        ) {
            Tabs.Horizontal.Layout(
                state = state.tabsState,
                tabs = Service.pages.opened,
                iconFn = { state.openedPage(it, createPageFn).icon },
                labelFn = { tabLabel(it) },
                isActiveFn = { Service.pages.active == it },
                onClick = { it.activate() },
                contextMenuFn = { state.contextMenuFn(it) },
                closeButtonFn = { IconButtonArg(icon = Icon.CLOSE) { state.close(it) } },
                buttons = listOf(IconButtonArg(Icon.ADD, enabled = enabled) {
                    Service.project.tryCreateUntitledFile()?.tryOpen()
                })
            )
            Separator.Horizontal()
            Service.pages.active?.let { state.openedPage(it, createPageFn).Layout() }
        }
        LaunchedEffect(focusReq) { mayRequestFocus() }
    }

    @Composable
    private fun tabLabel(pageable: Pageable) = if (pageable.isWritable) {
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
