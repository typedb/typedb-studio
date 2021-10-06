package com.vaticle.typedb.studio.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.thing.Relation
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.connection.TypeDBOptions
import com.vaticle.typedb.client.api.connection.TypeDBSession
import com.vaticle.typedb.client.api.connection.TypeDBSession.Type.DATA
import com.vaticle.typedb.client.api.connection.TypeDBTransaction
import com.vaticle.typedb.client.api.connection.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.logic.Explanation
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.data.VertexEncoding.*
import com.vaticle.typedb.studio.data.EdgeDirection.*
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.TypeQL.match
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

class DB(val client: DBClient, private val dbName: String) {

    val name: String
    get() {
        return dbName
    }

    private var session: TypeDBSession? by mutableStateOf(null)
    var tx: TypeDBTransaction? by mutableStateOf(null)
        private set
    private var responseStream: QueryResponseStream = QueryResponseStream.EMPTY
    var vertexGenerator = VertexGenerator()
    private val incompleteThingEdges: ConcurrentHashMap<String, MutableList<IncompleteEdgeData>> = ConcurrentHashMap()
    private val incompleteTypeEdges: ConcurrentHashMap<String, MutableList<IncompleteEdgeData>> = ConcurrentHashMap()
    private val explainables: ConcurrentHashMap<Int, ConceptMap.Explainable> = ConcurrentHashMap()
    private val explanationIterators: ConcurrentHashMap<Int, Iterator<Explanation>> = ConcurrentHashMap()
    private val currentExplanationID = AtomicInteger(0)

    private val typeDBClient = client.typeDBClient

    private fun loadAnswerStream(answerStream: Stream<ConceptMap>) {
        val tasks: MutableList<CompletableFuture<Void>> = mutableListOf()
        answerStream.forEach { tasks += loadConceptMapAsync(it) }
        CompletableFuture.allOf(*tasks.toTypedArray()).join()
    }

    private fun loadConceptMapAsync(conceptMap: ConceptMap, explanationID: Int? = null): CompletableFuture<Void> {
        val tasks: MutableList<CompletableFuture<Unit>> = mutableListOf()
        conceptMap.map().entries.parallelStream().forEach { (varName: String, concept: Concept) ->
            if (concept.isThing) {
                val thing = concept.asThing()
                val vertexID: Int = vertexGenerator.things.computeIfAbsent(thing.iid) {
                    val thingVertex: VertexData = vertexGenerator.generateVertex(thing)
//                                    println("Added a thing! $thingVertex")
                    responseStream.putVertex(thingVertex)
                    try {
                        if (thing.isInferred) {
                            explainables.computeIfAbsent(thingVertex.id) {
                                when {
                                    thing.isRelation -> conceptMap.explainables().relation(varName)
                                    thing.isAttribute -> conceptMap.explainables().attribute(varName)
                                    else -> throw IllegalStateException("Inferred Thing was neither a Relation nor an Attribute!")
                                }
                            }
                        }
                    } catch (e: TypeDBClientException) {
                        // TODO: Currently we need to catch this exception because not every Inferred concept is
                        //       Explainable. Once that bug is fixed, remove this catch statement.
                        println(e.message)
                    }

                    tasks += LoadOwnedAttributesTask(thing, tx!!, thingVertex, vertexGenerator, responseStream, incompleteThingEdges).supplyAsync()
                    if (thing.isRelation) {
                        tasks += LoadRoleplayersTask(thing.asRelation().asRemote(tx), thingVertex, vertexGenerator, responseStream).supplyAsync()
                    }

                    incompleteThingEdges.remove(thing.iid)?.forEach {
                        val edgeData = when (it.direction) {
                            INCOMING -> EdgeData(it.id, source = thingVertex.id, target = it.vertexID, it.label, it.inferred)
                            OUTGOING -> EdgeData(it.id, source = it.vertexID, target = thingVertex.id, it.label, it.inferred)
                        }
                        responseStream.putEdge(edgeData)
                    }

                    return@computeIfAbsent thingVertex.id
                }
                explanationID?.let { value ->
                    responseStream.putExplanationVertex(ExplanationVertexData(explanationID = value, vertexID = vertexID))
                }
            } else if (concept.isThingType) { // NOTE: RoleTypes are skipped - they generate edges, not vertices
                val thingType = concept.asThingType()
                vertexGenerator.types.computeIfAbsent(thingType.label.scopedName()) {
                    val typeVertex: VertexData = vertexGenerator.generateVertex(thingType)
                    responseStream.putVertex(typeVertex)

                    val remoteThingType = thingType.asRemote(tx)
                    tasks += LoadPlayableRolesTask(remoteThingType, typeVertex, vertexGenerator, responseStream, incompleteTypeEdges).supplyAsync()
                    tasks += LoadOwnedAttributeTypesTask(remoteThingType, typeVertex, vertexGenerator, responseStream, incompleteTypeEdges).supplyAsync()
                    tasks += LoadSupertypeTask(remoteThingType, typeVertex, vertexGenerator, responseStream, incompleteTypeEdges).supplyAsync()

                    incompleteTypeEdges.remove(thingType.label.scopedName())?.forEach {
                        val edgeData = when (it.direction) {
                            INCOMING -> EdgeData(it.id, source = typeVertex.id, target = it.vertexID, it.label, it.inferred)
                            OUTGOING -> EdgeData(it.id, source = it.vertexID, target = typeVertex.id, it.label, it.inferred)
                        }
                        responseStream.putEdge(edgeData)
                    }

                    return@computeIfAbsent typeVertex.id
                }
            }
        }
        return CompletableFuture.allOf(*tasks.toTypedArray())
    }

    fun matchQuery(query: String, enableReasoning: Boolean): QueryResponseStream {
        responseStream.clear()
        responseStream.completed = false
        incompleteThingEdges.clear()
        incompleteTypeEdges.clear()
        explainables.clear()
        explanationIterators.clear()
        currentExplanationID.set(0)
        try {
            session?.close()
            session = typeDBClient.session(dbName, DATA)
            val options = if (enableReasoning) TypeDBOptions.core().infer(true).explain(true) else TypeDBOptions.core()
            tx = session!!.transaction(READ, options)
            vertexGenerator = VertexGenerator()
            CompletableFuture.supplyAsync {
                try {
                    loadAnswerStream(tx!!.query().match(query))
                    println("Completed query: $query")
                    responseStream.completed = true
                } catch (e: Exception) {
                    responseStream.putError(e)
                    session?.close()
                }
            }
        } catch (e: Exception) {
            responseStream.putError(e)
            session?.close()
        }

        return responseStream
    }

    // TODO: either support or block concurrent access
    fun explainConcept(vertexID: Int): QueryResponseStream {
        try {
            CompletableFuture.supplyAsync {
                try {
                    var explanationIterator: Iterator<Explanation>? = explanationIterators[vertexID]

                    if (explanationIterator == null) {
                        val explainable: ConceptMap.Explainable? = explainables[vertexID]
                        if (explainable != null) {
                            explanationIterator = tx!!.query().explain(explainables[vertexID]).iterator()
                            explanationIterators[vertexID] = explanationIterator
                        } else throw IllegalStateException("This concept is not explainable")
                    }

                    val iter: Iterator<Explanation> = explanationIterator!!
                    if (iter.hasNext()) {
                        val explanation: Explanation = iter.next()
                        val explanationID: Int = currentExplanationID.incrementAndGet()
                        responseStream.putExplanationVertex(ExplanationVertexData(explanationID, vertexID))
                        loadConceptMapAsync(explanation.condition(), explanationID)
                    }
                    return@supplyAsync
                } catch (e: Exception) {
                    responseStream.putError(e)
                    session?.close()
                }
            }
        } catch (e: Exception) {
            responseStream.putError(e)
            session?.close()
        }
        return responseStream
    }

    fun close() {
        typeDBClient.close()
    }
}

class VertexGenerator(val things: ConcurrentHashMap<String, Int> = ConcurrentHashMap(),
                      val types: ConcurrentHashMap<String, Int> = ConcurrentHashMap()) {

    private val nextID = AtomicInteger(1)

    fun nextID(): Int {
        return nextID.getAndIncrement()
    }

    fun generateVertex(concept: Concept): VertexData {
        val encoding = concept.encoding()
        val label = concept.vertexLabel()

        return VertexData(
            concept = concept,
            id = nextID(),
            encoding = encoding,
            label = label,
            shortLabel = label.substring(startIndex = 0, endIndex =
            when (encoding) {
                RELATION_TYPE, RELATION -> 22.coerceAtMost(label.length)
                else -> 26.coerceAtMost(label.length)
            }),
            width = when (encoding) { RELATION_TYPE, RELATION -> 110F; else -> 100F },
            height = when (encoding) { RELATION_TYPE, RELATION -> 55F; else -> 35F },
            inferred = concept.isThing && concept.asThing().isInferred
        )
    }

    private fun Concept.encoding() = when {
        isEntity -> ENTITY
        isRelation -> RELATION
        isAttribute -> ATTRIBUTE
        isEntityType -> ENTITY_TYPE
        isRelationType -> RELATION_TYPE
        isAttributeType -> ATTRIBUTE_TYPE
        isRoleType -> throw IllegalArgumentException("Attempted to get the VertexEncoding of a RoleType, which is not allowed")
        else -> THING_TYPE
    }

    private fun Concept.vertexLabel() = when {
        isType -> asType().label.name()
        else -> when {
            isAttribute -> {
                val attribute = asAttribute()
                "${attribute.type.label.name()}: ${attribute.valueString()}"
            }
            else -> asThing().type.label.name()
        }
    }
}

private abstract class LoadConnectedConceptsTask(protected val vertex: VertexData, protected val vertexGenerator: VertexGenerator,
                                                 protected val responseStream: QueryResponseStream) {

    abstract fun run()

    fun supplyAsync(): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync { run() }
    }
}

private class LoadRoleplayersTask(private val relation: Relation.Remote, vertex: VertexData,
                                  vertexGenerator: VertexGenerator, responseStream: QueryResponseStream
) : LoadConnectedConceptsTask(vertex, vertexGenerator, responseStream) {

    override fun run() {
        for (rolePlayers in relation.playersByRoleType.entries) {
            for (player: Thing in rolePlayers.value) {
                val rolePlayerNodeID = vertexGenerator.things.computeIfAbsent(player.iid) {
                    val vertex = vertexGenerator.generateVertex(player)
                    responseStream.putVertex(vertex)
                    return@computeIfAbsent vertex.id
                }
                responseStream.putEdge(EdgeData(id = vertexGenerator.nextID(), source = vertex.id, target = rolePlayerNodeID,
                    label = rolePlayers.key.label.name(), inferred = relation.isInferred))
            }
        }
    }
}

private class LoadOwnedAttributesTask(private val thing: Thing, private val tx: TypeDBTransaction, vertex: VertexData,
                                      vertexGenerator: VertexGenerator, responseStream: QueryResponseStream,
                                      private val incompleteEdges: ConcurrentHashMap<String, MutableList<IncompleteEdgeData>>
) : LoadConnectedConceptsTask(vertex, vertexGenerator, responseStream) {

    override fun run() {
        val (x, y) = Pair("x", "y")
        tx.query().match(match(TypeQL.`var`(x).iid(thing.iid).has(TypeQL.`var`(y)))).forEach { cm: ConceptMap ->
            // TODO: if the owned attribute is inferred, get its Explainable
            val attribute = cm.get(y).asAttribute()
            val isEdgeInferred = y in cm.explainables().attributes().keys || y in cm.explainables().ownerships().keys.map { it.second() }
            val attributeNodeID = vertexGenerator.things[attribute.iid]
            if (attributeNodeID != null) {
                responseStream.putEdge(EdgeData(vertexGenerator.nextID(), source = vertex.id, target = attributeNodeID,
                    label = "has", inferred = isEdgeInferred))
            } else {
                incompleteEdges.getOrPut(attribute.iid) { mutableListOf() }
                    .add(IncompleteEdgeData(vertexGenerator.nextID(), vertexID = vertex.id, direction = OUTGOING,
                        label = "has", inferred = isEdgeInferred))
            }
        }
    }
}

private class LoadPlayableRolesTask(private val thingType: ThingType.Remote, vertex: VertexData,
                                    vertexGenerator: VertexGenerator, responseStream: QueryResponseStream,
                                    private val incompleteEdges: ConcurrentHashMap<String, MutableList<IncompleteEdgeData>>
) : LoadConnectedConceptsTask(vertex, vertexGenerator, responseStream) {

    override fun run() {
        thingType.plays.parallel().forEach { roleType ->
            val relationTypeNodeID = vertexGenerator.types[roleType.label.scope().get()]
            if (relationTypeNodeID != null) {
                responseStream.putEdge(EdgeData(vertexGenerator.nextID(), source = relationTypeNodeID, target = vertex.id, label = roleType.label.name()))
            } else {
                incompleteEdges.getOrPut(roleType.label.scope().get()) { mutableListOf() }
                    .add(IncompleteEdgeData(vertexGenerator.nextID(), vertexID = vertex.id, direction = INCOMING, label = roleType.label.name()))
            }
        }
    }
}

private class LoadOwnedAttributeTypesTask(private val thingType: ThingType.Remote, vertex: VertexData,
                                          vertexGenerator: VertexGenerator, responseStream: QueryResponseStream,
                                          private val incompleteEdges: ConcurrentHashMap<String, MutableList<IncompleteEdgeData>>
) : LoadConnectedConceptsTask(vertex, vertexGenerator, responseStream) {

    override fun run() {
        thingType.owns.parallel().forEach { attributeType ->
            val attributeTypeNodeID = vertexGenerator.types[attributeType.label.name()]
            if (attributeTypeNodeID != null) {
                responseStream.putEdge(EdgeData(vertexGenerator.nextID(), source = vertex.id, target = attributeTypeNodeID, label = "owns"))
            } else {
                incompleteEdges.getOrPut(attributeType.label.name()) { mutableListOf() }
                    .add(IncompleteEdgeData(vertexGenerator.nextID(), vertexID = vertex.id, direction = OUTGOING, label = "owns"))
            }
        }
    }
}

private class LoadSupertypeTask(private val thingType: ThingType.Remote, vertex: VertexData,
                                vertexGenerator: VertexGenerator, responseStream: QueryResponseStream,
                                private val incompleteEdges: ConcurrentHashMap<String, MutableList<IncompleteEdgeData>>
) : LoadConnectedConceptsTask(vertex, vertexGenerator, responseStream) {

    override fun run() {
        thingType.supertype?.let { supertype ->
            val supertypeNodeID = vertexGenerator.types[supertype.label.name()]
            if (supertypeNodeID != null) {
                responseStream.putEdge(EdgeData(vertexGenerator.nextID(), source = vertex.id, target = supertypeNodeID, label = "sub"))
            } else {
                incompleteEdges.getOrPut(supertype.label.name()) { mutableListOf() }
                    .add(IncompleteEdgeData(vertexGenerator.nextID(), vertexID = vertex.id, direction = OUTGOING, label = "sub"))
            }
        }
    }
}
