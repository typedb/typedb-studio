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

package com.vaticle.typedb.studio.view.output

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme

internal object LogOutput {

    private val BAR_WIDTH = 34.dp
    private val BUTTON_SIZE = 24.dp
    private val BAR_SPACING = 5.dp

    @Composable
    internal fun Layout() {
        Row {
            Bar(Modifier.fillMaxHeight().width(BAR_WIDTH))
            Separator.Vertical()
            Content(Modifier.fillMaxHeight().weight(1f))
        }
    }

    @Composable
    private fun Bar(modifier: Modifier) {
        Column(modifier.background(Theme.colors.background), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer()
            BarButton(Icon.Code.ARROW_UP_TO_LINE) {}
            Spacer()
            BarButton(Icon.Code.ARROW_DOWN_TO_LINE) {}
        }
    }

    @Composable
    private fun Spacer() {
        Spacer(Modifier.height(BAR_SPACING))
    }

    @Composable
    private fun BarButton(icon: Icon.Code, onClick: () -> Unit) {
        IconButton(
            icon = icon,
            onClick = onClick,
            modifier = Modifier.size(BUTTON_SIZE)
        )
    }

    @Composable
    private fun Content(modifier: Modifier) {
        Box(modifier.background(Theme.colors.background2))
    }
}
