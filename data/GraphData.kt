/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
