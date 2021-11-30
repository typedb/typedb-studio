package com.vaticle.typedb.studio.view.common.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
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

object Resizable {

    private val DRAGGABLE_BAR_SIZE = 8.dp
    private val MEMBER_MIN_SIZE = 10.dp

    data class SeparatorArgs(val size: Dp, val color: @Composable () -> Color = { Theme.colors.border })

    data class Item(
        val id: String,
        val initSize: Either<Dp, Float>,
        val minSize: Dp = MEMBER_MIN_SIZE,
        val content: @Composable (ItemState) -> Unit
    )

    class ItemState internal constructor(
        private val layoutState: AreaState,
        private val index: Int,
        val id: String,
        val initSize: Either<Dp, Float> = Either.second(1f),
        val minSize: Dp = MEMBER_MIN_SIZE,
        currentSize: Dp = 0.dp,
        currentFreezeSize: Dp? = null,
        val content: @Composable (ItemState) -> Unit
    ) {
        internal val isFirst: Boolean get() = index == 0
        internal val isLast: Boolean get() = index == layoutState.contents.size - 1
        internal val previous: ItemState? get() = if (isFirst) null else layoutState.contents[index - 1]
        internal val next: ItemState? get() = if (isLast) null else layoutState.contents[index + 1]
        internal var _size: Dp by mutableStateOf(currentSize)
        internal var freezeSize: Dp? by mutableStateOf(currentFreezeSize); private set
        internal var size: Dp
            get() = freezeSize ?: _size
            set(value) {
                _size = value
            }

        internal val nonDraggableSize: Dp
            get() = freezeSize
                ?: (_size - (if (isFirst || isLast) (DRAGGABLE_BAR_SIZE / 2) else DRAGGABLE_BAR_SIZE))

        internal fun tryOverride(delta: Dp) {
            _size += max(delta, minSize - _size)
        }

        internal fun tryResizeSelfAndNext(delta: Dp) {
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

    internal class AreaState(private val separatorSize: Dp?) {
        var resized: Boolean = false
        var maxSize: Dp by mutableStateOf(0.dp)
        var contents: List<ItemState> by mutableStateOf(emptyList())
        private val currentSize: Dp
            get() {
                var size = 0.dp
                contents.map { size += it.size }
                separatorSize?.let { size += it * (contents.size - 1) }
                return size
            }

        internal fun sync(inputs: List<Item>) {
            val inputIDs = inputs.map { it.id }.toSet()
            val removedIDs = contents.map { it.id }.filter { !inputIDs.contains(it) }.toSet()
            if (removedIDs.isNotEmpty()) removeItems(removedIDs)
            if (inputs.size > contents.size) replaceItems(inputs)
        }

        private fun removeItems(removedIDs: Set<String>) {
            contents.filter { removedIDs.contains(it.id) }.forEach {
                if (it.isFirst) it.next!!.size += it.size
                else it.previous!!.size += it.size
            }
            contents = contents.filter { !removedIDs.contains(it.id) }.mapIndexed { i, member ->
                ItemState(
                    layoutState = this, index = i, id = member.id, minSize = member.minSize,
                    currentSize = member._size, currentFreezeSize = member.freezeSize, content = member.content
                )
            }
        }

        private fun replaceItems(inputs: List<Item>) {
            contents = inputs.mapIndexed { i, input ->
                ItemState(
                    layoutState = this, index = i, id = input.id, initSize = input.initSize,
                    minSize = input.minSize, content = input.content
                )
            }
            mayInitialiseSizes()
        }

        internal fun onSizeChanged(newMaxSize: Dp) {
            maxSize = newMaxSize
            if (!resized) mayInitialiseSizes()
            mayShrinkOrExpandSizes()
        }

        private fun mayInitialiseSizes() {
            var fixedSize: Dp = 0.dp
            contents.filter { it.initSize.isFirst }.forEach {
                it.size = it.initSize.first()
                fixedSize += it.initSize.first()
            }
            val weightedItems = contents.filter { it.initSize.isSecond }
            val weightedSize = maxSize - fixedSize
            val weightedTotal = weightedItems.sumOf { it.initSize.second().toDouble() }.toFloat()
            weightedItems.forEach {
                it.size =
                    max(it.minSize, weightedSize * (it.initSize.second() / weightedTotal))
            }
        }

        private fun mayShrinkOrExpandSizes() {
            var i = contents.size - 1
            var size = currentSize
            // we add 1.dp only to accommodate for rounding errors never reaching equals
            while (size > maxSize + 1.dp && i >= 0) {
                contents[i].tryOverride(maxSize - size)
                size = currentSize
                i--
            }
            if (size < maxSize) contents.last().tryOverride(maxSize - size)
        }
    }

    @Composable
    fun Row(
        modifier: Modifier = Modifier,
        separator: SeparatorArgs? = null,
        vararg items: Item
    ) {
        assert(items.size >= 2)
        val pixelDensity = LocalDensity.current.density
        val areaState = remember { AreaState(separator?.size) }
        areaState.sync(items.toList())
        Box(modifier = modifier.onSizeChanged { areaState.onSizeChanged(Theme.toDP(it.width, pixelDensity)) }) {
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxSize()) {
                areaState.contents.forEach { member ->
                    Box(Modifier.fillMaxHeight().width(member.size)) { member.content(member) }
                    separator?.let { if (!member.isLast) Separator.Vertical(it.size, it.color()) }
                }
            }
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxSize()) {
                areaState.contents.filter { !it.isLast }.forEach {
                    Box(Modifier.fillMaxHeight().width(it.nonDraggableSize))
                    RowItemResizer(it, separator?.size)
                }
            }
        }
    }

    @Composable
    fun Column(
        modifier: Modifier = Modifier,
        separator: SeparatorArgs? = null,
        vararg items: Item
    ) {
        assert(items.size >= 2)
        val pixelDensity = LocalDensity.current.density
        val areaState = remember { AreaState(separator?.size) }
        areaState.sync(items.toList())
        Box(modifier = modifier.onSizeChanged { areaState.onSizeChanged(Theme.toDP(it.height, pixelDensity)) }) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                areaState.contents.forEach { member ->
                    Box(Modifier.fillMaxWidth().height(member.size)) { member.content(member) }
                    separator?.let { if (!member.isLast) Separator.Horizontal(it.size, it.color()) }
                }
            }
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                areaState.contents.filter { !it.isLast }.forEach {
                    Box(Modifier.fillMaxWidth().height(it.nonDraggableSize))
                    ColumnItemResizer(it, separator?.size)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun RowItemResizer(itemState: ItemState, separatorWidth: Dp?) {
        if (itemState.freezeSize != null) {
            if (separatorWidth != null) Box(modifier = Modifier.fillMaxHeight().width(separatorWidth))
        } else {
            val pixelDensity = LocalDensity.current.density
            Box(
                modifier = Modifier.fillMaxHeight()
                    .width(if (separatorWidth != null) DRAGGABLE_BAR_SIZE + separatorWidth else DRAGGABLE_BAR_SIZE)
                    .pointerHoverIcon(icon = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                    .draggable(orientation = Orientation.Horizontal, state = rememberDraggableState {
                        itemState.tryResizeSelfAndNext(Theme.toDP(it, pixelDensity))
                    })
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ColumnItemResizer(itemState: ItemState, separatorHeight: Dp?) {
        if (itemState.freezeSize != null) {
            if (separatorHeight != null) Box(modifier = Modifier.fillMaxWidth().height(separatorHeight))
        } else {
            val pixelDensity = LocalDensity.current.density
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(if (separatorHeight != null) DRAGGABLE_BAR_SIZE + separatorHeight else DRAGGABLE_BAR_SIZE)
                    .pointerHoverIcon(icon = PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR)))
                    .draggable(orientation = Orientation.Vertical, state = rememberDraggableState {
                        itemState.tryResizeSelfAndNext(Theme.toDP(it, pixelDensity))
                    })
            )
        }
    }
}