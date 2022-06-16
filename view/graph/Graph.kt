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

package com.vaticle.typedb.studio.view.graph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.vaticle.force.graph.api.Simulation
import com.vaticle.force.graph.force.CenterForce
import com.vaticle.force.graph.force.CollideForce
import com.vaticle.force.graph.force.LinkForce
import com.vaticle.force.graph.force.ManyBodyForce
import com.vaticle.force.graph.force.XForce
import com.vaticle.force.graph.force.YForce
import com.vaticle.force.graph.impl.BasicSimulation
import com.vaticle.force.graph.impl.BasicVertex
import com.vaticle.force.graph.util.RandomEffects
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.logic.Explanation
import com.vaticle.typedb.studio.view.graph.Graph.Physics.Constants.COLLIDE_RADIUS
import com.vaticle.typedb.studio.view.graph.Graph.Physics.Constants.CURVE_COLLIDE_RADIUS
import com.vaticle.typedb.studio.view.graph.Graph.Physics.Constants.CURVE_COMPRESSION_POWER
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

class Graph(private val interactions: Interactions) {

    private val _thingVertices: MutableMap<String, Vertex.Thing> = ConcurrentHashMap()
    private val _typeVertices: MutableMap<String, Vertex.Type> = ConcurrentHashMap()
    private val _edges: MutableList<Edge> = Collections.synchronizedList(mutableListOf())

    val thingVertices: Map<String, Vertex.Thing> get() = _thingVertices
    val typeVertices: Map<String, Vertex.Type> get() = _typeVertices
    val vertices: Collection<Vertex> get() = thingVertices.values + typeVertices.values
    val edges: Collection<Edge> get() = _edges

    val physics = Physics(this, interactions)
    val reasoning = Reasoning()

    fun putThingVertex(iid: String, vertex: Vertex.Thing) {
        putVertex(iid, _thingVertices, vertex)
    }

    fun putTypeVertex(label: String, vertex: Vertex.Type) {
        putVertex(label, _typeVertices, vertex)
    }

    private fun <VERTEX : Vertex> putVertex(key: String, vertexMap: MutableMap<String, VERTEX>, vertex: VERTEX) {
        vertexMap[key] = vertex
        physics.placeVertex(vertex.geometry)
        onChange()
    }

    fun addEdge(edge: Edge) {
        _edges += edge
        onChange()
    }

    fun makeEdgeCurved(edge: Edge) {
        physics.addCurveProvider(edge)
    }

    private fun onChange() {
        physics.addEnergy()
        interactions.rebuildFocusedVertexNetwork()
    }

    fun isEmpty() = vertices.isEmpty()
    fun isNotEmpty() = vertices.isNotEmpty()

    class Physics(val graph: Graph, private val interactions: Interactions) {

        private object Constants {
            const val COLLIDE_RADIUS = 65.0
            const val CURVE_COMPRESSION_POWER = 0.35
            const val CURVE_COLLIDE_RADIUS = 35.0
        }

        private val simulation = BasicSimulation().apply {
            alphaMin = 0.01
            alphaDecay = standardAlphaDecay()
        }
        var iteration by mutableStateOf(0L)
        private val isStable get() = simulation.alpha < simulation.alphaMin
        val isStepRunning = AtomicBoolean(false)
        var alpha: Double
            get() = simulation.alpha
            set(value) {
                simulation.alpha = value
            }
        val drag = Drag(this)
        var forceSource = ForceSource.Query
        private var edgeCurveProviders = ConcurrentHashMap<Edge, com.vaticle.force.graph.api.Vertex>()

        fun step() {
            if (isStable || graph.isEmpty()) return
            if (isStepRunning.compareAndSet(false, true)) {
                when (forceSource) {
                    ForceSource.Query -> initialiseForces()
                    ForceSource.Drag -> drag.initialiseForces(interactions.draggedVertex)
                }
                simulation.tick()
                iteration++
                isStepRunning.set(false)
            }
        }

        fun placeVertex(vertex: com.vaticle.force.graph.api.Vertex) {
            simulation.placeVertex(vertex)
        }

        fun addCurveProvider(edge: Edge) {
            edgeCurveProviders.computeIfAbsent(edge) {
                val basePosition = edge.geometry.midpoint
                val offset = RandomEffects.jiggle()
                edge.curvePoint = BasicVertex(basePosition.x + offset, basePosition.y + offset)
                edge.curvePoint!!
            }
        }

        fun addEnergy() {
            simulation.alpha = 1.0
        }

        fun terminate() {
            simulation.alpha = 0.0
        }

        fun Simulation.standardAlphaDecay(): Double {
            return 1 - alphaMin.pow(1.0 / 200)
        }

        private fun initialiseForces() {
            simulation.apply {
                forces.clear()
                localForces.clear()
                initialiseGlobalForces()
                initialiseLocalForces()
            }
        }

        private fun Simulation.initialiseGlobalForces() {
            // TODO: track changes to vertices + edges, rebuild forces only if there are changes
            val vertices = graph.vertices.map { it.geometry }
            val edges = graph.edges.map { it.geometry }
            forces.add(CenterForce(vertices, 0.0, 0.0))
            forces.add(CollideForce(vertices, COLLIDE_RADIUS))
            forces.add(ManyBodyForce(
                vertices, ((-500.0 - vertices.size / 3) * (1 + edges.size / (vertices.size + 1))) * 10
            ))
            // TODO: fix force-graph so that we can have a single LinkForce with strength = 4.0
            forces.add(LinkForce(vertices, edges, 90.0, 1.0))
            forces.add(LinkForce(vertices, edges, 90.0, 1.0))
            forces.add(LinkForce(vertices, edges, 90.0, 1.0))
            forces.add(LinkForce(vertices, edges, 90.0, 1.0))
            val gravityStrength = 0.5
            forces.add(XForce(vertices, 0.0, gravityStrength))
            forces.add(YForce(vertices, 0.0, gravityStrength))
        }

        private fun Simulation.initialiseLocalForces() {
            localForces.add(CollideForce(edgeCurveProviders.values.toList(), CURVE_COLLIDE_RADIUS))
            edgeCurveProviders.forEach { (edge, curveProvider) ->
                localForces.add(
                    XForce(listOf(curveProvider), edge.geometry.midpoint.x.toDouble(), CURVE_COMPRESSION_POWER)
                )
                localForces.add(
                    YForce(listOf(curveProvider), edge.geometry.midpoint.y.toDouble(), CURVE_COMPRESSION_POWER)
                )
            }
        }

        enum class ForceSource { Query, Drag }

        class Drag(private val physics: Physics) {

            private val graph = physics.graph
            private val simulation = physics.simulation
            private var verticesFrozenWhileDragging: Collection<Vertex> by mutableStateOf(listOf())

            fun onDragStart(vertex: Vertex) {
                freezePermanently(vertex)
                physics.forceSource = ForceSource.Drag
                simulation.apply {
                    alpha = 0.25
                    alphaDecay = 0.0
                }
            }

            fun onDragMove(vertex: Vertex, dragDistance: Offset) {
                vertex.geometry.position += dragDistance
            }

            fun onDragEnd() {
                with(physics) { simulation.alphaDecay = simulation.standardAlphaDecay() }
                releaseFrozenVertices()
            }

            fun initialiseForces(draggedVertex: Vertex?) {
                simulation.apply {
                    forces.clear()
                    localForces.clear()

                    val vertices = graph.vertices.map { it.geometry }
                    forces.add(CollideForce(vertices, COLLIDE_RADIUS))
                    if (draggedVertex is Vertex.Thing && draggedVertex.isRoleplayer()) {
                        pullLinkedRelations(draggedVertex)
                    }
                    with(physics) { initialiseLocalForces() }
                }
            }

            private fun Simulation.pullLinkedRelations(vertex: Vertex.Thing) {
                val attributeEdges = vertex.ownedAttributeEdges()
                val attributeVertices = attributeEdges.map { it.target }
                val relationVertices = vertex.playingRoleEdges().map { it.source }
                val roleplayerEdges = relationVertices.flatMap { it.roleplayerEdges() }
                val roleplayerVertices = roleplayerEdges.map { it.target }
                val vertices = (attributeVertices + relationVertices + roleplayerVertices).toSet()
                val edges = attributeEdges + roleplayerEdges

                freezeUntilDragEnd(roleplayerVertices.filter { it != vertex && !it.geometry.isFrozen })
                forces.add(LinkForce(vertices.map { it.geometry }, edges.map { it.geometry }, 90.0, 0.25))
            }

            private fun freezePermanently(vertex: Vertex) {
                vertex.geometry.isFrozen = true
            }

            private fun freezeUntilDragEnd(vertices: Collection<Vertex>) {
                vertices.forEach { it.geometry.isFrozen = true }
                verticesFrozenWhileDragging = vertices
            }

            private fun releaseFrozenVertices() {
                verticesFrozenWhileDragging.forEach { it.geometry.isFrozen = false }
                verticesFrozenWhileDragging = listOf()
            }
        }
    }

    class Reasoning {

        val explainables: MutableMap<Vertex.Thing, ConceptMap.Explainable> = ConcurrentHashMap()
        val explanationIterators: MutableMap<Vertex.Thing, Iterator<Explanation>> = ConcurrentHashMap()
        private val vertexExplanations =
            Collections.synchronizedList(mutableListOf<Pair<Vertex.Thing, Explanation>>())

        var explanationsByVertex: Map<Vertex.Thing, Set<Explanation>> = emptyMap(); private set

        fun addVertexExplanations(vertexExplanations: Iterable<Pair<Vertex.Thing, Explanation>>) {
            synchronized(vertexExplanations) {
                this.vertexExplanations += vertexExplanations
                rebuildIndexes()
            }
        }

        private fun rebuildIndexes() {
            explanationsByVertex = vertexExplanations
                .groupBy({ it.first }) { it.second }
                .mapValues { it.value.toSet() }
        }
    }
}