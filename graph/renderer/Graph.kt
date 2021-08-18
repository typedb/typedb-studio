package com.vaticle.graph.renderer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.vaticle.force.graph.CenterForce
import com.vaticle.force.graph.CollideForce
import com.vaticle.force.graph.ForceSimulation
import com.vaticle.force.graph.Link
import com.vaticle.force.graph.LinkForce
import com.vaticle.force.graph.ManyBodyForce
import java.util.stream.Collectors

class Graph {
    var previousTimeNanos: Long = 0
    private val initialVertices: List<VertexData> = listOf(
        VertexData(id = 1, label = "person", width = 100F, height = 32F),
        VertexData(id = 2, label = "student", width = 100F, height = 32F),
        VertexData(id = 3, label = "teacher", width = 100F, height = 32F),
        VertexData(id = 4, label = "undergrad", width = 100F, height = 32F),
        VertexData(id = 5, label = "postgrad", width = 100F, height = 32F),
        VertexData(id = 6, label = "supervisor", width = 100F, height = 32F),
        VertexData(id = 7, label = "professor", width = 100F, height = 32F),
    )
    private val initialEdgeData: List<EdgeDTO> = listOf(
        EdgeDTO(source = 1, target = 2),
        EdgeDTO(source = 1, target = 3),
        EdgeDTO(source = 2, target = 4),
        EdgeDTO(source = 2, target = 5),
        EdgeDTO(source = 3, target = 6),
        EdgeDTO(source = 3, target = 7),
    )

    var vertices: SnapshotStateList<VertexData> = mutableStateListOf()
        private set
    var edges: SnapshotStateList<EdgeData> = mutableStateListOf()
        private set
    var simulation: ForceSimulation? = null

    init {
        vertices.addAll(initialVertices)
        edges.addAll(initialEdgeData.stream().map { EdgeData(it.source, it.target) }.collect(Collectors.toList()))
    }

    fun toForceSimulation(size: Size, scale: Float): ForceSimulation {
        val simulation = ForceSimulation()
        simulation.addNodes(vertices.map { ForceSimulation.InputNode(it.id) })
        return simulation
            .force("center", CenterForce(simulation.nodes().values, (size.width / 2).toDouble(), (size.height / 2).toDouble()))
            .force("link", LinkForce(simulation.nodes().values, edges.map { Link(simulation.nodes()[it.sourceID], simulation.nodes()[it.targetID]) }, 120.0 * scale))
            .force("collide", CollideForce(simulation.nodes().values, 80.0 * scale))
            .force("charge", ManyBodyForce(simulation.nodes().values, -500.0))
            .alphaMin(0.005)
    }

    fun updateEdgePositions(scale: Float) {
        val vertexPositions = vertices.stream().collect(Collectors.toMap({ it.id },
            { Offset(it.position.x + it.width * scale / 2, it.position.y + it.height * scale / 2) }))

        edges.forEach {
            it.sourcePosition = vertexPositions[it.sourceID] as Offset
            it.targetPosition = vertexPositions[it.targetID] as Offset
        }
    }
}

data class VertexData(val id: Int, val label: String, val width: Float, val height: Float) {
    var position: Offset by mutableStateOf(Offset(0F, 0F))
}

data class EdgeData(val sourceID: Int = -1, val targetID: Int = -1) {
    var sourcePosition: Offset by mutableStateOf(Offset(0F, 0F))
    var targetPosition: Offset by mutableStateOf(Offset(0F, 0F))
}

data class EdgeDTO(val source: Int, val target: Int)
