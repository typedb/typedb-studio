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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.ButtonArgs
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Navigator
import com.vaticle.typedb.studio.view.common.component.Navigator.rememberNavigatorState
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
    override var buttons: List<ButtonArgs> by mutableStateOf(emptyList())

    @Composable
    override fun NavigatorLayout() {
        if (!isActive) OpenProjectHelper()
        else {
            val state = rememberNavigatorState(
                navigable = State.project.current!!,
                title = Label.PROJECT_BROWSER,
                initExpandDepth = 1,
                reloadOnExpand = true,
                liveUpdate = true
            )
            State.project.onChange = { state.replaceRoot(it) }
            buttons = state.buttons
            Navigator.Layout(state = state)
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
                onClick = { State.project.showDialog = true },
                leadingIcon = Icon.Code.FOLDER_OPEN
            )
        }
    }
}
