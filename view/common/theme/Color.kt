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

package com.vaticle.typedb.studio.view.common.theme

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Color {

    data class StudioTheme(
        val primary: Color,
        val secondary: Color,
        val tertiary: Color,
        val tertiary2: Color,
        val background0: Color,
        val background1: Color,
        val background2: Color,
        val surface: Color,
        val surface2: Color,
        val border: Color,
        val scrollbar: Color,
        val icon: Color,
        val warning: Color,
        val warning2: Color,
        val error: Color,
        val error2: Color,
        val onPrimary: Color,
        val onSecondary: Color,
        val onBackground: Color,
        val onSurface: Color,
        val onError: Color,
        val indicationBase: Color,
        val isLight: Boolean,
    )

    data class GraphTheme(
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
    }

    private object DarkPalette {
        val Black = Color(0xFF09022F)
        val Blue1 = Color(0xFF5CA8FF)
        val Blue2 = Color(0xFF92E4FC)
        val Green = Color(0xFF02DAC9)
        val Orange1 = Color(0xFFd68e11)
        val Orange2 = Color(0xFFF6C94C)
        val Orange3 = Color(0xFFEBC53D)
        val Pink1 = Color(0xFFF28DD7)
        val Pink2 = Color(0xFFFFA9E8)
        val Pink3 = Color(0xFFE69CFF)
        val Purple0 = Color(0xFF080226)
        val Purple1 = Color(0xFF0E053F)
        val Purple2 = Color(0xFF180F49)
        val Purple3 = Color(0xFF1D1354)
        val Purple4 = Color(0xFF261C5E)
        val Purple5 = Color(0xFF372E6A)
        val Purple6 = Color(0xFF392D7F)
        val Purple7 = Color(0xFF544899)
        val Purple8 = Color(0xFF888DCA)
        val Red1 = Color(0xFFF66B65)
        val Red2 = Color(0xFFCF4A55)
        val White = Color(0xFFFFFFFF)
        val White2 = Color(0xFFd5ccff)
    }

    object Themes {
        val DARK_STUDIO = StudioTheme(
            primary = DarkPalette.Purple4,
            secondary = DarkPalette.Green,
            tertiary = DarkPalette.Pink1,
            tertiary2 = DarkPalette.Pink2,
            background0 = DarkPalette.Purple0,
            background1 = DarkPalette.Purple1,
            background2 = DarkPalette.Purple2,
            surface = DarkPalette.Purple3,
            surface2 = DarkPalette.Purple5,
            border = DarkPalette.Purple6,
            scrollbar = DarkPalette.Purple8,
            icon = DarkPalette.Purple8,
            warning = DarkPalette.Orange1,
            warning2 = DarkPalette.Orange2,
            error = DarkPalette.Red1,
            error2 = DarkPalette.Red2,
            onPrimary = DarkPalette.White2,
            onSecondary = DarkPalette.White,
            onBackground = DarkPalette.White2,
            onSurface = DarkPalette.White2,
            onError = DarkPalette.White,
            indicationBase = DarkPalette.White,
            isLight = false
        )
        val DARK_GRAPH = GraphTheme(
            background = DarkPalette.Purple0,
            vertex = GraphTheme.Vertex(
                thingType = DarkPalette.Pink1,
                entityType = DarkPalette.Pink1,
                relationType = DarkPalette.Orange1,
                attributeType = DarkPalette.Blue1,
                entity = DarkPalette.Pink3,
                relation = DarkPalette.Orange3,
                attribute = DarkPalette.Blue2,
            ),
            vertexLabel = DarkPalette.Black,
            edge = DarkPalette.Blue1,
            edgeLabel = DarkPalette.Blue1,
            inferred = DarkPalette.Green,
            explanation = DarkPalette.Red2,
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
            background = colors.background1,
            surface = colors.surface,
            error = colors.error2,
            onPrimary = colors.onPrimary,
            onSecondary = __UNUSED_COLOR__,
            onBackground = colors.onBackground,
            onSurface = colors.onSurface,
            onError = __UNUSED_COLOR__,
            isLight = colors.isLight
        )
    }
}