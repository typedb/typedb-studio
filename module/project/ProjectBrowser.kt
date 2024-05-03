/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.project.DirectoryState
import com.vaticle.typedb.studio.service.project.FileState
import com.vaticle.typedb.studio.service.project.PathState
import mu.KotlinLogging

class ProjectBrowser(initOpen: Boolean = false, order: Int) : Browsers.Browser(initOpen, order) {

    override val label: String = Label.PROJECT
    override val icon: Icon = Icon.FOLDER
    override val isActive: Boolean get() = Service.project.current != null
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
            ) { Service.project.openProjectDialog.open() }
        }
    }

    @Composable
    private fun NavigatorLayout() {
        val navState = rememberNavigatorState(
            container = Service.project.current!!,
            title = Label.PROJECT_BROWSER,
            behaviour = Navigator.Behaviour.Browser(),
            initExpandDepth = 1,
            liveUpdate = true,
            openFn = { openPath(it) },
            contextMenuFn = { contextMenuItems(it) }
        ) { navState ->
            Service.project.onProjectChange { navState.replaceContainer(it) }
            Service.project.onContentChange { navState.reloadEntriesAsync() }
            Service.project.onClose { navState.close() }
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
