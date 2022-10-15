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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.common.theme.Typography
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.connection.TransactionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GraphArea(transactionState: TransactionState) {

    val interactions = Interactions(this)
    val graph = Graph(interactions)
    val coroutines = CoroutineScope(Dispatchers.Default)
    val graphBuilder = GraphBuilder(graph, transactionState, coroutines)
    val viewport = Viewport(graph)
    var typography: Typography.Theme? = null
    private val physicsRunner = PhysicsRunner(this)
    private val graphTheme = StudioState.preference.graphTheme
    internal val textRenderer = TextRenderer(viewport)

    companion object {
        val MIN_WIDTH = 120.dp
    }

    @Composable
    fun Layout() {
        val density = LocalDensity.current.density
        typography = Theme.typography

        Box(
            Modifier.graphicsLayer(clip = true).background(graphTheme.background)
                .onGloballyPositioned { onLayout(density, it) }
        ) { Graphics(graph.physics.iteration, viewport.density, viewport.physicalSize, viewport.scale) }

        LaunchedEffect(this) { physicsRunner.launch() }
        LaunchedEffect(this, viewport.scale, viewport.density) {
            interactions.hoveredVertexChecker.launch()
        }
        LaunchedEffect(this) {
            VertexExpandAnimator(graphArea = this@GraphArea, coroutines = this).launch()
        }
        LaunchedEffect(this) { viewport.autoScaler.launch() }
    }

    @Composable
    @Suppress("UNUSED_PARAMETER")
    // TODO: we tried using Composables.key here, but it performs drastically worse (while zooming in/out) than
    //       this explicit Composable with unused parameters - investigate why
    fun Graphics(physicsIteration: Long, density: Float, size: DpSize, scale: Float) {
        // Take snapshots of vertices and edges so that we can iterate them while the source collections are
        // concurrently modified.
        val edges = graph.edges.toList()
        val vertices = graph.vertices.filter { it.readyToCompose && viewport.rectIsVisible(it.geometry.rect) }
        // Since vertices contain MutableStates and are created on a different thread, we need to ensure their lifetime
        // has been at least one composition cycle before trying to read those states.
        graph.vertices.filter { !it.readyToCompose }.forEach { it.readyToCompose = true }
        val typography = Theme.typography
        val vertexLabelColor = graphTheme.vertexLabel
        Canvas(Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale)) {
            drawEdges(edges)
            drawVertices(vertices, vertexLabelColor, typography)
        }
        edges.filter { it.label !in textRenderer.edgeLabelSizes }.forEach { textRenderer.EdgeLabelMeasurer(it) }
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

    private fun rendererContext(drawScope: DrawScope) = RendererContext(drawScope, graphTheme, typography!!)

    private fun vertexBackgroundRenderer(vertex: Vertex, drawScope: DrawScope): VertexBackgroundRenderer {
        return VertexBackgroundRenderer.of(vertex, this, rendererContext(drawScope))
    }

    private fun edgeRenderer(drawScope: DrawScope): EdgeRenderer {
        return EdgeRenderer(this, rendererContext(drawScope))
    }

    private fun DrawScope.drawEdges(edges: Collection<Edge>) {
        // Because DrawScope.drawPoints() is so cheap, we can draw all edges as plain edges by default,
        // adding detail if they meet certain criteria.
        val detailedEdgeSet = detailedEdgeSet(edges, viewport.density)
        val simpleEdges = edges.filter { it !in detailedEdgeSet }

        edgeRenderer(this).draw(simpleEdges, false)
        edgeRenderer(this).draw(detailedEdgeSet, true)
    }

    // TODO: this should now be movable into EdgeRenderer
    private fun detailedEdgeSet(edges: Collection<Edge>, density: Float): Set<Edge> {
        // Ensure smooth performance when zoomed out, and during initial explosion
        if (edges.size > 500 && viewport.scale < 0.2) return emptySet()
        val edgesWithVisibleLabels = edges.filter { edge ->
            // Only draw visible labels (and only draw curves when label is visible, as curves are expensive)
            textRenderer.edgeLabelSizes[edge.label]
                ?.let { viewport.rectIsVisible(edge.geometry.labelRect(it, density)) } ?: false
        }
        val shouldDrawLabels = when {
            graph.physics.alpha > 0.5 -> edgesWithVisibleLabels.size < 50
            graph.physics.alpha > 0.05 -> edgesWithVisibleLabels.size < 100
            else -> edgesWithVisibleLabels.size < 500
        }
        return if (shouldDrawLabels) edgesWithVisibleLabels.toSet() else emptySet()
    }

    private fun DrawScope.drawVertices(
        vertices: Collection<Vertex>, labelColor: androidx.compose.ui.graphics.Color, typography: Typography.Theme
    ) {
        // Ensure smooth performance when zoomed out, and during initial explosion
        val drawLabels = viewport.scale > 0.2 && when {
            graph.physics.alpha > 0.5 -> vertices.size < 50
            graph.physics.alpha > 0.05 -> vertices.size < 100
            else -> vertices.size < 500
        }
        val drawVertexFn = { vertex: Vertex ->
            drawVertexBackground(vertex)
            if (drawLabels) with(textRenderer) { drawVertexLabel(vertex, labelColor, typography) }
        }
        // Draw strongly focused vertices over less focused ones
        sortByZIndex(vertices).forEach { vertex ->
            if (vertex.geometry.isVisiblyCollapsed) {
                drawVertexFn(vertex)
            } else {
                withTransform({
                    scale(vertex.geometry.scale, pivot = with(viewport) { vertex.geometry.rect.center.toViewport() })
                }) {
                    drawVertexFn(vertex)
                }
            }
        }
    }

    private fun vertexZIndex(vertex: Vertex) = when (vertex) {
        interactions.focusedVertex -> 2
        interactions.hoveredVertex -> 1
        else -> 0
    }

    private fun sortByZIndex(vertices: Collection<Vertex>): List<Vertex> {
        // O(n) sort because the number of possible z-indices is small
        return vertices.groupBy { vertexZIndex(it) }.toSortedMap().flatMap { it.value }
    }

    private fun DrawScope.drawVertexBackground(vertex: Vertex) {
        vertexBackgroundRenderer(vertex, this).draw()
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
                        viewport.wasManuallyRescaled = true
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
                    // TODO: this is not triggered reliably when the pointer leaves the window bounds
                    onExit = { graphArea.interactions.pointerPosition = null; false }
                )
                .pointerInput(graphArea) {
                    detectTapGestures(
                        onPress = { point ->
                            graphArea.interactions.draggedVertex =
                                graphArea.viewport.findVertexAt(point, graphArea.interactions)
                            if (tryAwaitRelease()) graphArea.interactions.draggedVertex = null
                        },
                        onDoubleTap = { point ->
                            graphArea.viewport.findVertexAt(point, graphArea.interactions)?.let {
                                // TODO: this should require SHIFT-doubleclick, not doubleclick
                                if (it is Vertex.Thing && it.thing.isInferred) graphArea.graphBuilder.explain(it)
                            }
                        }
                    ) /* onTap = */ { point ->
                        graphArea.interactions.focusedVertex =
                            graphArea.viewport.findVertexAt(point, graphArea.interactions)
                    }
                }
            )
        }
    }

    class VertexExpandAnimator(private val graphArea: GraphArea, private val coroutines: CoroutineScope)
        : BackgroundTask(runIntervalMs = 17) {

        private val interactions get() = graphArea.interactions

        override fun run() {
            val verticesToExpand = listOfNotNull(interactions.hoveredVertex, interactions.focusedVertex)
            graphArea.graph.vertices.forEach {
                if (it.geometry.isExpanded != it in verticesToExpand) {
                    it.geometry.isExpanded = !it.geometry.isExpanded
                    coroutines.launch { it.geometry.animateExpandOrCollapse() }
                }
            }
        }
    }
}