package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.vaticle.force.graph.Link
import com.vaticle.force.graph.LinkForce
import com.vaticle.force.graph.Node
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.VisualiserTheme
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.data.EdgeEncoding.*
import com.vaticle.typedb.studio.data.QueryResponseStream
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import com.vaticle.typedb.studio.ui.elements.Icon
import com.vaticle.typedb.studio.ui.elements.IconSize.*
import com.vaticle.typedb.studio.ui.elements.StudioIcon
import com.vaticle.typedb.studio.ui.elements.StudioTab
import com.vaticle.typedb.studio.ui.elements.StudioTabs
import com.vaticle.typedb.studio.ui.elements.TabHighlight
import com.vaticle.typedb.studio.visualiser.SimulationMetrics
import com.vaticle.typedb.studio.visualiser.TypeDBForceSimulation
import com.vaticle.typedb.studio.visualiser.TypeDBVisualiser
import com.vaticle.typedb.studio.visualiser.VertexState
import com.vaticle.typedb.studio.visualiser.simulationRunnerCoroutine
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.io.File
import java.io.FilenameFilter
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.math.pow

@Composable
fun WorkspaceScreen(workspace: WorkspaceScreenState, navigator: Navigator, visualiserTheme: VisualiserTheme, window: ComposeWindow,
                    devicePixelRatio: Float, titleBarHeight: Float, snackbarHostState: SnackbarHostState) {

    val snackbarCoroutineScope = rememberCoroutineScope()

    var dataStream: QueryResponseStream by remember { mutableStateOf(QueryResponseStream.EMPTY) }
    val typeDBForceSimulation: TypeDBForceSimulation by remember { mutableStateOf(TypeDBForceSimulation()) }
    var visualiserWorldOffset by remember { mutableStateOf(Offset.Zero) }
    var visualiserSize by mutableStateOf(Size.Zero)
    var visualiserMetricsID by remember { mutableStateOf("") }
    var visualiserScale by remember { mutableStateOf(1F) }
    var selectedVertex: VertexState? by remember { mutableStateOf(null) }
    val selectedVertexNetwork: MutableList<VertexState> = remember { mutableStateListOf() }
    var queryStartTimeNanos: Long? by remember { mutableStateOf(null) }
    val queryTabs: MutableList<QueryTabState> = remember { mutableStateListOf(
        QueryTabState(title = "Query1.tql", query = "match \$x sub thing;\n" +
                "offset 0;\n" +
                "limit 1000;\n")
    ) }
    var queryTabNextIndex by remember { mutableStateOf(2) }
    var activeQueryTabIndex by remember { mutableStateOf(0) }
    val executionTabs: MutableList<ExecutionTabState> = remember { mutableStateListOf(
        ExecutionTabState(title = "Query1 : run1")
    ) }
    var activeExecutionTabIndex by remember { mutableStateOf(0) }
    var showQuerySettingsPanel by remember { mutableStateOf(true) }
    var showConceptPanel by remember { mutableStateOf(true) }
    var querySettings by remember { mutableStateOf(QuerySettings()) }
    var temporarilyFrozenNodeIDs = remember { mutableStateListOf<Int>() }

    val db = requireNotNull(workspace.loginForm.db)
    val activeQueryTab: QueryTabState = queryTabs[activeQueryTabIndex]
    val activeExecutionTab: ExecutionTabState = executionTabs[activeExecutionTabIndex]

    fun addNewQueryTab() {
        queryTabs += QueryTabState(title = "Query${queryTabNextIndex}.tql", query = "\n")
        queryTabNextIndex++
    }

    fun openOpenQueryDialog() {
        try {
            FileDialog(window, "Open", FileDialog.LOAD).apply {
                isMultipleMode = false

                // TODO: look into file extension filtering
//                // Windows
//                file = "*.tql"
//
//                // Mac, Linux
//                filenameFilter = FilenameFilter { _, name -> name?.endsWith(".tql") ?: false }

                isVisible = true
                file?.let { filename ->
                    val path = Path.of(directory, filename)
                    val fileContent: String = Files.readString(Path.of(directory, filename))
                    queryTabs += QueryTabState(title = filename, query = fileContent, file = path.toFile())
                    activeQueryTabIndex = queryTabs.size - 1
                }
            }
        } catch (error: Exception) {
            snackbarCoroutineScope.launch {
                println(error.stackTraceToString())
                snackbarHostState.showSnackbar(error.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
            }
        }
    }

    fun openSaveQueryDialog() {
        try {
            FileDialog(window, "Save", FileDialog.SAVE).apply {
                isMultipleMode = false
                val queryTabFile = activeQueryTab.file
                if (queryTabFile == null) {
                    file = activeQueryTab.title
                } else {
                    file = queryTabFile.name
                    directory = queryTabFile.parentFile.absolutePath
                }
                filenameFilter = FilenameFilter { _, name -> name?.endsWith(".tql") ?: false }
                isVisible = true
                file?.let { filename ->
                    PrintWriter(Path.of(directory, filename).toFile()).use { printWriter ->
                        printWriter.print(activeQueryTab.query)
                        activeQueryTab.title = filename
                        activeQueryTab.file = File(filename)
                    }
                }
            }
        } catch (error: Exception) {
            snackbarCoroutineScope.launch {
                println(error.stackTraceToString())
                snackbarHostState.showSnackbar(error.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
            }
        }
    }

    fun onSelectVertex(vertex: VertexState?) {
        selectedVertex = vertex
        selectedVertexNetwork.clear()
        if (vertex != null) {
            val verticesToAdd = mutableSetOf(vertex)
            val verticesByID = typeDBForceSimulation.data.vertices.associateBy { it.id }
            typeDBForceSimulation.data.edges.forEach {
                if (it.sourceID == vertex.id) verticesToAdd += verticesByID[it.targetID]!!
                else if (it.targetID == vertex.id) verticesToAdd += verticesByID[it.sourceID]!!
            }
            selectedVertexNetwork += verticesToAdd
        }
    }

    fun onVertexDragStart(vertex: VertexState) {
        typeDBForceSimulation.nodes()[vertex.id]?.let { node: Node ->
            node.isXFixed = true
            node.isYFixed = true
        }
        typeDBForceSimulation
            .force("charge", null)
            .force("center", null)
            .force("x", null)
            .force("y", null)
            .alpha(0.25)
            .alphaDecay(0.0)

        if (typeDBForceSimulation.data.edges.any { it.targetID == vertex.id && it.encoding == ROLEPLAYER }) {
            val attributeEdges = typeDBForceSimulation.data.edges
                .filter { it.sourceID == vertex.id && it.encoding == HAS }
            val attributeNodeIDs = attributeEdges.map { it.targetID }
            val roleplayerEdges = typeDBForceSimulation.data.edges
                .filter { it.targetID == vertex.id && it.encoding == ROLEPLAYER }
                .map { it.sourceID }
                .flatMap { relationNodeID -> typeDBForceSimulation.data.edges
                    .filter { it.sourceID == relationNodeID && it.encoding == ROLEPLAYER } }
            val relationNodeIDs = roleplayerEdges.map { it.sourceID }
            val roleplayerNodeIDs = roleplayerEdges.map { it.targetID }
            val nodeIDs = (attributeNodeIDs + relationNodeIDs + roleplayerNodeIDs).toSet()
            val nodes = nodeIDs.map { typeDBForceSimulation.nodes()[it] }
            val links = (attributeEdges + roleplayerEdges)
                .map { Link(typeDBForceSimulation.nodes()[it.sourceID], typeDBForceSimulation.nodes()[it.targetID]) }
            val nodeIDsToFreeze = roleplayerNodeIDs
                .filter { it != vertex.id && typeDBForceSimulation.nodes()[it]?.isXFixed == false }
            nodeIDsToFreeze.forEach {
                typeDBForceSimulation.nodes()[it]?.isXFixed = true
                typeDBForceSimulation.nodes()[it]?.isYFixed = true
            }
            temporarilyFrozenNodeIDs += nodeIDsToFreeze
            typeDBForceSimulation.force("link", LinkForce(nodes, links, 90.0, 0.25))
        } else {
            typeDBForceSimulation.force("link", null)
        }
    }

    fun onVertexDragMove(vertex: VertexState, position: Offset) {
        typeDBForceSimulation.nodes()[vertex.id]?.let { node: Node ->
            node.x(position.x.toDouble())
            node.y(position.y.toDouble())
        }
    }

    fun onVertexDragEnd() {
        typeDBForceSimulation.force("link", null)
        typeDBForceSimulation.alphaDecay(1 - typeDBForceSimulation.alphaMin().pow(1.0 / 300))
        temporarilyFrozenNodeIDs.forEach {
            typeDBForceSimulation.nodes()[it]?.isXFixed = false
            typeDBForceSimulation.nodes()[it]?.isYFixed = false
        }
        temporarilyFrozenNodeIDs.clear()
    }

    Column(Modifier.fillMaxSize()) {
        Toolbar(dbName = db.name,
            onDBNameChange = { dbName ->
                // TODO: add confirmation dialog
                db.client.closeAllSessions()
                workspace.loginForm.dbFieldText = dbName
                workspace.loginForm.db = DB(db.client, dbName)
                // TODO: test if this actually reinitialises the workspace screen
                navigator.pushState(WorkspaceScreenState(workspace.loginForm))
            },
            allDBNames = remember { db.client.listDatabases() },
            onOpen = { openOpenQueryDialog() },
            onSave = { openSaveQueryDialog() },
            onRun = {
                typeDBForceSimulation.init()
                dataStream = db.matchQuery(query = activeQueryTab.query, enableReasoning = querySettings.enableReasoning)
                visualiserWorldOffset = visualiserSize.center
                visualiserMetricsID = UUID.randomUUID().toString()
                queryStartTimeNanos = System.nanoTime()
                selectedVertex = null
                selectedVertexNetwork.clear()
            },
            onLogout = {
                db.client.closeAllSessions()
                navigator.pushState(workspace.loginForm)
            })

        Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

        Row(modifier = Modifier.weight(1F)) {
//            Column(modifier = Modifier.width(20.dp)) {
//                StudioTabs(orientation = TabOrientation.BOTTOM_TO_TOP) {
//                    StudioTab("Schema Explorer", selected = false, leadingIcon = { StudioIcon(Icon.Layout) })
//                    StudioTab("Permissions", selected = false, leadingIcon = { StudioIcon(Icon.Shield) })
//                }
//                Row(modifier = Modifier.weight(1f)) {}
//                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
//            }
//
//            Column(modifier = Modifier.fillMaxHeight().width(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

            Column(modifier = Modifier.fillMaxHeight().weight(1F)) {
                StudioTabs(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                    queryTabs.mapIndexed { index: Int, queryTab: QueryTabState ->
                        val selected = index == activeQueryTabIndex
                        StudioTab(text = queryTab.title, selected = selected, highlight = TabHighlight.BOTTOM,
                            showCloseButton = true, modifier = Modifier.clickable {
                                activeQueryTabIndex = index.coerceAtMost(queryTabs.size - 1)
                            }, onClose = {
                                queryTabs.removeAt(index)
                                if (queryTabs.isEmpty()) { addNewQueryTab() }
                                activeQueryTabIndex = index.coerceAtMost(queryTabs.size - 1)
                            })
                    }
                    Spacer(Modifier.width(6.dp))
                    StudioIcon(icon = Icon.Plus, size = Size14, modifier = Modifier.clickable {
                        addNewQueryTab()
                        activeQueryTabIndex = queryTabs.size - 1
                    })
                }

                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

                CodeEditor(code = activeQueryTab.query, editorID = activeQueryTab.editorID,
                    onChange = { value -> activeQueryTab.query = value },
                    font = StudioTheme.typography.codeEditorSwing,
                    modifier = Modifier.fillMaxWidth().height(112.dp).background(StudioTheme.colors.editorBackground))

                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

                PanelHeader(modifier = Modifier.fillMaxWidth()) {
                    StudioTabs(modifier = Modifier.weight(1f)) {
                        Spacer(Modifier.width(8.dp))
                        Text("Output", style = StudioTheme.typography.body2)
                        Spacer(Modifier.width(8.dp))

//                        executionTabs.mapIndexed { index: Int, executionTab: ExecutionTabState ->
//                            val selected = index == activeExecutionTabIndex
//                            StudioTab(text = executionTab.title, selected = selected,
//                                highlight = TabHighlight.BOTTOM, showCloseButton = true)
//                        }
                    }

//                    StudioIcon(Icon.Cog)
//                    Spacer(Modifier.width(12.dp))

                    // TODO: This should be a "maximise" icon allowing the Output panel to go full-screen
//                    StudioIcon(Icon.Minus)
//                    Spacer(Modifier.width(12.dp))
                }

                Row(modifier = Modifier.weight(1F)) {
                    TypeDBVisualiser(modifier = Modifier.fillMaxSize().onGloballyPositioned { visualiserSize = it.size.toSize() / devicePixelRatio },
                        vertices = typeDBForceSimulation.data.vertices, edges = typeDBForceSimulation.data.edges,
                        hyperedges = typeDBForceSimulation.data.hyperedges,
                        vertexExplanations = typeDBForceSimulation.data.vertexExplanations, theme = visualiserTheme,
                        metrics = SimulationMetrics(id = visualiserMetricsID, worldOffset = visualiserWorldOffset, devicePixelRatio),
                        onZoom = { value -> visualiserScale = value },
                        explain = { vertex -> db.explainConcept(vertex.id) },
                        selectedVertex = selectedVertex,
                        onSelectVertex = ::onSelectVertex,
                        selectedVertexNetwork = selectedVertexNetwork,
                        onVertexDragStart = ::onVertexDragStart,
                        onVertexDragMove = ::onVertexDragMove,
                        onVertexDragEnd = ::onVertexDragEnd)
                }

//                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
//
//                StudioTabs(modifier = Modifier.fillMaxWidth().height(26.dp)) {
//                    StudioTab("Log", selected = false, highlight = TabHighlight.TOP,
//                        leadingIcon = { StudioIcon(Icon.HorizontalBarChartDesc) })
//                    StudioTab("Graph", selected = true, highlight = TabHighlight.TOP,
//                        leadingIcon = { StudioIcon(Icon.Graph) })
//                    StudioTab("Table", selected = false, highlight = TabHighlight.TOP,
//                        leadingIcon = { StudioIcon(Icon.Table) })
//                }

                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
            }

            if (showQuerySettingsPanel || showConceptPanel) {
                Column(modifier = Modifier.fillMaxHeight().width(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

                Column(modifier = Modifier.fillMaxHeight().requiredWidth(285.dp).background(StudioTheme.colors.background)) {
                    if (showQuerySettingsPanel) {
                        QuerySettingsPanel(settings = querySettings, onSettingsChange = { querySettings = it },
                            onCollapse = { showQuerySettingsPanel = false }, modifier = Modifier.weight(1f))
                    }
                    if (showConceptPanel) {
                        ConceptPanel(vertex = selectedVertex, onCollapse = { showConceptPanel = false },
                            modifier = Modifier.weight(2f))
                    }
                }
            }

            Column(modifier = Modifier.fillMaxHeight().width(1.dp).background(StudioTheme.colors.uiElementBorder).zIndex(20f)) {}

            Column(modifier = Modifier.width(20.dp)) {
                Box(Modifier.height(76.dp).background(if (showQuerySettingsPanel) StudioTheme.colors.backgroundHighlight else StudioTheme.colors.background)
                    .clickable { showQuerySettingsPanel = !showQuerySettingsPanel }) {
                    StudioIcon(Icon.Cog, modifier = Modifier.offset(x = 4.dp, y = 8.dp))
                    Text("Settings", maxLines = 1, style = StudioTheme.typography.body2, modifier = Modifier
                        .offset(y = 40.dp)
                        .requiredWidth(IntrinsicSize.Max)
                        .rotate(90f))
                }
                Box(Modifier.height(76.dp).background(if (showConceptPanel) StudioTheme.colors.backgroundHighlight else StudioTheme.colors.background)
                    .clickable { showConceptPanel = !showConceptPanel }) {
                    StudioIcon(Icon.SearchAround, modifier = Modifier.offset(x = 4.dp, y = 8.dp))
                    Text("Concept", maxLines = 1, style = StudioTheme.typography.body2, modifier = Modifier
                        .offset(y = 40.dp)
                        .requiredWidth(IntrinsicSize.Max)
                        .rotate(90f))
                }
                // TODO: We really need to make StudioTabs work - these hardcoded-height boxes are a dirty hack
                //       that will collapse the moment the font size is customised
//                StudioTabs(orientation = TabOrientation.TOP_TO_BOTTOM) {
//                    StudioTab("Settings", selected = showQuerySettingsPanel, leadingIcon = { StudioIcon(Icon.Cog) })
//                    StudioTab("Concept", selected = showConceptPanel, leadingIcon = { StudioIcon(Icon.SearchAround) })
//                }
                Row(modifier = Modifier.weight(1f)) {}
                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
            }
        }

        StatusBar(dataStream = dataStream, visualiserScale = visualiserScale,
            vertexCount = typeDBForceSimulation.data.vertices.size, edgeCount = typeDBForceSimulation.data.edges.size,
            queryStartTimeNanos = queryStartTimeNanos)

        LaunchedEffect(key1 = dataStream) {
            simulationRunnerCoroutine(typeDBForceSimulation, dataStream, snackbarHostState, snackbarCoroutineScope)
        }
    }
}
