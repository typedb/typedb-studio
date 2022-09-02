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

package com.vaticle.typedb.studio.state.schema

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.RoleType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.common.atomic.AtomicBooleanState
import com.vaticle.typedb.studio.state.connection.SessionState
import com.vaticle.typedb.studio.state.page.Navigable
import com.vaticle.typedb.studio.state.page.PageManager
import com.vaticle.typeql.lang.common.TypeQLToken
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mu.KotlinLogging

@OptIn(ExperimentalTime::class)
class SchemaManager(
    private val session: SessionState,
    internal val pages: PageManager,
    internal val notificationMgr: NotificationManager
) : Navigable<TypeState.Thing> {

    override val name: String = TypeQLToken.Type.THING.name.lowercase()
    override val parent: TypeState.Thing? = null
    override val info: String? = null
    override val isExpandable: Boolean = true
    override val isBulkExpandable: Boolean = true
    override val entries: List<TypeState.Thing>
        get() = rootEntityType?.let { listOf(it, rootRelationType!!, rootAttributeType!!) } ?: listOf()

    val isOpen: Boolean get() = isOpenAtomic.state
    var rootEntityType: TypeState.Entity? by mutableStateOf(null); private set
    var rootRelationType: TypeState.Relation? by mutableStateOf(null); private set
    var rootAttributeType: TypeState.Attribute? by mutableStateOf(null); private set
    val isWritable: Boolean get() = session.isSchema && session.transaction.isWrite
    private var writeTx: AtomicReference<TypeDBTransaction?> = AtomicReference()
    private var readTx: AtomicReference<TypeDBTransaction?> = AtomicReference()
    private val lastTransactionUse = AtomicLong(0)
    private val entityTypes = ConcurrentHashMap<EntityType, TypeState.Entity>()
    private val attributeTypes = ConcurrentHashMap<AttributeType, TypeState.Attribute>()
    private val relationTypes = ConcurrentHashMap<RelationType, TypeState.Relation>()
    private val roleTypes = ConcurrentHashMap<RoleType, TypeState.Role>()
    private val isOpenAtomic = AtomicBooleanState(false)
    private val onTypesUpdated = LinkedBlockingQueue<() -> Unit>()
    internal val database: String? get() = session.database
    internal val coroutineScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private val TX_IDLE_TIME = Duration.seconds(16)
        private val LOGGER = KotlinLogging.logger {}
    }

    init {
        session.onOpen { isNewDB -> if (isNewDB) refreshAndPruneTypesAndOpen() }
        session.onClose { willReopenSameDB -> if (!willReopenSameDB) close() }
        session.transaction.onSchemaWriteClose {
            mayRefreshReadTx()
            refreshAndPruneTypesAndOpen()
        }
    }

    fun onTypesUpdated(function: () -> Unit) = onTypesUpdated.put(function)

    override fun reloadEntries() {
        openOrGetReadTx()?.let { tx -> // TODO: Implement API to retrieve .hasSubtypes() on TypeDB Type API
            entries.forEach { it.hasSubtypes = it.conceptType.asRemote(tx).subtypesExplicit.findAny().isPresent }
        }
    }

    override fun compareTo(other: Navigable<TypeState.Thing>): Int {
        return if (other is SchemaManager) 0 else -1
    }

    internal fun createTypeState(type: Type): TypeState? = when (type) {
        is RoleType -> createTypeState(type)
        else -> createTypeState(type as ThingType)
    }

    internal fun createTypeState(type: ThingType): TypeState.Thing? = when (type) {
        is EntityType -> createTypeState(type)
        is RelationType -> createTypeState(type)
        is AttributeType -> createTypeState(type)
        else -> throw IllegalStateException("Unrecognised ThingType object")
    }

    internal fun createTypeState(entityType: EntityType): TypeState.Entity? {
        return openOrGetReadTx()?.let {
            val remote = entityType.asRemote(it)
            val supertype = remote.supertype?.let { st ->
                if (st.isEntityType) entityTypes[st] ?: createTypeState(st.asEntityType()) else null
            }
            entityTypes.computeIfAbsent(entityType) {
                TypeState.Entity(entityType, supertype, this)
            }
        }
    }

    internal fun createTypeState(attributeType: AttributeType): TypeState.Attribute? {
        return openOrGetReadTx()?.let {
            val remote = attributeType.asRemote(it)
            val supertype = remote.supertype?.let { st ->
                if (st.isAttributeType) attributeTypes[st] ?: createTypeState(st.asAttributeType()) else null
            }
            attributeTypes.computeIfAbsent(attributeType) {
                TypeState.Attribute(attributeType, supertype, this)
            }
        }
    }

    internal fun createTypeState(relationType: RelationType): TypeState.Relation? {
        return openOrGetReadTx()?.let {
            val remote = relationType.asRemote(it)
            val supertype = remote.supertype?.let { st ->
                if (st.isRelationType) relationTypes[st] ?: createTypeState(st.asRelationType()) else null
            }
            relationTypes.computeIfAbsent(relationType) {
                TypeState.Relation(relationType, supertype, this)
            }
        }
    }

    internal fun createTypeState(roleType: RoleType): TypeState.Role? {
        return openOrGetReadTx()?.let { tx ->
            val remote = roleType.asRemote(tx)
            val supertype = remote.supertype?.let { st ->
                if (st.isRoleType) roleTypes[st] ?: createTypeState(st.asRoleType()) else null
            }
            roleTypes[roleType] ?: createTypeState(remote.relationType)?.let { relationType ->
                roleTypes.computeIfAbsent(roleType) {
                    TypeState.Role(roleType, relationType, supertype, this)
                }
            }
        }
    }

    private fun refreshAndPruneTypesAndOpen() {
        openOrGetReadTx()?.let { tx ->
            if (rootEntityType == null) rootEntityType = TypeState.Entity(
                conceptType = tx.concepts().rootEntityType, supertype = null, schemaMgr = this
            ).also { entityTypes[tx.concepts().rootEntityType] = it }
            if (rootRelationType == null) rootRelationType = TypeState.Relation(
                conceptType = tx.concepts().rootRelationType, supertype = null, schemaMgr = this
            ).also { relationTypes[tx.concepts().rootRelationType] = it }
            if (rootAttributeType == null) rootAttributeType = TypeState.Attribute(
                conceptType = tx.concepts().rootAttributeType, supertype = null, schemaMgr = this
            ).also { attributeTypes[tx.concepts().rootAttributeType] = it }
            (entityTypes.values + relationTypes.values + attributeTypes.values).forEach {
                if (tx.concepts().getThingType(it.name) == null) it.closeRecursive()
            }
            isOpenAtomic.set(true)
            onTypesUpdated.forEach { it() }
        }
    }

    fun exportTypeSchema(onSuccess: (String) -> Unit) = coroutineScope.launchAndHandle(notificationMgr, LOGGER) {
        session.typeSchema()?.let { onSuccess(it) }
    }

    internal fun openOrGetWriteTx(): TypeDBTransaction? = synchronized(this) {
        if (!isWritable) return null
        if (readTx.get() != null) closeReadTx()
        if (writeTx.get() != null) return writeTx.get()
        writeTx.set(session.transaction.tryOpen()?.also { it.onClose { writeTx.set(null) } })
        return writeTx.get()
    }

    internal fun openOrGetReadTx(): TypeDBTransaction? = synchronized(this) {
        lastTransactionUse.set(System.currentTimeMillis())
        if (isWritable && session.transaction.isOpen) return openOrGetWriteTx()
        if (readTx.get() != null) return readTx.get()
        readTx.set(session.transaction()?.also {
            it.onClose { closeReadTx() }
            scheduleCloseReadTx()
        })
        return readTx.get()
    }

    fun mayRefreshReadTx() = synchronized(this) {
        if (readTx.get() != null) {
            closeReadTx()
            openOrGetReadTx()
        }
    }

    private fun scheduleCloseReadTx() = coroutineScope.launchAndHandle(notificationMgr, LOGGER) {
        var duration = TX_IDLE_TIME
        while (true) {
            delay(duration)
            val sinceLastUse = System.currentTimeMillis() - lastTransactionUse.get()
            if (sinceLastUse >= TX_IDLE_TIME.inWholeMilliseconds) {
                closeReadTx()
                break
            } else duration = TX_IDLE_TIME - Duration.milliseconds(sinceLastUse)
        }
    }

    private fun closeReadTx() = synchronized(this) { readTx.getAndSet(null)?.close() }

    fun remove(typeState: TypeState.Thing) = when (typeState) {
        is TypeState.Entity -> entityTypes.remove(typeState.conceptType)
        is TypeState.Relation -> relationTypes.remove(typeState.conceptType)
        is TypeState.Attribute -> attributeTypes.remove(typeState.conceptType)
    }

    fun close() {
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            entries.forEach { it.closeRecursive() }
            closeReadTx()
        }
    }
}