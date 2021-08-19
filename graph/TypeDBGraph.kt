package com.vaticle.graph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import java.util.stream.Collectors

class TypeDBGraph {
    private val initialVertices: List<VertexData> = listOf(
        VertexData(id = 1, encoding = VertexEncoding.ENTITY, label = "person: Leonardo", width = 140F, height = 32F),
        VertexData(id = 2, encoding = VertexEncoding.RELATION, label = "cast", width = 105F, height = 66F),
        VertexData(id = 3, encoding = VertexEncoding.ENTITY, label = "movie: Titanic", width = 140F, height = 32F),
        VertexData(id = 4, encoding = VertexEncoding.ENTITY, label = "character: Jack", width = 140F, height = 32F),
        VertexData(id = 5, encoding = VertexEncoding.ATTRIBUTE, label = "billing: 1", width = 120F, height = 32F),
    )
    private val initialEdgeData: List<EdgeDTO> = listOf(
        EdgeDTO(source = 2, target = 1, label = "actor"),
        EdgeDTO(source = 2, target = 3, label = "movie"),
        EdgeDTO(source = 2, target = 4, label = "character"),
        EdgeDTO(source = 2, target = 5, label = "has"),
    )

    var vertices: SnapshotStateList<VertexData> = mutableStateListOf()
        private set
    var edges: SnapshotStateList<EdgeData> = mutableStateListOf()
        private set

    init {
        vertices.addAll(initialVertices)
        edges.addAll(initialEdgeData.stream().map { EdgeData(it.source, it.target, it.label) }.collect(Collectors.toList()))
    }
}

enum class VertexEncoding {
    ENTITY_TYPE,
    RELATION_TYPE,
    ATTRIBUTE_TYPE,
    THING_TYPE,
    ENTITY,
    RELATION,
    ATTRIBUTE
}

data class VertexData(val id: Int, val encoding: VertexEncoding, val label: String, val width: Float, val height: Float) {
    var position: Offset by mutableStateOf(Offset(0F, 0F))
}

data class EdgeData(val sourceID: Int = -1, val targetID: Int = -1, val label: String) {
    var sourcePosition: Offset by mutableStateOf(Offset(0F, 0F))
    var targetPosition: Offset by mutableStateOf(Offset(0F, 0F))
}

data class EdgeDTO(val source: Int, val target: Int, val label: String)
