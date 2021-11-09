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

package com.vaticle.typedb.studio.appearance

import androidx.compose.ui.graphics.Color
import com.vaticle.typedb.studio.data.VertexEncoding
import com.vaticle.typedb.studio.data.VertexEncoding.*

private val Gold = Color(0xFFEBC53D)
private val SkyBlue = Color(0xFF92E4FC)
private val Blue = Color(0xFF7BA0FF)
private val Pink = Color(0xFFE69CFF)
private val Black = Color(0xFF09022F)

data class VisualiserTheme(
    val background: Color,
    val vertex: Map<VertexEncoding, Color>,
    val edge: Color,
    val inferred: Color,
    val explanation: Color,
    val error: Color,
    val vertexLabel: Color,
) {
    companion object {
        val Default = VisualiserTheme(
            background = VaticlePalette.Purple0,
            vertex = mapOf(
                THING_TYPE to VaticlePalette.Pink2,
                ENTITY_TYPE to VaticlePalette.Pink2,
                RELATION_TYPE to VaticlePalette.Yellow2,
                ATTRIBUTE_TYPE to Blue,
                ENTITY to Pink,
                RELATION to Gold,
                ATTRIBUTE to SkyBlue,
            ),
            edge = Blue,
            inferred = VaticlePalette.Green,
            explanation = VaticlePalette.Red1,
            error = VaticlePalette.Red1,
            vertexLabel = Black,
        )
    }
}
