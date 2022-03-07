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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.view.common.component.Form.ButtonArgs
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.TOOLBAR_SIZE

internal object LogOutput {

    @Composable
    internal fun Layout() {
        Row {
            Toolbar.Layout(Modifier.fillMaxHeight().width(TOOLBAR_SIZE), toolbarButtons())
            Separator.Vertical()
            Content(Modifier.fillMaxHeight().weight(1f))
        }
    }

    private fun toolbarButtons(): List<ButtonArgs> {
        return listOf(
            ButtonArgs(Icon.Code.ARROW_UP_TO_LINE) {},
            ButtonArgs(Icon.Code.ARROW_DOWN_TO_LINE) {}
        )
    }

    @Composable
    private fun Content(modifier: Modifier) {
        Box(modifier.background(Theme.colors.background2))
    }
}
