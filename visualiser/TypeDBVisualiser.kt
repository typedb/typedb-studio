package com.vaticle.typedb.studio.visualiser

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.VisualiserTheme
import com.vaticle.typedb.studio.data.VertexEncoding
import java.awt.Polygon
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TypeDBVisualiser(modifier: Modifier, vertices: List<VertexState>, edges: List<EdgeState>,
                     hyperedges: List<HyperedgeState>, vertexExplanations: List<VertexExplanationState>,
                     theme: VisualiserTheme, metrics: SimulationMetrics, onZoom: (scale: Float) -> Unit,
                     selectedVertex: VertexState?, onSelectVertex: (vertex: VertexState?) -> Unit,
                     selectedVertexNetwork: List<VertexState>, onVertexDragStart: (vertex: VertexState) -> Unit,
                     onVertexDragMove: (vertex: VertexState, position: Offset) -> Unit, onVertexDragEnd: () -> Unit,
                     explain: (vertex: VertexState) -> Unit) {

    var scale by remember { mutableStateOf(1F) }
    var worldOffset by remember { mutableStateOf(Offset.Zero) }
    var pointerPosition: Offset? by remember { mutableStateOf(null) }
    var hoveredVertexLastCheckDoneTimeNanos: Long by remember { mutableStateOf(0) }
    var viewportSize by remember { mutableStateOf(Size.Zero) }
    val highlightedExplanationIDs: SnapshotStateList<Int> = remember { mutableStateListOf() }
    val devicePixelRatio = metrics.devicePixelRatio
    val verticesByID = vertices.associateBy { it.id }
    var hoveredVertex: VertexState? by remember { mutableStateOf(null) }
    var draggedVertex: VertexState? by remember { mutableStateOf(null) }

    fun DrawScope.drawVertex(v: VertexState) {
        val viewportOffset: Offset = -worldOffset
        val vertexBaseColor = requireNotNull(theme.vertex[v.encoding])
        val focused = hoveredVertex === v || draggedVertex === v
        val faded = selectedVertex != null && v !in selectedVertexNetwork
        val alpha = when {
            focused && faded -> .175f
            faded -> .25f
            focused -> .675f
            else -> null
        }
        val vertexColor = if (alpha != null) vertexBaseColor.copy(alpha) else vertexBaseColor
        val position = (v.position - viewportOffset) * devicePixelRatio
        val width = v.width * devicePixelRatio
        val height = v.height * devicePixelRatio
        val cornerRadius = CornerRadius(5F * devicePixelRatio)
        val highlightWidth = devicePixelRatio
        val highlightBaseColor: Color? = when {
            vertexExplanations.firstOrNull { it.vertexID == v.id && it.explanationID in highlightedExplanationIDs } != null -> theme.explanation
            v.inferred -> theme.inferred
            else -> null
        }
        val highlightColor: Color? = if (alpha != null) highlightBaseColor?.copy(alpha) else highlightBaseColor

        when (v.encoding) {

            VertexEncoding.ENTITY_TYPE, VertexEncoding.THING_TYPE, VertexEncoding.ENTITY -> {
                if (highlightColor != null) {
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(position.x - width / 2 - highlightWidth, position.y - height / 2 - highlightWidth),
                        size = Size(width + highlightWidth * 2, height + highlightWidth * 2), cornerRadius = cornerRadius)
                }

                drawRoundRect(
                    color = vertexColor,
                    topLeft = Offset(position.x - width / 2, position.y - height / 2),
                    size = Size(width, height), cornerRadius = cornerRadius)
            }

            VertexEncoding.RELATION_TYPE, VertexEncoding.RELATION -> {
                // We start with a square of width n and transform it into a rhombus
                val n: Float = (height / sqrt(2.0)).toFloat()
                withTransform({
                    scale(scaleX = v.width / v.height, scaleY = 1F, pivot = position)
                    rotate(degrees = 45F, pivot = position)
                }) {
                    if (highlightColor != null) {
                        drawRoundRect(color = highlightColor,
                            topLeft = Offset(position.x - n / 2 - highlightWidth, position.y - n / 2 - highlightWidth),
                            size = Size(n + highlightWidth * 2, n + highlightWidth * 2), cornerRadius = cornerRadius)
                    }

                    drawRoundRect(color = vertexColor,
                        topLeft = Offset(position.x - n / 2, position.y - n / 2),
                        size = Size(n, n), cornerRadius = cornerRadius)
                }
            }

            VertexEncoding.ATTRIBUTE_TYPE, VertexEncoding.ATTRIBUTE -> {
                if (highlightColor != null) {
                    drawOval(color = highlightColor,
                        topLeft = Offset(position.x - width / 2 - highlightWidth, position.y - height / 2 - highlightWidth),
                        size = Size(width + highlightWidth * 2, height + highlightWidth * 2))
                }

                drawOval(color = vertexColor,
                    topLeft = Offset(position.x - width / 2, position.y - height / 2),
                    size = Size(width, height))
            }
        }
    }

    @Composable
    fun drawVertexLabel(v: VertexState) {
        val viewportOffset: Offset = -worldOffset
        val r = v.rect
        val x = (r.left - viewportOffset.x).dp
        val y = (r.top - viewportOffset.y).dp
        val color = theme.vertexLabel

        Column(modifier = Modifier.offset(x, y).width(v.width.dp).height(v.height.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = v.shortLabel, style = StudioTheme.typography.code1, color = color, textAlign = TextAlign.Center)
        }
    }

    fun DrawScope.drawSolidEdge(edge: EdgeState) {
        val viewportOffset: Offset = -worldOffset
        val sourceVertex = requireNotNull(verticesByID[edge.sourceID])
        val targetVertex = requireNotNull(verticesByID[edge.targetID])
        val lineSource = sourceVertex.position
        val lineTarget = targetVertex.position
        val faded = selectedVertex != null && selectedVertex !in listOf(sourceVertex, targetVertex)
        val baseColor = if (edge.inferred) theme.inferred else theme.edge
        val color = if (faded) baseColor.copy(alpha = 0.25f) else baseColor

        val hyperedge = hyperedges.find { it.edgeID == edge.id }
        val arc = if (hyperedge == null) null else arcThroughPoints(lineSource, hyperedge.position, lineTarget)

        when {
            arc == null -> {
                drawLine(start = (lineSource - viewportOffset) * devicePixelRatio,
                    end = (lineTarget - viewportOffset) * devicePixelRatio, color = color, strokeWidth = devicePixelRatio)
            }
            abs(arc.sweepAngle) < 270 -> {
                drawArc(color = color, startAngle = arc.startAngle, sweepAngle = arc.sweepAngle, useCenter = false,
                    topLeft = (arc.topLeft - viewportOffset) * devicePixelRatio, size = arc.size * devicePixelRatio,
                    style = Stroke(width = devicePixelRatio))
            }
            else -> {
                drawLine(start = (lineSource - viewportOffset) * devicePixelRatio,
                    end = (hyperedge!!.position - viewportOffset) * devicePixelRatio, color = color, strokeWidth = devicePixelRatio)
                drawLine(start = (hyperedge.position - viewportOffset) * devicePixelRatio,
                    end = (lineTarget - viewportOffset) * devicePixelRatio, color = color, strokeWidth = devicePixelRatio)
            }
        }

        val arrow = arrowhead(from = (lineSource - viewportOffset) * devicePixelRatio, to = (lineTarget - viewportOffset) * devicePixelRatio,
            arrowLength = 6F * devicePixelRatio, arrowWidth = 3F * devicePixelRatio)
        if (arrow != null) drawPath(path = arrow, color = color)
    }

    fun DrawScope.drawArrowSegments(lineSource: Offset, labelRect: Rect, lineTarget: Offset, color: Color) {
        val viewportOffset: Offset = -worldOffset
        val linePart1Target = rectIncomingLineIntersect(sourcePoint = lineSource, rect = labelRect)
        if (linePart1Target != null) {
            drawLine(
                start = (lineSource - viewportOffset) * devicePixelRatio, end = (linePart1Target - viewportOffset) * devicePixelRatio,
                color = color, strokeWidth = devicePixelRatio
            )
        }
        val linePart2Source = rectIncomingLineIntersect(sourcePoint = lineTarget, rect = labelRect)
        if (linePart2Source != null) {
            drawLine(
                start = (linePart2Source - viewportOffset) * devicePixelRatio, end = (lineTarget - viewportOffset) * devicePixelRatio,
                color = color, strokeWidth = devicePixelRatio
            )

            val arrow = arrowhead(
                from = (linePart2Source - viewportOffset) * devicePixelRatio, to = (lineTarget - viewportOffset) * devicePixelRatio,
                arrowLength = 6F * devicePixelRatio, arrowWidth = 3F * devicePixelRatio
            )
            if (arrow != null) drawPath(path = arrow, color = color)
        }
    }

    fun DrawScope.drawEdgeSegments(edge: EdgeState) {
        // match $x sub tracker-tree; $x relates $y; $z plays $y;
        val viewportOffset: Offset = -worldOffset
        val sourceVertex = requireNotNull(verticesByID[edge.sourceID])
        val targetVertex = requireNotNull(verticesByID[edge.targetID])
        val faded = selectedVertex != null && selectedVertex !in listOf(sourceVertex, targetVertex)
        val baseColor = if (edge.inferred) theme.inferred else theme.edge
        val color = if (faded) baseColor.copy(alpha = 0.25f) else baseColor

        val hyperedge = hyperedges.find { it.edgeID == edge.id }
        if (hyperedge == null) {
            val lineSource = edgeEndpoint(targetVertex.position, sourceVertex)
            val lineTarget = edgeEndpoint(sourceVertex.position, targetVertex)
            if (lineSource == null || lineTarget == null) return
            val m: Offset = midpoint(edge.sourcePosition, edge.targetPosition)
            // TODO: This Size is an approximation - a Compose equivalent of PixiJS TextMetrics would be more robust
            val labelRect = Rect(
                Offset(m.x - edge.label.length * 4 - 2, m.y - 7 - 2),
                Size(edge.label.length * 8F + 4, 14F + 4))
            drawArrowSegments(lineSource, labelRect, lineTarget, color)
        } else /* if (hyperedge != null) */ {
            val fullArc = arcThroughPoints(sourceVertex.position, hyperedge.position, targetVertex.position)
            val labelRect = Rect(
                Offset(hyperedge.position.x - edge.label.length * 4 - 2, hyperedge.position.y - 7 - 2),
                Size(edge.label.length * 8F + 4, 14F + 4))
            if (fullArc != null) {
//                for (angle in hyperedgeEndAngles(fullArc, targetVertex)) {
//                    val coords = fullArc.center + Offset(cos(angle * PI / 180).toFloat() * fullArc.size.width / 2, sin(angle * PI / 180).toFloat() * fullArc.size.height / 2)
//                    drawCircle(color = Color.Red, radius = 4f * devicePixelRatio, center = (coords - viewportOffset) * devicePixelRatio, style = Stroke(width = 2f * devicePixelRatio))
//                }
                val arcStartAngle = hyperedgeEndAngle(fullArc, sourceVertex)
                val arcEndAngle = hyperedgeEndAngle(fullArc, targetVertex)
                if (arcStartAngle == null) println("drawEdgeSegments: arcStartAngle is NULL for edge with label '${edge.label}'")
                if (arcEndAngle == null) println("drawEdgeSegments: arcEndAngle is NULL for edge with label '${edge.label}'")
                if (arcStartAngle == null || arcEndAngle == null) return
                val labelAngle = (atan2(y = hyperedge.position.y - fullArc.center.y, x = hyperedge.position.x - fullArc.center.x) * 180 / PI)
                    .toFloat().normalisedAngle()

                val sweepAngle1Unclipped = sweepAngle(from = fullArc.startAngle, to = labelAngle, direction = fullArc.direction)
                val arcPart1Unclipped = Arc(fullArc.topLeft, fullArc.size, fullArc.startAngle, sweepAngle1Unclipped)
                val arcPart1LabelIntersectAngles = rectArcIntersectAngles(arcPart1Unclipped, labelRect)
                if (arcPart1LabelIntersectAngles.isNotEmpty()) {
                    // We expect a single point of intersection
                    val arcPart1EndAngle = arcPart1LabelIntersectAngles[0]
                    val sweepAngle1 = sweepAngle(from = arcStartAngle, to = arcPart1EndAngle, direction = fullArc.direction)
                    drawArc(color = color, startAngle = arcStartAngle, sweepAngle = sweepAngle1, useCenter = false,
                        topLeft = (fullArc.topLeft - viewportOffset) * devicePixelRatio, size = fullArc.size * devicePixelRatio,
                        style = Stroke(width = devicePixelRatio))
                }

                val sweepAngle2Unclipped = sweepAngle(from = labelAngle, to = fullArc.endAngle, direction = fullArc.direction)
                val arcPart2Unclipped = Arc(fullArc.topLeft, fullArc.size, labelAngle, sweepAngle2Unclipped)
                val arcPart2LabelIntersectAngles = rectArcIntersectAngles(arcPart2Unclipped, labelRect)
                if (arcPart2LabelIntersectAngles.isNotEmpty()) {
                    val arcPart2StartAngle = arcPart2LabelIntersectAngles[0]
                    val sweepAngle2 = sweepAngle(from = arcPart2StartAngle, to = arcEndAngle, direction = fullArc.direction)
                    drawArc(color = color, startAngle = arcPart2StartAngle, sweepAngle = sweepAngle2, useCenter = false,
                        topLeft = (fullArc.topLeft - viewportOffset) * devicePixelRatio, size = fullArc.size * devicePixelRatio,
                        style = Stroke(width = devicePixelRatio))
                }

                // TODO: draw arrowhead parallel to arcEndAngle
            } else /* if (arc == null) */ {
                // Typically this means the 3 points are almost collinear, so we fall back to line segments
                val lineSource = edgeEndpoint(labelRect.center, sourceVertex)
                val lineTarget = edgeEndpoint(labelRect.center, targetVertex)
                if (lineSource == null || lineTarget == null) return
                drawArrowSegments(lineSource, labelRect, lineTarget, color)
            }
        }
    }

    @Composable
    fun drawEdgeLabel(edge: EdgeState) {
        val viewportOffset: Offset = -worldOffset
        val m: Offset = when (val hyperedge = hyperedges.find { it.edgeID == edge.id }) {
            null -> midpoint(edge.sourcePosition, edge.targetPosition) - viewportOffset
            else -> hyperedge.position - viewportOffset
        }
        val rect = Rect(Offset(m.x - edge.label.length * 4, m.y - 7), Size(edge.label.length * 8F, 14F))
        val faded = selectedVertex != null && selectedVertex.id !in listOf(edge.sourceID, edge.targetID)
        val baseColor = if (edge.inferred) theme.inferred else theme.edge
        val color = if (faded) baseColor.copy(alpha = 0.25f) else baseColor
        Column(
            modifier = Modifier.offset(rect.left.dp, rect.top.dp).width(rect.width.dp).height(rect.height.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = edge.label, style = StudioTheme.typography.code1.copy(color = color, textAlign = TextAlign.Center))
        }
    }

    // Metrics are recalculated on each query run
    LaunchedEffect(metrics.id) {
        worldOffset = metrics.worldOffset
    }

    Box(modifier = modifier
        .graphicsLayer(clip = true)
        .onGloballyPositioned { coordinates ->
            viewportSize = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
//            canvasPositionOnScreen = coordinates.localToWindow(Offset.Zero) / devicePixelRatio + Offset(window.x.toFloat(), window.y + titleBarHeight)
//            println("coordinatesLocal=${coordinates.localToWindow(Offset.Zero)},windowPosition=${window.location},canvasPositionOnScreen=$canvasPositionOnScreen")
        }
        .background(theme.background)) {

        Box(modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale)) {
            // TODO: don't render vertices or edges that are fully outside the viewport
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (edges.size <= 1000 && scale > 0.2) edges.forEach { drawEdgeSegments(it) }
                else edges.forEach { drawSolidEdge(it) }
            }

            // TODO: this condition is supposed to be a || but without out-of-viewport detection the performance would degrade unacceptably
            if (edges.size <= 1000 && scale > 0.2) edges.forEach { drawEdgeLabel(it) }

            Canvas(modifier = Modifier.fillMaxSize()) {
                vertices.forEach { drawVertex(it) }
            }

            if (vertices.size <= 1000 && scale > 0.2) vertices.forEach { drawVertexLabel(it) }
        }

        Box(modifier = Modifier.fillMaxSize().zIndex(100F)
            .pointerInput(devicePixelRatio) {
                detectDragGestures(
                    onDragStart = { _ ->
                        draggedVertex?.let { onVertexDragStart(it) }
//                        val closestVertices = getClosestVertices(dragOffset, vertices, 10, worldOffset, scale, devicePixelRatio)
//                        println(closestVertices)
                    },
                    onDragEnd = {
                        if (draggedVertex != null) onVertexDragEnd()
                        draggedVertex = null
                    },
                    onDragCancel = {
                        if (draggedVertex != null) onVertexDragEnd()
                        draggedVertex = null
                    }
                ) /* onDrag = */ { _, dragAmount: Offset ->
                    val worldDragDistance = dragAmount / (scale * devicePixelRatio)
                    val vertex = draggedVertex
                    if (vertex != null) {
                        vertex.position += worldDragDistance
                        onVertexDragMove(vertex, vertex.position)
                    }
                    else worldOffset += worldDragDistance
                }
            }
            .scrollable(orientation = Orientation.Vertical, state = rememberScrollableState { delta ->
                val zoomFactor = 1 + (delta * 0.0006F / devicePixelRatio)
                val newViewportScale = scale * zoomFactor
                // TODO: make the transform origin be where the mouse pointer is :-(
//                val pointerLocationOnScreen: Point = MouseInfo.getPointerInfo().location
//                val pointer = Offset(pointerLocationOnScreen.x.toFloat(), pointerLocationOnScreen.y.toFloat()) - canvasPositionOnScreen
//                val transformOrigin = Offset(canvasSize.width - pointer.x, canvasSize.height - pointer.y)
//                val transformOrigin = (pointer - Offset(canvasSize.width / 2, canvasSize.height / 2)) / viewportScale

//                println("viewportScale=$viewportScale,viewportOffset=$viewportOffset,cursorPosition=${cursorPosition},canvasCenter=${canvasSize.center}")
//                val newX = transformOrigin.x * (zoomFactor - 1) + (zoomFactor * viewportOffset.x)
//                val newY = transformOrigin.y * (zoomFactor - 1) + (zoomFactor * viewportOffset.y)
//                val newX = transformOrigin.x - zoomFactor*(transformOrigin.x - viewportOffset.x)
//                val newY = transformOrigin.y - zoomFactor*(transformOrigin.y - viewportOffset.y)
//                val newX = ((2 * viewportOffset.x) / zoomFactor) * ((viewportOffset.x - pointer.x) / (canvasSize.width / zoomFactor))
//                val newY = ((2 * viewportOffset.y) / zoomFactor) * ((viewportOffset.y - pointer.y) / (canvasSize.height / zoomFactor))
//                println("newViewportScale=$newViewportScale")
//                val newX = pointer.x + (viewportOffset.x - pointer.x) / zoomFactor
//                val newY = pointer.y + (viewportOffset.y - pointer.y) / zoomFactor
//                val newX = transformOrigin.x + (transformOrigin.x - viewportOffset.x) / zoomFactor
//                val newY = transformOrigin.y + (transformOrigin.y - viewportOffset.y) / zoomFactor
//                val newX = viewportOffset.x + (transformOrigin.x - viewportOffset.x)*((zoomFactor - 1) / zoomFactor)
//                val newY = viewportOffset.y + (transformOrigin.y - viewportOffset.y)*((zoomFactor - 1) / zoomFactor)
//                val newX = viewportOffset.x - transformOrigin.x * (1 / zoomFactor - 1)
//                val newY = viewportOffset.y - transformOrigin.y * (1 / zoomFactor - 1)
//                val newX = viewportOffset.x * (1 + ((2 * transformOrigin.x) / canvasSize.width) * (1 / zoomFactor - 1))
//                val newY = viewportOffset.y * (1 + ((2 * transformOrigin.y) / canvasSize.width) * (1 / zoomFactor - 1))
//                viewportOffset = Offset(newX, newY)
                scale = newViewportScale
                onZoom(newViewportScale)
                delta
            })) {

            // This nesting is required to prevent the drag event conflicting with the tap event
            Box(modifier = modifier.fillMaxSize()
                .pointerMoveFilter(
                    onMove = {
                        pointerPosition = it
                        return@pointerMoveFilter false
                    },
                    onExit = {
                        pointerPosition = null
                        return@pointerMoveFilter false
                    }
                )
                .pointerInput(devicePixelRatio) {
                    detectTapGestures(
                        onPress = { viewportPoint: Offset ->
                            val worldPoint = toWorldPoint(viewportSize, viewportPoint, worldOffset, scale, devicePixelRatio)
                            val closestVertices = getClosestVertices(worldPoint, vertices, resultSizeLimit = 10)
                            draggedVertex = closestVertices.find { it.intersects(worldPoint) }
                            draggedVertex?.let { println("pressed vertex: $it") }
                            val cancelled = tryAwaitRelease()
                            if (cancelled) draggedVertex = null
                        },
                        onDoubleTap = { viewportPoint: Offset ->
                            val worldPoint = toWorldPoint(viewportSize, viewportPoint, worldOffset, scale, devicePixelRatio)
                            val closestVertices = getClosestVertices(worldPoint, vertices, resultSizeLimit = 10)
                            val tappedVertex = closestVertices.find { it.intersects(worldPoint) }
                            tappedVertex?.let {
                                // TODO: this should require SHIFT-doubleclick, not doubleclick
//                                println("double-tapped vertex: $it")
                                if (it.inferred) explain(it)
                            }
                        }
                    ) /* onTap = */ { viewportPoint: Offset ->
                        val worldPoint = toWorldPoint(viewportSize, viewportPoint, worldOffset, scale, devicePixelRatio)
                        val closestVertices = getClosestVertices(worldPoint, vertices, resultSizeLimit = 10)
                        val tappedVertex = closestVertices.find { it.intersects(worldPoint) }
                        onSelectVertex(tappedVertex)
                    }
                })

            LaunchedEffect(vertexExplanations) {
                while (true) {
                    withFrameNanos {
                        val viewportPoint = pointerPosition
                        if (viewportPoint != null
                            && (hoveredVertexLastCheckDoneTimeNanos == 0L || System.nanoTime() - hoveredVertexLastCheckDoneTimeNanos > 5e7)) { // 50ms

                            val worldPoint = toWorldPoint(viewportSize, viewportPoint, worldOffset, scale, devicePixelRatio)
                            val closestVertices = getClosestVertices(worldPoint, vertices, resultSizeLimit = 10)
                            hoveredVertex = closestVertices.find { it.intersects(worldPoint) }
                            val v = hoveredVertex
                            hoveredVertexLastCheckDoneTimeNanos = System.nanoTime()
                            highlightedExplanationIDs.clear()
                            if (v != null) {
//                                println("hovering over vertex: $it")
                                highlightedExplanationIDs += vertexExplanations.filter { it.vertexID == v.id }.map { it.explanationID }
//                                println(vertexExplanations.map { it })
//                                println(vertexExplanations.filter { it.vertexID == v.id }.map { it.explanationID })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun toWorldPoint(viewportSize: Size, point: Offset, worldOffset: Offset, scale: Float, devicePixelRatio: Float): Offset {
    val viewportTransformOrigin = Offset(viewportSize.width / 2F, viewportSize.height / 2F) / devicePixelRatio
    val scaledOffset = point / devicePixelRatio

    // Let viewport be the position of a vertex in the viewport, vpOrigin be the transform origin of the viewport,
    // world be the position of a vertex in the world (ie vertex.position), worldOffset be the world offset
    // rendered in the top left corner of the viewport. Then:
    // viewport = viewportOrigin + scale * (world + worldOffset - viewportOrigin)
    // Rearranging this equation gives the result below:
    return (((scaledOffset - viewportTransformOrigin) / scale) + viewportTransformOrigin) - worldOffset
}

private fun getClosestVertices(worldPoint: Offset, vertices: List<VertexState>, resultSizeLimit: Int): List<VertexState> {

    // TODO: once we have out-of-viewport detection, use it to make this function more performant on large graphs
//    val computeStart: Long = System.nanoTime()
    val vertexDistances = vertices.associateWith { (worldPoint - it.position).getDistanceSquared() }
//    println("closestVertex: completed in ${(System.nanoTime() - computeStart) / 1e6}ms")
//    if (closestVertex != null) {
//        println(
//            "closestVertex: ${closestVertex.label} (${closestVertex.position}) was the closest " +
//            "vertex to the tap point, $tappedPoint (scaledTapOffset: $scaledTapOffset, " +
//            "worldOffset: $worldOffset, viewportTransformOrigin: $viewportTransformOrigin, " +
//            "scale: $scale)"
//        )
//    }
    return vertexDistances.entries.sortedBy { it.value }.map { it.key }.take(resultSizeLimit)
}

/**
 * Find the endpoint of an edge drawn from `source` position to `target` vertex
 */
private fun edgeEndpoint(source: Offset, target: VertexState): Offset? {
    return when (target.encoding) {
        VertexEncoding.ENTITY, VertexEncoding.RELATION, VertexEncoding.ENTITY_TYPE, VertexEncoding.RELATION_TYPE, VertexEncoding.THING_TYPE -> {
            val r = target.rect
            val targetRect = Rect(Offset(r.left - 4, r.top - 4), Size(r.width + 8, r.height + 8))
            when (target.encoding) {
                VertexEncoding.ENTITY, VertexEncoding.ENTITY_TYPE, VertexEncoding.THING_TYPE -> rectIncomingLineIntersect(source, targetRect)
                else -> diamondIncomingLineIntersect(source, targetRect)
            }
        }
        VertexEncoding.ATTRIBUTE, VertexEncoding.ATTRIBUTE_TYPE -> {
            val targetEllipse = Ellipse(target.position.x, target.position.y, target.width / 2 + 2, target.height / 2 + 2)
            ellipseIncomingLineIntersect(source, targetEllipse)
        }
    }
}

/**
 * Find the end angle of a hyperedge, represented by `arc`, drawn to `target`
 */
private fun hyperedgeEndAngle(arc: Arc, target: VertexState): Float? {
    val intersections = when (target.encoding) {
        VertexEncoding.ENTITY, VertexEncoding.RELATION, VertexEncoding.ENTITY_TYPE, VertexEncoding.RELATION_TYPE, VertexEncoding.THING_TYPE -> {
            val r = target.rect
            val targetRect = Rect(Offset(r.left - 4, r.top - 4), Size(r.width + 8, r.height + 8))
            when (target.encoding) {
                VertexEncoding.ENTITY, VertexEncoding.ENTITY_TYPE, VertexEncoding.THING_TYPE -> rectArcIntersectAngles(arc, targetRect)
                else -> diamondArcIntersectAngles(arc, targetRect)
            }
        }
        VertexEncoding.ATTRIBUTE, VertexEncoding.ATTRIBUTE_TYPE -> {
            // TODO
            throw NotImplementedError()
        }
    }
//    return intersections
    // We expect only one intersection point, although the same point might appear twice if it's a corner of the vertex
    return if (intersections.isEmpty()) null else intersections[0]
}

/**
 * Returns `true` if the given `Offset` intersects the given vertex, else, `false`
 */
private fun VertexState.intersects(point: Offset): Boolean {
    return when (encoding) {
        VertexEncoding.ENTITY_TYPE, VertexEncoding.THING_TYPE, VertexEncoding.ENTITY -> rect.contains(point)

        VertexEncoding.RELATION_TYPE, VertexEncoding.RELATION -> {
            val r = rect
            val polygon = Polygon(
                intArrayOf(r.left.toInt(), r.center.x.toInt(), r.right.toInt(), r.center.x.toInt()),
                intArrayOf(r.center.y.toInt(), r.top.toInt(), r.center.y.toInt(), r.bottom.toInt()),
                4)
            polygon.contains(point.x.toDouble(), point.y.toDouble())
        }

        VertexEncoding.ATTRIBUTE_TYPE, VertexEncoding.ATTRIBUTE -> {
            (point.x - position.x).pow(2) / (width / 2).pow(2) + (point.y - position.y).pow(2) / (height / 2).pow(2) <= 1
        }
    }
}
