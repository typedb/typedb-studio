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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.state.common.Catalog.Companion.MAX_ITEM_EXPANDED
import com.vaticle.typedb.studio.state.notification.Error
import com.vaticle.typedb.studio.state.notification.Message.Project.Companion.MAX_DIR_EXPANDED_REACHED
import com.vaticle.typedb.studio.state.project.Directory
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.state.project.Project
import com.vaticle.typedb.studio.state.project.ProjectItem
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Catalog
import com.vaticle.typedb.studio.view.common.component.Catalog.IconArgs
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Theme
import mu.KotlinLogging

internal class ProjectBrowser(areaState: BrowserArea.AreaState, initOpen: Boolean = false) :
    Browser(areaState, initOpen) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override val label: String = Label.PROJECT
    override val icon: Icon.Code = Icon.Code.FOLDER_BLANK
    override val isActive: Boolean get() = State.project.current != null
    override val buttons: List<ButtonArgs> = listOf(
        ButtonArgs(Icon.Code.CHEVRONS_DOWN) { State.project.current?.expand { onExpandLimitReached() } },
        ButtonArgs(Icon.Code.CHEVRONS_UP) { State.project.current?.collapse() }
    )

    private fun onExpandLimitReached() {
        val error = Error.fromUser(MAX_DIR_EXPANDED_REACHED, State.project.current!!.path, MAX_ITEM_EXPANDED)
        State.notification.userError(error, LOGGER)
    }

    @Composable
    override fun Catalog() {
        if (!isActive) OpenProjectHelper()
        else Catalog.Layout(
            catalog = State.project.current!!,
            iconArgs = { projectItemIcon(it) },
            contextMenuFn = { contextMenuItems(State.project.current!!, it) }
        )
    }

    @Composable
    private fun OpenProjectHelper() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().background(color = Theme.colors.disabled)
        ) {
            Form.TextButton(
                text = Label.OPEN_PROJECT,
                onClick = { State.project.showDialog = true },
                leadingIcon = Icon.Code.FOLDER_OPEN
            )
        }
    }

    private fun projectItemIcon(item: ProjectItem): IconArgs {
        return when (item) {
            is Directory -> when {
                item.isSymbolicLink -> IconArgs(Icon.Code.LINK_SIMPLE)
                item.isExpanded -> IconArgs(Icon.Code.FOLDER_OPEN)
                else -> IconArgs(Icon.Code.FOLDER_BLANK)
            }
            is File -> when {
                item.isTypeQL && item.isSymbolicLink -> IconArgs(Icon.Code.LINK_SIMPLE) { Theme.colors.secondary }
                item.isTypeQL -> IconArgs(Icon.Code.RECTANGLE_CODE) { Theme.colors.secondary }
                item.isSymbolicLink -> IconArgs(Icon.Code.LINK_SIMPLE)
                else -> IconArgs(Icon.Code.FILE_LINES)
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun contextMenuItems(project: Project, item: ProjectItem): List<ContextMenu.Item> {
        return when (item) {
            is Directory -> directoryContextMenuItems(project, item)
            is File -> fileContextMenuItems(project, item)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun directoryContextMenuItems(project: Project, directory: Directory): List<ContextMenu.Item> {
        return listOf(
            ContextMenu.Item(Label.EXPAND_COLLAPSE, Icon.Code.FOLDER_OPEN) { directory.toggle() },
            ContextMenu.Item(Label.CREATE_DIRECTORY, Icon.Code.FOLDER_PLUS) { }, // TODO
            ContextMenu.Item(Label.CREATE_FILE, Icon.Code.FILE_PLUS) { }, // TODO
            ContextMenu.Item(Label.DELETE, Icon.Code.TRASH_CAN) { directory.delete() }
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun fileContextMenuItems(project: Project, file: File): List<ContextMenu.Item> {
        return listOf(
            ContextMenu.Item(Label.OPEN, Icon.Code.PEN) { project.open(file) },
            ContextMenu.Item(Label.DELETE, Icon.Code.TRASH_CAN) { file.delete() }
        )
    }
}
