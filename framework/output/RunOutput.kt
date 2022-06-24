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

package com.vaticle.typedb.studio.framework.output

import androidx.compose.foundation.background
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
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.IconButton
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Separator

internal sealed class RunOutput {

    abstract val name: String
    abstract val icon: Icon.Code
    abstract val buttons: List<Form.IconButtonArg>

    @Composable
    abstract fun content(modifier: Modifier)

    @Composable
    internal fun Layout() {
        Row {
            Toolbar(Modifier.fillMaxHeight().width(Theme.TOOLBAR_SIZE), buttons)
            Separator.Vertical()
            content(Modifier.fillMaxHeight().weight(1f).background(Theme.studio.backgroundDark))
        }
    }

    @Composable
    private fun Toolbar(modifier: Modifier, buttons: List<Form.IconButtonArg>) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            buttons.forEach {
                Spacer(Modifier.height(Theme.TOOLBAR_SPACING))
                IconButton(
                    icon = it.icon,
                    hoverIcon = it.hoverIcon,
                    modifier = Modifier.size(Theme.TOOLBAR_BUTTON_SIZE),
                    iconColor = it.color(),
                    iconHoverColor = it.hoverColor?.invoke(),
                    disabledColor = it.disabledColor?.invoke(),
                    enabled = it.enabled,
                    tooltip = it.tooltip,
                    onClick = it.onClick
                )
            }
        }
    }
}