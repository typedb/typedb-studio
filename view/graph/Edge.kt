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

package com.vaticle.typedb.studio.view.graph

import androidx.compose.ui.geometry.Offset

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
    class Sub(override val source: Vertex.Type, override val target: Vertex.Type) : Edge(source, target) {
        override val label = Labels.SUB
        fun copy(source: Vertex.Type, target: Vertex.Type) = Sub(source, target)
    }

    class Owns(override val source: Vertex.Type, override val target: Vertex.Type.Attribute) :
        Edge(source, target) {
        override val label = Labels.OWNS
        fun copy(source: Vertex.Type, target: Vertex.Type.Attribute) = Owns(source, target)
    }

    class Plays(
        override val source: Vertex.Type.Relation, override val target: Vertex.Type, private val role: String
    ) : Edge(source, target) {
        override val label = role
        fun copy(source: Vertex.Type.Relation, target: Vertex.Type) = Plays(source, target, role)
    }

    // Thing edges
    class Has(
        override val source: Vertex.Thing, override val target: Vertex.Thing.Attribute,
        override val isInferred: Boolean = false
    ) : Edge(source, target), Inferrable {

        override val label = Labels.HAS
        fun copy(source: Vertex.Thing, target: Vertex.Thing.Attribute) = Has(source, target, isInferred)
    }

    class Roleplayer(
        override val source: Vertex.Thing.Relation, override val target: Vertex.Thing, val role: String,
        override val isInferred: Boolean = false
    ) : Edge(source, target), Inferrable {

        override val label = role
        fun copy(source: Vertex.Thing.Relation, target: Vertex.Thing) =
            Roleplayer(source, target, role, isInferred)
    }

    // Thing-to-type edges
    class Isa(override val source: Vertex.Thing, override val target: Vertex.Type) : Edge(source, target) {
        override val label = Labels.ISA
        fun copy(source: Vertex.Thing, target: Vertex.Type) = Isa(source, target)
    }

    class Geometry(private val edge: Edge) : com.vaticle.force.graph.api.Edge {

        val isCurved get() = edge.curvePoint != null
        val midpoint get() = com.vaticle.typedb.studio.view.common.geometry.Geometry.midpoint(
            edge.source.geometry.position,
            edge.target.geometry.position
        )
        val curveMidpoint
            get() = edge.curvePoint?.let {
                Offset(it.x.toFloat(), it.y.toFloat())
            }

        override fun source() = edge.source.geometry
        override fun target() = edge.target.geometry
    }
}