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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme

@Composable
fun StudioTabs(modifier: Modifier = Modifier, orientation: TabOrientation = TabOrientation.HORIZONTAL,
               content: @Composable () -> Unit) {
    when (orientation) {
        TabOrientation.HORIZONTAL -> {
            Row(modifier = modifier.background(StudioTheme.colors.background),
                horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                content()
            }
        }
        else -> {
            Row(modifier = modifier.background(StudioTheme.colors.background)
                .requiredWidth(IntrinsicSize.Max)
                .rotate(if (orientation == TabOrientation.TOP_TO_BOTTOM) 90f else -90f)
                .layout { measurable, constraints ->
                    val placeable: Placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        val offsetX = if (orientation == TabOrientation.TOP_TO_BOTTOM) placeable.width / 2 else -placeable.width / 2
                        placeable.placeRelative(x = offsetX, y = 0)
                    }
                },
                horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                content()
            }
        }
    }
}

@Composable
fun StudioTab(text: String, selected: Boolean, modifier: Modifier = Modifier, leadingIcon: (@Composable () -> Unit)? = null,
              showCloseButton: Boolean = false, onClose: () -> Unit = {}, highlight: TabHighlight = TabHighlight.NONE,
              arrangement: Arrangement.Horizontal = Arrangement.Start, textStyle: TextStyle = StudioTheme.typography.body2) {
    Box(modifier = modifier.width(IntrinsicSize.Max)) {
        val (backgroundColor, highlightColor) = when (selected) {
            true -> listOf(StudioTheme.colors.backgroundHighlight, StudioTheme.colors.primary)
            else -> listOf(StudioTheme.colors.background, StudioTheme.colors.background)
        }

        val highlightAlignment = when (highlight) {
            TabHighlight.TOP -> Alignment.TopStart
            TabHighlight.BOTTOM -> Alignment.BottomEnd
            else -> Alignment.Center
        }

        Row(modifier = Modifier.fillMaxSize().background(backgroundColor), horizontalArrangement = arrangement,
            verticalAlignment = Alignment.CenterVertically) {

            Spacer(Modifier.width(8.dp))
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(4.dp))
            }
            Text(text, style = textStyle)
            if (showCloseButton) {
                Spacer(Modifier.width(4.dp))
                StudioIcon(Icon.Cross, color = StudioTheme.colors.icon.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onClose() })
                Spacer(Modifier.width(4.dp))
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }

        if (highlight != TabHighlight.NONE) {
            Row(modifier = Modifier.align(highlightAlignment).fillMaxWidth().height(2.dp).background(highlightColor)) {}
        }
    }
}

enum class TabOrientation {
    HORIZONTAL,
    BOTTOM_TO_TOP,
    TOP_TO_BOTTOM
}

enum class TabHighlight {
    NONE,
    BOTTOM,
    TOP
}
