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

package com.vaticle.typedb.studio.visualiser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.studio.data.EdgeData
import com.vaticle.typedb.studio.data.EdgeEncoding
import com.vaticle.typedb.studio.data.ExplanationVertexData
import com.vaticle.typedb.studio.data.VertexData
import com.vaticle.typedb.studio.data.VertexEncoding

class GraphState {
    var vertices: SnapshotStateList<VertexState> = mutableStateListOf()
        private set
    var edges: SnapshotStateList<EdgeState> = mutableStateListOf()
        private set
    var hyperedges: SnapshotStateList<HyperedgeState> = mutableStateListOf()
        private set
    // TODO: try changing this to a SnapshotStateMap
    var vertexExplanations: SnapshotStateList<VertexExplanationState> = mutableStateListOf()
        private set

    fun clear() {
        hyperedges.clear()
        edges.clear()
        vertexExplanations.clear()
        vertices.clear()
    }
}

data class VertexState(val concept: Concept, val id: Int, val encoding: VertexEncoding, val label: String, val shortLabel: String,
                       val width: Float, val height: Float, val inferred: Boolean) {
    var position: Offset by mutableStateOf(Offset(0F, 0F))

    val rect: Rect
    get() = Rect(position - Offset(width, height) / 2F, Size(width, height))
}

fun vertexStateOf(data: VertexData): VertexState {
    return VertexState(data.concept, data.id, data.encoding, data.label, data.shortLabel, data.width, data.height, data.inferred)
}

data class EdgeState(val id: Int, val sourceID: Int = -1, val targetID: Int = -1, val encoding: EdgeEncoding, val label: String, val inferred: Boolean) {
    var sourcePosition: Offset by mutableStateOf(Offset(0F, 0F))
    var targetPosition: Offset by mutableStateOf(Offset(0F, 0F))
}

fun edgeStateOf(data: EdgeData): EdgeState {
    return EdgeState(data.id, data.source, data.target, data.encoding, data.label, data.inferred)
}

data class HyperedgeState(val edgeID: Int, val hyperedgeNodeID: Int) {
    var position: Offset by mutableStateOf(Offset(0F, 0F))
}

data class VertexExplanationState(val vertexID: Int, val explanationID: Int)

fun vertexExplanationStateOf(data: ExplanationVertexData): VertexExplanationState {
    return VertexExplanationState(data.vertexID, data.explanationID)
}
