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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.theme.Color.FADED_OPACITY
import com.vaticle.typedb.studio.view.common.theme.Theme

object Table {

    data class Column<T>(
        val header: AnnotatedString?,
        val headerAlignment: Alignment = Alignment.Center,
        val contentAlignment: Alignment = Alignment.Center,
        val size: Either<Float, Dp> = Either.first(1f),
        val content: @Composable (T) -> Unit
    ) {
        constructor(
            header: String,
            contentAlignment: Alignment = Alignment.Center,
            headerAlignment: Alignment = Alignment.Center,
            size: Either<Float, Dp> = Either.first(1f),
            function: @Composable (T) -> Unit
        ) : this(AnnotatedString(header), headerAlignment, contentAlignment, size, function)
    }

    val ROW_HEIGHT = 36.dp
    private val COLUMN_BORDER_SIZE = 2.dp
    private val CELL_PADDING_HORIZONTAL = 8.dp
    private val CELL_PADDING_VERTICAL = 4.dp

    @Composable
    private fun bgColor(i: Int): Color = if (i % 2 == 0) Theme.colors.background2 else Theme.colors.background1

    @Composable
    fun <T> Layout(
        items: List<T>,
        modifier: Modifier = Modifier,
        rowHeight: Dp = ROW_HEIGHT,
        columnBorderSize: Dp = COLUMN_BORDER_SIZE,
        horCellPadding: Dp = CELL_PADDING_HORIZONTAL,
        verCellPadding: Dp = CELL_PADDING_VERTICAL,
        columns: List<Column<T>>
    ) {
        Column(modifier.border(1.dp, Theme.colors.border)) {
            Header(rowHeight, columnBorderSize, horCellPadding, verCellPadding, columns)
            Body(items, rowHeight, columnBorderSize, horCellPadding, verCellPadding, columns)
        }
    }

    @Composable
    private fun <T> Header(
        rowHeight: Dp, columnBorderSize: Dp,
        horCellPadding: Dp, verCellPadding: Dp, columns: List<Column<T>>
    ) {
        Row(Modifier.fillMaxWidth().height(rowHeight), Arrangement.spacedBy(columnBorderSize)) {
            columns.forEach { col ->
                Box(
                    contentAlignment = col.headerAlignment,
                    modifier = col.size.apply({ Modifier.weight(it) }, { Modifier.width(it) })
                        .fillMaxHeight().padding(horCellPadding, verCellPadding)
                ) { col.header?.let { Form.Text(it) } }
            }
        }
    }

    @Composable
    private fun <T> Body(
        items: List<T>, rowHeight: Dp, columnBorderSize: Dp,
        horCellPadding: Dp, verCellPadding: Dp, columns: List<Column<T>>,
    ) {
        val density = LocalDensity.current.density
        val scroller = rememberLazyListState()
        var height by remember { mutableStateOf(0.dp) }
        Box(Modifier.fillMaxWidth().onSizeChanged { height = toDP(it.height, density) }) {
            if (items.isEmpty()) EmptyRow()
            else LazyColumn(Modifier, state = scroller) {
                items(items.count()) {
                    Row(items[it], it, rowHeight, columnBorderSize, horCellPadding, verCellPadding, columns)
                }
            }
            Scrollbar.Vertical(rememberScrollbarAdapter(scroller), Modifier.align(Alignment.CenterEnd), height)
        }
    }

    @Composable
    private fun EmptyRow() {
        Box(Modifier.fillMaxSize().background(Theme.colors.background1), Alignment.Center) {
            Form.Text(value = "(" + Label.NONE.lowercase() + ")", alpha = FADED_OPACITY)
        }
    }

    @Composable
    private fun <T> Row(
        item: T, rowID: Int, rowHeight: Dp, columnBorderSize: Dp,
        horCellPadding: Dp, verCellPadding: Dp, columns: List<Column<T>>
    ) {
        Row(Modifier.fillMaxWidth().height(rowHeight), Arrangement.spacedBy(columnBorderSize)) {
            columns.forEach { col ->
                Box(
                    contentAlignment = col.contentAlignment,
                    modifier = col.size.apply({ Modifier.weight(it) }, { Modifier.width(it) })
                        .fillMaxHeight().background(bgColor(rowID))
                        .padding(horCellPadding, verCellPadding)
                ) { col.content(item) }
            }
        }
    }
}