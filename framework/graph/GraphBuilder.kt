/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.framework.graph

import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.thing.Attribute
import com.vaticle.typedb.client.api.concept.thing.Relation
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.concept.type.RoleType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.concept.type.ThingType.Remote
import com.vaticle.typedb.client.api.logic.Explanation
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.NotificationService
import com.vaticle.typedb.studio.service.common.util.Message.Visualiser.Companion.EXPLAIN_NOT_ENABLED
import com.vaticle.typedb.studio.service.common.util.Message.Visualiser.Companion.FULLY_EXPLAINED
import com.vaticle.typedb.studio.service.common.util.Message.Visualiser.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.service.connection.TransactionState
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.streams.toList
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging

class GraphBuilder(
    val graph: Graph, private val transactionState: TransactionState, val coroutines: CoroutineScope,
    val schema: Schema = Schema()
) {
    private val newThingVertices = ConcurrentHashMap<String, Vertex.Thing>()
    private val newTypeVertices = ConcurrentHashMap<String, Vertex.Type>()
    private val allThingVertices = ConcurrentHashMap<String, Vertex.Thing>()
    private val allTypeVertices = ConcurrentHashMap<String, Vertex.Type>()
    private val edges = ConcurrentLinkedQueue<Edge>()
    private val edgeCandidates = ConcurrentHashMap<String, Collection<EdgeCandidate>>()
    private val explainables = ConcurrentHashMap<Vertex.Thing, ConceptMap.Explainable>()
    private val ownsScopedEdgeCandidates = ConcurrentHashMap<String, Collection<ScopedEdgeCandidate.Owns>>()
    private val playsRoleScopedEdgeCandidate = ConcurrentHashMap<String, Collection<ScopedEdgeCandidate.Plays>>()
    private val vertexExplanations = ConcurrentLinkedQueue<Pair<Vertex.Thing, Explanation>>()
    private val playsLock = ReentrantReadWriteLock(true)
    private val ownsLock = ReentrantReadWriteLock(true)
    private val lock = ReentrantReadWriteLock(true)
    private val transactionID = transactionState.transaction?.hashCode()
    private val snapshotEnabled = transactionState.snapshot.value
    private val isInitialTransaction: Boolean
        get() = transactionState.transaction?.hashCode() == transactionID
    private val transactionSnapshot: TypeDBTransaction?
        get() = if (snapshotEnabled && isInitialTransaction) transactionState.transaction else null

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
                        if (transactionState.transaction?.options()?.explain()?.get() == true && concept.isInferred) {
                            addExplainables(concept, vertex, conceptMap.explainables(), varName)
                        }
                        if (answerSource is AnswerSource.Explanation) {
                            vertexExplanations += Pair(vertex, answerSource.explanation)
                        }
                    }
                }
                concept is ThingType && concept.isRoot -> { /* skip root thing types */
                }
                concept is ThingType -> {
                    putVertexIfAbsent(concept)
                }
                concept is RoleType -> { /* skip role types */
                }
                else -> throw unsupportedEncodingException(concept)
            }
        }
    }

    private fun putVertexIfAbsent(concept: Concept): PutVertexResult = when {
        concept is Thing -> putVertexIfAbsent(
            concept.iid, concept, newThingVertices, allThingVertices
        ) { Vertex.Thing.of(concept, graph) }
        concept is ThingType && !concept.isRoot -> putVertexIfAbsent(
            concept.label.name(), concept, newTypeVertices, allTypeVertices
        ) { Vertex.Type.of(concept, graph) }
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
                EdgeBuilder.of(concept, v, this, transactionState.transaction).build()
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
        lock.readLock().withLock {
            (graph.thingVertices + graph.typeVertices).values.forEach { completeEdges(it) }
            ownsScopedEdgeCandidates.forEach { (_, u) -> u.forEach {
                val typeVertex = allTypeVertices[it.targetLabel]
                if (typeVertex != null) edges.add(it.toEdge(typeVertex))
            } }
            ownsScopedEdgeCandidates.clear()
            playsRoleScopedEdgeCandidate.forEach { (_, u) -> u.forEach {
                val typeVertex = allTypeVertices[it.sourceLabel]
                if (typeVertex != null) edges.add(it.toEdge(typeVertex))
            }}
            playsRoleScopedEdgeCandidate.clear()
        }
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

    fun tryExplain(vertex: Vertex.Thing) {
        val canExplain = transactionSnapshot?.options()?.explain()?.get() ?: false
        if (!canExplain) {
            Service.notification.userWarning(LOGGER, EXPLAIN_NOT_ENABLED)
        } else {
            NotificationService.launchCompletableFuture(Service.notification, LOGGER) {
                val iterator = graph.reasoning.explanationIterators[vertex]
                    ?: runExplainQuery(vertex).also { graph.reasoning.explanationIterators[vertex] = it }
                fetchNextExplanation(vertex, iterator)
            }.exceptionally { e -> Service.notification.systemError(LOGGER, e, UNEXPECTED_ERROR) }
        }
    }

    private fun runExplainQuery(vertex: Vertex.Thing): Iterator<Explanation> {
        val explainable = graph.reasoning.explainables[vertex] ?: throw IllegalStateException("Not explainable")
        return transactionSnapshot?.query()?.explain(explainable)?.iterator()
            ?: Collections.emptyIterator()
    }

    private fun fetchNextExplanation(vertex: Vertex.Thing, iterator: Iterator<Explanation>) {
        if (iterator.hasNext()) {
            val explanation = iterator.next()
            vertexExplanations += Pair(vertex, explanation)
            loadConceptMap(explanation.condition(), AnswerSource.Explanation(explanation))
        } else Service.notification.info(LOGGER, FULLY_EXPLAINED)
    }

    fun unsupportedEncodingException(concept: Concept): IllegalStateException {
        return IllegalStateException("[$concept]'s encoding is not supported by AnswerLoader")
    }

    sealed class AnswerSource {
        object Query : AnswerSource()
        class Explanation(val explanation: com.vaticle.typedb.client.api.logic.Explanation) : AnswerSource()
    }

    sealed class ScopedEdgeCandidate {
        abstract var supertypes: List<ThingType>
        abstract fun toEdge(vertex: Vertex): Edge
        open fun rescope(supertype: Vertex.Type) {
            this.supertypes.apply {
                take(indexOf(supertype.type))
            }
        }
        fun hasSupertype(supertype: ThingType) = supertypes.contains(supertype)

        class Owns(var source: Vertex, val targetLabel: String, override var supertypes: List<ThingType>) : ScopedEdgeCandidate() {
            override fun toEdge(vertex: Vertex) =
                Edge.Owns(source as Vertex.Type, vertex as Vertex.Type.Attribute)

            override fun rescope(supertype: Vertex.Type) {
                super.rescope(supertype)
                this.source = supertype
            }
        }

        class Plays(val sourceLabel: String, var target: Vertex.Type, val role: RoleType, override var supertypes: List<ThingType>) : ScopedEdgeCandidate() {
            override fun toEdge(vertex: Vertex) =
                Edge.Plays(vertex as Vertex.Type.Relation, target, role.label.name())

            override fun rescope(supertype: Vertex.Type) {
                super.rescope(supertype)
                this.target = supertype
            }
        }
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
            fun of(concept: Concept, vertex: Vertex, graphBuilder: GraphBuilder, transaction: TypeDBTransaction?): EdgeBuilder {
                return when (concept) {
                    is com.vaticle.typedb.client.api.concept.thing.Thing -> {
                        Thing(concept, vertex as Vertex.Thing, transaction, graphBuilder)
                    }
                    is com.vaticle.typedb.client.api.concept.type.ThingType -> {
                        ThingType(concept.asThingType(), vertex as Vertex.Type, transaction, graphBuilder)
                    }
                    else -> throw graphBuilder.unsupportedEncodingException(concept)
                }
            }
        }

        class Thing(
            val thing: com.vaticle.typedb.client.api.concept.thing.Thing,
            private val thingVertex: Vertex.Thing,
            private val transaction: TypeDBTransaction?,
            ctx: GraphBuilder,
            ) : EdgeBuilder(ctx) {
            private val remoteThing get() = transaction?.let { thing.asRemote(it) }

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
                graphBuilder.apply {
                    remoteThing?.getHas()?.forEach {
                        val attributeVertex = graphBuilder.allThingVertices[it.iid] as? Vertex.Thing.Attribute
                        if (attributeVertex != null) {
                            addEdge(Edge.Has(thingVertex, attributeVertex, it.isInferred))
                        } else {
                            addEdgeCandidate(EdgeCandidate.Has(thingVertex, it.iid, it.isInferred))
                        }
                    }
                }
            }

            private fun canOwnAttributes(): Boolean {
                val typeLabel = thing.type.label.name()
                return graphBuilder.schema.typeAttributeOwnershipMap.getOrPut(typeLabel) {
                    // non-atomic update as Concept API call is idempotent and cheaper than locking the map
                    transaction?.let {
                        thing.type.asRemote(it).owns.findAny().isPresent
                    } ?: false
                }
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
            private val transaction: TypeDBTransaction?,
            ctx: GraphBuilder,
            ) : EdgeBuilder(ctx) {
            private val remoteThingType get() = transaction?.let { thingType.asRemote(it) }

            override fun build() {
                loadSubEdge()
                loadOwnsEdges()
//                loadPlaysEdges()
            }

            private fun loadSubEdge() {
                remoteThingType?.supertype?.let { supertype ->
                    val supertypeVertex = graphBuilder.allTypeVertices[supertype.label.name()]
                    if (supertypeVertex != null) graphBuilder.addEdge(Edge.Sub(typeVertex, supertypeVertex))
                    else graphBuilder.addEdgeCandidate(EdgeCandidate.Sub(typeVertex, supertype.label.name()))
                }
            }

            private fun loadOwnsEdges() {
                graphBuilder.ownsLock.writeLock().withLock {
                    remoteThingType?.owns
                        ?.forEach { attrType ->
                            val attrTypeLabel = attrType.label.name()
                            if (!graphBuilder.ownsScopedEdgeCandidates.containsKey(attrTypeLabel)) {
                                val scopedOwnsEdge = ScopedEdgeCandidate.Owns(typeVertex, attrTypeLabel, getSupertypeList(remoteThingType!!))
                                graphBuilder.ownsScopedEdgeCandidates[attrTypeLabel] = listOf(scopedOwnsEdge)
                            }

                            var parentFound = false
                            val ownsScopedEdgeCandidates = graphBuilder.ownsScopedEdgeCandidates[attrTypeLabel]!!
                            ownsScopedEdgeCandidates.forEach {
                                if (it.hasSupertype(thingType)) {
                                    it.rescope(typeVertex)
                                }
                                else if (remoteThingType!!.supertypes.toList().toSet().contains(it.source.concept.asThingType())) {
                                    parentFound = true
                                }
                            }
                            if (!parentFound) {
                                val scopedEdgeCand = ScopedEdgeCandidate.Owns(typeVertex, attrTypeLabel, getSupertypeList(remoteThingType!!))
                                graphBuilder.ownsScopedEdgeCandidates[attrTypeLabel] =
                                    graphBuilder.ownsScopedEdgeCandidates[attrTypeLabel]!! + listOf(scopedEdgeCand)
                            }
                        }
                }
//                graphBuilder.ownsLock.writeLock().withLock {
//                    remoteThingType?.owns
//                        ?.forEach { attrType ->
//                            val attrTypeLabel = attrType.label.name()
//                            if (graphBuilder.ownsScopedEdgeCandidates.containsKey(attrTypeLabel)) {
//                                println("owns sec contains key $attrTypeLabel")
//                                var rescoped = false
//                                graphBuilder.ownsScopedEdgeCandidates[attrTypeLabel]!!.forEach {
//                                    if (it.hasSupertype(thingType)) {
//                                        it.rescope(typeVertex)
//                                    }
//                                    rescoped = true
//                                }
//                                if (!rescoped) {
//                                    println("couldn't find owns sec with supertypes containing thingtype $thingType")
//                                    val result =
//                                        graphBuilder.ownsScopedEdgeCandidates[attrTypeLabel]!!.firstOrNull {
//                                            val remoteThingTypeSupertypes = getSupertypeList(remoteThingType!!)
//                                            val supertypeUnion = it.supertypes.intersect(remoteThingTypeSupertypes)
//                                            supertypeUnion.isNotEmpty()
//                                        }
//                                    if (result == null) {
//                                        println("couldn't find owns sec with non-empty intersection of remotethingtype $remoteThingType and it $attrTypeLabel supertypes")
//                                        val scopedEdgeCand = ScopedEdgeCandidate.Owns(typeVertex, attrTypeLabel, getSupertypeList(remoteThingType!!))
//                                        graphBuilder.ownsScopedEdgeCandidates[attrTypeLabel] =
//                                            graphBuilder.ownsScopedEdgeCandidates[attrTypeLabel]!! + listOf(
//                                                scopedEdgeCand
//                                            )
//                                    }
//                                }
//                            } else {
//                                println("owns sec does not contains key $attrTypeLabel")
//                                val scopedEdgeCand = ScopedEdgeCandidate.Owns(typeVertex, attrTypeLabel, getSupertypeList(remoteThingType!!))
//                                graphBuilder.ownsScopedEdgeCandidates[attrTypeLabel] = listOf(scopedEdgeCand)
//                            }
//                        }
//                }
            }

            private fun loadPlaysEdges() {
                graphBuilder.playsLock.writeLock().withLock {
                    remoteThingType?.plays
                        ?.forEach { roleType ->
                            val relationTypeLabel = roleType.label.scope().get()
                            val roleLabel = roleType.label.name()
                            val roleRelationPair = Pair(relationTypeLabel, roleLabel).toString()
                            if (graphBuilder.playsRoleScopedEdgeCandidate.containsKey(roleRelationPair)) {
                                println("plays sec contains role relation pair $roleRelationPair")
                                val result = graphBuilder.playsRoleScopedEdgeCandidate[roleRelationPair]!!.firstOrNull {
                                    it.hasSupertype(thingType)
                                }
                                if (result != null) {
                                    println("found plays sec with supertypes containing thingtype $thingType")
                                    result.rescope(typeVertex)
                                } else {
                                    println("couldn't find plays sec with supertypes containing thingtype $thingType")
                                    val result =
                                        graphBuilder.playsRoleScopedEdgeCandidate[roleRelationPair]!!.firstOrNull {
                                            it.supertypes.intersect(getSupertypeList(remoteThingType!!)).isNotEmpty()
                                        }
                                    if (result == null) {
                                        println("couldn't find plays sec with non-empty union of remotethingtype $remoteThingType and it $roleRelationPair supertypes")
                                        val scopedEdgeCand = ScopedEdgeCandidate.Plays(
                                            relationTypeLabel,
                                            typeVertex, roleType, getSupertypeList(remoteThingType!!)
                                        )
                                        graphBuilder.playsRoleScopedEdgeCandidate[roleRelationPair] =
                                            graphBuilder.playsRoleScopedEdgeCandidate[roleRelationPair]!! + listOf(
                                                scopedEdgeCand
                                            )
                                    }
                                }
                            } else {
                                println("plays sec does not contains role relation pair $roleRelationPair")
                                val scopedEdgeCand = ScopedEdgeCandidate.Plays(
                                    relationTypeLabel,
                                    typeVertex,
                                    roleType,
                                    getSupertypeList(remoteThingType!!)
                                )
                                graphBuilder.playsRoleScopedEdgeCandidate[roleRelationPair] = listOf(scopedEdgeCand)
                            }
                        }
                }
            }

            private fun getSupertypeList(remoteThingType: Remote): List<com.vaticle.typedb.client.api.concept.type.ThingType> {
                return remoteThingType.supertypes
                    .filter { it.label.name() !in listOf("thing", "entity", "relation", "attribute", remoteThingType.label.name()) }
                    .toList()
            }
        }
    }
}