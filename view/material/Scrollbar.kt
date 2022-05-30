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

package com.vaticle.typedb.studio.view.material

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.view.common.theme.Theme.SCROLLBAR_END_PADDING
import com.vaticle.typedb.studio.view.common.theme.Theme.SCROLLBAR_LONG_PADDING

object Scrollbar {

    @Composable
    fun Vertical(adapter: ScrollbarAdapter, modifier: Modifier, containerSize: Dp = 0.dp) {
        val density = LocalDensity.current.density
        val maxOffset = adapter.maxScrollOffset((containerSize.value * density).toInt())
        if (maxOffset > 0 && maxOffset < Int.MAX_VALUE) {
            VerticalScrollbar(adapter, modifier.fillMaxHeight().padding(SCROLLBAR_LONG_PADDING, SCROLLBAR_END_PADDING))
        }
    }

    @Composable
    fun Horizontal(adapter: ScrollbarAdapter, modifier: Modifier, containerSize: Dp = 0.dp) {
        val density = LocalDensity.current.density
        val maxOffset = adapter.maxScrollOffset((containerSize.value * density).toInt())
        if (maxOffset > 0 && maxOffset < Int.MAX_VALUE) {
            HorizontalScrollbar(adapter, modifier.fillMaxWidth().padding(SCROLLBAR_END_PADDING, SCROLLBAR_LONG_PADDING))
        }
    }
}