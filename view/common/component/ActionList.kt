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

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.component.Form.FormRowSpacer
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.theme.Theme

object ActionList {

    private val ITEM_HEIGHT = 34.dp
    private val BUTTON_SIZE = 24.dp

    enum class Side { LEFT, RIGHT }

    @Composable
    fun <T : Any> Layout(
        items: List<T>,
        settingSide: Side,
        modifier: Modifier,
        itemHeight: Dp = ITEM_HEIGHT,
        buttonFn: (T) -> Form.IconButtonArg
    ) {
        @Composable
        fun Separator() {
            Separator.Vertical(2.dp, Theme.colors.background1, Modifier.height(itemHeight * items.size))
        }

        Row(modifier.verticalScroll(rememberScrollState())) {
            if (settingSide == Side.LEFT) {
                SettingColumn(items, itemHeight, buttonFn)
                Separator()
            }
            NameColumn(Modifier.weight(1f), items, itemHeight)
            if (settingSide == Side.RIGHT) {
                Separator()
                SettingColumn(items, itemHeight, buttonFn)
            }
        }
    }

    @Composable
    private fun bgColor(i: Int): Color = if (i % 2 == 0) Theme.colors.background2 else Theme.colors.background1

    @Composable
    private fun <T : Any> NameColumn(modifier: Modifier, items: List<T>, itemHeight: Dp) {
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
                    FormRowSpacer()
                    Form.Text(value = item.toString())
                    FormRowSpacer()
                }
            }
        }
    }

    @Composable
    private fun <T : Any> SettingColumn(items: List<T>, itemHeight: Dp, buttonFn: (T) -> Form.IconButtonArg) {
        val density = LocalDensity.current.density
        var minWidth by remember { mutableStateOf(0.dp) }
        Column(Modifier.defaultMinSize(minWidth = minWidth)) {
            items.forEachIndexed { i, item ->
                val button = buttonFn(item)
                Row(
                    Modifier.height(itemHeight)
                        .defaultMinSize(minWidth = minWidth)
                        .onSizeChanged { minWidth = toDP(it.width, density).coerceAtLeast(minWidth) }
                        .background(bgColor(i)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormRowSpacer()
                    IconButton(
                        icon = button.icon,
                        hoverIcon = button.hoverIcon,
                        iconColor = button.color(),
                        iconHoverColor = button.hoverColor?.invoke(),
                        disabledColor = button.disabledColor?.invoke(),
                        modifier = Modifier.size(BUTTON_SIZE),
                        tooltip = button.tooltip,
                        onClick = button.onClick
                    )
                    FormRowSpacer()
                }
            }
        }
    }
}
