/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.module.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.common.theme.Typography
import com.vaticle.typedb.studio.framework.common.theme.Typography.Style.FADED
import com.vaticle.typedb.studio.framework.common.theme.Typography.Style.ITALIC
import com.vaticle.typedb.studio.framework.material.Browsers
import com.vaticle.typedb.studio.framework.material.ContextMenu
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.IconArg
import com.vaticle.typedb.studio.framework.material.Form.IconButtonArg
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Navigator
import com.vaticle.typedb.studio.framework.material.Navigator.rememberNavigatorState
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.project.DirectoryState
import com.vaticle.typedb.studio.state.project.FileState
import com.vaticle.typedb.studio.state.project.PathState
import mu.KotlinLogging

class ProjectBrowser(initOpen: Boolean = false, order: Int) : Browsers.Browser(initOpen, order) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override val label: String = Label.PROJECT
    override val icon: Icon = Icon.PROJECT
    override val isActive: Boolean get() = StudioState.project.current != null
    override var buttons: List<IconButtonArg> by mutableStateOf(emptyList())

    @Composable
    override fun Content() {
        if (!isActive) OpenProjectHelper()
        else NavigatorLayout()
    }

    @Composable
    private fun OpenProjectHelper() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().background(color = Theme.studio.backgroundLight)
        ) {
            Form.TextButton(
                text = Label.OPEN_PROJECT,
                leadingIcon = IconArg(Icon.FOLDER_OPEN)
            ) { StudioState.project.openProjectDialog.open() }
        }
    }

    @Composable
    private fun NavigatorLayout() {
        val navState = rememberNavigatorState(
            container = StudioState.project.current!!,
            title = Label.PROJECT_BROWSER,
            mode = Navigator.Mode.BROWSER,
            initExpandDepth = 1,
            liveUpdate = true,
            openFn = { openPath(it) },
            contextMenuFn = { contextMenuItems(it) }
        ) { navState ->
            StudioState.project.onProjectChange { navState.replaceContainer(it) }
            StudioState.project.onContentChange { navState.reloadEntriesAsync() }
            StudioState.project.onClose { navState.close() }
        }
        buttons = navState.buttons
        Navigator.Layout(
            state = navState,
            modifier = Modifier.fillMaxSize(),
            iconArg = { pathIcon(it) },
            styleArgs = { pathStyles(it) }
        )
        LaunchedEffect(navState) { navState.launch() }
    }

    private fun openPath(itemState: Navigator.ItemState<PathState>) {
        when (val item = itemState.item) {
            is DirectoryState -> itemState.toggle()
            is FileState -> item.tryOpen()
        }
    }

    private fun pathIcon(itemState: Navigator.ItemState<PathState>): IconArg {
        return when (itemState.item) {
            is DirectoryState -> when {
                itemState.item.isSymbolicLink -> IconArg(Icon.SYMLINK)
                itemState.isExpanded -> IconArg(Icon.FOLDER_OPEN)
                else -> IconArg(Icon.FOLDER)
            }
            is FileState -> when {
                itemState.item.asFile().isTypeQL && itemState.item.isSymbolicLink -> IconArg(Icon.SYMLINK) { Theme.studio.secondary }
                itemState.item.asFile().isTypeQL -> IconArg(Icon.FILE_TYPEQL) { Theme.studio.secondary }
                itemState.item.isSymbolicLink -> IconArg(Icon.SYMLINK)
                else -> IconArg(Icon.FILE_OTHER)
            }
        }
    }

    private fun pathStyles(itemState: Navigator.ItemState<PathState>): List<Typography.Style> {
        return if (itemState.item.isProjectData) listOf(ITALIC, FADED) else listOf()
    }

    private fun contextMenuItems(itemState: Navigator.ItemState<PathState>): List<List<ContextMenu.Item>> {
        return when (itemState.item) {
            is DirectoryState -> directoryContextMenuItems(itemState)
            is FileState -> fileContextMenuItems(itemState)
        }
    }

    private fun directoryContextMenuItems(itemState: Navigator.ItemState<PathState>): List<List<ContextMenu.Item>> {
        val directory = itemState.item.asDirectory()
        return listOf(
            listOf(
                ContextMenu.Item(Label.EXPAND_COLLAPSE, Icon.FOLDER_OPEN) { itemState.toggle() },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.CREATE_DIRECTORY,
                    icon = Icon.DIRECTORY_CREATE,
                    enabled = !directory.isProjectData,
                ) { directory.initiateCreateDirectory { itemState.expand() } },
                ContextMenu.Item(
                    label = Label.CREATE_FILE,
                    icon = Icon.FILE_CREATE,
                    enabled = !directory.isProjectData,
                ) { directory.initiateCreateFile { itemState.expand() } },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.RENAME,
                    icon = Icon.RENAME,
                    enabled = !directory.isProjectData,
                ) { directory.initiateRename() },
                ContextMenu.Item(
                    label = Label.MOVE,
                    icon = Icon.FILE_CREATE,
                    enabled = !directory.isProjectData,
                ) { directory.initiateMove() }
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.DELETE,
                    icon = Icon.DELETE,
                    enabled = !directory.isRoot && !directory.isProjectData,
                ) { directory.initiateDelete { itemState.navState.reloadEntriesAsync() } }
            )
        )
    }

    private fun fileContextMenuItems(itemState: Navigator.ItemState<PathState>): List<List<ContextMenu.Item>> {
        val file = itemState.item.asFile()
        return listOf(
            listOf(
                ContextMenu.Item(
                    label = Label.OPEN,
                    icon = Icon.FILE_OPEN
                ) { file.tryOpen() },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.RENAME,
                    icon = Icon.RENAME,
                    enabled = !file.isProjectData,
                ) { file.initiateRename() },
                ContextMenu.Item(
                    label = Label.MOVE,
                    icon = Icon.MOVE,
                    enabled = !file.isProjectData,
                ) { file.initiateMove() },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.DELETE,
                    icon = Icon.DELETE,
                    enabled = !file.isProjectData,
                ) { file.initiateDelete { itemState.navState.reloadEntriesAsync() } }
            )
        )
    }
}
