package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.VisualiserTheme
import com.vaticle.typedb.studio.data.QueryResponseStream
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import com.vaticle.typedb.studio.ui.elements.Icon
import com.vaticle.typedb.studio.ui.elements.IconSize.*
import com.vaticle.typedb.studio.ui.elements.StudioIcon
import com.vaticle.typedb.studio.ui.elements.StudioTab
import com.vaticle.typedb.studio.ui.elements.StudioTabs
import com.vaticle.typedb.studio.ui.elements.TabHighlight
import com.vaticle.typedb.studio.ui.elements.TabOrientation
import com.vaticle.typedb.studio.visualiser.SimulationMetrics
import com.vaticle.typedb.studio.visualiser.TypeDBForceSimulation
import com.vaticle.typedb.studio.visualiser.TypeDBVisualiser
import com.vaticle.typedb.studio.visualiser.simulationRunnerCoroutine
import java.util.UUID
import kotlin.math.pow

@Composable
fun WorkspaceScreen(workspace: WorkspaceScreenState, visualiserTheme: VisualiserTheme, window: ComposeWindow,
                    devicePixelRatio: Float, titleBarHeight: Float, snackbarHostState: SnackbarHostState) {

    val snackbarCoroutineScope = rememberCoroutineScope()

    var dataStream: QueryResponseStream by remember { mutableStateOf(QueryResponseStream.EMPTY) }
    val typeDBForceSimulation: TypeDBForceSimulation by remember { mutableStateOf(TypeDBForceSimulation()) }
    var visualiserWorldOffset by remember { mutableStateOf(Offset.Zero) }
    var visualiserSize by mutableStateOf(Size.Zero)
    var visualiserMetricsID by remember { mutableStateOf("") }
    var visualiserScale by remember { mutableStateOf(1F) }
    var queryStartTimeNanos: Long? by remember { mutableStateOf(null) }
    val queryTabs: SnapshotStateList<QueryTabState> = remember { mutableStateListOf(
        QueryTabState(initialTitle = "Query1.tql", initialQuery = "match \$x sub thing;\n" +
                "offset 0;\n" +
                "limit 1000;\n")
    ) }
    var activeQueryTabIndex by remember { mutableStateOf(0) }
    val executionTabs: SnapshotStateList<ExecutionTabState> = remember { mutableStateListOf(
        ExecutionTabState(title = "Query1 : run1")
    ) }
    var activeExecutionTabIndex by remember { mutableStateOf(0) }

    val db = workspace.db
    val activeQueryTab: QueryTabState = queryTabs[activeQueryTabIndex]
    val activeExecutionTab: ExecutionTabState = executionTabs[activeExecutionTabIndex]

    DisposableEffect(workspace.db) {
        onDispose {
            workspace.db.close()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Toolbar(dbName = "grabl", onRun = {
            typeDBForceSimulation.init()
            dataStream = db.matchQuery(activeQueryTab.query)
            visualiserWorldOffset = visualiserSize.center
            visualiserMetricsID = UUID.randomUUID().toString()
            queryStartTimeNanos = System.nanoTime()
        })

        Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

        Row(modifier = Modifier.fillMaxWidth().weight(1F)) {
            Column(modifier = Modifier.width(20.dp)) {
                StudioTabs(orientation = TabOrientation.BOTTOM_TO_TOP) {
                    StudioTab("Schema Explorer", selected = false, leadingIcon = { StudioIcon(Icon.Layout) })
                    StudioTab("Permissions", selected = false, leadingIcon = { StudioIcon(Icon.Shield) })
                }
            }

            Column(modifier = Modifier.fillMaxHeight().width(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

            Column(modifier = Modifier.fillMaxHeight().weight(1F)) {
                StudioTabs(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                    queryTabs.mapIndexed { index: Int, queryTab: QueryTabState ->
                        val selected = index == activeQueryTabIndex
                        StudioTab(text = queryTab.title, selected = selected, highlight = TabHighlight.BOTTOM,
                            showCloseButton = true)
                    }
                    Spacer(Modifier.width(6.dp))
                    StudioIcon(icon = Icon.Plus, size = Size14)
                }

                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

                CodeEditor(code = activeQueryTab.query, onChange = { value -> activeQueryTab.query = value },
                    font = StudioTheme.typography.codeEditorSwing,
                    modifier = Modifier.fillMaxWidth().height(104.dp).background(StudioTheme.colors.editorBackground))

                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

                Row(modifier = Modifier.fillMaxWidth().height(26.dp).background(StudioTheme.colors.background),
                    verticalAlignment = Alignment.CenterVertically) {

                    StudioTabs(modifier = Modifier.weight(1f)) {
                        Spacer(Modifier.width(8.dp))
                        Text("Output:", style = StudioTheme.typography.body2)
                        Spacer(Modifier.width(8.dp))

                        executionTabs.mapIndexed { index: Int, executionTab: ExecutionTabState ->
                            val selected = index == activeExecutionTabIndex
                            StudioTab(text = executionTab.title, selected = selected,
                                highlight = TabHighlight.BOTTOM, showCloseButton = true)
                        }
                    }

                    StudioIcon(Icon.Cog)
                    Spacer(Modifier.width(14.dp))

                    StudioIcon(Icon.Minus)
                    Spacer(Modifier.width(12.dp))
                }

                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

                Row(modifier = Modifier.weight(1F).zIndex(-1F)) {
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

                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

                StudioTabs(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                    StudioTab("Log", selected = false, highlight = TabHighlight.TOP,
                        leadingIcon = { StudioIcon(Icon.HorizontalBarChartDesc) })
                    StudioTab("Graph", selected = true, highlight = TabHighlight.TOP,
                        leadingIcon = { StudioIcon(Icon.Graph) })
                    StudioTab("Table", selected = false, highlight = TabHighlight.TOP,
                        leadingIcon = { StudioIcon(Icon.Table) })
                }
            }

            Column(modifier = Modifier.fillMaxHeight().width(1.dp).background(StudioTheme.colors.uiElementBorder).zIndex(20f)) {}

            Column(modifier = Modifier.width(20.dp)) {
                StudioTabs(orientation = TabOrientation.TOP_TO_BOTTOM) {
                    StudioTab("Settings", selected = false, leadingIcon = { StudioIcon(Icon.Cog) })
                    StudioTab("Graph Explorer", selected = false, leadingIcon = { StudioIcon(Icon.TimelineBarChart) })
                }
            }
        }

        Row {
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
