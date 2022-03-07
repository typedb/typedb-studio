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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.theme.Theme

internal object Toolbar {

    @Composable
    internal fun Layout(modifier: Modifier, buttons: List<Form.ButtonArg>) {
        Column(modifier.background(Theme.colors.background), horizontalAlignment = Alignment.CenterHorizontally) {
            buttons.forEach {
                Spacer(Modifier.height(Theme.TOOLBAR_SPACING))
                BarButton(it)
            }
        }
    }

    @Composable
    private fun BarButton(buttonArg: Form.ButtonArg) {
        Form.IconButton(
            icon = buttonArg.icon,
            onClick = buttonArg.onClick,
            modifier = Modifier.size(Theme.TOOLBAR_BUTTON_SIZE)
        )
    }
}