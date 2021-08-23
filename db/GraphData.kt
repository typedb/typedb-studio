package com.vaticle.typedb.studio.db

data class GraphData(val vertices: List<VertexData> = listOf(), val edges: List<EdgeData> = listOf())

data class VertexData(val id: Int, val encoding: VertexEncoding, val label: String, val width: Float, val height: Float)

enum class VertexEncoding {
    ENTITY_TYPE,
    RELATION_TYPE,
    ATTRIBUTE_TYPE,
    THING_TYPE,
    ENTITY,
    RELATION,
    ATTRIBUTE,
}

data class EdgeData(val id: Int, val source: Int, val target: Int, val label: String,
                    val highlight: EdgeHighlight = EdgeHighlight.NONE)

data class IncompleteEdgeData(val id: Int, val vertexID: Int, val direction: EdgeDirection, val label: String,
                              val highlight: EdgeHighlight = EdgeHighlight.NONE)

enum class EdgeHighlight {
    NONE,
    INFERRED,
}

enum class EdgeDirection {
    OUTGOING,
    INCOMING
}
