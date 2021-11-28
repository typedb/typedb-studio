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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.common.theme.Theme.toDP
import java.awt.Cursor
import java.awt.Cursor.E_RESIZE_CURSOR
import java.awt.Cursor.N_RESIZE_CURSOR

object Layout {

    private val DRAG_SIZE = 6.dp
    private val MIN_SIZE = 10.dp

    interface Separator {
        val size: Dp
        val composable: @Composable () -> Unit

        companion object {
            fun of(size: Dp, composable: @Composable () -> Unit): Separator {
                return object : Separator {
                    override val size = size
                    override val composable = composable
                }
            }
        }
    }

    interface Member {
        val size: Either<Dp, Float>
        val minSize: Dp
        val composable: @Composable (MemberState) -> Unit

        companion object {
            fun of(
                size: Either<Dp, Float>,
                minSize: Dp = MIN_SIZE,
                composable: @Composable (MemberState) -> Unit
            ): Member {
                return object : Member {
                    override val size = size
                    override val minSize = minSize
                    override val composable = composable
                }
            }
        }
    }

    class MemberState(
        private val layoutState: AreaState,
        private val index: Int,
        val isLast: Boolean,
        val initSize: Either<Dp, Float>,
        val minSize: Dp,
        val composable: @Composable (MemberState) -> Unit
    ) {

        private val isFirst: Boolean = index == 0
        private val next: MemberState? get() = if (isLast) null else layoutState.members[index + 1]
        private var _size: Dp by mutableStateOf(0.dp)
        internal var freezeSize: Dp? by mutableStateOf(null); private set
        var size: Dp
            get() = freezeSize ?: _size
            set(value) {
                _size = value
            }

        val nonDraggableSize: Dp
            get() = freezeSize ?: (_size - (if (isFirst || isLast) (DRAG_SIZE / 2) else DRAG_SIZE))

        fun tryOverride(delta: Dp) {
            _size += max(delta, minSize - _size)
        }

        fun tryResizeSelfAndNext(delta: Dp) {
            assert(!isLast && next != null)
            layoutState.resized = true
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

    class AreaState(members: List<Member>, private val separatorSize: Dp?) {
        var resized: Boolean = false
        val members: List<MemberState> = members.mapIndexed { i, member ->
            MemberState(this, i, i == members.size - 1, member.size, member.minSize, member.composable)
        }
        private val currentSize: Dp
            get() {
                var size = 0.dp
                members.map { size += it.size }
                separatorSize?.let { size += it * (members.size - 1) }
                return size
            }

        fun onSizeChanged(maxSize: Dp) {
            if (!resized) mayInitialise(maxSize)
            mayShrinkOrExpand(maxSize)
        }

        private fun mayInitialise(maxSize: Dp) {
            var fixedSize: Dp = 0.dp
            members.filter { it.initSize.isFirst }.forEach {
                it.size = it.initSize.first()
                fixedSize += it.initSize.first()
            }
            val weightedMembers = members.filter { it.initSize.isSecond }
            val weightedSize = maxSize - fixedSize
            val weightedTotal = weightedMembers.sumOf { it.initSize.second().toDouble() }.toFloat()
            weightedMembers.forEach { it.size = max(it.minSize, weightedSize * (it.initSize.second() / weightedTotal)) }
        }

        private fun mayShrinkOrExpand(maxSize: Dp) {
            var i = members.size - 1
            var size = currentSize
            // we add 2.dp only to accommodate for rounding errors never reaching equals
            while (size > maxSize + 1.dp && i >= 0) {
                members[i].tryOverride(maxSize - size)
                size = currentSize
                i--
            }
            if (size < maxSize) members.last().tryOverride(maxSize - size)
        }
    }

    @Composable
    fun ResizableRow(modifier: Modifier = Modifier, separator: Separator? = null, vararg members: Member) {
        assert(members.size >= 2)
        val areaState = remember { AreaState(members.toList(), separator?.size) }
        val pixelDensity = LocalDensity.current.density
        Box(modifier = modifier.onSizeChanged { areaState.onSizeChanged(toDP(it.width, pixelDensity)) }) {
            Row(modifier = Modifier.fillMaxSize()) {
                areaState.members.forEach { member ->
                    Box(Modifier.fillMaxHeight().width(member.size)) { member.composable(member) }
                    separator?.let { if (!member.isLast) it.composable() }
                }
            }
            Row(modifier = Modifier.fillMaxSize()) {
                areaState.members.filter { !it.isLast }.forEach {
                    Box(Modifier.fillMaxHeight().width(it.nonDraggableSize))
                    VerticalSeparator(it, separator?.size)
                }
            }
        }
    }

    @Composable
    fun ResizableColumn(modifier: Modifier = Modifier, separator: Separator? = null, vararg members: Member) {
        assert(members.size >= 2)
        val areaState = remember { AreaState(members.toList(), separator?.size) }
        val pixelDensity = LocalDensity.current.density
        Box(modifier = modifier.onSizeChanged { areaState.onSizeChanged(toDP(it.height, pixelDensity)) }) {
            Column(modifier = Modifier.fillMaxSize()) {
                areaState.members.forEach { member ->
                    Box(Modifier.fillMaxWidth().height(member.size)) { member.composable(member) }
                    separator?.let { if (!member.isLast) it.composable() }
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                areaState.members.filter { !it.isLast }.forEach {
                    Box(Modifier.fillMaxWidth().height(it.nonDraggableSize))
                    HorizontalSeparator(it, separator?.size)
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
                        memberState.tryResizeSelfAndNext(toDP(it, pixelDensity))
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
                        memberState.tryResizeSelfAndNext(toDP(it, pixelDensity))
                    })
            )
        }
    }
}