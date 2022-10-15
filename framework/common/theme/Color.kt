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

package com.vaticle.typedb.studio.framework.common.theme

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.vaticle.typedb.studio.state.common.util.Label

object Color {

    data class StudioTheme(
        val primary: Color,
        val secondary: Color,
        val tertiary: Color,
        val backgroundDark: Color,
        val backgroundMedium: Color,
        val backgroundLight: Color,
        val surface: Color,
        val border: Color,
        val scrollbar: Color,
        val icon: Color,
        val warningStroke: Color,
        val warningBackground: Color,
        val errorStroke: Color,
        val errorBackground: Color,
        val onPrimary: Color,
        val onSecondary: Color,
        val onBackground: Color,
        val onSurface: Color,
        val onError: Color,
        val indicationBase: Color,
        val isLight: Boolean,
    )

    data class GraphTheme(
        val name: String,
        val background: Color,
        val vertex: Vertex,
        val vertexLabel: Color,
        val edge: Color,
        val edgeLabel: Color,
        val inferred: Color,
        val explanation: Color,
    ) {
        data class Vertex(
            val thingType: Color,
            val entityType: Color,
            val relationType: Color,
            val attributeType: Color,
            val entity: Color,
            val relation: Color,
            val attribute: Color,
        )
        override fun toString(): String {
            return this.name
        }
    }

    private object CommonPalette {
        val Black = Color(0xFF09022F)
        val Blue1 = Color(0xFF5CA8FF)
        val Blue2 = Color(0xFF92E4FC)
        val Green = Color(0xFF02DAC9)
        val Orange1 = Color(0xFFA98119)
        val Orange2 = Color(0xFFD68E11)
        val Orange3 = Color(0xFFF6C94C)
        val Pink1 = Color(0xFFF28DD7)
        val Pink2 = Color(0xFFFFA9E8)
        val Pink3 = Color(0xFFE69CFF)
        val Red1 = Color(0xFFCF4A55)
        val Red2 = Color(0xFFF66B65)
        val White = Color(0xFFFFFFFF)
        val White2 = Color(0xFFd5ccff)
    }

    private object DarkPalette {
        val Purple0 = Color(0xFF080226)
        val Purple1 = Color(0xFF0E053F)
        val Purple2 = Color(0xFF180F49)
        val Purple3 = Color(0xFF1D1354)
        val Purple4 = Color(0xFF261C5E)
        val Purple5 = Color(0xFF372E6A)
        val Purple6 = Color(0xFF392D7F)
        val Purple7 = Color(0xFF544899)
        val Purple8 = Color(0xFF888DCA)
    }

    object Themes {
        val DARK_STUDIO = StudioTheme(
            primary = DarkPalette.Purple4,
            secondary = CommonPalette.Green,
            tertiary = CommonPalette.Pink1,
            backgroundDark = DarkPalette.Purple0,
            backgroundMedium = DarkPalette.Purple1,
            backgroundLight = DarkPalette.Purple2,
            surface = DarkPalette.Purple3,
            border = DarkPalette.Purple6,
            scrollbar = DarkPalette.Purple8,
            icon = DarkPalette.Purple8,
            warningStroke = CommonPalette.Orange3,
            warningBackground = CommonPalette.Orange1,
            errorStroke = CommonPalette.Red2,
            errorBackground = CommonPalette.Red1,
            onPrimary = CommonPalette.White2,
            onSecondary = DarkPalette.Purple0,
            onBackground = CommonPalette.White2,
            onSurface = CommonPalette.White2,
            onError = CommonPalette.White,
            indicationBase = CommonPalette.White,
            isLight = false
        )
        val DARK_GRAPH = GraphTheme(
            name = Label.DARK_GRAPH_NAME,
            background = DarkPalette.Purple0,
            vertex = GraphTheme.Vertex(
                thingType = CommonPalette.Pink1,
                entityType = CommonPalette.Pink1,
                relationType = CommonPalette.Orange2,
                attributeType = CommonPalette.Blue1,
                entity = CommonPalette.Pink3,
                relation = CommonPalette.Orange3,
                attribute = CommonPalette.Blue2,
            ),
            vertexLabel = CommonPalette.Black,
            edge = CommonPalette.Blue1,
            edgeLabel = CommonPalette.Blue1,
            inferred = CommonPalette.Green,
            explanation = CommonPalette.Red1,
        )
        val LIGHT_GRAPH = GraphTheme(
            name = Label.LIGHT_GRAPH_NAME,
            background = CommonPalette.White,
            vertex = GraphTheme.Vertex(
                thingType = CommonPalette.Pink1,
                entityType = CommonPalette.Pink1,
                relationType = CommonPalette.Orange2,
                attributeType = CommonPalette.Blue1,
                entity = CommonPalette.Pink3,
                relation = CommonPalette.Orange3,
                attribute = CommonPalette.Blue2,
            ),
            vertexLabel = CommonPalette.Black,
            edge = CommonPalette.Blue1,
            edgeLabel = CommonPalette.Blue1,
            inferred = CommonPalette.Green,
            explanation = CommonPalette.Red1,
        )
    }

    const val FADED_OPACITY = 0.5f

    fun hexToColor(hexString: String): Color {
        return Color(("ff" + hexString.removePrefix("#").lowercase()).toLong(16))
    }

    fun fadeable(color: Color, faded: Boolean, opacity: Float = FADED_OPACITY): Color {
        return if (color == Color.Transparent) color
        else if (faded) color.copy(opacity)
        else color
    }

    @Composable
    fun materialOf(colors: StudioTheme): Colors {
        // TODO: replace __UNUSED_COLOUR__ the moment we know where they are used
        val __UNUSED_COLOR__ = Color.Magenta
        return Colors(
            primary = colors.primary,
            primaryVariant = __UNUSED_COLOR__,
            secondary = __UNUSED_COLOR__,
            secondaryVariant = __UNUSED_COLOR__,
            background = colors.backgroundMedium,
            surface = colors.surface,
            error = colors.errorBackground,
            onPrimary = colors.onPrimary,
            onSecondary = __UNUSED_COLOR__,
            onBackground = colors.onBackground,
            onSurface = colors.onSurface,
            onError = __UNUSED_COLOR__,
            isLight = colors.isLight
        )
    }
}