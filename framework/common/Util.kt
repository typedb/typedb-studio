/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.MouseInfo
import java.awt.Point
import kotlin.math.roundToInt

object Util {

    const val ELLIPSES = "..."

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

    fun mousePoint(windowCtx: WindowContext, titleBarHeight: Dp): Point {
        return if (windowCtx is WindowContext.Compose) {
            val raw = MouseInfo.getPointerInfo().location
            Point(raw.x - windowCtx.x, raw.y - windowCtx.y - titleBarHeight.value.toInt())
        } else {
            Point(0, 0)
        }
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

    fun mayTruncate(string: String, length: Int): String {
        return if (string.length <= length) string else string.take(length) + ELLIPSES
    }

    fun String.hyphenate(): String = this.replace(" ", "-")

    // TODO: Investigate usages of this method -- why were they needed to begin with. Most likely is race condition.
    fun AnnotatedString.subSequenceSafely(start: Int, end: Int): AnnotatedString {
        val coercedStart = start.coerceIn(0, length)
        return this.subSequence(coercedStart, end.coerceIn(coercedStart, length))
    }

    // TODO: Investigate usages of this method -- why were they needed to begin with. Most likely is race condition.
    fun TextLayoutResult.getCursorRectSafely(i: Int) = this.getCursorRect(i.coerceAtMost(this.getLineEnd(0)))
}
