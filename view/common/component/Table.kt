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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.view.common.theme.Theme

object Table {

    data class Column<T>(
        val header: AnnotatedString?,
        val alignment: Alignment = Alignment.Center,
        val size: Either<Dp, Float> = Either.second(1f),
        val content: @Composable (T) -> Unit
    ) {
        constructor(
            header: String,
            alignment: Alignment = Alignment.Center,
            size: Either<Dp, Float> = Either.second(1f),
            function: @Composable (T) -> Unit
        ) : this(AnnotatedString(header), alignment, size, function)
    }

    val ROW_HEIGHT = 34.dp
    private val COLUMN_BORDER_SIZE = 2.dp
    private val COLUMN_HORIZONTAL_PADDING = 8.dp

    @Composable
    private fun bgColor(i: Int): Color = if (i % 2 == 0) Theme.colors.background2 else Theme.colors.background1

    @Composable
    fun <T> Layout(
        items: List<T>,
        modifier: Modifier = Modifier,
        rowHeight: Dp = ROW_HEIGHT,
        columns: List<Column<T>>
    ) {
        LazyColumn(modifier.border(1.dp, Theme.colors.border)) {
            item {
                Row(Modifier.fillMaxWidth().height(rowHeight)) {
                    columns.forEach { col ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = col.size.apply({ Modifier.width(it) }, { Modifier.weight(it) }).fillMaxHeight()
                        ) { col.header?.let { Form.Text(it) } }
                    }
                }
            }
            items(items.count()) { rowID ->
                Row(Modifier.fillMaxWidth().height(rowHeight), Arrangement.spacedBy(COLUMN_BORDER_SIZE)) {
                    columns.forEach { col ->
                        Box(
                            contentAlignment = col.alignment,
                            modifier = col.size.apply({ Modifier.width(it) }, { Modifier.weight(it) })
                                .fillMaxHeight().background(bgColor(rowID))
                                .padding(horizontal = COLUMN_HORIZONTAL_PADDING)
                        ) { col.content(items[rowID]) }
                    }
                }
            }
        }
    }
}