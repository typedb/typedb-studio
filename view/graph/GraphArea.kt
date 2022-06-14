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
import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.TextDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.vaticle.typedb.studio.state.connection.TransactionState
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.material.Form
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.TextLine
import org.jetbrains.skia.Typeface

class GraphArea(transactionState: TransactionState) {

    val interactions = Interactions(this)
    val graph = Graph(interactions)
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    val graphBuilder = GraphBuilder(graph, transactionState, coroutineScope)
    val viewport = Viewport(graph)
    val physicsRunner = PhysicsRunner(this)
    var theme: Color.GraphTheme? = null

    // TODO: this needs a better home
    val edgeLabelSizes: MutableMap<String, DpSize> = ConcurrentHashMap()
    val vertexLabelSizes: MutableMap<String, IntSize> = ConcurrentHashMap()
    val vertexLabelBitmaps: MutableMap<String, ImageBitmap> = ConcurrentHashMap()

    val contextClassLoader = Thread.currentThread().contextClassLoader!!
    val resourceName = "resources/fonts/ubuntumono/UbuntuMono-Regular.ttf"
    val resource = contextClassLoader.getResourceAsStream(resourceName)
        ?: error("Can't load font from $resourceName")
    val bytes = resource.use { it.readAllBytes() }
    val typeface = Typeface.makeFromData(Data.makeFromBytes(bytes))
    var _font: Font? = null
    val font get() = if (_font != null) _font else { _font = Font(typeface, 13f * 2); _font }

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
        // Take snapshots of vertices and edges so we can iterate them while the source collections are concurrently modified
        val edges = graph.edges.toList()
        val vertices = graph.vertices.filter { viewport.rectIsVisible(it.geometry.rect) }
        Box(Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale)) {
            EdgeLayer(edges)
            VertexLayer(vertices)
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
    private fun EdgeLayer(edges: Collection<Edge>) {
        // Because DrawScope.drawPoints() is so cheap, we can draw all edges as plain edges by default,
        // adding detail if they meet certain criteria.
        val detailedEdgeSet = detailedEdgeSet(edges, LocalDensity.current.density)
        val simpleEdges = edges.filter { it !in detailedEdgeSet }

        edges.filter { it.label !in edgeLabelSizes }.forEach { EdgeLabelMeasurer(it) }

        Canvas(Modifier.fillMaxSize()) {
            edgeRenderer(this).draw(simpleEdges, false)
            edgeRenderer(this).draw(detailedEdgeSet, true)
        }
        detailedEdgeSet.forEach { EdgeLabel(it) }
    }

    private fun detailedEdgeSet(edges: Collection<Edge>, density: Float): Set<Edge> {
        // Ensure smooth performance when zoomed out
        if (edges.size > 500 && viewport.scale < 0.2) return emptySet()
        return edges.filter { edge ->
            // Only draw visible labels (and only draw curves when label is visible, as curves are expensive)
            edgeLabelSizes[edge.label]?.let { viewport.rectIsVisible(edge.geometry.labelRect(it, density)) } ?: false
        }.let {
            // Ensure smooth performance during initial explosion
            if (it.size < 200 || graph.physics.alpha < 0.5) it.toSet() else emptySet()
        }
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
    private fun VertexLabelMeasurer(vertex: Vertex) {
        Form.Text(
            value = vertex.label.text, textStyle = Theme.typography.code1,
            modifier = Modifier.graphicsLayer(alpha = 0f).onSizeChanged { vertexLabelSizes[vertex.label.text] = it }
        )
    }

    @OptIn(InternalFoundationTextApi::class)
    @Composable
    private fun VertexLayer(vertices: Collection<Vertex>) {
        val localDensity = LocalDensity.current
        val fontLoader = LocalFontLoader.current
        Canvas(Modifier.fillMaxSize()) {
            vertices.forEach { vertex ->
                drawVertexBackground(vertex)
                drawVertexLabel(vertex)
//                when (val img = vertexLabelBitmaps[vertex.label.text]) {
//                    null -> vertexLabelSizes[vertex.label.text]?.let {
//                        (this as Canvas).nativeCanvas.drawTextLine(
//                            TextLine.Companion.make(
//                                vertex.label.text, Font("resources/fonts/ubuntumono/UbuntuMono-Regular.ttf")
//                            )
//                        )
//                        vertexLabelBitmaps[vertex.label.text] = ImageBitmap(it.width, it.height, ImageBitmapConfig.Gpu)
//                            .apply {
//
//                            }
//                    }
//                    else -> drawImage(img, vertex.geometry.position - Offset(img.width / 2f, img.height / 2f))
//                }
//                drawIntoCanvas { canvas ->
//                    canvas.nativeCanvas.drawTextLine(TextLine.make(vertex.label.text, font))
//                }
            }
        }
        // Ensure smooth performance when zoomed out, and during initial explosion
//        if (viewport.scale > 0.2 && (vertices.size < 200 || graph.physics.alpha < 0.5)) {
//        vertices.filter { it.label.text !in vertexLabelSizes.keys }.forEach { VertexLabelMeasurer(it) }
//        }
    }

    private fun DrawScope.drawVertexBackground(vertex: Vertex) {
        vertexBackgroundRenderer(vertex, this).draw()
    }

    private fun DrawScope.drawVertexLabel(vertex: Vertex) {
        val center = vertex.geometry.position
        val x = (center.x - viewport.worldCoordinates.x) * density
        val y = (center.y - viewport.worldCoordinates.y) * density
//        val color = Theme.graph.vertexLabel

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawTextLine(TextLine.make(vertex.label.text, font), x, y, Paint())
        }
    }

    @Composable
    private fun VertexLabel(vertex: Vertex) {
        val r = vertex.geometry.rect
        val x = (r.left - viewport.worldCoordinates.x).dp
        val y = (r.top - viewport.worldCoordinates.y).dp
        val color = Theme.graph.vertexLabel

        Box(Modifier.offset(x, y).size(r.width.dp, r.height.dp), Alignment.Center) {
            Form.Text(vertex.label.text, textStyle = Theme.typography.code1, color = color, align = TextAlign.Center)
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