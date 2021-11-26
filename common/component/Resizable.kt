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

package com.vaticle.typedb.studio.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Resizable {

    val DRAG_WIDTH = 6.dp

    class RowItemState {
        var width: Dp? by mutableStateOf(null)
        val isResizable by mutableStateOf(true)
    }

    class RowState(splitCount: Int) {
        val rowItems: List<RowItemState> = (1 until splitCount).map { RowItemState() }
    }

    @Composable
    fun Row(
        splitCount: Int,
        separatorWidth: Dp? = null,
        modifier: Modifier = Modifier,
        content: @Composable (RowScope.(RowState) -> Unit)
    ) {
        assert(splitCount >= 2)
        val rowState = remember { RowState(splitCount) }
        Box(modifier = modifier) {
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                content(rowState)
            }
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                rowState.rowItems.forEachIndexed { i, itemState ->
                    val subtract = if (i == 0) (DRAG_WIDTH / 2) else DRAG_WIDTH
                    Box(Modifier.width(itemState.width?.let { w -> w - subtract } ?: 0.dp))
//                    if (itemState.isResizable) DraggableVerticalSeparator(itemState, separatorWidth)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun DraggableVerticalSeparator(state: RowItemState, separatorWidth: Dp?) {
        val width = if (separatorWidth != null) DRAG_WIDTH + separatorWidth else DRAG_WIDTH
        Box(
            modifier = Modifier.fillMaxHeight().width(width).pointerIcon(PointerIcon.Crosshair)
                .background(color = Color.Red.copy(alpha = 0.2f), shape = RectangleShape)
        )
    }
}