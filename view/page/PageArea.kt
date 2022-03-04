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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.view.common.KeyMapper
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.ButtonArgs
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.component.Tabs
import kotlinx.coroutines.CoroutineScope

object PageArea {

    val MIN_WIDTH = 300.dp

    internal class State(coroutineScope: CoroutineScope) {

        internal val tabsState = Tabs.State<Resource>(coroutineScope)
        internal val openedPages: MutableMap<Resource, Page> = mutableMapOf()
        internal var density: Float
            get() = tabsState.density
            set(value) {
                tabsState.density = value
            }

        fun handleKeyEvent(event: KeyEvent): Boolean {
            return if (event.type == KeyEventType.KeyUp) false
            else KeyMapper.CURRENT.map(event)?.let { execute(it) } ?: false
        }

        private fun execute(command: KeyMapper.Command): Boolean {
            return when (command) {
                KeyMapper.Command.NEW_PAGE -> createAndOpenNewFile()
                KeyMapper.Command.SAVE -> saveActivePage()
                KeyMapper.Command.CLOSE -> closeActivePage()
                KeyMapper.Command.TAB_CTRL -> showNextPage()
                KeyMapper.Command.TAB_CTRL_SHIFT -> showPreviousPage()
                else -> false
            }
        }


        internal fun createAndOpenNewFile(): Boolean {
            GlobalState.project.tryCreateUntitledFile()?.let { GlobalState.resource.open(it) }
            return true
        }

        private fun saveActivePage(): Boolean {
            GlobalState.resource.saveAndReopen(GlobalState.resource.active!!)
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

        internal fun close(resource: Resource): Boolean {
            resource.execBeforeClose()
            fun closeFn() {
                openedPages.remove(resource)
                GlobalState.resource.close(resource)
                if (resource.isUnsavedResource) resource.delete()
            }
            if (resource.needSaving) {
                GlobalState.confirmation.submit(
                    title = Label.SAVE_OR_DELETE,
                    message = Sentence.SAVE_OR_DELETE_FILE,
                    confirmLabel = Label.SAVE,
                    cancelLabel = Label.DELETE,
                    onCancel = { closeFn() },
                    onConfirm = { resource.save() },
                )
            } else closeFn()
            return true
        }

        internal fun contextMenuFn(resource: Resource): List<List<ContextMenu.Item>> {
            val modKey = if (Property.OS.Current == Property.OS.MACOS) Label.CMD else Label.CTRL
            val enableSave = resource.hasUnsavedChanges || resource.isUnsavedResource
            return listOf(
                listOf(
                    ContextMenu.Item(Label.SAVE, Icon.Code.FLOPPY_DISK, "$modKey + S", enableSave) {
                        GlobalState.resource.saveAndReopen(resource)
                    },
                    ContextMenu.Item(Label.CLOSE, Icon.Code.XMARK, "$modKey + W") { close(resource) }
                )
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Layout() {
        val density = LocalDensity.current.density
        val coroutineScope = rememberCoroutineScope()
        val state = remember { State(coroutineScope) }
        state.density = density
        val focusReq = FocusRequester()
        fun mayRequestFocus() {
            if (GlobalState.resource.opened.isEmpty()) focusReq.requestFocus()
        }
        state.openedPages.values.forEach { it.resetFocus() }
        Column(
            modifier = Modifier.fillMaxSize().focusRequester(focusReq).focusable()
                .onPointerEvent(Press) { if (it.buttons.isPrimaryPressed) mayRequestFocus() }
                .onKeyEvent { state.handleKeyEvent(it) }
        ) {
            Tabs.Layout(
                state = state.tabsState,
                tabs = GlobalState.resource.opened,
                iconFn = { resource -> state.openedPages[resource]?.icon ?: Form.IconArgs(Icon.Code.FILE_LINES) },
                labelFn = { tabLabel(it) },
                isActiveFn = { GlobalState.resource.isActive(it) },
                onClick = { GlobalState.resource.activate(it) },
                onClose = { state.close(it) },
                contextMenuFn = { state.contextMenuFn(it) },
                ButtonArgs(Icon.Code.PLUS, GlobalState.project.current != null) { state.createAndOpenNewFile() }
            )
            Separator.Horizontal()
            GlobalState.resource.active?.let { resource ->
                state.openedPages.getOrPut(resource) {
                    val page = Page.of(resource)
                    resource.onClose { state.openedPages.remove(it) }
                    resource.onReopen {
                        page.updateResource(it)
                        state.openedPages[it] = page
                    }
                    page
                }.Layout()
            }
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
            val style = SpanStyle(color = Theme.colors.onPrimary.copy(alpha = 0.6f))
            builder.append(resource.name)
            builder.pushStyle(style)
            builder.append(" -- (${Label.READ_ONLY.lowercase()})")
            builder.pop()
            builder.toAnnotatedString()
        }
    }
}
