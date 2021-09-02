package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.data.QueryResponseStream
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@Composable
fun StatusBar(modifier: Modifier = Modifier, dataStream: QueryResponseStream, visualiserScale: Float, vertexCount: Int,
              edgeCount: Int, queryStartTimeNanos: Long?) {

    val mathContext3SigFigures: MathContext = remember { MathContext(3, RoundingMode.HALF_UP) }
    var queryRunTimeMillis: Double by remember { mutableStateOf(0.0) }
    var principalStatus: String by remember { mutableStateOf("Ready") }

    Column(modifier = modifier.height(27.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.panelSeparator)) {}
        Row(modifier = modifier.fillMaxHeight().background(StudioTheme.colors.background),
            verticalAlignment = Alignment.CenterVertically) {

            Spacer(modifier = modifier.width(8.dp))
            Text(principalStatus, style = StudioTheme.typography.body2)

            Spacer(modifier = modifier.weight(1F))

            Text("Zoom: ${BigDecimal(visualiserScale * 100.0, mathContext3SigFigures).toPlainString()}%",
                style = StudioTheme.typography.body2)
            Spacer(modifier = modifier.width(16.dp))
            Text("Vertices: $vertexCount | Edges: $edgeCount | ${queryRunTimeMillis.toDurationString()}",
                style = StudioTheme.typography.body2)
            Spacer(modifier = modifier.width(8.dp))
        }
    }

    LaunchedEffect(dataStream.completed, queryStartTimeNanos, dataStream.queryEndTimeNanos) {
        while (true) {
            withFrameNanos {
                principalStatus = if (dataStream.completed) "Ready" else "Running Match query..."
                queryRunTimeMillis = when (queryStartTimeNanos) {
                    null -> 0.0
                    else -> when (val endTime = dataStream.queryEndTimeNanos) {
                        null -> (System.nanoTime() - queryStartTimeNanos) * 1e-6
                        else -> (endTime - queryStartTimeNanos) * 1e-6
                    }
                }
            }
            if (dataStream.completed) {
                principalStatus = "Ready"
                return@LaunchedEffect
            }
        }
    }
}

private fun Double.toDurationString(): String {
    val millis = String.format("%03d", (this % 1000).toInt())
    val seconds = String.format("%02d", ((this / 1000) % 60).toInt())
    val minutes = String.format("%02d", ((this / (1000 * 60)) % 60).toInt())
    val hours = String.format("%02d", ((this / (1000 * 60 * 60)) % 24).toInt())

    return "${when (hours) { "00" -> ""; else -> "$hours:" } }$minutes:$seconds.$millis"
}
