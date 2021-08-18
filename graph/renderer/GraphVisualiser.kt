package com.vaticle.graph.renderer

import androidx.compose.desktop.ui.tooling.preview.Preview
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.lang.IllegalStateException

@Preview
@Composable
fun GraphVisualiser() {
    val graph = remember { Graph() }
    var running: Boolean by mutableStateOf(false)
    var canvasSize by mutableStateOf(Size(0F, 0F))
    var devicePixelRatio by mutableStateOf(1F)
    with(LocalDensity.current) {
        println("Updated devicePixelRatio to ${1.dp.toPx()}")
        devicePixelRatio = 1.dp.toPx()
    }

    Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
        Button(modifier = Modifier.align(Alignment.Start), onClick = {
            graph.simulation = graph.toForceSimulation(canvasSize, devicePixelRatio)
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

                graph.edges.forEach {
                    if (it.sourcePosition.x != 0F) drawLine(
                        Color(0xFF7BA0FF),
                        Offset(it.sourcePosition.x, it.sourcePosition.y),
                        Offset(it.targetPosition.x, it.targetPosition.y),
                        strokeWidth = devicePixelRatio
                    )
                }

                graph.vertices.forEach { v ->
                    if (v.position.x == 0F) return@forEach;
                    drawRoundRect(Color(0xFFFFA9E8), Offset(v.position.x, v.position.y), Size(v.width * devicePixelRatio, v.height * devicePixelRatio), CornerRadius(5F * devicePixelRatio))
                }
            }

            graph.vertices.forEach {
                if (it.position.x != 0F) {
                    Column(
                        modifier = Modifier
                            .offset(x = it.position.x.dp / devicePixelRatio, y = it.position.y.dp / devicePixelRatio)
                            .width(it.width.dp)
                            .height(it.height.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = it.label, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    LaunchedEffect(devicePixelRatio) {
        while (true) {
            withFrameNanos {
                // TODO: factor in the "average iteration time" to make this as smooth as possible
                if (!running
                    || graph.simulation == null
                    || System.nanoTime() - graph.previousTimeNanos < 1.667e7 // 16.67ms
                    || graph.simulation!!.alpha() < graph.simulation!!.alphaMin()) { return@withFrameNanos }
                graph.simulation!!.tick()
                graph.vertices.forEach {
                    val node = graph.simulation!!.nodes()[it.id]
                        ?: throw IllegalStateException("Received bad simulation data: no entry received for vertex ID ${it.id}!")
                    it.position = Offset(node.x().toFloat(), node.y().toFloat())
                }
                graph.updateEdgePositions(devicePixelRatio)
                graph.previousTimeNanos = System.nanoTime()
            }
        }
    }
}
