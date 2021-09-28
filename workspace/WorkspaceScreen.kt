package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.VisualiserTheme
import com.vaticle.typedb.studio.data.QueryResponseStream
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import com.vaticle.typedb.studio.visualiser.SimulationMetrics
import com.vaticle.typedb.studio.visualiser.TypeDBForceSimulation
import com.vaticle.typedb.studio.visualiser.TypeDBVisualiser
import com.vaticle.typedb.studio.visualiser.simulationRunnerCoroutine
import java.util.UUID
import kotlin.math.pow

@Composable
fun WorkspaceScreen(workspace: WorkspaceScreenState, visualiserTheme: VisualiserTheme, window: ComposeWindow,
                    devicePixelRatio: Float, titleBarHeight: Float, snackbarHostState: SnackbarHostState) {

    val db = workspace.db
    val snackbarCoroutineScope = rememberCoroutineScope()

    DisposableEffect(workspace.db) {
        onDispose {
            workspace.db.close()
        }
    }

    Column(Modifier.fillMaxSize()) {

        var dataStream: QueryResponseStream by remember { mutableStateOf(QueryResponseStream.EMPTY) }
        val typeDBForceSimulation: TypeDBForceSimulation by remember { mutableStateOf(TypeDBForceSimulation()) }
        var visualiserWorldOffset by remember { mutableStateOf(Offset.Zero) }
        var visualiserSize by mutableStateOf(Size.Zero)
        var visualiserMetricsID by remember { mutableStateOf("") }
        var visualiserScale by remember { mutableStateOf(1F) }
        var queryStartTimeNanos: Long? by remember { mutableStateOf(null) }

        Row(modifier = Modifier.fillMaxWidth().zIndex(10F)) {
            Toolbar(dbName = "grabl")
        }

        Row(modifier = Modifier.fillMaxWidth().height(128.dp).background(StudioTheme.colors.background)
            .zIndex(10F).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            var query by remember { mutableStateOf("match \$x sub thing;\n" +
                    "offset 0;\n" +
                    "limit 1000;\n") }

            Button(modifier = Modifier.size(40.dp).offset(y = 8.dp), onClick = {
                typeDBForceSimulation.init()
                dataStream = db.matchQuery(query)
                visualiserWorldOffset = visualiserSize.center
                visualiserMetricsID = UUID.randomUUID().toString()
                queryStartTimeNanos = System.nanoTime()
            }) {
                Text("▶️")
            }

            OutlinedTextField(modifier = Modifier.fillMaxSize(), label = { Text("Query") },
                value = query, onValueChange = { query = it }, textStyle = StudioTheme.typography.code1,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = StudioTheme.colors.editorBackground,
                    focusedLabelColor = Color(0x99FFFFFF),
                    focusedBorderColor = Color(0x99FFFFFF)))
        }

        Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.panelSeparator)) {}

        Row(modifier = Modifier.fillMaxSize().weight(1F)) {
            TypeDBVisualiser(modifier = Modifier.fillMaxSize().onGloballyPositioned { visualiserSize = it.size.toSize() / devicePixelRatio },
                vertices = typeDBForceSimulation.data.vertices, edges = typeDBForceSimulation.data.edges,
                vertexExplanations = typeDBForceSimulation.data.vertexExplanations, theme = visualiserTheme,
                metrics = SimulationMetrics(id = visualiserMetricsID, worldOffset = visualiserWorldOffset, devicePixelRatio),
                onZoom = { value -> visualiserScale = value },
                explain = { vertex -> db.explainConcept(vertex.id) },
                onVertexDragStart = { vertex ->
                    typeDBForceSimulation.nodes()[vertex.id]?.let { node ->
                        node.isXFixed = true
                        node.isYFixed = true
                    }
                    typeDBForceSimulation
                        .force("link", null)
                        .force("charge", null)
                        .force("center", null)
                        .alpha(0.25)
                        .alphaDecay(0.0)
                }, onVertexDragMove = { vertex, position ->
                    typeDBForceSimulation.nodes()[vertex.id]?.let { node ->
                        node.x(position.x.toDouble())
                        node.y(position.y.toDouble())
                    }
                }, onVertexDragEnd = {
                    typeDBForceSimulation.alphaDecay(1 - typeDBForceSimulation.alphaMin().pow(1.0 / 300))
                })
        }

        Row(modifier = Modifier.fillMaxWidth().zIndex(10F)) {
            StatusBar(
                dataStream = dataStream,
                visualiserScale = visualiserScale,
                vertexCount = typeDBForceSimulation.data.vertices.size,
                edgeCount = typeDBForceSimulation.data.edges.size,
                queryStartTimeNanos = queryStartTimeNanos)
        }

        LaunchedEffect(key1 = dataStream) {
            simulationRunnerCoroutine(typeDBForceSimulation, dataStream, snackbarHostState, snackbarCoroutineScope)
        }
    }
}
