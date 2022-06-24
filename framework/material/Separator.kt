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

package com.vaticle.typedb.studio.framework.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.common.theme.Theme

object Separator {

    val WEIGHT = 1.dp

    @Composable
    fun Horizontal(height: Dp = WEIGHT, color: Color = Theme.studio.border, modifier: Modifier = Modifier) {
        Spacer(modifier = modifier.fillMaxWidth().height(height = height).background(color = color))
    }

    @Composable
    fun Vertical(width: Dp = WEIGHT, color: Color = Theme.studio.border, modifier: Modifier = Modifier) {
        Spacer(modifier = modifier.fillMaxHeight().width(width = width).background(color = color))
    }

}