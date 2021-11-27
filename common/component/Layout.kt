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
import androidx.compose.foundation.layout.Row
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

    class MemberState(private val layoutState: AreaState, private val index: Int, val isLast: Boolean) {

        private val isFirst: Boolean = index == 0
        private val next: MemberState? get() = if (isLast) null else layoutState.items[index + 1]
        private var _width: Dp by mutableStateOf(0.dp)

        var minWidth: Dp by mutableStateOf(MIN_WIDTH)
        var freezeWidth: Dp? by mutableStateOf(null)
        var width: Dp
            get() = freezeWidth ?: _width
            set(value) {
                _width = value
            }

        fun resize(delta: Dp) {
            assert(!isLast)
            if (_width + delta > minWidth && next!!._width - delta > next!!.minWidth) {
                _width += delta
                next!!._width -= delta
            }
        }

        val nonDraggableWidth: Dp
            get() = freezeWidth ?: (_width - (if (isFirst || isLast) (DRAG_WIDTH / 2) else DRAG_WIDTH))
    }

    class AreaState(splitCount: Int) {
        val items: List<MemberState> = (0 until splitCount).map { MemberState(this, it, it == splitCount - 1) }
    }

    @Composable
    fun ResizableRow(
        splitCount: Int,
        separatorWidth: Dp? = null,
        modifier: Modifier = Modifier,
        content: @Composable (RowScope.(AreaState) -> Unit)
    ) {
        assert(splitCount >= 2)
        val layoutState = remember { AreaState(splitCount) }
        Box(modifier = modifier) {
            Row(modifier = Modifier.fillMaxWidth()) { content(layoutState) }
            Row(modifier = Modifier.fillMaxWidth()) {
                layoutState.items.filter { !it.isLast }.forEach {
                    Box(Modifier.width(it.nonDraggableWidth))
                    if (it.freezeWidth == null) VerticalSeparator(it, separatorWidth)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun VerticalSeparator(memberState: MemberState, separatorWidth: Dp?) {
        val pixelDensity = LocalDensity.current.density
        Box(
            modifier = Modifier.fillMaxHeight()
                .width(if (separatorWidth != null) DRAG_WIDTH + separatorWidth else DRAG_WIDTH)
                .pointerHoverIcon(icon = PointerIcon(Cursor(E_RESIZE_CURSOR)))
                .draggable(orientation = Horizontal, state = rememberDraggableState {
                    memberState.resize((it / pixelDensity).roundToInt().dp)
                })
        )
    }
}