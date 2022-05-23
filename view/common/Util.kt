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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.client.api.concept.thing.Attribute
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.studio.state.schema.TypeState
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.GraphTheme
import com.vaticle.typedb.studio.view.common.theme.Theme
import java.awt.MouseInfo
import java.awt.Point
import java.time.format.DateTimeFormatter
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

    fun toDP(size: Size, density: Float): DpSize {
        return DpSize(size.width.dp / density, size.height.dp / density)
    }

    fun mousePoint(window: ComposeWindow, titleBarHeight: Dp): Point {
        val raw = MouseInfo.getPointerInfo().location
        return Point(raw.x - window.x, raw.y - window.y - titleBarHeight.value.toInt())
    }

    fun isMouseHover(area: Rect, window: ComposeWindow, titleBarHeight: Dp): Boolean {
        val mouse = mousePoint(window, titleBarHeight)
        return area.contains(mouse.x, mouse.y)
    }

    // TODO: move these methods to a package about type visualisations
    fun typeIcon(type: TypeState.Thing) = when (type) {
        is TypeState.Entity -> Form.IconArg(Icon.Code.RECTANGLE) { GraphTheme.colors.vertex.entityType }
        is TypeState.Relation -> Form.IconArg(Icon.Code.RHOMBUS) { GraphTheme.colors.vertex.relationType }
        is TypeState.Attribute -> Form.IconArg(Icon.Code.OVAL) { GraphTheme.colors.vertex.attributeType }
    }

    // TODO: copied from typeIcon on 23/05/2022, needs refactor
    fun typeIcon(type: Type) = when (type) {
        is RelationType -> Form.IconArg(Icon.Code.RHOMBUS) { GraphTheme.colors.vertex.relationType }
        is AttributeType -> Form.IconArg(Icon.Code.OVAL) { GraphTheme.colors.vertex.attributeType }
        else -> Form.IconArg(Icon.Code.RECTANGLE) { GraphTheme.colors.vertex.entityType }
    }

    fun Attribute<*>.valueString(): String = when {
        isDateTime -> asDateTime().value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        else -> value.toString()
    }

    fun AttributeType.ValueType.schemaString(): String = name.lowercase()
}
