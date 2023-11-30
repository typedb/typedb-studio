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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Theme {

    val DIALOG_PADDING = 16.dp
    val PANEL_BAR_HEIGHT = 28.dp
    val PANEL_BAR_SPACING = 8.dp
    val SCROLLBAR_LONG_PADDING = 4.dp
    val SCROLLBAR_END_PADDING = 6.dp
    val TOOLBAR_SIZE = 34.dp
    val TOOLBAR_SPACING = 5.dp
    val TOOLBAR_BUTTON_SIZE = 24.dp
    val TOOLBAR_SEPARATOR_HEIGHT = 20.dp
    val ROUNDED_CORNER_RADIUS = 4.dp
    val ROUNDED_CORNER_SHAPE = RoundedCornerShape(ROUNDED_CORNER_RADIUS)
    const val TARGET_SELECTION_ALPHA = 0.25f
    const val FIND_SELECTION_ALPHA = 0.25f
    const val INDICATION_HOVER_ALPHA = 0.1f
    private const val INDICATION_PRESSED_ALPHA = 0.2f
    private val StudioColorsState = staticCompositionLocalOf { Color.Themes.DARK_STUDIO }
    private val GraphColorsState = staticCompositionLocalOf { Color.Themes.DARK_GRAPH }
    private val TypographyState = staticCompositionLocalOf { Typography.Themes.DEFAULT }

    enum class RoundedCorners(val topLeft: Float, val topRight: Float, val bottomRight: Float, val bottomLeft: Float) {
        LEFT(4f, 0f, 0f, 4f),
        RIGHT(0f, 4f, 4f, 0f),
        ALL(4f, 4f, 4f, 4f),
        NONE(0f, 0f, 0f, 0f);

        fun shape(density: Float): RoundedCornerShape {
            return RoundedCornerShape(
                topStart = topLeft * density,
                topEnd = topRight * density,
                bottomEnd = bottomRight * density,
                bottomStart = bottomLeft * density
            )
        }

        fun rectangle(size: Size, density: Float) = RoundRect(
            0f, 0f, size.width, size.height,
            CornerRadius(topLeft * density),
            CornerRadius(topRight * density),
            CornerRadius(bottomRight * density),
            CornerRadius(bottomLeft * density)
        )
    }

    @OptIn(ExperimentalMaterialApi::class)
    private val MaterialThemeOverrides
        @Composable
        @ReadOnlyComposable
        get() = listOf(
            LocalMinimumTouchTargetEnforcement provides false,
            LocalScrollbarStyle provides scrollbarStyle(studio.scrollbar),
            LocalIndication provides rectangleIndication(studio.indicationBase, 1f, RoundedCorners.NONE),
            LocalTextSelectionColors provides TextSelectionColors(
                backgroundColor = studio.tertiary.copy(alpha = TARGET_SELECTION_ALPHA),
                handleColor = studio.tertiary
            )
        )

    val studio: Color.StudioTheme
        @Composable
        @ReadOnlyComposable
        get() = StudioColorsState.current

    val graph: Color.GraphTheme
        @Composable
        @ReadOnlyComposable
        get() = GraphColorsState.current

    val typography: Typography.Theme
        @Composable
        @ReadOnlyComposable
        get() = TypographyState.current

    @Composable
    fun Material(content: @Composable () -> Unit) {
        MaterialTheme(colors = Color.materialOf(studio), typography = Typography.materialOf(typography)) {
            CompositionLocalProvider(*MaterialThemeOverrides.toTypedArray()) { content() }
        }
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

    fun rectangleIndication(
        color: androidx.compose.ui.graphics.Color, density: Float, roundedCorners: RoundedCorners = RoundedCorners.ALL
    ): Indication {
        return rawIndication { isPressed, isHovered, isFocused ->
            val path = Path().apply { addRoundRect(roundedCorners.rectangle(size, density)) }
            if (isPressed.value) drawPath(path, color.copy(INDICATION_PRESSED_ALPHA))
            else if (isHovered.value || isFocused.value) drawPath(path, color.copy(INDICATION_HOVER_ALPHA))
        }
    }

    private fun rawIndication(
        indication: ContentDrawScope.(isPressed: State<Boolean>, isHovered: State<Boolean>, isFocused: State<Boolean>) -> Unit
    ): Indication {
        return object : Indication {
            @Composable
            override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
                val isPressed = interactionSource.collectIsPressedAsState()
                val isHovered = interactionSource.collectIsHoveredAsState()
                val isFocused = interactionSource.collectIsFocusedAsState()
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
