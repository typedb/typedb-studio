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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.vaticle.force.graph.api.Simulation
import com.vaticle.force.graph.force.CenterForce
import com.vaticle.force.graph.force.CollideForce
import com.vaticle.force.graph.force.LinkForce
import com.vaticle.force.graph.force.ManyBodyForce
import com.vaticle.force.graph.force.XForce
import com.vaticle.force.graph.force.YForce
import com.vaticle.force.graph.impl.BasicSimulation
import com.vaticle.force.graph.impl.BasicVertex
import com.vaticle.force.graph.util.RandomEffects
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.thing.Attribute
import com.vaticle.typedb.client.api.concept.thing.Relation
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.RoleType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.client.api.logic.Explanation
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchCompletableFuture
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.state.connection.TransactionState
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.geometry.Geometry.AngularDirection.Clockwise
import com.vaticle.typedb.studio.view.common.geometry.Geometry.AngularDirection.CounterClockwise
import com.vaticle.typedb.studio.view.common.geometry.Geometry.Arc
import com.vaticle.typedb.studio.view.common.geometry.Geometry.Ellipse
import com.vaticle.typedb.studio.view.common.geometry.Geometry.Line
import com.vaticle.typedb.studio.view.common.geometry.Geometry.arcThroughPoints
import com.vaticle.typedb.studio.view.common.geometry.Geometry.arrowhead
import com.vaticle.typedb.studio.view.common.geometry.Geometry.diamondArcIntersectAngles
import com.vaticle.typedb.studio.view.common.geometry.Geometry.diamondIncomingLineIntersect
import com.vaticle.typedb.studio.view.common.geometry.Geometry.ellipseIncomingLineIntersect
import com.vaticle.typedb.studio.view.common.geometry.Geometry.midpoint
import com.vaticle.typedb.studio.view.common.geometry.Geometry.normalisedAngle
import com.vaticle.typedb.studio.view.common.geometry.Geometry.radToDeg
import com.vaticle.typedb.studio.view.common.geometry.Geometry.rectArcIntersectAngles
import com.vaticle.typedb.studio.view.common.geometry.Geometry.rectIncomingLineIntersect
import com.vaticle.typedb.studio.view.common.geometry.Geometry.sweepAngle
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.material.BrowserGroup
import com.vaticle.typedb.studio.view.material.Form
import com.vaticle.typedb.studio.view.material.Frame
import com.vaticle.typedb.studio.view.material.Icon
import com.vaticle.typedb.studio.view.material.Separator
import com.vaticle.typedb.studio.view.material.Table
import com.vaticle.typedb.studio.view.material.Tabs
import com.vaticle.typedb.studio.view.output.GraphOutput.Graph.Physics.Constants.COLLIDE_RADIUS
import com.vaticle.typedb.studio.view.output.GraphOutput.Graph.Physics.Constants.CURVE_COLLIDE_RADIUS
import com.vaticle.typedb.studio.view.output.GraphOutput.Graph.Physics.Constants.CURVE_COMPRESSION_POWER
import com.vaticle.typedb.studio.view.output.GraphOutput.Vertex.Type.Companion.typeIcon
import com.vaticle.typeql.lang.TypeQL.`var`
import com.vaticle.typeql.lang.TypeQL.match
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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import mu.KotlinLogging

internal class GraphOutput constructor(transactionState: TransactionState, number: Int) : RunOutput() {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override val name: String = "${Label.GRAPH} ($number)"
    override val icon: Icon.Code = Icon.Code.DIAGRAM_PROJECT
    override val buttons: List<Form.IconButtonArg> = emptyList()

    val interactions = Interactions(this)
    val graph = Graph(this)
    val coroutineScope = CoroutineScope(EmptyCoroutineContext)
    val graphBuilder = GraphBuilder(graph, transactionState, coroutineScope)
    val viewport = Viewport(this)
    val physicsRunner = PhysicsRunner(this)
    var theme: Color.GraphTheme? = null
    val visualiser = Visualiser(this)
    val browsers: List<BrowserGroup.Browser> = listOf(Visualiser.ConceptPreview(this, 0, false))

    // TODO: this needs a better home
    val edgeLabelSizes: MutableMap<String, DpSize> = ConcurrentHashMap()

    fun output(conceptMap: ConceptMap) {
        graphBuilder.loadConceptMap(conceptMap)
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

    class Graph(private val output: GraphOutput) {

        private val _thingVertices: MutableMap<String, Vertex.Thing> = ConcurrentHashMap()
        private val _typeVertices: MutableMap<String, Vertex.Type> = ConcurrentHashMap()
        private val _edges: MutableList<Edge> = Collections.synchronizedList(mutableListOf())

        val thingVertices: Map<String, Vertex.Thing> get() = _thingVertices
        val typeVertices: Map<String, Vertex.Type> get() = _typeVertices
        val vertices: Collection<Vertex> get() = thingVertices.values + typeVertices.values
        val edges: Collection<Edge> get() = _edges

        val physics = Physics(this, output.interactions)
        val reasoner = Reasoner()

        fun putThingVertexIfAbsent(iid: String, vertexFn: () -> Vertex.Thing): Boolean {
            return putVertexIfAbsent(iid, _thingVertices, vertexFn)
        }

        fun putTypeVertexIfAbsent(label: String, vertexFn: () -> Vertex.Type): Boolean {
            return putVertexIfAbsent(label, _typeVertices, vertexFn)
        }

        private fun <VERTEX : Vertex> putVertexIfAbsent(
            key: String, vertexMap: MutableMap<String, VERTEX>, vertexFn: () -> VERTEX
        ): Boolean {
            var added = false
            val vertex = vertexMap.computeIfAbsent(key) { added = true; vertexFn() }
            if (added) {
                physics.placeVertex(vertex.geometry)
                onChange()
            }
            return added
        }

        fun addEdge(edge: Edge) {
            // TODO: figure out why this deduplication is required for correctness
            if (_edges.none { it.source == edge.source && it.target == edge.target && it.label == edge.label }) {
                _edges += edge
                onChange()
            }
        }

        fun makeEdgeCurved(edge: Edge) {
            physics.addCurveProvider(edge)
        }

        private fun onChange() {
            physics.addEnergy()
            output.interactions.rebuildFocusedVertexNetwork()
        }

        fun isEmpty() = vertices.isEmpty()
        fun isNotEmpty() = vertices.isNotEmpty()

        class Physics(val graph: Graph, private val interactions: Interactions) {

            private object Constants {
                const val COLLIDE_RADIUS = 65.0
                const val CURVE_COMPRESSION_POWER = 0.35
                const val CURVE_COLLIDE_RADIUS = 35.0
            }

            private val simulation = BasicSimulation().apply {
                alphaMin = 0.01
                alphaDecay = standardAlphaDecay()
            }
            var iteration by mutableStateOf(0L)
            private val isStable get() = simulation.alpha < simulation.alphaMin
            val isStepRunning = AtomicBoolean(false)
            var alpha: Double
                get() = simulation.alpha
                set(value) {
                    simulation.alpha = value
                }
            val drag = Drag(this)
            var forceSource = ForceSource.Query
            private var edgeCurveProviders = ConcurrentHashMap<Edge, com.vaticle.force.graph.api.Vertex>()

            fun step() {
                if (isStable || graph.isEmpty()) return
                if (isStepRunning.compareAndSet(false, true)) {
                    when (forceSource) {
                        ForceSource.Query -> initialiseForces()
                        ForceSource.Drag -> drag.initialiseForces(interactions.draggedVertex)
                    }
                    simulation.tick()
                    iteration++
                    isStepRunning.set(false)
                }
            }

            fun placeVertex(vertex: com.vaticle.force.graph.api.Vertex) {
                simulation.placeVertex(vertex)
            }

            fun addCurveProvider(edge: Edge) {
                edgeCurveProviders.computeIfAbsent(edge) {
                    val basePosition = edge.geometry.midpoint
                    val offset = RandomEffects.jiggle()
                    val curveProvider = BasicVertex(basePosition.x + offset, basePosition.y + offset)
                    edge.physics.curveProvider = curveProvider
                    curveProvider
                }
            }

            fun addEnergy() {
                simulation.alpha = 1.0
            }

            fun terminate() {
                simulation.alpha = 0.0
            }

            fun Simulation.standardAlphaDecay(): Double {
                return 1 - alphaMin.pow(1.0 / 200)
            }

            private fun initialiseForces() {
                simulation.apply {
                    forces.clear()
                    localForces.clear()
                    initialiseGlobalForces()
                    initialiseLocalForces()
                }
            }

            private fun Simulation.initialiseGlobalForces() {
                // TODO: track changes to vertices + edges, rebuild forces only if there are changes
                val vertices = graph.vertices.map { it.geometry }
                val edges = graph.edges.map { it.geometry }
                forces.add(CenterForce(vertices, 0.0, 0.0))
                forces.add(CollideForce(vertices, COLLIDE_RADIUS))
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

            private fun Simulation.initialiseLocalForces() {
                localForces.add(CollideForce(edgeCurveProviders.values.toList(), CURVE_COLLIDE_RADIUS))
                edgeCurveProviders.forEach { (edge, curveProvider) ->
                    localForces.add(
                        XForce(listOf(curveProvider), edge.geometry.midpoint.x.toDouble(), CURVE_COMPRESSION_POWER)
                    )
                    localForces.add(
                        YForce(listOf(curveProvider), edge.geometry.midpoint.y.toDouble(), CURVE_COMPRESSION_POWER)
                    )
                }
            }

            enum class ForceSource { Query, Drag }

            class Drag(private val physics: Physics) {

                private val graph = physics.graph
                private val simulation = physics.simulation
                private var verticesFrozenWhileDragging: Collection<Vertex> by mutableStateOf(listOf())

                fun onDragStart(vertex: Vertex) {
                    freezePermanently(vertex)
                    physics.forceSource = ForceSource.Drag
                    simulation.apply {
                        alpha = 0.25
                        alphaDecay = 0.0
                    }
                }

                fun onDragMove(vertex: Vertex, dragDistance: Offset) {
                    vertex.geometry.position += dragDistance
                }

                fun onDragEnd() {
                    with(physics) { simulation.alphaDecay = simulation.standardAlphaDecay() }
                    releaseFrozenVertices()
                }

                fun initialiseForces(draggedVertex: Vertex?) {
                    simulation.apply {
                        forces.clear()
                        localForces.clear()

                        val vertices = graph.vertices.map { it.geometry }
                        forces.add(CollideForce(vertices, COLLIDE_RADIUS))
                        if (draggedVertex is Vertex.Thing && draggedVertex.isRoleplayer()) {
                            pullLinkedRelations(draggedVertex)
                        }
                        with(physics) { initialiseLocalForces() }
                    }
                }

                private fun Simulation.pullLinkedRelations(vertex: Vertex.Thing) {
                    val attributeEdges = vertex.ownedAttributeEdges()
                    val attributeVertices = attributeEdges.map { it.target }
                    val relationVertices = vertex.playingRoleEdges().map { it.source }
                    val roleplayerEdges = relationVertices.flatMap { it.roleplayerEdges() }
                    val roleplayerVertices = roleplayerEdges.map { it.target }
                    val vertices = (attributeVertices + relationVertices + roleplayerVertices).toSet()
                    val edges = attributeEdges + roleplayerEdges

                    freezeUntilDragEnd(roleplayerVertices.filter { it != vertex && !it.geometry.isFrozen })
                    forces.add(LinkForce(vertices.map { it.geometry }, edges.map { it.geometry }, 90.0, 0.25))
                }

                private fun freezePermanently(vertex: Vertex) {
                    vertex.geometry.isFrozen = true
                }

                private fun freezeUntilDragEnd(vertices: Collection<Vertex>) {
                    vertices.forEach { it.geometry.isFrozen = true }
                    verticesFrozenWhileDragging = vertices
                }

                private fun releaseFrozenVertices() {
                    verticesFrozenWhileDragging.forEach { it.geometry.isFrozen = false }
                    verticesFrozenWhileDragging = listOf()
                }
            }
        }

        class Reasoner {

            val explainables: MutableMap<Vertex.Thing, ConceptMap.Explainable> = ConcurrentHashMap()
            val explanationIterators: MutableMap<Vertex.Thing, Iterator<Explanation>> = ConcurrentHashMap()
            private val vertexExplanations =
                Collections.synchronizedList(mutableListOf<Pair<Vertex.Thing, Explanation>>())

            var explanationsByVertex: Map<Vertex.Thing, Set<Explanation>> = emptyMap(); private set

            fun addVertexExplanations(vertexExplanations: Iterable<Pair<Vertex.Thing, Explanation>>) {
                synchronized(vertexExplanations) {
                    this.vertexExplanations += vertexExplanations
                    rebuildIndexes()
                }
            }

            private fun rebuildIndexes() {
                explanationsByVertex = vertexExplanations
                    .groupBy({ it.first }) { it.second }
                    .mapValues { it.value.toSet() }
            }
        }
    }

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

        sealed class Type(val type: com.vaticle.typedb.client.api.concept.type.Type, graph: Graph) :
            Vertex(type, graph) {

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

        class Label(val fullText: String, truncatedLength: Int) {

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
            abstract fun curvedEdgeEndAngle(arc: Arc): Float?

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
                    return rectIncomingLineIntersect(source, incomingEdgeTargetRect)
                }

                override fun curvedEdgeEndAngle(arc: Arc): Float? {
                    // There should be only one intersection point when the arc has an endpoint within the vertex
                    return rectArcIntersectAngles(arc, incomingEdgeTargetRect).firstOrNull()
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
                    return diamondIncomingLineIntersect(source, incomingEdgeTargetRect)
                }

                override fun curvedEdgeEndAngle(arc: Arc): Float? {
                    return diamondArcIntersectAngles(arc, incomingEdgeTargetRect).firstOrNull()
                }
            }

            class Attribute(size: Size) : Geometry(size) {

                override fun intersects(point: Offset): Boolean {
                    val xi = (point.x - position.x).pow(2) / (size.width / 2).pow(2)
                    val yi = (point.y - position.y).pow(2) / (size.height).pow(2)
                    return xi + yi < 1f
                }

                override fun edgeEndpoint(source: Offset): Offset {
                    val ellipse = Ellipse(position.x, position.y, size.width / 2 + 2, size.height / 2 + 2)
                    return ellipseIncomingLineIntersect(source, ellipse)
                }

                override fun curvedEdgeEndAngle(arc: Arc): Float? {
                    // TODO: this implementation approximates the elliptical vertex as a diamond (like a relation);
                    //       we should have a dedicated implementation for intersecting an arc with an ellipse
                    val incomingEdgeTargetRect = Rect(
                        Offset(rect.left - 4, rect.top - 4), Size(rect.width + 8, rect.height + 8)
                    )
                    return diamondArcIntersectAngles(arc, incomingEdgeTargetRect).firstOrNull()
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
        val physics = Physics()
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

            val isCurved get() = edge.physics.curveProvider != null
            val midpoint get() = midpoint(edge.source.geometry.position, edge.target.geometry.position)
            val curveMidpoint
                get() = edge.physics.curveProvider?.let {
                    Offset(it.x.toFloat(), it.y.toFloat())
                }

            override fun source() = edge.source.geometry
            override fun target() = edge.target.geometry
        }

        class Physics {
            var curveProvider: com.vaticle.force.graph.api.Vertex? = null
        }
    }

    class GraphBuilder(
        val graph: Graph, val transactionState: TransactionState, val coroutineScope: CoroutineScope,
        val schema: Schema = Schema()
    ) {
        private val thingVertices = ConcurrentHashMap<String, Vertex.Thing>()
        private val typeVertices = ConcurrentHashMap<String, Vertex.Type>()
        private val edges = ConcurrentLinkedQueue<Edge>()
        private val edgeCandidates = ConcurrentHashMap<String, Collection<EdgeCandidate>>()
        private val explainables = ConcurrentHashMap<Vertex.Thing, ConceptMap.Explainable>()
        private val vertexExplanations = ConcurrentLinkedQueue<Pair<Vertex.Thing, Explanation>>()
        private val lock = ReentrantReadWriteLock(true)

        fun loadConceptMap(conceptMap: ConceptMap, answerSource: AnswerSource = AnswerSource.Query) {
            conceptMap.map().entries.map { (varName: String, concept: Concept) ->
                when (concept) {
                    is Thing, is ThingType -> {
                        val (added, vertex) = putVertexIfAbsent(concept)
                        if (added) {
                            if (concept is Thing) {
                                vertex as Vertex.Thing
                                if (transactionState.explain.value && concept.isInferred) {
                                    addExplainables(concept, vertex, conceptMap.explainables(), varName)
                                }
                                if (answerSource is AnswerSource.Explanation) {
                                    vertexExplanations += Pair(vertex, answerSource.explanation)
                                }
                            }
                            EdgeBuilder.of(concept, vertex, this).build()
                        }
                    }
                    is RoleType -> { /* do nothing */
                    }
                    else -> throw unsupportedEncodingException(concept)
                }
            }
        }

        private fun putVertexIfAbsent(concept: Concept): PutVertexResult {
            return when (concept) {
                is Thing -> putVertexIfAbsent(concept.iid, thingVertices) { Vertex.Thing.of(concept, graph) }
                is ThingType -> putVertexIfAbsent(concept.label.name(), typeVertices) { Vertex.Type.of(concept, graph) }
                else -> throw unsupportedEncodingException(concept)
            }
        }

        private fun <VERTEX : Vertex> putVertexIfAbsent(
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

        private fun addExplainables(
            thing: Thing, thingVertex: Vertex.Thing, explainables: ConceptMap.Explainables, varName: String
        ) {
            try {
                this.explainables.computeIfAbsent(thingVertex) {
                    when (thing) {
                        is Relation -> explainables.relation(varName)
                        is Attribute<*> -> explainables.attribute(varName)
                        else -> throw IllegalStateException("Inferred Thing was neither a Relation nor an Attribute")
                    }
                }
            } catch (_: TypeDBClientException) {
                // TODO: Currently we need to catch this exception because not every Inferred concept is
                //       Explainable. Once that bug is fixed, remove this catch statement.
                /* do nothing */
            }
        }

        fun dumpTo(graph: Graph) {
            lock.writeLock().withLock {
                dumpVerticesTo(graph)
                dumpEdgesTo(graph)
                dumpExplainablesTo(graph)
                dumpExplanationStructureTo(graph)
            }
        }

        private fun dumpVerticesTo(graph: Graph) {
            thingVertices.forEach { (iid, vertex) -> graph.putThingVertexIfAbsent(iid) { vertex } }
            typeVertices.forEach { (label, vertex) -> graph.putTypeVertexIfAbsent(label) { vertex } }
            thingVertices.clear()
            typeVertices.clear()
        }

        private fun dumpEdgesTo(graph: Graph) {
            edges.forEach { dumpEdgeTo(it, graph) }
            computeCurvedEdges(edges, graph)
            edges.clear()
        }

        private fun dumpEdgeTo(edge: Edge, graph: Graph): Edge {
            // 'source' and 'vertex' may be stale if they represent vertices previously added to the graph.
            // Here we rebind them to the current graph state.
            return graph.run {
                when (edge) {
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
                }.also { addEdge(it) }
            }
        }

        private fun computeCurvedEdges(edges: Iterable<Edge>, graph: Graph) {
            val edgesBySource = graph.edges.groupBy { it.source }
            edges.forEach { edge ->
                val edgeBand = getEdgeBand(edge, edgesBySource)
                if (edgeBand.size > 1) edgeBand.forEach { graph.makeEdgeCurved(it) }
            }
        }

        private fun getEdgeBand(edge: Edge, allEdgesBySource: Map<Vertex, Collection<Edge>>): Collection<Edge> {
            // Grouping edges by source allows us to minimise the number of passes we do over the whole graph
            return (allEdgesBySource.getOrDefault(edge.source, listOf()).filter { it.target == edge.target }
                    + allEdgesBySource.getOrDefault(edge.target, listOf()).filter { it.target == edge.source })
        }

        private fun dumpExplainablesTo(graph: Graph) {
            explainables.forEach { graph.reasoner.explainables.putIfAbsent(it.key, it.value) }
            explainables.clear()
        }

        private fun dumpExplanationStructureTo(graph: Graph) {
            graph.reasoner.addVertexExplanations(vertexExplanations)
            vertexExplanations.clear()
        }

        fun explain(vertex: Vertex.Thing) {
            launchCompletableFuture(GlobalState.notification, LOGGER) {
                val iterator = graph.reasoner.explanationIterators[vertex]
                    ?: runExplainQuery(vertex).also { graph.reasoner.explanationIterators[vertex] = it }
                fetchNextExplanation(vertex, iterator)
            }.exceptionally { e ->
                GlobalState.notification.systemError(LOGGER, e, Message.Visualiser.UNEXPECTED_ERROR)
            }
        }

        private fun runExplainQuery(vertex: Vertex.Thing): Iterator<Explanation> {
            val explainable = graph.reasoner.explainables[vertex] ?: throw IllegalStateException("Not explainable")
            return transactionState.transaction?.query()?.explain(explainable)?.iterator()
                ?: Collections.emptyIterator()
        }

        private fun fetchNextExplanation(vertex: Vertex.Thing, iterator: Iterator<Explanation>) {
            if (iterator.hasNext()) {
                val explanation = iterator.next()
                vertexExplanations += Pair(vertex, explanation)
                loadConceptMap(explanation.condition(), AnswerSource.Explanation(explanation))
            } else {
                GlobalState.notification.info(LOGGER, Message.Visualiser.FULLY_EXPLAINED)
            }
        }

        fun unsupportedEncodingException(concept: Concept): IllegalStateException {
            return IllegalStateException("[$concept]'s encoding is not supported by AnswerLoader")
        }

        sealed class AnswerSource {
            object Query : AnswerSource()
            class Explanation(val explanation: com.vaticle.typedb.client.api.logic.Explanation) : AnswerSource()
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
                    return when (concept) {
                        is com.vaticle.typedb.client.api.concept.thing.Thing -> {
                            Thing(concept, vertex as Vertex.Thing, graphBuilder)
                        }
                        is com.vaticle.typedb.client.api.concept.type.ThingType -> {
                            ThingType(concept.asThingType(), vertex as Vertex.Type, graphBuilder)
                        }
                        else -> throw graphBuilder.unsupportedEncodingException(concept)
                    }
                }
            }

            class Thing(
                val thing: com.vaticle.typedb.client.api.concept.thing.Thing,
                private val thingVertex: Vertex.Thing,
                private val ctx: GraphBuilder
            ) : EdgeBuilder(ctx) {
                private val remoteThing get() = ctx.transactionState.transaction?.let { thing.asRemote(it) }

                override fun build() {
                    loadIsaEdge()
                    loadHasEdges()
                    if (thing is Relation) loadRoleplayerEdgesAndVertices()
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
                    graphBuilder.transactionState.transaction?.query()
                        ?.match(match(`var`(x).iid(thing.iid).has(`var`(attr))))
                        ?.forEach { answer ->
                            val attribute = answer.get(attr).asAttribute()
                            // TODO: test logic (was 'attr in explainables().attributes().keys', not 'attribute.isInferred')
                            val isEdgeInferred = attribute.isInferred || ownershipIsExplainable(attr, answer)
                            val attributeVertex =
                                graphBuilder.thingVertices[attribute.iid] as? Vertex.Thing.Attribute
                            if (attributeVertex != null) {
                                graphBuilder.addEdge(Edge.Has(thingVertex, attributeVertex, isEdgeInferred))
                            } else {
                                graphBuilder.addEdgeCandidate(
                                    EdgeCandidate.Has(
                                        thingVertex,
                                        attribute.iid,
                                        isEdgeInferred
                                    )
                                )
                            }
                        }
                }

                private fun canOwnAttributes(): Boolean {
                    val typeLabel = thing.type.label.name()
                    return graphBuilder.schema.typeAttributeOwnershipMap.getOrPut(typeLabel) {
                        // non-atomic update as Concept API call is idempotent and cheaper than locking the map
                        graphBuilder.transactionState.transaction?.let {
                            thing.type.asRemote(it).owns.findAny().isPresent
                        } ?: false
                    }
                }

                private fun ownershipIsExplainable(attributeVarName: String, conceptMap: ConceptMap): Boolean {
                    return attributeVarName in conceptMap.explainables().ownerships().keys.map { it.second() }
                }

                private fun loadRoleplayerEdgesAndVertices() {
                    graphBuilder.apply {
                        remoteThing?.asRelation()?.playersByRoleType?.entries?.forEach { (roleType, roleplayers) ->
                            roleplayers.forEach { roleplayer ->
                                putVertexIfAbsent(roleplayer.iid, thingVertices) {
                                    Vertex.Thing.of(roleplayer, graph)
                                }
                                val roleplayerVertex = thingVertices[roleplayer.iid]!!
                                addEdge(
                                    Edge.Roleplayer(
                                        thingVertex as Vertex.Thing.Relation, roleplayerVertex,
                                        roleType.label.name(), thing.isInferred
                                    )
                                )
                            }
                        }
                    }
                }
            }

            class ThingType(
                private val thingType: com.vaticle.typedb.client.api.concept.type.ThingType,
                private val typeVertex: Vertex.Type,
                private val ctx: GraphBuilder
            ) : EdgeBuilder(ctx) {
                private val remoteThingType get() = ctx.transactionState.transaction?.let { thingType.asRemote(it) }

                override fun build() {
                    loadSubEdge()
                    loadOwnsEdges()
                    loadPlaysEdges()
                }

                private fun loadSubEdge() {
                    remoteThingType?.supertype?.let { supertype ->
                        val supertypeVertex = graphBuilder.typeVertices[supertype.label.name()]
                        if (supertypeVertex != null) graphBuilder.addEdge(Edge.Sub(typeVertex, supertypeVertex))
                        else graphBuilder.addEdgeCandidate(EdgeCandidate.Sub(typeVertex, supertype.label.name()))
                    }
                }

                private fun loadOwnsEdges() {
                    remoteThingType?.owns?.forEach { attributeType ->
                        val attributeTypeLabel = attributeType.label.name()
                        val attributeTypeVertex = graphBuilder.typeVertices[attributeTypeLabel]
                                as? Vertex.Type.Attribute
                        if (attributeTypeVertex != null) {
                            graphBuilder.addEdge(Edge.Owns(typeVertex, attributeTypeVertex))
                        } else graphBuilder.addEdgeCandidate(EdgeCandidate.Owns(typeVertex, attributeTypeLabel))
                    }
                }

                private fun loadPlaysEdges() {
                    remoteThingType?.plays?.forEach { roleType ->
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

    class Viewport(private val output: GraphOutput) {
        var density: Float by mutableStateOf(1f); private set
        var physicalSize by mutableStateOf(DpSize.Zero); private set

        /** The world coordinates at the top-left corner of the viewport. */
        var worldCoordinates by mutableStateOf(Offset.Zero)
        private val physicalCenter get() = DpOffset(physicalSize.width / 2, physicalSize.height / 2)
        private var _scale by mutableStateOf(1f)
        var scale: Float
            get() = _scale
            set(value) {
                _scale = value.coerceIn(0.001f..10f)
            }
        var areInitialWorldCoordinatesSet = AtomicBoolean(false)

        fun updatePhysicalDimensions(size: Size, density: Float) {
            this.density = density
            physicalSize = toDP(size, density)
        }

        fun alignWorldCenterWithPhysicalCenter() {
            worldCoordinates = Offset(-physicalCenter.x.value, -physicalCenter.y.value) / scale
        }

        fun findVertexAt(physicalPoint: Offset): Vertex? {
            val worldPoint = physicalPointToWorldPoint(physicalPoint)
            val nearestVertices = nearestVertices(worldPoint)
            return nearestVertices.find { it.geometry.intersects(worldPoint) }
        }

        private fun physicalPointToWorldPoint(physicalPoint: Offset): Offset {
            val transformOrigin = Offset(physicalSize.width.value, physicalSize.height.value) / 2f
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
            output.graph.vertices.associateWithTo(vertexDistances) {
                (worldPoint - it.geometry.position).getDistanceSquared()
            }
            return sequence {
                while (vertexDistances.isNotEmpty()) {
                    yield(vertexDistances.entries.minByOrNull { it.value }!!.key)
                }
            }.take(10)
        }
    }

    sealed class VertexBackgroundRenderer(
        private val vertex: Vertex, protected val output: GraphOutput, protected val ctx: RendererContext
    ) {
        companion object {
            private const val CORNER_RADIUS = 5f
            private const val HOVERED_ALPHA = .675f
            private const val BACKGROUND_ALPHA = .25f
            private const val HOVERED_BACKGROUND_ALPHA = .175f

            fun of(vertex: Vertex, output: GraphOutput, ctx: RendererContext): VertexBackgroundRenderer =
                when (vertex) {
                    is Vertex.Type.Entity, is Vertex.Type.Thing, is Vertex.Thing.Entity -> Entity(vertex, output, ctx)
                    is Vertex.Type.Relation, is Vertex.Thing.Relation -> Relation(vertex, output, ctx)
                    is Vertex.Type.Attribute, is Vertex.Thing.Attribute -> Attribute(vertex, output, ctx)
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

        // Logically, if the vertex is dragged, it should also be hovered; however, this is not always true
        // because the vertex takes some time to "catch up" to the pointer. So check both conditions.
        private val alpha = with(output.interactions) {
            when {
                vertex.isHovered && vertex.isBackground -> HOVERED_BACKGROUND_ALPHA
                vertex.isBackground -> BACKGROUND_ALPHA
                vertex.isHovered -> HOVERED_ALPHA
                else -> 1f
            }
        }
        protected val color = baseColor.copy(alpha)
        private val density = output.viewport.density
        protected val rect = vertex.geometry.let {
            Rect(
                (it.position - output.viewport.worldCoordinates) * density
                        - Offset(it.size.width * density / 2, it.size.height * density / 2),
                it.size * density
            )
        }

        protected val cornerRadius get() = CornerRadius(CORNER_RADIUS * density)

        protected fun getHighlight(): Highlight? = when {
            isInHoveredExplanationTree() -> Highlight.of(ctx.theme.explanation.copy(alpha), density * 1.5f, this)
            isInferred() -> Highlight.of(ctx.theme.inferred.copy(alpha), density, this)
            else -> null
        }

        protected open fun getHighlightRect(highlightWidth: Float) = Rect(
            rect.topLeft - Offset(highlightWidth, highlightWidth),
            Size(rect.size.width + highlightWidth * 2, rect.size.height + highlightWidth * 2)
        )

        private fun isInHoveredExplanationTree(): Boolean {
            return output.graph.reasoner.explanationsByVertex[vertex]
                ?.any { it in output.interactions.hoveredVertexExplanations } ?: false
        }

        private fun isInferred() = vertex.concept is Thing && vertex.concept.isInferred

        abstract fun draw()

        class Highlight private constructor(
            val color: androidx.compose.ui.graphics.Color, val width: Float, val rect: Rect
        ) {
            companion object {
                fun of(color: androidx.compose.ui.graphics.Color, width: Float, renderer: VertexBackgroundRenderer)
                        : Highlight {
                    return Highlight(color, width, renderer.getHighlightRect(width))
                }
            }
        }

        class Entity(vertex: Vertex, output: GraphOutput, ctx: RendererContext) :
            VertexBackgroundRenderer(vertex, output, ctx) {

            override fun draw() {
                getHighlight()?.let {
                    ctx.drawScope.drawRoundRect(it.color, it.rect.topLeft, it.rect.size, cornerRadius)
                }
                ctx.drawScope.drawRoundRect(color, rect.topLeft, rect.size, cornerRadius)
            }
        }

        class Relation(vertex: Vertex, output: GraphOutput, ctx: RendererContext) :
            VertexBackgroundRenderer(vertex, output, ctx) {

            // We start with a square of width n and transform it into a rhombus
            private val n = (rect.height / sqrt(2.0)).toFloat()
            private val baseShape = Rect(offset = rect.center - Offset(n / 2, n / 2), size = Size(n, n))

            override fun getHighlightRect(highlightWidth: Float) = Rect(
                baseShape.topLeft - Offset(highlightWidth, highlightWidth),
                Size(baseShape.size.width + highlightWidth * 2, baseShape.size.height + highlightWidth * 2)
            )

            override fun draw() {
                with(ctx.drawScope) {
                    withTransform({
                        scale(scaleX = rect.width / rect.height, scaleY = 1f, pivot = rect.center)
                        rotate(degrees = 45f, pivot = rect.center)
                    }) {
                        getHighlight()?.let { drawRoundRect(it.color, it.rect.topLeft, it.rect.size, cornerRadius) }
                        drawRoundRect(color, baseShape.topLeft, baseShape.size, cornerRadius)
                    }
                }
            }
        }

        class Attribute(vertex: Vertex, output: GraphOutput, ctx: RendererContext) :
            VertexBackgroundRenderer(vertex, output, ctx) {

            override fun draw() {
                getHighlight()?.let { ctx.drawScope.drawOval(it.color, it.rect.topLeft, it.rect.size) }
                ctx.drawScope.drawOval(color, rect.topLeft, rect.size)
            }
        }
    }

    class EdgeRenderer(private val output: GraphOutput, private val ctx: RendererContext) {

        companion object {
            private const val BACKGROUND_ALPHA = .25f
            private const val ARROWHEAD_LENGTH = 6f
            private const val ARROWHEAD_WIDTH = 3f
        }

        val density = output.viewport.density
        private val edgeLabelSizes = output.edgeLabelSizes

        fun draw(edges: Iterable<Edge>, detailed: Boolean) {
            for ((colorCode, edgeGroup) in EdgesByColorCode(edges, output)) {
                draw(edgeGroup, detailed, colorCode.toColor(ctx.theme))
            }
        }

        private fun draw(edges: Iterable<Edge>, detailed: Boolean, color: androidx.compose.ui.graphics.Color) {
            if (detailed) {
                val (curvedEdges, straightEdges) = edges.partition { it.geometry.isCurved }
                drawLines(straightEdges, true, color)
                curvedEdges.forEach { drawCurvedEdge(it, color) }
            } else {
                drawLines(edges, false, color)
            }
        }

        private fun drawLines(edges: Iterable<Edge>, detailed: Boolean, color: androidx.compose.ui.graphics.Color) {
            ctx.drawScope.drawPoints(edgeCoordinates(edges, detailed), PointMode.Lines, color, density)
        }

        private fun drawCurvedEdge(edge: Edge, color: androidx.compose.ui.graphics.Color) {
            val curveMidpoint = edge.geometry.curveMidpoint!!
            val arc = arcThroughPoints(edge.source.geometry.position, curveMidpoint, edge.target.geometry.position)
            if (arc != null) {
                drawCurvedEdge(edge, color, arc, curveMidpoint)
            } else {
                // the 3 points are almost collinear, so fall back to straight lines
                drawLines(listOf(edge), true, color)
            }
        }

        private fun drawCurvedEdge(
            edge: Edge, color: androidx.compose.ui.graphics.Color, fullArc: Arc, labelPosition: Offset
        ) {
            val source = edge.source.geometry
            val target = edge.target.geometry
            val labelRect = labelRect(edge, labelPosition) ?: return
            val arcStartAngle = source.curvedEdgeEndAngle(fullArc) ?: return
            val arcEndAngle = target.curvedEdgeEndAngle(fullArc) ?: return

            // Once we have the arc angle at the label's position, we can split the arc into 2 segments,
            // using them to identify the angles where the segments go into the label
            val arcLabelAngle = atan2(labelPosition.y - fullArc.center.y, labelPosition.x - fullArc.center.x)
                .radToDeg().normalisedAngle()

            // The "full arc" runs between the midpoints of the two vertices;
            // the "major arc" runs between the points where the final arcs will enter the two vertices
            val majorArc = MajorArc(arcStartAngle, arcLabelAngle, arcEndAngle)
            drawCurveSegment1(fullArc, majorArc, labelRect, color, edge)
            drawCurveSegment2(fullArc, majorArc, labelRect, color, edge)
        }

        class MajorArc(val start: Float, val label: Float, val end: Float)

        private fun drawCurveSegment1(
            fullArc: Arc, majorArc: MajorArc, labelRect: Rect,
            color: androidx.compose.ui.graphics.Color, edge: Edge
        ) {
            // Find where the first arc segment goes into the label
            // There should be precisely one intersection point since the arc ends inside the rectangle
            val sweepAngleUnclipped = sweepAngle(from = majorArc.start, to = majorArc.label, fullArc.direction)
            val arcSegmentUnclipped = Arc(fullArc.topLeft, fullArc.size, majorArc.start, sweepAngleUnclipped)
            val labelIntersectAngle = rectArcIntersectAngles(arcSegmentUnclipped, labelRect).firstOrNull() ?: return

            val sweepAngle = sweepAngle(from = majorArc.start, to = labelIntersectAngle, fullArc.direction)
            when {
                abs(sweepAngle) < 180 -> ctx.drawScope.drawArc(
                    color = color, startAngle = majorArc.start, sweepAngle = sweepAngle, useCenter = false,
                    topLeft = fullArc.topLeft.toViewport(), size = fullArc.size.toViewport(), style = Stroke(density)
                )
                // If sweep angle > 180, most likely the label has reached an awkward spot to draw an arc through,
                // so we fall back to a straight line segment
                else -> PrettyEdgeCoordinates(edge, this).arrowSegment1(edge.source, labelRect)?.let {
                    ctx.drawScope.drawPoints(it.toList(), PointMode.Lines, color, density)
                }
            }
        }

        private fun drawCurveSegment2(
            fullArc: Arc, majorArc: MajorArc, labelRect: Rect,
            color: androidx.compose.ui.graphics.Color, edge: Edge
        ) {
            val sweepAngleUnclipped = sweepAngle(from = majorArc.label, to = majorArc.end, fullArc.direction)
            val arcSegmentUnclipped = Arc(fullArc.topLeft, fullArc.size, majorArc.label, sweepAngleUnclipped)
            val labelIntersectAngle = rectArcIntersectAngles(arcSegmentUnclipped, labelRect).firstOrNull() ?: return

            val sweepAngle = sweepAngle(from = labelIntersectAngle, to = majorArc.end, fullArc.direction)
            when {
                abs(sweepAngle) < 180 -> {
                    ctx.drawScope.drawArc(
                        color = color, startAngle = labelIntersectAngle, sweepAngle = sweepAngle, useCenter = false,
                        topLeft = fullArc.topLeft.toViewport(), size = fullArc.size.toViewport(),
                        style = Stroke(density)
                    )
                    curveArrowhead(fullArc, majorArc)?.toList()?.forEach {
                        ctx.drawScope.drawLine(color, it.from, it.to, density)
                    }
                }
                else -> PrettyEdgeCoordinates(edge, this).arrowSegment2(labelRect, edge.target)?.let {
                    ctx.drawScope.drawPoints(it.toList(), PointMode.Lines, color, density)
                }
            }
        }

        private fun curveArrowhead(fullArc: Arc, majorArc: MajorArc): Pair<Line, Line>? {
            val arrowTarget = fullArc.offsetAtAngle(majorArc.end)
            val approachAngle = when (fullArc.direction) {
                Clockwise -> (majorArc.end - 1).normalisedAngle()
                CounterClockwise -> (majorArc.end + 1).normalisedAngle()
            }
            val arrowSource = fullArc.offsetAtAngle(approachAngle)
            val lines = arrowhead(arrowSource, arrowTarget, ARROWHEAD_LENGTH, ARROWHEAD_WIDTH)
            return lines?.let { Pair(it.first.toViewport(), it.second.toViewport()) }
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
            return (this - output.viewport.worldCoordinates) * density
        }

        private fun Size.toViewport(): Size {
            return this * density
        }

        private fun Line.toViewport(): Line {
            return Line(from.toViewport(), to.toViewport())
        }

        fun line(source: Offset, target: Offset): Iterable<Offset> {
            return listOf(source.toViewport(), target.toViewport())
        }

        fun labelRect(edge: Edge, position: Offset): Rect? {
            val labelSize = edgeLabelSizes[edge.label]
            return labelSize?.let {
                Rect(
                    Offset(position.x - it.width.value / 2 - 2, position.y - it.height.value / 2 - 2),
                    Size(it.width.value + 4, it.height.value + 4)
                )
            }
        }

        private enum class EdgeColorCode {
            Regular,
            Background,
            Inferred,
            InferredBackground;

            companion object {
                fun of(edge: Edge, output: GraphOutput): EdgeColorCode {
                    val isInferred = edge is Edge.Inferrable && edge.isInferred
                    val isBackground = with(output.interactions) { edge.isBackground }
                    return when {
                        isInferred && isBackground -> InferredBackground
                        isBackground -> Background
                        isInferred -> Inferred
                        else -> Regular
                    }
                }
            }

            fun toColor(theme: Color.GraphTheme) = when (this) {
                Regular -> theme.edge
                Background -> theme.edge.copy(alpha = BACKGROUND_ALPHA)
                Inferred -> theme.inferred
                InferredBackground -> theme.inferred.copy(alpha = BACKGROUND_ALPHA)
            }
        }

        private class EdgesByColorCode(
            edges: Iterable<Edge>, output: GraphOutput
        ) : Iterable<Map.Entry<EdgeColorCode, List<Edge>>> {

            private val _map = EdgeColorCode.values().associateWith { mutableListOf<Edge>() }
            val map: Map<EdgeColorCode, List<Edge>> get() = _map

            init {
                synchronized(edges) { edges.forEach { _map[EdgeColorCode.of(it, output)]!! += it } }
            }

            override fun iterator(): Iterator<Map.Entry<EdgeColorCode, List<Edge>>> {
                return map.iterator()
            }
        }

        private class PrettyEdgeCoordinates(val edge: Edge, private val renderer: EdgeRenderer) {

            private val labelRect = renderer.labelRect(
                edge, midpoint(edge.source.geometry.position, edge.target.geometry.position)
            )

            fun get(): Iterable<Offset> {
                val arrowSegment1 = labelRect?.let { arrowSegment1(edge.source, it) }
                val arrowSegment2 = labelRect?.let { arrowSegment2(it, edge.target) }
                return listOfNotNull(arrowSegment1, arrowSegment2).flatten()
            }

            fun arrowSegment1(sourceVertex: Vertex, targetRect: Rect): Iterable<Offset>? {
                val source = sourceVertex.geometry.edgeEndpoint(targetRect.center)
                val target = source?.let { rectIncomingLineIntersect(it, targetRect) } ?: return null
                return renderer.line(source, target)
            }

            fun arrowSegment2(sourceRect: Rect, targetVertex: Vertex): Iterable<Offset>? {
                val target = targetVertex.geometry.edgeEndpoint(sourceRect.center)
                val source = target?.let { rectIncomingLineIntersect(it, sourceRect) } ?: return null
                return listOfNotNull(renderer.line(source, target), arrowhead(source, target)).flatten()
            }

            private fun arrowhead(source: Offset, target: Offset): Iterable<Offset>? {
                return with(renderer) {
                    val lines = arrowhead(source, target, ARROWHEAD_LENGTH, ARROWHEAD_WIDTH)
                    lines?.toList()?.flatMap { listOf(it.from.toViewport(), it.to.toViewport()) }
                }
            }
        }
    }

    data class RendererContext(val drawScope: DrawScope, val theme: Color.GraphTheme)

    class PhysicsRunner(private val output: GraphOutput) {

        suspend fun run() {
            while (true) {
                withFrameMillis {
                    return@withFrameMillis if (isReadyToStep()) {
                        output.coroutineScope.launchAndHandle(GlobalState.notification, LOGGER) { step() }
                    } else Job()
                }.join()
            }
        }

        private fun isReadyToStep(): Boolean {
            return !output.graph.physics.isStepRunning.get()
        }

        private fun step() {
            try {
                output.graphBuilder.dumpTo(output.graph)
                output.graph.physics.step()
            } catch (e: Exception) {
                GlobalState.notification.systemError(LOGGER, e, Message.Visualiser.UNEXPECTED_ERROR)
                output.graph.physics.terminate()
            }
        }
    }

    class Interactions(private val output: GraphOutput) {

        var pointerPosition: Offset? by mutableStateOf(null)

        var hoveredVertex: Vertex? by mutableStateOf(null)
        val hoveredVertexChecker = HoveredVertexChecker(output)
        var hoveredVertexExplanations: Set<Explanation> by mutableStateOf(emptySet())

        private var _focusedVertex: Vertex? by mutableStateOf(null)
        var focusedVertex: Vertex?
            get() = _focusedVertex
            set(value) {
                rebuildFocusedVertexNetwork(value)
                _focusedVertex = value
            }
        var focusedVertexNetwork: Set<Vertex> by mutableStateOf(emptySet())

        var draggedVertex: Vertex? by mutableStateOf(null)

        // Logically, if the vertex is dragged, it should also be hovered; however, this is not always true
        // because the vertex takes some time to "catch up" to the pointer. So check both conditions.
        val Vertex.isHovered get() = this == hoveredVertex || this == draggedVertex

        val Vertex.isBackground get() = focusedVertex != null && this !in focusedVertexNetwork

        val Edge.isBackground get() = focusedVertex != null && source != focusedVertex && target != focusedVertex

        fun rebuildFocusedVertexNetwork(focusedVertex: Vertex? = _focusedVertex) {
            focusedVertexNetwork = if (focusedVertex != null) {
                val linkedVertices = output.graph.edges.mapNotNull { edge ->
                    when (focusedVertex) {
                        edge.source -> edge.target
                        edge.target -> edge.source
                        else -> null
                    }
                }
                setOf(focusedVertex) + linkedVertices
            } else emptySet()
        }

        class HoveredVertexChecker(private val output: GraphOutput) {

            private var lastScanDoneTime = System.currentTimeMillis()

            suspend fun poll() {
                while (true) {
                    withFrameMillis { output.interactions.pointerPosition?.let { if (isReadyToScan()) scan(it) } }
                }
            }

            private fun isReadyToScan() = System.currentTimeMillis() - lastScanDoneTime > 33

            private fun scan(pointerPosition: Offset) {
                val hoveredVertex = output.viewport.findVertexAt(pointerPosition)
                if (output.interactions.hoveredVertex == hoveredVertex) return
                output.interactions.hoveredVertex = hoveredVertex
                output.interactions.hoveredVertexExplanations = when (hoveredVertex) {
                    null -> emptySet()
                    else -> output.graph.reasoner.explanationsByVertex[hoveredVertex] ?: emptySet()
                }
                lastScanDoneTime = System.currentTimeMillis()
            }
        }
    }

    class Visualiser(private val output: GraphOutput) {

        private val graphArea = GraphArea(output)

        @Composable
        fun Layout(modifier: Modifier) {
            key(output) {
                Frame.Row(
                    modifier = modifier,
                    separator = Frame.SeparatorArgs(Separator.WEIGHT),
                    Frame.Pane(
                        id = GraphArea::class.java.name,
                        minSize = GraphArea.MIN_WIDTH,
                        initSize = Either.second(1f)
                    ) { graphArea.Layout() },
                    Frame.Pane(
                        id = ConceptPreview::class.java.name,
                        minSize = BrowserGroup.MIN_WIDTH,
                        initSize = Either.first(Tabs.Vertical.WIDTH),
                        initFreeze = true
                    ) { BrowserGroup.Layout(output.browsers, it) }
                )
            }
        }

        class GraphArea(private val output: GraphOutput) {

            companion object {
                val MIN_WIDTH = 120.dp

                // TODO: this is duplicated in 3 places
                private const val BACKGROUND_ALPHA = .25f
            }

            @Composable
            fun Layout() {
                val density = LocalDensity.current.density
                output.theme = Theme.graph

                Box(
                    Modifier.graphicsLayer(clip = true).background(Theme.graph.background)
                        .onGloballyPositioned { onLayout(density, it) }
                ) {
                    Graphics(
                        output.graph.physics.iteration, output.viewport.density, output.viewport.physicalSize,
                        output.viewport.scale
                    )
                }

                LaunchedEffect(output) { output.physicsRunner.run() }
                LaunchedEffect(
                    output,
                    output.viewport.scale,
                    output.viewport.density
                ) { output.interactions.hoveredVertexChecker.poll() }
            }

            @Composable
            @Suppress("UNUSED_PARAMETER")
            // TODO: we tried using Composables.key here, but it performs drastically worse (while zooming in/out) than
            //       this explicit Composable with unused parameters - investigate why
            fun Graphics(physicsIteration: Long, density: Float, size: DpSize, scale: Float) {
                // Since edges is a List we need to synchronize on it. Additionally we keep EdgeLayer and VertexLayer
                // synchronized on the same object. Otherwise, the renderer may block waiting
                // to acquire a lock, and the vertex and edge drawing may go out of sync.
                synchronized(output.graph.edges) {
                    Box(Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale)) {
                        EdgeLayer()
                        VertexLayer()
                    }
                }
                PointerInput.Handler(output, Modifier.fillMaxSize().zIndex(100f))
            }

            private fun onLayout(density: Float, layout: LayoutCoordinates) {
                output.viewport.updatePhysicalDimensions(layout.size.toSize(), density)
                // TODO: this check exists because the first composition of GraphArea will have a width of MIN_WIDTH,
                //       before inflating to fill the max width, but there should be a more elegant solution than this.
                if (layout.size.width > MIN_WIDTH.value * density) {
                    if (output.viewport.areInitialWorldCoordinatesSet.compareAndSet(false, true)) {
                        output.viewport.alignWorldCenterWithPhysicalCenter()
                    }
                }
            }

            @Composable
            private fun EdgeLayer() {
                // TODO: this will improve sharply after out-of-viewport detection
                val simpleEdges = mutableListOf<Edge>()
                val detailedEdges = mutableListOf<Edge>()
                output.graph.edges.forEach {
                    if (
                        (output.graph.edges.size <= 500 && output.viewport.scale > 0.2)
                        || output.interactions.focusedVertex in listOf(it.source, it.target)
                    ) detailedEdges += it
                    else simpleEdges += it
                }

                Canvas(Modifier.fillMaxSize()) {
                    output.edgeRenderer(this).draw(simpleEdges, false)
                    output.edgeRenderer(this).draw(detailedEdges, true)
                }
                detailedEdges.forEach { EdgeLabel(it) }
            }

            @Composable
            private fun EdgeLabel(edge: Edge) {
                val density = LocalDensity.current.density
                val rawPosition = edge.geometry.curveMidpoint ?: edge.geometry.midpoint
                val position = rawPosition - output.viewport.worldCoordinates
                val size =
                    output.edgeLabelSizes[edge.label]?.let { Size(it.width.value * density, it.height.value * density) }
                val baseColor = if (edge is Edge.Inferrable && edge.isInferred) Theme.graph.inferred
                else Theme.graph.edgeLabel
                val alpha = with(output.interactions) { if (edge.isBackground) BACKGROUND_ALPHA else 1f }
                val color = baseColor.copy(alpha)

                when (size) {
                    null -> EdgeLabelMeasurer(edge)
                    else -> {
                        val rect = Rect(Offset(position.x - size.width / 2, position.y - size.height / 2), size)
                        Box(
                            Modifier.offset(rect.left.dp, rect.top.dp).size(rect.width.dp, rect.height.dp),
                            Alignment.Center
                        ) {
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
            private fun EdgeLabelMeasurer(edge: Edge) {
                with(LocalDensity.current) {
                    Form.Text(
                        value = edge.label, textStyle = Theme.typography.code1,
                        modifier = Modifier.graphicsLayer(alpha = 0f).onSizeChanged {
                            output.edgeLabelSizes[edge.label] = DpSize(it.width.toDp(), it.height.toDp())
                        }
                    )
                }
            }

            @Composable
            private fun VertexLayer() {
                val vertices = output.graph.vertices
                Canvas(Modifier.fillMaxSize()) { vertices.forEach { drawVertexBackground(it) } }
                // TODO: we won't need this distinction after out-of-viewport detection is done
                val labelledVertices = if (vertices.size <= 200) vertices else output.interactions.focusedVertexNetwork
                labelledVertices.forEach { VertexLabel(it, it.geometry.position) }
            }

            private fun DrawScope.drawVertexBackground(vertex: Vertex) {
                output.vertexBackgroundRenderer(vertex, this).draw()
            }

            @Composable
            @Suppress("UNUSED_PARAMETER")
            // TODO: I'm not really sure why we need 'position' here. Without it, the vertex label intermittently desyncs
            //       from the vertex's position, but it should be updating when physicsIteration does
            private fun VertexLabel(vertex: Vertex, position: Offset) {
                val r = vertex.geometry.rect
                val x = (r.left - output.viewport.worldCoordinates.x).dp
                val y = (r.top - output.viewport.worldCoordinates.y).dp
                val color = Theme.graph.vertexLabel

                Box(Modifier.offset(x, y).size(r.width.dp, r.height.dp), Alignment.Center) {
                    Form.Text(
                        vertex.label.text,
                        textStyle = Theme.typography.code1,
                        color = color,
                        align = TextAlign.Center
                    )
                }
            }

            private object PointerInput {

                @Composable
                fun Handler(output: GraphOutput, modifier: Modifier) {
                    DragAndScroll(output, modifier) {
                        // Nested elements are required for drag and tap events to not conflict with each other
                        TapAndHover(output, modifier)
                    }
                }

                @Composable
                fun DragAndScroll(output: GraphOutput, modifier: Modifier, content: @Composable () -> Unit) {
                    val viewport = output.viewport
                    Box(
                        modifier
                            .pointerInput(output, viewport.density, viewport.scale) {
                                detectDragGestures(
                                    onDragStart = { _ ->
                                        output.interactions.draggedVertex?.let {
                                            output.graph.physics.drag.onDragStart(
                                                it
                                            )
                                        }
                                    },
                                    onDragEnd = {
                                        output.graph.physics.drag.onDragEnd()
                                        output.interactions.draggedVertex = null
                                    },
                                    onDragCancel = {
                                        output.graph.physics.drag.onDragEnd()
                                        output.interactions.draggedVertex = null
                                    }
                                ) /* onDrag = */ { _, dragAmount ->
                                    val worldDragDistance = dragAmount / (viewport.scale * viewport.density)
                                    val draggedVertex = output.interactions.draggedVertex
                                    if (draggedVertex != null) {
                                        output.graph.physics.drag.onDragMove(draggedVertex, worldDragDistance)
                                    } else {
                                        viewport.worldCoordinates -= worldDragDistance
                                    }
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
                fun TapAndHover(output: GraphOutput, modifier: Modifier) {
                    Box(modifier
                        .pointerMoveFilter(
                            onMove = { output.interactions.pointerPosition = it; false },
                            onExit = { output.interactions.pointerPosition = null; false }
                        )
                        .pointerInput(output) {
                            detectTapGestures(
                                onPress = { point ->
                                    output.interactions.draggedVertex = output.viewport.findVertexAt(point)
                                    if (tryAwaitRelease()) output.interactions.draggedVertex = null
                                },
                                onDoubleTap = { point ->
                                    output.viewport.findVertexAt(point)?.let {
                                        // TODO: this should require SHIFT-doubleclick, not doubleclick
                                        if (it is Vertex.Thing && it.thing.isInferred) output.graphBuilder.explain(
                                            it
                                        )
                                    }
                                }
                            ) /* onTap = */ { point ->
                                output.interactions.focusedVertex = output.viewport.findVertexAt(point)
                            }
                        }
                    )
                }
            }
        }

        class ConceptPreview constructor(
            private val output: GraphOutput,
            order: Int,
            isOpen: Boolean
        ) : BrowserGroup.Browser(isOpen, order) {

            override val label: String = Label.PREVIEW
            override val icon: Icon.Code = Icon.Code.EYE
            override val isActive: Boolean = true
            override var buttons: List<Form.IconButtonArg> = emptyList()

            private val titleSectionPadding = 10.dp

            data class Property(val key: String, val value: String)

            companion object {
                private val MESSAGE_PADDING = 20.dp

                fun propertiesOf(concept: Concept): List<Property> {
                    return listOfNotNull(
                        if (concept is Thing) Label.INTERNAL_ID to concept.iid else null,
                        if (concept is Attribute<*>) Label.VALUE to concept.valueString() else null,
                    )
                }

                private infix fun String.to(value: String): Property {
                    return Property(this, value)
                }

                private fun Attribute<*>.valueString(): String = when {
                    isDateTime -> asDateTime().value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    else -> value.toString()
                }
            }

            @Composable
            override fun BrowserLayout() {
                val focusedVertex = output.interactions.focusedVertex
                if (focusedVertex == null) SelectVertexMessage()
                else Column(Modifier.fillMaxSize().background(Theme.studio.backgroundMedium)) {
                    TitleSection(focusedVertex.concept)
                    if (propertiesOf(focusedVertex.concept).isNotEmpty()) Table(focusedVertex.concept)
                }
            }

            @Composable
            private fun SelectVertexMessage() {
                Box(
                    modifier = Modifier.fillMaxSize().background(Theme.studio.backgroundLight).padding(MESSAGE_PADDING),
                    contentAlignment = Alignment.Center
                ) { Form.Text(Label.GRAPH_CONCEPT_PREVIEW_PLACEHOLDER, align = TextAlign.Center, softWrap = true) }
            }

            // TODO: copied from TypePage.kt on 23/05/2022
            @Composable
            private fun TitleSection(concept: Concept) {
                val type = if (concept is Type) concept else concept.asThing().type
                Box(Modifier.padding(titleSectionPadding)) {
                    Form.TextBox(text = displayName(type), leadingIcon = typeIcon(type))
                }
            }

            @Composable
            private fun displayName(type: Type): AnnotatedString = displayName(type, Theme.studio.onPrimary)

            private fun displayName(type: Type, baseFontColor: androidx.compose.ui.graphics.Color): AnnotatedString {
                return buildAnnotatedString {
                    append(type.label.scopedName())
                    if (type is AttributeType) type.valueType?.let { valueType ->
                        append(" ")
                        withStyle(SpanStyle(baseFontColor.copy(Color.FADED_OPACITY))) {
                            append("(${valueType.name.lowercase()})")
                        }
                    }
                }
            }

            @Composable
            private fun Table(concept: Concept) {
                Table.Layout(
                    items = propertiesOf(concept),
                    modifier = Modifier.fillMaxWidth().height(Table.ROW_HEIGHT * (propertiesOf(concept).size + 1)),
                    columns = listOf(
                        Table.Column(Label.PROPERTY, Alignment.CenterStart, size = Either.first(1f)) {
                            Form.Text(it.key, fontWeight = FontWeight.Bold)
                        },
                        Table.Column(Label.VALUE, Alignment.CenterStart, size = Either.first(2f)) {
                            Form.SelectableText(it.value, singleLine = true)
                        }
                    )
                )
            }
        }
    }

    @Composable
    override fun content(modifier: Modifier) {
        visualiser.Layout(modifier)
    }
}
