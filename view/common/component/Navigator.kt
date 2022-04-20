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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.Message.View.Companion.EXPAND_LIMIT_REACHED
import com.vaticle.typedb.studio.state.common.Message.View.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.state.common.Navigable
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Util.contains
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.Util.toRectDP
import com.vaticle.typedb.studio.view.common.component.Form.IconArg
import com.vaticle.typedb.studio.view.common.component.Form.IconButtonArg
import com.vaticle.typedb.studio.view.common.component.Form.RawIconButton
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Navigator.ItemState.Container
import com.vaticle.typedb.studio.view.common.theme.Color.FADED_OPACITY
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.INDICATION_HOVER_ALPHA
import com.vaticle.typedb.studio.view.common.theme.Typography
import com.vaticle.typedb.studio.view.common.theme.Typography.Style.BOLD
import com.vaticle.typedb.studio.view.common.theme.Typography.Style.ITALIC
import com.vaticle.typedb.studio.view.common.theme.Typography.Style.UNDERLINE
import java.awt.event.MouseEvent
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.LinkedList
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

object Navigator {

    @OptIn(ExperimentalTime::class)
    private val LIVE_UPDATE_REFRESH_RATE = Duration.seconds(1)
    private val ITEM_HEIGHT = 26.dp
    private val ICON_WIDTH = 20.dp
    private val TEXT_SPACING = 4.dp
    private val AREA_PADDING = 8.dp
    private val BOTTOM_SPACE = 32.dp
    private const val MAX_ITEM_EXPANDED = 5000
    private const val SCROLL_ITEM_OFFSET = 3
    private val LOGGER = KotlinLogging.logger {}

    open class ItemState<T : Navigable.Item<T>> internal constructor(
        val item: T, internal val parent: ItemState<T>?, private val navState: NavigatorState<T>
    ) : Comparable<ItemState<T>> {

        var isExpanded: Boolean by mutableStateOf(false)
        internal val isExpandable: Boolean get() = item.isExpandable
        internal val name get() = item.name
        internal val info get() = item.info
        internal val isBulkExpandable: Boolean get() = item.isBulkExpandable
        internal var entries: List<ItemState<T>> by mutableStateOf(emptyList())
        internal var focusReq = FocusRequester()
        internal var next: ItemState<T>? by mutableStateOf(null)
        internal var previous: ItemState<T>? by mutableStateOf(null)
        internal var index: Int by mutableStateOf(0)
        internal var depth: Int by mutableStateOf(0)
        private var buttonArea: Rect? by mutableStateOf(null)

        override fun compareTo(other: ItemState<T>): Int {
            return item.compareTo(other.item)
        }

        internal fun isHoverButton(x: Int, y: Int): Boolean {
            return isExpandable && buttonArea?.contains(x, y) ?: false
        }

        private fun navigables(depth: Int): List<ItemState<T>> {
            val list: MutableList<ItemState<T>> = mutableListOf(this)
            if (isExpanded) list.addAll(children(depth + 1))
            return list
        }

        protected fun children(depth: Int): List<ItemState<T>> {
            return entries.onEach { it.depth = depth }.map { it.navigables(depth) }.flatten()
        }

        fun toggle() {
            if (isExpanded) collapse()
            else expand()
        }

        fun collapse(recomputeNavigator: Boolean = true) {
            isExpanded = false
            if (recomputeNavigator) navState.recomputeList()
        }

        fun expand() {
            expand(true)
        }

        internal fun expand(recomputeNavigator: Boolean) {
            expand(recomputeNavigator, 1)
        }

        internal fun expand(recomputeNavigator: Boolean, depth: Int) {
            expand(1, depth)
            if (recomputeNavigator) navState.recomputeList()
        }

        private fun expand(currentDepth: Int, maxDepth: Int) {
            isExpanded = true
            reloadEntries()
            if (currentDepth < maxDepth) entries.forEach { it.expand(currentDepth + 1, maxDepth) }
        }

        internal fun updateButtonArea(rawRectangle: Rect) {
            buttonArea = toRectDP(rawRectangle, navState.density)
        }

        internal fun reloadEntries() {
            item.reloadEntries()
            val new = item.entries.toSet()
            val old = entries.map { it.item }.toSet()
            if (new != old) {
                val deleted = old - new
                val added = new - old
                val updatedEntries = entries.filter { !deleted.contains(it.item) } +
                        added.map { ItemState(it, this, navState) }.toList()
                entries = updatedEntries.sorted()
            }
            entries.filter { it.isExpanded }.forEach { it.reloadEntries() }
        }

        internal fun checkForUpdate(recomputeNavigator: Boolean): Boolean {
            var hasUpdate = false
            if (!isExpanded) return hasUpdate
            item.reloadEntries()
            if (item.entries.toSet() != entries.map { it.item }.toSet()) {
                reloadEntries()
                hasUpdate = true
            }
            entries.filter { it.isExpanded }.forEach {
                if (it.checkForUpdate(false)) hasUpdate = true
            }

            if (hasUpdate && recomputeNavigator) navState.recomputeList()
            return hasUpdate
        }

        override fun toString(): String {
            return "Navigable Item: $name"
        }

        internal class Container<T : Navigable.Item<T>> internal constructor(
            item: Navigable.Container<T>, navState: NavigatorState<T>
        ) : ItemState<T>(item as T, null, navState) {

            fun navigables(): List<ItemState<T>> {
                return children(0)
            }

            override fun toString(): String {
                return "Navigable Container: $name"
            }
        }
    }

    class NavigatorState<T : Navigable.Item<T>> internal constructor(
        container: Navigable.Container<T>,
        private val title: String,
        private val initExpandDepth: Int,
        private val liveUpdate: Boolean,
        private val openFn: (ItemState<T>) -> Unit,
        private var coroutineScope: CoroutineScope
    ) {
        private var container: Container<T> by mutableStateOf(Container(container, this))
        internal var entries: List<ItemState<T>> by mutableStateOf(emptyList()); private set
        internal var density by mutableStateOf(0f)
        private var itemWidth by mutableStateOf(0.dp)
        private var areaWidth by mutableStateOf(0.dp)
        internal val minWidth get() = max(itemWidth, areaWidth)
        internal var selected: ItemState<T>? by mutableStateOf(null); private set
        internal var hovered: ItemState<T>? by mutableStateOf(null)
        internal var scroller = LazyListState(0, 0)
        internal val contextMenu = ContextMenu.State()
        val buttons: List<IconButtonArg> = listOf(
            IconButtonArg(icon = Icon.Code.CHEVRONS_DOWN, tooltip = Tooltip.Arg(title = Label.EXPAND)) { expandAll() },
            IconButtonArg(icon = Icon.Code.CHEVRONS_UP, tooltip = Tooltip.Arg(title = Label.COLLAPSE)) { collapse() }
        )

        init {
            initialiseContainer()
        }

        private fun initialiseContainer() = coroutineScope.launch {
            container.expand(false, 1 + initExpandDepth)
            if (liveUpdate) launchWatcher(container)
            recomputeList()
        }

        fun replaceContainer(newContainer: Navigable.Container<T>) {
            container = Container(newContainer, this)
            initialiseContainer()
        }

        @OptIn(ExperimentalTime::class)
        private fun launchWatcher(root: Container<T>) {
            coroutineScope.launch(IO) {
                try {
                    do {
                        delay(LIVE_UPDATE_REFRESH_RATE) // TODO: is there better way?
                        root.checkForUpdate(true)
                    } while (root == container)
                } catch (e: CancellationException) {
                } catch (e: java.lang.Exception) {
                    GlobalState.notification.systemError(LOGGER, e, UNEXPECTED_ERROR)
                }
            }
        }

        private fun expandAll() = coroutineScope.launch(IO) {
            var i = 0
            fun filter(el: List<ItemState<T>>) = el.filter { it.isBulkExpandable }
            val queue = LinkedList(filter(container.entries))
            while (queue.isNotEmpty() && i < MAX_ITEM_EXPANDED) {
                val item = queue.pop()
                item.expand(false)
                i += 1 + item.entries.count { !it.isExpandable }
                queue.addAll(filter(item.entries))
            }
            recomputeList()
            if (!queue.isEmpty()) {
                GlobalState.notification.userWarning(LOGGER, EXPAND_LIMIT_REACHED, title, MAX_ITEM_EXPANDED)
            }
        }

        private fun collapse() = coroutineScope.launch(IO) {
            val queue = LinkedList(container.entries)
            while (queue.isNotEmpty()) {
                val item = queue.pop()
                item.collapse(false)
                queue.addAll(item.entries.filter { it.isExpanded })
            }
            recomputeList()
        }

        fun reloadEntriesAndExpand(depth: Int) = coroutineScope.launch(IO) {
            container.reloadEntries()
            container.expand(false, 1 + depth)
            recomputeList()
        }

        fun reloadEntries() = coroutineScope.launch(IO) {
            container.reloadEntries()
            recomputeList()
        }

        internal fun recomputeList() {
            var previous: ItemState<T>? = null
            entries = container.navigables().onEachIndexed { i, item ->
                previous?.let { it.next = item }
                item.previous = previous
                item.index = i
                previous = item
            }
            previous?.next = null
        }

        internal fun updateAreaWidth(newRawWidth: Int) {
            areaWidth = toDP(newRawWidth, density)
        }

        internal fun mayIncreaseItemWidth(newRawWidth: Int) {
            val newWidth = toDP(newRawWidth, density)
            if (newWidth > itemWidth) itemWidth = newWidth
        }

        internal fun open(item: ItemState<T>) {
            openFn(item)
        }

        internal fun selectNext(item: ItemState<T>) {
            item.next?.let { select(it) }
        }

        internal fun selectPrevious(item: ItemState<T>) {
            item.previous?.let { select(it) }
        }

        internal fun selectParent(item: ItemState<T>) {
            item.parent?.let { select(it) }
        }

        internal fun select(item: ItemState<T>) {
            selected = item
            mayScrollToAndFocusOnSelected()
        }

        private fun mayScrollToAndFocusOnSelected() {
            var scrollTo = -1
            val layout = scroller.layoutInfo
            if (layout.visibleItemsInfo.isNotEmpty()) {
                val visible = max(layout.visibleItemsInfo.size - 1, 1)
                val offset = min(SCROLL_ITEM_OFFSET, floor(visible / 2.0).toInt())
                val firstInc = scroller.firstVisibleItemIndex
                val startInc = firstInc + offset
                val lastExc = firstInc + visible
                val endExc = lastExc - offset
                val target = selected!!.index

                if (target < startInc) scrollTo = max(target - offset, 0)
                else if (target >= endExc) scrollTo = min(target + offset + 1, layout.totalItemsCount) - visible
            }

            if (scrollTo >= 0) {
                coroutineScope.launch {
                    if (scrollTo >= 0) scroller.animateScrollToItem(scrollTo)
                    selected!!.focusReq.requestFocus()
                }
            } else selected!!.focusReq.requestFocus()
        }
    }

    @Composable
    fun <T : Navigable.Item<T>> rememberNavigatorState(
        container: Navigable.Container<T>, title: String, initExpandDepth: Int,
        liveUpdate: Boolean = false, openFn: (ItemState<T>) -> Unit
    ): NavigatorState<T> {
        val coroutineScope = rememberCoroutineScope()
        return remember { NavigatorState(container, title, initExpandDepth, liveUpdate, openFn, coroutineScope) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun <T : Navigable.Item<T>> Layout(
        state: NavigatorState<T>,
        iconArg: (ItemState<T>) -> IconArg,
        styleArgs: ((ItemState<T>) -> List<Typography.Style>) = { listOf() },
        contextMenuFn: ((item: ItemState<T>, onChangeEntries: () -> Unit) -> List<List<ContextMenu.Item>>)? = null
    ) {
        val density = LocalDensity.current.density
        val horScrollState = rememberScrollState()
        if (state.entries.isNotEmpty()) {
            val root = state.entries.first()
            Box(modifier = Modifier.fillMaxSize().pointerInput(root) { onPointerInput(state, root) }.onGloballyPositioned {
                state.density = density
                state.updateAreaWidth(it.size.width)
            }) {
                contextMenuFn?.let { ContextMenu.Popup(state.contextMenu) { it(state.selected!!) { state.reloadEntries() } } }
                LazyColumn(
                    state = state.scroller, modifier = Modifier.widthIn(min = state.minWidth)
                        .horizontalScroll(state = horScrollState)
                        .pointerMoveFilter(onExit = { state.hovered = null; false })
                ) {
                    state.entries.forEach { item { ItemLayout(state, it, iconArg, styleArgs) } }
                    item { Spacer(Modifier.height(BOTTOM_SPACE)) }
                }
                Scrollbar.Vertical(rememberScrollbarAdapter(state.scroller), Modifier.align(Alignment.CenterEnd))
                Scrollbar.Horizontal(rememberScrollbarAdapter(horScrollState), Modifier.align(Alignment.BottomCenter))
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
    @Composable
    private fun <T : Navigable.Item<T>> ItemLayout(
        state: NavigatorState<T>, item: ItemState<T>, iconArg: (ItemState<T>) -> IconArg,
        styleArgs: (ItemState<T>) -> List<Typography.Style>
    ) {
        val styles = styleArgs(item)
        val bgColor = when {
            state.selected == item -> Theme.colors.primary
            state.hovered == item -> Theme.colors.indicationBase.copy(INDICATION_HOVER_ALPHA)
            else -> Color.Transparent
        }

        fun onDoublePrimaryReleased(event: MouseEvent) {
            if (event.button == 1 && event.clickCount == 2 && !item.isHoverButton(event.x, event.y)) state.open(item)
        }

        Row(
            modifier = Modifier.background(color = bgColor)
                .alpha(if (styles.contains(Typography.Style.FADED)) FADED_OPACITY else 1f)
                .widthIn(min = state.minWidth).height(ITEM_HEIGHT)
                .focusRequester(item.focusReq).focusable()
                .onKeyEvent { onKeyEvent(it, state, item) }
                .pointerHoverIcon(PointerIconDefaults.Hand)
                .pointerInput(item) { onPointerInput(state, item) }
                .onPointerEvent(Release) { onDoublePrimaryReleased(it.awtEvent) }
                .pointerMoveFilter(onEnter = { state.hovered = item; false })
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.onGloballyPositioned { state.mayIncreaseItemWidth(it.size.width) }
            ) {
                if (item.depth > 0) Spacer(modifier = Modifier.width(ICON_WIDTH * item.depth))
                ItemButton(item)
                ItemIcon(item, iconArg)
                Spacer(Modifier.width(TEXT_SPACING))
                ItemText(item, styles)
                Spacer(modifier = Modifier.width(AREA_PADDING))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun <T : Navigable.Item<T>> ItemButton(item: ItemState<T>) {
        if (item.isExpandable) RawIconButton(
            icon = if (item.isExpanded) Icon.Code.CHEVRON_DOWN else Icon.Code.CHEVRON_RIGHT,
            onClick = { item.toggle() },
            modifier = Modifier.size(ITEM_HEIGHT).onGloballyPositioned {
                item.updateButtonArea(it.boundsInWindow())
            },
        ) else Spacer(Modifier.size(ITEM_HEIGHT))
    }

    @Composable
    private fun <T : Navigable.Item<T>> ItemIcon(item: ItemState<T>, iconArg: (ItemState<T>) -> IconArg) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ICON_WIDTH)) {
            Icon.Render(icon = iconArg(item).code, color = iconArg(item).color())
        }
    }

    @Composable
    private fun <T : Navigable.Item<T>> ItemText(item: ItemState<T>, styleArgs: List<Typography.Style>) {
        Row(modifier = Modifier.height(ICON_WIDTH)) {
            Text(
                value = item.name,
                fontStyle = if (styleArgs.contains(ITALIC)) FontStyle.Italic else null,
                fontWeight = if (styleArgs.contains(BOLD)) FontWeight.SemiBold else null,
                textDecoration = if (styleArgs.contains(UNDERLINE)) TextDecoration.Underline else null
            )
            item.info?.let {
                Spacer(Modifier.width(TEXT_SPACING))
                Text(value = "( $it )", alpha = 0.4f)
            }
        }
    }

    private suspend fun <T : Navigable.Item<T>> PointerInputScope.onPointerInput(
        state: NavigatorState<T>, item: ItemState<T>
    ) {
        state.contextMenu.onPointerInput(
            pointerInputScope = this,
            onSinglePrimaryPressed = { state.select(item); item.focusReq.requestFocus() },
            onSecondaryClick = { state.select(item) }
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun <T : Navigable.Item<T>> onKeyEvent(
        event: KeyEvent, state: NavigatorState<T>, item: ItemState<T>
    ): Boolean {
        return when (event.awtEvent.id) {
            java.awt.event.KeyEvent.KEY_RELEASED -> false
            else -> when (event.key) {
                Key.Enter, Key.NumPadEnter -> {
                    if (state.selected == item) state.open(item)
                    else state.select(item)
                    true
                }
                Key.DirectionLeft -> {
                    if (item.isExpanded) item.collapse()
                    else state.selectParent(item)
                    true
                }
                Key.DirectionRight -> {
                    if (!item.isExpanded) item.expand()
                    else state.selectNext(item)
                    true
                }
                Key.DirectionUp -> {
                    state.selectPrevious(item)
                    true
                }
                Key.DirectionDown -> {
                    state.selectNext(item)
                    true
                }
                else -> false
            }
        }
    }
}
