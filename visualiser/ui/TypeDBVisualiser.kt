package com.vaticle.typedb.studio.visualiser.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.db.DB
import com.vaticle.typedb.studio.db.GraphData
import com.vaticle.typedb.studio.db.VertexEncoding
import com.vaticle.typedb.studio.visualiser.EdgeState
import com.vaticle.typedb.studio.visualiser.Ellipse
import com.vaticle.typedb.studio.visualiser.TypeDBForceSimulation
import com.vaticle.typedb.studio.visualiser.TypeDBVisualiserState
import com.vaticle.typedb.studio.visualiser.VertexState
import java.lang.IllegalStateException

import com.vaticle.typedb.studio.db.VertexEncoding.*
import com.vaticle.typedb.studio.visualiser.GraphState
import com.vaticle.typedb.studio.visualiser.arrowhead
import com.vaticle.typedb.studio.visualiser.diamondIncomingLineIntersect
import com.vaticle.typedb.studio.visualiser.ellipseIncomingLineIntersect
import com.vaticle.typedb.studio.visualiser.midpoint
import com.vaticle.typedb.studio.visualiser.rectIncomingLineIntersect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.MouseInfo
import java.awt.Point
import java.util.concurrent.CompletionException
import kotlin.math.sqrt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TypeDBVisualiser(db: DB, theme: VisualiserTheme, window: ComposeWindow, devicePixelRatio: Float, titleBarHeight: Float,
    snackbarHostState: SnackbarHostState, snackbarCoroutineScope: CoroutineScope) {

    val visualiser = remember { TypeDBVisualiserState() }
    val simulation: TypeDBForceSimulation = visualiser.simulation;
    val data: GraphState = simulation.data
    var running by remember { mutableStateOf(false) }
    var viewportScale by remember { mutableStateOf(1F) }
    var viewportOffset by remember { mutableStateOf(Offset.Zero) }

//    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
//        viewportScale *= zoomChange
//        viewportOffset += panChange
//    }

//    val swipeableState = rememberSwipeableState(initialValue = 1000, confirmStateChange = { value ->
//        println(value)
//        true
//    })
    val anchors = mapOf(0F to 100, 1000F to 1000, 2000F to 2000)

    val ubuntuMono = FontFamily(
        Font(resource = "fonts/UbuntuMono/UbuntuMono-Regular.ttf", weight = FontWeight.W400, style = FontStyle.Normal)
    )

    Column(Modifier.fillMaxSize()) {

        var canvasSize by remember { mutableStateOf(Size.Zero) }
        var canvasPositionOnScreen by remember { mutableStateOf(Offset.Zero) }

        Box(modifier = Modifier.fillMaxWidth()
            .onGloballyPositioned { coordinates ->
//                canvasPositionOnScreen = coordinates.localToWindow(Offset.Zero) + Offset(window.x.toFloat(), window.y.toFloat())
//                println("'Run' button: coordinatesLocal=${coordinates.localToWindow(Offset.Zero)}")
            }
            .background(Color.White).align(Alignment.Start).zIndex(10F)) {
            Button(onClick = {
                simulation.init()
                visualiser.dataStream = db.matchQuery("grabl", "match \$x isa thing; offset 0; limit 1000000;")
                viewportOffset = canvasSize.center
                running = true
            }) {
                Text("Run ▶️")
            }
        }

        Box(modifier = Modifier.fillMaxSize()
            .pointerInput(keys = emptyArray(), block = {
                detectDragGestures { _, dragAmount ->
                    viewportOffset += dragAmount / (viewportScale * devicePixelRatio)
                }
//                detectTransformGestures { centroid, pan, zoom, rotation ->
//                    println("centroid=$centroid,pan=$pan,zoom=$zoom,rotation=$rotation")
//                }
            })
            .onGloballyPositioned { coordinates ->
                canvasPositionOnScreen = coordinates.localToWindow(Offset.Zero) / devicePixelRatio + Offset(window.x.toFloat(), window.y + titleBarHeight)
//                println("coordinatesLocal=${coordinates.localToWindow(Offset.Zero)},windowPosition=${window.location},canvasPositionOnScreen=$canvasPositionOnScreen")
            }
            .scrollable(orientation = Orientation.Vertical, state = rememberScrollableState { delta ->
                val zoomFactor = 1 + (delta * 0.0006F / devicePixelRatio)
                val newViewportScale = viewportScale * zoomFactor
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
                println("newViewportScale=$newViewportScale")
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
                viewportScale = newViewportScale
                delta
            })
            .background(Color(theme.background.argb))) {

            Box(modifier = Modifier.fillMaxSize()
                .graphicsLayer(
                    scaleX = viewportScale, scaleY = viewportScale,
                    translationX = viewportOffset.x * viewportScale * devicePixelRatio,
                    translationY = viewportOffset.y * viewportScale * devicePixelRatio)
            ) {

                Canvas(modifier = Modifier.fillMaxSize()) {
                    canvasSize = size / devicePixelRatio // This tells the simulation where its centre point should lie.

                    val verticesByID = data.vertices.associateBy { it.id }
                    if (data.edges.size <= 1000 && viewportScale > 0.2) {
                        data.edges.forEach { drawEdgeSegments(it, verticesByID, theme, Offset.Zero, devicePixelRatio) }
                    } else {
                        data.edges.forEach { drawSolidEdge(it, verticesByID, theme, Offset.Zero, devicePixelRatio) }
                    }
                }

                if (data.edges.size <= 1000 && viewportScale > 0.2) data.edges.forEach { drawEdgeLabel(it, theme, ubuntuMono, Offset.Zero) }

                Canvas(modifier = Modifier.fillMaxSize()
//                .swipeable(state = swipeableState, anchors = anchors, orientation = Orientation.Vertical)
//                        .transformable(state = transformableState)
                ) {
                    val vertexColors: Map<VertexEncoding, Color> =
                        theme.vertex.map { Pair(it.key, Color(it.value.argb)) }.toMap()
                    data.vertices.forEach { drawVertex(it, vertexColors, Offset.Zero, devicePixelRatio) }
                }

                if (data.vertices.size <= 1000 && viewportScale > 0.2) data.vertices.forEach { drawVertexLabel(it, theme, ubuntuMono, Offset.Zero) }
            }
        }
    }

    LaunchedEffect(devicePixelRatio) {
        while (true) {
            withFrameNanos {
                if (!running
                    || System.nanoTime() - simulation.lastTickStartNanos < 1.667e7 // 60 FPS
                    || simulation.alpha() < simulation.alphaMin()
                    || !visualiser.dataStream.completed ) { return@withFrameNanos } // TODO: this just waits for the stream to complete, it should actually stream.

                val response: Either<GraphData, Exception> = visualiser.dataStream.drain()
                if (response.isSecond) {
                    running = false
                    val error: Throwable = when {
                        response.second() is CompletionException -> response.second().cause.let {
                            when (it) { null -> response.second() else -> it }
                        }
                        else -> response.second()
                    }
                    snackbarCoroutineScope.launch {
                        println(error.toString())
                        snackbarHostState.showSnackbar(error.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
                    }
                    return@withFrameNanos
                }
                val graphData: GraphData = response.first()
                simulation.addVertices(graphData.vertices.map(VertexState::of))
                simulation.addEdges(graphData.edges.map(EdgeState::of))

                simulation.lastTickStartNanos = System.nanoTime()
                simulation.tick()
//                println("simulation.tick: ${(System.nanoTime() - simulation.lastTickStartNanos) * 0.000001}ms (alpha: ${simulation.alpha()})")


                data.vertices.forEach {
                    val node = simulation.nodes()[it.id]
                        ?: throw IllegalStateException("Received bad simulation data: no entry received for vertex ID ${it.id}!")
                    it.position = Offset(node.x().toFloat(), node.y().toFloat())
                }
                val verticesByID: Map<Int, VertexState> = data.vertices.associateBy { it.id }
                data.edges.forEach {
                    it.sourcePosition = verticesByID[it.sourceID]!!.position
                    it.targetPosition = verticesByID[it.targetID]!!.position
                }
            }
        }
    }
}

private fun DrawScope.drawVertex(v: VertexState, vertexColors: Map<VertexEncoding, Color>, viewportOffset: Offset, devicePixelRatio: Float) {
    val vertexColor = requireNotNull(vertexColors[v.encoding])
    val displayPosition = (v.position - viewportOffset) * devicePixelRatio
    val displayWidth = v.width * devicePixelRatio
    val displayHeight = v.height * devicePixelRatio
    val displayCornerRadius = CornerRadius(5F * devicePixelRatio)
    when (v.encoding) {

        ENTITY_TYPE, THING_TYPE, ENTITY -> drawRoundRect(
            color = vertexColor,
            topLeft = Offset(displayPosition.x - displayWidth / 2, displayPosition.y - displayHeight / 2),
            size = Size(displayWidth, displayHeight),
            cornerRadius = displayCornerRadius)

        RELATION_TYPE, RELATION -> {
            // We start with a square of width n and transform it into a rhombus
            val n: Float = (displayHeight / sqrt(2.0)).toFloat()
            withTransform({
                scale(scaleX = v.width / v.height, scaleY = 1F, pivot = displayPosition)
                rotate(degrees = 45F, pivot = displayPosition)
            }) {
                drawRoundRect(
                    color = vertexColor,
                    topLeft = Offset(displayPosition.x - n / 2, displayPosition.y - n / 2),
                    size = Size(n, n),
                    cornerRadius = displayCornerRadius)
            }
        }

        ATTRIBUTE_TYPE, ATTRIBUTE -> drawOval(
            color = vertexColor,
            topLeft = Offset(displayPosition.x - displayWidth / 2, displayPosition.y - displayHeight / 2),
            size = Size(displayWidth, displayHeight))
    }
}

@Composable
private fun drawVertexLabel(v: VertexState, theme: VisualiserTheme, fontFamily: FontFamily, viewportOffset: Offset) {
    val x = (v.position.x - viewportOffset.x - v.width / 2).dp
    val y = (v.position.y - viewportOffset.y - v.height / 2).dp
    Column(modifier = Modifier.offset(x, y).width(v.width.dp).height(v.height.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = v.label, color = Color(theme.vertexLabel.argb), fontSize = 16.sp, fontFamily = fontFamily, textAlign = TextAlign.Center)
    }
}

private fun DrawScope.drawSolidEdge(edge: EdgeState, verticesByID: Map<Int, VertexState>, theme: VisualiserTheme, viewportOffset: Offset, devicePixelRatio: Float) {
    val sourceVertex = requireNotNull(verticesByID[edge.sourceID])
    val targetVertex = requireNotNull(verticesByID[edge.targetID])
    val lineSource = sourceVertex.position
    val lineTarget = targetVertex.position

    drawLine(
        start = (lineSource - viewportOffset) * devicePixelRatio, end = (lineTarget - viewportOffset) * devicePixelRatio,
        color = Color(theme.edge.argb), strokeWidth = devicePixelRatio
    )

    val arrow = arrowhead(
        from = (lineSource - viewportOffset) * devicePixelRatio, to = (lineTarget - viewportOffset) * devicePixelRatio,
        arrowLength = 6F * devicePixelRatio, arrowWidth = 3F * devicePixelRatio
    )
    if (arrow != null) drawPath(path = arrow, color = Color(theme.edge.argb))
}

private fun DrawScope.drawEdgeSegments(edge: EdgeState, verticesByID: Map<Int, VertexState>, theme: VisualiserTheme, viewportOffset: Offset, devicePixelRatio: Float) {
    val sourceVertex = requireNotNull(verticesByID[edge.sourceID])
    val targetVertex = requireNotNull(verticesByID[edge.targetID])
    val lineSource = edgeEndpoint(targetVertex, sourceVertex)
    val lineTarget = edgeEndpoint(sourceVertex, targetVertex)

    if (lineSource != null && lineTarget != null) {
        val m: Offset = midpoint(edge.sourcePosition, edge.targetPosition)
        // TODO: This Size is an approximation - a Compose equivalent of TextMetrics would be more robust
        val labelRect = Rect(
            Offset(m.x - edge.label.length * 4 - 2, m.y - 7 - 2),
            Size(edge.label.length * 8F + 4, 14F + 4)
        )
        val linePart1Target = rectIncomingLineIntersect(sourcePoint = lineSource, rect = labelRect)
        if (linePart1Target != null) {
            drawLine(
                start = (lineSource - viewportOffset) * devicePixelRatio, end = (linePart1Target - viewportOffset) * devicePixelRatio,
                color = Color(theme.edge.argb), strokeWidth = devicePixelRatio
            )
        }
        val linePart2Source = rectIncomingLineIntersect(sourcePoint = lineTarget, rect = labelRect)
        if (linePart2Source != null) {
            drawLine(
                start = (linePart2Source - viewportOffset) * devicePixelRatio, end = (lineTarget - viewportOffset) * devicePixelRatio,
                color = Color(theme.edge.argb), strokeWidth = devicePixelRatio
            )

            val arrow = arrowhead(
                from = (lineSource - viewportOffset) * devicePixelRatio, to = (lineTarget - viewportOffset) * devicePixelRatio,
                arrowLength = 6F * devicePixelRatio, arrowWidth = 3F * devicePixelRatio
            )
            if (arrow != null) drawPath(path = arrow, color = Color(theme.edge.argb))
        }
    }
}

@Composable
private fun drawEdgeLabel(edge: EdgeState, theme: VisualiserTheme, fontFamily: FontFamily, viewportOffset: Offset) {
    val m: Offset = midpoint(edge.sourcePosition, edge.targetPosition) - viewportOffset
    val rect = Rect(Offset(m.x - edge.label.length * 4, m.y - 7), Size(edge.label.length * 8F, 14F))
    Column(
        modifier = Modifier.offset(rect.left.dp, rect.top.dp).width(rect.width.dp).height(rect.height.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = edge.label, color = Color(theme.edge.argb), fontSize = 14.sp, fontFamily = fontFamily, textAlign = TextAlign.Center)
    }
}

/**
 * Find the endpoint of an edge drawn from `source` to `target`
 */
private fun edgeEndpoint(source: VertexState, target: VertexState): Offset? {
    return when (target.encoding) {
        ENTITY, RELATION, ENTITY_TYPE, RELATION_TYPE, THING_TYPE -> {
            val targetRect = Rect(Offset(target.position.x - target.width / 2 - 4, target.position.y - target.height / 2 - 4), Size(target.width + 8, target.height + 8))
            when (target.encoding) {
                ENTITY, ENTITY_TYPE, THING_TYPE -> rectIncomingLineIntersect(source.position, targetRect)
                else -> diamondIncomingLineIntersect(source.position, targetRect)
            }
        }
        ATTRIBUTE, ATTRIBUTE_TYPE -> {
            val targetEllipse = Ellipse(target.position.x, target.position.y, target.width / 2 + 2, target.height / 2 + 2)
            ellipseIncomingLineIntersect(source.position, targetEllipse)
        }
    }
}
