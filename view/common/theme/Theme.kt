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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

object Theme {

    val DIALOG_PADDING = 16.dp
    val SCROLLBAR_LONG_PADDING = 4.dp
    val SCROLLBAR_END_PADDING = 6.dp
    val ROUNDED_CORNER_RADIUS = 4.dp
    val ROUNDED_RECTANGLE = RoundedCornerShape(ROUNDED_CORNER_RADIUS)
    const val TARGET_SELECTION_ALPHA = 0.35f
    const val FIND_SELECTION_ALPHA = 0.3f
    const val INDICATION_HOVER_ALPHA = 0.1f
    private const val INDICATION_PRESSED_ALPHA = 0.25f
    private val ColorsState = staticCompositionLocalOf { Color.Themes.DARK }
    private val TypographyState = staticCompositionLocalOf { Typography.Themes.DEFAULT }

    @OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
    private val MaterialThemeOverrides
        @Composable
        @ReadOnlyComposable
        get() = listOf(
            LocalMinimumTouchTargetEnforcement provides false,
            LocalScrollbarStyle provides scrollbarStyle(color = colors.scrollbar),
            LocalIndication provides rectangleIndication(color = colors.indicationBase),
            LocalTextSelectionColors provides TextSelectionColors(
                backgroundColor = colors.tertiary.copy(alpha = TARGET_SELECTION_ALPHA),
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

    fun scrollbarStyle(
        color: androidx.compose.ui.graphics.Color,
        thickness: Dp = 8.dp,
        unhoverAlpha: Float = 0.3f,
        hoverAlpha: Float = 0.6f
    ) =
        ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = thickness,
            shape = RoundedCornerShape(4.dp),
            hoverDurationMillis = 300,
            unhoverColor = color.copy(alpha = unhoverAlpha),
            hoverColor = color.copy(alpha = hoverAlpha)
        )

    private fun roundedCornerRadius(density: Float): CornerRadius {
        return CornerRadius(x = ROUNDED_CORNER_RADIUS.value * density, y = ROUNDED_CORNER_RADIUS.value * density)
    }

    fun leftRoundedIndication(color: androidx.compose.ui.graphics.Color, density: Float): Indication {
        return rawIndication { isPressed, isHovered, isFocused ->
            if (isHovered.value || isFocused.value) {
                drawRect(
                    topLeft = Offset(size.width / 2, 0f),
                    color = color.copy(INDICATION_HOVER_ALPHA),
                    size = Size(size.width / 2, size.height)
                )
                drawRoundRect(
                    color = color.copy(INDICATION_HOVER_ALPHA), size = size, cornerRadius = roundedCornerRadius(density)
                )
            } else if (isPressed.value) drawRoundRect(
                color = color.copy(INDICATION_PRESSED_ALPHA), size = size, cornerRadius = roundedCornerRadius(density)
            )
        }
    }

    fun roundedIndication(color: androidx.compose.ui.graphics.Color, density: Float): Indication {
        return rawIndication { isPressed, isHovered, isFocused ->
            if (isHovered.value || isFocused.value) drawRoundRect(
                color = color.copy(INDICATION_HOVER_ALPHA), size = size, cornerRadius = roundedCornerRadius(density)
            ) else if (isPressed.value) drawRoundRect(
                color = color.copy(INDICATION_PRESSED_ALPHA), size = size, cornerRadius = roundedCornerRadius(density)
            )
        }
    }

    fun rectangleIndication(color: androidx.compose.ui.graphics.Color): Indication {
        return rawIndication { isPressed, isHovered, isFocused ->
            if (isHovered.value || isFocused.value) drawRect(color = color.copy(INDICATION_HOVER_ALPHA), size = size)
            else if (isPressed.value) drawRect(color = color.copy(INDICATION_PRESSED_ALPHA), size = size)
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
