/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.framework.material

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.vaticle.typedb.studio.framework.common.Util.contains
import com.vaticle.typedb.studio.framework.common.Util.toDP
import com.vaticle.typedb.studio.framework.common.Util.toRectDP
import com.vaticle.typedb.studio.framework.common.theme.Color.FADED_OPACITY
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.common.theme.Theme.INDICATION_HOVER_ALPHA
import com.vaticle.typedb.studio.framework.common.theme.Typography
import com.vaticle.typedb.studio.framework.common.theme.Typography.Style.BOLD
import com.vaticle.typedb.studio.framework.common.theme.Typography.Style.ITALIC
import com.vaticle.typedb.studio.framework.common.theme.Typography.Style.UNDERLINE
import com.vaticle.typedb.studio.framework.material.Form.IconArg
import com.vaticle.typedb.studio.framework.material.Form.IconButtonArg
import com.vaticle.typedb.studio.framework.material.Form.RawIconButton
import com.vaticle.typedb.studio.framework.material.Form.Text
import com.vaticle.typedb.studio.service.Service.notification
import com.vaticle.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Message.Framework.Companion.EXPAND_LIMIT_REACHED
import com.vaticle.typedb.studio.service.page.Navigable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.awt.event.MouseEvent
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds

object Navigator {

    sealed class Behaviour(val clicksToOpenItem: Int, val itemsAreFocusable: Boolean) {
        class Browser(clicksToOpenItem: Int = 2) : Behaviour(clicksToOpenItem, true)
        class Menu(clicksToOpenItem: Int = 1) : Behaviour(clicksToOpenItem, false)
    }

    val ITEM_HEIGHT = 26.dp
    private val LIVE_UPDATE_REFRESH_RATE = 1.seconds
    private val ICON_WIDTH = 20.dp
    private val TEXT_SPACING = 4.dp
    private val AREA_PADDING = 8.dp
    private val BOTTOM_SPACE = 32.dp
    private const val MAX_ITEM_EXPANDED = 5000
    private const val SCROLL_ITEM_OFFSET = 3
    private val LOGGER = KotlinLogging.logger {}

    class ItemState<T : Navigable<T>> internal constructor(
        val item: T,
        val parent: ItemState<T>?,
        val navState: NavigatorState<T>,
        val coroutines: CoroutineScope
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

        internal fun isHoverExpandButton(x: Int, y: Int): Boolean {
            return isExpandable && buttonArea?.contains(x, y) ?: false
        }

        internal fun navigables(): List<ItemState<T>> {
            return children(0)
        }

        private fun children(depth: Int): List<ItemState<T>> {
            return entries.onEach { it.depth = depth }.map {
                listOf(it) + if (it.isExpanded) it.children(depth + 1) else emptyList()
            }.flatten()
        }

        fun toggle() {
            if (isExpanded) collapse()
            else expand()
        }

        fun collapse(recomputeNavigator: Boolean = true) {
            isExpanded = false
            if (recomputeNavigator) navState.recomputeList()
        }

        fun expand() = expand(maxDepth = 1)

        internal fun expand(maxDepth: Int) = expandRecursive(1, maxDepth)

        private fun expandRecursive(currentDepth: Int = 1, maxDepth: Int) {
            isExpanded = true
            reloadEntries()
            navState.recomputeList()
            if (currentDepth < maxDepth) entries.forEach {
                coroutines.launchAndHandle(notification, LOGGER) {
                    it.expandRecursive(currentDepth + 1, maxDepth)
                }
            }
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
                        added.map { ItemState(it, this, navState, coroutines) }.toList()
                entries = updatedEntries.sorted()
            } else entries = entries.sorted()
            navState.recomputeList()
            entries.filter { it.isExpanded }.forEach {
                coroutines.launchAndHandle(notification, LOGGER) { it.reloadEntries() }
            }
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
            return "Navigator ItemState: $name"
        }
    }

    class NavigatorState<T : Navigable<T>> constructor(
        container: Navigable<T>,
        private val title: String,
        internal val behaviour: Behaviour,
        private val initExpandDepth: Int = 0,
        private val liveUpdate: Boolean = false,
        private var coroutines: CoroutineScope,
        internal val contextMenuFn: ((item: ItemState<T>) -> List<List<ContextMenu.Item>>)? = null,
        private val openFn: (ItemState<T>) -> Unit
    ) {

        private var container: ItemState<T> = ItemState(container as T, null, this, coroutines)
        internal var entries: List<ItemState<T>> by mutableStateOf(emptyList()); private set
        internal var density by mutableStateOf(0f)
        private var itemWidth by mutableStateOf(0.dp)
        internal var areaWidth by mutableStateOf(0.dp)
        internal var areaHeight by mutableStateOf(0.dp)
        internal val minWidth get() = max(itemWidth, areaWidth)
        internal var selected: ItemState<T>? by mutableStateOf(null); private set
        internal var hovered: ItemState<T>? by mutableStateOf(null)
        internal var scroller = LazyListState(0, 0)
        internal val contextMenu = ContextMenu.State()
        private val isExpanding = AtomicBoolean(false)
        private val isCollapsing = AtomicBoolean(false)
        private val watchUpdate = AtomicBoolean(false)
        val buttons: List<IconButtonArg> = listOf(
            IconButtonArg(Icon.EXPAND, tooltip = Tooltip.Arg(title = Label.EXPAND)) { expandAllAsync() },
            IconButtonArg(Icon.COLLAPSE, tooltip = Tooltip.Arg(title = Label.COLLAPSE)) { collapseAsync() }
        )

        fun reloadEntriesAsync() = coroutines.launchAndHandle(notification, LOGGER) { reloadEntries() }

        fun reloadEntries() = container.reloadEntries()

        fun launchAsync() = coroutines.launchAndHandle(notification, LOGGER) { launch() }

        fun launch() {
            container.expand(1 + initExpandDepth)
            if (liveUpdate) {
                watchUpdate.set(true)
                launchWatcher(container)
            }
        }

        fun replaceContainer(newContainer: Navigable<T>) {
            container = ItemState(newContainer as T, null, this, coroutines)
            launchAsync()
        }

        fun close() {
            watchUpdate.set(false)
        }

        private fun launchWatcher(root: ItemState<T>) = coroutines.launchAndHandle(notification, LOGGER) {
            do {
                root.checkForUpdate(true)
                delay(LIVE_UPDATE_REFRESH_RATE) // TODO: is there better way?
            } while (root == container && watchUpdate.get())
        }

        private fun expandAllAsync() = coroutines.launchAndHandle(notification, LOGGER) {
            var i = 0
            fun filter(el: List<ItemState<T>>) = el.filter { it.isBulkExpandable }
            val queue = LinkedList(filter(container.entries))
            isCollapsing.set(false)
            isExpanding.set(true)
            while (queue.isNotEmpty() && i < MAX_ITEM_EXPANDED && isExpanding.get()) {
                val item = queue.pop()
                item.expand()
                i += 1 + item.entries.count { !it.isExpandable }
                queue.addAll(filter(item.entries))
            }
            isExpanding.set(false)
            if (!queue.isEmpty() && i == MAX_ITEM_EXPANDED) {
                notification.userWarning(LOGGER, EXPAND_LIMIT_REACHED, title, MAX_ITEM_EXPANDED)
            }
        }

        private fun collapseAsync() = coroutines.launchAndHandle(notification, LOGGER) {
            val queue = LinkedList(container.entries)
            isExpanding.set(false)
            isCollapsing.set(true)
            while (queue.isNotEmpty() && isCollapsing.get()) {
                val item = queue.pop()
                item.collapse(false)
                queue.addAll(item.entries.filter { it.isExpanded })
            }
            recomputeList()
            isCollapsing.set(false)
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

        internal fun updateAreaSize(rawSize: IntSize) {
            areaWidth = toDP(rawSize.width, density)
            areaHeight = toDP(rawSize.height, density)
        }

        internal fun mayIncreaseItemWidth(newRawWidth: Int) {
            val newWidth = toDP(newRawWidth, density)
            if (newWidth > itemWidth) itemWidth = newWidth
        }

        internal fun open(item: ItemState<T>) {
            openFn(item)
            if (!behaviour.itemsAreFocusable) hovered = null
        }

        internal fun maySelectNext(item: ItemState<T>) {
            item.next?.let { maySelect(it) }
        }

        internal fun maySelectPrevious(item: ItemState<T>) {
            item.previous?.let { maySelect(it) }
        }

        internal fun maySelectParent(item: ItemState<T>) {
            item.parent?.let { if (it != container) maySelect(it) }
        }

        internal fun maySelect(item: ItemState<T>) {
            if (!behaviour.itemsAreFocusable) return
            selected = item
            item.focusReq.requestFocus()
            mayScrollToAndFocusOnSelected()
        }

        fun maySelectFirstWithoutFocus() {
            if (behaviour.itemsAreFocusable) entries.firstOrNull()?.let { selected = it }
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

            if (scrollTo >= 0) coroutines.launch {
                if (scrollTo >= 0) scroller.animateScrollToItem(scrollTo)
                selected!!.focusReq.requestFocus()
            } else selected!!.focusReq.requestFocus()
        }
    }

    @Composable
    fun <T : Navigable<T>> rememberNavigatorState(
        container: Navigable<T>, title: String, behaviour: Behaviour,
        initExpandDepth: Int = 0, liveUpdate: Boolean = false,
        openFn: (ItemState<T>) -> Unit,
        contextMenuFn: ((item: ItemState<T>) -> List<List<ContextMenu.Item>>)? = null,
        initFn: ((NavigatorState<T>) -> Unit)? = null
    ): NavigatorState<T> {
        val coroutines = rememberCoroutineScope()
        return remember {
            NavigatorState(
                container = container,
                title = title,
                behaviour = behaviour,
                initExpandDepth = initExpandDepth,
                liveUpdate = liveUpdate,
                coroutines = coroutines,
                contextMenuFn = contextMenuFn,
                openFn = openFn
            ).also { initFn?.let { fn -> fn(it) } }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun <T : Navigable<T>> Layout(
        state: NavigatorState<T>,
        modifier: Modifier = Modifier,
        itemHeight: Dp = ITEM_HEIGHT,
        bottomSpace: Dp = BOTTOM_SPACE,
        iconArg: ((ItemState<T>) -> IconArg)? = null,
        styleArgs: ((ItemState<T>) -> List<Typography.Style>) = { listOf() },
    ) {
        val density = LocalDensity.current.density
        val horScrollState = rememberScrollState()
        val verScrollAdapter = rememberScrollbarAdapter(state.scroller)
        val horScrollAdapter = rememberScrollbarAdapter(horScrollState)
        val root: ItemState<T>? = state.entries.firstOrNull()
        if (state.entries.isNotEmpty()) Box(
            modifier = modifier.pointerInput(root) { root?.let { onPointerInput(state, it) } }
                .onGloballyPositioned { state.density = density; state.updateAreaSize(it.size) }
        ) {
            state.contextMenuFn?.let { fn -> ContextMenu.Popup(state.contextMenu) { fn(state.selected!!) } }
            LazyColumn(
                state = state.scroller, modifier = Modifier.widthIn(min = state.minWidth)
                    .horizontalScroll(state = horScrollState)
                    .onPointerEvent(Exit) { state.hovered = null }
            ) {
                state.entries.forEach { item { ItemLayout(state, it, itemHeight, iconArg, styleArgs) } }
                if (bottomSpace > 0.dp) item { Spacer(Modifier.height(bottomSpace)) }
            }
            Scrollbar.Vertical(verScrollAdapter, Modifier.align(Alignment.CenterEnd))
            Scrollbar.Horizontal(horScrollAdapter, Modifier.align(Alignment.BottomCenter))
        } else Box(modifier, Alignment.Center) {
            Text(value = Label.NONE_IN_PARENTHESES.lowercase())
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun <T : Navigable<T>> ItemLayout(
        state: NavigatorState<T>, item: ItemState<T>, itemHeight: Dp,
        iconArg: ((ItemState<T>) -> IconArg)?, styleArgs: (ItemState<T>) -> List<Typography.Style>,
    ) {
        val horizontalItemPadding = if (iconArg == null) Theme.DIALOG_PADDING else 0.dp
        val styles = styleArgs(item)
        val bgColor = when {
            state.selected == item -> Theme.studio.primary
            state.hovered == item -> Theme.studio.indicationBase.copy(INDICATION_HOVER_ALPHA)
            else -> Color.Transparent
        }

        fun mayOpenItem(event: MouseEvent) {
            val isLeftClick = event.button == 1
            val isOpenClickCount = event.clickCount == state.behaviour.clicksToOpenItem
            val isHoverExpandButton = item.isHoverExpandButton(event.x, event.y)
            if (isLeftClick && isOpenClickCount && !isHoverExpandButton) state.open(item)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(color = bgColor)
                .alpha(if (styles.contains(Typography.Style.FADED)) FADED_OPACITY else 1f)
                .widthIn(min = state.minWidth).height(itemHeight)
                .focusRequester(item.focusReq).focusable()
                .onKeyEvent { onKeyEvent(it, state, item) }
                .padding(horizontal = horizontalItemPadding)
                .pointerHoverIcon(PointerIcon.Hand)
                .pointerInput(item) { onPointerInput(state, item) }
                .onPointerEvent(Release) { it.awtEventOrNull?.let(::mayOpenItem) }
                .onPointerEvent(Enter) { state.hovered = item }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.onGloballyPositioned { state.mayIncreaseItemWidth(it.size.width) }
            ) {
                val rootsAreExpandable = state.entries.any { it.isExpandable }
                val itemDepth = if (rootsAreExpandable) item.depth else item.depth - 1
                if (itemDepth > 0) Spacer(modifier = Modifier.width(ICON_WIDTH * itemDepth))
                if (rootsAreExpandable) ItemButton(item, itemHeight) else Spacer(Modifier.width(TEXT_SPACING))
                if (iconArg != null) ItemIcon(item, iconArg)
                Spacer(Modifier.width(TEXT_SPACING))
                ItemText(item, styles)
                Spacer(modifier = Modifier.width(AREA_PADDING))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    @Composable
    private fun <T : Navigable<T>> ItemButton(item: ItemState<T>, itemHeight: Dp) {
        if (!item.isExpandable) Spacer(Modifier.size(itemHeight))
        else RawIconButton(
            icon = if (item.isExpanded) Icon.ITEM_EXPANDED else Icon.ITEM_COLLAPSED,
            modifier = Modifier.size(itemHeight).onGloballyPositioned {
                item.updateButtonArea(it.boundsInWindow())
            },
        ) { item.toggle() }
    }

    @Composable
    private fun <T : Navigable<T>> ItemIcon(item: ItemState<T>, iconArg: (ItemState<T>) -> IconArg) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ICON_WIDTH)) {
            Icon.Render(icon = iconArg(item).icon, color = iconArg(item).color())
        }
    }

    @Composable
    private fun <T : Navigable<T>> ItemText(item: ItemState<T>, styleArgs: List<Typography.Style>) {
        Row(modifier = Modifier.height(ICON_WIDTH)) {
            Text(
                value = item.name,
                fontStyle = if (styleArgs.contains(ITALIC)) FontStyle.Italic else null,
                fontWeight = if (styleArgs.contains(BOLD)) FontWeight.SemiBold else null,
                textDecoration = if (styleArgs.contains(UNDERLINE)) TextDecoration.Underline else null
            )
            item.info?.let {
                Spacer(Modifier.width(TEXT_SPACING))
                Text(value = "( $it )", alpha = FADED_OPACITY)
            }
        }
    }

    private suspend fun <T : Navigable<T>> PointerInputScope.onPointerInput(
        state: NavigatorState<T>, item: ItemState<T>
    ) = state.contextMenu.onPointerInput(
        pointerInputScope = this,
        onSinglePrimaryPressed = { state.maySelect(item) },
        onSecondaryClick = { state.maySelect(item) }
    )

    @OptIn(ExperimentalComposeUiApi::class)
    private fun <T : Navigable<T>> onKeyEvent(
        event: KeyEvent, state: NavigatorState<T>, item: ItemState<T>
    ): Boolean {
        return when (event.awtEventOrNull?.id) {
            java.awt.event.KeyEvent.KEY_RELEASED -> false
            else -> when (event.key) {
                Key.Enter, Key.NumPadEnter -> {
                    if (state.selected == item) state.open(item)
                    else state.maySelect(item)
                    true
                }
                Key.DirectionLeft -> {
                    if (item.isExpanded) item.collapse()
                    else state.maySelectParent(item)
                    true
                }
                Key.DirectionRight -> {
                    if (!item.isExpanded) item.expand()
                    else state.maySelectNext(item)
                    true
                }
                Key.DirectionUp -> {
                    state.maySelectPrevious(item)
                    true
                }
                Key.DirectionDown -> {
                    state.maySelectNext(item)
                    true
                }
                else -> false
            }
        }
    }
}
