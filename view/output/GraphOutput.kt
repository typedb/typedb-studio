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

package com.vaticle.typedb.studio.view.output

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.GraphTheme
import com.vaticle.typedb.studio.view.output.GraphOutput.State.Graph.Companion.emptyGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.sqrt

internal object GraphOutput : RunOutput() {

    internal class State(val transaction: TypeDBTransaction, number: Int) : RunOutput.State() {

        override val name: String = "${Label.GRAPH} ($number)"
        val graph: Graph = emptyGraph()
        var density: Float by mutableStateOf(1f)
        var frameID by mutableStateOf(0)
        var theme: Color.GraphTheme? = null
        private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

        internal fun output(conceptMap: ConceptMap) {
            conceptMap.map().entries.forEach { (varName: String, concept: Concept) ->
                when {
                    concept.isThing -> concept.asThing().let { thing ->
                        graph.thingVertices.computeIfAbsent(thing.iid) { Vertex.Thing.of(thing) }
                    }
                    concept.isThingType -> concept.asThingType().let { thingType ->
                        graph.typeVertices.computeIfAbsent(thingType.label.name()) { Vertex.Type.of(thingType) }
                    }
                    concept.isRoleType -> { /* do nothing */ }
                    else -> throw IllegalStateException("[$concept]'s encoding is not supported by GraphOutput")
                }
            }
        }

        fun vertexRenderer(vertex: Vertex, drawScope: DrawScope) = VertexRenderer(vertex, drawScope, density, theme!!)
        fun edgeRenderer(edge: Edge, drawScope: DrawScope) = EdgeRenderer(this, edge, drawScope)

        internal class Graph private constructor() {

            val thingVertices: MutableMap<String, Vertex.Thing> = ConcurrentHashMap()
            val typeVertices: MutableMap<String, Vertex.Type> = ConcurrentHashMap()
            val vertices: List<Vertex> get() = thingVertices.values + typeVertices.values
            val edges: MutableList<Edge> = mutableListOf()
            // val explanations: MutableList<Explanation> = mutableListOf()

            companion object {
                internal fun emptyGraph() = Graph()
            }
        }

        internal sealed class Vertex {

            abstract val label: Label
            open val geometry = Geometry.concept()

            internal sealed class Thing(val thing: com.vaticle.typedb.client.api.concept.thing.Thing) : Vertex() {

                override val label = Label(thing.type.label.name(), Label.LengthLimits.CONCEPT)

                companion object {
                    fun of(thing: com.vaticle.typedb.client.api.concept.thing.Thing): Thing {
                        return when {
                            thing.isEntity -> Entity(thing.asEntity())
                            thing.isRelation -> Relation(thing.asRelation())
                            thing.isAttribute -> Attribute(thing.asAttribute())
                            else -> throw IllegalStateException("[$thing]'s encoding is not supported by Vertex.Thing")
                        }
                    }
                }

                internal class Entity(val entity: com.vaticle.typedb.client.api.concept.thing.Entity) : Thing(entity)

                internal class Relation(val relation: com.vaticle.typedb.client.api.concept.thing.Relation)
                    : Thing(relation) {

                    override val label = Label(relation.type.label.name(), Label.LengthLimits.RELATION)
                    override val geometry = Geometry.relation()
                }

                internal class Attribute(val attribute: com.vaticle.typedb.client.api.concept.thing.Attribute<*>)
                    : Thing(attribute) {

                    private val valueString = when {
                        attribute.isDateTime -> {
                            attribute.asDateTime().value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        }
                        else -> attribute.value.toString()
                    }

                    override val label = Label(
                        "${attribute.type.label.name()}: $valueString", Label.LengthLimits.CONCEPT
                    )
                }
            }

            internal sealed class Type(val type: com.vaticle.typedb.client.api.concept.type.Type) : Vertex() {

                override val label = Label(type.label.name(), Label.LengthLimits.CONCEPT)

                companion object {
                    fun of(type: com.vaticle.typedb.client.api.concept.type.Type): Type {
                        return when {
                            type.isEntityType -> Entity(type.asEntityType())
                            type.isRelationType -> Relation(type.asRelationType())
                            type.isAttributeType -> Attribute(type.asAttributeType())
                            type.isThingType -> Thing(type.asThingType())
                            else -> throw IllegalStateException("[$type]'s encoding is not supported by Vertex.Type")
                        }
                    }
                }

                internal class Thing(val thingType: ThingType) : Type(thingType)

                internal class Entity(val entityType: EntityType) : Type(entityType)

                internal class Relation(val relationType: RelationType) : Type(relationType) {
                    override val label = Label(relationType.label.name(), Label.LengthLimits.RELATION)
                    override val geometry = Geometry.relation()
                }

                internal class Attribute(val attributeType: AttributeType) : Type(attributeType)
            }

            internal class Label(val fullText: String, truncatedLength: Int) {

                val text = fullText.substring(0, truncatedLength.coerceAtMost(fullText.length))

                object LengthLimits {
                    const val CONCEPT = 26
                    const val RELATION = 22
                }
            }

            data class Geometry(var position: Offset, val size: Size) {

                var isFrozen = false
                private var velocity = Offset.Zero
                val rect get() = Rect(offset = position - Offset(size.width, size.height) / 2f, size = size)

                companion object {
                    fun concept() = Geometry(Offset.Zero, Size(100f, 35f))
                    fun relation() = Geometry(Offset.Zero, Size(110f, 55f))
                }
            }
        }

        internal class EdgeCandidate {
            // TODO
        }

        internal class Edge(val source: Vertex, val target: Vertex) {
            // TODO
        }

        internal class VertexRenderer(
            private val vertex: Vertex, private val drawScope: DrawScope, private val density: Float,
            private val theme: Color.GraphTheme
        ) {

            companion object {
                private const val CORNER_RADIUS = 5f
            }

            private val baseColor = theme.vertex.let { colors ->
                when (vertex) {
                    is Vertex.Thing.Attribute -> colors.attribute
                    is Vertex.Thing.Entity -> colors.entity
                    is Vertex.Thing.Relation -> colors.relation
                    is Vertex.Type.Attribute -> colors.attributeType
                    is Vertex.Type.Entity -> colors.entityType
                    is Vertex.Type.Relation -> colors.relationType
                    is Vertex.Type.Thing -> colors.thingType
                }
            }
            private val color = baseColor
            private val rect = Rect(vertex.geometry.position * density, vertex.geometry.size * density)
            private val cornerRadius get() = CornerRadius(CORNER_RADIUS * density)

            fun render() {
                when (vertex) {
                    is Vertex.Type.Entity, is Vertex.Thing.Entity -> renderEntity()
                    is Vertex.Type.Relation, is Vertex.Thing.Relation -> renderRelation()
                    else -> renderEntity() // TODO
                }
            }

            private fun renderEntity() {
                drawScope.drawRoundRect(color, rect.topLeft, rect.size, cornerRadius)
            }

            private fun renderRelation() {
                // We start with a square of width n and transform it into a rhombus
                val n = (rect.height / sqrt(2.0)).toFloat()
                val baseShape = Rect(offset = rect.center - Offset(n / 2, n / 2), size = Size(n, n))
                drawScope.run {
                    withTransform({
                        scale(scaleX = rect.width / rect.height, scaleY = 1f, pivot = rect.center)
                        rotate(degrees = 45f, pivot = rect.center)
                    }) {
                        drawRoundRect(color, baseShape.topLeft, baseShape.size, cornerRadius)
                    }
                }
            }
        }

        internal class EdgeRenderer(private val state: State, private val edge: Edge, private val drawScope: DrawScope) {

            private val density get() = state.density

        }
    }

    @Composable
    internal fun Layout(state: State) {
        super.Layout(toolbarButtons(state)) { modifier ->
            Content(state, modifier)
        }
    }

    private fun toolbarButtons(state: State): List<Form.IconButtonArg> {
        return listOf()
    }

    @Composable
    private fun Content(state: State, modifier: Modifier) {
        val density = LocalDensity.current.density
        state.theme = GraphTheme.colors
        key(state.frameID) {
            Box(modifier
                .onGloballyPositioned { state.density = density }
                .graphicsLayer(/*translationX = state.frameID.toFloat()*/)) {
                Canvas(modifier.fillMaxSize()) { state.graph.vertices.forEach { drawVertex(state, it) } }
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                delay(33)
                state.frameID++
            }
        }
    }

    private fun DrawScope.drawVertex(state: State, vertex: State.Vertex) {
        state.vertexRenderer(vertex, this).render()
    }
}
