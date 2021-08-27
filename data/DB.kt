package com.vaticle.typedb.studio.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.connection.TypeDBOptions
import com.vaticle.typedb.client.api.connection.TypeDBSession
import com.vaticle.typedb.client.api.connection.TypeDBSession.Type.DATA
import com.vaticle.typedb.client.api.connection.TypeDBTransaction
import com.vaticle.typedb.client.api.connection.TypeDBTransaction.Type.READ
import com.vaticle.typedb.studio.data.VertexEncoding.*
import com.vaticle.typedb.studio.data.EdgeDirection.*
import java.util.concurrent.CompletableFuture
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

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
        val answerTasks: MutableList<CompletableFuture<Unit>> = mutableListOf()
        try {
            session?.close()
            session = client.session(dbName, DATA)
            tx = session!!.transaction(READ, TypeDBOptions.core().infer(true).explain(true))
            val answerStream = tx!!.query().match(query)
            val elementIDs = GraphElementIDRegistry()
            val incompleteThingEdges: MutableMap<String, MutableList<IncompleteEdgeData>> = mutableMapOf()
            val incompleteTypeEdges: MutableMap<String, MutableList<IncompleteEdgeData>> = mutableMapOf()
            CompletableFuture.supplyAsync {
                answerStream.parallel().forEach { cm ->
                    cm.concepts().parallelStream().forEach { concept ->
                        val encoding: VertexEncoding = when {
                            concept.isEntity -> ENTITY
                            concept.isRelation -> RELATION
                            concept.isAttribute -> ATTRIBUTE
                            concept.isEntityType -> ENTITY_TYPE
                            concept.isRelationType -> RELATION_TYPE
                            concept.isAttributeType -> ATTRIBUTE_TYPE // TODO: Support RoleType variables
                            else -> THING_TYPE
                        }
                        if (concept.isThing) {
                            val thing = concept.asThing()
                            // no point re-querying if we've already explored this Thing
                            if (!elementIDs.things.contains(thing.iid)) {
                                val label: String = when {
                                    thing.isAttribute -> {
                                        val attribute = thing.asAttribute()
                                        "${attribute.type.label.name()}:${
                                            when {
                                                attribute.isDateTime -> attribute.asDateTime().value.format(
                                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                )
                                                else -> attribute.value
                                            }
                                        }"
                                    }
                                    else -> thing.type.label.name()
                                }
                                val thingVertex = VertexData(
                                    id = elementIDs.nextID++,
                                    encoding = encoding,
                                    label = label,
                                    shortLabel = label.substring(
                                        0, when {
                                            thing.isRelation -> 22.coerceAtMost(label.length)
                                            else -> 26.coerceAtMost(label.length)
                                        }
                                    ),
                                    width = when {
                                        thing.isRelation -> 120F
                                        else -> 110F
                                    },
                                    height = when {
                                        thing.isRelation -> 60F
                                        else -> 40F
                                    }
                                )
                                elementIDs.things[thing.iid] = thingVertex.id
                                responseStream.putVertex(thingVertex)

                                if (thing.isRelation) {
                                    val playersTask = CompletableFuture.supplyAsync {
                                        val playersMap = thing.asRelation().asRemote(tx).playersByRoleType
                                        for (rolePlayers in playersMap.entries) {
                                            for (player: Thing in rolePlayers.value) {
                                                val rolePlayerNodeID = elementIDs.things[player.iid]
                                                if (rolePlayerNodeID != null) {
                                                    responseStream.putEdge(
                                                        EdgeData(id = elementIDs.nextID++, source = thingVertex.id, target = rolePlayerNodeID, label)
                                                    )
                                                } else {
                                                    if (!incompleteThingEdges.contains(player.iid)) incompleteThingEdges[player.iid] =
                                                        mutableListOf()
                                                    incompleteThingEdges[player.iid]!!.add(
                                                        IncompleteEdgeData(id = elementIDs.nextID++, vertexID = thingVertex.id, direction = OUTGOING, label)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    answerTasks += playersTask
                                } else if (thing.isAttribute) {
                                    val ownersTask = CompletableFuture.supplyAsync {
                                        thing.asAttribute().asRemote(tx).owners.parallel().forEach { owner ->
                                            val ownerNodeID = elementIDs.things[owner.iid]
                                            if (ownerNodeID != null) {
                                                responseStream.putEdge(
                                                    EdgeData(id = elementIDs.nextID++, source = ownerNodeID, target = thingVertex.id, label = "has")
                                                )
                                            } else {
                                                if (!incompleteThingEdges.contains(owner.iid)) incompleteThingEdges[owner.iid] =
                                                    mutableListOf()
                                                incompleteThingEdges[owner.iid]!!.add(
                                                    IncompleteEdgeData(id = elementIDs.nextID++, vertexID = thingVertex.id, direction = INCOMING, label = "has")
                                                )
                                            }
                                        }
                                    }
                                    answerTasks += ownersTask
                                }

                                incompleteThingEdges[thing.iid]?.forEach {
                                    val edgeData = when (it.direction) {
                                        INCOMING -> EdgeData(it.id, source = thingVertex.id, target = it.vertexID, it.label, it.highlight)
                                        OUTGOING -> EdgeData(it.id, source = it.vertexID, target = thingVertex.id, it.label, it.highlight)
                                    }
                                    responseStream.putEdge(edgeData)
                                }
                                incompleteThingEdges.remove(thing.iid)
                            }
                        } else {
                            val type = concept.asType()
                            if (!elementIDs.types.contains(type.label.scopedName())) {
                                val label = type.label.name()
                                val typeVertex = VertexData(
                                    id = elementIDs.nextID++,
                                    encoding = encoding,
                                    label = label,
                                    shortLabel = label.substring(
                                        0, when {
                                            type.isRelationType -> 22.coerceAtMost(label.length)
                                            else -> 26.coerceAtMost(label.length)
                                        }
                                    ),
                                    width = when {
                                        type.isRelationType -> 120F
                                        else -> 110F
                                    },
                                    height = when {
                                        type.isRelationType -> 60F
                                        else -> 40F
                                    }
                                )
                                elementIDs.types[type.label.scopedName()] = typeVertex.id
                                responseStream.putVertex(typeVertex)

                                if (type.isThingType) {
                                    val remoteThingType = type.asThingType().asRemote(tx)
                                    val playsTask = CompletableFuture.supplyAsync {
                                        val plays = remoteThingType.plays.collect(Collectors.toList())
                                        remoteThingType.plays.parallel().forEach { roleType ->
                                            val relationTypeNodeID = elementIDs.types[roleType.label.scope().get()]
                                            if (relationTypeNodeID != null) {
                                                responseStream.putEdge(EdgeData(id = elementIDs.nextID++, source = relationTypeNodeID, target = typeVertex.id, label = roleType.label.name()))
                                            } else {
                                                if (!incompleteTypeEdges.contains(roleType.label.scope().get())) incompleteTypeEdges[roleType.label.scope().get()] = mutableListOf()
                                                incompleteTypeEdges[roleType.label.scope().get()]!!.add(
                                                    IncompleteEdgeData(
                                                    id = elementIDs.nextID++, vertexID = typeVertex.id, direction = INCOMING, label = roleType.label.name())
                                                )
                                            }
                                        }
                                    }
                                    val ownsTask = CompletableFuture.supplyAsync {
                                        remoteThingType.owns.parallel().forEach { attributeType ->
                                            val attributeTypeNodeID = elementIDs.types[attributeType.label.name()]
                                            if (attributeTypeNodeID != null) {
                                                responseStream.putEdge(
                                                    EdgeData(id = elementIDs.nextID++, source = typeVertex.id, target = attributeTypeNodeID, label = "owns")
                                                )
                                            } else {
                                                if (!incompleteTypeEdges.contains(attributeType.label.name())) incompleteTypeEdges[attributeType.label.name()] =
                                                    mutableListOf()
                                                incompleteTypeEdges[attributeType.label.name()]!!.add(
                                                    IncompleteEdgeData(id = elementIDs.nextID++, vertexID = typeVertex.id, direction = OUTGOING, label = "owns")
                                                )
                                            }
                                        }
                                    }
                                    answerTasks += playsTask
                                    answerTasks += ownsTask
                                }

                                incompleteTypeEdges[type.label.scopedName()]?.forEach {
                                    val edgeData = when (it.direction) {
                                        INCOMING -> EdgeData(it.id, source = typeVertex.id, target = it.vertexID, it.label, it.highlight)
                                        OUTGOING -> EdgeData(it.id, source = it.vertexID, target = typeVertex.id, it.label, it.highlight)
                                    }
                                    responseStream.putEdge(edgeData)
                                }
                                incompleteTypeEdges.remove(type.label.scopedName())
                            }
                        }
                    }
                }

                try {
                    CompletableFuture.allOf(*answerTasks.toTypedArray()).join()
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

data class GraphElementIDRegistry(var nextID: Int = 1,
                                  val things: MutableMap<String, Int> = mutableMapOf(),
                                  val types: MutableMap<String, Int> = mutableMapOf())
