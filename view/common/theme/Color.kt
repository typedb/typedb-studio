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

    data class Theme(
        val primary: Color,
        val secondary: Color,
        val tertiary: Color,
        val tertiary2: Color,
        val quaternary: Color,
        val quaternary2: Color,
        val background: Color,
        val background2: Color,
        val surface: Color,
        val surface2: Color,
        val border: Color,
        val icon: Color,
        val error: Color,
        val error2: Color,
        val onPrimary: Color,
        val onSecondary: Color,
        val onBackground: Color,
        val onSurface: Color,
        val onError: Color,
        val isLight: Boolean,
    )

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
        val Green = Color(0xFF02DAC9)
        val Red1 = Color(0xFFFA5765)
        val Red2 = Color(0xFFF66B65)
        val Yellow1 = Color(0xFFF6C94C)
        val Yellow2 = Color(0xFFFFE4A7)
        val Pink1 = Color(0xFFF28DD7)
        val Pink2 = Color(0xFFFFA9E8)
        val White = Color(0xFFFFFFFF)
        val White2 = Color(0xFFd5ccff)
    }

    object Themes {
        val DARK = Theme(
            primary = DarkPalette.Purple4,
            secondary = DarkPalette.Green,
            tertiary = DarkPalette.Pink1,
            tertiary2 = DarkPalette.Pink2,
            quaternary = DarkPalette.Yellow1,
            quaternary2 = DarkPalette.Yellow2,
            background = DarkPalette.Purple1,
            background2 = DarkPalette.Purple0,
            surface = DarkPalette.Purple3,
            surface2 = DarkPalette.Purple5,
            border = DarkPalette.Purple6,
            icon = DarkPalette.Purple8,
            error = DarkPalette.Red1,
            error2 = DarkPalette.Red2,
            onPrimary = DarkPalette.White2,
            onSecondary = DarkPalette.White,
            onBackground = DarkPalette.White2,
            onSurface = DarkPalette.White2,
            onError = DarkPalette.White,
            isLight = false
        )
    }

    private const val FADED_OPACITY = 0.4f

    fun fadeable(color: Color, faded: Boolean): Color {
        return if (faded) color.copy(alpha = FADED_OPACITY) else color
    }

    @Composable
    fun materialOf(colors: Theme): Colors {
        // TODO: replace __UNUSED_COLOUR__ the moment we know where they are used
        val __UNUSED_COLOR__ = Color.Magenta
        return Colors(
            primary = colors.primary,
            primaryVariant = __UNUSED_COLOR__,
            secondary = __UNUSED_COLOR__,
            secondaryVariant = __UNUSED_COLOR__,
            background = colors.background,
            surface = colors.surface,
            error = colors.error,
            onPrimary = colors.onPrimary,
            onSecondary = __UNUSED_COLOR__,
            onBackground = colors.onBackground,
            onSurface = colors.onSurface,
            onError = __UNUSED_COLOR__,
            isLight = colors.isLight
        )
    }
}