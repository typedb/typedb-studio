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
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.state.resource.Resource
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

        private val openedPages: MutableMap<Resource, Page> = mutableMapOf()
        internal val tabsState = Tabs.Horizontal.State<Resource>()

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
        internal fun openedPage(resource: Resource): Page {
            return openedPages.getOrPut(resource) {
                val page = createPage(resource)
                resource.onClose { openedPages.remove(it) }
                resource.onReopen {
                    page.updateResource(it)
                    openedPages[it] = page
                }
                page
            }
        }

        @Composable
        private fun createPage(resource: Resource) = when (resource) {
            is File -> FilePage.create(resource)
            is TypeState.Thing -> TypePage.create(resource)
            else -> throw IllegalStateException("Unrecognised resource type")
        }

        internal fun createAndOpenNewFile(): Boolean {
            GlobalState.project.tryCreateUntitledFile()?.let { GlobalState.resource.tryOpen(it) }
            return true
        }

        private fun saveActivePage(): Boolean {
            GlobalState.resource.active?.initiateSave()
            return true
        }

        private fun showNextPage(): Boolean {
            GlobalState.resource.activateNext()
            return true
        }

        private fun showPreviousPage(): Boolean {
            GlobalState.resource.activatePrevious()
            return true
        }

        private fun closeActivePage(): Boolean {
            return GlobalState.resource.active?.let { close(it) } ?: false
        }

        internal fun close(resource: Resource, stopRunner: Boolean = false): Boolean {
            resource.execBeforeClose()
            fun closeFn() {
                openedPages.remove(resource)
                GlobalState.resource.close(resource)
                if (resource.isUnsavedResource) resource.delete()
            }
            if (resource.isRunnable && GlobalState.client.hasRunningQuery && !stopRunner) {
                GlobalState.confirmation.submit(
                    title = Label.QUERY_IS_RUNNING,
                    message = Sentence.STOP_RUNNING_QUERY_BEFORE_CLOSING_PAGE_DESCRIPTION,
                    cancelLabel = Label.OK,
                )
            } else if (resource.needSaving) {
                GlobalState.confirmation.submit(
                    title = Label.SAVE_OR_DELETE,
                    message = Sentence.SAVE_OR_DELETE_FILE,
                    confirmLabel = Label.SAVE,
                    rejectLabel = Label.DELETE,
                    onReject = { closeFn() },
                    onConfirm = { resource.initiateSave(reopen = false) }
                )
            } else closeFn()
            return true
        }

        internal fun contextMenuFn(resource: Resource): List<List<ContextMenu.Item>> {
            return listOf(
                listOf(
                    saveMenuItem(resource),
                    closeMenuItem(resource)
                )
            )
        }

        private fun closeMenuItem(resource: Resource) = ContextMenu.Item(
            label = Label.CLOSE,
            icon = Icon.Code.XMARK,
            info = "${KeyMapper.CURRENT.modKey} + W"
        ) { close(resource) }

        private fun saveMenuItem(resource: Resource) = ContextMenu.Item(
            label = Label.SAVE,
            icon = Icon.Code.FLOPPY_DISK,
            info = "${KeyMapper.CURRENT.modKey} + S",
            enabled = resource.hasUnsavedChanges || resource.isUnsavedResource
        ) { resource.initiateSave() }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Layout() {
        val state = remember { State() }
        val focusReq = remember { FocusRequester() }
        fun mayRequestFocus() {
            if (GlobalState.resource.opened.isEmpty()) focusReq.requestFocus()
        }
        Column(
            modifier = Modifier.fillMaxSize().focusRequester(focusReq).focusable()
                .onPointerEvent(Press) { if (it.buttons.isPrimaryPressed) mayRequestFocus() }
                .onKeyEvent { state.handleKeyEvent(it) }
        ) {
            Tabs.Horizontal.Layout(
                state = state.tabsState,
                tabs = GlobalState.resource.opened,
                iconFn = { resource -> state.openedPage(resource).icon },
                labelFn = { tabLabel(it) },
                isActiveFn = { GlobalState.resource.isActive(it) },
                onClick = { GlobalState.resource.activate(it) },
                contextMenuFn = { state.contextMenuFn(it) },
                closeButtonFn = { IconButtonArg(icon = Icon.Code.XMARK) { state.close(it) } },
                trailingTabButtonFn = null,
                extraBarButtons = listOf(IconButtonArg(Icon.Code.PLUS, enabled = GlobalState.project.current != null) {
                    state.createAndOpenNewFile()
                })
            )
            Separator.Horizontal()
            GlobalState.resource.active?.let { resource -> state.openedPage(resource).Layout() }
        }
        LaunchedEffect(focusReq) { mayRequestFocus() }
    }

    @Composable
    private fun tabLabel(resource: Resource): AnnotatedString {
        return if (resource.isWritable) {
            val changedIndicator = " *"
            AnnotatedString(resource.name) + when {
                resource.needSaving -> AnnotatedString(changedIndicator)
                else -> AnnotatedString(changedIndicator, SpanStyle(color = Color.Transparent))
            }
        } else {
            val builder = AnnotatedString.Builder()
            val style = SpanStyle(color = Theme.studio.onPrimary.copy(alpha = 0.6f))
            builder.append(resource.name)
            builder.pushStyle(style)
            builder.append(" -- (${Label.READ_ONLY.lowercase()})")
            builder.pop()
            builder.toAnnotatedString()
        }
    }
}
