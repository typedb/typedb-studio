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

package com.vaticle.typedb.studio.state.schema

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.RoleType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.common.atomic.AtomicBooleanState
import com.vaticle.typedb.studio.state.connection.SessionState
import com.vaticle.typedb.studio.state.connection.TransactionState.Companion.ONE_HOUR_IN_MILLS
import com.vaticle.typedb.studio.state.resource.Navigable
import com.vaticle.typeql.lang.common.TypeQLToken
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTime::class)
class SchemaManager(
    private val session: SessionState,
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
    var onRootsUpdated: (() -> Unit)? = null
    val hasWriteTx: Boolean get() = session.isSchema && session.transaction.isWrite
    private var readTx: AtomicReference<TypeDBTransaction> = AtomicReference()
    private val lastTransactionUse = AtomicLong(0)
    private val entityTypes = ConcurrentHashMap<EntityType, TypeState.Entity>()
    private val attributeTypes = ConcurrentHashMap<AttributeType, TypeState.Attribute>()
    private val relationTypes = ConcurrentHashMap<RelationType, TypeState.Relation>()
    private val roleTypes = ConcurrentHashMap<RoleType, TypeState.Role>()
    private val isOpenAtomic = AtomicBooleanState(false)
    internal val database: String? get() = session.database
    internal val coroutineScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private val TX_IDLE_TIME = Duration.seconds(16)
    }

    init {
        session.onOpen { isNewDB -> if (isNewDB) loadTypesAndOpen() }
        session.onClose { willReopenSameDB -> if (willReopenSameDB) closeReadTx() else close() }
        session.transaction.onSchemaWrite {
            refreshReadTx()
            loadTypesAndOpen()
        }
    }

    override fun reloadEntries() {
        entries.forEach {
            it.hasSubtypes = it.conceptType.asRemote(openOrGetReadTx()).subtypesExplicit.findAny().isPresent
        }
    }

    override fun compareTo(other: Navigable<TypeState.Thing>): Int {
        return if (other is SchemaManager) 0 else -1
    }

    internal fun createTypeState(type: Type): TypeState = when (type) {
        is RoleType -> createTypeState(type)
        else -> createTypeState(type as ThingType)
    }

    internal fun createTypeState(type: ThingType): TypeState.Thing = when (type) {
        is EntityType -> createTypeState(type)
        is RelationType -> createTypeState(type)
        is AttributeType -> createTypeState(type)
        else -> throw IllegalStateException("Unrecognised ThingType object")
    }

    internal fun createTypeState(type: EntityType): TypeState.Entity {
        val remote = type.asRemote(openOrGetReadTx())
        val supertype = remote.supertype?.let {
            if (it.isEntityType) entityTypes[it] ?: createTypeState(it.asEntityType()) else null
        }
        return entityTypes.computeIfAbsent(type) {
            TypeState.Entity(type, supertype, remote.subtypesExplicit.findAny().isPresent, this)
        }
    }

    internal fun createTypeState(type: AttributeType): TypeState.Attribute {
        val remote = type.asRemote(openOrGetReadTx())
        val supertype = remote.supertype?.let {
            if (it.isAttributeType) attributeTypes[it] ?: createTypeState(it.asAttributeType()) else null
        }
        return attributeTypes.computeIfAbsent(type) {
            TypeState.Attribute(type, supertype, remote.subtypesExplicit.findAny().isPresent, this)
        }
    }

    internal fun createTypeState(type: RelationType): TypeState.Relation {
        val remote = type.asRemote(openOrGetReadTx())
        val supertype = remote.supertype?.let {
            if (it.isRelationType) relationTypes[it] ?: createTypeState(it.asRelationType()) else null
        }
        return relationTypes.computeIfAbsent(type) {
            TypeState.Relation(type, supertype, remote.subtypesExplicit.findAny().isPresent, this)
        }
    }

    internal fun createTypeState(type: RoleType): TypeState.Role {
        val remote = type.asRemote(openOrGetReadTx())
        val supertype = remote.supertype?.let {
            if (it.isRoleType) roleTypes[it] ?: createTypeState(it.asRoleType()) else null
        }
        return roleTypes.computeIfAbsent(type) {
            val relationType = createTypeState(remote.relationType)
            TypeState.Role(type, relationType, supertype, remote.subtypesExplicit.findAny().isPresent, this)
        }
    }

    private fun loadTypesAndOpen() {
        val conceptMgr = openOrGetReadTx().concepts()
        rootEntityType = TypeState.Entity(
            conceptType = conceptMgr.rootEntityType, supertype = null, hasSubtypes = true, schemaMgr = this
        ).also { entityTypes[conceptMgr.rootEntityType] = it }
        rootRelationType = TypeState.Relation(
            conceptType = conceptMgr.rootRelationType, supertype = null, hasSubtypes = true, schemaMgr = this
        ).also { relationTypes[conceptMgr.rootRelationType] = it }
        rootAttributeType = TypeState.Attribute(
            conceptType = conceptMgr.rootAttributeType, supertype = null, hasSubtypes = true, schemaMgr = this
        ).also { attributeTypes[conceptMgr.rootAttributeType] = it }
        onRootsUpdated?.let { it() }
        isOpenAtomic.set(true)
    }

    fun exportTypeSchema(onSuccess: (String) -> Unit) = coroutineScope.launch {
        session.typeSchema()?.let { onSuccess(it) }
    }

    fun refreshReadTx() {
        synchronized(this) {
            if (readTx.get() != null) {
                closeReadTx()
                openOrGetReadTx()
            }
        }
    }

    internal fun openOrGetReadTx(): TypeDBTransaction {
        synchronized(this) {
            lastTransactionUse.set(System.currentTimeMillis())
            if (readTx.get() != null) return readTx.get()
            readTx.set(session.transaction().also { it?.onClose { closeReadTx() } })
            scheduleCloseReadTx()
            return readTx.get()
        }
    }

    private fun scheduleCloseReadTx() = coroutineScope.launch {
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

    private fun closeReadTx() {
        synchronized(this) { readTx.getAndSet(null)?.close() }
    }

    fun close() {
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            entries.forEach { it.closeRecursive() }
            closeReadTx()
        }
    }
}