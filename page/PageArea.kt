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

package com.vaticle.typedb.studio.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.common.component.Layout
import com.vaticle.typedb.studio.common.component.Separator

object PageArea {

    private val MIN_WIDTH = 100.dp
    private val TAB_HEIGHT = 26.dp
    private val TAB_WIDTH = 100.dp

    private class AreaState(val layoutState: Layout.ItemState) {

        init {
            layoutState.minWidth = MIN_WIDTH
        }
    }

    @Composable
    fun Layout(layoutState: Layout.ItemState) {
        val areaState = remember { AreaState(layoutState) }
        val pixelDensity = LocalDensity.current.density
        fun updateLayoutWidth(coord: LayoutCoordinates){
            layoutState.width = (coord.size.width / pixelDensity).toInt().dp
        }

        Column(modifier = Modifier.fillMaxWidth().onGloballyPositioned { updateLayoutWidth(it) }) {
            Row(Modifier.fillMaxWidth().height(TAB_HEIGHT), horizontalArrangement = Arrangement.Start) {
                val tabs = arrayOf(1, 2, 3, 4, 5)
                for ((i, tab) in tabs.withIndex()) {
                    if (i > 0) Separator.Vertical()
                    Column(Modifier.fillMaxHeight().width(TAB_WIDTH)) {

                    }
                }
            }
            Separator.Horizontal()
            Row(Modifier.fillMaxWidth()) {

            }
        }
    }
}
