/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.common.theme

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
        val Blue1 = Color(0xFF7BA0FF)
        val Green = Color(0xFF02DAC9)
        val Orange = Color(0xFFB0740C)
        val Yellow = Color(0xFFF6C94C)
        val Pink = Color(0xFFFF87DC)
        val Purple1 = Color(0xFF0E0D17)
        val Purple2 = Color(0xFF14121F)
        val Purple3 = Color(0xFF151322)
        val Purple4 = Color(0xFF1A182A)
        val Purple5 = Color(0xFF232135)
        val Purple6 = Color(0xFF2D2A46)
        val Red1 = Color(0xFFCF4A55)
        val Red2 = Color(0xFFFF8080)
        val White = Color(0xFFFFFFFF)
        val White2 = Color(0xFFd5ccff)
    }

    object Themes {
        val DARK_STUDIO = StudioTheme(
            primary = DarkPalette.Purple5,
            secondary = DarkPalette.Green,
            tertiary = DarkPalette.Pink,
            backgroundDark = DarkPalette.Purple1,
            backgroundMedium = DarkPalette.Purple2,
            backgroundLight = DarkPalette.Purple3,
            surface = DarkPalette.Purple4,
            border = DarkPalette.Purple6,
            scrollbar = DarkPalette.Purple6,
            icon = DarkPalette.White,
            warningStroke = DarkPalette.Yellow,
            warningBackground = DarkPalette.Orange,
            errorStroke = DarkPalette.Red2,
            errorBackground = DarkPalette.Red1,
            onPrimary = DarkPalette.White2,
            onSecondary = DarkPalette.Purple1,
            onBackground = DarkPalette.White2,
            onSurface = DarkPalette.White2,
            onError = DarkPalette.White,
            indicationBase = DarkPalette.White,
            isLight = false
        )
        val DARK_GRAPH = GraphTheme(
            background = DarkPalette.Purple1,
            vertex = GraphTheme.Vertex(
                thingType = DarkPalette.Pink,
                entityType = DarkPalette.Pink,
                relationType = DarkPalette.Yellow,
                attributeType = DarkPalette.Blue1,
                entity = DarkPalette.Pink,
                relation = DarkPalette.Yellow,
                attribute = DarkPalette.Blue1,
            ),
            vertexLabel = DarkPalette.Black,
            edge = DarkPalette.Blue1,
            edgeLabel = DarkPalette.Blue1,
            inferred = DarkPalette.Green,
            explanation = DarkPalette.Red1,
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
