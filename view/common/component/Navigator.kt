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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.state.common.Message.System.Companion.ILLEGAL_CAST
import com.vaticle.typedb.studio.state.common.Message.View.Companion.EXPAND_LIMIT_REACHED
import com.vaticle.typedb.studio.state.common.Message.View.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.state.common.Navigable
import com.vaticle.typedb.studio.state.notification.Error
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Navigator.ItemState.Expandable
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

    enum class Type(val label: String) {
        PROJECT(Label.PROJECT);
    }

    open class ItemState<T : Navigable.Item<T>> internal constructor(
        open val item: Navigable.Item<T>, val container: Expandable<T>?
    ) : Comparable<ItemState<T>> {

        open val isExpandable: Boolean = false
        val name get() = item.name
        val info get() = item.info

        open fun asExpandable(): Expandable<T> {
            throw TypeCastException(ILLEGAL_CAST.message(ItemState::class.simpleName, Expandable::class.simpleName))
        }

        override fun compareTo(other: ItemState<T>): Int {
            return item.compareTo(other.item)
        }

        class Expandable<T : Navigable.Item<T>> internal constructor(
            override val item: Navigable.Container<T>, container: Expandable<T>?, private val reloadOnExpand: Boolean
        ) : ItemState<T>(item, container) {

            override val isExpandable: Boolean = true
            var isExpanded: Boolean by mutableStateOf(false)
            var entries: List<ItemState<T>> by mutableStateOf(emptyList())

            override fun asExpandable(): Expandable<T> {
                return this
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
                item.reloadEntries()
                val new = item.entries.toSet()
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
                item.reloadEntries()
                if (item.entries.toSet() != entries.map { it.item }.toSet()) reloadEntries()
                entries.filterIsInstance<Expandable<T>>().filter { it.isExpanded }.forEach { it.checkForUpdate() }
            }

            private fun itemStateOf(item: Navigable.Item<T>): ItemState<T> {
                return if (item.isContainer) Expandable(item.asContainer(), this, reloadOnExpand)
                else ItemState(item, this)
            }
        }
    }

    class NavigatorState<T : Navigable.Item<T>> internal constructor(
        newRoot: Navigable.Container<T>,
        private val title: String,
        private val initExpandDepth: Int,
        private val reloadOnExpand: Boolean,
        private val liveUpdate: Boolean
    ) {

        private var coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)
        var minWidth by mutableStateOf(0.dp)
        var root: Expandable<T> by mutableStateOf(newRootOf(newRoot)); private set
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
                } catch (e: Exception) {
                    State.notification.systemError(Error.fromSystem(e, UNEXPECTED_ERROR), LOGGER)
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
                val error = Error.fromUser(EXPAND_LIMIT_REACHED, title, MAX_ITEM_EXPANDED)
                State.notification.userError(error, LOGGER)
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
    }

    @Composable
    fun <T : Navigable.Item<T>> rememberNavigatorState(
        navigable: Navigable.Container<T>, title: String,
        initExpandDepth: Int, reloadOnExpand: Boolean, liveUpdate: Boolean
    ): NavigatorState<T> {
        return remember { NavigatorState(navigable, title, initExpandDepth, reloadOnExpand, liveUpdate) }
    }

    @Composable
    fun <T : Navigable.Item<T>> Layout(state: NavigatorState<T>) {
        LazyColumn(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            expandedItemLayouts(state, state.root.entries, 0).forEach { item { it() } }
        }
    }

    private fun <T : Navigable.Item<T>> expandedItemLayouts(
        state: NavigatorState<T>,
        entries: List<ItemState<T>>,
        depth: Int
    ): List<@Composable () -> Unit> {
        val itemLayouts: MutableList<@Composable () -> Unit> = mutableListOf()
        entries.forEach { item ->
            itemLayouts.add { ItemLayout(state, item, depth) }
            if (item.isExpandable && item.asExpandable().isExpanded) {
                itemLayouts.addAll(expandedItemLayouts(state, item.asExpandable().entries, depth + 1))
            }
        }
        return itemLayouts
    }

    @Composable
    private fun <T : Navigable.Item<T>> ItemLayout(state: NavigatorState<T>, item: ItemState<T>, depth: Int) {
        Form.Text(value = "-".repeat(depth) + item.name)
    }
}
