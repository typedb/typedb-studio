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

package com.vaticle.typedb.studio.view.project

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
import com.vaticle.typedb.studio.view.common.component.Browser
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.IconArg
import com.vaticle.typedb.studio.view.common.component.Form.IconButtonArg
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Icon.Code.FOLDER_PLUS
import com.vaticle.typedb.studio.view.common.component.Navigator
import com.vaticle.typedb.studio.view.common.component.Navigator.rememberNavigatorState
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Typography
import com.vaticle.typedb.studio.view.common.theme.Typography.Style.FADED
import com.vaticle.typedb.studio.view.common.theme.Typography.Style.ITALIC
import mu.KotlinLogging

class ProjectBrowser constructor(
    order: Int,
    initOpen: Boolean = false,
    onUpdatePane: () -> Unit
) : Browser(initOpen, order, onUpdatePane) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override val label: String = Label.PROJECT
    override val icon: Icon.Code = Icon.Code.FOLDER_BLANK
    override val isActive: Boolean get() = GlobalState.project.current != null
    override var buttons: List<IconButtonArg> by mutableStateOf(emptyList())

    @Composable
    override fun BrowserLayout() {
        if (!isActive) OpenProjectHelper()
        else {
            val navState = rememberNavigatorState(
                container = GlobalState.project.current!!,
                title = Label.PROJECT_BROWSER,
                mode = Navigator.Mode.BROWSER,
                initExpandDepth = 1,
                liveUpdate = true,
                contextMenuFn = { contextMenuItems(it) }
            ) { projectItemOpen(it) }
            GlobalState.project.onProjectChange = { navState.replaceContainer(it) }
            GlobalState.project.onContentChange = { navState.reloadEntries() }
            buttons = navState.buttons
            Navigator.Layout(
                state = navState,
                modifier = Modifier.fillMaxSize(),
                iconArg = { projectItemIcon(it) },
                styleArgs = { projectItemStyles(it) }
            )
        }
    }

    @Composable
    private fun OpenProjectHelper() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().background(color = Theme.studio.background2)
        ) {
            Form.TextButton(
                text = Label.OPEN_PROJECT,
                leadingIcon = IconArg(Icon.Code.FOLDER_OPEN)
            ) { GlobalState.project.openProjectDialog.open() }
        }
    }

    private fun projectItemOpen(itemState: Navigator.ItemState<ProjectItem>) {
        when (itemState.item) {
            is Directory -> itemState.toggle()
            is File -> GlobalState.resource.open(itemState.item.asFile())
        }
    }

    private fun projectItemIcon(itemState: Navigator.ItemState<ProjectItem>): IconArg {
        return when (itemState.item) {
            is Directory -> when {
                itemState.item.isSymbolicLink -> IconArg(Icon.Code.LINK_SIMPLE)
                itemState.isExpanded -> IconArg(Icon.Code.FOLDER_OPEN)
                else -> IconArg(Icon.Code.FOLDER_BLANK)
            }
            is File -> when {
                itemState.item.asFile().isTypeQL && itemState.item.isSymbolicLink -> IconArg(Icon.Code.LINK_SIMPLE) { Theme.studio.secondary }
                itemState.item.asFile().isTypeQL -> IconArg(Icon.Code.RECTANGLE_CODE) { Theme.studio.secondary }
                itemState.item.isSymbolicLink -> IconArg(Icon.Code.LINK_SIMPLE)
                else -> IconArg(Icon.Code.FILE_LINES)
            }
        }
    }

    private fun projectItemStyles(itemState: Navigator.ItemState<ProjectItem>): List<Typography.Style> {
        return if (itemState.item.isProjectData) listOf(ITALIC, FADED) else listOf()
    }

    private fun contextMenuItems(itemState: Navigator.ItemState<ProjectItem>): List<List<ContextMenu.Item>> {
        return when (itemState.item) {
            is Directory -> directoryContextMenuItems(itemState)
            is File -> fileContextMenuItems(itemState)
        }
    }

    private fun directoryContextMenuItems(itemState: Navigator.ItemState<ProjectItem>): List<List<ContextMenu.Item>> {
        val createItemDialog = GlobalState.project.createItemDialog
        val directory = itemState.item.asDirectory()
        return listOf(
            listOf(
                ContextMenu.Item(Label.EXPAND_COLLAPSE, Icon.Code.FOLDER_OPEN) { itemState.toggle() },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.CREATE_DIRECTORY,
                    icon = FOLDER_PLUS,
                    enabled = !directory.isProjectData,
                ) { createItemDialog.open(directory, DIRECTORY) { itemState.expand() } },
                ContextMenu.Item(
                    label = Label.CREATE_FILE,
                    icon = Icon.Code.FILE_PLUS,
                    enabled = !directory.isProjectData,
                ) { createItemDialog.open(directory, FILE) { itemState.expand() } },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.RENAME,
                    icon = Icon.Code.PEN,
                    enabled = !directory.isProjectData,
                ) { GlobalState.project.renameDirectoryDialog.open(directory) },
                ContextMenu.Item(
                    label = Label.MOVE,
                    icon = Icon.Code.FOLDER_ARROW_DOWN,
                    enabled = !directory.isProjectData,
                ) { GlobalState.project.moveDirectoryDialog.open(directory) },
                ContextMenu.Item(
                    label = Label.DELETE,
                    icon = Icon.Code.TRASH_CAN,
                    enabled = !directory.isRoot && !directory.isProjectData,
                ) {
                    GlobalState.confirmation.submit(
                        title = Label.CONFIRM_DIRECTORY_DELETION,
                        message = Sentence.CONFIRM_DIRECTORY_DELETION,
                        onConfirm = { directory.delete(); itemState.navState.reloadEntries() }
                    )
                }
            )
        )
    }

    private fun fileContextMenuItems(itemState: Navigator.ItemState<ProjectItem>): List<List<ContextMenu.Item>> {
        val file = itemState.item.asFile()
        return listOf(
            listOf(
                ContextMenu.Item(
                    label = Label.OPEN,
                    icon = Icon.Code.BLOCK_QUOTE
                ) { GlobalState.resource.open(file.asFile()) },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.RENAME,
                    icon = Icon.Code.PEN,
                    enabled = !file.isProjectData,
                ) { if (file.isOpen) GlobalState.resource.renameAndReopen(file) else file.rename() },
                ContextMenu.Item(
                    label = Label.MOVE,
                    icon = Icon.Code.FOLDER_ARROW_DOWN,
                    enabled = !file.isProjectData,
                ) { if (file.isOpen) GlobalState.resource.moveAndReopen(file) else file.move() },
                ContextMenu.Item(
                    label = Label.DELETE,
                    icon = Icon.Code.TRASH_CAN,
                    enabled = !file.isProjectData,
                ) {
                    GlobalState.confirmation.submit(
                        title = Label.CONFIRM_FILE_DELETION,
                        message = Sentence.CONFIRM_FILE_DELETION,
                        onConfirm = { file.delete(); itemState.navState.reloadEntries() }
                    )
                }
            )
        )
    }
}
