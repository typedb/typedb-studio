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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.view.common.Context.LocalTitleBarHeight
import com.vaticle.typedb.studio.view.common.Context.LocalWindow
import com.vaticle.typedb.studio.view.common.Util.mousePoint
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.theme.Theme
import java.awt.Cursor

object Frame {

    private val DRAGGABLE_BAR_SIZE = 8.dp
    private val PANE_MIN_SIZE = 10.dp

    data class SeparatorArgs(val size: Dp, val color: @Composable () -> Color = { Theme.colors.border })

    data class Pane constructor(
        val id: String,
        val order: Int = 0,
        val minSize: Dp = PANE_MIN_SIZE,
        val initSize: Either<Dp, Float>,
        val initFreeze: Boolean = false,
        val content: @Composable (PaneState) -> Unit
    )

    class PaneState internal constructor(
        internal var index: Int,
        val frame: FrameState,
        val id: String,
        val order: Int,
        val minSize: Dp = PANE_MIN_SIZE,
        val initSize: Either<Dp, Float> = Either.second(1f),
        initFreeze: Boolean = false,
        val content: @Composable (PaneState) -> Unit
    ) {
        internal val isFirst: Boolean get() = index == 0
        internal val isLast: Boolean get() = index == frame.panes.size - 1
        internal val previous: PaneState? get() = if (isFirst) null else frame.panes[index - 1]
        internal val next: PaneState? get() = if (isLast) null else frame.panes[index + 1]
        private var start: Dp by mutableStateOf(0.dp)
        private var end: Dp by mutableStateOf(0.dp)
        var isFrozen by mutableStateOf(initFreeze)
        var size: Dp by mutableStateOf(0.dp)

        private val maxResize: Dp get() = if (isFrozen) 0.dp else size - minSize
        internal val nonDraggableSize: Dp
            get() = if (isFrozen) size
            else (size - (if (isFirst || isLast) (DRAGGABLE_BAR_SIZE / 2) else DRAGGABLE_BAR_SIZE))

        fun freeze(newSize: Dp) {
            tryResizeSelfAndAdjacentBy(newSize - size, true)
            isFrozen = true
        }

        fun unfreeze(newSize: Dp) {
            isFrozen = false
            tryResizeSelfAndAdjacentBy(newSize - size)
        }

        fun hasBeenResized() {
            frame.isManuallyResized = true
        }

        internal fun updatePosition(start: Dp, end: Dp) {
            this.start = start - frame.start
            this.end = end - frame.start
        }

        internal fun dragResizerBy(delta: Dp, mousePos: Dp) {
            val relMousePos = mousePos - frame.start
            if ((delta > 0.dp && relMousePos > start) || (delta < 0.dp && relMousePos < end)) {
                tryResizeSelfAndNextBy(delta)
            }
        }

        private fun tryResizeSelfAndAdjacentBy(delta: Dp, overrideBounds: Boolean = false) {
            if (isFrozen) return
            if (frame.panes.size == 1) tryResizeSelfBy(delta, overrideBounds)
            else if (isLast) tryResizeSelfAndPreviousBy(delta, overrideBounds)
            else tryResizeSelfAndNextBy(delta, overrideBounds)
        }

        internal fun tryResizeSelfBy(delta: Dp, overrideConstraints: Boolean = false) {
            if (isFrozen) return
            size += delta
            if (!overrideConstraints) size.coerceAtLeast(minSize)
        }

        private fun tryResizeSelfAndNextBy(delta: Dp, overrideBounds: Boolean = false) {
            if (isFrozen) return
            assert(!isLast && next != null)
            val cappedDelta = if (overrideBounds) delta else delta.coerceIn(minSize - size, next!!.maxResize)
            size += cappedDelta
            next!!.size -= cappedDelta
        }

        private fun tryResizeSelfAndPreviousBy(delta: Dp, overrideBounds: Boolean = false) {
            if (isFrozen) return
            assert(isLast && previous != null)
            val cappedDelta = if (overrideBounds) delta else delta.coerceIn(minSize - size, previous!!.maxResize)
            size += cappedDelta
            previous!!.size -= cappedDelta
        }
    }

    class FrameState internal constructor(internal val separator: SeparatorArgs?) {
        var maxSize: Dp by mutableStateOf(0.dp)
        internal var start by mutableStateOf(0.dp)
        internal var panes: List<PaneState> by mutableStateOf(emptyList())
        internal var isManuallyResized: Boolean = false
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
                    index = 0, frame = this, id = it.id, order = it.order, minSize = it.minSize,
                    initSize = it.initSize, initFreeze = it.initFreeze, content = it.content
                )
            }).sortedBy { it.order }.onEachIndexed { i, pane -> pane.index = i }
            mayInitialiseSizes()
        }

        internal fun updateSize(start: Dp, newSize: Dp) {
            this.start = start
            if (maxSize != newSize) {
                maxSize = newSize
                if (!isManuallyResized) mayInitialiseSizes()
                mayShrinkOrExpandSizes()
            }
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
                panes[i].tryResizeSelfBy(maxSize - size)
                size = currentSize
                i--
            }
            i = panes.size - 1
            while (size < maxSize && i >= 0) {
                panes[i].tryResizeSelfBy(maxSize - size)
                size = currentSize
                i--
            }
        }
    }

    fun createFrameState(separator: SeparatorArgs? = null, vararg panes: Pane): FrameState {
        return FrameState(separator).also { it.sync(panes.toList()) }
    }

    @Composable
    fun Row(modifier: Modifier = Modifier, separator: SeparatorArgs? = null, vararg panes: Pane) {
        Row(remember { FrameState(separator) }.also { it.sync(panes.toList()) }, modifier)
    }

    @Composable
    fun Row(state: FrameState, modifier: Modifier = Modifier) {
        val density = LocalDensity.current.density
        Box(modifier = modifier.onGloballyPositioned {
            val bounds = it.boundsInWindow()
            state.updateSize(toDP(bounds.left, density), toDP(bounds.width, density))
        }) {
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
        Column(remember { FrameState(separator) }.also { it.sync(panes.toList()) }, modifier)
    }

    @Composable
    fun Column(state: FrameState, modifier: Modifier = Modifier) {
        val density = LocalDensity.current.density
        Box(modifier = modifier.onGloballyPositioned {
            val bounds = it.boundsInWindow()
            state.updateSize(toDP(bounds.top, density), toDP(bounds.height, density))
        }) {
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
    private fun RowPaneResizer(pane: PaneState, separatorWidth: Dp?) {
        if (!pane.isFrozen) {
            val density = LocalDensity.current.density
            val window = LocalWindow.current!!
            val titleBarHeight = LocalTitleBarHeight.current
            Box(
                modifier = Modifier.fillMaxHeight()
                    .width(if (separatorWidth != null) DRAGGABLE_BAR_SIZE + separatorWidth else DRAGGABLE_BAR_SIZE)
                    .pointerHoverIcon(icon = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                    .onGloballyPositioned {
                        val bounds = it.boundsInWindow()
                        pane.updatePosition(toDP(bounds.left, density), toDP(bounds.right, density))
                    }.draggable(orientation = Orientation.Horizontal, state = rememberDraggableState {
                        pane.frame.isManuallyResized = true
                        pane.dragResizerBy(toDP(it, density), mousePoint(window, titleBarHeight).x.dp)
                    })
            )
        } else if (separatorWidth != null) Box(modifier = Modifier.fillMaxHeight().width(separatorWidth))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ColumnPaneResizer(pane: PaneState, separatorHeight: Dp?) {
        if (!pane.isFrozen) {
            val density = LocalDensity.current.density
            val window = LocalWindow.current!!
            val titleBarHeight = LocalTitleBarHeight.current
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(if (separatorHeight != null) DRAGGABLE_BAR_SIZE + separatorHeight else DRAGGABLE_BAR_SIZE)
                    .pointerHoverIcon(icon = PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR)))
                    .onGloballyPositioned {
                        val bounds = it.boundsInWindow()
                        pane.updatePosition(toDP(bounds.top, density), toDP(bounds.bottom, density))
                    }.draggable(orientation = Orientation.Vertical, state = rememberDraggableState {
                        pane.frame.isManuallyResized = true
                        pane.dragResizerBy(toDP(it, density), mousePoint(window, titleBarHeight).y.dp)
                    })
            )
        } else if (separatorHeight != null) Box(modifier = Modifier.fillMaxWidth().height(separatorHeight))
    }
}