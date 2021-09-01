package com.vaticle.typedb.studio.visualiser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.vaticle.typedb.studio.data.EdgeData
import com.vaticle.typedb.studio.data.Highlight
import com.vaticle.typedb.studio.data.VertexData
import com.vaticle.typedb.studio.data.VertexEncoding

class GraphState {
    var vertices: SnapshotStateList<VertexState> = mutableStateListOf()
        private set
    var edges: SnapshotStateList<EdgeState> = mutableStateListOf()
        private set

    fun clear() {
        edges.clear()
        vertices.clear()
    }
}

data class VertexState(val id: Int, val encoding: VertexEncoding, val label: String, val shortLabel: String,
                       val width: Float, val height: Float, val highlight: Highlight) {
    var position: Offset by mutableStateOf(Offset(0F, 0F))

    val rect: Rect
    get() {
        return Rect(position - Offset(width, height) / 2F, Size(width, height))
    }

    companion object {
        fun of(data: VertexData): VertexState {
            return VertexState(data.id, data.encoding, data.label, data.shortLabel, data.width, data.height, data.highlight)
        }
    }
}

data class EdgeState(val sourceID: Int = -1, val targetID: Int = -1, val label: String, val highlight: Highlight) {
    var sourcePosition: Offset by mutableStateOf(Offset(0F, 0F))
    var targetPosition: Offset by mutableStateOf(Offset(0F, 0F))

    companion object {
        fun of(data: EdgeData): EdgeState {
            return EdgeState(data.source, data.target, data.label, data.highlight)
        }
    }
}
