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

package com.vaticle.typedb.studio.ui.elements

import androidx.compose.foundation.Image
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.IconSize.*

@Composable
fun StudioIcon(icon: Icon, color: Color = StudioTheme.colors.icon, size: IconSize = Size12, modifier: Modifier = Modifier) {
    Text(text = icon.charString, modifier = modifier, color = color, fontSize = size.fontSize, fontFamily = size.fontFamily)
}

private val blueprintIcons16 = FontFamily(Font(resource = "icons/blueprint-icons-16.ttf"))
private val blueprintIcons20 = FontFamily(Font(resource = "icons/blueprint-icons-20.ttf"))

enum class IconSize(val fontSize: TextUnit, val fontFamily: FontFamily) {
    Size12(fontSize = 12.sp, fontFamily = blueprintIcons16), // WARNING: Blueprint recommends against using this nonstandard size
    Size14(fontSize = 14.sp, fontFamily = blueprintIcons16),
    Size16(fontSize = 16.sp, fontFamily = blueprintIcons16),
    Size16Light(fontSize = 16.sp, fontFamily = blueprintIcons20),
    Size18(fontSize = 18.sp, fontFamily = blueprintIcons20),
    Size20(fontSize = 20.sp, fontFamily = blueprintIcons20),
}

enum class Icon(charCode: UShort) {
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
fun StudioDatabaseIcon() {
    val pixelDensity = LocalDensity.current.density
    when {
        pixelDensity <= 1f -> Image(painter = painterResource("icons/database.png"),
            contentDescription = "Database",
            modifier = Modifier.graphicsLayer(scaleX = pixelDensity, scaleY = pixelDensity))
        else -> Image(painter = loadSvgPainter(ClassLoader.getSystemResourceAsStream("icons/database.svg")!!, LocalDensity.current),
            contentDescription = "Database",
            modifier = Modifier.graphicsLayer(scaleX = 14f / 12f, scaleY = 14f / 12f))
    }
}
