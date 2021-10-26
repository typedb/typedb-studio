package com.vaticle.typedb.studio.data

import com.vaticle.typedb.client.api.concept.Concept

data class GraphData(val vertices: List<VertexData> = listOf(),
                     val edges: List<EdgeData> = listOf(),
                     val explanationVertices: List<ExplanationVertexData> = listOf(),
                     val explanationEdges: List<ExplanationEdgeData> = listOf())

data class VertexData(val concept: Concept, val id: Int, val encoding: VertexEncoding, val label: String, val shortLabel: String,
                      val width: Float, val height: Float, val inferred: Boolean = false)

enum class VertexEncoding(val displayName: String) {
    ENTITY_TYPE("Entity Type"),
    RELATION_TYPE("Relation Type"),
    ATTRIBUTE_TYPE("Attribute Type"),
    THING_TYPE("Thing Type"),
    ENTITY("Entity"),
    RELATION("Relation"),
    ATTRIBUTE("Attribute"),
}

data class EdgeData(val id: Int, val source: Int, val target: Int, val encoding: EdgeEncoding, val label: String,
                    val inferred: Boolean = false)

enum class EdgeEncoding {
    // Type edges
    SUB,
    OWNS,
    PLAYS,

    // Thing edges
    HAS,
    ROLEPLAYER,

    // Thing-to-type edges
    ISA,
}

data class IncompleteEdgeData(val id: Int, val vertexID: Int, val direction: EdgeDirection, val encoding: EdgeEncoding,
                              val label: String, val inferred: Boolean = false)

enum class EdgeDirection {
    OUTGOING,
    INCOMING
}

data class ExplanationVertexData(val explanationID: Int, val vertexID: Int)

data class ExplanationEdgeData(val explanationID: Int, val edgeID: Int)
