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

package com.vaticle.typedb.studio.view.editor

import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.view.common.Util.toDP
import java.lang.Integer.min
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.floor

/**
 * LazyLines is a custom variant of of Compose' native
 * [androidx.compose.foundation.lazy.LazyColumn]. This library is different from
 * that of Compose' in that it is much simpler and lightweight: every entry in
 * the column has the same, fixed height, and uses the same lambda to produced a
 * [androidx.compose.runtime.Composable]
 */
internal object LazyLines {

    internal class ScrollState internal constructor(
        private val lineHeightUnscaled: Dp, var bottomSpace: Dp, val lineCount: () -> Int
    ) : ScrollbarAdapter {
        private val onScrollToBottom = LinkedBlockingQueue<() -> Unit>()
        private var _offset: Dp by mutableStateOf(0.dp)
        private var _stickToBottom by mutableStateOf(false)
        internal var viewHeight: Dp by mutableStateOf(0.dp)
        internal val lineHeight: Dp get() = lineHeightUnscaled * GlobalState.editor.scale
        private val contentHeight: Dp get() = lineHeight * lineCount() + bottomSpace
        private val maxOffset: Dp get() = max(contentHeight - viewHeight, 0.dp)
        internal val offset: Dp get() = if (!stickToBottom) _offset.coerceAtMost(maxOffset) else maxOffset
        internal val firstVisibleIndex: Int get() = floor(offset.value / lineHeight.value).toInt()
        internal val firstVisibleOffset: Dp get() = offset - lineHeight * firstVisibleIndex
        internal val lastVisibleIndexPossible: Int
            get() {
                val lineArea = viewHeight.value + firstVisibleOffset.value
                return firstVisibleIndex + floor(lineArea / lineHeight.value).toInt()
            }
        internal var stickToBottom
            get() = _stickToBottom
            set(value) {
                if (_stickToBottom && !value) _offset = maxOffset
                _stickToBottom = value
            }

        override val scrollOffset: Float get() = offset.value

        override fun maxScrollOffset(containerSize: Int): Float {
            return contentHeight.value - viewHeight.value
        }

        override suspend fun scrollTo(containerSize: Int, scrollOffset: Float) {
            updateOffset(scrollOffset.dp)
        }

        fun onScrollToBottom(function: () -> Unit) {
            onScrollToBottom.put(function)
        }

        fun scrollToTop() {
            updateOffset(0.dp)
        }

        fun scrollToBottom() {
            updateOffset(contentHeight - viewHeight)
        }

        fun updateOffsetBy(delta: Dp) {
            updateOffset(offset + delta)
        }

        @OptIn(ExperimentalComposeUiApi::class)
        internal fun updateOffset(event: MouseScrollEvent): Boolean {
            if (event.delta !is MouseScrollUnit.Line || event.orientation != MouseScrollOrientation.Vertical) return false
            updateOffsetBy(lineHeight * (event.delta as MouseScrollUnit.Line).value)
            return true
        }

        private fun updateOffset(newOffset: Dp) {
            _offset = newOffset.coerceIn(0.dp, maxOffset)
            if (newOffset >= maxOffset) onScrollToBottom.forEach { it() }
        }
    }

    data class State<T : Any> internal constructor(
        internal val lines: List<T>,
        internal val scroller: ScrollState
    )

    fun createScrollState(lineHeight: Dp, bottomSpace: Dp = 0.dp, lineCount: () -> Int): ScrollState {
        return ScrollState(lineHeight, bottomSpace, lineCount)
    }

    fun <T : Any> createState(lines: List<T>, scroller: ScrollState): State<T> {
        return State(lines, scroller)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun <T : Any> Area(
        state: State<T>,
        onScroll: () -> Unit,
        modifier: Modifier = Modifier,
        lineFn: @Composable (index: Int, item: T) -> Unit
    ) {
        val density = LocalDensity.current.density
        Box(modifier = modifier.fillMaxHeight().clipToBounds()
            .mouseScrollFilter { event, _ -> onScroll(); state.scroller.updateOffset(event) }
            .onSizeChanged { state.scroller.viewHeight = toDP(it.height, density) }) {
            if (state.lines.isNotEmpty()) {
                val lastVisibleIndex = min(state.scroller.lastVisibleIndexPossible, state.scroller.lineCount() - 1)
                (state.scroller.firstVisibleIndex..lastVisibleIndex).forEach { i ->
                    val indexInView = i - state.scroller.firstVisibleIndex
                    val offset = state.scroller.lineHeight * indexInView - state.scroller.firstVisibleOffset
                    Box(Modifier.offset(y = offset)) { lineFn(i, state.lines[i]) }
                }
            }
        }
    }
}