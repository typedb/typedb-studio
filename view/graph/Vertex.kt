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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.vaticle.force.graph.impl.BasicVertex
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.material.Form
import com.vaticle.typedb.studio.view.material.Icon
import java.awt.Polygon
import java.time.format.DateTimeFormatter
import kotlin.math.pow

sealed class Vertex(val concept: Concept, protected val graph: Graph) {

    abstract val label: Label
    abstract val geometry: Geometry

    sealed class Thing(val thing: com.vaticle.typedb.client.api.concept.thing.Thing, graph: Graph) :
        Vertex(thing, graph) {

        override val label = Label(thing.type.label.name(), Label.LengthLimits.CONCEPT)

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
            override val geometry = Geometry.entity()
        }

        class Relation(relation: com.vaticle.typedb.client.api.concept.thing.Relation, graph: Graph) :
            Thing(relation, graph) {

            override val label = Label(relation.type.label.name(), Label.LengthLimits.RELATION)
            override val geometry = Geometry.relation()

            fun roleplayerEdges(): Collection<Edge.Roleplayer> {
                return graph.edges.filterIsInstance<Edge.Roleplayer>().filter { it.source == this }
            }
        }

        class Attribute(val attribute: com.vaticle.typedb.client.api.concept.thing.Attribute<*>, graph: Graph) :
            Thing(attribute, graph) {

            private val valueString = when {
                attribute.isDateTime -> {
                    attribute.asDateTime().value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                }
                else -> attribute.value.toString()
            }

            override val label = Label(
                "${attribute.type.label.name()}: $valueString", Label.LengthLimits.CONCEPT
            )
            override val geometry = Geometry.attribute()
        }
    }

    sealed class Type constructor(
        val type: com.vaticle.typedb.client.api.concept.type.Type,
        graph: Graph
    ) : Vertex(type, graph) {

        override val label = Label(type.label.name(), Label.LengthLimits.CONCEPT)

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

            // TODO: copied from typeIcon on 23/05/2022, needs refactor
            fun typeIcon(type: com.vaticle.typedb.client.api.concept.type.Type) = when (type) {
                is RelationType -> Form.IconArg(Icon.Code.RHOMBUS) { Theme.graph.vertex.relationType }
                is AttributeType -> Form.IconArg(Icon.Code.OVAL) { Theme.graph.vertex.attributeType }
                else -> Form.IconArg(Icon.Code.RECTANGLE) { Theme.graph.vertex.entityType }
            }
        }

        class Thing(thingType: ThingType, graph: Graph) : Type(thingType, graph) {
            override val geometry = Geometry.entity()
        }

        class Entity(entityType: EntityType, graph: Graph) : Type(entityType, graph) {
            override val geometry = Geometry.entity()
        }

        class Relation(val relationType: RelationType, graph: Graph) : Type(relationType, graph) {
            override val label = Label(relationType.label.name(), Label.LengthLimits.RELATION)
            override val geometry = Geometry.relation()
        }

        class Attribute(attributeType: AttributeType, graph: Graph) : Type(attributeType, graph) {
            override val geometry = Geometry.attribute()
        }
    }

    class Label(fullText: String, truncatedLength: Int) {

        val text = fullText.substring(0, truncatedLength.coerceAtMost(fullText.length))

        object LengthLimits {
            const val CONCEPT = 26
            const val RELATION = 22
        }
    }

    sealed class Geometry(val size: Size) : BasicVertex(0.0, 0.0) {

        var position: Offset
            get() {
                return Offset(x.toFloat(), y.toFloat())
            }
            set(value) {
                x = value.x.toDouble()
                y = value.y.toDouble()
            }

        val rect get() = Rect(offset = position - Offset(size.width, size.height) / 2f, size = size)

        var isFrozen: Boolean
            get() {
                return isXFixed
            }
            set(value) {
                isXFixed = value
                isYFixed = value
            }

        /** Returns `true` if the given `Offset` intersects the given vertex, else, `false` */
        abstract fun intersects(point: Offset): Boolean

        /** Find the endpoint of an edge drawn from `source` position to this vertex */
        abstract fun edgeEndpoint(source: Offset): Offset?

        /** Find the end angle of the given `Arc` when drawn as a curved edge to this vertex */
        abstract fun curvedEdgeEndAngle(arc: com.vaticle.typedb.studio.view.common.geometry.Geometry.Arc): Float?

        companion object {
            private const val ENTITY_WIDTH = 100f
            private const val ENTITY_HEIGHT = 35f
            private const val RELATION_WIDTH = 110f
            private const val RELATION_HEIGHT = 55f
            private const val ATTRIBUTE_WIDTH = 100f
            private const val ATTRIBUTE_HEIGHT = 35f

            fun entity() = Entity(Size(ENTITY_WIDTH, ENTITY_HEIGHT))
            fun relation() = Relation(Size(RELATION_WIDTH, RELATION_HEIGHT))
            fun attribute() = Attribute(Size(ATTRIBUTE_WIDTH, ATTRIBUTE_HEIGHT))
        }

        class Entity(size: Size) : Geometry(size) {

            private val incomingEdgeTargetRect
                get() = Rect(
                    Offset(rect.left - 4, rect.top - 4), Size(rect.width + 8, rect.height + 8)
                )

            override fun intersects(point: Offset) = rect.contains(point)

            override fun edgeEndpoint(source: Offset): Offset? {
                return com.vaticle.typedb.studio.view.common.geometry.Geometry.rectIncomingLineIntersect(
                    source,
                    incomingEdgeTargetRect
                )
            }

            override fun curvedEdgeEndAngle(arc: com.vaticle.typedb.studio.view.common.geometry.Geometry.Arc): Float? {
                // There should be only one intersection point when the arc has an endpoint within the vertex
                return com.vaticle.typedb.studio.view.common.geometry.Geometry.rectArcIntersectAngles(
                    arc,
                    incomingEdgeTargetRect
                ).firstOrNull()
            }
        }

        class Relation(size: Size) : Geometry(size) {

            private val incomingEdgeTargetRect
                get() = Rect(
                    Offset(rect.left - 4, rect.top - 4), Size(rect.width + 8, rect.height + 8)
                )

            override fun intersects(point: Offset): Boolean {
                val r = rect
                return Polygon(
                    intArrayOf(r.left.toInt(), r.center.x.toInt(), r.right.toInt(), r.center.x.toInt()),
                    intArrayOf(r.center.y.toInt(), r.top.toInt(), r.center.y.toInt(), r.bottom.toInt()),
                    4
                ).contains(point.x.toDouble(), point.y.toDouble())
            }

            override fun edgeEndpoint(source: Offset): Offset? {
                return com.vaticle.typedb.studio.view.common.geometry.Geometry.diamondIncomingLineIntersect(
                    source,
                    incomingEdgeTargetRect
                )
            }

            override fun curvedEdgeEndAngle(arc: com.vaticle.typedb.studio.view.common.geometry.Geometry.Arc): Float? {
                return com.vaticle.typedb.studio.view.common.geometry.Geometry.diamondArcIntersectAngles(
                    arc,
                    incomingEdgeTargetRect
                ).firstOrNull()
            }
        }

        class Attribute(size: Size) : Geometry(size) {

            override fun intersects(point: Offset): Boolean {
                val xi = (point.x - position.x).pow(2) / (size.width / 2).pow(2)
                val yi = (point.y - position.y).pow(2) / (size.height).pow(2)
                return xi + yi < 1f
            }

            override fun edgeEndpoint(source: Offset): Offset {
                val ellipse = com.vaticle.typedb.studio.view.common.geometry.Geometry.Ellipse(
                    position.x,
                    position.y,
                    size.width / 2 + 2,
                    size.height / 2 + 2
                )
                return com.vaticle.typedb.studio.view.common.geometry.Geometry.ellipseIncomingLineIntersect(
                    source,
                    ellipse
                )
            }

            override fun curvedEdgeEndAngle(arc: com.vaticle.typedb.studio.view.common.geometry.Geometry.Arc): Float? {
                // TODO: this implementation approximates the elliptical vertex as a diamond (like a relation);
                //       we should have a dedicated implementation for intersecting an arc with an ellipse
                val incomingEdgeTargetRect = Rect(
                    Offset(rect.left - 4, rect.top - 4), Size(rect.width + 8, rect.height + 8)
                )
                return com.vaticle.typedb.studio.view.common.geometry.Geometry.diamondArcIntersectAngles(
                    arc,
                    incomingEdgeTargetRect
                ).firstOrNull()
            }
        }
    }
}