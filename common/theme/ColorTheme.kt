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

package com.vaticle.typedb.studio.common.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Stable
data class ColorTheme(
    val primary: Color, val secondary: Color, val background: Color, val surface: Color, val surface2: Color,
    val onPrimary: Color, val onBackground: Color, val onSurface: Color, val error: Color, private val isLight: Boolean
) {
    @Composable
    @ReadOnlyComposable
    internal fun materialColors(): androidx.compose.material.Colors {
        return androidx.compose.material.Colors(
            primary = Theme.colors.primary,
            primaryVariant = UNUSED_COLOR,
            secondary = UNUSED_COLOR,
            secondaryVariant = UNUSED_COLOR,
            background = Theme.colors.background,
            surface = Theme.colors.surface,
            error = Theme.colors.error,
            onPrimary = Theme.colors.onPrimary,
            onSecondary = UNUSED_COLOR,
            onBackground = Theme.colors.onBackground,
            onSurface = Theme.colors.onSurface,
            onError = UNUSED_COLOR,
            isLight = isLight
        )
    }

    object Palette {
        val Purple0 = Color(0xFF08022E)
        val Purple1 = Color(0xFF0E053F)
        val Purple2 = Color(0xFF180F49)
        val Purple3 = Color(0xFF1D1354)
        val Purple4 = Color(0xFF261C5E)
        val Purple5 = Color(0xFF372E6A)
        val Purple6 = Color(0xFF392D7F)
        val Purple7 = Color(0xFF544899)
        val Purple8 = Color(0xFF888DCA)
        val Green = Color(0xFF02DAC9)
        val Red1 = Color(0xFFF66B65)
        val Red2 = Color(0xFFFFA187)
        val Yellow1 = Color(0xFFF6C94C)
        val Yellow2 = Color(0xFFFFE4A7)
        val Pink1 = Color(0xFFF28DD7)
        val Pink2 = Color(0xFFFFA9E8)
        val White = Color.White
    }

    object Presets {
        val dark = ColorTheme(
            primary = Palette.Purple4,
            secondary = Palette.Green,
            background = Palette.Purple1,
            surface = Palette.Purple3,
            surface2 = Palette.Purple6,
            onPrimary = Palette.White,
            onBackground = Palette.White,
            onSurface = Palette.White,
            error = Palette.Red1,
            isLight = false
        )
    }

    object ColorExtensions {
        fun Color.toSwingColor() = java.awt.Color(red, green, blue, alpha)
    }
}

/**
 * Filler colour for Material Design colours that we don't know the purpose of.
 * We use a clearly visible colour that will stand out if it ever appears in the application.
 */
val UNUSED_COLOR = Color.Magenta
