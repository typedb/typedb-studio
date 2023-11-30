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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.framework.common.Util.toDP
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.service.common.util.Label

object Table {

    data class Column<T> constructor(
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
    private fun bgColor(isHover: Boolean, i: Int): Color = when {
        isHover -> Theme.studio.indicationBase.copy(alpha = Theme.INDICATION_HOVER_ALPHA)
        i % 2 == 0 -> Theme.studio.backgroundLight
        else -> Theme.studio.backgroundMedium
    }

    @Composable
    fun <T> Layout(
        items: List<T>,
        modifier: Modifier = Modifier,
        showHeader: Boolean = true,
        rowHeight: Dp = ROW_HEIGHT,
        columnBorderSize: Dp = COLUMN_BORDER_SIZE,
        horCellPadding: Dp = CELL_PADDING_HORIZONTAL,
        verCellPadding: Dp = CELL_PADDING_VERTICAL,
        contextMenuFn: ((item: T) -> List<List<ContextMenu.Item>>)? = null,
        columns: List<Column<T>>,
    ) = Column(modifier.background(Theme.studio.backgroundMedium)) {
        if (showHeader) Header(rowHeight, columnBorderSize, horCellPadding, verCellPadding, columns)
        Body(items, rowHeight, columnBorderSize, horCellPadding, verCellPadding, columns, contextMenuFn)
    }

    @Composable
    private fun <T> Header(
        rowHeight: Dp, columnBorderSize: Dp,
        horCellPadding: Dp, verCellPadding: Dp, columns: List<Column<T>>
    ) = Row(
        modifier = Modifier.fillMaxWidth().height(rowHeight).background(Theme.studio.backgroundDark),
        horizontalArrangement = Arrangement.spacedBy(columnBorderSize)
    ) {
        columns.forEach { col ->
            Box(
                contentAlignment = col.headerAlignment,
                modifier = col.size.apply({ Modifier.weight(it) }, { Modifier.width(it) })
                    .fillMaxHeight().padding(horCellPadding, verCellPadding)
            ) { col.header?.let { Form.Text(it) } }
        }
    }

    @Composable
    private fun <T> Body(
        items: List<T>,
        rowHeight: Dp,
        columnBorderSize: Dp,
        horCellPadding: Dp,
        verCellPadding: Dp,
        columns: List<Column<T>>,
        contextMenuFn: ((item: T) -> List<List<ContextMenu.Item>>)?,
    ) {
        val density = LocalDensity.current.density
        val scroller = rememberLazyListState()
        var height by remember { mutableStateOf(0.dp) }
        Box(Modifier.fillMaxWidth().onSizeChanged { height = toDP(it.height, density) }) {
            if (items.isEmpty()) EmptyRow()
            else LazyColumn(Modifier, state = scroller) {
                items(items.count()) {
                    Row(
                        items[it], it, rowHeight, columnBorderSize,
                        horCellPadding, verCellPadding, columns, contextMenuFn
                    )
                }
            }
            Scrollbar.Vertical(rememberScrollbarAdapter(scroller), Modifier.align(Alignment.CenterEnd), height)
        }
    }

    @Composable
    private fun EmptyRow() = Box(
        modifier = Modifier.fillMaxSize().background(Theme.studio.backgroundMedium),
        contentAlignment = Alignment.Center
    ) { Form.Text(value = Label.NONE_IN_PARENTHESES.lowercase()) }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun <T> Row(
        item: T,
        rowID: Int,
        rowHeight: Dp,
        columnBorderSize: Dp,
        horCellPadding: Dp,
        verCellPadding: Dp,
        columns: List<Column<T>>,
        contextMenuFn: ((item: T) -> List<List<ContextMenu.Item>>)?
    ) {
        val contextMenuState = remember { ContextMenu.State() }
        var isHover by remember { mutableStateOf(false) }
        Box {
            var modifier = Modifier.fillMaxWidth().height(rowHeight)
            contextMenuFn?.let { fn ->
                ContextMenu.Popup(contextMenuState) { fn(item) }
                modifier = modifier.pointerHoverIcon(PointerIconDefaults.Hand)
                    .pointerInput(item) { contextMenuState.onPointerInput(this) }
                    .onPointerEvent(Enter) { isHover = true }
                    .onPointerEvent(Exit) { isHover = false }
            }
            Row(modifier) {
                columns.forEachIndexed { i, col ->
                    Box(
                        contentAlignment = col.contentAlignment,
                        modifier = col.size.apply({ Modifier.weight(it) }, { Modifier.width(it) })
                            .fillMaxHeight().background(bgColor(isHover, rowID))
                            .padding(horCellPadding, verCellPadding)
                    ) { col.content(item) }
                    if (i < columns.size - 1) Spacer(
                        Modifier.height(rowHeight).width(columnBorderSize).background(Theme.studio.backgroundDark)
                    )
                }
            }
        }
    }
}
