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
    get() {
        return Rect(position - Offset(width, height) / 2F, Size(width, height))
    }

    companion object {
        fun of(data: VertexData): VertexState {
            return VertexState(data.concept, data.id, data.encoding, data.label, data.shortLabel, data.width, data.height, data.inferred)
        }
    }
}

data class EdgeState(val id: Int, val sourceID: Int = -1, val targetID: Int = -1, val encoding: EdgeEncoding, val label: String, val inferred: Boolean) {
    var sourcePosition: Offset by mutableStateOf(Offset(0F, 0F))
    var targetPosition: Offset by mutableStateOf(Offset(0F, 0F))

    companion object {
        fun of(data: EdgeData): EdgeState {
            return EdgeState(data.id, data.source, data.target, data.encoding, data.label, data.inferred)
        }
    }
}

data class HyperedgeState(val edgeID: Int, val hyperedgeNodeID: Int) {
    var position: Offset by mutableStateOf(Offset(0F, 0F))
}

data class VertexExplanationState(val vertexID: Int, val explanationID: Int) {
    companion object {
        fun of(data: ExplanationVertexData): VertexExplanationState {
            return VertexExplanationState(data.vertexID, data.explanationID)
        }
    }
}
