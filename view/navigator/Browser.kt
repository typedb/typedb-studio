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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme

sealed class Browser(private val areaState: NavigatorArea.AreaState, initOpen: Boolean = false) {

    companion object {
        internal val MIN_HEIGHT = 80.dp
        private val BAR_HEIGHT = 26.dp
        private val BAR_SPACING = 8.dp
    }

    data class ButtonArgs(val icon: Icon.Code, val onClick: () -> Unit)

    internal abstract val label: String
    internal abstract val icon: Icon.Code
    internal abstract val isActive: Boolean
    internal abstract val buttons: List<ButtonArgs>

    internal var isOpen: Boolean by mutableStateOf(initOpen)

    @Composable
    abstract fun Catalog()

    fun toggle() {
        isOpen = !isOpen
        areaState.mayHidePanelArea()
    }


    @Composable
    internal fun Layout() {
        Column {
            Bar()
            Separator.Horizontal()
            Box(modifier = Modifier.weight(1f)) { Catalog() }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Bar() {
        Row(
            modifier = Modifier.fillMaxWidth().height(BAR_HEIGHT).background(color = Theme.colors.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(BAR_SPACING))
            Icon.Render(icon = icon)
            Spacer(Modifier.width(BAR_SPACING))
            Form.Text(value = label)
            Spacer(Modifier.weight(1f))
            Buttons(*buttons.toTypedArray(), isActive = isActive)
            Buttons(ButtonArgs(Icon.Code.XMARK) { toggle() }, isActive = true)
        }
    }

    @Composable
    private fun Buttons(vararg buttons: ButtonArgs, isActive: Boolean) {
        buttons.forEach {
            Form.IconButton(
                icon = it.icon,
                onClick = { it.onClick() },
                modifier = Modifier.size(BAR_HEIGHT),
                bgColor = Color.Transparent,
                rounded = false,
                enabled = isActive
            )
        }
    }
}
