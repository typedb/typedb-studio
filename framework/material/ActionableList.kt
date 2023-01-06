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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.common.Util.toDP
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Form.IconButton
import com.vaticle.typedb.studio.framework.material.Form.RowSpacer

object ActionableList {

    private val ITEM_HEIGHT = 34.dp
    private val BUTTON_SIZE = 24.dp

    enum class Side { LEFT, RIGHT }

    @Composable
    fun <T : Any> Layout(
        items: List<T>,
        itemHeight: Dp = ITEM_HEIGHT,
        modifier: Modifier,
        buttonsSide: Side,
        buttonsFn: (T) -> List<Form.IconButtonArg>
    ) {
        val scrollState = rememberScrollState()

        @Composable
        fun Separator() {
            Separator.Vertical(2.dp, Theme.studio.backgroundMedium, Modifier.height(itemHeight * items.size))
        }

        Box(modifier) {
            Row(Modifier.verticalScroll(scrollState)) {
                if (buttonsSide == Side.LEFT) {
                    ActionColumn(items, itemHeight, buttonsFn)
                    Separator()
                }
                ItemColumn(Modifier.weight(1f), items, itemHeight)
                if (buttonsSide == Side.RIGHT) {
                    Separator()
                    ActionColumn(items, itemHeight, buttonsFn, scrollState)
                }
            }
            Scrollbar.Vertical(rememberScrollbarAdapter(scrollState), Modifier.align(Alignment.CenterEnd))
        }
    }

    @Composable
    fun <T: Any> SingleButtonLayout(
        items: List<T>,
        itemHeight: Dp = ITEM_HEIGHT,
        modifier: Modifier,
        buttonSide: Side,
        buttonFn: (T) -> Form.IconButtonArg
    ) = Layout(items, itemHeight, modifier, buttonSide) { listOf(buttonFn(it)) }

    @Composable
    private fun bgColor(i: Int): Color =
        if (i % 2 == 0) Theme.studio.backgroundLight else Theme.studio.backgroundMedium

    @Composable
    private fun <T : Any> ItemColumn(modifier: Modifier, items: List<T>, itemHeight: Dp) {
        val density = LocalDensity.current.density
        var minWidth by remember { mutableStateOf(0.dp) }
        Column(
            modifier.horizontalScroll(rememberScrollState())
                .onSizeChanged { minWidth = toDP(it.width, density).coerceAtLeast(minWidth) }
        ) {
            items.forEachIndexed { i, item ->
                Row(
                    Modifier.height(itemHeight)
                        .defaultMinSize(minWidth = minWidth)
                        .onSizeChanged { minWidth = toDP(it.width, density).coerceAtLeast(minWidth) }
                        .background(bgColor(i)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RowSpacer()
                    Form.Text(value = item.toString())
                    RowSpacer()
                }
            }
        }
    }

    @Composable
    private fun <T : Any> ActionColumn(
        items: List<T>, itemHeight: Dp, buttonsFn: (T) -> List<Form.IconButtonArg>, scrollState: ScrollState? = null
    ) {
        val density = LocalDensity.current.density
        var minWidth by remember { mutableStateOf(0.dp) }
        Column(Modifier.defaultMinSize(minWidth = minWidth)) {
            items.forEachIndexed { i, item ->
                val buttons = buttonsFn(item)
                Row(
                    Modifier.height(itemHeight)
                        .defaultMinSize(minWidth = minWidth)
                        .onSizeChanged { minWidth = toDP(it.width, density).coerceAtLeast(minWidth) }
                        .background(bgColor(i)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RowSpacer()
                    buttons.forEach {
                        IconButton(
                            icon = it.icon,
                            hoverIcon = it.hoverIcon,
                            modifier = Modifier.size(BUTTON_SIZE),
                            iconColor = it.color(),
                            iconHoverColor = it.hoverColor?.invoke(),
                            disabledColor = it.disabledColor?.invoke(),
                            tooltip = it.tooltip,
                            onClick = it.onClick,
                            enabled = it.enabled,
                        )
                        RowSpacer()
                    }
                    scrollState?.let { if (it.maxValue > 0 && it.maxValue < Int.MAX_VALUE) RowSpacer() }
                }
            }
        }
    }
}
