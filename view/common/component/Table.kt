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
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.theme.Theme

object Table {

    data class Column<T>(
        val header: AnnotatedString?,
        val contentAlignment: Alignment = Alignment.Center,
        val headerAlignment: Alignment = Alignment.Center,
        val size: Either<Dp, Float> = Either.second(1f),
        val content: @Composable (T) -> Unit
    ) {
        constructor(
            header: String,
            contentAlignment: Alignment = Alignment.Center,
            headerAlignment: Alignment = Alignment.Center,
            size: Either<Dp, Float> = Either.second(1f),
            function: @Composable (T) -> Unit
        ) : this(AnnotatedString(header), contentAlignment, headerAlignment, size, function)
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
        cellPaddingHorizontal: Dp = CELL_PADDING_HORIZONTAL,
        cellPaddingVertical: Dp = CELL_PADDING_VERTICAL,
        columns: List<Column<T>>
    ) {
        val density = LocalDensity.current.density
        val scroller = rememberLazyListState()
        var height by remember { mutableStateOf(0.dp) }
        Column(modifier.border(1.dp, Theme.colors.border).onSizeChanged { height = toDP(it.height, density) }) {
            Row(Modifier.fillMaxWidth().height(rowHeight)) {
                columns.forEach { col ->
                    Box(
                        contentAlignment = col.headerAlignment,
                        modifier = col.size.apply({ Modifier.width(it) }, { Modifier.weight(it) })
                            .fillMaxHeight().padding(cellPaddingHorizontal, cellPaddingVertical)
                    ) { col.header?.let { Form.Text(it) } }
                }
            }
            Box(Modifier.fillMaxWidth().height(height - rowHeight)) {
                LazyColumn(Modifier, state = scroller) {
                    items(items.count()) { rowID ->
                        Row(Modifier.fillMaxWidth().height(rowHeight), Arrangement.spacedBy(columnBorderSize)) {
                            columns.forEach { col ->
                                Box(
                                    contentAlignment = col.contentAlignment,
                                    modifier = col.size.apply({ Modifier.width(it) }, { Modifier.weight(it) })
                                        .fillMaxHeight().background(bgColor(rowID))
                                        .padding(cellPaddingHorizontal, cellPaddingVertical)
                                ) { col.content(items[rowID]) }
                            }
                        }
                    }
                }
                Scrollbar.Vertical(
                    adapter = rememberScrollbarAdapter(scroller),
                    modifier = Modifier.align(Alignment.CenterEnd),
                    containerSize = height - rowHeight
                )
            }
        }
    }
}