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
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Cursor
import java.awt.Cursor.E_RESIZE_CURSOR
import kotlin.math.roundToInt

object Resizable {

    val DRAG_WIDTH = 6.dp

    class RowItemState {
        var width: Dp? by mutableStateOf(null)
        var delta: Float by mutableStateOf(0f)
        var isResizable by mutableStateOf(true)
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
                    val itemWidth = itemState.width?.let { it + itemState.delta.roundToInt().dp } ?: 0.dp
                    val draggableWidth = if (i == 0) (DRAG_WIDTH / 2) else DRAG_WIDTH
                    Box(Modifier.width(itemWidth - draggableWidth))
                    if (itemState.isResizable) DraggableVerticalSeparator(itemState, separatorWidth)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun DraggableVerticalSeparator(itemState: RowItemState, separatorWidth: Dp?) {
        val pixelDensity = LocalDensity.current.density

        fun updateItemStateWidth(delta: Float) {
            itemState.width = itemState.width!! + (delta / pixelDensity).roundToInt().dp
        }

        Box(
            modifier = Modifier.fillMaxHeight()
                .width(if (separatorWidth != null) DRAG_WIDTH + separatorWidth else DRAG_WIDTH)
                .pointerHoverIcon(icon = PointerIcon(Cursor(E_RESIZE_CURSOR)))
                .draggable(orientation = Horizontal, state = rememberDraggableState { updateItemStateWidth(it) })
        )
    }
}