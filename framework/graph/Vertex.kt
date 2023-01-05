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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.vaticle.force.graph.impl.BasicVertex
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.studio.framework.common.geometry.Geometry
import com.vaticle.typedb.studio.framework.common.geometry.Geometry.Ellipse
import com.vaticle.typedb.studio.framework.common.geometry.Geometry.diamondArcIntersectAngles
import com.vaticle.typedb.studio.framework.common.geometry.Geometry.diamondIncomingLineIntersect
import com.vaticle.typedb.studio.framework.common.geometry.Geometry.ellipseIncomingLineIntersect
import com.vaticle.typedb.studio.framework.common.geometry.Geometry.rectArcIntersectAngles
import com.vaticle.typedb.studio.framework.common.geometry.Geometry.rectIncomingLineIntersect
import com.vaticle.typedb.studio.framework.material.ConceptDisplay.attributeValue
import java.awt.Polygon
import kotlin.math.pow

sealed class Vertex(val concept: Concept, protected val graph: Graph) {

    abstract val label: String
    abstract val geometry: Geometry
    var readyToCompose = false

    sealed class Thing(val thing: com.vaticle.typedb.client.api.concept.thing.Thing, graph: Graph) :
        Vertex(thing, graph) {

        override val label = thing.type.label.name()

        companion object {
            fun of(thing: com.vaticle.typedb.client.api.concept.thing.Thing, graph: Graph): Thing {
                return when (thing) {
                    is com.vaticle.typedb.client.api.concept.thing.Entity -> Entity(thing, graph)
                    is com.vaticle.typedb.client.api.concept.thing.Relation -> Relation(thing, graph)
                    is com.vaticle.typedb.client.api.concept.thing.Attribute<*> -> Attribute(thing, graph)
                    else -> throw IllegalStateException("[$thing]'s encoding is not supported by Vertex.Thing")
                }
            }
        }

        fun ownedAttributeEdges(): Collection<Edge.Has> {
            return graph.edges.filterIsInstance<Edge.Has>().filter { it.source == this }
        }

        fun isRoleplayer(): Boolean {
            return graph.edges.any { it is Edge.Roleplayer && it.target == this }
        }

        fun playingRoleEdges(): Collection<Edge.Roleplayer> {
            return graph.edges.filterIsInstance<Edge.Roleplayer>().filter { it.target == this }
        }

        class Entity(val entity: com.vaticle.typedb.client.api.concept.thing.Entity, graph: Graph) :
            Thing(entity, graph) {
            override val geometry = Geometry.Entity()
        }

        class Relation(relation: com.vaticle.typedb.client.api.concept.thing.Relation, graph: Graph) :
            Thing(relation, graph) {

            override val label = relation.type.label.name()
            override val geometry = Geometry.Relation()

            fun roleplayerEdges(): Collection<Edge.Roleplayer> {
                return graph.edges.filterIsInstance<Edge.Roleplayer>().filter { it.source == this }
            }
        }

        class Attribute(val attribute: com.vaticle.typedb.client.api.concept.thing.Attribute<*>, graph: Graph) :
            Thing(attribute, graph) {

            override val label = "${attribute.type.label.name()}: ${attributeValue(attribute)}"
            override val geometry = Geometry.Attribute()
        }
    }

    sealed class Type constructor(
        val type: com.vaticle.typedb.client.api.concept.type.Type,
        graph: Graph
    ) : Vertex(type, graph) {

        override val label = type.label.name()

        companion object {
            fun of(type: com.vaticle.typedb.client.api.concept.type.Type, graph: Graph): Type {
                return when (type) {
                    is EntityType -> Entity(type, graph)
                    is RelationType -> Relation(type, graph)
                    is AttributeType -> Attribute(type, graph)
                    is ThingType -> Thing(type, graph)
                    else -> throw IllegalStateException("[$type]'s encoding is not supported by Vertex.Type")
                }
            }
        }

        class Thing(thingType: ThingType, graph: Graph) : Type(thingType, graph) {
            override val geometry = Geometry.Entity()
        }

        class Entity(entityType: EntityType, graph: Graph) : Type(entityType, graph) {
            override val geometry = Geometry.Entity()
        }

        class Relation(relationType: RelationType, graph: Graph) : Type(relationType, graph) {
            override val geometry = Geometry.Relation()
        }

        class Attribute(attributeType: AttributeType, graph: Graph) : Type(attributeType, graph) {
            override val geometry = Geometry.Attribute()
        }
    }

    sealed class Geometry(private val baseSize: Size, private val expandedMaxSize: Size) : BasicVertex(0.0, 0.0) {

        private val baseScale = 1f
        private val expandedSize get() = if (contentOverflowsBaseShape) expandedMaxSize else baseSize
        private val expandedScale = 1.15f
        abstract val expandSizeMultiplier: Offset
        val size get() = _sizeAndScale.value.size
        val scale get() = _sizeAndScale.value.scale

        var position: Offset
            get() = Offset(x.toFloat(), y.toFloat())
            set(value) {
                x = value.x.toDouble()
                y = value.y.toDouble()
            }
        val rect get() = Rect(position - Offset(size.width, size.height) / 2f, size)
        val visualRect get() = Rect(position - Offset(size.width, size.height) * scale / 2f, size * scale)

        var isFrozen: Boolean
            get() = isXFixed
            set(value) {
                isXFixed = value
                isYFixed = value
            }

        var contentOverflowsBaseShape = false
        var isExpanded by mutableStateOf(false)
        val isVisiblyCollapsed get() = scale < lerp(baseScale, expandedScale, 0.1f)

        private val _sizeAndScale = Animatable(SizeAndScale(baseSize, baseScale), SizeAndScale.VectorConverter)

        private data class SizeAndScale(val size: Size, val scale: Float) {
            companion object {
                val VectorConverter: TwoWayConverter<SizeAndScale, AnimationVector3D> = TwoWayConverter(
                    convertToVector = { AnimationVector3D(it.size.width, it.size.height, it.scale) },
                    convertFromVector = { SizeAndScale(Size(it.v1, it.v2), it.v3) }
                )
            }
        }

        abstract val labelMaxWidth: Float

        fun labelMaxLines(fontSize: Int) = when (isVisiblyCollapsed) {
            true -> 2
            false -> ((size.height - PADDING) / fontSize).toInt()
        }

        /** Returns `true` if the given `Offset` intersects the given vertex, else, `false` */
        abstract fun visuallyIntersects(point: Offset): Boolean

        /** Find the endpoint of an edge drawn from `source` position to this vertex */
        abstract fun edgeEndpoint(source: Offset): Offset?

        /** Find the end angle of the given `Arc` when drawn as a curved edge to this vertex */
        abstract fun curvedEdgeEndAngle(arc: com.vaticle.typedb.studio.framework.common.geometry.Geometry.Arc): Float?

        suspend fun animateExpandOrCollapse() {
            _sizeAndScale.animateTo(
                if (isExpanded) SizeAndScale(expandedSize, expandedScale) else SizeAndScale(baseSize, baseScale),
                spring(stiffness = Spring.StiffnessHigh)
            )
        }

        companion object {
            protected const val PADDING = 4f

            val ENTITY_SIZE = Size(100f, 35f)
            val RELATION_SIZE = Size(110f, 55f)
            val ATTRIBUTE_SIZE = Size(100f, 35f)
            val CONCEPT_SIZE_EXPANDED = Size(150f, 55f)
            val ATTRIBUTE_SIZE_EXPANDED = Size(200f, 70f)

            private fun lerp(start: Float, stop: Float, fraction: Float): Float {
                return start + fraction * (stop - start)
            }
        }

        class Entity : Geometry(ENTITY_SIZE, CONCEPT_SIZE_EXPANDED) {

            private val incomingEdgeTargetRect
                get() = Rect(
                    Offset(rect.left - 4, rect.top - 4), Size(rect.width + 8, rect.height + 8)
                )

            override val labelMaxWidth get() = size.width - PADDING

            override val expandSizeMultiplier = Offset(1.6f, 1.6f)

            override fun visuallyIntersects(point: Offset) = visualRect.contains(point)

            override fun edgeEndpoint(source: Offset): Offset? {
                return rectIncomingLineIntersect(source, incomingEdgeTargetRect)
            }

            override fun curvedEdgeEndAngle(arc: com.vaticle.typedb.studio.framework.common.geometry.Geometry.Arc): Float? {
                // There should be only one intersection point when the arc has an endpoint within the vertex
                return rectArcIntersectAngles(arc, incomingEdgeTargetRect).firstOrNull()
            }
        }

        class Relation : Geometry(RELATION_SIZE, CONCEPT_SIZE_EXPANDED) {

            private val incomingEdgeTargetRect
                get() = Rect(Offset(rect.left - 4, rect.top - 4), Size(rect.width + 8, rect.height + 8))

            private val drawAsRect get() = !isVisiblyCollapsed && contentOverflowsBaseShape

            override val labelMaxWidth
                get() = when {
                    drawAsRect -> size.width - PADDING
                    else -> size.width * 0.66f - PADDING
                }

            override val expandSizeMultiplier = Offset(1.6f, 1.2f)

            override fun visuallyIntersects(point: Offset): Boolean {
                val r = visualRect
                return when (drawAsRect) {
                    true -> r.contains(point)
                    false -> Polygon(
                        intArrayOf(r.left.toInt(), r.center.x.toInt(), r.right.toInt(), r.center.x.toInt()),
                        intArrayOf(r.center.y.toInt(), r.top.toInt(), r.center.y.toInt(), r.bottom.toInt()),
                        4
                    ).contains(point.x.toDouble(), point.y.toDouble())
                }
            }

            override fun edgeEndpoint(source: Offset): Offset? {
                return diamondIncomingLineIntersect(source, incomingEdgeTargetRect)
            }

            override fun curvedEdgeEndAngle(arc: com.vaticle.typedb.studio.framework.common.geometry.Geometry.Arc): Float? {
                return diamondArcIntersectAngles(arc, incomingEdgeTargetRect).firstOrNull()
            }
        }

        class Attribute : Geometry(ATTRIBUTE_SIZE, ATTRIBUTE_SIZE_EXPANDED) {

            private val drawAsRect get() = !isVisiblyCollapsed && contentOverflowsBaseShape

            override val labelMaxWidth
                get() = when {
                    drawAsRect -> size.width - PADDING
                    else -> size.width * 0.8f - PADDING
                }

            override val expandSizeMultiplier = Offset(2f, 2f)

            override fun visuallyIntersects(point: Offset): Boolean {
                if (drawAsRect) return visualRect.contains(point)
                val xi = (point.x - position.x).pow(2) / (size.width / 2).pow(2)
                val yi = (point.y - position.y).pow(2) / (size.height / 2).pow(2)
                return xi + yi < 1f
            }

            override fun edgeEndpoint(source: Offset): Offset? {
                val ellipse = Ellipse(position.x, position.y, size.width / 2 + 2, size.height / 2 + 2)
                return ellipseIncomingLineIntersect(source, ellipse)
            }

            override fun curvedEdgeEndAngle(arc: com.vaticle.typedb.studio.framework.common.geometry.Geometry.Arc): Float? {
                // This implementation approximates the elliptical vertex as a diamond (like a relation); technically it
                // should intersect an arc with an ellipse. However, curved edges to/from attribute vertices are rare.
                val incomingEdgeTargetRect = Rect(
                    Offset(rect.left - 4, rect.top - 4), Size(rect.width + 8, rect.height + 8)
                )
                return diamondArcIntersectAngles(arc, incomingEdgeTargetRect).firstOrNull()
            }
        }
    }
}