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
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.state.common.util.Message.Companion.UNKNOWN
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_DELETE_TYPE
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_LOAD_TYPE
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.page.Navigable
import com.vaticle.typedb.studio.state.page.Pageable
import com.vaticle.typeql.lang.common.TypeQLToken
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.streams.toList
import mu.KotlinLogging

sealed class TypeState private constructor(name: String, val encoding: Encoding, val schemaMgr: SchemaManager) {

    enum class Encoding(val label: String) {
        ENTITY_TYPE(Label.ENTITY.lowercase()),
        RELATION_TYPE(Label.RELATION.lowercase()),
        ATTRIBUTE_TYPE(Label.ATTRIBUTE.lowercase()),
        ROLE_TYPE(Label.ROLE.lowercase())
    }

    data class AttributeTypeProperties(
        val attributeType: Attribute,
        val overriddenType: Attribute?,
        val isKey: Boolean,
        val isInherited: Boolean
    )

    data class OwnerTypeProperties(val ownerType: Thing, val isKey: Boolean, val isInherited: Boolean)
    data class RoleTypeProperties(val roleType: Role, val overriddenType: Role?, val isInherited: Boolean)

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    internal abstract val baseType: TypeQLToken.Type
    internal abstract val subtypesExplicit: List<TypeState>
    abstract val conceptType: Type
    abstract val supertype: TypeState?
    abstract val subtypes: List<TypeState>
    abstract val supertypes: List<TypeState>
    abstract val canBeDeleted: Boolean

    val isRoot get() = conceptType.isRoot
    var name: String by mutableStateOf(name)
    var isAbstract: Boolean by mutableStateOf(false)
    var hasSubtypes: Boolean by mutableStateOf(false)
    var ownsAttributeTypeProperties: List<AttributeTypeProperties> by mutableStateOf(emptyList())
    val ownsAttributeTypes: List<Attribute> get() = ownsAttributeTypeProperties.map { it.attributeType }
    var playsRoleTypeProperties: List<RoleTypeProperties> by mutableStateOf(emptyList())
    val playsRoleTypes: List<Role> get() = playsRoleTypeProperties.map { it.roleType }
    val canBeAbstract get() = false // TODO

    abstract fun updateConceptTypeAndName(label: String)
    abstract fun updateSubtypesExplicit(newSubtypes: List<TypeState>)
    abstract fun removeSubtypeExplicit(subtype: TypeState)
    abstract fun loadSupertypes()
    abstract fun loadOtherPageProperties()
    abstract fun loadOtherContextMenuProperties()
    abstract override fun toString(): String

    fun loadPageProperties() = schemaMgr.coroutineScope.launchAndHandle(schemaMgr.notification, LOGGER) {
        try {
            loadSupertypes()
            loadAbstract()
            loadOtherPageProperties()
            loadSubtypesRecursivelyBlocking()
        } catch (e: TypeDBClientException) {
            schemaMgr.notification.userError(LOGGER, FAILED_TO_LOAD_TYPE, e.message ?: UNKNOWN)
        }
    }

    fun loadContextMenuProperties() = schemaMgr.coroutineScope.launchAndHandle(schemaMgr.notification, LOGGER) {
        loadContextMenuPropertiesBlocking()
    }

    private fun loadContextMenuPropertiesBlocking() {
        loadHasSubtypes()
        loadOtherContextMenuProperties()
    }

    private fun loadAbstract() = schemaMgr.openOrGetReadTx()?.let {
        isAbstract = conceptType.asRemote(it).isAbstract
    }

    private fun loadHasSubtypes() = schemaMgr.openOrGetReadTx()?.let {
        // TODO: Implement API to retrieve .hasSubtypes() on TypeDB Type API
        hasSubtypes = conceptType.asRemote(it).subtypesExplicit.findAny().isPresent
    }

    fun loadSubtypesRecursively() = schemaMgr.coroutineScope.launchAndHandle(schemaMgr.notification, LOGGER) {
        loadSubtypesRecursivelyBlocking()
    }

    private fun loadSubtypesRecursivelyBlocking() {
        loadSubtypesExplicit()
        subtypes.forEach { it.loadSubtypesRecursivelyBlocking() }
    }

    protected fun loadSubtypesExplicit(): Unit = synchronized(this) {
        schemaMgr.openOrGetReadTx()?.let { tx ->
            val new = conceptType.asRemote(tx).subtypesExplicit.toList().toSet()
            val old = subtypesExplicit.map { it.conceptType }.toSet()
            val retained: List<TypeState>
            if (new != old) {
                val deleted = old - new
                val added = (new - old).mapNotNull { schemaMgr.createTypeState(it) }
                retained = subtypesExplicit.filter { !deleted.contains(it.conceptType) }
                updateSubtypesExplicit((retained + added).sortedBy { it.conceptType.label.scopedName() })
            }
            subtypesExplicit.onEach { it.loadContextMenuPropertiesBlocking() }
            hasSubtypes = subtypesExplicit.isNotEmpty()
        }
    }

    fun initiateRename() = schemaMgr.renameTypeDialog.open(this)

    fun tryRename(label: String) = try {
        schemaMgr.openOrGetWriteTx()?.let {
            conceptType.asRemote(it).setLabel(label)
            schemaMgr.remove(this)
            updateConceptTypeAndName(label)
            schemaMgr.register(this)
            schemaMgr.onTypesUpdated.forEach { it() }
            schemaMgr.renameTypeDialog.close()
        } ?: Unit
    } catch (e: Exception) {
        schemaMgr.notification.userError(
            LOGGER, Message.Schema.FAILED_TO_RENAME_TYPE,
            encoding.label, conceptType.label, label, e.message ?: UNKNOWN
        )
    }

    open fun purge() {
        supertype?.removeSubtypeExplicit(this)
        schemaMgr.remove(this)
        subtypesExplicit.forEach { it.purge() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TypeState
        return this.conceptType == other.conceptType
    }

    override fun hashCode(): Int {
        return conceptType.hashCode()
    }

    sealed class Thing constructor(
        encoding: Encoding,
        name: String,
        schemaMgr: SchemaManager
    ) : TypeState(name, encoding, schemaMgr), Navigable<Thing>, Pageable {

        private class Callbacks {

            val onReopen = LinkedBlockingQueue<(Thing) -> Unit>()
            val onClose = LinkedBlockingQueue<(Thing) -> Unit>()

            fun clear() {
                onReopen.clear()
                onClose.clear()
            }
        }

        var hasInstancesExplicit: Boolean by mutableStateOf(false)
        abstract override val conceptType: ThingType
        override val supertype: Thing? = null
        override val supertypes: List<Thing> = emptyList()
        override val subtypesExplicit: List<Thing> by mutableStateOf(listOf())
        override val subtypes: List<Thing> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()
        override val canBeDeleted get() = !hasSubtypes && !hasInstancesExplicit

        override val info: String? = null
        override val isBulkExpandable: Boolean = true
        override val isExpandable: Boolean get() = hasSubtypes
        override val entries: List<Thing> get() = subtypesExplicit

        override val windowTitle: String get() = computeWindowTitle()
        override val isOpen: Boolean get() = isOpenAtomic.get()
        override val isWritable: Boolean = true
        override val isEmpty: Boolean = false
        override val isUnsavedPageable: Boolean = false
        override val hasUnsavedChanges: Boolean by mutableStateOf(false)

        private val isOpenAtomic = AtomicBoolean(false)
        private val callbacks = Callbacks()

        private fun computeWindowTitle(): String {
            val props = listOf(baseType.name.lowercase()) + info?.let { listOf(it) }
            return "$name (" + props.joinToString(", ") + ") @ " + schemaMgr.database
        }

        override fun deactivate() {}
        override fun execBeforeClose() {}
        override fun initiateSave(reopen: Boolean) {}
        override fun reloadEntries() = loadSubtypesExplicit()
        override fun onReopen(function: (Pageable) -> Unit) = callbacks.onReopen.put(function)
        override fun onClose(function: (Pageable) -> Unit) = callbacks.onClose.put(function)
        override fun compareTo(other: Navigable<Thing>): Int = name.compareTo(other.name)

        override fun tryOpen(): Boolean {
            isOpenAtomic.set(true)
            callbacks.onReopen.forEach { it(this) }
            schemaMgr.pages.opened(this)
            loadPageProperties()
            return true
        }

        override fun activate() {
            schemaMgr.pages.active(this)
            loadPageProperties()
        }

        fun exportSyntax(onSuccess: (syntax: String) -> Unit) =
            schemaMgr.coroutineScope.launchAndHandle(schemaMgr.notification, LOGGER) {
                schemaMgr.openOrGetReadTx()?.let { tx ->
                    conceptType.asRemote(tx).syntax?.let { onSuccess(it) }
                }
            }

        override fun loadOtherPageProperties() {
            loadHasInstancesExplicit()
            loadOwnsAttributeTypes()
            loadPlaysRoleTypes()
        }

        override fun loadOtherContextMenuProperties() {
            loadHasInstancesExplicit()
        }

        private fun loadHasInstancesExplicit() = schemaMgr.openOrGetReadTx()?.let {
            hasInstancesExplicit = conceptType.asRemote(it).instancesExplicit.findAny().isPresent
        }

        private fun loadOwnsAttributeTypes() {
            val loaded = mutableSetOf<AttributeType>()
            val properties = mutableListOf<AttributeTypeProperties>()

            fun load(typeTx: ThingType.Remote, attributeType: AttributeType, isKey: Boolean, isInherited: Boolean) {
                loaded.add(attributeType)
                schemaMgr.createTypeState(attributeType)?.let { ats ->
                    val ots = typeTx.getOwnsOverridden(attributeType)?.let { schemaMgr.createTypeState(it) }
                    properties.add(AttributeTypeProperties(ats, ots, isKey, isInherited))
                }
            }

            schemaMgr.openOrGetReadTx()?.let { tx ->
                val typeTx = conceptType.asRemote(tx)
                typeTx.getOwnsExplicit(true).forEach {
                    load(typeTx = typeTx, attributeType = it, isKey = true, isInherited = false)
                }
                typeTx.getOwnsExplicit(false).filter { !loaded.contains(it) }.forEach {
                    load(typeTx = typeTx, attributeType = it, isKey = false, isInherited = false)
                }
                typeTx.getOwns(true).filter { !loaded.contains(it) }.forEach {
                    load(typeTx = typeTx, attributeType = it, isKey = true, isInherited = true)
                }
                typeTx.getOwns(false).filter { !loaded.contains(it) }.forEach {
                    load(typeTx = typeTx, attributeType = it, isKey = false, isInherited = true)
                }
            }
            ownsAttributeTypeProperties = properties
        }

        private fun loadPlaysRoleTypes() {
            val loaded = mutableSetOf<RoleType>()
            val properties = mutableListOf<RoleTypeProperties>()

            fun load(typeTx: ThingType.Remote, roleType: RoleType, isInherited: Boolean) {
                loaded.add(roleType)
                schemaMgr.createTypeState(roleType)?.let { rts ->
                    val ots = typeTx.getPlaysOverridden(roleType)?.let { schemaMgr.createTypeState(it) }
                    properties.add(RoleTypeProperties(rts, ots, isInherited))
                }
            }

            schemaMgr.openOrGetReadTx()?.let { tx ->
                val typeTx = conceptType.asRemote(tx)
                typeTx.playsExplicit.forEach { load(typeTx, it, false) }
                typeTx.plays.filter { !loaded.contains(it) }.forEach { load(typeTx, it, true) }
            }
            playsRoleTypeProperties = properties
        }

        abstract fun initiateCreateSubtype(onSuccess: () -> Unit)

        abstract fun tryCreateSubtype(label: String, isAbstract: Boolean)

        protected fun tryCreateSubtype(
            label: String, dialogState: SchemaManager.EditTypeDialog<*>, creatorFn: (TypeDBTransaction) -> Unit
        ) {
            if (schemaMgr.openOrGetReadTx()?.concepts()?.getThingType(label) != null) schemaMgr.notification.userError(
                LOGGER, Message.Schema.FAILED_TO_CREATE_TYPE_DUE_TO_DUPLICATE, encoding.label, label
            ) else schemaMgr.openOrGetWriteTx()?.let { tx ->
                try {
                    creatorFn(tx)
                    dialogState.onSuccess?.invoke()
                    dialogState.close()
                    schemaMgr.onTypesUpdated.forEach { it() }
                } catch (e: Exception) {
                    schemaMgr.notification.userError(
                        LOGGER, Message.Schema.FAILED_TO_CREATE_TYPE, encoding.label, label, e.message ?: UNKNOWN
                    )
                }
            }
        }

        fun initiateEditSupertype() = schemaMgr.editSuperTypeDialog.open(this)

        fun initiateEditAbstract() = schemaMgr.editAbstractDialog.open(this)

        fun tryDefinePlaysRoleType(roleType: Role, overriddenType: Role?) {
            // TODO
        }

        fun tryUndefinePlaysRoleType(roleType: Role) {
            // TODO
        }

        fun tryDefineOwnsAttributeType(attributeType: Attribute, overriddenType: Attribute?, key: Boolean) {
            // TODO
        }

        fun tryUndefineOwnsAttributeType(attributeType: Attribute) {
            // TODO
        }

        fun initiateDelete() = schemaMgr.confirmation.submit(
            title = Label.CONFIRM_TYPE_DELETION,
            message = Sentence.CONFIRM_TYPE_DELETION.format(name),
            onConfirm = { tryDelete() }
        )

        override fun tryDelete() = try {
            schemaMgr.openOrGetWriteTx()?.let {
                conceptType.asRemote(it).delete()
                purge()
                schemaMgr.onTypesUpdated.forEach { it() }
            } ?: Unit
        } catch (e: Exception) {
            schemaMgr.notification.userError(LOGGER, FAILED_TO_DELETE_TYPE, e.message ?: UNKNOWN)
        }

        override fun close() {
            if (isOpenAtomic.compareAndSet(true, false)) {
                schemaMgr.pages.close(this)
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
        conceptType: EntityType,
        supertype: Entity?,
        schemaMgr: SchemaManager
    ) : Thing(Encoding.ENTITY_TYPE, conceptType.label.name(), schemaMgr) {

        override val parent: Entity? get() = supertype
        override val baseType = TypeQLToken.Type.ENTITY
        override var conceptType: EntityType by mutableStateOf(conceptType)
        override var supertype: Entity? by mutableStateOf(supertype)
        override var supertypes: List<Entity> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())
        override var subtypesExplicit: List<Entity> by mutableStateOf(listOf())
        override val subtypes: List<Entity> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()

        override fun updateConceptTypeAndName(label: String) = schemaMgr.openOrGetReadTx()?.let {
            conceptType = it.concepts().getEntityType(label)!!
            name = conceptType.label.name()
        } ?: Unit

        override fun updateSubtypesExplicit(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Entity }
        }

        override fun removeSubtypeExplicit(subtype: TypeState) {
            subtypesExplicit = subtypesExplicit.filter { it != subtype as Entity }
        }

        override fun loadSupertypes() = schemaMgr.openOrGetReadTx()?.let { tx ->
            val typeTx = conceptType.asRemote(tx)
            supertype = typeTx.supertype?.let {
                if (it.isEntityType) schemaMgr.createTypeState(it.asEntityType()) else null
            }
            supertypes = typeTx.supertypes
                .filter { it.isEntityType && it != typeTx }
                .map { schemaMgr.createTypeState(it.asEntityType()) }.filter { it != null }.toList().filterNotNull()
        } ?: Unit

        override fun initiateCreateSubtype(onSuccess: () -> Unit) = schemaMgr.createEntTypeDialog.open(this, onSuccess)

        override fun tryCreateSubtype(label: String, isAbstract: Boolean) {
            tryCreateSubtype(label, schemaMgr.createEntTypeDialog) { tx ->
                val type = tx.concepts().putEntityType(label)
                if (isAbstract || !isRoot) {
                    val typeTx = type.asRemote(tx)
                    if (isAbstract) typeTx.setAbstract()
                    if (!isRoot) typeTx.setSupertype(conceptType)
                }
            }
        }

        override fun toString(): String = "TypeState.Entity: $conceptType"
    }

    class Attribute internal constructor(
        conceptType: AttributeType,
        supertype: Attribute?,
        schemaMgr: SchemaManager
    ) : Thing(Encoding.ATTRIBUTE_TYPE, conceptType.label.name(), schemaMgr) {

        override val info get() = valueType?.name?.lowercase()
        override val parent: Attribute? get() = supertype
        override val baseType = TypeQLToken.Type.ATTRIBUTE
        override var conceptType: AttributeType by mutableStateOf(conceptType)
        override var supertype: Attribute? by mutableStateOf(supertype)
        override var supertypes: List<Attribute> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())
        override var subtypesExplicit: List<Attribute> by mutableStateOf(listOf())
        override val subtypes: List<Attribute> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()

        val valueType: AttributeType.ValueType? = if (!conceptType.isRoot) conceptType.valueType else null
        val isKeyable: Boolean get() = conceptType.valueType.isKeyable
        var ownerTypeProperties: Map<ThingType, OwnerTypeProperties> by mutableStateOf(mapOf())
        val ownerTypes get() = ownerTypeProperties.values.map { it.ownerType }

        override fun updateConceptTypeAndName(label: String) = schemaMgr.openOrGetReadTx()?.let {
            conceptType = it.concepts().getAttributeType(label)!!
            name = conceptType.label.name()
        } ?: Unit

        override fun updateSubtypesExplicit(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Attribute }
        }

        override fun removeSubtypeExplicit(subtype: TypeState) {
            subtypesExplicit = subtypesExplicit.filter { it != subtype as Attribute }
        }

        override fun loadSupertypes() = schemaMgr.openOrGetReadTx()?.let { tx ->
            val typeTx = conceptType.asRemote(tx)
            supertype = typeTx.supertype?.let {
                if (it.isAttributeType) schemaMgr.createTypeState(it.asAttributeType()) else null
            }
            supertypes = typeTx.supertypes
                .filter { it.isAttributeType && it != typeTx }
                .map { schemaMgr.createTypeState(it.asAttributeType()) }.toList().filterNotNull()
        } ?: Unit

        override fun loadOtherPageProperties() {
            super.loadOtherPageProperties()
            loadOwnerTypes()
        }

        private fun loadOwnerTypes() {
            val props = mutableMapOf<ThingType, OwnerTypeProperties>()

            fun load(ownerType: ThingType, isKey: Boolean, isInherited: Boolean) {
                schemaMgr.createTypeState(ownerType)?.let {
                    props[ownerType] = OwnerTypeProperties(it, isKey, isInherited)
                }
            }

            schemaMgr.openOrGetReadTx()?.let { tx ->
                val typeTx = conceptType.asRemote(tx)
                typeTx.getOwnersExplicit(true).forEach {
                    load(it, isKey = true, isInherited = false)
                }
                typeTx.getOwnersExplicit(false).filter { !props.contains(it) }.forEach {
                    load(it, isKey = false, isInherited = false)
                }
                typeTx.getOwners(true).filter { !props.contains(it) }.forEach {
                    load(it, isKey = true, isInherited = true)
                }
                typeTx.getOwners(false).filter { !props.contains(it) }.forEach {
                    load(it, isKey = false, isInherited = true)
                }
            }

            ownerTypeProperties = props
        }

        override fun initiateCreateSubtype(onSuccess: () -> Unit) = schemaMgr.createAttTypeDialog.open(this, onSuccess)

        override fun tryCreateSubtype(label: String, isAbstract: Boolean) {
            tryCreateSubtype(label, isAbstract, conceptType.valueType)
        }

        fun tryCreateSubtype(label: String, isAbstract: Boolean, valueType: AttributeType.ValueType) {
            tryCreateSubtype(label, schemaMgr.createAttTypeDialog) { tx ->
                val type = tx.concepts().putAttributeType(label, valueType)
                if (isAbstract || !isRoot) {
                    val typeTx = type.asRemote(tx)
                    if (isAbstract) typeTx.setAbstract()
                    if (!isRoot) typeTx.setSupertype(conceptType)
                }
            }
        }

        override fun toString(): String = "TypeState.Attribute: $conceptType"
    }

    class Relation internal constructor(
        conceptType: RelationType,
        supertype: Relation?,
        schemaMgr: SchemaManager
    ) : Thing(Encoding.RELATION_TYPE, conceptType.label.name(), schemaMgr) {

        override val parent: Relation? get() = supertype
        override val baseType = TypeQLToken.Type.RELATION
        override var conceptType: RelationType by mutableStateOf(conceptType)
        override var supertype: Relation? by mutableStateOf(supertype)
        override var supertypes: List<Relation> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())
        override var subtypesExplicit: List<Relation> by mutableStateOf(listOf())
        override val subtypes: List<Relation> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()
        var relatesRoleTypeProperties: List<RoleTypeProperties> by mutableStateOf(emptyList())
        val relatesRoleTypes: List<Role> get() = relatesRoleTypeProperties.map { it.roleType }

        override fun updateConceptTypeAndName(label: String) = schemaMgr.openOrGetReadTx()?.let {
            conceptType = it.concepts().getRelationType(label)!!
            name = conceptType.label.name()
        } ?: Unit

        override fun updateSubtypesExplicit(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Relation }
        }

        override fun removeSubtypeExplicit(subtype: TypeState) {
            subtypesExplicit = subtypesExplicit.filter { it != subtype as Relation }
        }

        override fun loadSupertypes() = schemaMgr.openOrGetReadTx()?.let { tx ->
            val typeTx = conceptType.asRemote(tx)
            supertype = typeTx.supertype?.let {
                if (it.isRelationType) schemaMgr.createTypeState(it.asRelationType()) else null
            }
            supertypes = typeTx.supertypes
                .filter { it.isRelationType && it != typeTx }
                .map { schemaMgr.createTypeState(it.asRelationType()) }.toList().filterNotNull()
        } ?: Unit

        override fun loadOtherPageProperties() {
            super.loadOtherPageProperties()
            loadRelatesRoleType()
        }

        fun loadRelatesRoleTypeRecursively() =
            schemaMgr.coroutineScope.launchAndHandle(schemaMgr.notification, LOGGER) {
                loadRelatesRoleTypeRecursivelyBlocking()
            }

        private fun loadRelatesRoleTypeRecursivelyBlocking() {
            loadRelatesRoleType()
            subtypesExplicit.forEach { it.loadRelatesRoleTypeRecursivelyBlocking() }
        }

        private fun loadRelatesRoleType() {
            val loaded = mutableSetOf<RoleType>()
            val properties = mutableListOf<RoleTypeProperties>()

            fun load(typeTx: RelationType.Remote, roleType: RoleType, isInherited: Boolean) {
                loaded.add(roleType)
                schemaMgr.createTypeState(roleType)?.let { rts ->
                    val ots = typeTx.getRelatesOverridden(roleType)?.let { schemaMgr.createTypeState(it) }
                    properties.add(RoleTypeProperties(rts, ots, isInherited))
                }
            }

            schemaMgr.openOrGetReadTx()?.let { tx ->
                val typeTx = conceptType.asRemote(tx)
                typeTx.relatesExplicit.forEach { load(typeTx, it, false) }
                typeTx.relates.filter { !loaded.contains(it) }.forEach { load(typeTx, it, true) }
            }
            relatesRoleTypeProperties = properties
        }

        override fun initiateCreateSubtype(onSuccess: () -> Unit) = schemaMgr.createRelTypeDialog.open(this, onSuccess)

        override fun tryCreateSubtype(label: String, isAbstract: Boolean) {
            tryCreateSubtype(label, schemaMgr.createRelTypeDialog) { tx ->
                val type = tx.concepts().putRelationType(label)
                if (isAbstract || !isRoot) {
                    val typeTx = type.asRemote(tx)
                    if (isAbstract) typeTx.setAbstract()
                    if (!isRoot) typeTx.setSupertype(conceptType)
                }
            }
        }

        fun tryDefineRelatesRoleType(roleType: String, overriddenType: Role?) {
            // TODO
        }

        fun tryUndefineRelatesRoleType(roleType: Role) {
            // TODO
        }

        override fun toString(): String = "TypeState.Relation: $conceptType"
    }

    class Role constructor(
        conceptType: RoleType,
        val relationType: Relation,
        supertype: Role?,
        schemaMgr: SchemaManager
    ) : TypeState(conceptType.label.name(), Encoding.ROLE_TYPE, schemaMgr) {

        val scopedName get() = relationType.name + ":" + name

        var hasPlayers: Boolean by mutableStateOf(false)
        override val baseType: TypeQLToken.Type = TypeQLToken.Type.ROLE
        override var conceptType: RoleType by mutableStateOf(conceptType)
        override var subtypesExplicit: List<Role> by mutableStateOf(emptyList())
        override val subtypes: List<Role> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()
        override var supertype: Role? by mutableStateOf(supertype)
        override var supertypes: List<Role> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())
        override val canBeDeleted: Boolean get() = !hasSubtypes && !hasPlayers

        override fun updateConceptTypeAndName(label: String) = schemaMgr.openOrGetReadTx()?.let {
            conceptType = relationType.conceptType.asRemote(it).getRelates(label)!!
            name = conceptType.label.name()
        } ?: Unit

        override fun updateSubtypesExplicit(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Role }
        }

        override fun removeSubtypeExplicit(subtype: TypeState) {
            subtypesExplicit = subtypesExplicit.filter { it != subtype as Role }
        }

        override fun loadSupertypes() = schemaMgr.openOrGetReadTx()?.let { tx ->
            val typeTx = conceptType.asRemote(tx)
            supertype = typeTx.supertype?.let {
                if (it.isRoleType) schemaMgr.createTypeState(it.asRoleType()) else null
            }
            supertypes = typeTx.supertypes
                .filter { it.isRoleType && it != typeTx }
                .map { schemaMgr.createTypeState(it.asRoleType()) }.toList().filterNotNull()
        } ?: Unit

        override fun loadOtherPageProperties() {
            loadHasPlayers()
        }

        override fun loadOtherContextMenuProperties() {
            loadHasPlayers()
        }

        private fun loadHasPlayers() = schemaMgr.openOrGetReadTx()?.let {
            hasPlayers = conceptType.asRemote(it).players.findAny().isPresent
        }

        override fun toString(): String = "TypeState.Role: $conceptType"
    }
}