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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.vaticle.typedb.studio.state.connection.TransactionState
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Typography
import com.vaticle.typedb.studio.view.material.Form
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.Font
import org.jetbrains.skia.TextLine

class GraphArea(transactionState: TransactionState) {

    val interactions = Interactions(this)
    val graph = Graph(interactions)
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    val graphBuilder = GraphBuilder(graph, transactionState, coroutineScope)
    val viewport = Viewport(graph)
    val physicsRunner = PhysicsRunner(this)
    var theme: Color.GraphTheme? = null
    var typography: Typography.Theme? = null

    // TODO: this needs a better home
    val edgeLabelSizes: MutableMap<String, DpSize> = ConcurrentHashMap()

    companion object {
        val MIN_WIDTH = 120.dp
    }

    @Composable
    fun Layout() {
        val density = LocalDensity.current.density
        theme = Theme.graph
        typography = Theme.typography

        Box(
            Modifier.graphicsLayer(clip = true).background(Theme.graph.background)
                .onGloballyPositioned { onLayout(density, it) }
        ) { Graphics(graph.physics.iteration, viewport.density, viewport.physicalSize, viewport.scale) }

        LaunchedEffect(this) { physicsRunner.launch() }
        LaunchedEffect(this, viewport.scale, viewport.density) {
            interactions.hoveredVertexChecker.launch()
        }
        LaunchedEffect(this) { viewport.autoScaler.launch() }
    }

    @Composable
    @Suppress("UNUSED_PARAMETER")
    // TODO: we tried using Composables.key here, but it performs drastically worse (while zooming in/out) than
    //       this explicit Composable with unused parameters - investigate why
    fun Graphics(physicsIteration: Long, density: Float, size: DpSize, scale: Float) {
        // Take snapshots of vertices and edges so we can iterate them while the source collections are concurrently modified
        val edges = graph.edges.toList()
        val vertices = graph.vertices.filter { viewport.rectIsVisible(it.geometry.rect) }
        val typography = Theme.typography
        val vertexLabelPaint = Paint().apply { color = Theme.graph.vertexLabel }.asFrameworkPaint()
        Canvas(Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale)) {
            drawEdges(edges)
            drawVertices(vertices, vertexLabelPaint, typography)
        }
        edges.filter { it.label !in edgeLabelSizes }.forEach { EdgeLabelMeasurer(it) }
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

    private fun rendererContext(drawScope: DrawScope) = RendererContext(drawScope, theme!!, typography!!)

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
            edgeLabelSizes[edge.label]?.let { viewport.rectIsVisible(edge.geometry.labelRect(it, density)) } ?: false
        }
        val shouldDrawLabels = when {
            graph.physics.alpha > 0.5 -> edgesWithVisibleLabels.size < 50
            graph.physics.alpha > 0.05 -> edgesWithVisibleLabels.size < 100
            else -> edgesWithVisibleLabels.size < 500
        }
        return if (shouldDrawLabels) edgesWithVisibleLabels.toSet() else emptySet()
    }

    // TODO: get these metrics from EdgeRenderer instead? (via Skia's TextLine.width and TextLine.capHeight)
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

    private fun DrawScope.drawVertices(
        vertices: Collection<Vertex>, labelPaint: org.jetbrains.skia.Paint, typography: Typography.Theme
    ) {
        // Ensure smooth performance when zoomed out, and during initial explosion
        val shouldDrawLabels = viewport.scale > 0.2 && when {
            graph.physics.alpha > 0.5 -> vertices.size < 50
            graph.physics.alpha > 0.05 -> vertices.size < 100
            else -> vertices.size < 500
        }
        vertices.forEach { vertex ->
            drawVertexBackground(vertex)
            if (shouldDrawLabels) drawVertexLabel(vertex, labelPaint, typography)
        }
    }

    private fun DrawScope.drawVertexBackground(vertex: Vertex) {
        vertexBackgroundRenderer(vertex, this).draw()
    }

    private fun DrawScope.drawVertexLabel(
        vertex: Vertex, paint: org.jetbrains.skia.Paint, typography: Typography.Theme
    ) {
        drawIntoCanvas {
            drawVertexLabelLines(
                canvas = it,
                text = vertex.label,
                font = Font(typography.fixedWidthSkiaTypeface, typography.codeSizeMedium * density),
                center = (vertex.geometry.position - viewport.worldCoordinates) * density,
                maxWidth = vertex.geometry.labelMaxWidth * density,
                paint = paint
            )
        }
    }

    // TODO: this method is expensive for long labels
    private fun drawVertexLabelLines(
        canvas: Canvas, text: String, font: Font, center: Offset, maxWidth: Float, paint: org.jetbrains.skia.Paint
    ) {
        val line = TextLine.make(text, font)
        val lineBreakIndex = lineBreakIndex(line, maxWidth)
        if (lineBreakIndex == null) {
            canvas.nativeCanvas.drawTextLine(line, center.x - line.width / 2, center.y + line.capHeight / 2, paint)
        } else {
            val line1 = TextLine.make(text.substring(0 until lineBreakIndex), font)
            var line2Text = text.substring(lineBreakIndex)
            var line2 = TextLine.make(line2Text, font)
            val line2BreakIndex = lineBreakIndex(line2, maxWidth)
            if (line2BreakIndex != null) {
                line2Text = line2Text.substring(0 until line2BreakIndex)
                line2 = TextLine.make(line2Text, font)
            }
            canvas.nativeCanvas.drawTextLine(
                line1, center.x - line1.width / 2, center.y + line1.capHeight / 2 - line1.height / 2, paint
            )
            canvas.nativeCanvas.drawTextLine(
                line2, center.x - line2.width / 2, center.y + line2.capHeight / 2 + line2.height / 2, paint
            )
        }
    }

    private fun lineBreakIndex(textLine: TextLine, lineMaxWidth: Float): Int? {
        return textLine.positions
            .filterIndexed { idx, _ -> idx % 2 == 0 }
            .indexOfFirst { it > lineMaxWidth }
            .let { if (it == -1) null else it }
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
                                if (it is Vertex.Thing && it.thing.isInferred) graphArea.graphBuilder.explain(it)
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