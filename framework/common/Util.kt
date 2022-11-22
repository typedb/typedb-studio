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

package com.vaticle.typedb.studio.framework.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.MouseInfo
import java.awt.Point
import kotlin.math.roundToInt

object Util {

    fun Rect.contains(x: Int, y: Int): Boolean = this.contains(Offset(x.toFloat(), y.toFloat()))

    fun toRectDP(rawRectangle: Rect, density: Float) = Rect(
        left = toDP(rawRectangle.left, density).value,
        top = toDP(rawRectangle.top, density).value,
        right = toDP(rawRectangle.right, density).value,
        bottom = toDP(rawRectangle.bottom, density).value
    )

    fun fromDP(dp: Dp, density: Float): Float = dp.value * density

    fun toDP(pixel: Number, density: Float): Dp {
        return (pixel.toDouble() / density).roundToInt().dp
    }

    fun toDP(size: Size, density: Float): DpSize {
        return DpSize(size.width.dp / density, size.height.dp / density)
    }

    fun mousePoint(window: WindowContext, titleBarHeight: Dp): Point {
        val raw = MouseInfo.getPointerInfo().location
        return Point(raw.x - window.x, raw.y - window.y - titleBarHeight.value.toInt())
    }

    fun isMouseHover(area: Rect, window: WindowContext, titleBarHeight: Dp): Boolean {
        val mouse = mousePoint(window, titleBarHeight)
        return area.contains(mouse.x, mouse.y)
    }

    fun italics(placeholder: String) = AnnotatedString.Builder().apply {
        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
        append(placeholder)
        pop()
    }.toAnnotatedString()

    fun String.hyphenate(): String = this.replace(" ", "-")

    // TODO: Investigate usages of this method -- why were they needed to begin with. Most likely is race condition.
    fun AnnotatedString.subSequenceSafely(start: Int, end: Int): AnnotatedString {
        val coercedStart = start.coerceIn(0, length)
        return this.subSequence(coercedStart, end.coerceIn(coercedStart, length))
    }
}
