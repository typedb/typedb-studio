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

import androidx.compose.ui.geometry.Offset
import com.vaticle.force.graph.CenterForce
import com.vaticle.force.graph.CollideForce
import com.vaticle.force.graph.ForceSimulation
import com.vaticle.force.graph.Link
import com.vaticle.force.graph.LinkForce
import com.vaticle.force.graph.ManyBodyForce
import com.vaticle.force.graph.Node
import com.vaticle.force.graph.RandomEffects
import com.vaticle.force.graph.XForce
import com.vaticle.force.graph.YForce
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

class TypeDBForceSimulation(val data: GraphState = GraphState()) : ForceSimulation() {

    var lastTickStartNanos: Long = 0
    var isStarted = false
    private val edgeBandsByEndpoints: MutableMap<Pair<Int, Int>, MutableSet<Int>> = mutableMapOf()
    private val edgeBandsByEdge: MutableMap<Int, Pair<Int, Int>> = mutableMapOf()
    private val nextHyperedgeNodeID = AtomicInteger(-1)
    private val vertexNodes: MutableCollection<Node> = mutableListOf()
    val hyperedgeNodes: MutableMap<Int, Node> = mutableMapOf()

    fun init() {
        clear()
        addNodes(data.vertices.map { InputNode(it.id) })
        force("center", CenterForce(nodes().values, 0.0, 0.0))
        force("collide", CollideForce(vertexNodes, 80.0))
        force("charge", ManyBodyForce(vertexNodes, -100.0))
        force("x", XForce(vertexNodes, 0.0, 0.05))
        force("y", YForce(vertexNodes, 0.0, 0.05))
        force("hyperedgeCollide", CollideForce(hyperedgeNodes.values, 40.0))
        alpha(1.0)
        alphaTarget(0.0)
        alphaMin(0.01)

        isStarted = true
        lastTickStartNanos = 0
    }

    fun isEmpty(): Boolean {
        return nodes().isEmpty()
    }

    override fun tick() {
        super.tick()
        data.vertices.forEach {
            val node = nodes()[it.id]
                ?: throw IllegalStateException("Received bad simulation data: no entry received for vertex ID ${it.id}!")
            it.position = Offset(node.x().toFloat(), node.y().toFloat())
        }
        val verticesByID: Map<Int, VertexState> = data.vertices.associateBy { it.id }
        data.edges.forEach {
            it.sourcePosition = verticesByID[it.sourceID]!!.position
            it.targetPosition = verticesByID[it.targetID]!!.position
        }
        val hyperedgeNodesByNodeID: Map<Int, Node> = hyperedgeNodes.values.associateBy { it.index() }
        data.hyperedges.forEach {
            val hyperedgeNode = hyperedgeNodesByNodeID[it.hyperedgeNodeID]
                ?: throw IllegalStateException("Received bad simulation data: no hyperedge node found with ID ${it.hyperedgeNodeID}!")
            it.position = Offset(hyperedgeNode.x().toFloat(), hyperedgeNode.y().toFloat())
        }
    }

    override fun clear() {
        super.clear()
        isStarted = false
        data.clear()
        vertexNodes.clear()
        edgeBandsByEndpoints.clear()
        edgeBandsByEdge.clear()
        hyperedgeNodes.clear()
        nextHyperedgeNodeID.set(-1)
    }

    fun addVertices(vertices: List<VertexState>) {
        if (vertices.isEmpty()) return
        data.vertices += vertices
        val newVertices = vertices.map { InputNode(it.id) }
        val addedNodes = addNodes(newVertices)
        vertexNodes += addedNodes
    }

    fun addEdges(edges: List<EdgeState>) {
        if (edges.isEmpty()) return
        data.edges += edges
        val links: MutableCollection<Link> = mutableListOf()
        data.edges.forEach {
            val sourceNode = nodes()[it.sourceID]
            val targetNode = nodes()[it.targetID]
            if (sourceNode != null && targetNode != null) links += Link(nodes()[it.sourceID], nodes()[it.targetID])
            else println("addEdges: Could not create link force for $it because one of its nodes is not in the simulation!")
        }
        force("link", LinkForce(vertexNodes, links, 90.0, 0.5))
        force("charge", ManyBodyForce(vertexNodes, -600.0 * data.edges.size / (data.vertices.size + 1)))

        val edgesBySource = data.edges.groupBy { it.sourceID }
        edges.forEach { edge ->
            val edgeBand = (edgesBySource.getOrDefault(edge.sourceID, listOf()).filter { it.targetID == edge.targetID }
                + edgesBySource.getOrDefault(edge.targetID, listOf()).filter { it.targetID == edge.sourceID })
            if (edgeBand.size > 1) {
                edgeBand.forEach(::addEdgeBandMember)
            }
        }
    }

    private fun addEdgeBandMember(edge: EdgeState) {
        // These operations are idempotent so we can safely repeatedly call this method on every edge band member
        val (endpoint1, endpoint2) = listOf(min(edge.sourceID, edge.targetID), max(edge.sourceID, edge.targetID))
        edgeBandsByEdge[edge.id] = Pair(endpoint1, endpoint2)
        edgeBandsByEndpoints.getOrPut(Pair(endpoint1, endpoint2)) { mutableSetOf() } += edge.id

        if (edge.id !in hyperedgeNodes) {
            val edgeMidpoint = edgeMidpoint(edge)
            val nodeID = nextHyperedgeNodeID.getAndAdd(-1)
            val node = Node(nodeID, edgeMidpoint.x, edgeMidpoint.y)
            nodes()[nodeID] = node
            hyperedgeNodes[edge.id] = node

            val placementOffset = RandomEffects.jiggle()
            force("x_$nodeID", XForce(listOf(node), { edgeMidpoint(edge).x + placementOffset }, 0.35))
            force("y_$nodeID", YForce(listOf(node), { edgeMidpoint(edge).y + placementOffset }, 0.35))
            data.hyperedges += HyperedgeState(edge.id, nodeID)
        }
    }

    private fun edgeMidpoint(edge: EdgeState): Point {
        val node1 = nodes()[edge.sourceID]
        val node2 = nodes()[edge.targetID]
        if (node1 == null || node2 == null)
            throw IllegalStateException("edgeMidpoint: Either the source or the target node is not present in the simulation!")
        return Point((node1.x() + node2.x()) / 2, (node1.y() + node2.y()) / 2)
    }

    fun addVertexExplanations(vertexExplanations: List<VertexExplanationState>) {
        if (vertexExplanations.isEmpty()) return
        data.vertexExplanations += vertexExplanations
    }
}

data class Point(val x: Double, val y: Double)
