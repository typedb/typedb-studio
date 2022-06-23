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

package com.vaticle.typedb.studio.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.page.Pageable
import com.vaticle.typedb.studio.state.project.FileState
import com.vaticle.typedb.studio.state.schema.TypeState
import com.vaticle.typedb.studio.view.common.KeyMapper
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.material.ContextMenu
import com.vaticle.typedb.studio.view.material.Form.IconButtonArg
import com.vaticle.typedb.studio.view.material.Icon
import com.vaticle.typedb.studio.view.material.Page
import com.vaticle.typedb.studio.view.material.Separator
import com.vaticle.typedb.studio.view.material.Tabs
import com.vaticle.typedb.studio.view.project.FilePage
import com.vaticle.typedb.studio.view.type.TypePage

object PageArea {

    val MIN_WIDTH = 300.dp

    internal class State {

        private val openedPages: MutableMap<Pageable, Page> = mutableMapOf()
        internal val tabsState = Tabs.Horizontal.State<Pageable>()

        fun handleKeyEvent(event: KeyEvent): Boolean {
            return if (event.type == KeyEventType.KeyUp) false
            else KeyMapper.CURRENT.map(event)?.let { execute(it) } ?: false
        }

        private fun execute(command: KeyMapper.Command): Boolean {
            return when (command) {
                KeyMapper.Command.NEW_PAGE -> createAndOpenNewFile()
                KeyMapper.Command.SAVE -> saveActivePage()
                KeyMapper.Command.CLOSE -> closeActivePage()
                KeyMapper.Command.CTRL_TAB -> showNextPage()
                KeyMapper.Command.CTRL_TAB_SHIFT -> showPreviousPage()
                else -> false
            }
        }

        @Composable
        internal fun openedPage(pageable: Pageable): Page {
            return openedPages.getOrPut(pageable) {
                val page = createPage(pageable)
                pageable.onClose { openedPages.remove(it) }
                pageable.onReopen {
                    page.updatePageable(it)
                    openedPages[it] = page
                }
                page
            }
        }

        @Composable
        private fun createPage(pageable: Pageable) = when (pageable) {
            is FileState -> FilePage.create(pageable)
            is TypeState.Thing -> TypePage.create(pageable)
            else -> throw IllegalStateException("Unrecognised pageable type")
        }

        internal fun createAndOpenNewFile(): Boolean {
            StudioState.project.tryCreateUntitledFile()?.let { it.tryOpen() }
            return true
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
            if (pageable.isRunnable && StudioState.client.hasRunningQuery && !stopRunner) {
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
    fun Layout() {
        val state = remember { State() }
        val focusReq = remember { FocusRequester() }
        fun mayRequestFocus() {
            if (StudioState.pages.opened.isEmpty()) focusReq.requestFocus()
        }
        Column(
            modifier = Modifier.fillMaxSize().focusRequester(focusReq).focusable()
                .onPointerEvent(Press) { if (it.buttons.isPrimaryPressed) mayRequestFocus() }
                .onKeyEvent { state.handleKeyEvent(it) }
        ) {
            Tabs.Horizontal.Layout(
                state = state.tabsState,
                tabs = StudioState.pages.opened,
                iconFn = { state.openedPage(it).icon },
                labelFn = { tabLabel(it) },
                isActiveFn = { StudioState.pages.active == it },
                onClick = { it.activate() },
                contextMenuFn = { state.contextMenuFn(it) },
                closeButtonFn = { IconButtonArg(icon = Icon.Code.XMARK) { state.close(it) } },
                trailingTabButtonFn = null,
                extraBarButtons = listOf(IconButtonArg(Icon.Code.PLUS, enabled = StudioState.project.current != null) {
                    state.createAndOpenNewFile()
                })
            )
            Separator.Horizontal()
            StudioState.pages.active?.let { state.openedPage(it).Layout() }
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
}
