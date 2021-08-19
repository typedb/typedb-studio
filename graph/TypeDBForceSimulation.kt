package com.vaticle.graph

import androidx.compose.ui.geometry.Size
import com.vaticle.force.graph.CenterForce
import com.vaticle.force.graph.CollideForce
import com.vaticle.force.graph.ForceSimulation
import com.vaticle.force.graph.Link
import com.vaticle.force.graph.LinkForce
import com.vaticle.force.graph.ManyBodyForce

class TypeDBForceSimulation(val graph: TypeDBGraph = TypeDBGraph()): ForceSimulation() {

    var previousTimeNanos: Long = 0

    fun init(size: Size) {
        clear()
        addNodes(graph.vertices.map { InputNode(it.id) })
        this
            .force("center", CenterForce(nodes().values, (size.width / 2).toDouble(), (size.height / 2).toDouble()))
            .force("link", LinkForce(nodes().values, graph.edges.map { Link(nodes()[it.sourceID], nodes()[it.targetID]) }, 120.0))
            .force("collide", CollideForce(nodes().values, 80.0))
            .force("charge", ManyBodyForce(nodes().values, -500.0))
            .alpha(1.0)
            .alphaTarget(0.0)
            .alphaMin(0.005)
    }
}
