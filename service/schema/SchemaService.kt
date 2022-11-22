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

package com.vaticle.typedb.studio.service.schema

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.concept.type.*
import com.vaticle.typedb.studio.service.common.ConfirmationService
import com.vaticle.typedb.studio.service.common.NotificationService
import com.vaticle.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.vaticle.typedb.studio.service.common.NotificationService.Notification
import com.vaticle.typedb.studio.service.common.StatusService
import com.vaticle.typedb.studio.service.common.StatusService.Key.SCHEMA_EXCEPTIONS
import com.vaticle.typedb.studio.service.common.StatusService.Status.Type.WARNING
import com.vaticle.typedb.studio.service.common.atomic.AtomicBooleanState
import com.vaticle.typedb.studio.service.common.atomic.AtomicIntegerState
import com.vaticle.typedb.studio.service.common.util.DialogState
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Message.Companion.UNKNOWN
import com.vaticle.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_OPEN_READ_TX
import com.vaticle.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_OPEN_WRITE_TX
import com.vaticle.typedb.studio.service.common.util.Message.Schema.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.service.connection.SessionState
import com.vaticle.typedb.studio.service.page.Navigable
import com.vaticle.typedb.studio.service.page.PageService
import com.vaticle.typedb.studio.service.schema.AttributeTypeState.OwnedAttTypeProperties
import com.vaticle.typedb.studio.service.schema.RoleTypeState.PlayedRoleTypeProperties
import com.vaticle.typedb.studio.service.schema.RoleTypeState.RelatedRoleTypeProperties
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
class SchemaService(
    private val session: SessionState,
    internal val pages: PageService,
    internal val notification: NotificationService,
    internal val confirmation: ConfirmationService,
    private val status: StatusService
) : Navigable<ThingTypeState<*, *>> {

    class TypeDialogState<T : TypeState<*, *>> : DialogState() {

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

    class TypePropertiesDialogState<T : ThingTypeState<*, *>, U : TypeState.OverridingTypeProperties<*>>
        : DialogState() {

        var typeState: T? by mutableStateOf(null); private set
        var properties: U? by mutableStateOf(null); private set

        internal fun open(typeState: T, properties: U) {
            isOpen = true
            this.typeState = typeState
            this.properties = properties
        }

        override fun close() {
            isOpen = false
            typeState = null
        }
    }

    override val name: String = TypeQLToken.Type.THING.name.lowercase()
    override val parent: ThingTypeState<*, *>? = null
    override val info: String? = null
    override val isExpandable: Boolean = true
    override val isBulkExpandable: Boolean = true
    override val entries: List<ThingTypeState<*, *>>
        get() = rootEntityType?.let { listOf(it, rootRelationType!!, rootAttributeType!!) } ?: listOf()

    val isOpen: Boolean get() = isOpenAtomic.state
    val hasRunningTx: Boolean get() = hasRunningWriteAtomic.state || countRunningReadAtomic.state > 0
    var rootEntityType: EntityTypeState? by mutableStateOf(null); private set
    var rootRelationType: RelationTypeState? by mutableStateOf(null); private set
    var rootRoleType: RoleTypeState? by mutableStateOf(null); private set
    var rootAttributeType: AttributeTypeState? by mutableStateOf(null); private set
    val isWritable: Boolean get() = session.isSchema && session.transaction.isWrite
    val createEntityTypeDialog = TypeDialogState<EntityTypeState>()
    val createAttributeTypeDialog = TypeDialogState<AttributeTypeState>()
    val createRelationTypeDialog = TypeDialogState<RelationTypeState>()
    val renameTypeDialog = TypeDialogState<TypeState<*, *>>() // class parameters needed by compiler
    val changeEntitySupertypeDialog = TypeDialogState<EntityTypeState>()
    val changeAttributeSupertypeDialog = TypeDialogState<AttributeTypeState>()
    val changeRelationSupertypeDialog = TypeDialogState<RelationTypeState>()
    val changeOverriddenOwnedAttributeTypeDialog =
        TypePropertiesDialogState<ThingTypeState<*, *>, OwnedAttTypeProperties>()
    val changeOverriddenPlayedRoleTypeDialog =
        TypePropertiesDialogState<ThingTypeState<*, *>, PlayedRoleTypeProperties>()
    val changeOverriddenRelatedRoleTypeDialog =
        TypePropertiesDialogState<RelationTypeState, RelatedRoleTypeProperties>()
    val changeAbstractDialog = TypeDialogState<ThingTypeState<*, *>>()
    private var writeTx: AtomicReference<TypeDBTransaction?> = AtomicReference()
    private var readTx: AtomicReference<TypeDBTransaction?> = AtomicReference()
    private val lastTransactionUse = AtomicLong(0)
    private val entityTypes = ConcurrentHashMap<EntityType, EntityTypeState>()
    private val attributeTypes = ConcurrentHashMap<AttributeType, AttributeTypeState>()
    private val relationTypes = ConcurrentHashMap<RelationType, RelationTypeState>()
    private val roleTypes = ConcurrentHashMap<RoleType, RoleTypeState>()
    private val isOpenAtomic = AtomicBooleanState(false)
    private val onTypesUpdated = LinkedBlockingQueue<() -> Unit>()
    private var hasRunningWriteAtomic = AtomicBooleanState(initValue = false)
    private var countRunningReadAtomic = AtomicIntegerState(initValue = 0)
    private var currentSchemaExceptions = listOf<Notification>()
    private var viewedSchemaExceptions = listOf<Notification>()
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
            updateSchemaExceptionsStatus()
        }
    }

    fun onTypesUpdated(function: () -> Unit) = onTypesUpdated.put(function)

    override fun reloadEntries() = mayRunReadTx { tx ->
        // TODO: Implement API to retrieve .hasSubtypes() on TypeDB Type API
        entries.forEach { it.hasSubtypes = it.conceptType.asRemote(tx).subtypesExplicit.findAny().isPresent }
    } ?: Unit

    override fun compareTo(other: Navigable<ThingTypeState<*, *>>): Int = if (other is SchemaService) 0 else -1

    internal fun typeStateOf(type: ThingType): ThingTypeState<*, *>? = when (type) {
        is EntityType -> typeStateOf(type)
        is RelationType -> typeStateOf(type)
        is AttributeType -> typeStateOf(type)
        else -> throw IllegalStateException("Unrecognised ThingType object")
    }

    internal fun typeStateOf(entityType: EntityType): EntityTypeState? = mayRunReadTx { tx ->
        val remote = entityType.asRemote(tx)
        entityTypes[entityType] ?: let {
            val supertype = remote.supertype?.let { st ->
                if (st.isEntityType) entityTypes[st] ?: typeStateOf(st.asEntityType()) else null
            }
            entityTypes.computeIfAbsent(entityType) {
                EntityTypeState(entityType, supertype, this)
            }
        }
    }

    internal fun typeStateOf(attributeType: AttributeType): AttributeTypeState? = mayRunReadTx { tx ->
        val remote = attributeType.asRemote(tx)
        attributeTypes[attributeType] ?: let {
            val supertype = remote.supertype?.let { st ->
                if (st.isAttributeType) attributeTypes[st] ?: typeStateOf(st.asAttributeType()) else null
            }
            attributeTypes.computeIfAbsent(attributeType) {
                AttributeTypeState(attributeType, supertype, this)
            }
        }
    }

    internal fun typeStateOf(relationType: RelationType): RelationTypeState? = mayRunReadTx { tx ->
        val remote = relationType.asRemote(tx)
        relationTypes[relationType] ?: let {
            val supertype = remote.supertype?.let { st ->
                if (st.isRelationType) relationTypes[st] ?: typeStateOf(st.asRelationType()) else null
            }
            relationTypes.computeIfAbsent(relationType) {
                RelationTypeState(relationType, supertype, this)
            }
        }
    }

    internal fun typeStateOf(roleType: RoleType): RoleTypeState? = mayRunReadTx { tx ->
        val remote = roleType.asRemote(tx)
        roleTypes[roleType] ?: let {
            val supertype = remote.supertype?.let { st ->
                if (st.isRoleType) roleTypes[st] ?: typeStateOf(st.asRoleType()) else null
            }
            typeStateOf(remote.relationType)?.let { relationType ->
                roleTypes.computeIfAbsent(roleType) {
                    RoleTypeState(relationType, it, supertype, this)
                }
            }
        }
    }

    private fun refreshTypesAndOpen() = mayRunReadTx { tx ->
        if (rootEntityType == null) rootEntityType = EntityTypeState(
            conceptType = tx.concepts().rootEntityType, supertype = null, schemaSrv = this
        ).also { entityTypes[tx.concepts().rootEntityType] = it }
        if (rootRelationType == null) rootRelationType = RelationTypeState(
            conceptType = tx.concepts().rootRelationType, supertype = null, schemaSrv = this
        ).also { relationTypes[tx.concepts().rootRelationType] = it }
        val conceptRoleType = tx.concepts().rootRelationType.asRemote(tx).relates.findFirst().get()
        if (rootRoleType == null) rootRoleType = RoleTypeState(
            relationType = rootRelationType!!, conceptType = conceptRoleType, supertype = null, schemaSrv = this
        ).also { roleTypes[conceptRoleType] = it }
        if (rootAttributeType == null) rootAttributeType = AttributeTypeState(
            conceptType = tx.concepts().rootAttributeType, supertype = null, schemaSrv = this
        ).also { attributeTypes[tx.concepts().rootAttributeType] = it }
        (entityTypes.values + attributeTypes.values + relationTypes.values).forEach {
            if (tx.concepts().getThingType(it.name) == null) it.purge()
            else if (it.isOpen) it.loadConstraintsAsync()
        }
        roleTypes.values.forEach {
            val exists = tx.concepts().getThingType(it.relationType.name)
                ?.asRelationType()?.asRemote(tx)?.getRelates(it.name) == null
            if (exists) it.purge()
        }
        isOpenAtomic.set(true)
        execOnTypesUpdated()
    }

    fun exportTypeSchemaAsync(onSuccess: (String) -> Unit) = coroutines.launchAndHandle(notification, LOGGER) {
        session.typeSchema()?.let { onSuccess(it) }
    }

    internal fun mayRunWriteTxAsync(function: (TypeDBTransaction) -> Unit) {
        if (hasRunningWriteAtomic.compareAndSet(expected = false, new = true)) {
            coroutines.launchAndHandle(notification, LOGGER) {
                openOrGetWriteTx()?.let { tx ->
                    function(tx)
                    updateSchemaExceptionsStatus()
                } ?: notification.userWarning(LOGGER, FAILED_TO_OPEN_WRITE_TX)
            }.invokeOnCompletion { hasRunningWriteAtomic.set(false) }
        }
    }

    internal fun <T : Any> mayRunReadTx(function: (TypeDBTransaction) -> T?): T? {
        fun openOrGetReadTx(): TypeDBTransaction? = synchronized(this) {
            lastTransactionUse.set(System.currentTimeMillis())
            if (isWritable && session.transaction.isOpen) return openOrGetWriteTx()
            if (readTx.get() != null) return readTx.get()
            readTx.set(session.transaction()?.also {
                it.onClose { closeReadTx() }
                scheduleCloseReadTxAsync()
            })
            return readTx.get()
        }

        var result: T? = null
        try {
            countRunningReadAtomic.incrementAndGet()
            openOrGetReadTx()?.let {
                result = function(it)
            } ?: notification.userWarning(LOGGER, FAILED_TO_OPEN_READ_TX)
        } catch (e: Throwable) {
            notification.systemError(LOGGER, e, UNEXPECTED_ERROR, e.message ?: UNKNOWN)
        } finally {
            countRunningReadAtomic.decrementAndGet()
        }
        return result
    }

    private fun openOrGetWriteTx(): TypeDBTransaction? = synchronized(this) {
        if (!isWritable) return null
        if (readTx.get() != null) closeReadTx()
        if (writeTx.get() != null) return writeTx.get()
        writeTx.set(session.transaction.tryOpen()?.also { it.onClose { writeTx.set(null) } })
        return writeTx.get()
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

    private fun updateSchemaExceptionsStatus() = mayRunReadTx { tx ->
        val exceptions = tx.concepts().schemaExceptions
        currentSchemaExceptions = exceptions.map {
            Notification(Notification.Type.WARNING, it.code, it.message)
        }.toList()
        if (exceptions.isEmpty()) status.clear(SCHEMA_EXCEPTIONS)
        else status.publish(
            key = SCHEMA_EXCEPTIONS,
            status = exceptions.size.toString() + " " + Label.SCHEMA_EXCEPTIONS,
            type = WARNING
        ) {
            viewedSchemaExceptions.forEach { e -> notification.dismiss(e) }
            currentSchemaExceptions.forEach { e -> notification.userNotification(notification = e) }
            viewedSchemaExceptions = currentSchemaExceptions
        }
    }

    internal fun execOnTypesUpdated() = onTypesUpdated.forEach { it() }

    fun closeWriteTx() = synchronized(this) { writeTx.getAndSet(null)?.close() }

    fun closeReadTx() = synchronized(this) { readTx.getAndSet(null)?.close() }

    fun register(typeState: TypeState<*, *>) = when (typeState) {
        is EntityTypeState -> entityTypes[typeState.conceptType] = typeState
        is AttributeTypeState -> attributeTypes[typeState.conceptType] = typeState
        is RelationTypeState -> relationTypes[typeState.conceptType] = typeState
        is RoleTypeState -> roleTypes[typeState.conceptType] = typeState
    }

    fun remove(typeState: TypeState<*, *>) = when (typeState) {
        is EntityTypeState -> entityTypes.remove(typeState.conceptType)
        is AttributeTypeState -> attributeTypes.remove(typeState.conceptType)
        is RelationTypeState -> relationTypes.remove(typeState.conceptType)
        is RoleTypeState -> roleTypes.remove(typeState.conceptType)
    }

    fun close() {
        if (isOpenAtomic.compareAndSet(expected = true, new = false)) {
            entries.forEach { it.purge() }
            rootEntityType = null
            rootRelationType = null
            rootAttributeType = null
            rootRoleType = null
            entityTypes.values.forEach { it.close() }
            attributeTypes.values.forEach { it.close() }
            relationTypes.values.forEach { it.close() }
            entityTypes.clear()
            attributeTypes.clear()
            relationTypes.clear()
            roleTypes.clear()
            closeWriteTx()
            closeReadTx()
        }
    }
}