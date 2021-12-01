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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.common.TreeItem
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.theme.Theme

object Tree {

    private val ITEM_HEIGHT = 26.dp
    private val ICON_WIDTH = 20.dp
    private val AREA_PADDING = 4.dp

    data class IconArgs(val code: Icon.Code, val color: @Composable () -> Color = { Theme.colors.icon })

    @Composable
    fun <T : TreeItem<T>> Layout(items: List<T>, icon: (T) -> IconArgs, itemHeight: Dp = ITEM_HEIGHT) {
        List(
            items = items,
            icon = icon,
            itemHeight = itemHeight,
            modifier = Modifier
                .fillMaxWidth().padding(PaddingValues(start = AREA_PADDING, top = AREA_PADDING))
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun <T : TreeItem<T>> List(items: List<T>, icon: (T) -> IconArgs, itemHeight: Dp, modifier: Modifier) {
        Column(modifier = modifier) { // .border(1.dp, Color.Red, RectangleShape)
            items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(itemHeight).pointerHoverIcon(PointerIconDefaults.Hand).clickable { }
                ) {
                    ExpandOrCollapseOrNoButton(item)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ICON_WIDTH)) {
                        Icon.Render(icon = icon(item).code, color = icon(item).color())
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(value = item.name, modifier = Modifier.height(ICON_WIDTH).offset(y = (-1).dp))
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (item.isExpandable && item.asExpandable().isExpanded) {
                    NestedList(items = item.asExpandable().children, icon = icon, itemHeight = itemHeight)
                }
            }
        }
    }

    @Composable
    private fun <T : TreeItem<T>> NestedList(items: List<T>, icon: (T) -> IconArgs, itemHeight: Dp) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.width(ICON_WIDTH))
            List(items = items, icon = icon, itemHeight = itemHeight, modifier = Modifier.weight(1f))
        }
    }

    @Composable
    private fun <T : TreeItem<T>> ExpandOrCollapseOrNoButton(item: TreeItem<T>) {
        if (item.isExpandable) Form.IconButton(
            icon = if (item.asExpandable().isExpanded) Icon.Code.CHEVRON_DOWN else Icon.Code.CHEVRON_RIGHT,
            onClick = { item.asExpandable().toggle() },
            bgColor = Color.Transparent,
            modifier = Modifier.size(ICON_WIDTH)
        )
        else Spacer(Modifier.size(ICON_WIDTH))
    }
}