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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.common.Navigable
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import java.awt.event.KeyEvent.KEY_RELEASED

object Navigator {

    private val ITEM_HEIGHT = 26.dp
    private val ICON_WIDTH = 20.dp
    private val TEXT_SPACING = 4.dp
    private val AREA_PADDING = 8.dp

    data class IconArgs(val code: Icon.Code, val color: @Composable () -> Color = { Theme.colors.icon })

    private class NavigatorState {
        var minWidth by mutableStateOf(0.dp)
    }

    @Composable
    fun <T : Navigable.Item<T>> Layout(
        navigable: Navigable<T>, iconArgs: (T) -> IconArgs, itemHeight: Dp = ITEM_HEIGHT,
        contextMenuFn: ((T) -> List<ContextMenu.Item>)? = null
    ) {
        val density = LocalDensity.current.density
        val state = remember { NavigatorState() }
        Box(
            modifier = Modifier.fillMaxSize()
                .onSizeChanged { state.minWidth = toDP(it.width, density) }
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        ) { NestedNavigator(0, navigable, navigable.entries, iconArgs, itemHeight, contextMenuFn, state) }
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
    @Composable
    private fun <T : Navigable.Item<T>> NestedNavigator(
        depth: Int, navigable: Navigable<T>, items: List<T>, iconArgs: (T) -> IconArgs,
        itemHeight: Dp, contextMenuFn: ((T) -> List<ContextMenu.Item>)?, state: NavigatorState
    ) {
        val density = LocalDensity.current.density
        fun increaseToAtLeast(widthSize: Int) {
            val newWidth = toDP(widthSize, density)
            if (newWidth > state.minWidth) state.minWidth = newWidth
        }
        Column(modifier = Modifier.widthIn(min = state.minWidth).onSizeChanged { increaseToAtLeast(it.width) }) {
            items.forEach { item ->
                ContextMenu.Area(contextMenuFn?.let { { it(item) } }, { navigable.select(item) }) {
                    ItemLayout(depth, navigable, item, iconArgs, itemHeight, { increaseToAtLeast(it) }, state)
                }
                if (item.isExpandable && item.asExpandable().isExpanded) NestedNavigator(
                    depth + 1, navigable, item.asExpandable().entries, iconArgs, itemHeight, contextMenuFn, state
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
    @Composable
    private fun <T : Navigable.Item<T>> ItemLayout(
        depth: Int, navigable: Navigable<T>, item: T, iconArgs: (T) -> IconArgs,
        itemHeight: Dp, onSizeChanged: (Int) -> Unit, state: NavigatorState
    ) {
        val focusReq = remember { FocusRequester() }.also { item.focusFn = { it.requestFocus() } }
        val bgColor = when {
            navigable.isSelected(item) -> Theme.colors.primary
            else -> Color.Transparent
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(color = bgColor)
                .widthIn(min = state.minWidth).height(itemHeight)
                .onSizeChanged { onSizeChanged(it.width) }
                .focusRequester(focusReq)
                .onKeyEvent { onKeyEvent(it, navigable, item) }
                .pointerHoverIcon(PointerIconDefaults.Hand)
                .onPointerEvent(PointerEventType.Press) { onPointerEvent(it, focusReq, navigable, item) }
                .clickable { }
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
    private fun <T : Navigable.Item<T>> ItemButton(item: T, size: Dp) {
        if (item.isExpandable) Form.RawClickableIcon(
            icon = if (item.asExpandable().isExpanded) Icon.Code.CHEVRON_DOWN else Icon.Code.CHEVRON_RIGHT,
            onClick = { item.asExpandable().toggle() },
            modifier = Modifier.size(size)
        ) else Spacer(Modifier.size(size))
    }

    @Composable
    private fun <T : Navigable.Item<T>> ItemIcon(item: T, iconArgs: (T) -> IconArgs) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ICON_WIDTH)) {
            Icon.Render(icon = iconArgs(item).code, color = iconArgs(item).color())
        }
    }

    @Composable
    private fun <T : Navigable.Item<T>> ItemText(item: Navigable.Item<T>) {
        Row(modifier = Modifier.height(ICON_WIDTH)) {
            Form.Text(value = item.name)
            item.info?.let {
                Spacer(Modifier.width(TEXT_SPACING))
                Form.Text(value = "( $it )", alpha = 0.4f)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun <T : Navigable.Item<T>> onKeyEvent(event: KeyEvent, navigable: Navigable<T>, item: T): Boolean {
        return when (event.awtEvent.id) {
            KEY_RELEASED -> false
            else -> when (event.key) {
                Key.Enter, Key.NumPadEnter -> {
                    if (navigable.isSelected(item)) navigable.open(item)
                    else navigable.select(item)
                    true
                }
                Key.DirectionLeft -> {
                    if (item.isExpandable && item.asExpandable().isExpanded) item.asExpandable().collapse()
                    else navigable.selectParent(item)
                    true
                }
                Key.DirectionRight -> {
                    if (item.isExpandable && !item.asExpandable().isExpanded) item.asExpandable().expand()
                    else navigable.selectNext(item)
                    true
                }
                Key.DirectionUp -> {
                    navigable.selectPrevious(item)
                    true
                }
                Key.DirectionDown -> {
                    navigable.selectNext(item)
                    true
                }
                else -> false
            }
        }
    }

    private fun <T : Navigable.Item<T>> onPointerEvent(
        event: PointerEvent, focusReq: FocusRequester, navigable: Navigable<T>, item: T
    ) {
        when {
            event.buttons.isPrimaryPressed -> when (event.awtEvent.clickCount) {
                1 -> {
                    navigable.select(item)
                    focusReq.requestFocus()
                }
                2 -> navigable.open(item)
            }
        }
    }
}
