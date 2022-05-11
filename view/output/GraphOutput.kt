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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.vaticle.force.graph.force.CenterForce
import com.vaticle.force.graph.force.CollideForce
import com.vaticle.force.graph.force.LinkForce
import com.vaticle.force.graph.force.ManyBodyForce
import com.vaticle.force.graph.force.XForce
import com.vaticle.force.graph.force.YForce
import com.vaticle.force.graph.impl.BasicSimulation
import com.vaticle.force.graph.impl.BasicVertex
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.GraphTheme
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.output.GraphOutput.State.Graph.Companion.emptyGraph
import com.vaticle.typeql.lang.TypeQL.`var`
import com.vaticle.typeql.lang.TypeQL.match
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.awt.Polygon
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

internal object GraphOutput : RunOutput() {

    val LOGGER = KotlinLogging.logger {}

    internal class State(val transaction: TypeDBTransaction, number: Int) : RunOutput.State() {

        override val name: String = "${Label.GRAPH} ($number)"
        val viewport = Viewport(this)
        val graph: Graph = emptyGraph()
        val simulationRunner = SimulationRunner(this)
        var pointerPosition: Offset? by mutableStateOf(null)
        var hoveredVertex: Vertex? by mutableStateOf(null)
        val hoveredVertexChecker = HoveredVertexChecker(this)
        var theme: Color.GraphTheme? = null
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)

        private val answerLoaderContext = AnswerLoaderContext(graph, transaction, coroutineScope)

        suspend fun output(conceptMap: ConceptMap) {
            ConceptMapLoader(conceptMap, answerLoaderContext).load()
        }

        fun onQueryCompleted() {
            graph.completeAllEdges()
        }

        private fun rendererContext(drawScope: DrawScope) = RendererContext(drawScope, theme!!)

        fun vertexBackgroundRenderer(vertex: Vertex, drawScope: DrawScope): VertexBackgroundRenderer {
            return VertexBackgroundRenderer.of(vertex, this, rendererContext(drawScope))
        }

        fun edgeRenderer(drawScope: DrawScope): EdgeRenderer {
            return EdgeRenderer(this, rendererContext(drawScope))
        }

        class Viewport(private val state: State) {
            var density: Float by mutableStateOf(1f)
            var size by mutableStateOf(Size.Zero)
            /** The world coordinates at the top-left corner of the viewport. */
            var position by mutableStateOf(Offset.Zero)
            /** The world coordinates at the center of the viewport. */
            val center get() = position + Offset(size.width / 2, size.height / 2) / scale
            private var _scale by mutableStateOf(1f)
            var scale: Float
                get() = _scale
                set(value) { _scale = value.coerceIn(0.001f..10f) }
            var isLayoutInitialised = AtomicBoolean(false)

            fun findVertexAtPhysicalPoint(physicalPoint: Offset): Vertex? {
                val worldPoint = physicalPointToWorldPoint(physicalPoint)
                val nearestVertices = nearestVertices(worldPoint)
                return nearestVertices.find { it.geometry.intersects(worldPoint) }
            }

            private fun physicalPointToWorldPoint(physicalPoint: Offset): Offset {
                val transformOrigin = (Offset(size.width, size.height) / 2f) * density
                val scaledPhysicalPoint = physicalPoint / density

                // Let 'physical' be the physical position of a point in the viewport, 'origin' be the transform origin
                // of the viewport, 'world' be the position of 'physical' in the world, 'viewportPosition' be the world
                // offset at the top left corner of the viewport. Then:
                // physical = origin + scale * (world - viewportPosition - origin)
                // Rearranging this equation gives the result below:
                return (((scaledPhysicalPoint - transformOrigin) / scale) + transformOrigin) + position
            }

            private fun nearestVertices(worldPoint: Offset): Sequence<Vertex> {
                // TODO: once we have out-of-viewport detection, use it to make this function more performant on large graphs
                val vertexDistances: MutableMap<Vertex, Float> = mutableMapOf()
                state.graph.vertices.associateWithTo(vertexDistances) {
                    (worldPoint - it.geometry.position).getDistanceSquared()
                }
                return sequence {
                    while (vertexDistances.isNotEmpty()) {
                        yield(vertexDistances.entries.minByOrNull { it.value }!!.key)
                    }
                }.take(10)
            }
        }

        class Graph private constructor() {

            private val _thingVertices: MutableMap<String, Vertex.Thing> = ConcurrentHashMap()
            private val _typeVertices: MutableMap<String, Vertex.Type> = ConcurrentHashMap()
            val thingVertices: Map<String, Vertex.Thing> get() = _thingVertices
            val typeVertices: Map<String, Vertex.Type> get() = _typeVertices
            val vertices: Collection<Vertex> get() = thingVertices.values + typeVertices.values
            private val _edges: MutableList<Edge> = mutableListOf()
            val edges: Collection<Edge> get() = _edges
            private val edgeCandidates: MutableMap<String, MutableList<EdgeCandidate>> = mutableMapOf()

            // val explanations: MutableList<Explanation> = mutableListOf()
            val physics = Physics(this)

            companion object {
                internal fun emptyGraph() = Graph()
            }

            fun putThingVertexIfAbsent(iid: String, vertexFn: () -> Vertex.Thing): Boolean {
                var added = false
                val vertex = _thingVertices.computeIfAbsent(iid) { added = true; vertexFn() }
                if (added) {
                    physics.placeVertex(vertex.geometry)
                    completeEdges(missingVertex = vertex)
                }
                return added
            }

            fun putTypeVertexIfAbsent(label: String, vertexFn: () -> Vertex.Type): Boolean {
                var added = false
                val vertex = _typeVertices.computeIfAbsent(label) { added = true; vertexFn() }
                if (added) {
                    physics.placeVertex(vertex.geometry)
                    completeEdges(missingVertex = vertex)
                }
                return added
            }

            fun addEdge(edge: Edge) {
                _edges += edge
            }

            fun addEdgeCandidate(edge: EdgeCandidate) {
                val key = when (edge) {
                    is EdgeCandidate.Has -> edge.targetIID
                    is EdgeCandidate.Isa -> edge.targetLabel
                    is EdgeCandidate.Owns -> edge.targetLabel
                    is EdgeCandidate.Plays -> edge.sourceLabel
                    is EdgeCandidate.Sub -> edge.targetLabel
                }
                edgeCandidates.getOrPut(key) { mutableListOf() }.add(edge)
            }

            private fun completeEdges(missingVertex: Vertex) {
                val key = when (missingVertex) {
                    is Vertex.Type -> missingVertex.type.label.name()
                    is Vertex.Thing -> missingVertex.thing.iid
                }
                edgeCandidates.remove(key)?.forEach {
                    val completedEdge = when (it) {
                        is EdgeCandidate.Isa -> it.toEdge(missingVertex as Vertex.Type)
                        is EdgeCandidate.Has -> it.toEdge(missingVertex as Vertex.Thing.Attribute)
                        is EdgeCandidate.Owns -> it.toEdge(missingVertex as Vertex.Type.Attribute)
                        is EdgeCandidate.Plays -> it.toEdge(missingVertex as Vertex.Type.Relation)
                        is EdgeCandidate.Sub -> it.toEdge(missingVertex as Vertex.Type)
                    }
                    addEdge(completedEdge)
                }
            }

            fun completeAllEdges() {
                vertices.forEach { completeEdges(it) }
            }

            fun isEmpty() = vertices.isEmpty()
            fun isNotEmpty() = vertices.isNotEmpty()

            class Physics(private val graph: Graph) {
                private val simulation = BasicSimulation()
                var iteration by mutableStateOf(0L)
                val isStable get() = simulation.alpha < simulation.alphaMin

                fun step() {
                    if (isStable) return
                    setupForces()
                    simulation.tick()
                    iteration++
                }

                fun placeVertex(vertex: com.vaticle.force.graph.api.Vertex) {
                    simulation.placeVertex(vertex)
                }

                fun terminate() {
                    simulation.alpha = 0.0
                }

                private fun setupForces() {
                    simulation.apply {
                        forces.clear()
                        localForces.clear()

                        // TODO: track changes to vertices + edges, rebuild forces only if there are changes
                        val vertices = graph.vertices.map { it.geometry }
                        val edges = graph.edges.map { it.physics }
                        forces.add(CenterForce(vertices, 0.0, 0.0))
                        forces.add(CollideForce(vertices, 65.0))
                        forces.add(
                            ManyBodyForce(
                                vertices, (-500.0 - vertices.size / 3) * (1 + edges.size / (vertices.size + 1))
                            )
                        )
                        forces.add(LinkForce(vertices, edges, 90.0, 0.5))
                        val gravityStrength = 0.02 * log10(vertices.size + 100.0)
                        forces.add(XForce(vertices, 0.0, gravityStrength))
                        forces.add(YForce(vertices, 0.0, gravityStrength))
                    }
                }
            }
        }

        sealed class Vertex {

            abstract val label: Label
            abstract val geometry: Geometry

            sealed class Thing(val thing: com.vaticle.typedb.client.api.concept.thing.Thing) : Vertex() {

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

                class Entity(val entity: com.vaticle.typedb.client.api.concept.thing.Entity) : Thing(entity) {
                    override val geometry = Geometry.entity()
                }

                class Relation(val relation: com.vaticle.typedb.client.api.concept.thing.Relation) : Thing(relation) {

                    override val label = Label(relation.type.label.name(), Label.LengthLimits.RELATION)
                    override val geometry = Geometry.relation()
                    var explanationMgr: ExplanationManager? = null
                    val isExplainable get() = explanationMgr != null
                }

                class Attribute(val attribute: com.vaticle.typedb.client.api.concept.thing.Attribute<*>)
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
                    override val geometry = Geometry.attribute()
                    var explanationMgr: ExplanationManager? = null
                    val isExplainable get() = explanationMgr != null
                }

                class ExplanationManager
            }

            sealed class Type(val type: com.vaticle.typedb.client.api.concept.type.Type) : Vertex() {

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

                class Thing(val thingType: ThingType) : Type(thingType) {
                    override val geometry = Geometry.entity()
                }

                class Entity(val entityType: EntityType) : Type(entityType) {
                    override val geometry = Geometry.entity()
                }

                class Relation(val relationType: RelationType) : Type(relationType) {
                    override val label = Label(relationType.label.name(), Label.LengthLimits.RELATION)
                    override val geometry = Geometry.relation()
                }

                class Attribute(val attributeType: AttributeType) : Type(attributeType) {
                    override val geometry = Geometry.attribute()
                }
            }

            class Label(val fullText: String, truncatedLength: Int) {

                val text = fullText.substring(0, truncatedLength.coerceAtMost(fullText.length))

                object LengthLimits {
                    const val CONCEPT = 26
                    const val RELATION = 22
                }
            }

            sealed class Geometry(val size: Size) : BasicVertex(0.0, 0.0) {

                var position: Offset
                    get() { return Offset(x.toFloat(), y.toFloat()) }
                    set(value) {
                        x = value.x.toDouble()
                        y = value.y.toDouble()
                    }

                val rect get() = Rect(offset = position - Offset(size.width, size.height) / 2f, size = size)

                var isFrozen: Boolean
                    get() { return isXFixed }
                    set(value) {
                        isXFixed = value
                        isYFixed = value
                    }

                /** Returns `true` if the given `Offset` intersects the given vertex, else, `false` */
                abstract fun intersects(point: Offset): Boolean

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
                    override fun intersects(point: Offset) = rect.contains(point)
                }

                class Relation(size: Size) : Geometry(size) {

                    override fun intersects(point: Offset): Boolean {
                        val r = rect
                        return Polygon(
                            intArrayOf(r.left.toInt(), r.center.x.toInt(), r.right.toInt(), r.center.x.toInt()),
                            intArrayOf(r.center.y.toInt(), r.top.toInt(), r.center.y.toInt(), r.bottom.toInt()),
                            4
                        ).contains(point.x.toDouble(), point.y.toDouble())
                    }
                }

                class Attribute(size: Size) : Geometry(size) {

                    override fun intersects(point: Offset): Boolean {
                        val xi = (point.x - position.x).pow(2) / (size.width / 2).pow(2)
                        val yi = (point.y - position.y).pow(2) / (size.height).pow(2)
                        return xi + yi < 1f
                    }
                }
            }
        }

        sealed class Edge(val source: Vertex, val target: Vertex) {

            object Labels {
                const val HAS = "has"
                const val ISA = "isa"
                const val OWNS = "owns"
                const val SUB = "sub"
            }

            val geometry = Geometry()
            val physics = Physics(this)
            abstract val label: String

            interface Inferrable {
                val isInferred: Boolean
            }

            // Type edges
            class Sub(source: Vertex.Type, target: Vertex.Type) : Edge(source, target) {
                override val label = Labels.SUB
            }

            class Owns(source: Vertex.Type, target: Vertex.Type.Attribute) : Edge(source, target) {
                override val label = Labels.OWNS
            }

            class Plays(source: Vertex.Type.Relation, target: Vertex.Type, val role: String) : Edge(source, target) {
                override val label = role
            }

            // Thing edges
            class Has(source: Vertex.Thing, target: Vertex.Thing.Attribute, override val isInferred: Boolean = false)
                : Edge(source, target), Inferrable {

                override val label = Labels.HAS
            }

            class Roleplayer(
                source: Vertex.Thing.Relation, target: Vertex.Thing, val role: String,
                override val isInferred: Boolean = false
            ) : Edge(source, target), Inferrable {

                override val label = role
            }

            // Thing-to-type edges
            class Isa(source: Vertex.Thing, target: Vertex.Type) : Edge(source, target) {
                override val label = Labels.ISA
            }

            class Geometry {
                var curveMidpoint: Offset? = null
                val isCurved get() = curveMidpoint != null
            }

            class Physics(private val edge: Edge) : com.vaticle.force.graph.api.Edge {
                override fun source() = edge.source.geometry
                override fun target() = edge.target.geometry
            }
        }

        sealed class EdgeCandidate {

            interface Inferrable {
                val isInferred: Boolean
            }

            // Type edges
            class Sub(val source: Vertex.Type, val targetLabel: String) : EdgeCandidate() {
                fun toEdge(target: Vertex.Type) = Edge.Sub(source, target).also { /*println("promoted SUB edge candidate: [${source.label.fullText} -> ${target.label.fullText}]")*/}
            }

            class Owns(val source: Vertex.Type, val targetLabel: String) : EdgeCandidate() {
                fun toEdge(target: Vertex.Type.Attribute) = Edge.Owns(source, target)
            }

            class Plays(val sourceLabel: String, val target: Vertex.Type, val role: String) : EdgeCandidate() {

                fun toEdge(source: Vertex.Type.Relation) = Edge.Plays(source, target, role)
            }

            // Thing edges
            class Has(
                val source: Vertex.Thing, val targetIID: String, override val isInferred: Boolean = false
            ) : EdgeCandidate(), Inferrable {

                fun toEdge(target: Vertex.Thing.Attribute) = Edge.Has(source, target, isInferred)
            }

            // Thing-to-type edges
            class Isa(val source: Vertex.Thing, val targetLabel: String) : EdgeCandidate() {
                fun toEdge(target: Vertex.Type) = Edge.Isa(source, target)
            }
        }

        class Explanation(val vertices: Set<Vertex>)

        class ConceptMapLoader(private val conceptMap: ConceptMap, private val ctx: AnswerLoaderContext) {
            suspend fun load() {
                conceptMap.map().entries.map { (varName: String, concept: Concept) ->
                    loadEntry(varName, concept)
                }
            }

            private suspend fun loadEntry(varName: String, concept: Concept) {
                when {
                    concept.isThing || concept.isThingType -> {
                        val (added, vertex) = putVertexIfAbsent(concept)
                        if (added) {
                            if (concept.isThing && concept.asThing().isInferred) initExplainables(concept, varName)
                            ConnectedConceptLoader.of(concept, vertex, ctx).load()
                        }
                    }
                    concept.isRoleType -> { /* do nothing */ }
                    else -> throw ctx.unsupportedEncodingException(concept)
                }
            }

            private fun putVertexIfAbsent(concept: Concept): PutVertexResult {
                when {
                    concept.isThing -> concept.asThing().let { thing ->
                        val added = ctx.graph.putThingVertexIfAbsent(thing.iid) { Vertex.Thing.of(thing) }
                        return PutVertexResult(added, ctx.graph.thingVertices[thing.iid]!!)
                    }
                    concept.isThingType -> concept.asThingType().let { type ->
                        val added = ctx.graph.putTypeVertexIfAbsent(type.label.name()) { Vertex.Type.of(type) }
                        return PutVertexResult(added, ctx.graph.typeVertices[type.label.name()]!!)
                    }
                    else -> throw ctx.unsupportedEncodingException(concept)
                }
            }

            private fun initExplainables(concept: Concept, varName: String) {
                // TODO
            }

            data class PutVertexResult(val added: Boolean, val vertex: Vertex)
        }

        sealed class ConnectedConceptLoader(val ctx: AnswerLoaderContext) {

            abstract suspend fun load()

            protected suspend fun runAsync(fn: () -> Unit) {
                ctx.coroutineScope.launch(Dispatchers.IO) {
                    try {
                        fn()
                    } catch (e: Exception) {
                        GlobalState.notification.systemError(LOGGER, e, Message.Visualiser.UNEXPECTED_ERROR)
                    }
                }.join()
            }

            companion object {
                fun of(concept: Concept, vertex: Vertex, ctx: AnswerLoaderContext): ConnectedConceptLoader {
                    return when {
                        concept.isThing -> Thing(concept.asThing(), vertex as Vertex.Thing, ctx)
                        concept.isThingType -> ThingType(concept.asThingType(), vertex as Vertex.Type, ctx)
                        else -> throw ctx.unsupportedEncodingException(concept)
                    }
                }
            }

            class Thing(
                val thing: com.vaticle.typedb.client.api.concept.thing.Thing,
                val thingVertex: Vertex.Thing, ctx: AnswerLoaderContext
            ) : ConnectedConceptLoader(ctx) {
                private val remoteThing = thing.asRemote(ctx.transaction)

                override suspend fun load() {
                    loadIsaEdge()
                    loadHasEdges()
                    if (thing.isRelation) loadRoleplayerEdgesAndVertices()
                }

                private fun loadIsaEdge() {
                    thing.type.let { type ->
                        val typeVertex = ctx.graph.typeVertices[type.label.name()]
                        if (typeVertex != null) ctx.graph.addEdge(Edge.Isa(thingVertex, typeVertex))
                        else ctx.graph.addEdgeCandidate(EdgeCandidate.Isa(thingVertex, type.label.name()))
                    }
                }

                private fun loadHasEdges() {
                    // construct TypeQL query so that reasoning can run
                    // test for ability to own attributes, to ensure query will not throw during type inference
                    if (!canOwnAttributes()) return
                    val (x, attr) = Pair("x", "attr")
                    ctx.transaction.query().match(match(`var`(x).iid(thing.iid).has(`var`(attr)))).forEach { answer ->
                        val attribute = answer.get(attr).asAttribute()
                        // TODO: test logic (was 'attr in explainables().attributes().keys', not 'attribute.isInferred')
                        val isEdgeInferred = attribute.isInferred || ownershipIsExplainable(attr, answer)
                        val attributeVertex = ctx.graph.thingVertices[attribute.iid] as? Vertex.Thing.Attribute
                        if (attributeVertex != null) {
                            ctx.graph.addEdge(Edge.Has(thingVertex, attributeVertex, isEdgeInferred))
                        } else {
                            ctx.graph.addEdgeCandidate(EdgeCandidate.Has(thingVertex, attribute.iid, isEdgeInferred))
                        }
                    }
                }

                private fun canOwnAttributes(): Boolean {
                    val typeLabel = thing.type.label.name()
                    return ctx.schema.typeAttributeOwnershipMap.getOrPut(typeLabel) {
                        // non-atomic update as Concept API call is idempotent and cheaper than locking the map
                        thing.type.asRemote(ctx.transaction).owns.findAny().isPresent
                    }
                }

                private fun ownershipIsExplainable(attributeVarName: String, conceptMap: ConceptMap): Boolean {
                    return attributeVarName in conceptMap.explainables().ownerships().keys.map { it.second() }
                }

                private fun loadRoleplayerEdgesAndVertices() {
                    remoteThing.asRelation().playersByRoleType.entries.forEach { (roleType, roleplayers) ->
                        roleplayers.forEach { roleplayer ->
                            ctx.graph.putThingVertexIfAbsent(roleplayer.iid) { Vertex.Thing.of(roleplayer) }
                            val roleplayerVertex = ctx.graph.thingVertices[roleplayer.iid]!!
                            ctx.graph.addEdge(Edge.Roleplayer(
                                thingVertex as Vertex.Thing.Relation, roleplayerVertex,
                                roleType.label.name(), thing.isInferred
                            ))
                        }
                    }
                }
            }

            class ThingType(
                thingType: com.vaticle.typedb.client.api.concept.type.ThingType,
                private val typeVertex: Vertex.Type, ctx: AnswerLoaderContext
            ) : ConnectedConceptLoader(ctx) {
                private val remoteThingType = thingType.asRemote(ctx.transaction)

                override suspend fun load() = runAsync {
                    loadSubEdge()
                    loadOwnsEdges()
                    loadPlaysEdges()
                }

                private fun loadSubEdge() {
                    remoteThingType.supertype?.let { supertype ->
                        val supertypeVertex = ctx.graph.typeVertices[supertype.label.name()]
                        if (supertypeVertex != null) ctx.graph.addEdge(Edge.Sub(typeVertex, supertypeVertex)).also { /*println("added SUB edge [${typeVertex.label.fullText} -> ${supertypeVertex.label.fullText}]")*/ }
                        else ctx.graph.addEdgeCandidate(EdgeCandidate.Sub(typeVertex, supertype.label.name())).also { /*println("added SUB edge candidate: [${typeVertex.label.fullText} -> LABEL[${supertype.label.name()}]]")*/ }
                    }
                }

                private fun loadOwnsEdges() {
                    remoteThingType.owns.forEach { attributeType ->
                        val attributeTypeLabel = attributeType.label.name()
                        val attributeTypeVertex = ctx.graph.typeVertices[attributeTypeLabel] as? Vertex.Type.Attribute
                        if (attributeTypeVertex != null) ctx.graph.addEdge(Edge.Owns(typeVertex, attributeTypeVertex))
                        else ctx.graph.addEdgeCandidate(EdgeCandidate.Owns(typeVertex, attributeTypeLabel))
                    }
                }

                private fun loadPlaysEdges() {
                    remoteThingType.plays.forEach { roleType ->
                        val relationTypeLabel = roleType.label.scope().get()
                        val roleLabel = roleType.label.name()
                        val relationTypeVertex = ctx.graph.typeVertices[relationTypeLabel] as? Vertex.Type.Relation
                        if (relationTypeVertex != null) {
                            ctx.graph.addEdge(Edge.Plays(relationTypeVertex, typeVertex, roleLabel))
                        } else {
                            ctx.graph.addEdgeCandidate(EdgeCandidate.Plays(relationTypeLabel, typeVertex, roleLabel))
                        }
                    }
                }
            }
        }

        class AnswerLoaderContext(
            val graph: Graph, val transaction: TypeDBTransaction, val coroutineScope: CoroutineScope,
            val schema: Schema = Schema()
        ) {
            fun unsupportedEncodingException(concept: Concept): IllegalStateException {
                return IllegalStateException("[$concept]'s encoding is not supported by ConceptMapLoader")
            }

            class Schema(val typeAttributeOwnershipMap: MutableMap<String, Boolean> = mutableMapOf())
        }

        sealed class VertexBackgroundRenderer(
            private val vertex: Vertex, protected val state: State, protected val ctx: RendererContext
        ) {
            companion object {
                private const val CORNER_RADIUS = 5f

                fun of(vertex: Vertex, state: State, ctx: RendererContext): VertexBackgroundRenderer = when (vertex) {
                    is Vertex.Type.Entity, is Vertex.Type.Thing, is Vertex.Thing.Entity -> Entity(vertex, state, ctx)
                    is Vertex.Type.Relation, is Vertex.Thing.Relation -> Relation(vertex, state, ctx)
                    is Vertex.Type.Attribute, is Vertex.Thing.Attribute -> Attribute(vertex, state, ctx)
                }
            }

            private val baseColor = ctx.theme.vertex.let { colors ->
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
            private val isFocused = vertex == state.hoveredVertex
            private val alpha = when {
                isFocused -> .675f
                else -> null
            }
            protected val color = if (alpha != null) baseColor.copy(alpha) else baseColor
            private val density = state.viewport.density
            protected val rect = vertex.geometry.let {
                Rect(
                    (it.position - state.viewport.position) * density
                            - Offset(it.size.width * density / 2, it.size.height * density / 2),
                    it.size * density
                )
            }
            protected val cornerRadius get() = CornerRadius(CORNER_RADIUS * density)

            abstract fun draw()

            class Entity(vertex: Vertex, state: State, ctx: RendererContext)
                : VertexBackgroundRenderer(vertex, state, ctx) {

                override fun draw() {
                    ctx.drawScope.drawRoundRect(color, rect.topLeft, rect.size, cornerRadius)
                }
            }

            class Relation(vertex: Vertex, state: State, ctx: RendererContext)
                : VertexBackgroundRenderer(vertex, state, ctx) {

                override fun draw() {
                    // We start with a square of width n and transform it into a rhombus
                    val n = (rect.height / sqrt(2.0)).toFloat()
                    val baseShape = Rect(offset = rect.center - Offset(n / 2, n / 2), size = Size(n, n))
                    with(ctx.drawScope) {
                        withTransform({
                            scale(scaleX = rect.width / rect.height, scaleY = 1f, pivot = rect.center)
                            rotate(degrees = 45f, pivot = rect.center)
                        }) {
                            drawRoundRect(color, baseShape.topLeft, baseShape.size, cornerRadius)
                        }
                    }
                }
            }

            class Attribute(vertex: Vertex, state: State, ctx: RendererContext)
                : VertexBackgroundRenderer(vertex, state, ctx) {

                override fun draw() {
                    ctx.drawScope.drawOval(color, rect.topLeft, rect.size)
                }
            }
        }

        class EdgeRenderer(private val state: State, private val ctx: RendererContext) {

            private val density = state.viewport.density

            fun draw() {
                ctx.drawScope.drawPoints(
                    points = edgeCoordinates(state.graph.edges), pointMode = PointMode.Lines,
                    color = ctx.theme.edge, strokeWidth = 1f
                )
            }

            private fun edgeCoordinates(edges: Iterable<Edge>): List<Offset> {
                return sequence {
                    edges.forEach {
                        yield((it.source.geometry.position - state.viewport.position) * density)
                        yield((it.target.geometry.position - state.viewport.position) * density)
                    }
                }.toList()
            }
        }

        data class RendererContext(val drawScope: DrawScope, val theme: Color.GraphTheme)

        class SimulationRunner(private val state: State) {

            private var isTickRunning = false
            private var lastFrameTime = System.currentTimeMillis()

            suspend fun run() {
                while (true) {
                    withFrameMillis {
                        if (isReadyToTick()) {
                            lastFrameTime = System.currentTimeMillis()
                            isTickRunning = true
                            tickAsync().invokeOnCompletion { isTickRunning = false }
                        }
                    }
                }
            }

            private fun isReadyToTick(): Boolean {
                return System.currentTimeMillis() - lastFrameTime > 16
                        && !isTickRunning
                        && state.graph.isNotEmpty()
                        && !state.graph.physics.isStable
            }

            private fun tickAsync(): Job {
                return state.coroutineScope.launch {
                    try {
                        state.graph.physics.step()
                    } catch (e: Exception) {
                        GlobalState.notification.systemError(LOGGER, e, Message.Visualiser.UNEXPECTED_ERROR)
                        state.graph.physics.terminate()
                    }
                }
            }
        }

        class HoveredVertexChecker(private val state: State) {

            private var lastScanDoneTime = System.currentTimeMillis()

            suspend fun poll() {
                while (true) {
                    withFrameMillis {
                        state.pointerPosition?.let {
                            if (isReadyToScan()) scan(it)
                        }
                    }
                }
            }

            private fun isReadyToScan() = System.currentTimeMillis() - lastScanDoneTime > 33

            private fun scan(pointerPosition: Offset) {
                state.hoveredVertex = state.viewport.findVertexAtPhysicalPoint(pointerPosition)
                lastScanDoneTime = System.currentTimeMillis()
            }
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

        Box(
            modifier.graphicsLayer(clip = true).background(GraphTheme.colors.background)
            .onGloballyPositioned { updateViewportState(state, density, it) }
        ) {
            // TODO: refactor into Graph.Visualiser
            key(state.graph.physics.iteration) {
                Box(Modifier.fillMaxSize().graphicsLayer(scaleX = state.viewport.scale, scaleY = state.viewport.scale)) {
                    EdgeLayer(state)
                    VertexLayer(state)
                }
            }
            PointerInput.Handler(state, Modifier.fillMaxSize().zIndex(100f))
        }

        LaunchedEffect(Unit) { state.simulationRunner.run() }
        LaunchedEffect(Unit) { state.hoveredVertexChecker.poll() }
    }

    private fun updateViewportState(state: State, density: Float, layout: LayoutCoordinates) {
        state.viewport.density = density
        if (state.viewport.isLayoutInitialised.compareAndSet(false, true)) {
            state.viewport.size = layout.size.toSize()
            state.viewport.position = -state.viewport.size.center / density
        }
    }

    @Composable
    private fun EdgeLayer(state: State) {
        Canvas(Modifier.fillMaxSize()) { state.edgeRenderer(this).draw() }
    }

    @Composable
    private fun VertexLayer(state: State) {
        val vertices = state.graph.vertices
        Canvas(Modifier.fillMaxSize()) { vertices.forEach { drawVertexBackground(it, state) } }
        if (vertices.size <= 1000) vertices.forEach { VertexLabel(it, state) }
    }

    private fun DrawScope.drawVertexBackground(vertex: State.Vertex, state: State) {
        state.vertexBackgroundRenderer(vertex, this).draw()
    }

    @Composable
    private fun VertexLabel(vertex: State.Vertex, state: State) {
        val r = vertex.geometry.rect
        val x = (r.left - state.viewport.position.x).dp
        val y = (r.top - state.viewport.position.y).dp
        val color = GraphTheme.colors.vertexLabel

        Box(Modifier.offset(x, y).size(r.width.dp, r.height.dp), Alignment.Center) {
            Form.Text(vertex.label.text, textStyle = Theme.typography.code1, color = color, align = TextAlign.Center)
        }
    }

    private object PointerInput {
        @Composable
        fun Handler(state: State, modifier: Modifier) {
            DragAndScroll(state, modifier) {
                // Nested elements are required for drag and tap events to not conflict with each other
                TapAndHover(state, modifier)
            }
        }

        @Composable
        fun DragAndScroll(state: State, modifier: Modifier, content: @Composable () -> Unit) {
            val viewport = state.viewport
            Box(modifier
                .pointerInput(viewport.density, viewport.scale) {
                    detectDragGestures { _, dragAmount ->
                        viewport.position -= dragAmount / (viewport.scale * viewport.density)
                    }
                }
                .scrollable(orientation = Orientation.Vertical, state = rememberScrollableState { delta ->
                    viewport.scale *= 1 + (delta * 0.0006f / viewport.density)
                    delta
                })
            ) {
                content()
            }
        }

        @OptIn(ExperimentalComposeUiApi::class)
        @Composable
        fun TapAndHover(state: State, modifier: Modifier) {
            Box(modifier
                .pointerMoveFilter(
                    onMove = { state.pointerPosition = it; false },
                    onExit = { state.pointerPosition = null; false }
                )
            )
        }
    }
}
