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

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
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

    enum class Code(charCode: UShort) {
        // these codes can be found in resources/icons/blueprint-icons.md
        CaretUp(0x2303u),
        CaretDown(0x2304u),
        ChevronLeft(0xe694u),
        ChevronRight(0xe695u),
        Cog(0xe645u),
        Cross(0x2717u),
        DataLineage(0xe908u),
        Database(0xe683u),
        FloppyDisk(0xe6b7u),
        FolderOpen(0xe651u),
        Graph(0xe673u),
        Heatmap(0xe614u),
        HorizontalBarChartDesc(0xe71du),
        Layout(0xe60cu),
        LogOut(0xe64cu),
        Minus(0x2212u),
        Play(0xe6abu),
        Plus(0x002bu),
        SearchAround(0xe608u),
        Shield(0xe7b2u),
        Stop(0xe6aau),
        Table(0xe667u),
        TimelineBarChart(0xe620u);

        val charString: String = Char(charCode).toString()
    }

    @Composable
    fun Render(icon: Code, color: Color = Theme.colors.primary, size: Size = Size.Size12, modifier: Modifier = Modifier) {
        Text(
            text = icon.charString,
            modifier = modifier,
            color = color,
            fontSize = size.fontSize,
            fontFamily = size.fontFamily
        )
    }
}
