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

package com.vaticle.typedb.studio.framework.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.DpSize

sealed class Edge(open val source: Vertex, open val target: Vertex) {

    object Labels {
        const val HAS = "has"
        const val ISA = "isa"
        const val OWNS = "owns"
        const val SUB = "sub"
    }

    val geometry = Geometry(this)
    var curvePoint: com.vaticle.force.graph.api.Vertex? = null
    abstract val label: String

    interface Inferrable {
        val isInferred: Boolean
    }

    // Type edges
    data class Sub(override val source: Vertex.Type, override val target: Vertex.Type) : Edge(source, target) {
        override val label = Labels.SUB
    }

    data class Owns(override val source: Vertex.Type, override val target: Vertex.Type.Attribute) :
        Edge(source, target) {
        override val label = Labels.OWNS
    }

    data class Plays(
        override val source: Vertex.Type.Relation, override val target: Vertex.Type, private val role: String
    ) : Edge(source, target) {
        override val label = role
    }

    // Thing edges
    data class Has(
        override val source: Vertex.Thing, override val target: Vertex.Thing.Attribute,
        override val isInferred: Boolean = false
    ) : Edge(source, target), Inferrable {
        override val label = Labels.HAS
    }

    data class Roleplayer(
        override val source: Vertex.Thing.Relation, override val target: Vertex.Thing, val role: String,
        override val isInferred: Boolean = false
    ) : Edge(source, target), Inferrable {
        override val label = role
    }

    // Thing-to-type edges
    data class Isa(override val source: Vertex.Thing, override val target: Vertex.Type) : Edge(source, target) {
        override val label = Labels.ISA
    }

    data class Geometry(private val edge: Edge) : com.vaticle.force.graph.api.Edge {

        val isCurved get() = edge.curvePoint != null
        val midpoint
            get() = com.vaticle.typedb.studio.framework.common.geometry.Geometry.midpoint(
                edge.source.geometry.position,
                edge.target.geometry.position
            )
        val curveMidpoint
            get() = edge.curvePoint?.let {
                Offset(it.x.toFloat(), it.y.toFloat())
            }

        fun labelRect(dpSize: DpSize, density: Float): Rect {
            val labelCenter = curveMidpoint ?: midpoint
            val size = Size(dpSize.width.value * density, dpSize.height.value * density)
            return Rect(Offset(labelCenter.x - size.width / 2, labelCenter.y - size.height / 2), size)
        }

        override fun source() = edge.source.geometry
        override fun target() = edge.target.geometry
    }
}