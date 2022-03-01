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

package com.vaticle.typedb.studio.view.response

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Frame
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.PANEL_BAR_HEIGHT

object Response {

    class State(private val paneState: Frame.PaneState, val name: String) {

        internal var isOpen: Boolean by mutableStateOf(false)

        internal fun toggle() {
            isOpen = !isOpen
            mayUpdatePaneState()
        }

        private fun mayUpdatePaneState() {
            if (!isOpen) paneState.freeze(PANEL_BAR_HEIGHT)
            else paneState.unfreeze()
        }
    }

    @Composable
    fun Layout(state: State) {
        Column(Modifier.fillMaxSize()) {
            Bar(state)
            if (state.isOpen) {
                Separator.Horizontal()
            }
        }
    }

    @Composable
    private fun Bar(state: State) {
        Row(
            modifier = Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT).background(color = Theme.colors.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(Theme.PANEL_BAR_SPACING))
            Form.Text(value = Label.RUN + ":")
            Spacer(Modifier.weight(1f))
            ToggleButton(state)
        }
    }

    @Composable
    private fun ToggleButton(state: State) {
        Form.IconButton(
            icon = if (state.isOpen) Icon.Code.CARET_DOWN else Icon.Code.CARET_UP,
            onClick = { state.toggle() },
            modifier = Modifier.size(PANEL_BAR_HEIGHT),
            bgColor = Color.Transparent,
            rounded = false,
        )
    }
}
