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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.output.GraphOutput.State.Graph.Companion.emptyGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.EmptyCoroutineContext

internal object GraphOutput : RunOutput() {

    internal class State(val transaction: TypeDBTransaction, number: Int) : RunOutput.State() {

        override val name: String = "${Label.GRAPH} ($number)"
        val graph: Graph = emptyGraph()
        var frameID by mutableStateOf(0)
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
        Box(modifier, Alignment.Center) {
            key(state.frameID) {
                Form.Text(value = "${state.name}: vertices: ${state.graph.vertices.size}, edges: ${state.graph.edges.size}")
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                delay(33)
                state.frameID++
            }
        }
    }
}
