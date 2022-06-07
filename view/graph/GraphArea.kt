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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.vaticle.typedb.studio.state.connection.TransactionState
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.material.Form
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope

class GraphArea(transactionState: TransactionState) {

    val interactions = Interactions(this)
    val graph = Graph(interactions)
    val coroutineScope = CoroutineScope(EmptyCoroutineContext)
    val graphBuilder = GraphBuilder(graph, transactionState, coroutineScope)
    // TODO: this needs a better home
    val edgeLabelSizes: MutableMap<String, DpSize> = ConcurrentHashMap()
    val viewport = Viewport(graph, edgeLabelSizes)
    val physicsRunner = PhysicsRunner(this)
    var theme: Color.GraphTheme? = null

    companion object {
        val MIN_WIDTH = 120.dp

        // TODO: this is duplicated in 3 places
        private const val BACKGROUND_ALPHA = .25f
    }

    @Composable
    fun Layout() {
        val density = LocalDensity.current.density
        theme = Theme.graph

        Box(
            Modifier.graphicsLayer(clip = true).background(Theme.graph.background)
                .onGloballyPositioned { onLayout(density, it) }
        ) { Graphics(graph.physics.iteration, viewport.density, viewport.physicalSize, viewport.scale) }

        LaunchedEffect(this) { physicsRunner.run() }
        LaunchedEffect(this, viewport.scale, viewport.density) {
            interactions.hoveredVertexChecker.poll()
        }
    }

    @Composable
    @Suppress("UNUSED_PARAMETER")
    // TODO: we tried using Composables.key here, but it performs drastically worse (while zooming in/out) than
    //       this explicit Composable with unused parameters - investigate why
    fun Graphics(physicsIteration: Long, density: Float, size: DpSize, scale: Float) {
        // Since edges is a List we need to synchronize on it. Additionally we keep EdgeLayer and VertexLayer
        // synchronized on the same object. Otherwise, the renderer may block waiting
        // to acquire a lock, and the vertex and edge drawing may go out of sync.
        synchronized(graph.edges) {
            Box(Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale)) {
                EdgeLayer()
                VertexLayer()
            }
        }
        PointerInput.Handler(this, Modifier.fillMaxSize().zIndex(100f))
    }

    private fun onLayout(density: Float, layout: LayoutCoordinates) {
        viewport.updatePhysicalDimensions(layout.size.toSize(), density)
        // TODO: this check exists because the first composition of GraphArea will have a width of MIN_WIDTH,
        //       before inflating to fill the max width, but there should be a more elegant solution than this.
        if (layout.size.width > MIN_WIDTH.value * density) {
            if (viewport.areInitialWorldCoordinatesSet.compareAndSet(false, true)) {
                viewport.alignWorldCenterWithPhysicalCenter()
            }
        }
    }

    private fun rendererContext(drawScope: DrawScope) = RendererContext(drawScope, theme!!)

    private fun vertexBackgroundRenderer(vertex: Vertex, drawScope: DrawScope): VertexBackgroundRenderer {
        return VertexBackgroundRenderer.of(vertex, this, rendererContext(drawScope))
    }

    private fun edgeRenderer(drawScope: DrawScope): EdgeRenderer {
        return EdgeRenderer(this, rendererContext(drawScope))
    }

    @Composable
    private fun EdgeLayer() {
        val detailedEdges = viewport.detailedEdges.let { if (it.size < 500 && viewport.scale > 0.2) it else emptySet() }
        val simpleEdges = graph.edges.filter { it !in detailedEdges }

        graph.edges.filter { it.label !in edgeLabelSizes }.forEach { EdgeLabelMeasurer(it) }

        Canvas(Modifier.fillMaxSize()) {
            edgeRenderer(this).draw(simpleEdges, false)
            edgeRenderer(this).draw(detailedEdges, true)
        }
        detailedEdges.forEach { EdgeLabel(it) }
    }

    @Composable
    private fun EdgeLabel(edge: Edge) {
        val size = edgeLabelSizes[edge.label] ?: return
        val density = LocalDensity.current.density
        val rect = edge.geometry.labelRect(size, density).translate(-viewport.worldCoordinates)
        val baseColor = if (edge is Edge.Inferrable && edge.isInferred) Theme.graph.inferred
        else Theme.graph.edgeLabel
        val alpha = with(interactions) { if (edge.isBackground) BACKGROUND_ALPHA else 1f }
        val color = baseColor.copy(alpha)

        Box(Modifier.offset(rect.left.dp, rect.top.dp).size(rect.width.dp, rect.height.dp), Alignment.Center) {
            Form.Text(
                value = edge.label,
                textStyle = Theme.typography.code1.copy(color = color, textAlign = TextAlign.Center),
                color = color, // TODO: remove this hack when Form.Text.textStyle.color is supported
            )
        }
    }

    @Composable
    private fun EdgeLabelMeasurer(edge: Edge) {
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
        val vertices = viewport.visibleVertices
        Canvas(Modifier.fillMaxSize()) { vertices.forEach { drawVertexBackground(it) } }
        if (viewport.scale > 0.2 && (vertices.size < 200 || graph.physics.alpha < 0.5)) {
            vertices.forEach { VertexLabel(it, it.geometry.position) }
        }
    }

    private fun DrawScope.drawVertexBackground(vertex: Vertex) {
        vertexBackgroundRenderer(vertex, this).draw()
    }

    @Composable
    @Suppress("UNUSED_PARAMETER")
    // TODO: I'm not really sure why we need 'position' here. Without it, the vertex label intermittently desyncs
    //       from the vertex's position, but it should be updating when physicsIteration does
    private fun VertexLabel(vertex: Vertex, position: Offset) {
        val r = vertex.geometry.rect
        val x = (r.left - viewport.worldCoordinates.x).dp
        val y = (r.top - viewport.worldCoordinates.y).dp
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
        fun Handler(graphArea: GraphArea, modifier: Modifier) {
            DragAndScroll(graphArea, modifier) {
                // Nested elements are required for drag and tap events to not conflict with each other
                TapAndHover(graphArea, modifier)
            }
        }

        @Composable
        fun DragAndScroll(graphArea: GraphArea, modifier: Modifier, content: @Composable () -> Unit) {
            val viewport = graphArea.viewport
            Box(
                modifier
                    .pointerInput(graphArea, viewport.density, viewport.scale) {
                        detectDragGestures(
                            onDragStart = { _ ->
                                graphArea.interactions.draggedVertex?.let {
                                    graphArea.graph.physics.drag.onDragStart(it)
                                }
                            },
                            onDragEnd = {
                                graphArea.graph.physics.drag.onDragEnd()
                                graphArea.interactions.draggedVertex = null
                            },
                            onDragCancel = {
                                graphArea.graph.physics.drag.onDragEnd()
                                graphArea.interactions.draggedVertex = null
                            }
                        ) /* onDrag = */ { _, dragAmount ->
                            val worldDragDistance = dragAmount / (viewport.scale * viewport.density)
                            val draggedVertex = graphArea.interactions.draggedVertex
                            if (draggedVertex != null) {
                                graphArea.graph.physics.drag.onDragMove(draggedVertex, worldDragDistance)
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
        fun TapAndHover(graphArea: GraphArea, modifier: Modifier) {
            Box(modifier
                .pointerMoveFilter(
                    onMove = { graphArea.interactions.pointerPosition = it; false },
                    onExit = { graphArea.interactions.pointerPosition = null; false }
                )
                .pointerInput(graphArea) {
                    detectTapGestures(
                        onPress = { point ->
                            graphArea.interactions.draggedVertex = graphArea.viewport.findVertexAt(point)
                            if (tryAwaitRelease()) graphArea.interactions.draggedVertex = null
                        },
                        onDoubleTap = { point ->
                            graphArea.viewport.findVertexAt(point)?.let {
                                // TODO: this should require SHIFT-doubleclick, not doubleclick
                                if (it is Vertex.Thing && it.thing.isInferred) graphArea.graphBuilder.explain(
                                    it
                                )
                            }
                        }
                    ) /* onTap = */ { point ->
                        graphArea.interactions.focusedVertex = graphArea.viewport.findVertexAt(point)
                    }
                }
            )
        }
    }
}