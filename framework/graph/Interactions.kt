/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.graph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.vaticle.typedb.driver.api.logic.Explanation

class Interactions constructor(private val graphArea: GraphArea) {

    var pointerPosition: Offset? by mutableStateOf(null)
    var hoveredVertex: Vertex? by mutableStateOf(null)
    val hoveredVertexChecker = HoveredVertexChecker(graphArea)
    var hoveredVertexExplanations: Set<Explanation> by mutableStateOf(emptySet())

    private var _focusedVertex: Vertex? by mutableStateOf(null)
    var focusedVertex: Vertex?
        get() = _focusedVertex
        set(value) {
            rebuildFocusedVertexNetwork(value)
            _focusedVertex = value
        }
    var focusedVertexNetwork: Set<Vertex> by mutableStateOf(emptySet())

    var draggedVertex: Vertex? by mutableStateOf(null)

    // Logically, if the vertex is dragged, it should also be hovered; however, this is not always true
    // because the vertex takes some time to "catch up" to the pointer. So check both conditions.
    val Vertex.isHovered get() = this == hoveredVertex || this == draggedVertex

    val Vertex.isBackground get() = focusedVertex != null && this !in focusedVertexNetwork

    val Edge.isBackground get() = focusedVertex != null && source != focusedVertex && target != focusedVertex

    fun rebuildFocusedVertexNetwork(focusedVertex: Vertex? = _focusedVertex) {
        focusedVertexNetwork = if (focusedVertex != null) {
            val linkedVertices = graphArea.graph.edges.mapNotNull { edge ->
                when (focusedVertex) {
                    edge.source -> edge.target
                    edge.target -> edge.source
                    else -> null
                }
            }
            setOf(focusedVertex) + linkedVertices
        } else emptySet()
    }

    class HoveredVertexChecker constructor(private val graphArea: GraphArea) : BackgroundTask(runIntervalMs = 33) {

        private val interactions get() = graphArea.interactions

        override fun canRun(): Boolean {
            return interactions.pointerPosition != null
        }

        override fun run() {
            val hoveredVertex = interactions.pointerPosition?.let { graphArea.viewport.findVertexAt(it, interactions) }
            if (interactions.hoveredVertex == hoveredVertex) return
            interactions.hoveredVertex = hoveredVertex
            interactions.hoveredVertexExplanations = when (hoveredVertex) {
                null -> emptySet()
                else -> graphArea.graph.reasoning.explanationsByVertex[hoveredVertex] ?: emptySet()
            }
        }
    }
}
