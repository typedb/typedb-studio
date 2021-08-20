package com.vaticle.graph.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.graph.EdgeData
import com.vaticle.graph.Ellipse
import com.vaticle.graph.TypeDBForceSimulation
import com.vaticle.graph.VertexData
import com.vaticle.graph.VertexEncoding
import java.lang.IllegalStateException

import com.vaticle.graph.VertexEncoding.*
import com.vaticle.graph.arrowhead
import com.vaticle.graph.diamondIncomingLineIntersect
import com.vaticle.graph.ellipseIncomingLineIntersect
import com.vaticle.graph.midpoint
import com.vaticle.graph.rectIncomingLineIntersect
import kotlin.math.sqrt

@Composable
fun GraphVisualiser(theme: VisualiserTheme) {
    val simulation = remember { TypeDBForceSimulation() }
    var running: Boolean by mutableStateOf(false)
    var canvasSize by mutableStateOf(Size(0F, 0F))
    var devicePixelRatio by mutableStateOf(1F)
    with(LocalDensity.current) { devicePixelRatio = 1.dp.toPx() }

    val ubuntuMono = FontFamily(
        Font(resource = "fonts/UbuntuMono/UbuntuMono-Regular.ttf", weight = FontWeight.W400, style = FontStyle.Normal)
    )

    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
        Button(modifier = Modifier.align(Alignment.Start), onClick = {
            simulation.init(canvasSize)
            running = true
        }) {
            Text("Run ▶️")
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(theme.background.argb))) {
            val visibleVertices = simulation.graph.vertices.filter { it.position.x != 0F }
            val visibleEdges = simulation.graph.edges.filter { it.sourcePosition.x != 0F }

            Canvas(modifier = Modifier.fillMaxSize()) {
                canvasSize = size / devicePixelRatio // This tells the simulation where its centre point should lie.

                val verticesByID = simulation.graph.vertices.associateBy { it.id }
                visibleEdges.forEach { drawEdge(it, verticesByID, theme, devicePixelRatio) }
            }

            visibleEdges.forEach { drawEdgeLabel(it, theme, ubuntuMono) }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val vertexColors: Map<VertexEncoding, Color> = theme.vertex.map { Pair(it.key, Color(it.value.argb)) }.toMap()
                visibleVertices.forEach { drawVertex(it, vertexColors, devicePixelRatio) }
            }

            visibleVertices.forEach { drawVertexLabel(it, theme, ubuntuMono) }
        }
    }

    LaunchedEffect(devicePixelRatio) {
        while (true) {
            withFrameNanos {
                // TODO: adjust the simulation scale if the device pixel ratio has changed since it ran
                if (!running
                    || System.nanoTime() - simulation.previousTimeNanos < 1.667e7 // 60 FPS
                    || simulation.alpha() < simulation.alphaMin()) { return@withFrameNanos }

                simulation.previousTimeNanos = System.nanoTime()
                simulation.tick()

                simulation.graph.vertices.forEach {
                    val node = simulation.nodes()[it.id]
                        ?: throw IllegalStateException("Received bad simulation data: no entry received for vertex ID ${it.id}!")
                    it.position = Offset(node.x().toFloat(), node.y().toFloat())
                }
                val verticesByID: Map<Int, VertexData> = simulation.graph.vertices.associateBy { it.id }
                simulation.graph.edges.forEach {
                    it.sourcePosition = verticesByID[it.sourceID]!!.position
                    it.targetPosition = verticesByID[it.targetID]!!.position
                }
            }
        }
    }
}

private fun DrawScope.drawVertex(v: VertexData, vertexColors: Map<VertexEncoding, Color>, devicePixelRatio: Float) {
    val vertexColor = requireNotNull(vertexColors[v.encoding])
    val scaledPosition = v.position * devicePixelRatio
    val scaledWidth = v.width * devicePixelRatio
    val scaledHeight = v.height * devicePixelRatio
    val scaledCornerRadius = CornerRadius(5F * devicePixelRatio)
    when (v.encoding) {

        ENTITY_TYPE, THING_TYPE, ENTITY -> drawRoundRect(
            color = vertexColor,
            topLeft = Offset(scaledPosition.x - scaledWidth / 2, scaledPosition.y - scaledHeight / 2),
            size = Size(scaledWidth, scaledHeight),
            cornerRadius = scaledCornerRadius)

        RELATION_TYPE, RELATION -> {
            // We start with a square of width n and transform it into a rhombus
            val n: Float = (scaledHeight / sqrt(2.0)).toFloat()
            withTransform({
                scale(scaleX = v.width / v.height, scaleY = 1F, pivot = scaledPosition)
                rotate(degrees = 45F, pivot = scaledPosition)
            }) {
                drawRoundRect(
                    color = vertexColor,
                    topLeft = Offset(scaledPosition.x - n / 2, scaledPosition.y - n / 2),
                    size = Size(n, n),
                    cornerRadius = scaledCornerRadius)
            }
        }

        ATTRIBUTE_TYPE, ATTRIBUTE -> drawOval(
            color = vertexColor,
            topLeft = Offset(scaledPosition.x - scaledWidth / 2, scaledPosition.y - scaledHeight / 2),
            size = Size(scaledWidth, scaledHeight))
    }
}

@Composable
private fun drawVertexLabel(v: VertexData, theme: VisualiserTheme, fontFamily: FontFamily) {
    val x = v.position.x.dp - v.width.dp / 2
    val y = v.position.y.dp - v.height.dp / 2
    Column(modifier = Modifier.offset(x, y).width(v.width.dp).height(v.height.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = v.label, color = Color(theme.vertexLabel.argb), fontSize = 16.sp, fontFamily = fontFamily, textAlign = TextAlign.Center)
    }
}

private fun DrawScope.drawEdge(edge: EdgeData, verticesByID: Map<Int, VertexData>, theme: VisualiserTheme, devicePixelRatio: Float) {
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
                start = lineSource * devicePixelRatio, end = linePart1Target * devicePixelRatio,
                color = Color(theme.edge.argb), strokeWidth = devicePixelRatio
            )
        }
        val linePart2Source = rectIncomingLineIntersect(sourcePoint = lineTarget, rect = labelRect)
        if (linePart2Source != null) {
            drawLine(
                start = linePart2Source * devicePixelRatio, end = lineTarget * devicePixelRatio,
                color = Color(theme.edge.argb), strokeWidth = devicePixelRatio
            )

            val arrow = arrowhead(
                from = lineSource * devicePixelRatio, to = lineTarget * devicePixelRatio,
                arrowLength = 6F * devicePixelRatio, arrowWidth = 3F * devicePixelRatio
            )
            if (arrow != null) drawPath(path = arrow, color = Color(theme.edge.argb))
        }
    }
}

@Composable
private fun drawEdgeLabel(edge: EdgeData, theme: VisualiserTheme, fontFamily: FontFamily) {
    val m: Offset = midpoint(edge.sourcePosition, edge.targetPosition)
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
private fun edgeEndpoint(source: VertexData, target: VertexData): Offset? {
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
