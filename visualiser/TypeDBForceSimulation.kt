package com.vaticle.typedb.studio.visualiser

import com.vaticle.force.graph.CenterForce
import com.vaticle.force.graph.CollideForce
import com.vaticle.force.graph.ForceSimulation
import com.vaticle.force.graph.Link
import com.vaticle.force.graph.LinkForce
import com.vaticle.force.graph.ManyBodyForce

class TypeDBForceSimulation(val data: GraphState = GraphState()) : ForceSimulation() {

    var lastTickStartNanos: Long = 0
    var isStarted = false

    fun init() {
        clear()
        addNodes(data.vertices.map { InputNode(it.id) })
        force("center", CenterForce(nodes().values, 0.0, 0.0))
        force("link", LinkForce(nodes().values, data.edges.map { Link(nodes()[it.sourceID], nodes()[it.targetID]) }, 120.0,  0.5))
        force("collide", CollideForce(nodes().values, 80.0))
//        force("charge", ManyBodyForce(nodes().values) { forceEmitter, forceReceiver ->
//            when (data.edges.count { it.sourceID == forceReceiver.index() || it.targetID == forceReceiver.index() }) {
//                0 -> 1000.0
//                else -> -1000.0
//            }
//        })
        force("charge", ManyBodyForce(nodes().values, -100.0))
        alpha(1.0)
        alphaTarget(0.0)
        alphaMin(0.01)

        isStarted = true
        lastTickStartNanos = 0
    }

    fun isEmpty(): Boolean {
        return nodes().isEmpty()
    }

    override fun clear() {
        super.clear()
        data.clear()
    }

    fun addVertices(vertices: List<VertexState>) {
        if (vertices.isEmpty()) return
        data.vertices += vertices
        addNodes(vertices.map { InputNode(it.id) })
    }

    fun addEdges(edges: List<EdgeState>) {
        if (edges.isEmpty()) return
        data.edges += edges
        force("link", LinkForce(nodes().values, data.edges.map { Link(nodes()[it.sourceID], nodes()[it.targetID]) }, 120.0, 0.5))
        force("charge", ManyBodyForce(nodes().values, -500.0 * data.edges.size / (data.vertices.size + 1)))
        println("New charge strength = ${-200.0 * data.edges.size / (data.vertices.size + 1)}")
    }
}
