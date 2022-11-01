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
import com.vaticle.typedb.client.api.concept.type.*
import com.vaticle.typedb.studio.state.app.ConfirmationManager
import com.vaticle.typedb.studio.state.app.DialogManager
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.common.atomic.AtomicBooleanState
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_OPEN_WRITE_TX
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
class SchemaManager constructor(
    private val session: SessionState,
    internal val pages: PageManager,
    internal val notification: NotificationManager,
    internal val confirmation: ConfirmationManager
) : Navigable<TypeState.Thing<*, *>> {

    class TypeDialogManager<T : TypeState<*, *>> : DialogManager() {

        var typeState: T? by mutableStateOf(null); private set
        var onSuccess: (() -> Unit)? by mutableStateOf(null); private set

        internal fun open(typeState: T, onSuccess: (() -> Unit)? = null) {
            isOpen = true
            this.typeState = typeState
            this.onSuccess = onSuccess
        }

        override fun close() {
            isOpen = false
            typeState = null
            onSuccess = null
        }
    }

    override val name: String = TypeQLToken.Type.THING.name.lowercase()
    override val parent: TypeState.Thing<*, *>? = null
    override val info: String? = null
    override val isExpandable: Boolean = true
    override val isBulkExpandable: Boolean = true
    override val entries: List<TypeState.Thing<*, *>>
        get() = rootEntityType?.let { listOf(it, rootRelationType!!, rootAttributeType!!) } ?: listOf()

    val isOpen: Boolean get() = isOpenAtomic.state
    val hasRunningWrite: Boolean get() = hasRunningWriteAtomic.state
    var rootEntityType: TypeState.Entity? by mutableStateOf(null); private set
    var rootRelationType: TypeState.Relation? by mutableStateOf(null); private set
    var rootRoleType: TypeState.Role? by mutableStateOf(null); private set
    var rootAttributeType: TypeState.Attribute? by mutableStateOf(null); private set
    val isWritable: Boolean get() = session.isSchema && session.transaction.isWrite
    val createEntityTypeDialog = TypeDialogManager<TypeState.Entity>()
    val createAttributeTypeDialog = TypeDialogManager<TypeState.Attribute>()
    val createRelationTypeDialog = TypeDialogManager<TypeState.Relation>()
    val renameTypeDialog = TypeDialogManager<TypeState<*, *>>()
    val changeEntitySupertypeDialog = TypeDialogManager<TypeState.Entity>()
    val changeAttributeSupertypeDialog = TypeDialogManager<TypeState.Attribute>()
    val changeRelationSupertypeDialog = TypeDialogManager<TypeState.Relation>()
    val changeOverriddenRoleTypeDialog = TypeDialogManager<TypeState.Role>()
    val changeAbstractDialog = TypeDialogManager<TypeState.Thing<*, *>>()
    private var writeTx: AtomicReference<TypeDBTransaction?> = AtomicReference()
    private var readTx: AtomicReference<TypeDBTransaction?> = AtomicReference()
    private val lastTransactionUse = AtomicLong(0)
    private val entityTypes = ConcurrentHashMap<EntityType, TypeState.Entity>()
    private val attributeTypes = ConcurrentHashMap<AttributeType, TypeState.Attribute>()
    private val relationTypes = ConcurrentHashMap<RelationType, TypeState.Relation>()
    private val roleTypes = ConcurrentHashMap<RoleType, TypeState.Role>()
    private val isOpenAtomic = AtomicBooleanState(false)
    private val onTypesUpdated = LinkedBlockingQueue<() -> Unit>()
    private var hasRunningWriteAtomic = AtomicBooleanState(false)
    internal val database: String? get() = session.database
    internal val coroutines = CoroutineScope(Dispatchers.Default)

    companion object {
        private val TX_IDLE_TIME = Duration.seconds(16)
        private val LOGGER = KotlinLogging.logger {}
    }

    init {
        session.onOpen { refreshTypesAndOpen() }
        session.onClose { close() }
        session.transaction.onSchemaWriteReset {
            closeReadTx()
            refreshTypesAndOpen()
        }
    }

    fun onTypesUpdated(function: () -> Unit) = onTypesUpdated.put(function)

    override fun reloadEntries() = openOrGetReadTx()?.let { tx ->
        // TODO: Implement API to retrieve .hasSubtypes() on TypeDB Type API
        entries.forEach { it.hasSubtypes = it.conceptType.asRemote(tx).subtypesExplicit.findAny().isPresent }
    } ?: Unit

    override fun compareTo(other: Navigable<TypeState.Thing<*, *>>): Int = if (other is SchemaManager) 0 else -1

    internal fun typeStateOf(type: ThingType): TypeState.Thing<*, *>? = when (type) {
        is EntityType -> typeStateOf(type)
        is RelationType -> typeStateOf(type)
        is AttributeType -> typeStateOf(type)
        else -> throw IllegalStateException("Unrecognised ThingType object")
    }

    internal fun typeStateOf(entityType: EntityType): TypeState.Entity? = openOrGetReadTx()?.let { tx ->
        val remote = entityType.asRemote(tx)
        entityTypes[entityType] ?: let {
            val supertype = remote.supertype?.let { st ->
                if (st.isEntityType) entityTypes[st] ?: typeStateOf(st.asEntityType()) else null
            }
            entityTypes.computeIfAbsent(entityType) {
                TypeState.Entity(entityType, supertype, this)
            }
        }
    }

    internal fun typeStateOf(attributeType: AttributeType): TypeState.Attribute? = openOrGetReadTx()?.let { tx ->
        val remote = attributeType.asRemote(tx)
        attributeTypes[attributeType] ?: let {
            val supertype = remote.supertype?.let { st ->
                if (st.isAttributeType) attributeTypes[st] ?: typeStateOf(st.asAttributeType()) else null
            }
            attributeTypes.computeIfAbsent(attributeType) {
                TypeState.Attribute(attributeType, supertype, this)
            }
        }
    }

    internal fun typeStateOf(relationType: RelationType): TypeState.Relation? = openOrGetReadTx()?.let { tx ->
        val remote = relationType.asRemote(tx)
        relationTypes[relationType] ?: let {
            val supertype = remote.supertype?.let { st ->
                if (st.isRelationType) relationTypes[st] ?: typeStateOf(st.asRelationType()) else null
            }
            relationTypes.computeIfAbsent(relationType) {
                TypeState.Relation(relationType, supertype, this)
            }
        }
    }

    internal fun typeStateOf(roleType: RoleType): TypeState.Role? = openOrGetReadTx()?.let { tx ->
        val remote = roleType.asRemote(tx)
        roleTypes[roleType] ?: let {
            val supertype = remote.supertype?.let { st ->
                if (st.isRoleType) roleTypes[st] ?: typeStateOf(st.asRoleType()) else null
            }
            typeStateOf(remote.relationType)?.let { relationType ->
                roleTypes.computeIfAbsent(roleType) {
                    TypeState.Role(relationType, it, supertype, this)
                }
            }
        }
    }

    private fun refreshTypesAndOpen() = openOrGetReadTx()?.let { tx ->
        if (rootEntityType == null) rootEntityType = TypeState.Entity(
            conceptType = tx.concepts().rootEntityType, supertype = null, schemaMgr = this
        ).also { entityTypes[tx.concepts().rootEntityType] = it }
        if (rootRelationType == null) rootRelationType = TypeState.Relation(
            conceptType = tx.concepts().rootRelationType, supertype = null, schemaMgr = this
        ).also { relationTypes[tx.concepts().rootRelationType] = it }
        val conceptRoleType = tx.concepts().rootRelationType.asRemote(tx).relates.findFirst().get()
        if (rootRoleType == null) rootRoleType = TypeState.Role(
            relationType = rootRelationType!!, conceptType = conceptRoleType, supertype = null, schemaMgr = this
        ).also { roleTypes[conceptRoleType] = it }
        if (rootAttributeType == null) rootAttributeType = TypeState.Attribute(
            conceptType = tx.concepts().rootAttributeType, supertype = null, schemaMgr = this
        ).also { attributeTypes[tx.concepts().rootAttributeType] = it }
        (entityTypes.values + attributeTypes.values + relationTypes.values).forEach {
            if (tx.concepts().getThingType(it.name) == null) it.purge()
            else if (it.isOpen) it.loadConstraintsAsync()
        }
        roleTypes.values.forEach {
            if (it.relationType.conceptType.asRemote(tx).getRelates(it.name) == null) it.purge()
        }
        isOpenAtomic.set(true)
        execOnTypesUpdated()
    }

    fun exportTypeSchemaAsync(onSuccess: (String) -> Unit) = coroutines.launchAndHandle(notification, LOGGER) {
        session.typeSchema()?.let { onSuccess(it) }
    }

    internal fun mayWriteAsync(function: (TypeDBTransaction) -> Unit) {
        if (hasRunningWriteAtomic.compareAndSet(expected = false, new = true)) {
            coroutines.launchAndHandle(notification, LOGGER) {
                openOrGetWriteTx()?.let { function(it) } ?: notification.userWarning(LOGGER, FAILED_TO_OPEN_WRITE_TX)
            }.invokeOnCompletion { hasRunningWriteAtomic.set(false) }
        }
    }

    private fun openOrGetWriteTx(): TypeDBTransaction? = synchronized(this) {
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
            scheduleCloseReadTxAsync()
        })
        return readTx.get()
    }

    private fun scheduleCloseReadTxAsync() = coroutines.launchAndHandle(notification, LOGGER) {
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

    internal fun execOnTypesUpdated() = onTypesUpdated.forEach { it() }

    fun closeWriteTx() = synchronized(this) { writeTx.getAndSet(null)?.close() }

    fun closeReadTx() = synchronized(this) { readTx.getAndSet(null)?.close() }

    fun register(typeState: TypeState<*, *>) = when (typeState) {
        is TypeState.Entity -> entityTypes[typeState.conceptType] = typeState
        is TypeState.Attribute -> attributeTypes[typeState.conceptType] = typeState
        is TypeState.Relation -> relationTypes[typeState.conceptType] = typeState
        is TypeState.Role -> roleTypes[typeState.conceptType] = typeState
    }

    fun remove(typeState: TypeState<*, *>) = when (typeState) {
        is TypeState.Entity -> entityTypes.remove(typeState.conceptType)
        is TypeState.Attribute -> attributeTypes.remove(typeState.conceptType)
        is TypeState.Relation -> relationTypes.remove(typeState.conceptType)
        is TypeState.Role -> roleTypes.remove(typeState.conceptType)
    }

    fun close() {
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            entries.forEach { it.purge() }
            rootEntityType = null
            rootRelationType = null
            rootAttributeType = null
            rootRoleType = null
            entityTypes.clear()
            attributeTypes.clear()
            relationTypes.clear()
            roleTypes.clear()
            closeWriteTx()
            closeReadTx()
        }
    }
}