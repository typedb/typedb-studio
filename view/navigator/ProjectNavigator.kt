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

package com.vaticle.typedb.studio.view.navigator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.state.project.Directory
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.state.project.ProjectItem
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Catalog
import com.vaticle.typedb.studio.view.common.component.Catalog.IconArgs
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Theme

internal class ProjectNavigator(areaState: NavigatorArea.AreaState, initOpen: Boolean = false) :
    Navigator(areaState, initOpen) {

    override val icon: Icon.Code = Icon.Code.FOLDER_BLANK
    override val label: String = Label.PROJECT
    override val buttons: List<ButtonArgs> = listOf(
        ButtonArgs(Icon.Code.CHEVRONS_DOWN) { State.project.current?.expand() },
        ButtonArgs(Icon.Code.CHEVRONS_UP) { State.project.current?.collapse() }
    )

    @Composable
    override fun Catalog() {
        if (State.project.current == null) OpenProjectHelper()
        else Catalog.Layout(items = listOf(State.project.current!!.directory), iconArgs = { projectItemIcon(it) })
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
}
