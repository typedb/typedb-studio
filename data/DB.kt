package com.vaticle.typedb.studio.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.thing.Relation
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.connection.TypeDBOptions
import com.vaticle.typedb.client.api.connection.TypeDBSession
import com.vaticle.typedb.client.api.connection.TypeDBSession.Type.DATA
import com.vaticle.typedb.client.api.connection.TypeDBTransaction
import com.vaticle.typedb.client.api.connection.TypeDBTransaction.Type.READ
import com.vaticle.typedb.studio.data.VertexEncoding.*
import com.vaticle.typedb.studio.data.EdgeDirection.*
import java.util.concurrent.CompletableFuture
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class DB(dbServer: DBServer, private val dbName: String) {

    val name: String
    get() {
        return dbName
    }

    private var session: TypeDBSession? by mutableStateOf(null)
    var tx: TypeDBTransaction? by mutableStateOf(null)
        private set

    private val client = dbServer.client

    fun matchQuery(query: String): QueryResponseStream {
        val responseStream = QueryResponseStream()
        val tasks: MutableList<CompletableFuture<Unit>> = mutableListOf()
        try {
            session?.close()
            session = client.session(dbName, DATA)
            tx = session!!.transaction(READ, TypeDBOptions.core().infer(true).explain(true))
            val answerStream = tx!!.query().match(query)
            val vertexGenerator = VertexGenerator()
            val incompleteTypeEdges: ConcurrentHashMap<String, MutableList<IncompleteEdgeData>> = ConcurrentHashMap()
            val queryAnswerCount = AtomicInteger(0)
            CompletableFuture.supplyAsync {
                try {
                    answerStream.parallel().forEach { cm ->
                        queryAnswerCount.incrementAndGet()
                        cm.concepts().parallelStream().forEach { concept ->
                            if (concept.isThing) {
                                val thing = concept.asThing()
                                vertexGenerator.things.computeIfAbsent(thing.iid) {
                                    val thingVertex = vertexGenerator.generateVertex(thing)
                                    responseStream.putVertex(thingVertex)

                                    tasks += LoadOwnedAttributesTask(thing.asRemote(tx), thingVertex, vertexGenerator, responseStream).supplyAsync()
                                    if (thing.isRelation) {
                                        tasks += LoadRoleplayersTask(thing.asRelation().asRemote(tx), thingVertex, vertexGenerator, responseStream).supplyAsync()
                                    }

                                    return@computeIfAbsent thingVertex.id
                                }
                            } else {
                                val type = concept.asType()
                                vertexGenerator.types.computeIfAbsent(type.label.scopedName()) {
                                    val typeVertex = vertexGenerator.generateVertex(type)
                                    responseStream.putVertex(typeVertex)

                                    if (type.isThingType) {
                                        val remoteThingType = type.asThingType().asRemote(tx)
                                        tasks += LoadPlayableRolesTask(remoteThingType, typeVertex, vertexGenerator, responseStream, incompleteTypeEdges).supplyAsync()
                                        tasks += LoadOwnedAttributeTypesTask(remoteThingType, typeVertex, vertexGenerator, responseStream).supplyAsync()
                                    }

                                    incompleteTypeEdges.remove(type.label.scopedName())?.forEach {
                                        val edgeData = when (it.direction) {
                                            INCOMING -> EdgeData(it.id, source = typeVertex.id, target = it.vertexID, it.label, it.highlight)
                                            OUTGOING -> EdgeData(it.id, source = it.vertexID, target = typeVertex.id, it.label, it.highlight)
                                        }
                                        responseStream.putEdge(edgeData)
                                    }

                                    return@computeIfAbsent typeVertex.id
                                }
                            }
                        }
                    }
                    CompletableFuture.allOf(*tasks.toTypedArray()).join()
                    println("Completed query: $query and found $queryAnswerCount answers (excluding connected concepts)")
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

    fun close() {
        client.close()
    }
}

class VertexGenerator(val things: ConcurrentHashMap<String, Int> = ConcurrentHashMap(),
                      val types: ConcurrentHashMap<String, Int> = ConcurrentHashMap()) {

    private val nextID = AtomicInteger(1)

    fun nextID(): Int {
        return nextID.getAndIncrement()
    }

    fun generateVertex(concept: Concept): VertexData {
        val encoding: VertexEncoding = when {
            concept.isEntity -> ENTITY
            concept.isRelation -> RELATION
            concept.isAttribute -> ATTRIBUTE
            concept.isEntityType -> ENTITY_TYPE
            concept.isRelationType -> RELATION_TYPE
            concept.isAttributeType -> ATTRIBUTE_TYPE // TODO: Support RoleType variables
            else -> THING_TYPE
        }

        val label = when (concept.isType) {
            true -> concept.asType().label.name()
            false -> when {
                concept.isAttribute -> {
                    val attribute = concept.asAttribute()
                    "${attribute.type.label.name()}:${
                        when {
                            attribute.isDateTime -> attribute.asDateTime().value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            else -> attribute.value
                        }
                    }"
                }
                else -> concept.asThing().type.label.name()
            }
        }

        return VertexData(
            id = nextID(),
            encoding = encoding,
            label = label,
            shortLabel = label.substring(startIndex = 0, endIndex =
            when (encoding) {
                RELATION_TYPE, RELATION -> 22.coerceAtMost(label.length)
                else -> 26.coerceAtMost(label.length)
            }),
            width = when (encoding) { RELATION_TYPE, RELATION -> 120F; else -> 110F },
            height = when (encoding) { RELATION_TYPE, RELATION -> 60F; else -> 40F },
        )
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
                responseStream.putEdge(EdgeData(vertexGenerator.nextID(), source = vertex.id, target = rolePlayerNodeID, vertex.label))
            }
        }
    }
}

private class LoadOwnedAttributesTask(private val thing: Thing.Remote, vertex: VertexData,
                                      vertexGenerator: VertexGenerator, responseStream: QueryResponseStream
) : LoadConnectedConceptsTask(vertex, vertexGenerator, responseStream) {

    override fun run() {
        thing.getHas(false).forEach { attribute ->
            val attributeNodeID = vertexGenerator.things.computeIfAbsent(attribute.iid) {
                val vertex = vertexGenerator.generateVertex(attribute)
                responseStream.putVertex(vertex)
                return@computeIfAbsent vertex.id
            }
            responseStream.putEdge(EdgeData(vertexGenerator.nextID(), source = vertex.id, target = attributeNodeID, label = "has",
                highlight = if (thing.isInferred || attribute.isInferred) EdgeHighlight.INFERRED else EdgeHighlight.NONE))
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
                                          vertexGenerator: VertexGenerator, responseStream: QueryResponseStream
) : LoadConnectedConceptsTask(vertex, vertexGenerator, responseStream) {

    override fun run() {
        thingType.owns.parallel().forEach { attributeType ->
            val attributeTypeNodeID = vertexGenerator.types.computeIfAbsent(attributeType.label.name()) {
                val vertex = vertexGenerator.generateVertex(attributeType)
                responseStream.putVertex(vertex)
                return@computeIfAbsent vertex.id
            }
            responseStream.putEdge(EdgeData(vertexGenerator.nextID(), source = vertex.id, target = attributeTypeNodeID, label = "owns"))
        }
    }
}
