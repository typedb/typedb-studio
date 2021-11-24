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

package com.vaticle.typedb.studio.common.component

import androidx.compose.foundation.focusable
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.common.theme.Color.fadeable
import com.vaticle.typedb.studio.common.theme.Theme

object Icon {

    private val BLUEPRINT_ICONS_16 = FontFamily(Font(resource = "icons/blueprint-icons-16.ttf"))
    private val BLUEPRINT_ICONS_20 = FontFamily(Font(resource = "icons/blueprint-icons-20.ttf"))

    enum class Size(val fontSize: TextUnit, val fontFamily: FontFamily) {
        // WARNING: Blueprint recommends against using this nonstandard size
        Size12(fontSize = 12.sp, fontFamily = BLUEPRINT_ICONS_16),
        Size14(fontSize = 14.sp, fontFamily = BLUEPRINT_ICONS_16),
        Size16(fontSize = 16.sp, fontFamily = BLUEPRINT_ICONS_16),
        Size16Light(fontSize = 16.sp, fontFamily = BLUEPRINT_ICONS_20), // TODO: why do we need this exception?
        Size18(fontSize = 18.sp, fontFamily = BLUEPRINT_ICONS_20),
        Size20(fontSize = 20.sp, fontFamily = BLUEPRINT_ICONS_20),
    }

    enum class Code(charCode: UShort, val defaultSize: Size) {
        // these codes can be found in resources/icons/blueprint-icons.md
        CaretUp(0x2303u, Size.Size12),
        CaretDown(0x2304u, Size.Size16),
        ChevronLeft(0xe694u, Size.Size12),
        ChevronRight(0xe695u, Size.Size12),
        Cog(0xe645u, Size.Size12),
        Cross(0x2717u, Size.Size12),
        DataLineage(0xe908u, Size.Size12),
        Database(0xe683u, Size.Size12),
        FloppyDisk(0xe6b7u, Size.Size12),
        FolderOpen(0xe651u, Size.Size14),
        Graph(0xe673u, Size.Size12),
        Heatmap(0xe614u, Size.Size12),
        HorizontalBarChartDesc(0xe71du, Size.Size12),
        Layout(0xe60cu, Size.Size12),
        LogOut(0xe64cu, Size.Size12),
        Minus(0x2212u, Size.Size12),
        Play(0xe6abu, Size.Size16),
        Plus(0x002bu, Size.Size12),
        SearchAround(0xe608u, Size.Size12),
        Shield(0xe7b2u, Size.Size12),
        Stop(0xe6aau, Size.Size12),
        Table(0xe667u, Size.Size12),
        TimelineBarChart(0xe620u, Size.Size12);

        val charString: String = Char(charCode).toString()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Render(
        icon: Code,
        size: Size = icon.defaultSize,
        color: Color = Theme.colors.icon,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        Text(
            text = icon.charString,
            color = fadeable(color, !enabled),
            fontSize = size.fontSize,
            fontFamily = size.fontFamily,
            modifier = modifier.pointerIcon(PointerIcon.Hand).focusable(true)
        )
    }
}
