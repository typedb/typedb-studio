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

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumTouchTargetEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

object Theme {

    private const val DEFAULT_SELECTION_TRANSPARENCY = 0.7f
    private val ColorsState = staticCompositionLocalOf { Color.Themes.DARK }
    private val TypographyState = staticCompositionLocalOf { Typography.Themes.DEFAULT }

    @OptIn(ExperimentalMaterialApi::class)
    private val MaterialThemeOverrides
        @Composable
        @ReadOnlyComposable
        get() = listOf(
            LocalMinimumTouchTargetEnforcement provides false,
            LocalTextSelectionColors provides TextSelectionColors(
                backgroundColor = colors.tertiary.copy(alpha = DEFAULT_SELECTION_TRANSPARENCY),
                handleColor = colors.tertiary
            ),
        )

    val colors: Color.Theme
        @Composable
        @ReadOnlyComposable
        get() = ColorsState.current

    val typography: Typography.Theme
        @Composable
        @ReadOnlyComposable
        get() = TypographyState.current

    @Composable
    fun Material(content: @Composable () -> Unit) {
        MaterialTheme(colors = Color.materialOf(colors), typography = Typography.materialOf(typography)) {
            CompositionLocalProvider(*MaterialThemeOverrides.toTypedArray()) {
                content()
            }
        }
    }

    fun toDP(pixel: Number, pixelDensity: Float): Dp {
        return (pixel.toDouble() / pixelDensity).roundToInt().dp
    }
}
