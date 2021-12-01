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

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumTouchTargetEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

object Theme {

    const val ROUNDED_CORNER_RADIUS = 4f
    val ROUNDED_CORNER_SIZE = CornerSize(ROUNDED_CORNER_RADIUS.dp)
    private const val DEFAULT_SELECTION_TRANSPARENCY = 0.7f
    private val ColorsState = staticCompositionLocalOf { Color.Themes.DARK }
    private val TypographyState = staticCompositionLocalOf { Typography.Themes.DEFAULT }

    @OptIn(ExperimentalMaterialApi::class)
    private val MaterialThemeOverrides
        @Composable
        @ReadOnlyComposable
        get() = listOf(
            LocalMinimumTouchTargetEnforcement provides false,
            LocalIndication provides rectangleIndication(color = colors.indicationBase),
            LocalTextSelectionColors provides TextSelectionColors(
                backgroundColor = colors.tertiary.copy(alpha = DEFAULT_SELECTION_TRANSPARENCY),
                handleColor = colors.tertiary
            )
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

    // TODO: this may be more suitable in a utility class when we have one
    fun toDP(pixel: Number, density: Float): Dp {
        return (pixel.toDouble() / density).roundToInt().dp
    }

    fun roundedCornerRadius(density: Float): CornerRadius {
        return CornerRadius(x = ROUNDED_CORNER_RADIUS * density, y = ROUNDED_CORNER_RADIUS * density)
    }

    fun roundedIndication(color: androidx.compose.ui.graphics.Color, density: Float): Indication {
        return rawIndication { isPressed, isHovered, isFocused ->
            if (isHovered.value || isFocused.value) drawRoundRect(
                color = color.copy(0.1f), size = size, cornerRadius = roundedCornerRadius(density)
            )
            else if (isPressed.value) drawRoundRect(
                color = color.copy(0.25f), size = size, cornerRadius = roundedCornerRadius(density)
            )
        }
    }

    fun rectangleIndication(color: androidx.compose.ui.graphics.Color): Indication {
        return rawIndication { isPressed, isHovered, isFocused ->
            if (isHovered.value || isFocused.value) drawRect(color = color.copy(0.1f), size = size)
            else if (isPressed.value) drawRect(color = color.copy(0.25f), size = size)
        }
    }

    private fun rawIndication(
        indication: ContentDrawScope.(isPressed: State<Boolean>, isHovered: State<Boolean>, isFocused: State<Boolean>) -> Unit
    ): Indication {
        return object : Indication {
            @Composable
            override fun rememberUpdatedInstance(src: InteractionSource): IndicationInstance {
                val isPressed = src.collectIsPressedAsState()
                val isHovered = src.collectIsHoveredAsState()
                val isFocused = src.collectIsFocusedAsState()
                return object : IndicationInstance {
                    override fun ContentDrawScope.drawIndication() {
                        drawContent()
                        indication(isPressed, isHovered, isFocused)
                    }
                }
            }
        }
    }
}
