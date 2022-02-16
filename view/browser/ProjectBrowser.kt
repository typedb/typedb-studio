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

package com.vaticle.typedb.studio.view.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.project.Directory
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.state.project.ProjectItem
import com.vaticle.typedb.studio.state.project.ProjectItem.Type.DIRECTORY
import com.vaticle.typedb.studio.state.project.ProjectItem.Type.FILE
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.ButtonArgs
import com.vaticle.typedb.studio.view.common.component.Form.IconArgs
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Icon.Code.FOLDER_PLUS
import com.vaticle.typedb.studio.view.common.component.Navigator
import com.vaticle.typedb.studio.view.common.component.Navigator.rememberNavigatorState
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Typography
import com.vaticle.typedb.studio.view.common.theme.Typography.Style.FADED
import com.vaticle.typedb.studio.view.common.theme.Typography.Style.ITALIC
import mu.KotlinLogging

internal class ProjectBrowser(areaState: BrowserArea.AreaState, order: Int, initOpen: Boolean = false) :
    Browser(areaState, order, initOpen) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override val label: String = Label.PROJECT
    override val icon: Icon.Code = Icon.Code.FOLDER_BLANK
    override val isActive: Boolean get() = GlobalState.project.current != null
    override var buttons: List<ButtonArgs> by mutableStateOf(emptyList())

    @Composable
    override fun NavigatorLayout() {
        if (!isActive) OpenProjectHelper()
        else {
            val state = rememberNavigatorState(
                container = GlobalState.project.current!!,
                title = Label.PROJECT_BROWSER,
                initExpandDepth = 1,
                liveUpdate = true
            ) { projectItemOpen(it) }
            GlobalState.project.onProjectChange = { state.replaceContainer(it) }
            GlobalState.project.onContentChange = { state.reloadEntries() }
            buttons = state.buttons
            Navigator.Layout(
                state = state,
                iconArgs = { projectItemIcon(it) },
                styleArgs = { projectItemStyles(it) },
                contextMenuFn = { item, onChangeEntries -> contextMenuItems(item, onChangeEntries) }
            )
        }
    }

    @Composable
    private fun OpenProjectHelper() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().background(color = Theme.colors.disabled)
        ) {
            Form.TextButton(
                text = Label.OPEN_PROJECT,
                onClick = { GlobalState.project.openProjectDialog.open() },
                leadingIcon = Icon.Code.FOLDER_OPEN
            )
        }
    }

    private fun projectItemOpen(itemState: Navigator.ItemState<ProjectItem>) {
        when (itemState.item) {
            is Directory -> itemState.asExpandable().toggle()
            is File -> GlobalState.page.open(itemState.item.asFile())
        }
    }

    private fun projectItemIcon(itemState: Navigator.ItemState<ProjectItem>): IconArgs {
        return when (itemState.item) {
            is Directory -> when {
                itemState.item.isSymbolicLink -> IconArgs(Icon.Code.LINK_SIMPLE)
                itemState.asExpandable().isExpanded -> IconArgs(Icon.Code.FOLDER_OPEN)
                else -> IconArgs(Icon.Code.FOLDER_BLANK)
            }
            is File -> when {
                itemState.item.asFile().isTypeQL && itemState.item.isSymbolicLink -> IconArgs(Icon.Code.LINK_SIMPLE) { Theme.colors.secondary }
                itemState.item.asFile().isTypeQL -> IconArgs(Icon.Code.RECTANGLE_CODE) { Theme.colors.secondary }
                itemState.item.isSymbolicLink -> IconArgs(Icon.Code.LINK_SIMPLE)
                else -> IconArgs(Icon.Code.FILE_LINES)
            }
        }
    }

    private fun projectItemStyles(itemState: Navigator.ItemState<ProjectItem>): List<Typography.Style> {
        return if (itemState.item.isProjectData) listOf(ITALIC, FADED) else listOf()
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun contextMenuItems(
        itemState: Navigator.ItemState<ProjectItem>, onChangeEntries: () -> Unit
    ): List<List<ContextMenu.Item>> {
        return when (itemState.item) {
            is Directory -> directoryContextMenuItems(itemState, onChangeEntries)
            is File -> fileContextMenuItems(itemState, onChangeEntries)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun directoryContextMenuItems(
        itemState: Navigator.ItemState<ProjectItem>, onChangeEntries: () -> Unit
    ): List<List<ContextMenu.Item>> {
        val createItemDialog = GlobalState.project.createItemDialog
        val directory = itemState.item.asDirectory()
        val state = itemState.asExpandable()
        return listOf(
            listOf(
                ContextMenu.Item(Label.EXPAND_COLLAPSE, Icon.Code.FOLDER_OPEN) { state.toggle() },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.CREATE_DIRECTORY,
                    icon = FOLDER_PLUS,
                    enabled = !directory.isProjectData,
                ) { createItemDialog.open(directory, DIRECTORY) { state.expand() } },
                ContextMenu.Item(
                    label = Label.CREATE_FILE,
                    icon = Icon.Code.FILE_PLUS,
                    enabled = !directory.isProjectData,
                ) { createItemDialog.open(directory, FILE) { state.expand() } },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.RENAME,
                    icon = Icon.Code.PEN,
                    enabled = !directory.isProjectData,
                ) { GlobalState.project.renameItemDialog.open(itemState.item) },
                ContextMenu.Item(
                    label = Label.DELETE,
                    icon = Icon.Code.TRASH_CAN,
                    enabled = !itemState.item.isRoot && !directory.isProjectData,
                ) {
                    GlobalState.confirmation.submit(
                        title = Label.CONFIRM_DIRECTORY_DELETION,
                        message = Sentence.CONFIRM_DIRECTORY_DELETION + " " + Sentence.CANNOT_BE_UNDONE,
                        onConfirm = { itemState.item.delete(); onChangeEntries() }
                    )
                }
            )
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun fileContextMenuItems(
        itemState: Navigator.ItemState<ProjectItem>, onChangeEntries: () -> Unit
    ): List<List<ContextMenu.Item>> {
        val file = itemState.item.asFile()
        return listOf(
            listOf(
                ContextMenu.Item(
                    label = Label.OPEN,
                    icon = Icon.Code.BLOCK_QUOTE
                ) { GlobalState.page.open(file.asFile()) },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.RENAME,
                    icon = Icon.Code.PEN,
                    enabled = !file.isProjectData,
                ) { GlobalState.project.renameItemDialog.open(file) },
                ContextMenu.Item(
                    label = Label.MOVE,
                    icon = Icon.Code.FOLDER_ARROW_DOWN,
                    enabled = !file.isProjectData,
                ) {
                    if (file.isOpen) GlobalState.page.moveAndReopen(file)
                    else file.moveFile()
                },
                ContextMenu.Item(
                    label = Label.DELETE,
                    icon = Icon.Code.TRASH_CAN,
                    enabled = !file.isProjectData,
                ) {
                    GlobalState.confirmation.submit(
                        title = Label.CONFIRM_FILE_DELETION,
                        message = Sentence.CONFIRM_FILE_DELETION + " " + Sentence.CANNOT_BE_UNDONE,
                        onConfirm = { file.delete(); onChangeEntries() }
                    )
                }
            )
        )
    }
}
