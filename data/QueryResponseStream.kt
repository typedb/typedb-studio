package com.vaticle.typedb.studio.data

import com.vaticle.typedb.common.collection.Either

class QueryResponseStream(@Volatile var completed: Boolean = false) {

    var lastFetchedNanos: Long = 0
    private var vertexStore: MutableList<VertexData> = mutableListOf()
    private var edgeStore: MutableList<EdgeData> = mutableListOf()
    private var exception: Exception? = null

    @Synchronized
    fun putVertex(vertex: VertexData) {
        if (!completed) {
//            println("Adding a vertex! ${vertex.label}")
            vertexStore += vertex
        }
    }

    @Synchronized
    fun putEdge(edge: EdgeData) {
        if (!completed) {
//            println("Adding an edge! (${edge.source} -> ${edge.target})")
            edgeStore += edge
        }
    }

    @Synchronized
    fun putError(exception: Exception) {
        if (this.completed) return
        this.exception = exception
    }

    @Synchronized
    fun drain(): Either<GraphData, Exception> {
        return when (exception) {
            null -> {
                // TODO: try simply emptying the lists rather than re-assigning them, though I'm fairly sure it won't work
                val data = GraphData(vertexStore, edgeStore)
                vertexStore = mutableListOf()
                edgeStore = mutableListOf()
                println("QueryResponseStream.drain: Fetched ${data.vertices.size} vertices and ${data.edges.size} edges")
                if (data.vertices.isNotEmpty()) println("Vertex IDs: ${data.vertices.map { it.id }}")
                Either.first(data)
            }
            else -> {
                completed = true
                vertexStore = mutableListOf()
                edgeStore = mutableListOf()
                Either.second(exception)
            }
        }
    }

    fun isEmpty(): Boolean {
        return vertexStore.isEmpty() && edgeStore.isEmpty()
    }

    companion object {
        val EMPTY = QueryResponseStream(completed = true)
    }
}
