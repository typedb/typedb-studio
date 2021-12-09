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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.state.common.Message.System.Companion.ILLEGAL_CAST
import com.vaticle.typedb.studio.state.common.Message.View.Companion.EXPAND_LIMIT_REACHED
import com.vaticle.typedb.studio.state.common.Message.View.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.state.common.Navigable
import com.vaticle.typedb.studio.view.common.component.Form.IconArgs
import com.vaticle.typedb.studio.view.common.component.Navigator.ItemState.Expandable
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.INDICATION_HOVER_ALPHA
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import java.util.LinkedList
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

object Navigator {

    @OptIn(ExperimentalTime::class)
    private val LIVE_UPDATE_REFRESH_RATE = Duration.seconds(3)
    private const val MAX_ITEM_EXPANDED = 5000
    private val ITEM_HEIGHT = 26.dp
    private val ICON_WIDTH = 20.dp
    private val TEXT_SPACING = 4.dp
    private val AREA_PADDING = 8.dp
    private val LOGGER = KotlinLogging.logger {}

    open class ItemState<T : Navigable.Item<T>> internal constructor(
        open val item: T, val container: Expandable<T>?
    ) : Comparable<ItemState<T>> {

        open val isExpandable: Boolean = false
        val name get() = item.name
        val info get() = item.info
        var focusFn: (() -> Unit)? = null

        open fun asExpandable(): Expandable<T> {
            throw TypeCastException(ILLEGAL_CAST.message(ItemState::class.simpleName, Expandable::class.simpleName))
        }

        override fun compareTo(other: ItemState<T>): Int {
            return item.compareTo(other.item)
        }

        class Expandable<T : Navigable.Item<T>> internal constructor(
            val expandable: Navigable.Container<T>, container: Expandable<T>?, private val reloadOnExpand: Boolean
        ) : ItemState<T>(expandable as T, container) {

            override val isExpandable: Boolean = true
            var isExpanded: Boolean by mutableStateOf(false)
            var entries: List<ItemState<T>> by mutableStateOf(emptyList())

            override fun asExpandable(): Expandable<T> {
                return this
            }

            fun toggle() {
                if (isExpanded) collapse()
                else expand()
            }

            fun collapse() {
                isExpanded = false
            }

            fun expand() {
                expand(1)
            }

            internal fun expand(depth: Int) {
                expand(1, depth)
            }

            private fun expand(currentDepth: Int, maxDepth: Int) {
                isExpanded = true
                if (reloadOnExpand) reloadEntries()
                if (currentDepth < maxDepth) {
                    entries.filterIsInstance<Expandable<T>>().forEach { it.expand(currentDepth + 1, maxDepth) }
                }
            }

            private fun reloadEntries() {
                item.asContainer().reloadEntries()
                val new = item.asContainer().entries.toSet()
                val old = entries.map { it.item }.toSet()
                if (new != old) {
                    val deleted = old - new
                    val added = new - old
                    val updatedEntries = entries.filter { !deleted.contains(it.item) } +
                            added.map { itemStateOf(it) }.toList()
                    entries = updatedEntries.sorted()
                }
                entries.filterIsInstance<Expandable<T>>().filter { it.isExpanded }.forEach { it.reloadEntries() }
            }

            internal fun checkForUpdate() {
                if (!isExpanded) return
                item.asContainer().reloadEntries()
                if (item.asContainer().entries.toSet() != entries.map { it.item }.toSet()) reloadEntries()
                entries.filterIsInstance<Expandable<T>>().filter { it.isExpanded }.forEach { it.checkForUpdate() }
            }

            private fun itemStateOf(item: T): ItemState<T> {
                return if (item.isContainer) Expandable(item.asContainer(), this, reloadOnExpand)
                else ItemState(item, this)
            }
        }
    }

    class NavigatorState<T : Navigable.Item<T>> internal constructor(
        root: Navigable.Container<T>,
        private val title: String,
        private val initExpandDepth: Int,
        private val reloadOnExpand: Boolean,
        private val liveUpdate: Boolean,
        private val openFn: (ItemState<T>) -> Unit
    ) {

        private var coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)
        var minWidth by mutableStateOf(0.dp); private set
        var selected: ItemState<T>? by mutableStateOf(null); private set
        var hovered: ItemState<T>? by mutableStateOf(null)
        var root: Expandable<T> by mutableStateOf(newRootOf(root)); private set
        val buttons: List<Form.ButtonArgs> = listOf(
            Form.ButtonArgs(Icon.Code.CHEVRONS_DOWN) { expand() },
            Form.ButtonArgs(Icon.Code.CHEVRONS_UP) { collapse() }
        )

        fun replaceRoot(newRoot: Navigable.Container<T>) {
            coroutineScope.cancel()
            coroutineScope = CoroutineScope(EmptyCoroutineContext)
            root = newRootOf(newRoot)
        }

        private fun newRootOf(newRoot: Navigable.Container<T>): Expandable<T> {
            return Expandable(newRoot, null, reloadOnExpand)
                .also { it.expand(1 + initExpandDepth) }
                .also { if (liveUpdate) initWatcher(it) }
        }

        @OptIn(ExperimentalTime::class)
        private fun initWatcher(root: Expandable<T>) {
            coroutineScope.launch {
                try {
                    do {
                        delay(LIVE_UPDATE_REFRESH_RATE) // TODO: is there better way?
                        root.checkForUpdate()
                    } while (true)
                } catch (e: CancellationException) {
                } catch (e: java.lang.Exception) {
                    State.notification.systemError(LOGGER, e, UNEXPECTED_ERROR)
                }
            }
        }

        private fun expand() {
            var i = 0
            val queue = LinkedList(root.entries.filterIsInstance<Expandable<T>>())
            while (queue.isNotEmpty() && i < MAX_ITEM_EXPANDED) {
                val item = queue.pop()
                item.expand()
                i += 1 + item.entries.count { !it.isExpandable }
                queue.addAll(item.entries.filterIsInstance<Expandable<T>>())
            }
            if (!queue.isEmpty()) {
                State.notification.userWarning(LOGGER, EXPAND_LIMIT_REACHED, title, MAX_ITEM_EXPANDED)
            }
        }

        private fun collapse() {
            val queue = LinkedList(root.entries.filterIsInstance<Expandable<T>>())
            while (queue.isNotEmpty()) {
                val item = queue.pop()
                item.collapse()
                queue.addAll(item.entries.filterIsInstance<Expandable<T>>().filter { it.isExpanded })
            }
        }

        fun mayIncreaseMinWidth(width: Dp) {
            if (width > minWidth) minWidth = width
        }

        fun open(item: ItemState<T>) {
            openFn(item)
        }

        fun select(item: ItemState<T>) {
            selected = item
            item.focusFn?.let { it() }
        }

        fun selectNext(item: ItemState<T>) {
            println("Select next from: ${item.item}") // TODO
        }

        fun selectPrevious(item: ItemState<T>) {
            println("Select previous from: ${item.item}") // TODO
        }

        fun selectContainer(item: ItemState<T>) {
            item.container?.let { if (!it.expandable.isRoot) select(it) }
        }
    }

    @Composable
    fun <T : Navigable.Item<T>> rememberNavigatorState(
        root: Navigable.Container<T>, title: String, initExpandDepth: Int,
        reloadOnExpand: Boolean, liveUpdate: Boolean, openFn: (ItemState<T>) -> Unit
    ): NavigatorState<T> {
        return remember { NavigatorState(root, title, initExpandDepth, reloadOnExpand, liveUpdate, openFn) }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun <T : Navigable.Item<T>> Layout(
        navState: NavigatorState<T>, itemHeight: Dp = ITEM_HEIGHT,
        iconArgs: (ItemState<T>) -> IconArgs, contextMenuFn: (ItemState<T>) -> List<ContextMenu.Item>
    ) {
        val density = LocalDensity.current.density
        Box(modifier = Modifier.fillMaxSize().onSizeChanged { navState.mayIncreaseMinWidth(toDP(it.width, density)) }) {
            val ctmState = ContextMenu.rememberState()
            ContextMenu.Popup(ctmState, contextMenuFn.let { { it(navState.selected!!) } })
            LazyColumn(
                modifier = Modifier.widthIn(min = navState.minWidth)
                    .horizontalScroll(rememberScrollState())
                    .pointerMoveFilter(onExit = { navState.hovered = null; false })
            ) {
                expandedItemLayouts(
                    navigatorState = navState,
                    contextMenuState = ctmState,
                    entries = navState.root.entries,
                    depth = 0,
                    itemHeight = itemHeight,
                    iconArgs = iconArgs,
                    onSizeChanged = { navState.mayIncreaseMinWidth(toDP(it, density)) },
                ).forEach { item { it() } }
            }
        }
    }

    private fun <T : Navigable.Item<T>> expandedItemLayouts(
        navigatorState: NavigatorState<T>, contextMenuState: ContextMenu.State,
        entries: List<ItemState<T>>, depth: Int, itemHeight: Dp = ITEM_HEIGHT,
        iconArgs: (ItemState<T>) -> IconArgs, onSizeChanged: (Int) -> Unit
    ): List<@Composable () -> Unit> {
        val itemLayouts: MutableList<@Composable () -> Unit> = mutableListOf()
        entries.forEach { item ->
            itemLayouts.add {
                ItemLayout(
                    navState = navigatorState,
                    contextMenuState = contextMenuState,
                    item = item,
                    depth = depth,
                    itemHeight = itemHeight,
                    iconArgs = iconArgs,
                    onSizeChanged = onSizeChanged
                )
            }
            if (item.isExpandable && item.asExpandable().isExpanded) itemLayouts.addAll(
                expandedItemLayouts(
                    navigatorState = navigatorState,
                    contextMenuState = contextMenuState,
                    entries = item.asExpandable().entries,
                    depth = depth + 1,
                    itemHeight = itemHeight,
                    iconArgs = iconArgs,
                    onSizeChanged = onSizeChanged
                )
            )
        }
        return itemLayouts
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
    @Composable
    private fun <T : Navigable.Item<T>> ItemLayout(
        navState: NavigatorState<T>,
        contextMenuState: ContextMenu.State,
        item: ItemState<T>,
        depth: Int,
        itemHeight: Dp = ITEM_HEIGHT,
        iconArgs: (ItemState<T>) -> IconArgs,
        onSizeChanged: (Int) -> Unit
    ) {
        val focusReq = remember { FocusRequester() }.also { item.focusFn = { it.requestFocus() } }
        val bgColor = when {
            navState.selected == item -> Theme.colors.primary
            navState.hovered == item -> Theme.colors.indicationBase.copy(INDICATION_HOVER_ALPHA)
            else -> Color.Transparent
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(color = bgColor)
                .widthIn(min = navState.minWidth).height(itemHeight)
                .onSizeChanged { onSizeChanged(it.width) }
                .focusRequester(focusReq).focusable(true)
                .onKeyEvent { onKeyEvent(it, navState, item) }
                .pointerHoverIcon(PointerIconDefaults.Hand)
                .pointerInput(item) { onPointerInput(contextMenuState, navState, focusReq, item) }
                .pointerMoveFilter(onEnter = { navState.hovered = item; false })
        ) {
            if (depth > 0) Spacer(modifier = Modifier.width(ICON_WIDTH * depth))
            ItemButton(item, itemHeight)
            ItemIcon(item, iconArgs)
            Spacer(Modifier.width(TEXT_SPACING))
            ItemText(item)
            Spacer(modifier = Modifier.width(AREA_PADDING))
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    @Composable
    private fun <T : Navigable.Item<T>> ItemButton(item: ItemState<T>, size: Dp) {
        if (item.isExpandable) Form.RawClickableIcon(
            icon = if (item.asExpandable().isExpanded) Icon.Code.CHEVRON_DOWN else Icon.Code.CHEVRON_RIGHT,
            onClick = { item.asExpandable().toggle() },
            modifier = Modifier.size(size)
        ) else Spacer(Modifier.size(size))
    }

    @Composable
    private fun <T : Navigable.Item<T>> ItemIcon(item: ItemState<T>, iconArgs: (ItemState<T>) -> IconArgs) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ICON_WIDTH)) {
            Icon.Render(icon = iconArgs(item).code, color = iconArgs(item).color())
        }
    }

    @Composable
    private fun <T : Navigable.Item<T>> ItemText(item: ItemState<T>) {
        Row(modifier = Modifier.height(ICON_WIDTH)) {
            Form.Text(value = item.name)
            item.info?.let {
                Spacer(Modifier.width(TEXT_SPACING))
                Form.Text(value = "( $it )", alpha = 0.4f)
            }
        }
    }

    private suspend fun <T : Navigable.Item<T>> PointerInputScope.onPointerInput(
        contextMenuState: ContextMenu.State, navigatorState: NavigatorState<T>,
        focusReq: FocusRequester, item: ItemState<T>
    ) {
        contextMenuState.onPointerInput(
            pointerInputScope = this,
            onSinglePrimaryClick = { navigatorState.select(item); focusReq.requestFocus() },
            onDoublePrimaryClick = { navigatorState.open(item) },
            onSecondaryClick = { navigatorState.select(item) }
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
                    if (item.isExpandable && item.asExpandable().isExpanded) item.asExpandable().collapse()
                    else state.selectContainer(item)
                    true
                }
                Key.DirectionRight -> {
                    if (item.isExpandable && !item.asExpandable().isExpanded) item.asExpandable().expand()
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
