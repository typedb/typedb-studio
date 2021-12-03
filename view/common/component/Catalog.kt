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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.common.Catalog
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP

object Catalog {

    private val ITEM_HEIGHT = 26.dp
    private val ICON_WIDTH = 20.dp
    private val TEXT_SPACING = 4.dp
    private val AREA_PADDING = 8.dp

    data class IconArgs(val code: Icon.Code, val color: @Composable () -> Color = { Theme.colors.icon })

    private class CatalogState {
        var minWidth by mutableStateOf(0.dp)
    }

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
        ) { NestedCatalog(0, catalog, catalog.items, iconArgs, itemHeight, contextMenuItems, state) }
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
    @Composable
    private fun <T : Catalog.Item<T>> NestedCatalog(
        depth: Int,
        catalog: Catalog<T>,
        items: List<T>,
        iconArgs: (T) -> IconArgs,
        itemHeight: Dp,
        contextMenuItems: ((T) -> List<ContextMenu.Item>)?,
        state: CatalogState
    ) {
        val density = LocalDensity.current.density

        fun increaseToAtLeast(widthSize: Int) {
            val newWidth = toDP(widthSize, density)
            if (newWidth > state.minWidth) state.minWidth = newWidth
        }

        Column(modifier = Modifier.widthIn(min = state.minWidth).onSizeChanged { increaseToAtLeast(it.width) }) {
            items.forEach { item ->
                val menuItems = contextMenuItems?.let { it(item) }
                ContextMenu.Area(items = menuItems, enabled = !menuItems.isNullOrEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.widthIn(min = state.minWidth).height(itemHeight)
                            .pointerHoverIcon(PointerIconDefaults.Hand)
                            .onSizeChanged { increaseToAtLeast(it.width) }
                            .combinedClickable(onClick = { catalog.select(item) }, onDoubleClick = { item.open() })
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
                if (item.isExpandable && item.asExpandable().isExpanded) {
                    NestedCatalog(
                        depth = depth + 1,
                        catalog = catalog,
                        items = item.asExpandable().entries,
                        iconArgs = iconArgs,
                        itemHeight = itemHeight,
                        contextMenuItems = contextMenuItems,
                        state = state
                    )
                }
            }
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
        )
        else Spacer(Modifier.size(size))
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
