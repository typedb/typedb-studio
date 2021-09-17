package com.vaticle.typedb.studio.data

import com.vaticle.typedb.common.collection.Either
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.StampedLock

class QueryResponseStream(completed: Boolean = false) {

    @Volatile
    var completed: Boolean = completed
    set(value) {
        field = value
        if (value) queryEndTimeNanos = System.nanoTime()
    }

    var lastFetchedNanos: Long = 0
    private var accessLock: ReadWriteLock = StampedLock().asReadWriteLock()
    private var vertexStore: MutableList<VertexData> = mutableListOf()
    private var edgeStore: MutableList<EdgeData> = mutableListOf()
    private var explanationVertexStore: MutableList<ExplanationVertexData> = mutableListOf()
    private var explanationEdgeStore: MutableList<ExplanationEdgeData> = mutableListOf()
    private var exception: Exception? = null
    var queryEndTimeNanos: Long? = null

    fun putVertex(vertex: VertexData) {
        try {
            accessLock.readLock().lock()
            vertexStore += vertex
        } finally {
            accessLock.readLock().unlock()
        }
    }

    fun putEdge(edge: EdgeData) {
        try {
            accessLock.readLock().lock()
            edgeStore += edge
        } finally {
            accessLock.readLock().unlock()
        }
    }

    fun putExplanationVertex(explanationVertex: ExplanationVertexData) {
        try {
            accessLock.readLock().lock()
            explanationVertexStore += explanationVertex
        } finally {
            accessLock.readLock().unlock()
        }
    }

    fun putExplanationEdge(explanationEdge: ExplanationEdgeData) {
        try {
            accessLock.readLock().lock()
            explanationEdgeStore += explanationEdge
        } finally {
            accessLock.readLock().unlock()
        }
    }

    fun putError(exception: Exception) {
        try {
            accessLock.readLock().lock()
            this.exception = exception
            this.completed = true
        } finally {
            accessLock.readLock().unlock()
        }
    }

    fun drain(): Either<GraphData, Exception> {
        try {
            accessLock.writeLock().lock()
            return when (exception) {
                null -> {
                    // TODO: try simply emptying the lists rather than re-assigning them, though I'm fairly sure it won't work
                    val data = GraphData(vertexStore, edgeStore, explanationVertexStore)
                    clear()
    //                println("QueryResponseStream.drain: Fetched ${data.vertices.size} vertices and ${data.edges.size} edges")
    //                if (data.vertices.isNotEmpty()) println("Vertex IDs: ${data.vertices.map { it.id }}")
                    Either.first(data)
                }
                else -> {
                    val response = Either.second<GraphData, Exception>(exception)
                    clear()
                    return response
                }
            }
        } finally {
            accessLock.writeLock().unlock()
        }
    }

    fun clear() {
        vertexStore = mutableListOf()
        edgeStore = mutableListOf()
        explanationVertexStore = mutableListOf()
        explanationEdgeStore = mutableListOf()
        exception = null
        queryEndTimeNanos = null
    }

    fun isEmpty(): Boolean {
        return vertexStore.isEmpty() && edgeStore.isEmpty() && exception == null
    }

    companion object {
        val EMPTY = QueryResponseStream(completed = true)
    }
}
