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

package com.vaticle.typedb.studio.view.common

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.schema.TypeState
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Theme
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

    fun toDP(pixel: Number, density: Float): Dp {
        return (pixel.toDouble() / density).roundToInt().dp
    }

    fun mousePoint(window: ComposeWindow, titleBarHeight: Dp): Point {
        val raw = MouseInfo.getPointerInfo().location
        return Point(raw.x - window.x, raw.y - window.y - titleBarHeight.value.toInt())
    }

    fun isMouseHover(area: Rect, window: ComposeWindow, titleBarHeight: Dp): Boolean {
        val mouse = mousePoint(window, titleBarHeight)
        return area.contains(mouse.x, mouse.y)
    }

    // TODO: may move this method to a package about type visualisations once we implement graph visualiser
    fun typeIcon(type: TypeState) = when {
        type.isEntityType -> Form.IconArg(Icon.Code.RECTANGLE) { Theme.colors.tertiary }
        type.isRelationType -> Form.IconArg(Icon.Code.RHOMBUS) { Theme.colors.quaternary }
        type.isAttributeType -> Form.IconArg(Icon.Code.OVAL) { Theme.colors.quinary }
        else -> throw IllegalStateException("Unrecognised Schema Type: $type")
    }
}