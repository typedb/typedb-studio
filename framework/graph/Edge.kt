/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
    class Sub(override val source: Vertex.Type, override val target: Vertex.Type) : Edge(source, target) {
        override val label = Labels.SUB
    }

    class Owns(override val source: Vertex.Type, override val target: Vertex.Type.Attribute) :
        Edge(source, target) {
        override val label = Labels.OWNS
    }

    class Plays(
        override val source: Vertex.Type.Relation, override val target: Vertex.Type, private val role: String
    ) : Edge(source, target) {
        override val label = role
    }

    // Thing edges
    class Has(
        override val source: Vertex.Thing, override val target: Vertex.Thing.Attribute,
        override val isInferred: Boolean = false
    ) : Edge(source, target), Inferrable {
        override val label = Labels.HAS
    }

    class Roleplayer(
        override val source: Vertex.Thing.Relation, override val target: Vertex.Thing, val role: String,
        override val isInferred: Boolean = false
    ) : Edge(source, target), Inferrable {
        override val label = role
    }

    // Thing-to-type edges
    class Isa(override val source: Vertex.Thing, override val target: Vertex.Type) : Edge(source, target) {
        override val label = Labels.ISA
    }

    class Geometry(private val edge: Edge) : com.vaticle.force.graph.api.Edge {

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
