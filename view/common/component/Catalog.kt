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
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.common.Catalog
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import java.awt.event.KeyEvent.KEY_RELEASED

object Catalog {

    private val ITEM_HEIGHT = 26.dp
    private val ICON_WIDTH = 20.dp
    private val TEXT_SPACING = 4.dp
    private val AREA_PADDING = 8.dp

    data class IconArgs(val code: Icon.Code, val color: @Composable () -> Color = { Theme.colors.icon })

    private class CatalogState {
        var minWidth by mutableStateOf(0.dp)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun <T : Catalog.Item<T>> Layout(
        catalog: Catalog<T>,
        iconArgs: (T) -> IconArgs,
        itemHeight: Dp = ITEM_HEIGHT,
        contextMenuItems: ((T) -> List<ContextMenu.Item>)? = null
    ) {
        val density = LocalDensity.current.density
        val state = remember { CatalogState() }
        Box(
            modifier = Modifier.fillMaxSize()
                .onSizeChanged { state.minWidth = toDP(it.width, density) }
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        ) {
            ContextMenu.Area(contextMenuItems?.let { { it(catalog.selected!!) } }, contextMenuItems != null) {
                NestedCatalog(0, catalog, catalog.entries, iconArgs, itemHeight, state)
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun <T : Catalog.Item<T>> NestedCatalog(
        depth: Int, catalog: Catalog<T>, items: List<T>, iconArgs: (T) -> IconArgs, itemHeight: Dp, state: CatalogState
    ) {
        val density = LocalDensity.current.density

        fun increaseToAtLeast(widthSize: Int) {
            val newWidth = toDP(widthSize, density)
            if (newWidth > state.minWidth) state.minWidth = newWidth
        }

        Column(modifier = Modifier.widthIn(min = state.minWidth).onSizeChanged { increaseToAtLeast(it.width) }) {
            items.forEach { item ->
                val focusReq = remember { FocusRequester() }.also { item.focusFn = { it.requestFocus() } }
                val bgColor = if (catalog.isSelected(item)) Theme.colors.primary else Color.Transparent
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(color = bgColor)
                        .widthIn(min = state.minWidth).height(itemHeight)
                        .onSizeChanged { increaseToAtLeast(it.width) }
                        .focusRequester(focusReq)
                        .onKeyEvent { onKeyEvent(it, catalog, item) }
                        .pointerHoverIcon(PointerIconDefaults.Hand)
                        .onPointerEvent(PointerEventType.Press) { onPointerEvent(it, focusReq, catalog, item) }
                        .clickable { } // Keep this to enable mouse hovering behaviour
                ) {
                    if (depth > 0) Spacer(modifier = Modifier.width(ICON_WIDTH * depth))
                    ItemButton(item, itemHeight)
                    ItemIcon(item, iconArgs)
                    Spacer(Modifier.width(TEXT_SPACING))
                    ItemText(item)
                    Spacer(modifier = Modifier.width(AREA_PADDING))
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (item.isExpandable && item.asExpandable().isExpanded) {
                    NestedCatalog(depth + 1, catalog, item.asExpandable().entries, iconArgs, itemHeight, state)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun <T : Catalog.Item<T>> onKeyEvent(event: KeyEvent, catalog: Catalog<T>, item: T): Boolean {
        return when (event.awtEvent.id) {
            KEY_RELEASED -> false
            else -> when (event.key) {
                Key.Enter, Key.NumPadEnter -> {
                    if (catalog.isSelected(item)) catalog.open(item)
                    else catalog.select(item)
                    true
                }
                Key.DirectionLeft -> {
                    if (item.isExpandable && item.asExpandable().isExpanded) item.asExpandable().collapse()
                    else catalog.selectParent(item)
                    true
                }
                Key.DirectionRight -> {
                    if (item.isExpandable && !item.asExpandable().isExpanded) item.asExpandable().expand()
                    else catalog.selectNext(item)
                    true
                }
                Key.DirectionUp -> {
                    catalog.selectPrevious(item)
                    true
                }
                Key.DirectionDown -> {
                    catalog.selectNext(item)
                    true
                }
                else -> false
            }
        }
    }

    private fun <T : Catalog.Item<T>> onPointerEvent(
        event: PointerEvent, focusReq: FocusRequester, catalog: Catalog<T>, item: T
    ) {
        when {
            event.buttons.isPrimaryPressed -> when (event.awtEvent.clickCount) {
                1 -> {
                    catalog.select(item)
                    focusReq.requestFocus()
                }
                2 -> catalog.open(item)
            }
            event.buttons.isSecondaryPressed -> catalog.select(item)
        }
    }

    @Composable
    private fun <T : Catalog.Item<T>> ItemButton(item: T, size: Dp) {
        if (item.isExpandable) Form.IconButton(
            icon = if (item.asExpandable().isExpanded) Icon.Code.CHEVRON_DOWN else Icon.Code.CHEVRON_RIGHT,
            onClick = { item.asExpandable().toggle() },
            modifier = Modifier.size(size),
            bgColor = Color.Transparent,
            rounded = false
        ) else Spacer(Modifier.size(size))
    }

    @Composable
    private fun <T : Catalog.Item<T>> ItemIcon(item: T, iconArgs: (T) -> IconArgs) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ICON_WIDTH)) {
            Icon.Render(icon = iconArgs(item).code, color = iconArgs(item).color())
        }
    }

    @Composable
    private fun <T : Catalog.Item<T>> ItemText(item: Catalog.Item<T>) {
        Row(modifier = Modifier.height(ICON_WIDTH)) {
            Form.Text(value = item.name)
            item.info?.let {
                Spacer(Modifier.width(TEXT_SPACING))
                Form.Text(value = "( $it )", alpha = 0.4f)
            }
        }
    }
}
