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
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
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
import com.vaticle.typedb.studio.view.common.geometry.Geometry.Ellipse
import com.vaticle.typedb.studio.view.common.geometry.Geometry.arrowhead
import com.vaticle.typedb.studio.view.common.geometry.Geometry.diamondIncomingLineIntersect
import com.vaticle.typedb.studio.view.common.geometry.Geometry.ellipseIncomingLineIntersect
import com.vaticle.typedb.studio.view.common.geometry.Geometry.midpoint
import com.vaticle.typedb.studio.view.common.geometry.Geometry.rectIncomingLineIntersect
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.GraphTheme
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.output.GraphOutput.State.Graph.Companion.emptyGraph
import com.vaticle.typeql.lang.TypeQL.`var`
import com.vaticle.typeql.lang.TypeQL.match
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.awt.Polygon
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.pow
import kotlin.math.sqrt

internal object GraphOutput : RunOutput() {

    val LOGGER = KotlinLogging.logger {}

    internal class State(val transaction: TypeDBTransaction, number: Int) : RunOutput.State() {

        override val name: String = "${Label.GRAPH} ($number)"
        val graph: Graph = emptyGraph()
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        val graphBuilder = GraphBuilder(transaction, coroutineScope)
        val viewport = Viewport(this)
        val physicsEngine = PhysicsEngine(this)
        var pointerPosition: Offset? by mutableStateOf(null)
        var hoveredVertex: Vertex? by mutableStateOf(null)
        val hoveredVertexChecker = HoveredVertexChecker(this)
        var theme: Color.GraphTheme? = null
        val visualiser = Visualiser(this)

        fun output(conceptMap: ConceptMap) {
            graphBuilder.add(conceptMap)
        }

        fun onQueryCompleted() {
            graphBuilder.completeAllEdges(graph)
        }

        private fun rendererContext(drawScope: DrawScope) = RendererContext(drawScope, theme!!)

        fun vertexBackgroundRenderer(vertex: Vertex, drawScope: DrawScope): VertexBackgroundRenderer {
            return VertexBackgroundRenderer.of(vertex, this, rendererContext(drawScope))
        }

        fun edgeRenderer(drawScope: DrawScope): EdgeRenderer {
            return EdgeRenderer(this, rendererContext(drawScope))
        }

        class Viewport(private val state: State) {
            var density: Float by mutableStateOf(1f); private set
            var physicalSize by mutableStateOf(Size.Zero); private set
            /** The world coordinates at the top-left corner of the viewport. */
            var worldCoordinates by mutableStateOf(Offset.Zero)
            private val physicalCenter get() = Offset(physicalSize.width / 2, physicalSize.height / 2)
            private var _scale by mutableStateOf(1f)
            var scale: Float
                get() = _scale
                set(value) { _scale = value.coerceIn(0.001f..10f) }
            var areInitialWorldCoordinatesSet = AtomicBoolean(false)

            fun updatePhysicalDimensions(size: Size, density: Float) {
                this.density = density
                physicalSize = size
            }

            fun alignWorldCenterWithPhysicalCenter() {
                worldCoordinates = -physicalCenter * density / scale
            }

            fun findVertexAtPhysicalPoint(physicalPoint: Offset): Vertex? {
                val worldPoint = physicalPointToWorldPoint(physicalPoint)
                val nearestVertices = nearestVertices(worldPoint)
                return nearestVertices.find { it.geometry.intersects(worldPoint) }
            }

            private fun physicalPointToWorldPoint(physicalPoint: Offset): Offset {
                val transformOrigin = (Offset(physicalSize.width, physicalSize.height) / 2f) / density
                val scaledPhysicalPoint = physicalPoint / density

                // Let 'physical' be the physical position of a point in the viewport, 'origin' be the transform origin
                // of the viewport, 'world' be the position of 'physical' in the world, 'viewportPosition' be the world
                // offset at the top left corner of the viewport. Then:
                // physical = origin + scale * (world - viewportPosition - origin)
                // Rearranging this equation gives the result below:
                return (((scaledPhysicalPoint - transformOrigin) / scale) + transformOrigin) + worldCoordinates
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
            private val _edges: MutableList<Edge> = Collections.synchronizedList(mutableListOf())
            val edges: Collection<Edge> get() = _edges

            // val explanations: MutableList<Explanation> = mutableListOf()
            val physics = Physics(this)

            companion object {
                internal fun emptyGraph() = Graph()
            }

            fun putThingVertexIfAbsent(iid: String, vertexFn: () -> Vertex.Thing): Boolean {
                return putVertexIfAbsent(iid, _thingVertices, vertexFn)
            }

            fun putTypeVertexIfAbsent(label: String, vertexFn: () -> Vertex.Type): Boolean {
                return putVertexIfAbsent(label, _typeVertices, vertexFn)
            }

            private fun <VERTEX: Vertex> putVertexIfAbsent(
                key: String, vertexMap: MutableMap<String, VERTEX>, vertexFn: () -> VERTEX
            ): Boolean {
                var added = false
                val vertex = vertexMap.computeIfAbsent(key) { added = true; vertexFn() }
                if (added) {
                    physics.placeVertex(vertex.geometry)
                    physics.addEnergy()
                }
                return added
            }

            fun addEdge(edge: Edge) {
                _edges += edge
                physics.addEnergy()
            }

            fun isEmpty() = vertices.isEmpty()
            fun isNotEmpty() = vertices.isNotEmpty()

            class Physics(private val graph: Graph) {
                private val simulation = BasicSimulation().apply {
                    alphaMin = 0.01
                    alphaDecay = 1 - alphaMin.pow(1.0 / 100)
                }
                var iteration by mutableStateOf(0L)
                val isStable get() = simulation.alpha < simulation.alphaMin
                val isStepRunning = AtomicBoolean(false)
                var alpha: Double
                    get() = simulation.alpha
                    set(value) { simulation.alpha = value }

                fun step() {
                    if (isStable || graph.isEmpty()) return
                    if (isStepRunning.compareAndSet(false, true)) {
                        setupForces()
                        simulation.tick()
                        iteration++
                        isStepRunning.set(false)
                    }
                }

                fun placeVertex(vertex: com.vaticle.force.graph.api.Vertex) {
                    simulation.placeVertex(vertex)
                }

                fun addEnergy() {
                    simulation.alpha = 1.0
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
                        val gravityStrength = 0.1
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

                /** Find the endpoint of an edge drawn from `source` position to this vertex */
                abstract fun edgeEndpoint(source: Offset): Offset?

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

                    override fun edgeEndpoint(source: Offset): Offset? {
                        val r = rect
                        val targetRect = Rect(Offset(r.left - 4, r.top - 4), Size(r.width + 8, r.height + 8))
                        return rectIncomingLineIntersect(source, targetRect)
                    }
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

                    override fun edgeEndpoint(source: Offset): Offset? {
                        val r = rect
                        val targetRect = Rect(Offset(r.left - 4, r.top - 4), Size(r.width + 8, r.height + 8))
                        return diamondIncomingLineIntersect(source, targetRect)
                    }
                }

                class Attribute(size: Size) : Geometry(size) {

                    override fun intersects(point: Offset): Boolean {
                        val xi = (point.x - position.x).pow(2) / (size.width / 2).pow(2)
                        val yi = (point.y - position.y).pow(2) / (size.height).pow(2)
                        return xi + yi < 1f
                    }

                    override fun edgeEndpoint(source: Offset): Offset? {
                        val ellipse = Ellipse(position.x, position.y, size.width / 2 + 2, size.height / 2 + 2)
                        return ellipseIncomingLineIntersect(source, ellipse)
                    }
                }
            }
        }

        sealed class Edge(open val source: Vertex, open val target: Vertex) {

            object Labels {
                const val HAS = "has"
                const val ISA = "isa"
                const val OWNS = "owns"
                const val SUB = "sub"
            }

            val geometry = Geometry(this)
            val physics = Physics(this)
            abstract val label: String

            interface Inferrable {
                val isInferred: Boolean
            }

            // Type edges
            class Sub(override val source: Vertex.Type, override val target: Vertex.Type) : Edge(source, target) {
                override val label = Labels.SUB
                fun copy(source: Vertex.Type, target: Vertex.Type) = Sub(source, target)
            }

            class Owns(override val source: Vertex.Type, override val target: Vertex.Type.Attribute) : Edge(source, target) {
                override val label = Labels.OWNS
                fun copy(source: Vertex.Type, target: Vertex.Type.Attribute) = Owns(source, target)
            }

            class Plays(override val source: Vertex.Type.Relation, override val target: Vertex.Type, val role: String) : Edge(source, target) {
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

            class Geometry(private val edge: Edge) {
                private var curveMidpoint: Offset? = null
                val isCurved get() = curveMidpoint != null
                val midpoint get() = curveMidpoint ?: midpoint(edge.source.geometry.position, edge.target.geometry.position)
            }

            class Physics(private val edge: Edge) : com.vaticle.force.graph.api.Edge {
                override fun source() = edge.source.geometry
                override fun target() = edge.target.geometry
            }
        }

        class Explanation(val vertices: Set<Vertex>)

        class GraphBuilder(
            val transaction: TypeDBTransaction, val coroutineScope: CoroutineScope, val schema: Schema = Schema()
        ) {
            private val thingVertices: MutableMap<String, Vertex.Thing> = ConcurrentHashMap()
            private val typeVertices: MutableMap<String, Vertex.Type> = ConcurrentHashMap()
            private val edges: ConcurrentLinkedQueue<Edge> = ConcurrentLinkedQueue()
            private val edgeCandidates: MutableMap<String, Collection<EdgeCandidate>> = ConcurrentHashMap()
            private val lock = ReentrantReadWriteLock(true)

            fun add(conceptMap: ConceptMap) {
                conceptMap.map().entries.map { (varName: String, concept: Concept) ->
                    add(varName, concept)
                }
            }

            private fun add(varName: String, concept: Concept) {
                when {
                    concept.isThing || concept.isThingType -> {
                        val (added, vertex) = putVertexIfAbsent(concept)
                        if (added) {
                            if (concept.isThing && concept.asThing().isInferred) initExplainables(concept, varName)
                            EdgeBuilder.of(concept, vertex, this).build()
                        }
                    }
                    concept.isRoleType -> { /* do nothing */ }
                    else -> throw unsupportedEncodingException(concept)
                }
            }

            private fun putVertexIfAbsent(concept: Concept): PutVertexResult {
                return when {
                    concept.isThing -> concept.asThing().let { thing ->
                        putVertexIfAbsent(thing.iid, thingVertices) { Vertex.Thing.of(thing) }
                    }
                    concept.isThingType -> concept.asThingType().let { type ->
                        putVertexIfAbsent(type.label.name(), typeVertices) { Vertex.Type.of(type) }
                    }
                    else -> throw unsupportedEncodingException(concept)
                }
            }

            private fun <VERTEX: Vertex> putVertexIfAbsent(
                key: String, vertexMap: MutableMap<String, VERTEX>, vertexFn: () -> VERTEX
            ): PutVertexResult {
                var added = false
                val vertex = lock.readLock().withLock {
                    val v = vertexMap.computeIfAbsent(key) { added = true; vertexFn() }
                    if (added) completeEdges(missingVertex = v)
                    v
                }
                return PutVertexResult(added, vertex)
            }

            data class PutVertexResult(val added: Boolean, val vertex: Vertex)

            fun addEdge(edge: Edge) {
                lock.readLock().withLock { edges += edge }
            }

            fun addEdgeCandidate(edge: EdgeCandidate) {
                val key = when (edge) {
                    is EdgeCandidate.Has -> edge.targetIID
                    is EdgeCandidate.Isa -> edge.targetLabel
                    is EdgeCandidate.Owns -> edge.targetLabel
                    is EdgeCandidate.Plays -> edge.sourceLabel
                    is EdgeCandidate.Sub -> edge.targetLabel
                }
                edgeCandidates.compute(key) { _, existing -> if (existing == null) listOf(edge) else existing + edge }
            }

            private fun completeEdges(missingVertex: Vertex) {
                val key = when (missingVertex) {
                    is Vertex.Type -> missingVertex.type.label.name()
                    is Vertex.Thing -> missingVertex.thing.iid
                }
                edgeCandidates.remove(key)?.let { candidates ->
                    candidates.forEach { edges += it.toEdge(missingVertex) }
                }
            }

            fun completeAllEdges(graph: Graph) {
                // Since there's no protection against an edge candidate, and the vertex that completes it, being added
                // concurrently, we do a final sanity check once all vertices + edges have been loaded.
                lock.readLock().withLock {
                    (graph.thingVertices + graph.typeVertices).values.forEach { completeEdges(it) }
                }
            }

            private fun initExplainables(concept: Concept, varName: String) {
                // TODO
            }

            fun dumpTo(graph: Graph) {
                lock.writeLock().withLock {
                    thingVertices.forEach { (iid, vertex) -> graph.putThingVertexIfAbsent(iid) { vertex } }
                    typeVertices.forEach { (label, vertex) -> graph.putTypeVertexIfAbsent(label) { vertex } }
                    edges.forEach { dumpEdgeTo(it, graph) }
                    thingVertices.clear()
                    typeVertices.clear()
                    edges.clear()
                }
            }

            private fun dumpEdgeTo(edge: Edge, graph: Graph) {
                // 'source' and 'vertex' may be stale if they represent vertices previously added to the graph.
                // Here we rebind them to the current graph state.
                graph.apply {
                    val syncedEdge = when (edge) {
                        is Edge.Has -> edge.copy(
                            thingVertices[edge.source.thing.iid]!!,
                            thingVertices[edge.target.thing.iid] as Vertex.Thing.Attribute
                        )
                        is Edge.Isa -> edge.copy(
                            thingVertices[edge.source.thing.iid]!!, typeVertices[edge.target.type.label.name()]!!
                        )
                        is Edge.Owns -> edge.copy(
                            typeVertices[edge.source.type.label.name()]!!,
                            typeVertices[edge.target.type.label.name()] as Vertex.Type.Attribute
                        )
                        is Edge.Plays -> edge.copy(
                            typeVertices[edge.source.type.label.name()]!! as Vertex.Type.Relation,
                            typeVertices[edge.target.type.label.name()]!!
                        )
                        is Edge.Roleplayer -> edge.copy(
                            thingVertices[(edge.source as Vertex.Thing).thing.iid]!! as Vertex.Thing.Relation,
                            thingVertices[edge.target.thing.iid]!!
                        )
                        is Edge.Sub -> edge.copy(
                            typeVertices[edge.source.type.label.name()]!!, typeVertices[edge.target.type.label.name()]!!
                        )
                    }
                    graph.addEdge(syncedEdge)
                }
            }

            fun unsupportedEncodingException(concept: Concept): IllegalStateException {
                return IllegalStateException("[$concept]'s encoding is not supported by AnswerLoader")
            }

            sealed class EdgeCandidate {

                interface Inferrable {
                    val isInferred: Boolean
                }

                abstract fun toEdge(vertex: Vertex): Edge

                // Type edges
                class Sub(val source: Vertex.Type, val targetLabel: String) : EdgeCandidate() {
                    override fun toEdge(vertex: Vertex) = Edge.Sub(source, vertex as Vertex.Type)
                }

                class Owns(val source: Vertex.Type, val targetLabel: String) : EdgeCandidate() {
                    override fun toEdge(vertex: Vertex) = Edge.Owns(source, vertex as Vertex.Type.Attribute)
                }

                class Plays(val sourceLabel: String, val target: Vertex.Type, val role: String) : EdgeCandidate() {
                    override fun toEdge(vertex: Vertex) = Edge.Plays(vertex as Vertex.Type.Relation, target, role)
                }

                // Thing edges
                class Has(
                    val source: Vertex.Thing, val targetIID: String, override val isInferred: Boolean = false
                ) : EdgeCandidate(), Inferrable {

                    override fun toEdge(vertex: Vertex) = Edge.Has(source, vertex as Vertex.Thing.Attribute, isInferred)
                }

                // Thing-to-type edges
                class Isa(val source: Vertex.Thing, val targetLabel: String) : EdgeCandidate() {
                    override fun toEdge(vertex: Vertex) = Edge.Isa(source, vertex as Vertex.Type)
                }
            }

            class Schema(val typeAttributeOwnershipMap: ConcurrentMap<String, Boolean> = ConcurrentHashMap())

            sealed class EdgeBuilder(val graphBuilder: GraphBuilder) {

                abstract fun build()

                companion object {
                    fun of(concept: Concept, vertex: Vertex, graphBuilder: GraphBuilder): EdgeBuilder {
                        return when {
                            concept.isThing -> Thing(concept.asThing(), vertex as Vertex.Thing, graphBuilder)
                            concept.isThingType -> ThingType(concept.asThingType(), vertex as Vertex.Type, graphBuilder)
                            else -> throw graphBuilder.unsupportedEncodingException(concept)
                        }
                    }
                }

                class Thing(
                    val thing: com.vaticle.typedb.client.api.concept.thing.Thing,
                    val thingVertex: Vertex.Thing, ctx: GraphBuilder
                ) : EdgeBuilder(ctx) {
                    private val remoteThing = thing.asRemote(ctx.transaction)

                    override fun build() {
                        loadIsaEdge()
                        loadHasEdges()
                        if (thing.isRelation) loadRoleplayerEdgesAndVertices()
                    }

                    private fun loadIsaEdge() {
                        thing.type.let { type ->
                            val typeVertex = graphBuilder.typeVertices[type.label.name()]
                            if (typeVertex != null) graphBuilder.addEdge(Edge.Isa(thingVertex, typeVertex))
                            else graphBuilder.addEdgeCandidate(EdgeCandidate.Isa(thingVertex, type.label.name()))
                        }
                    }

                    private fun loadHasEdges() {
                        // construct TypeQL query so that reasoning can run
                        // test for ability to own attributes, to ensure query will not throw during type inference
                        if (!canOwnAttributes()) return
                        val (x, attr) = Pair("x", "attr")
                        graphBuilder.transaction.query().match(match(`var`(x).iid(thing.iid).has(`var`(attr)))).forEach { answer ->
                            val attribute = answer.get(attr).asAttribute()
                            // TODO: test logic (was 'attr in explainables().attributes().keys', not 'attribute.isInferred')
                            val isEdgeInferred = attribute.isInferred || ownershipIsExplainable(attr, answer)
                            val attributeVertex = graphBuilder.thingVertices[attribute.iid] as? Vertex.Thing.Attribute
                            if (attributeVertex != null) {
                                graphBuilder.addEdge(Edge.Has(thingVertex, attributeVertex, isEdgeInferred))
                            } else {
                                graphBuilder.addEdgeCandidate(EdgeCandidate.Has(thingVertex, attribute.iid, isEdgeInferred))
                            }
                        }
                    }

                    private fun canOwnAttributes(): Boolean {
                        val typeLabel = thing.type.label.name()
                        return graphBuilder.schema.typeAttributeOwnershipMap.getOrPut(typeLabel) {
                            // non-atomic update as Concept API call is idempotent and cheaper than locking the map
                            thing.type.asRemote(graphBuilder.transaction).owns.findAny().isPresent
                        }
                    }

                    private fun ownershipIsExplainable(attributeVarName: String, conceptMap: ConceptMap): Boolean {
                        return attributeVarName in conceptMap.explainables().ownerships().keys.map { it.second() }
                    }

                    private fun loadRoleplayerEdgesAndVertices() {
                        remoteThing.asRelation().playersByRoleType.entries.forEach { (roleType, roleplayers) ->
                            roleplayers.forEach { roleplayer ->
                                graphBuilder.putVertexIfAbsent(roleplayer.iid, graphBuilder.thingVertices) {
                                    Vertex.Thing.of(roleplayer)
                                }
                                val roleplayerVertex = graphBuilder.thingVertices[roleplayer.iid]!!
                                graphBuilder.addEdge(Edge.Roleplayer(
                                    thingVertex as Vertex.Thing.Relation, roleplayerVertex,
                                    roleType.label.name(), thing.isInferred
                                ))
                            }
                        }
                    }
                }

                class ThingType(
                    thingType: com.vaticle.typedb.client.api.concept.type.ThingType,
                    private val typeVertex: Vertex.Type, ctx: GraphBuilder
                ) : EdgeBuilder(ctx) {
                    private val remoteThingType = thingType.asRemote(ctx.transaction)

                    override fun build() {
                        loadSubEdge()
                        loadOwnsEdges()
                        loadPlaysEdges()
                    }

                    private fun loadSubEdge() {
                        remoteThingType.supertype?.let { supertype ->
                            val supertypeVertex = graphBuilder.typeVertices[supertype.label.name()]
                            if (supertypeVertex != null) graphBuilder.addEdge(Edge.Sub(typeVertex, supertypeVertex))
                            else graphBuilder.addEdgeCandidate(EdgeCandidate.Sub(typeVertex, supertype.label.name()))
                        }
                    }

                    private fun loadOwnsEdges() {
                        remoteThingType.owns.forEach { attributeType ->
                            val attributeTypeLabel = attributeType.label.name()
                            val attributeTypeVertex = graphBuilder.typeVertices[attributeTypeLabel]
                                    as? Vertex.Type.Attribute
                            if (attributeTypeVertex != null) {
                                graphBuilder.addEdge(Edge.Owns(typeVertex, attributeTypeVertex))
                            } else graphBuilder.addEdgeCandidate(EdgeCandidate.Owns(typeVertex, attributeTypeLabel))
                        }
                    }

                    private fun loadPlaysEdges() {
                        remoteThingType.plays.forEach { roleType ->
                            val relationTypeLabel = roleType.label.scope().get()
                            val roleLabel = roleType.label.name()
                            val relationTypeVertex = graphBuilder.typeVertices[relationTypeLabel]
                                    as? Vertex.Type.Relation
                            if (relationTypeVertex != null) {
                                graphBuilder.addEdge(Edge.Plays(relationTypeVertex, typeVertex, roleLabel))
                            } else {
                                graphBuilder.addEdgeCandidate(
                                    EdgeCandidate.Plays(relationTypeLabel, typeVertex, roleLabel)
                                )
                            }
                        }
                    }
                }
            }
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
                    (it.position - state.viewport.worldCoordinates) * density
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

            val density = state.viewport.density
            val edgeLabelSizes = state.visualiser.edgeLabelSizes

            fun draw(edges: Iterable<Edge>, detailed: Boolean) {
                ctx.drawScope.drawPoints(
                    points = edgeCoordinates(edges, detailed), pointMode = PointMode.Lines,
                    color = ctx.theme.edge, strokeWidth = density
                )
            }

            private fun edgeCoordinates(edges: Iterable<Edge>, detailed: Boolean): List<Offset> {
                return synchronized(edges) {
                    if (detailed) edges.flatMap { prettyEdgeCoordinates(it) }
                    else edges.flatMap { simpleEdgeCoordinates(it) }
                }
            }

            private fun simpleEdgeCoordinates(edge: Edge): Iterable<Offset> {
                return line(edge.source.geometry.position, edge.target.geometry.position)
            }

            private fun prettyEdgeCoordinates(edge: Edge): Iterable<Offset> {
                return PrettyEdgeCoordinates(edge, this).get()
            }

            fun Offset.toViewport(): Offset {
                return (this - state.viewport.worldCoordinates) * density
            }

            fun line(source: Offset, target: Offset): Iterable<Offset> {
                return listOf(source.toViewport(), target.toViewport())
            }

            private class PrettyEdgeCoordinates(edge: Edge, private val renderer: EdgeRenderer) {

                private val linePart1Source: Offset?
                private val linePart1Target: Offset?
                private val linePart2Source: Offset?
                private val linePart2Target: Offset?
                private val labelRect: Rect?

                init {
                    val source = edge.source.geometry
                    val target = edge.target.geometry

                    val labelSize = renderer.edgeLabelSizes[edge.label]
                    labelRect = labelSize?.let {
                        val m = midpoint(source.position, target.position)
                        Rect(
                            Offset(m.x - it.width.value / 2 - 2, m.y - it.height.value / 2 - 2),
                            Size(it.width.value + 4, it.height.value + 4)
                        )
                    }

                    linePart1Source = source.edgeEndpoint(target.position)
                    linePart2Target = target.edgeEndpoint(source.position)
                    linePart1Target = if (linePart1Source != null && labelRect != null) {
                        rectIncomingLineIntersect(linePart1Source, labelRect)
                    } else null
                    linePart2Source = if (linePart2Target != null && labelRect != null) {
                        rectIncomingLineIntersect(linePart2Target, labelRect)
                    } else null
                }

                fun get(): Iterable<Offset> {
                    return listOfNotNull(linePart1(), linePart2(), arrowhead()).flatten()
                }

                private fun linePart1(): Iterable<Offset>? {
                    return if (linePart1Source == null || linePart1Target == null) null
                    else renderer.line(linePart1Source, linePart1Target)
                }

                private fun linePart2(): Iterable<Offset>? {
                    return if (linePart2Source == null || linePart2Target == null) null
                    else renderer.line(linePart2Source, linePart2Target)
                }

                private fun arrowhead(): Iterable<Offset>? {
                    if (linePart2Source == null || linePart2Target == null) return null
                    return with(renderer) {
                        val lines = arrowhead(linePart2Source, linePart2Target, arrowLength = 6f, arrowWidth = 3f)
                        lines?.toList()?.flatMap { listOf(it.from.toViewport(), it.to.toViewport()) }
                    }
                }
            }
        }

        data class RendererContext(val drawScope: DrawScope, val theme: Color.GraphTheme)

        class PhysicsEngine(private val state: State) {

            suspend fun run() {
                while (true) {
                    withFrameMillis {
                        return@withFrameMillis if (isReadyToStep()) { state.coroutineScope.launch { step() } } else Job()
                    }.join()
                }
            }

            private fun isReadyToStep(): Boolean {
                return !state.graph.physics.isStepRunning.get()
            }

            private fun step() {
                try {
                    state.graphBuilder.dumpTo(state.graph)
                    state.graph.physics.step()
                } catch (e: Exception) {
                    GlobalState.notification.systemError(LOGGER, e, Message.Visualiser.UNEXPECTED_ERROR)
                    state.graph.physics.terminate()
                }
            }
        }

        class HoveredVertexChecker(private val state: State) {
            private var lastScanDoneTime = System.currentTimeMillis()

            suspend fun poll() {
                while (true) {
                    withFrameMillis { state.pointerPosition?.let { if (isReadyToScan()) scan(it) } }
                }
            }

            private fun isReadyToScan() = System.currentTimeMillis() - lastScanDoneTime > 33

            private fun scan(pointerPosition: Offset) {
                state.hoveredVertex = state.viewport.findVertexAtPhysicalPoint(pointerPosition)
                lastScanDoneTime = System.currentTimeMillis()
            }
        }
    }

    class Visualiser(private val state: State) {

        val edgeLabelSizes: MutableMap<String, DpSize> = ConcurrentHashMap()

        @Composable
        fun Layout(modifier: Modifier) {
            val density = LocalDensity.current.density
            state.theme = GraphTheme.colors

            Box(
                modifier.graphicsLayer(clip = true).background(GraphTheme.colors.background)
                    .onGloballyPositioned { onLayout(density, it) }
            ) {
                Graphics(state.graph.physics.iteration, state.viewport.density, state.viewport.physicalSize, state.viewport.scale)
            }

            LaunchedEffect(Unit) { state.physicsEngine.run() }
            LaunchedEffect(state.viewport.scale, state.viewport.density) { state.hoveredVertexChecker.poll() }
        }

        @Composable
        @Suppress("UNUSED_PARAMETER")
        // TODO: we tried using Composables.key here, but it performs drastically worse than this explicit Composable
        //       with unused parameters - investigate why
        fun Graphics(physicsIteration: Long, density: Float, size: Size, scale: Float) {
            Box(Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale)) {
                EdgeLayer()
                VertexLayer()
            }
            PointerInput.Handler(state, Modifier.fillMaxSize().zIndex(100f))
        }

        private fun onLayout(density: Float, layout: LayoutCoordinates) {
            state.viewport.updatePhysicalDimensions(layout.size.toSize(), density)
            if (state.viewport.areInitialWorldCoordinatesSet.compareAndSet(false, true)) {
                state.viewport.alignWorldCenterWithPhysicalCenter()
            }
        }

        @Composable
        private fun EdgeLayer() {
            synchronized(state.graph.edges) {
                // TODO: change this conditional operator to a || after implementing out-of-viewport detection
                val detailed = state.graph.edges.size <= 500 && state.viewport.scale > 0.2
                Canvas(Modifier.fillMaxSize()) { state.edgeRenderer(this).draw(state.graph.edges, detailed) }
                if (detailed) state.graph.edges.forEach { EdgeLabel(it) }
            }
        }

        @Composable
        private fun EdgeLabel(edge: State.Edge) {
            val density = LocalDensity.current.density
            val position = edge.geometry.midpoint - state.viewport.worldCoordinates
            val size = edgeLabelSizes[edge.label]?.let { Size(it.width.value * density, it.height.value * density) }
            val baseColor = if (edge is State.Edge.Inferrable && edge.isInferred) GraphTheme.colors.inferred
                else GraphTheme.colors.edgeLabel
            val color = baseColor

            when (size) {
                null -> EdgeLabelMeasurer(edge)
                else -> {
                    val rect = Rect(Offset(position.x - size.width / 2, position.y - size.height / 2), size)
                    Box(Modifier.offset(rect.left.dp, rect.top.dp).size(rect.width.dp, rect.height.dp), Alignment.Center) {
                        Form.Text(
                            value = edge.label,
                            textStyle = Theme.typography.code1.copy(color = color, textAlign = TextAlign.Center),
                            color = color, // TODO: remove this hack when Form.Text.textStyle.color is supported
                        )
                    }
                }
            }
        }

        @Composable
        private fun EdgeLabelMeasurer(edge: State.Edge) {
            with(LocalDensity.current) {
                Form.Text(
                    value = edge.label, textStyle = Theme.typography.code1,
                    modifier = Modifier.graphicsLayer(alpha = 0f).onSizeChanged {
                        edgeLabelSizes[edge.label] = DpSize(it.width.toDp(), it.height.toDp())
                    }
                )
            }
        }

        @Composable
        private fun VertexLayer() {
            val vertices = state.graph.vertices
            Canvas(Modifier.fillMaxSize()) { vertices.forEach { drawVertexBackground(it) } }
            if (vertices.size <= 200) vertices.forEach { VertexLabel(it) }
        }

        private fun DrawScope.drawVertexBackground(vertex: State.Vertex) {
            state.vertexBackgroundRenderer(vertex, this).draw()
        }

        @Composable
        private fun VertexLabel(vertex: State.Vertex) {
            val r = vertex.geometry.rect
            val x = (r.left - state.viewport.worldCoordinates.x).dp
            val y = (r.top - state.viewport.worldCoordinates.y).dp
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
                            viewport.worldCoordinates -= dragAmount / (viewport.scale * viewport.density)
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

    @Composable
    internal fun Layout(state: State) {
        super.Layout(toolbarButtons(state)) { modifier ->
            state.visualiser.Layout(modifier)
        }
    }

    private fun toolbarButtons(state: State): List<Form.IconButtonArg> {
        return listOf()
    }
}
