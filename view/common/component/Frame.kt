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

package com.vaticle.typedb.studio.view.common.component

import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.view.common.theme.Theme
import java.awt.Cursor

object Frame {

    private val DRAGGABLE_BAR_SIZE = 8.dp
    private val PANE_MIN_SIZE = 10.dp

    data class SeparatorArgs(val size: Dp, val color: @Composable () -> Color = { Theme.colors.border })

    data class Pane(
        val id: String,
        val initSize: Either<Dp, Float>,
        val minSize: Dp = PANE_MIN_SIZE,
        val order: Int = 0,
        val content: @Composable (PaneState) -> Unit
    )

    class PaneState internal constructor(
        private val frameState: FrameState,
        internal var index: Int,
        val id: String,
        val initSize: Either<Dp, Float> = Either.second(1f),
        val minSize: Dp = PANE_MIN_SIZE,
        val order: Int,
        val content: @Composable (PaneState) -> Unit
    ) {
        internal val isFirst: Boolean get() = index == 0
        internal val isLast: Boolean get() = index == frameState.panes.size - 1
        internal val previous: PaneState? get() = if (isFirst) null else frameState.panes[index - 1]
        internal val next: PaneState? get() = if (isLast) null else frameState.panes[index + 1]
        internal var freezeSize: Dp? by mutableStateOf(null); private set
        private var _size: Dp by mutableStateOf(0.dp)
        internal var size: Dp
            get() = freezeSize ?: _size
            set(value) {
                _size = value
            }

        internal val nonDraggableSize: Dp
            get() = freezeSize ?: (_size - (if (isFirst || isLast) (DRAGGABLE_BAR_SIZE / 2) else DRAGGABLE_BAR_SIZE))

        internal fun tryOverride(delta: Dp) {
            _size += max(delta, minSize - _size)
        }

        internal fun tryResizeSelfAndNext(delta: Dp) {
            assert(!isLast && next != null)
            frameState.resized = true
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

    class FrameState internal constructor(internal val separator: SeparatorArgs?) {
        internal var resized: Boolean = false
        internal var panes: List<PaneState> by mutableStateOf(emptyList())
        private var maxSize: Dp by mutableStateOf(0.dp)
        private val currentSize: Dp
            get() {
                var size = 0.dp
                panes.map { size += it.size }
                separator?.size?.let { size += it * (panes.size - 1) }
                return size
            }

        internal fun sync(inputs: List<Pane>) {
            val new = inputs.map { it.id }.toSet()
            val old = panes.map { it.id }.toSet()
            val deleted = old - new
            val added = new - old
            if (deleted.isNotEmpty()) remove(deleted)
            if (added.isNotEmpty()) add(inputs.filter { added.contains(it.id) })
        }

        private fun remove(deletedPaneIDs: Set<String>) {
            panes.filter { deletedPaneIDs.contains(it.id) }.forEach {
                if (it.isFirst) it.next!!.size += it.size
                else it.previous!!.size += it.size
            }
            panes = panes.filter { !deletedPaneIDs.contains(it.id) }.onEachIndexed { i, pane -> pane.index = i }
        }

        private fun add(added: List<Pane>) {
            panes = (panes + added.map {
                PaneState(
                    frameState = this, index = 0, id = it.id, initSize = it.initSize,
                    minSize = it.minSize, order = it.order, content = it.content
                )
            }).sortedBy { it.order }.onEachIndexed { i, pane -> pane.index = i }
            mayInitialiseSizes()
        }

        internal fun onSizeChanged(newMaxSize: Dp) {
            maxSize = newMaxSize
            if (!resized) mayInitialiseSizes()
            mayShrinkOrExpandSizes()
        }

        private fun mayInitialiseSizes() {
            var fixedSize: Dp = 0.dp
            panes.filter { it.initSize.isFirst }.forEach {
                it.size = it.initSize.first()
                fixedSize += it.initSize.first()
            }
            val weightedPanes = panes.filter { it.initSize.isSecond }
            val weightedTotalSize = maxSize - fixedSize
            val weightedTotalWeight = weightedPanes.sumOf { it.initSize.second().toDouble() }.toFloat()
            weightedPanes.forEach {
                it.size = max(it.minSize, weightedTotalSize * (it.initSize.second() / weightedTotalWeight))
            }
        }

        private fun mayShrinkOrExpandSizes() {
            var i = panes.size - 1
            var size = currentSize
            // we add 1.dp only to accommodate for rounding errors never reaching equals
            while (size > maxSize + 1.dp && i >= 0) {
                panes[i].tryOverride(maxSize - size)
                size = currentSize
                i--
            }
            if (size < maxSize) panes.last().tryOverride(maxSize - size)
        }
    }

    fun createFrameState(separator: SeparatorArgs? = null, vararg panes: Pane): FrameState {
        return FrameState(separator).also { it.sync(panes.toList()) }
    }

    @Composable
    fun Row(modifier: Modifier = Modifier, separator: SeparatorArgs? = null, vararg panes: Pane) {
        assert(panes.size >= 2)
        Row(remember { FrameState(separator) }.also { it.sync(panes.toList()) }, modifier)
    }

    @Composable
    fun Row(state: FrameState, modifier: Modifier = Modifier) {
        val pixelDensity = LocalDensity.current.density
        Box(modifier = modifier.onSizeChanged { state.onSizeChanged(Theme.toDP(it.width, pixelDensity)) }) {
            Row(modifier = Modifier.fillMaxSize()) {
                state.panes.forEach { pane ->
                    Box(Modifier.fillMaxHeight().width(pane.size)) { pane.content(pane) }
                    state.separator?.let { if (!pane.isLast) Separator.Vertical(it.size, it.color()) }
                }
            }
            Row(modifier = Modifier.fillMaxSize()) {
                state.panes.filter { !it.isLast }.forEach {
                    Box(Modifier.fillMaxHeight().width(it.nonDraggableSize))
                    RowPaneResizer(it, state.separator?.size)
                }
            }
        }
    }

    @Composable
    fun Column(modifier: Modifier = Modifier, separator: SeparatorArgs? = null, vararg panes: Pane) {
        assert(panes.size >= 2)
        Column(remember { FrameState(separator) }.also { it.sync(panes.toList()) }, modifier)
    }

    @Composable
    fun Column(state: FrameState, modifier: Modifier = Modifier) {
        val pixelDensity = LocalDensity.current.density
        Box(modifier = modifier.onSizeChanged { state.onSizeChanged(Theme.toDP(it.height, pixelDensity)) }) {
            Column(modifier = Modifier.fillMaxSize()) {
                state.panes.forEach { pane ->
                    Box(Modifier.fillMaxWidth().height(pane.size)) { pane.content(pane) }
                    state.separator?.let { if (!pane.isLast) Separator.Horizontal(it.size, it.color()) }
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                state.panes.filter { !it.isLast }.forEach {
                    Box(Modifier.fillMaxWidth().height(it.nonDraggableSize))
                    ColumnPaneResizer(it, state.separator?.size)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun RowPaneResizer(paneState: PaneState, separatorWidth: Dp?) {
        if (paneState.freezeSize != null) {
            if (separatorWidth != null) Box(modifier = Modifier.fillMaxHeight().width(separatorWidth))
        } else {
            val pixelDensity = LocalDensity.current.density
            Box(
                modifier = Modifier.fillMaxHeight()
                    .width(if (separatorWidth != null) DRAGGABLE_BAR_SIZE + separatorWidth else DRAGGABLE_BAR_SIZE)
                    .pointerHoverIcon(icon = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                    .draggable(orientation = Orientation.Horizontal, state = rememberDraggableState {
                        paneState.tryResizeSelfAndNext(Theme.toDP(it, pixelDensity))
                    })
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ColumnPaneResizer(paneState: PaneState, separatorHeight: Dp?) {
        if (paneState.freezeSize != null) {
            if (separatorHeight != null) Box(modifier = Modifier.fillMaxWidth().height(separatorHeight))
        } else {
            val pixelDensity = LocalDensity.current.density
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(if (separatorHeight != null) DRAGGABLE_BAR_SIZE + separatorHeight else DRAGGABLE_BAR_SIZE)
                    .pointerHoverIcon(icon = PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR)))
                    .draggable(orientation = Orientation.Vertical, state = rememberDraggableState {
                        paneState.tryResizeSelfAndNext(Theme.toDP(it, pixelDensity))
                    })
            )
        }
    }
}