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
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.state.common.NotificationService.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.state.common.util.Message.Companion.UNKNOWN
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_CHANGE_ABSTRACT
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_CHANGE_SUPERTYPE
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_CREATE_TYPE
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_CREATE_TYPE_DUE_TO_DUPLICATE
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_DELETE_TYPE
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_OWN_ATTRIBUTE_TYPE
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_PLAY_ROLE_TYPE
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_RELATE_ROLE_TYPE
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_RENAME_TYPE
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.page.Navigable
import com.vaticle.typedb.studio.state.page.Pageable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.streams.toList
import mu.KotlinLogging

sealed class TypeState<T : Type, TS : TypeState<T, TS>> private constructor(
    conceptType: T, supertype: TS?, val encoding: Encoding, val schemaSrv: SchemaService
) {

    enum class Encoding(val label: String) {
        ENTITY_TYPE(Label.ENTITY.lowercase()),
        ATTRIBUTE_TYPE(Label.ATTRIBUTE.lowercase()),
        RELATION_TYPE(Label.RELATION.lowercase()),
        ROLE_TYPE(Label.ROLE.lowercase())
    }

    data class AttributeTypeProperties(
        val attributeType: Attribute,
        val overriddenType: Attribute?,
        val extendedType: Thing<*, *>?,
        val isInherited: Boolean,
        val isKey: Boolean,
        val canBeUndefined: Boolean,
    )

    data class OwnerTypeProperties constructor(
        val ownerType: Thing<*, *>,
        val extendedType: Thing<*, *>?,
        val isInherited: Boolean,
        val isKey: Boolean,
    )

    interface RoleTypeProperties {
        val roleType: Role
        val overriddenType: Role?
        val extendedType: Thing<*, *>?
        val isInherited: Boolean
    }

    data class RelatesRoleTypeProperties constructor(
        override val roleType: Role,
        override val overriddenType: Role?,
        override val extendedType: Relation?,
        override val isInherited: Boolean,
    ) : RoleTypeProperties

    data class PlaysRoleTypeProperties constructor(
        override val roleType: Role,
        override val overriddenType: Role?,
        override val extendedType: Thing<*, *>?,
        override val isInherited: Boolean,
    ) : RoleTypeProperties

    data class PlayerTypeProperties constructor(
        val playerType: Thing<*, *>,
        val isInherited: Boolean,
    )

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    abstract val canBeDeleted: Boolean
    abstract val canBeAbstract: Boolean

    var conceptType: T by mutableStateOf(conceptType)
    var supertype: TS? by mutableStateOf(supertype)
    var supertypes: List<TS> by mutableStateOf(this.supertype?.let { listOf(it) } ?: listOf()) // exclude self
    var subtypesExplicit: List<TS> by mutableStateOf(listOf())
    val subtypes: List<TS> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten() // exclude self
    val subtypesWithSelf: List<TS> get() = listOf(this as TS) + subtypes

    val isRoot get() = conceptType.isRoot
    var name: String by mutableStateOf(conceptType.label.name())
    var isAbstract by mutableStateOf(conceptType.isAbstract)
    var hasSubtypes: Boolean by mutableStateOf(false)
    val notifications get() = schemaSrv.notification
    val coroutines get() = schemaSrv.coroutines

    protected abstract fun isSameEncoding(conceptType: Type): Boolean
    protected abstract fun asSameEncoding(conceptType: Type): T
    protected abstract fun typeStateOf(type: T): TS?
    protected abstract fun requestSubtypesExplicit(): List<T>?
    protected abstract fun loadInheritables()
    internal abstract fun loadDependencies()
    internal abstract fun updateConceptType(label: String = name)
    abstract override fun toString(): String

    fun loadSupertypesAsync() = coroutines.launchAndHandle(notifications, LOGGER) { loadSupertypes() }

    fun loadSupertypes(): Unit = schemaSrv.mayRunReadTx { tx ->
        val typeTx = conceptType.asRemote(tx)
        supertype = typeTx.supertype?.let {
            if (isSameEncoding(it)) typeStateOf(asSameEncoding(it)) else null
        }?.also { it.loadInheritables() }
        supertype?.loadSupertypes()
        supertypes = supertype?.let { listOf(it) + it.supertypes } ?: listOf()
    } ?: Unit

    protected fun loadHasSubtypes() = schemaSrv.mayRunReadTx {
        // TODO: Implement API to retrieve .hasSubtypes() on TypeDB Type API
        hasSubtypes = conceptType.asRemote(it).subtypesExplicit.findAny().isPresent
    }

    fun loadSubtypesRecursivelyAsync() = coroutines.launchAndHandle(notifications, LOGGER) {
        loadSubtypesRecursively()
    }

    protected fun loadSubtypesRecursively() {
        loadSubtypesExplicit()
        subtypesExplicit.forEach { it.loadSubtypesRecursively() }
    }

    fun removeSubtypeExplicit(subtype: TS) {
        subtypesExplicit = subtypesExplicit.filter { it != subtype }
    }

    protected fun loadSubtypesExplicit(): Unit = synchronized(this) {
        requestSubtypesExplicit()?.let { list ->
            val new = list.toSet()
            val old = subtypesExplicit.map { it.conceptType }.toSet()
            val retained: List<TS>
            if (new != old) {
                val deleted = old - new
                val added = (new - old).mapNotNull { typeStateOf(it) }
                retained = subtypesExplicit.filter { !deleted.contains(it.conceptType) }
                subtypesExplicit = (retained + added).sortedBy { it.conceptType.label.scopedName() }
            }
            subtypesExplicit.onEach { it.loadDependencies() }
            hasSubtypes = subtypesExplicit.isNotEmpty()
        }
    }

    fun initiateRename() = schemaSrv.renameTypeDialog.open(this)

    fun tryRename(label: String) = schemaSrv.mayRunWriteTxAsync {
        try {
            conceptType.asRemote(it).setLabel(label)
            schemaSrv.remove(this)
            updateConceptType(label)
            schemaSrv.register(this)
            schemaSrv.execOnTypesUpdated()
            schemaSrv.renameTypeDialog.close()
        } catch (e: Exception) {
            notifications.userError(
                LOGGER, FAILED_TO_RENAME_TYPE, encoding.label, conceptType.label, label, e.message ?: UNKNOWN
            )
        }
    }

    protected fun tryChangeSupertype(
        dialogState: SchemaService.TypeDialogState<*>, function: (TypeDBTransaction) -> Unit
    ) = schemaSrv.mayRunWriteTxAsync {
        try {
            function(it)
            dialogState.onSuccess?.invoke()
            dialogState.close()
            when (this) {
                is Thing -> loadConstraints()
                is Role -> relationType.loadConstraints()
            }
        } catch (e: Exception) {
            notifications.userError(
                LOGGER, FAILED_TO_CHANGE_SUPERTYPE, encoding.label, conceptType.label, e.message ?: UNKNOWN
            )
        }
    }

    open fun purge() {
        supertype?.removeSubtypeExplicit(this as TS)
        schemaSrv.remove(this)
        subtypesExplicit.forEach { it.purge() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TS
        return this.conceptType == other.conceptType
    }

    override fun hashCode(): Int {
        return conceptType.hashCode()
    }

    sealed class Thing<TT : ThingType, TTS : Thing<TT, TTS>> constructor(
        conceptType: TT, supertype: TTS?, encoding: Encoding, schemaSrv: SchemaService
    ) : TypeState<TT, TTS>(conceptType, supertype, encoding, schemaSrv), Navigable<Thing<TT, TTS>>, Pageable {

        private class Callbacks {

            val onReopen = LinkedBlockingQueue<(Thing<*, *>) -> Unit>()
            val onClose = LinkedBlockingQueue<(Thing<*, *>) -> Unit>()
            val onSubtypesUpdated = LinkedBlockingQueue<() -> Unit>()

            fun clear() {
                onReopen.clear()
                onClose.clear()
                onSubtypesUpdated.clear()
            }
        }

        var ownsAttTypeProperties: List<AttributeTypeProperties> by mutableStateOf(emptyList())
        val ownsAttTypes: List<Attribute> get() = ownsAttTypeProperties.map { it.attributeType }
        var playsRoleTypeProperties: List<PlaysRoleTypeProperties> by mutableStateOf(emptyList())
        val playsRoleTypes: List<Role> get() = playsRoleTypeProperties.map { it.roleType }

        private var hasInstancesExplicit: Boolean by mutableStateOf(false)
        override val canBeDeleted get() = !hasSubtypes && !hasInstancesExplicit
        override val canBeAbstract get() = !hasInstancesExplicit

        override val info: String? = null
        override val isBulkExpandable: Boolean = true
        override val isExpandable: Boolean get() = hasSubtypes
        override val entries: List<TTS> get() = subtypesExplicit

        override val windowTitle: String get() = computeWindowTitle()
        override val isOpen: Boolean get() = isOpenAtomic.get()
        override val isWritable: Boolean = true
        override val isEmpty: Boolean = false
        override val isUnsavedPageable: Boolean = false
        override val hasUnsavedChanges: Boolean by mutableStateOf(false)

        private val isOpenAtomic = AtomicBoolean(false)
        private val callbacks = Callbacks()

        private fun computeWindowTitle(): String {
            val props = listOf(encoding.label) + info?.let { listOf(it) }
            return "$name (" + props.joinToString(", ") + ") @ " + schemaSrv.database
        }

        override fun deactivate() {}
        override fun execBeforeClose() {}
        override fun initiateSave(reopen: Boolean) {}
        override fun reloadEntries() = loadSubtypesExplicit()
        override fun onReopen(function: (Pageable) -> Unit) = callbacks.onReopen.put(function)
        override fun onClose(function: (Pageable) -> Unit) = callbacks.onClose.put(function)
        override fun compareTo(other: Navigable<Thing<TT, TTS>>): Int = name.compareTo(other.name)

        abstract fun initiateCreateSubtype(onSuccess: () -> Unit)
        abstract fun tryCreateSubtype(label: String, isAbstract: Boolean)
        abstract fun initiateChangeSupertype()
        abstract fun tryChangeSupertype(supertypeState: TTS)

        fun onSubtypesUpdated(function: () -> Unit) = callbacks.onSubtypesUpdated.put(function)

        override fun updateConceptType(label: String) = schemaSrv.mayRunReadTx {
            val newConceptType = asSameEncoding(it.concepts().getThingType(label)!!)
            isAbstract = newConceptType.isAbstract
            name = newConceptType.label.name()
            conceptType = newConceptType // we need to update the mutable state last
        } ?: Unit

        override fun tryOpen(): Boolean {
            isOpenAtomic.set(true)
            callbacks.onReopen.forEach { it(this) }
            schemaSrv.pages.opened(this)
            loadConstraintsAsync()
            return true
        }

        override fun activate() {
            schemaSrv.pages.active(this)
            loadConstraintsAsync()
        }

        fun exportSyntaxAsync(onSuccess: (String) -> Unit) = coroutines.launchAndHandle(notifications, LOGGER) {
            schemaSrv.mayRunReadTx { tx -> conceptType.asRemote(tx).syntax?.let { onSuccess(it) } }
        }

        fun loadConstraintsAsync() = coroutines.launchAndHandle(notifications, LOGGER) {
            loadConstraints()
        }

        fun loadConstraints() = schemaSrv.mayRunReadTx {
            try {
                loadSupertypes()
                loadOtherConstraints()
                loadSubtypesRecursively()
            } catch (e: TypeDBClientException) {
                notifications.userError(
                    LOGGER, Message.Schema.FAILED_TO_LOAD_TYPE, e.message ?: UNKNOWN
                )
            }
        }

        fun loadTypeDependenciesAsync() = coroutines.launchAndHandle(notifications, LOGGER) {
            loadDependencies()
        }

        override fun loadDependencies() {
            loadHasSubtypes()
            loadHasInstancesExplicit()
        }

        override fun loadInheritables() {
            loadOwnsAttributeTypes()
            loadPlaysRoleTypes()
        }

        open fun loadOtherConstraints() {
            loadHasInstancesExplicit()
            loadOwnsAttributeTypes()
            loadPlaysRoleTypes()
        }

        private fun loadHasInstancesExplicit() = schemaSrv.mayRunReadTx {
            hasInstancesExplicit = conceptType.asRemote(it).instancesExplicit.findAny().isPresent
        }

        private fun loadOwnsAttributeTypes() {
            val loaded = mutableSetOf<AttributeType>()
            val properties = mutableListOf<AttributeTypeProperties>()

            fun load(typeTx: ThingType.Remote, attTypeConcept: AttributeType, isKey: Boolean, isInherited: Boolean) {
                loaded.add(attTypeConcept)
                schemaSrv.typeStateOf(attTypeConcept)?.let { attType ->
                    val overriddenType = typeTx.getOwnsOverridden(attTypeConcept)?.let { schemaSrv.typeStateOf(it) }
                    val inheritedType = when {
                        isInherited -> attType
                        else -> overriddenType
                    }?.also { it.loadOwnerTypes() }
                    val extendedType = inheritedType?.ownerTypesExplicit?.toSet()
                        ?.intersect(supertypes.toSet())?.firstOrNull()
                    val canBeUndefined = false // TODO
                    properties.add(
                        AttributeTypeProperties(
                            attType, overriddenType, extendedType, isInherited, isKey, canBeUndefined
                        )
                    )
                }
            }

            schemaSrv.mayRunReadTx { tx ->
                val typeTx = conceptType.asRemote(tx)
                typeTx.getOwnsExplicit(true).forEach {
                    load(typeTx = typeTx, attTypeConcept = it, isKey = true, isInherited = false)
                }
                typeTx.getOwnsExplicit(false).filter { !loaded.contains(it) }.forEach {
                    load(typeTx = typeTx, attTypeConcept = it, isKey = false, isInherited = false)
                }
                typeTx.getOwns(true).filter { !loaded.contains(it) }.forEach {
                    load(typeTx = typeTx, attTypeConcept = it, isKey = true, isInherited = true)
                }
                typeTx.getOwns(false).filter { !loaded.contains(it) }.forEach {
                    load(typeTx = typeTx, attTypeConcept = it, isKey = false, isInherited = true)
                }
            }
            ownsAttTypeProperties = properties
        }

        private fun loadPlaysRoleTypes() {
            val loaded = mutableSetOf<RoleType>()
            val properties = mutableListOf<PlaysRoleTypeProperties>()

            fun load(typeTx: ThingType.Remote, roleTypeConcept: RoleType, isInherited: Boolean) {
                loaded.add(roleTypeConcept)
                schemaSrv.typeStateOf(roleTypeConcept)?.let { roleType ->
                    roleType.loadConstraints()
                    val overriddenType = typeTx.getPlaysOverridden(roleTypeConcept)?.let { schemaSrv.typeStateOf(it) }
                    val inheritedType = when {
                        isInherited -> roleType
                        else -> overriddenType
                    }?.also { it.loadPlayerTypes() }
                    val extendedType = inheritedType?.playerTypesExplicit?.toSet()
                        ?.intersect(supertypes.toSet())?.firstOrNull()
                    properties.add(PlaysRoleTypeProperties(roleType, overriddenType, extendedType, isInherited))
                }
            }

            schemaSrv.mayRunReadTx { tx ->
                val typeTx = conceptType.asRemote(tx)
                typeTx.playsExplicit.forEach { load(typeTx, it, false) }
                typeTx.plays.filter { !loaded.contains(it) }.forEach { load(typeTx, it, true) }
            }
            playsRoleTypeProperties = properties
        }

        protected fun tryCreateSubtype(
            label: String, dialogState: SchemaService.TypeDialogState<*>, creatorFn: (TypeDBTransaction) -> Unit
        ) {
            if (schemaSrv.mayRunReadTx { it.concepts()?.getThingType(label) } != null) notifications.userError(
                LOGGER, FAILED_TO_CREATE_TYPE_DUE_TO_DUPLICATE, encoding.label, label
            ) else schemaSrv.mayRunWriteTxAsync { tx ->
                try {
                    creatorFn(tx)
                    dialogState.onSuccess?.invoke()
                    dialogState.close()
                    execOnTypesUpdated()
                } catch (e: Exception) {
                    notifications.userError(LOGGER, FAILED_TO_CREATE_TYPE, encoding.label, label, e.message ?: UNKNOWN)
                }
            }
        }

        fun initiateChangeAbstract() = schemaSrv.changeAbstractDialog.open(this)

        fun tryChangeAbstract(isAbstract: Boolean) = schemaSrv.mayRunWriteTxAsync { tx ->
            try {
                conceptType.asRemote(tx).let { if (isAbstract) it.setAbstract() else it.unsetAbstract() }
                schemaSrv.changeAbstractDialog.close()
                updateConceptType()
            } catch (e: Exception) {
                notifications.userError(LOGGER, FAILED_TO_CHANGE_ABSTRACT, encoding.label, e.message ?: UNKNOWN)
            }
        }

        fun tryDefineOwnsAttributeType(
            attributeType: Attribute, overriddenType: Attribute?, isKey: Boolean, onSuccess: () -> Unit
        ) = schemaSrv.mayRunWriteTxAsync { tx ->
            try {
                overriddenType?.let {
                    conceptType.asRemote(tx).setOwns(attributeType.conceptType, overriddenType.conceptType, isKey)
                } ?: conceptType.asRemote(tx).setOwns(attributeType.conceptType, isKey)
                loadOwnsAttributeTypes()
                onSuccess()
            } catch (e: Exception) {
                notifications.userError(
                    LOGGER, FAILED_TO_OWN_ATTRIBUTE_TYPE,
                    encoding.label, name, attributeType.name, e.message ?: UNKNOWN
                )
            }
        }

        fun initiateRemoveOwnsAttributeType(attributeType: Attribute) {
            // TODO
        }

        fun tryDefinePlaysRoleType(
            roleType: Role, overriddenType: Role?, onSuccess: () -> Unit
        ) = schemaSrv.mayRunWriteTxAsync { tx ->
            try {
                overriddenType?.let {
                    conceptType.asRemote(tx).setPlays(roleType.conceptType, it.conceptType)
                } ?: conceptType.asRemote(tx).setPlays(roleType.conceptType)
                loadPlaysRoleTypes()
                onSuccess()
            } catch (e: Exception) {
                notifications.userError(
                    LOGGER, FAILED_TO_PLAY_ROLE_TYPE, encoding.label, name, roleType.name, e.message ?: UNKNOWN
                )
            }
        }

        fun initiateRemovePlaysRoleType(roleType: Role) {
            // TODO
        }

        fun initiateDelete() = schemaSrv.confirmation.submit(
            title = Label.CONFIRM_TYPE_DELETION,
            message = Sentence.CONFIRM_TYPE_DELETION.format(encoding.label, name),
            onConfirm = { this.tryDelete() }
        )

        override fun tryDelete() = schemaSrv.mayRunWriteTxAsync {
            try {
                conceptType.asRemote(it).delete()
                purge()
                schemaSrv.execOnTypesUpdated()
            } catch (e: Exception) {
                notifications.userError(LOGGER, FAILED_TO_DELETE_TYPE, encoding.label, e.message ?: UNKNOWN)
            }
        }

        private fun execOnTypesUpdated() {
            callbacks.onSubtypesUpdated.forEach { it() }
            supertype?.execOnTypesUpdated() ?: schemaSrv.execOnTypesUpdated()
        }

        override fun close() {
            if (isOpenAtomic.compareAndSet(true, false)) {
                schemaSrv.pages.close(this)
                callbacks.onClose.forEach { it(this) }
                callbacks.clear()
            }
        }

        override fun purge() {
            close()
            super.purge()
        }
    }

    class Entity internal constructor(
        conceptType: EntityType, supertype: Entity?, schemaSrv: SchemaService
    ) : Thing<EntityType, Entity>(conceptType, supertype, Encoding.ENTITY_TYPE, schemaSrv) {

        override val parent: Entity? get() = supertype

        override fun isSameEncoding(conceptType: Type) = conceptType.isEntityType
        override fun asSameEncoding(conceptType: Type) = conceptType.asEntityType()!!
        override fun typeStateOf(type: EntityType) = schemaSrv.typeStateOf(type)

        override fun requestSubtypesExplicit() = schemaSrv.mayRunReadTx {
            conceptType.asRemote(it).subtypesExplicit.toList()
        }

        override fun initiateCreateSubtype(onSuccess: () -> Unit) =
            schemaSrv.createEntityTypeDialog.open(this, onSuccess)

        override fun tryCreateSubtype(
            label: String, isAbstract: Boolean
        ) = tryCreateSubtype(label, schemaSrv.createEntityTypeDialog) { tx ->
            val type = tx.concepts().putEntityType(label)
            if (isAbstract || !isRoot) {
                val typeTx = type.asRemote(tx)
                if (isAbstract) typeTx.setAbstract()
                if (!isRoot) typeTx.setSupertype(conceptType)
            }
        }

        override fun initiateChangeSupertype() = schemaSrv.changeEntitySupertypeDialog.open(this) {
            schemaSrv.execOnTypesUpdated()
            loadConstraintsAsync()
        }

        override fun tryChangeSupertype(
            supertypeState: Entity
        ) = super.tryChangeSupertype(schemaSrv.changeEntitySupertypeDialog) {
            conceptType.asRemote(it).setSupertype(supertypeState.conceptType)
        }

        override fun toString(): String = "TypeState.Entity: $conceptType"
    }

    class Attribute internal constructor(
        conceptType: AttributeType, supertype: Attribute?, schemaSrv: SchemaService
    ) : Thing<AttributeType, Attribute>(conceptType, supertype, Encoding.ATTRIBUTE_TYPE, schemaSrv) {

        override val info get() = valueType?.name?.lowercase()
        override val parent: Attribute? get() = supertype

        val valueType: AttributeType.ValueType? = if (!conceptType.isRoot) conceptType.valueType else null
        val isKeyable: Boolean get() = conceptType.valueType.isKeyable
        var ownerTypeProperties: List<OwnerTypeProperties> by mutableStateOf(listOf())
        val ownerTypes get() = ownerTypeProperties.map { it.ownerType }
        val ownerTypesExplicit get() = ownerTypeProperties.filter { !it.isInherited }.map { it.ownerType }

        override fun isSameEncoding(conceptType: Type) = conceptType.isAttributeType
        override fun asSameEncoding(conceptType: Type) = conceptType.asAttributeType()!!
        override fun typeStateOf(type: AttributeType) = schemaSrv.typeStateOf(type)

        override fun requestSubtypesExplicit() = schemaSrv.mayRunReadTx {
            conceptType.asRemote(it).subtypesExplicit.toList()
        }

        override fun loadOtherConstraints() {
            super.loadOtherConstraints()
            loadOwnerTypes()
        }

        fun loadOwnerTypes() {
            val loaded = mutableSetOf<ThingType>()
            val properties = mutableListOf<OwnerTypeProperties>()

            fun load(ownerTypeConcept: ThingType, isKey: Boolean, isInherited: Boolean) {
                loaded.add(ownerTypeConcept)
                schemaSrv.typeStateOf(ownerTypeConcept.asThingType())?.let { ownerType ->
                    val extendedType = if (isInherited) {
                        ownerType.loadSupertypes()
                        ownerType.supertypes.toSet().intersect(
                            properties.filter { !it.isInherited }.map { it.ownerType }.toSet()
                        ).firstOrNull()
                    } else null
                    properties.add(OwnerTypeProperties(ownerType, extendedType, isInherited, isKey))
                }
            }

            schemaSrv.mayRunReadTx { tx ->
                val typeTx = conceptType.asRemote(tx)
                typeTx.getOwnersExplicit(true).forEach {
                    load(it, isKey = true, isInherited = false)
                }
                typeTx.getOwnersExplicit(false).filter { !loaded.contains(it) }.forEach {
                    load(it, isKey = false, isInherited = false)
                }
                typeTx.getOwners(true).filter { !loaded.contains(it) }.forEach {
                    load(it, isKey = true, isInherited = true)
                }
                typeTx.getOwners(false).filter { !loaded.contains(it) }.forEach {
                    load(it, isKey = false, isInherited = true)
                }
            }

            ownerTypeProperties = properties
        }

        override fun initiateCreateSubtype(onSuccess: () -> Unit) =
            schemaSrv.createAttributeTypeDialog.open(this, onSuccess)

        override fun tryCreateSubtype(label: String, isAbstract: Boolean) = tryCreateSubtype(
            label, isAbstract, conceptType.valueType
        )

        fun tryCreateSubtype(
            label: String, isAbstract: Boolean, valueType: AttributeType.ValueType
        ) = tryCreateSubtype(label, schemaSrv.createAttributeTypeDialog) { tx ->
            val type = tx.concepts().putAttributeType(label, valueType)
            if (isAbstract || !isRoot) {
                val typeTx = type.asRemote(tx)
                if (isAbstract) typeTx.setAbstract()
                if (!isRoot) typeTx.setSupertype(conceptType)
            }
        }

        override fun initiateChangeSupertype() = schemaSrv.changeAttributeSupertypeDialog.open(this) {
            schemaSrv.execOnTypesUpdated()
            loadConstraintsAsync()
        }

        override fun tryChangeSupertype(
            supertypeState: Attribute
        ) = super.tryChangeSupertype(schemaSrv.changeAttributeSupertypeDialog) {
            conceptType.asRemote(it).setSupertype(supertypeState.conceptType)
        }

        override fun toString(): String = "TypeState.Attribute: $conceptType"
    }

    class Relation internal constructor(
        conceptType: RelationType, supertype: Relation?, schemaSrv: SchemaService
    ) : Thing<RelationType, Relation>(conceptType, supertype, Encoding.RELATION_TYPE, schemaSrv) {

        override val parent: Relation? get() = supertype
        var relatesRoleTypeProperties: List<RelatesRoleTypeProperties> by mutableStateOf(emptyList())
        val relatesRoleTypes: List<Role> get() = relatesRoleTypeProperties.map { it.roleType }
        val relatesRoleTypesExplicit: List<Role>
            get() = relatesRoleTypeProperties.filter { !it.isInherited }.map { it.roleType }

        override fun isSameEncoding(conceptType: Type) = conceptType.isRelationType
        override fun asSameEncoding(conceptType: Type) = conceptType.asRelationType()!!
        override fun typeStateOf(type: RelationType) = schemaSrv.typeStateOf(type)

        override fun updateConceptType(label: String) {
            super.updateConceptType(label)
            relatesRoleTypesExplicit.forEach { it.updateConceptType() }
        }

        override fun requestSubtypesExplicit() = schemaSrv.mayRunReadTx {
            conceptType.asRemote(it).subtypesExplicit.toList()
        }

        override fun loadInheritables() {
            super.loadInheritables()
            loadRelatesRoleTypes()
            relatesRoleTypes.forEach { it.loadPlayerTypes() }
        }

        override fun loadOtherConstraints() {
            super.loadOtherConstraints()
            loadRelatesRoleTypes()
            relatesRoleTypes.forEach { it.loadSupertypes() }
        }

        fun loadRelatesRoleTypesRecursivelyAsync() = coroutines.launchAndHandle(notifications, LOGGER) {
            loadRelatesRoleTypesRecursively()
        }

        private fun loadRelatesRoleTypesRecursively() {
            loadRelatesRoleTypes()
            subtypesExplicit.forEach { it.loadRelatesRoleTypesRecursively() }
        }

        private fun loadRelatesRoleTypes() {
            val loaded = mutableSetOf<RoleType>()
            val properties = mutableListOf<RelatesRoleTypeProperties>()

            fun load(relTypeTx: RelationType.Remote, roleTypeConcept: RoleType, isInherited: Boolean) {
                loaded.add(roleTypeConcept)
                schemaSrv.typeStateOf(roleTypeConcept)?.let { roleType ->
                    roleType.loadConstraints()
                    val overriddenType = relTypeTx.getRelatesOverridden(roleTypeConcept)
                        ?.let { schemaSrv.typeStateOf(it) }
                    val extendedType = when {
                        isInherited -> roleType
                        else -> overriddenType
                    }?.relationType
                    properties.add(RelatesRoleTypeProperties(roleType, overriddenType, extendedType, isInherited))
                }
            }

            schemaSrv.mayRunReadTx { tx ->
                val relTypeTx = conceptType.asRemote(tx)
                relTypeTx.relatesExplicit.forEach { load(relTypeTx, it, false) }
                relTypeTx.relates.filter { !loaded.contains(it) && !it.isRoot }.forEach { load(relTypeTx, it, true) }
            }
            relatesRoleTypeProperties = properties
        }

        override fun initiateCreateSubtype(onSuccess: () -> Unit) =
            schemaSrv.createRelationTypeDialog.open(this, onSuccess)

        override fun tryCreateSubtype(
            label: String, isAbstract: Boolean
        ) = tryCreateSubtype(label, schemaSrv.createRelationTypeDialog) { tx ->
            val type = tx.concepts().putRelationType(label)
            if (isAbstract || !isRoot) {
                val typeTx = type.asRemote(tx)
                if (isAbstract) typeTx.setAbstract()
                if (!isRoot) typeTx.setSupertype(conceptType)
            }
        }

        override fun initiateChangeSupertype() = schemaSrv.changeRelationSupertypeDialog.open(this) {
            schemaSrv.execOnTypesUpdated()
            loadConstraintsAsync()
        }

        override fun tryChangeSupertype(
            supertypeState: Relation
        ) = super.tryChangeSupertype(schemaSrv.changeRelationSupertypeDialog) {
            conceptType.asRemote(it).setSupertype(supertypeState.conceptType)
        }

        fun tryDefineRelatesRoleType(
            roleType: String, overriddenType: Role?, onSuccess: (() -> Unit)?
        ) = schemaSrv.mayRunWriteTxAsync { tx ->
            try {
                overriddenType?.let {
                    conceptType.asRemote(tx).setRelates(roleType, it.name)
                } ?: conceptType.asRemote(tx).setRelates(roleType)
                loadRelatesRoleTypes()
                onSuccess?.let { it() }
            } catch (e: Exception) {
                notifications.userError(LOGGER, FAILED_TO_RELATE_ROLE_TYPE, name, roleType, e.message ?: UNKNOWN)
            }
        }

        fun initiateDeleteRoleType(roleType: Role) = schemaSrv.confirmation.submit(
            title = Label.CONFIRM_TYPE_DELETION,
            message = Sentence.CONFIRM_TYPE_DELETION.format(Encoding.ROLE_TYPE.label, roleType.scopedName),
            onConfirm = { tryDeleteRoleType(roleType) }
        )

        fun tryDeleteRoleType(roleType: Role) = schemaSrv.mayRunWriteTxAsync {
            try {
                conceptType.asRemote(it).unsetRelates(roleType.conceptType)
                loadConstraintsAsync()
            } catch (e: Exception) {
                notifications.userError(LOGGER, FAILED_TO_DELETE_TYPE, encoding.label, e.message ?: UNKNOWN)
            }
        }

        override fun purge() {
            super.purge()
            relatesRoleTypesExplicit.forEach { it.purge() }
        }

        override fun toString(): String = "TypeState.Relation: $conceptType"
    }

    class Role constructor(
        val relationType: Relation, conceptType: RoleType, supertype: Role?, schemaSrv: SchemaService
    ) : TypeState<RoleType, Role>(conceptType, supertype, Encoding.ROLE_TYPE, schemaSrv) {

        val scopedName get() = relationType.name + ":" + name

        private var hasPlayerInstancesExplicit: Boolean by mutableStateOf(false)
        var playerTypeProperties: List<PlayerTypeProperties> by mutableStateOf(emptyList())
        val playerTypes get() = playerTypeProperties.map { it.playerType }
        val playerTypesExplicit get() = playerTypeProperties.filter { !it.isInherited }.map { it.playerType }
        override val canBeDeleted: Boolean get() = !hasSubtypes && !hasPlayerInstancesExplicit
        override val canBeAbstract get() = !hasPlayerInstancesExplicit

        override fun loadInheritables() {}
        override fun isSameEncoding(conceptType: Type) = conceptType.isRoleType
        override fun asSameEncoding(conceptType: Type) = conceptType.asRoleType()!!
        override fun typeStateOf(type: RoleType) = schemaSrv.typeStateOf(type)

        override fun updateConceptType(label: String) = schemaSrv.mayRunReadTx {
            val newConceptType = relationType.conceptType.asRemote(it).getRelates(label)!!
            isAbstract = newConceptType.isAbstract
            name = newConceptType.label.name()
            conceptType = newConceptType // we need to update the mutable state last
        } ?: Unit

        override fun requestSubtypesExplicit() = schemaSrv.mayRunReadTx {
            conceptType.asRemote(it).subtypesExplicit.toList()
        }

        fun loadConstraints() {
            loadHasPlayerInstances()
        }

        override fun loadDependencies() {
            loadHasPlayerInstances()
        }

        private fun loadHasPlayerInstances() = schemaSrv.mayRunReadTx { tx ->
            hasPlayerInstancesExplicit = conceptType.asRemote(tx).playerInstancesExplicit.findAny().isPresent
        }

        fun loadPlayerTypes() {
            val loaded = mutableSetOf<ThingType>()
            val properties = mutableListOf<PlayerTypeProperties>()

            fun load(playerType: ThingType, isInherited: Boolean) {
                loaded.add(playerType)
                schemaSrv.typeStateOf(playerType.asThingType())?.let {
                    properties.add(PlayerTypeProperties(it, isInherited))
                }
            }

            schemaSrv.mayRunReadTx { tx ->
                val roleTypeTx = conceptType.asRemote(tx)
                roleTypeTx.playerTypesExplicit.forEach { load(it, isInherited = false) }
                roleTypeTx.playerTypes.filter { !loaded.contains(it) }.forEach { load(it, isInherited = true) }
            }

            playerTypeProperties = properties
        }

        fun initiateChangeOverriddenType() = schemaSrv.changeOverriddenRoleTypeDialog.open(this)

        fun tryChangeOverriddenType(
            overriddenType: Role?
        ) = super.tryChangeSupertype(schemaSrv.changeOverriddenRoleTypeDialog) { tx ->
            relationType.conceptType.asRemote(tx).let { r ->
                overriddenType?.let { o -> r.setRelates(name, o.conceptType) } ?: r.setRelates(name)
            }
        }

        override fun toString(): String = "TypeState.Role: $conceptType"
    }
}