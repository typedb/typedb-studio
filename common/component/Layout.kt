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
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import java.awt.Cursor
import java.awt.Cursor.E_RESIZE_CURSOR
import java.awt.Cursor.N_RESIZE_CURSOR
import kotlin.math.roundToInt

object Layout {

    private val DRAG_SIZE = 6.dp
    private val MIN_SIZE = 10.dp

    class MemberState(private val layoutState: AreaState, private val index: Int, val isLast: Boolean) {

        private val isFirst: Boolean = index == 0
        private val next: MemberState? get() = if (isLast) null else layoutState.members[index + 1]
        private var _size: Dp by mutableStateOf(0.dp)
        internal var freezeSize: Dp? by mutableStateOf(null); private set
        var minSize: Dp by mutableStateOf(MIN_SIZE)
        var size: Dp
            get() = freezeSize ?: _size
            set(value) {
                _size = value
            }

        val nonDraggableSize: Dp
            get() = freezeSize ?: (_size - (if (isFirst || isLast) (DRAG_SIZE / 2) else DRAG_SIZE))

        fun tryResize(delta: Dp) {
            _size += max(delta, minSize - _size)
        }

        fun tryResizeSelfAndNext(delta: Dp) {
            assert(!isLast && next != null)
            val cappedDelta = min(max(delta, minSize - _size), next!!.size - next!!.minSize)
            _size += cappedDelta
            next!!.size -= cappedDelta
        }

        fun freeze(size: Dp) {
            freezeSize = size
        }

        fun unfreeze() {
            freezeSize = null
        }
    }

    class AreaState(members: Int, private val separatorSize: Dp?) {
        val members: List<MemberState> = (0 until members).map { MemberState(this, it, it == members - 1) }
        private val currentSize: Dp
            get() {
                var size = 0.dp
                members.map { size += it.size }
                separatorSize?.let { size += it * (members.size - 1) }
                return size
            }

        fun constraint(maxSize: Dp) {
            var i = members.size - 1
            var size = currentSize
            while (size > maxSize && i >= 0) {
                members[i].tryResize(maxSize - size)
                size = currentSize
                i--
            }
        }
    }

    private fun toDP(pixel: Int, pixelDensity: Float): Dp {
        return (pixel / pixelDensity).roundToInt().dp
    }

    @Composable
    fun ResizableRow(
        members: Int,
        separatorWidth: Dp? = null,
        modifier: Modifier = Modifier,
        content: @Composable (RowScope.(AreaState) -> Unit)
    ) {
        assert(members >= 2)
        val state = remember { AreaState(members, separatorWidth) }
        val pixelDensity = LocalDensity.current.density
        Box(modifier = modifier.onGloballyPositioned { state.constraint(toDP(it.size.width, pixelDensity)) }) {
            Row(modifier = Modifier.fillMaxWidth()) { content(state) }
            Row(modifier = Modifier.fillMaxWidth()) {
                state.members.filter { !it.isLast }.forEach {
                    Box(Modifier.width(it.nonDraggableSize))
                    VerticalSeparator(it, separatorWidth)
                }
            }
        }
    }

    @Composable
    fun ResizableColumn(
        members: Int,
        separatorHeight: Dp? = null,
        modifier: Modifier = Modifier,
        content: @Composable (ColumnScope.(AreaState) -> Unit)
    ) {
        assert(members >= 2)
        val state = remember { AreaState(members, separatorHeight) }
        val pixelDensity = LocalDensity.current.density
        Box(modifier = modifier.onGloballyPositioned { state.constraint(toDP(it.size.height, pixelDensity)) }) {
            Column(modifier = Modifier.fillMaxHeight()) { content(state) }
            Column(modifier = Modifier.fillMaxHeight()) {
                state.members.filter { !it.isLast }.forEach {
                    Box(Modifier.height(it.nonDraggableSize))
                    HorizontalSeparator(it, separatorHeight)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun VerticalSeparator(memberState: MemberState, separatorWidth: Dp?) {
        if (memberState.freezeSize != null) {
            if (separatorWidth != null) Box(modifier = Modifier.fillMaxHeight().width(separatorWidth))
        } else {
            val pixelDensity = LocalDensity.current.density
            Box(
                modifier = Modifier.fillMaxHeight()
                    .width(if (separatorWidth != null) DRAG_SIZE + separatorWidth else DRAG_SIZE)
                    .pointerHoverIcon(icon = PointerIcon(Cursor(E_RESIZE_CURSOR)))
                    .draggable(orientation = Horizontal, state = rememberDraggableState {
                        memberState.tryResizeSelfAndNext((it / pixelDensity).roundToInt().dp)
                    })
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun HorizontalSeparator(memberState: MemberState, separatorHeight: Dp?) {
        if (memberState.freezeSize != null) {
            if (separatorHeight != null) Box(modifier = Modifier.fillMaxWidth().height(separatorHeight))
        } else {
            val pixelDensity = LocalDensity.current.density
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(if (separatorHeight != null) DRAG_SIZE + separatorHeight else DRAG_SIZE)
                    .pointerHoverIcon(icon = PointerIcon(Cursor(N_RESIZE_CURSOR)))
                    .draggable(orientation = Vertical, state = rememberDraggableState {
                        memberState.tryResizeSelfAndNext((it / pixelDensity).roundToInt().dp)
                    })
            )
        }
    }
}