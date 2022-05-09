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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.vaticle.force.graph.force.CenterForce
import com.vaticle.force.graph.force.CollideForce
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.awt.Polygon
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.pow
import kotlin.math.sqrt

internal object GraphOutput : RunOutput() {

    val LOGGER = KotlinLogging.logger {}

    internal class State(val transaction: TypeDBTransaction, number: Int) : RunOutput.State() {

        override val name: String = "${Label.GRAPH} ($number)"
        val viewport = Viewport(this)
        val graph: Graph = emptyGraph()
        val forceSimulation = ForceSimulation(graph)
        val simulationRunner = SimulationRunner(this)
        var pointerPosition: Offset? by mutableStateOf(null)
        var hoveredVertex: Vertex? by mutableStateOf(null)
        val hoveredVertexPoller = HoveredVertexPoller(this)
        var theme: Color.GraphTheme? = null
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)

        fun output(conceptMap: ConceptMap) {
            conceptMap.map().entries.forEach { (varName: String, concept: Concept) ->
                when {
                    concept.isThing -> concept.asThing().let { thing ->
                        graph.thingVertices.computeIfAbsent(thing.iid) { _ ->
                            Vertex.Thing.of(thing).also { forceSimulation.placeVertex(it) }
                        }
                    }
                    concept.isThingType -> concept.asThingType().let { thingType ->
                        graph.typeVertices.computeIfAbsent(thingType.label.name()) { _ ->
                            Vertex.Type.of(thingType).also { forceSimulation.placeVertex(it) }
                        }
                    }
                    concept.isRoleType -> { /* do nothing */ }
                    else -> throw IllegalStateException("[$concept]'s encoding is not supported by GraphOutput")
                }
            }
        }

        private fun rendererContext(drawScope: DrawScope) = RendererContext(drawScope, theme!!)

        fun vertexBackgroundRenderer(vertex: Vertex, drawScope: DrawScope): VertexBackgroundRenderer {
            return VertexBackgroundRenderer.of(vertex, this, rendererContext(drawScope))
        }

        fun edgeRenderer(edge: Edge, drawScope: DrawScope): EdgeRenderer {
            return EdgeRenderer(edge, this, rendererContext(drawScope))
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

            val thingVertices: MutableMap<String, Vertex.Thing> = ConcurrentHashMap()
            val typeVertices: MutableMap<String, Vertex.Type> = ConcurrentHashMap()
            val vertices: List<Vertex> get() = thingVertices.values + typeVertices.values
            val edges: MutableList<Edge> = mutableListOf()
            // val explanations: MutableList<Explanation> = mutableListOf()

            fun isEmpty() = vertices.isEmpty()
            fun isNotEmpty() = vertices.isNotEmpty()

            companion object {
                internal fun emptyGraph() = Graph()
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

                class Relation(val relation: com.vaticle.typedb.client.api.concept.thing.Relation)
                    : Thing(relation) {

                    override val label = Label(relation.type.label.name(), Label.LengthLimits.RELATION)
                    override val geometry = Geometry.relation()
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
                }
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

        class EdgeCandidate {
            // TODO
        }

        class Edge(val source: Vertex, val target: Vertex) {
            // TODO
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

        class EdgeRenderer(private val edge: Edge, private val state: State, private val ctx: RendererContext) {

            private val density = state.viewport.density
        }

        data class RendererContext(val drawScope: DrawScope, val theme: Color.GraphTheme)

        class ForceSimulation(private val graph: Graph): BasicSimulation() {
            var frameID by mutableStateOf(0L)
            val physics = Physics(this)
            val isStable get() = alpha < alphaMin

            fun placeVertex(vertex: Vertex) {
                placeVertex(vertex.geometry)
            }

            override fun tick() {
                if (isStable) return
                physics.rebuild()
                super.tick()
                frameID++
            }

            class Physics(private val simulation: ForceSimulation) {

                fun rebuild() {
                    simulation.apply {
                        forces.clear()
                        localForces.clear()

                        // TODO: track changes to vertices + edges, rebuild forces only if there are changes
                        val vertices = graph.vertices.map { it.geometry }
                        val edges = graph.edges
                        forces.add(CenterForce(vertices, 0.0, 0.0))
                        forces.add(CollideForce(vertices, 80.0))
                        forces.add(
                            ManyBodyForce(
                                vertices, (-500.0 - vertices.size / 3) * (1 + edges.size / (vertices.size + 1))
                            )
                        )
//                        forces.add(LinkForce(vertexList, ))
                        forces.add(XForce(vertices, 0.0, 0.1))
                        forces.add(YForce(vertices, 0.0, 0.1))
                    }
                }
            }
        }

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
                        && !state.forceSimulation.isStable
            }

            private fun tickAsync(): Job {
                return state.coroutineScope.launch {
                    try {
                        state.forceSimulation.tick()
                    } catch (e: Exception) {
                        GlobalState.notification.systemError(LOGGER, e, Message.Visualiser.UNEXPECTED_ERROR)
                        state.forceSimulation.alpha = 0.0
                    }
                }
            }
        }

        class HoveredVertexPoller(private val state: State) {

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
        val vertices = state.graph.vertices

        Box(modifier.graphicsLayer(clip = true).onGloballyPositioned {
            state.viewport.density = density
            if (state.viewport.isLayoutInitialised.compareAndSet(false, true)) {
                state.viewport.size = it.size.toSize()
                state.viewport.position = -state.viewport.size.center / density
            }
        }) {
            key(state.forceSimulation.frameID) {
                Box(Modifier.graphicsLayer(scaleX = state.viewport.scale, scaleY = state.viewport.scale)) {
                    Canvas(modifier.fillMaxSize()) { vertices.forEach { drawVertexBackground(it, state) } }

                    if (vertices.size <= 1000) vertices.forEach { VertexLabel(it, state) }
                }
            }
            PointerInput.Handler(state, Modifier.fillMaxSize().zIndex(100f))
        }

        LaunchedEffect(Unit) { state.simulationRunner.run() }
        LaunchedEffect(Unit) { state.hoveredVertexPoller.poll() }
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
}
