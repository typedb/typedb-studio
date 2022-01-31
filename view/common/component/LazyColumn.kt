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
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import java.lang.Integer.min
import kotlin.math.floor

/**
 * A custom LazyColumn library -- a variant of Compose' native
 * [androidx.compose.foundation.lazy.LazyColumn]. This library is different from
 * that of Compose' in that it is much simpler and lightweight: every entry in
 * the column has the same, fixed height, and uses the same lambda to produced a
 * [androidx.compose.runtime.Composable]
 */
object LazyColumn {

    class ScrollState internal constructor(val itemHeight: Dp, val itemCount: () -> Int): ScrollbarAdapter {
        var offset: Dp by mutableStateOf(0.dp); private set
        private val contentHeight: Dp get() = itemHeight * itemCount()
        private var viewHeight: Dp by mutableStateOf(0.dp)
        internal var firstVisibleOffset: Dp by mutableStateOf(0.dp)
        internal var firstVisibleIndex: Int by mutableStateOf(0)
        internal var lastVisibleIndexPossible: Int by mutableStateOf(0)

        override val scrollOffset: Float get() = offset.value

        override fun maxScrollOffset(containerSize: Int): Float {
            return contentHeight.value - viewHeight.value
        }

        override suspend fun scrollTo(containerSize: Int, scrollOffset: Float) {
            updateOffset(scrollOffset.dp)
        }

        fun updateOffsetBy(delta: Dp) {
            updateOffset(offset + delta)
        }

        @OptIn(ExperimentalComposeUiApi::class)
        internal fun updateOffset(event: MouseScrollEvent): Boolean {
            if (event.delta !is MouseScrollUnit.Line || event.orientation != MouseScrollOrientation.Vertical) return false
            updateOffsetBy(itemHeight * (event.delta as MouseScrollUnit.Line).value)
            return true
        }

        private fun updateOffset(newOffset: Dp) {
            offset = newOffset.coerceIn(0.dp, max(contentHeight - viewHeight, 0.dp))
            updateView()
        }

        internal fun updateViewHeight(newHeight: Dp) {
            viewHeight = newHeight
            updateView()
        }

        private fun updateView() {
            firstVisibleIndex = floor(offset.value / itemHeight.value).toInt()
            firstVisibleOffset = offset - itemHeight * firstVisibleIndex
            val visibleItems = floor((viewHeight.value + firstVisibleOffset.value) / itemHeight.value).toInt()
            lastVisibleIndexPossible = firstVisibleIndex + visibleItems
        }
    }

    data class State<T : Any> internal constructor(
        internal val items: List<T>,
        internal val scroller: ScrollState
    )

    fun createScrollState(itemHeight: Dp, itemCount: () -> Int): ScrollState {
        return ScrollState(itemHeight, itemCount)
    }

    fun <T : Any> createState(items: List<T>, itemHeight: Dp): State<T> {
        return State(items, createScrollState(itemHeight) { items.size })
    }

    fun <T : Any> createState(items: List<T>, scroller: ScrollState): State<T> {
        return State(items, scroller)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun <T : Any> Area(
        state: State<T>,
        modifier: Modifier = Modifier,
        itemFn: @Composable (index: Int, item: T) -> Unit
    ) {
        val density = LocalDensity.current.density
        Box(modifier = modifier.fillMaxHeight().clipToBounds()
            .mouseScrollFilter { event, _ -> state.scroller.updateOffset(event) }
            .onSizeChanged { state.scroller.updateViewHeight(toDP(it.height, density)) }) {
            if (state.items.isNotEmpty()) {
                val lastVisibleIndex = min(state.scroller.lastVisibleIndexPossible, state.scroller.itemCount() - 1)
                (state.scroller.firstVisibleIndex..lastVisibleIndex).forEach { i ->
                    val indexInView = i - state.scroller.firstVisibleIndex
                    val offset = state.scroller.itemHeight * indexInView - state.scroller.firstVisibleOffset
                    Box(Modifier.offset(y = offset)) { itemFn(i, state.items[i]) }
                }
            }
        }
    }
}