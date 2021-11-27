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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Cursor
import java.awt.Cursor.E_RESIZE_CURSOR
import kotlin.math.roundToInt

object Layout {

    private val DRAG_WIDTH = 6.dp
    private val MIN_WIDTH = 10.dp

    class RowItemState {
        private var _width: Dp by mutableStateOf(0.dp)
        var minWidth: Dp by mutableStateOf(MIN_WIDTH)
        var freezeWidth: Dp? by mutableStateOf(null)
        var width: Dp
            get() = freezeWidth ?: _width
            set(width) { _width = if (width > minWidth) width else minWidth }
    }

    class RowState(splitCount: Int) {
        val rowItems: List<RowItemState> = (1 until splitCount).map { RowItemState() }
    }

    @Composable
    fun ResizableRow(
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
                    Box(Modifier.width(itemState.width - (if (i == 0) (DRAG_WIDTH / 2) else DRAG_WIDTH)))
                    if (itemState.freezeWidth == null) VerticalSeparator(itemState, separatorWidth)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun VerticalSeparator(itemState: RowItemState, separatorWidth: Dp?) {
        val pixelDensity = LocalDensity.current.density
        fun updateWidth(delta: Float) { itemState.width = itemState.width + (delta / pixelDensity).roundToInt().dp }
        Box(
            modifier = Modifier.fillMaxHeight()
                .width(if (separatorWidth != null) DRAG_WIDTH + separatorWidth else DRAG_WIDTH)
                .pointerHoverIcon(icon = PointerIcon(Cursor(E_RESIZE_CURSOR)))
                .draggable(orientation = Horizontal, state = rememberDraggableState { updateWidth(it) })
        )
    }
}