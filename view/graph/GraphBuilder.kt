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

import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.thing.Attribute
import com.vaticle.typedb.client.api.concept.thing.Relation
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.concept.type.RoleType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.logic.Explanation
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.state.connection.TransactionState
import com.vaticle.typeql.lang.TypeQL
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging

class GraphBuilder(
    val graph: Graph, val transactionState: TransactionState, val coroutineScope: CoroutineScope,
    val schema: Schema = Schema()
) {
    private val newThingVertices = ConcurrentHashMap<String, Vertex.Thing>()
    private val newTypeVertices = ConcurrentHashMap<String, Vertex.Type>()
    private val allThingVertices = ConcurrentHashMap<String, Vertex.Thing>()
    private val allTypeVertices = ConcurrentHashMap<String, Vertex.Type>()
    private val edges = ConcurrentLinkedQueue<Edge>()
    private val edgeCandidates = ConcurrentHashMap<String, Collection<EdgeCandidate>>()
    private val explainables = ConcurrentHashMap<Vertex.Thing, ConceptMap.Explainable>()
    private val vertexExplanations = ConcurrentLinkedQueue<Pair<Vertex.Thing, Explanation>>()
    private val lock = ReentrantReadWriteLock(true)

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    fun loadConceptMap(conceptMap: ConceptMap, answerSource: AnswerSource = AnswerSource.Query) {
        conceptMap.map().entries.forEach { (varName: String, concept: Concept) ->
            when {
                concept is Thing -> {
                    val (added, vertex) = putVertexIfAbsent(concept)
                    if (added) {
                        vertex as Vertex.Thing
                        if (transactionState.explain.value && concept.isInferred) {
                            addExplainables(concept, vertex, conceptMap.explainables(), varName)
                        }
                        if (answerSource is AnswerSource.Explanation) {
                            vertexExplanations += Pair(vertex, answerSource.explanation)
                        }
                    }
                }
                concept is ThingType && concept.isRoot -> { /* skip root thing types */ }
                concept is ThingType -> { putVertexIfAbsent(concept) }
                concept is RoleType -> { /* skip role types */ }
                else -> throw unsupportedEncodingException(concept)
            }
        }
    }

    private fun putVertexIfAbsent(concept: Concept): PutVertexResult = when {
        concept is Thing -> putVertexIfAbsent(concept.iid, concept, newThingVertices, allThingVertices) {
            Vertex.Thing.of(concept, graph)
        }
        concept is ThingType && !concept.isRoot -> putVertexIfAbsent(concept.label.name(), concept, newTypeVertices, allTypeVertices) {
            Vertex.Type.of(concept, graph)
        }
        else -> throw unsupportedEncodingException(concept)
    }

    private fun <VERTEX : Vertex> putVertexIfAbsent(
        key: String, concept: Concept, newRecords: MutableMap<String, VERTEX>, allRecords: MutableMap<String, VERTEX>,
        vertexFn: () -> VERTEX
    ): PutVertexResult {
        var added = false
        val vertex = lock.readLock().withLock {
            val v = allRecords.computeIfAbsent(key) { added = true; vertexFn() }
            if (added) {
                newRecords[key] = v
                completeEdges(missingVertex = v)
                EdgeBuilder.of(concept, v, this).build()
            }
            v
        }
        return PutVertexResult(added, vertex)
    }

    data class PutVertexResult(val added: Boolean, val vertex: Vertex)

    fun addEdge(edge: Edge) {
        lock.readLock().withLock { edges += edge }
    }

    fun addEdgeCandidate(edge: EdgeCandidate) {
        val key = when (edge) {
            is EdgeCandidate.Has -> edge.targetIID
            is EdgeCandidate.Isa -> edge.targetLabel
            is EdgeCandidate.Owns -> edge.targetLabel
            is EdgeCandidate.Plays -> edge.sourceLabel
            is EdgeCandidate.Sub -> edge.targetLabel
        }
        edgeCandidates.compute(key) { _, existing -> if (existing == null) listOf(edge) else existing + edge }
    }

    private fun completeEdges(missingVertex: Vertex) {
        val key = when (missingVertex) {
            is Vertex.Type -> missingVertex.type.label.name()
            is Vertex.Thing -> missingVertex.thing.iid
        }
        edgeCandidates.remove(key)?.let { candidates ->
            candidates.forEach { edges += it.toEdge(missingVertex) }
        }
    }

    fun completeAllEdges() {
        // Since there is no protection against an edge candidate, and the vertex that completes it, being added
        // concurrently, we do a final sanity check once all vertices + edges have been loaded.
        lock.readLock().withLock { (graph.thingVertices + graph.typeVertices).values.forEach { completeEdges(it) } }
    }

    private fun addExplainables(
        thing: Thing, thingVertex: Vertex.Thing, explainables: ConceptMap.Explainables, varName: String
    ) {
        try {
            this.explainables.computeIfAbsent(thingVertex) {
                when (thing) {
                    is Relation -> explainables.relation(varName)
                    is Attribute<*> -> explainables.attribute(varName)
                    else -> throw IllegalStateException("Inferred Thing was neither a Relation nor an Attribute")
                }
            }
        } catch (_: TypeDBClientException) {
            // TODO: Currently we need to catch this exception because not every Inferred concept is
            //       Explainable. Once that bug is fixed, remove this catch statement.
            /* do nothing */
        }
    }

    fun dumpTo(graph: Graph) {
        lock.writeLock().withLock {
            dumpVerticesTo(graph)
            dumpEdgesTo(graph)
            dumpExplainablesTo(graph)
            dumpExplanationStructureTo(graph)
        }
    }

    private fun dumpVerticesTo(graph: Graph) {
        newThingVertices.forEach { (iid, vertex) -> graph.putThingVertex(iid, vertex) }
        newTypeVertices.forEach { (label, vertex) -> graph.putTypeVertex(label, vertex) }
        newThingVertices.clear()
        newTypeVertices.clear()
    }

    private fun dumpEdgesTo(graph: Graph) {
        edges.forEach { graph.addEdge(it) }
        computeCurvedEdges(edges, graph)
        edges.clear()
    }

    private fun computeCurvedEdges(edges: Iterable<Edge>, graph: Graph) {
        val edgesBySource = graph.edges.groupBy { it.source }
        edges.forEach { edge ->
            val edgeBand = getEdgeBand(edge, edgesBySource)
            if (edgeBand.size > 1) edgeBand.forEach { graph.makeEdgeCurved(it) }
        }
    }

    private fun getEdgeBand(edge: Edge, allEdgesBySource: Map<Vertex, Collection<Edge>>): Collection<Edge> {
        // Grouping edges by source allows us to minimise the number of passes we do over the whole graph
        return (allEdgesBySource.getOrDefault(edge.source, listOf()).filter { it.target == edge.target }
                + allEdgesBySource.getOrDefault(edge.target, listOf()).filter { it.target == edge.source })
    }

    private fun dumpExplainablesTo(graph: Graph) {
        explainables.forEach { graph.reasoning.explainables.putIfAbsent(it.key, it.value) }
        explainables.clear()
    }

    private fun dumpExplanationStructureTo(graph: Graph) {
        graph.reasoning.addVertexExplanations(vertexExplanations)
        vertexExplanations.clear()
    }

    fun explain(vertex: Vertex.Thing) {
        NotificationManager.launchCompletableFuture(GlobalState.notification, LOGGER) {
            val iterator = graph.reasoning.explanationIterators[vertex]
                ?: runExplainQuery(vertex).also { graph.reasoning.explanationIterators[vertex] = it }
            fetchNextExplanation(vertex, iterator)
        }.exceptionally { e ->
            GlobalState.notification.systemError(LOGGER, e, Message.Visualiser.UNEXPECTED_ERROR)
        }
    }

    private fun runExplainQuery(vertex: Vertex.Thing): Iterator<Explanation> {
        val explainable = graph.reasoning.explainables[vertex] ?: throw IllegalStateException("Not explainable")
        return transactionState.transaction?.query()?.explain(explainable)?.iterator()
            ?: Collections.emptyIterator()
    }

    private fun fetchNextExplanation(vertex: Vertex.Thing, iterator: Iterator<Explanation>) {
        if (iterator.hasNext()) {
            val explanation = iterator.next()
            vertexExplanations += Pair(vertex, explanation)
            loadConceptMap(explanation.condition(), AnswerSource.Explanation(explanation))
        } else {
            GlobalState.notification.info(LOGGER, Message.Visualiser.FULLY_EXPLAINED)
        }
    }

    fun unsupportedEncodingException(concept: Concept): IllegalStateException {
        return IllegalStateException("[$concept]'s encoding is not supported by AnswerLoader")
    }

    sealed class AnswerSource {
        object Query : AnswerSource()
        class Explanation(val explanation: com.vaticle.typedb.client.api.logic.Explanation) : AnswerSource()
    }

    sealed class EdgeCandidate {

        interface Inferrable {
            val isInferred: Boolean
        }

        abstract fun toEdge(vertex: Vertex): Edge

        // Type edges
        class Sub(val source: Vertex.Type, val targetLabel: String) : EdgeCandidate() {
            override fun toEdge(vertex: Vertex) = Edge.Sub(source, vertex as Vertex.Type)
        }

        class Owns(val source: Vertex.Type, val targetLabel: String) : EdgeCandidate() {
            override fun toEdge(vertex: Vertex) = Edge.Owns(source, vertex as Vertex.Type.Attribute)
        }

        class Plays(val sourceLabel: String, val target: Vertex.Type, val role: String) : EdgeCandidate() {
            override fun toEdge(vertex: Vertex) = Edge.Plays(vertex as Vertex.Type.Relation, target, role)
        }

        // Thing edges
        class Has(
            val source: Vertex.Thing, val targetIID: String, override val isInferred: Boolean = false
        ) : EdgeCandidate(), Inferrable {

            override fun toEdge(vertex: Vertex) = Edge.Has(source, vertex as Vertex.Thing.Attribute, isInferred)
        }

        // Thing-to-type edges
        class Isa(val source: Vertex.Thing, val targetLabel: String) : EdgeCandidate() {
            override fun toEdge(vertex: Vertex) = Edge.Isa(source, vertex as Vertex.Type)
        }
    }

    class Schema(val typeAttributeOwnershipMap: ConcurrentMap<String, Boolean> = ConcurrentHashMap())

    sealed class EdgeBuilder(val graphBuilder: GraphBuilder) {

        abstract fun build()

        companion object {
            fun of(concept: Concept, vertex: Vertex, graphBuilder: GraphBuilder): EdgeBuilder {
                return when (concept) {
                    is com.vaticle.typedb.client.api.concept.thing.Thing -> {
                        Thing(concept, vertex as Vertex.Thing, graphBuilder)
                    }
                    is com.vaticle.typedb.client.api.concept.type.ThingType -> {
                        ThingType(concept.asThingType(), vertex as Vertex.Type, graphBuilder)
                    }
                    else -> throw graphBuilder.unsupportedEncodingException(concept)
                }
            }
        }

        class Thing(
            val thing: com.vaticle.typedb.client.api.concept.thing.Thing,
            private val thingVertex: Vertex.Thing,
            private val ctx: GraphBuilder
        ) : EdgeBuilder(ctx) {
            private val remoteThing get() = ctx.transactionState.transaction?.let { thing.asRemote(it) }

            override fun build() {
                loadIsaEdge()
                loadHasEdges()
                if (thing is Relation) loadRoleplayerEdgesAndVertices()
            }

            private fun loadIsaEdge() {
                thing.type.let { type ->
                    val typeVertex = graphBuilder.allTypeVertices[type.label.name()]
                    if (typeVertex != null) graphBuilder.addEdge(Edge.Isa(thingVertex, typeVertex))
                    else graphBuilder.addEdgeCandidate(EdgeCandidate.Isa(thingVertex, type.label.name()))
                }
            }

            private fun loadHasEdges() {
                // construct TypeQL query so that reasoning can run
                // test for ability to own attributes, to ensure query will not throw during type inference
                if (!canOwnAttributes()) return
                val (x, attr) = Pair("x", "attr")
                graphBuilder.transactionState.transaction?.query()
                    ?.match(TypeQL.match(TypeQL.`var`(x).iid(thing.iid).has(TypeQL.`var`(attr))))
                    ?.forEach { answer ->
                        val attribute = answer.get(attr).asAttribute()
                        val isEdgeInferred = attributeIsExplainable(attr, answer) || ownershipIsExplainable(attr, answer)
                        val attributeVertex = graphBuilder.allThingVertices[attribute.iid] as? Vertex.Thing.Attribute
                        if (attributeVertex != null) {
                            graphBuilder.addEdge(Edge.Has(thingVertex, attributeVertex, isEdgeInferred))
                        } else {
                            graphBuilder.addEdgeCandidate(EdgeCandidate.Has(thingVertex, attribute.iid, isEdgeInferred))
                        }
                    }
            }

            private fun canOwnAttributes(): Boolean {
                val typeLabel = thing.type.label.name()
                return graphBuilder.schema.typeAttributeOwnershipMap.getOrPut(typeLabel) {
                    // non-atomic update as Concept API call is idempotent and cheaper than locking the map
                    graphBuilder.transactionState.transaction?.let {
                        thing.type.asRemote(it).owns.findAny().isPresent
                    } ?: false
                }
            }

            private fun attributeIsExplainable(attributeVarName: String, conceptMap: ConceptMap): Boolean {
                return attributeVarName in conceptMap.explainables().attributes().keys
            }

            private fun ownershipIsExplainable(attributeVarName: String, conceptMap: ConceptMap): Boolean {
                return attributeVarName in conceptMap.explainables().ownerships().keys.map { it.second() }
            }

            private fun loadRoleplayerEdgesAndVertices() {
                graphBuilder.apply {
                    remoteThing?.asRelation()?.playersByRoleType?.entries?.forEach { (roleType, roleplayers) ->
                        roleplayers.forEach { rp ->
                            val result = putVertexIfAbsent(rp.iid, rp, newThingVertices, allThingVertices) {
                                Vertex.Thing.of(rp, graph)
                            }
                            addEdge(
                                Edge.Roleplayer(
                                    thingVertex as Vertex.Thing.Relation, result.vertex as Vertex.Thing,
                                    roleType.label.name(), thing.isInferred
                                )
                            )
                        }
                    }
                }
            }
        }

        class ThingType(
            private val thingType: com.vaticle.typedb.client.api.concept.type.ThingType,
            private val typeVertex: Vertex.Type,
            private val ctx: GraphBuilder
        ) : EdgeBuilder(ctx) {
            private val remoteThingType get() = ctx.transactionState.transaction?.let { thingType.asRemote(it) }

            override fun build() {
                loadSubEdge()
                loadOwnsEdges()
                loadPlaysEdges()
            }

            private fun loadSubEdge() {
                remoteThingType?.supertype?.let { supertype ->
                    val supertypeVertex = graphBuilder.allTypeVertices[supertype.label.name()]
                    if (supertypeVertex != null) graphBuilder.addEdge(Edge.Sub(typeVertex, supertypeVertex))
                    else graphBuilder.addEdgeCandidate(EdgeCandidate.Sub(typeVertex, supertype.label.name()))
                }
            }

            private fun loadOwnsEdges() {
                remoteThingType?.owns?.forEach { attrType ->
                    val attrTypeLabel = attrType.label.name()
                    val attrTypeVertex = graphBuilder.allTypeVertices[attrTypeLabel] as? Vertex.Type.Attribute
                    if (attrTypeVertex != null) graphBuilder.addEdge(Edge.Owns(typeVertex, attrTypeVertex))
                    else graphBuilder.addEdgeCandidate(EdgeCandidate.Owns(typeVertex, attrTypeLabel))
                }
            }

            private fun loadPlaysEdges() {
                remoteThingType?.plays?.forEach { roleType ->
                    val relationTypeLabel = roleType.label.scope().get()
                    val roleLabel = roleType.label.name()
                    val relationTypeVertex = graphBuilder.allTypeVertices[relationTypeLabel] as? Vertex.Type.Relation
                    if (relationTypeVertex != null) {
                        graphBuilder.addEdge(Edge.Plays(relationTypeVertex, typeVertex, roleLabel))
                    } else {
                        graphBuilder.addEdgeCandidate(EdgeCandidate.Plays(relationTypeLabel, typeVertex, roleLabel))
                    }
                }
            }
        }
    }
}