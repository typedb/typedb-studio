package com.vaticle.graph.renderer

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaticle.graph.TypeDBForceSimulation
import com.vaticle.graph.VertexEncoding
import java.lang.IllegalStateException

import com.vaticle.graph.VertexEncoding.*
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
            simulation.init(canvasSize, devicePixelRatio)
            running = true
        }) {
            Text("Run ▶️")
        }

        Box(modifier = Modifier.fillMaxSize()) {

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth: Float = size.width
                val canvasHeight: Float = size.height
                canvasSize = Size(canvasWidth, canvasHeight)

                drawRect(Color(0xFF0E053F), Offset.Zero, Size(canvasWidth, canvasHeight))

                simulation.graph.edges.forEach {
                    if (it.sourcePosition.x != 0F) drawLine(
                        Color(0xFF7BA0FF),
                        Offset(it.sourcePosition.x, it.sourcePosition.y),
                        Offset(it.targetPosition.x, it.targetPosition.y),
                        strokeWidth = devicePixelRatio
                    )
                }

                val vertexColors: Map<VertexEncoding, Color> = theme.vertex.map { Pair(it.key, Color(it.value.argb)) }.toMap()

                simulation.graph.vertices.forEach { v ->
                    if (v.position.x == 0F) return@forEach
                    val vertexColor = requireNotNull(vertexColors[v.encoding])
                    val scaledWidth = v.width * devicePixelRatio
                    val scaledHeight = v.height * devicePixelRatio
                    val scaledCornerRadius = CornerRadius(5F * devicePixelRatio)
                    when (v.encoding) {

                        ENTITY_TYPE, THING_TYPE, ENTITY -> drawRoundRect(
                            color = vertexColor,
                            topLeft = Offset(v.position.x - scaledWidth / 2, v.position.y - scaledHeight / 2),
                            size = Size(scaledWidth, scaledHeight),
                            cornerRadius = scaledCornerRadius)

                        RELATION_TYPE, RELATION -> {
                            // We start with a square of width n and transform it into a rhombus
                            val n: Float = (scaledHeight / sqrt(2.0)).toFloat()
                            withTransform({
                                scale(scaleX = v.width / v.height, scaleY = 1F, pivot = v.position)
                                rotate(degrees = 45F, pivot = v.position)
                            }) {
                                drawRoundRect(
                                    color = vertexColor,
                                    topLeft = Offset(v.position.x - n / 2, v.position.y - n / 2),
                                    size = Size(n, n),
                                    cornerRadius = scaledCornerRadius)
                            }
                        }

                        ATTRIBUTE_TYPE, ATTRIBUTE -> drawOval(
                            color = vertexColor,
                            topLeft = Offset(v.position.x - scaledWidth / 2, v.position.y - scaledHeight / 2),
                            size = Size(scaledWidth, scaledHeight))
                    }
                }
            }

            simulation.graph.vertices.forEach {
                if (it.position.x != 0F) {
                    val scaledX = (it.position.x.dp / devicePixelRatio) - it.width.dp / 2
                    val scaledY = (it.position.y.dp / devicePixelRatio) - it.height.dp / 2
                    Column(
                        modifier = Modifier.offset(scaledX, scaledY).width(it.width.dp).height(it.height.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = it.label, fontFamily = ubuntuMono, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    LaunchedEffect(devicePixelRatio) {
        while (true) {
            withFrameNanos {
                // TODO: adjust the simulation scale if the device pixel ratio has changed since it ran
                // TODO: properties of the graph that involve the Simulation should be extracted to a separate class
                if (!running
                    || System.nanoTime() - simulation.previousTimeNanos < 1.667e7 // 60 FPS
                    || simulation.alpha() < simulation.alphaMin()) { return@withFrameNanos }

                simulation.previousTimeNanos = System.nanoTime()
                simulation.tick()

                val vertexPositions: HashMap<Int, Offset> = HashMap()
                simulation.graph.vertices.forEach {
                    val node = simulation.nodes()[it.id]
                        ?: throw IllegalStateException("Received bad simulation data: no entry received for vertex ID ${it.id}!")
                    it.position = Offset(node.x().toFloat(), node.y().toFloat())
                    vertexPositions[it.id] = it.position
                }
                simulation.graph.edges.forEach {
                    it.sourcePosition = vertexPositions[it.sourceID] as Offset
                    it.targetPosition = vertexPositions[it.targetID] as Offset
                }
            }
        }
    }
}
