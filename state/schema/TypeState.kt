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
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.RoleType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_DELETE_TYPE
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_LOAD_TYPE
import com.vaticle.typedb.studio.state.resource.Navigable
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typeql.lang.common.TypeQLToken
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.streams.toList
import kotlinx.coroutines.launch
import mu.KotlinLogging

sealed class TypeState private constructor(hasSubtypes: Boolean, val schemaMgr: SchemaManager) {

    data class AttributeTypeProperties(
        val attributeType: Attribute,
        val overriddenType: Attribute?,
        val isKey: Boolean,
        val isInherited: Boolean
    )

    data class OwnerTypeProperties(
        val ownerType: Thing,
        val isKey: Boolean,
        val isInherited: Boolean
    )

    data class RoleTypeProperties(
        val roleType: Role,
        val overriddenType: Role?,
        val isInherited: Boolean
    )

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    internal abstract val conceptType: Type
    internal abstract val baseType: TypeQLToken.Type
    internal abstract val subtypesExplicit: List<TypeState>
    abstract val subtypes: List<TypeState>
    abstract val supertype: TypeState?
    abstract val supertypes: List<TypeState>

    val isRoot get() = conceptType.isRoot
    var isAbstract: Boolean by mutableStateOf(false)
    var hasSubtypes: Boolean by mutableStateOf(hasSubtypes)
    var ownsAttributeTypeProperties: List<AttributeTypeProperties> by mutableStateOf(emptyList())
    val ownsAttributeTypes: List<Attribute> get() = ownsAttributeTypeProperties.map { it.attributeType }
    var playsRoleTypeProperties: List<RoleTypeProperties> by mutableStateOf(emptyList())
    val playsRoleTypes: List<Role> get() = playsRoleTypeProperties.map { it.roleType }

    abstract fun updateSubtypes(newSubtypes: List<TypeState>)
    abstract fun loadSupertypes()
    abstract fun loadOtherProperties()
    abstract override fun toString(): String

    fun loadProperties() = schemaMgr.coroutineScope.launch {
        try {
            loadSupertypes()
            loadAbstract()
            loadOtherProperties()
            loadSubtypesRecursivelyBlocking()
        } catch (e: TypeDBClientException) {
            schemaMgr.notificationMgr.userError(LOGGER, FAILED_TO_LOAD_TYPE, e.message ?: "Unknown")
        }
    }

    private fun loadAbstract() {
        isAbstract = conceptType.asRemote(schemaMgr.openOrGetReadTx()).isAbstract
    }

    fun loadSubtypesRecursively() = schemaMgr.coroutineScope.launch {
        loadSubtypesRecursivelyBlocking()
    }

    private fun loadSubtypesRecursivelyBlocking() {
        loadSubtypesExplicit()
        subtypes.forEach { it.loadSubtypesRecursivelyBlocking() }
    }

    protected fun loadSubtypesExplicit() {
        val tx = schemaMgr.openOrGetReadTx()
        val new = conceptType.asRemote(tx).subtypesExplicit.toList().toSet()
        val old = subtypes.map { it.conceptType }.toSet()
        val retained: List<TypeState>
        if (new != old) {
            val deleted = old - new
            val added = (new - old).map { schemaMgr.createTypeState(it) }
            retained = subtypes.filter { !deleted.contains(it.conceptType) }
            updateSubtypes((retained + added).sortedBy { it.conceptType.label.scopedName() })
        } else retained = subtypes
        retained.onEach { it.hasSubtypes = it.conceptType.asRemote(tx).subtypesExplicit.findAny().isPresent }
        hasSubtypes = subtypesExplicit.isNotEmpty()
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
        override val conceptType: ThingType,
        name: String,
        hasSubtypes: Boolean,
        schemaMgr: SchemaManager
    ) : TypeState(hasSubtypes, schemaMgr), Navigable<Thing>, Resource {

        private class Callbacks {

            val onReopen = LinkedBlockingQueue<(Thing) -> Unit>()
            val onClose = LinkedBlockingQueue<(Thing) -> Unit>()

            fun clear() {
                onReopen.clear()
                onClose.clear()
            }
        }

        override val supertype: Thing? = null
        override val supertypes: List<Thing> = emptyList()
        override val subtypesExplicit: List<Thing> by mutableStateOf(listOf())
        override val subtypes: List<Thing> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()

        override val name: String by mutableStateOf(name)
        override val info: String? = null
        override val isBulkExpandable: Boolean = true
        override val isExpandable: Boolean get() = hasSubtypes
        override val entries: List<Thing> get() = subtypesExplicit

        override val windowTitle: String get() = computeWindowTitle()
        override val isOpen: Boolean get() = isOpenAtomic.get()
        override val isWritable: Boolean = true
        override val isEmpty: Boolean = false
        override val isUnsavedResource: Boolean = false
        override val hasUnsavedChanges: Boolean by mutableStateOf(false)

        private val isOpenAtomic = AtomicBoolean(false)
        private val callbacks = Callbacks()

        private fun computeWindowTitle(): String {
            val props = listOf(baseType.name.lowercase()) + info?.let { listOf(it) }
            return "$name (" + props.joinToString(", ") + ") @ " + schemaMgr.database
        }

        override fun deactivate() {}
        override fun beforeRun(function: (Resource) -> Unit) {}
        override fun beforeSave(function: (Resource) -> Unit) {}
        override fun beforeClose(function: (Resource) -> Unit) {}
        override fun execBeforeClose() {}
        override fun save(onSuccess: ((Resource) -> Unit)?) {}
        override fun move(onSuccess: ((Resource) -> Unit)?) {}
        override fun reloadEntries() = loadSubtypesExplicit()
        override fun rename(onSuccess: ((Resource) -> Unit)?) = Unit // TODO
        override fun onReopen(function: (Resource) -> Unit) = callbacks.onReopen.put(function)
        override fun onClose(function: (Resource) -> Unit) = callbacks.onClose.put(function)
        override fun compareTo(other: Navigable<Thing>): Int = name.compareTo(other.name)

        override fun tryOpen(): Boolean {
            isOpenAtomic.set(true)
            callbacks.onReopen.forEach { it(this) }
            return true
        }

        override fun activate() {
            loadProperties()
        }

        override fun close() {
            if (isOpenAtomic.compareAndSet(true, false)) {
                callbacks.onClose.forEach { it(this) }
                callbacks.clear()
            }
        }

        override fun closeRecursive() {
            close()
            entries.forEach { it.closeRecursive() }
        }

        override fun delete() {
            try {
                close()
                // TODO
            } catch (e: Exception) {
                schemaMgr.notificationMgr.userError(LOGGER, FAILED_TO_DELETE_TYPE, e.message ?: "Unknown")
            }
        }

        override fun loadOtherProperties() {
            loadOwnsAttributeTypes()
            loadPlaysRoleTypes()
        }

        private fun loadOwnsAttributeTypes() {
            val loaded = mutableSetOf<AttributeType>()
            val properties = mutableListOf<AttributeTypeProperties>()
            val typeTx = conceptType.asRemote(schemaMgr.openOrGetReadTx())

            fun load(attributeType: AttributeType, isKey: Boolean, isInherited: Boolean) {
                loaded.add(attributeType)
                properties.add(
                    AttributeTypeProperties(
                        attributeType = schemaMgr.createTypeState(attributeType),
                        overriddenType = typeTx.getOwnsOverridden(attributeType)?.let { schemaMgr.createTypeState(it) },
                        isKey = isKey,
                        isInherited = isInherited
                    )
                )
            }

            typeTx.getOwnsExplicit(true).forEach {
                load(attributeType = it, isKey = true, isInherited = false)
            }
            typeTx.getOwnsExplicit(false).filter { !loaded.contains(it) }.forEach {
                load(attributeType = it, isKey = false, isInherited = false)
            }
            typeTx.getOwns(true).filter { !loaded.contains(it) }.forEach {
                load(attributeType = it, isKey = true, isInherited = true)
            }
            typeTx.getOwns(false).filter { !loaded.contains(it) }.forEach {
                load(attributeType = it, isKey = false, isInherited = true)
            }
            ownsAttributeTypeProperties = properties
        }

        fun defineOwnsAttributeType(attributeType: Attribute, overriddenType: Attribute?, key: Boolean) {
            // TODO
        }

        fun undefineOwnsAttributeType(attributeType: Attribute) {
            // TODO
        }

        private fun loadPlaysRoleTypes() {
            val loaded = mutableSetOf<RoleType>()
            val properties = mutableListOf<RoleTypeProperties>()
            val typeTx = conceptType.asRemote(schemaMgr.openOrGetReadTx())

            fun load(roleType: RoleType, isInherited: Boolean) {
                loaded.add(roleType)
                properties.add(
                    RoleTypeProperties(
                        roleType = schemaMgr.createTypeState(roleType),
                        overriddenType = typeTx.getPlaysOverridden(roleType)?.let { schemaMgr.createTypeState(it) },
                        isInherited = isInherited
                    )
                )
            }

            typeTx.playsExplicit.forEach { load(roleType = it, isInherited = false) }
            typeTx.plays.filter { !loaded.contains(it) }.forEach { load(roleType = it, isInherited = true) }
            playsRoleTypeProperties = properties
        }

        fun definePlaysRoleType(roleType: Role, overriddenType: Role?) {
            // TODO
        }

        fun undefinePlaysRoleType(roleType: Role) {
            // TODO
        }

        fun exportSyntax(onSuccess: (syntax: String) -> Unit) = schemaMgr.coroutineScope.launch {
            conceptType.asRemote(schemaMgr.openOrGetReadTx()).syntax?.let { onSuccess(it) }
        }
    }

    class Entity internal constructor(
        override val conceptType: EntityType,
        supertype: Entity?,
        hasSubtypes: Boolean,
        schemaMgr: SchemaManager
    ) : Thing(conceptType, conceptType.label.name(), hasSubtypes, schemaMgr) {

        override val parent: Entity? get() = supertype
        override val baseType = TypeQLToken.Type.ENTITY
        override var supertype: Entity? by mutableStateOf(supertype)
        override var supertypes: List<Entity> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())
        override var subtypesExplicit: List<Entity> by mutableStateOf(listOf())
        override val subtypes: List<Entity> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()

        override fun updateSubtypes(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Entity }
        }

        override fun loadSupertypes() {
            val remoteType = conceptType.asRemote(schemaMgr.openOrGetReadTx())
            supertype = remoteType.supertype
                ?.let { if (it.isEntityType) schemaMgr.createTypeState(it.asEntityType()) else null }
                ?.also { it.loadProperties() }
            supertypes = remoteType.supertypes
                .filter { it.isEntityType && it != remoteType }
                .map { schemaMgr.createTypeState(it.asEntityType()) }.toList()
        }

        override fun toString(): String {
            return "TypeState.Entity: $conceptType"
        }
    }

    class Attribute internal constructor(
        override val conceptType: AttributeType,
        supertype: Attribute?,
        hasSubtypes: Boolean,
        schemaMgr: SchemaManager
    ) : Thing(conceptType, conceptType.label.name(), hasSubtypes, schemaMgr) {

        override val info get() = valueType
        override val parent: Attribute? get() = supertype
        override val baseType = TypeQLToken.Type.ATTRIBUTE
        override var supertype: Attribute? by mutableStateOf(supertype)
        override var supertypes: List<Attribute> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())
        override var subtypesExplicit: List<Attribute> by mutableStateOf(listOf())
        override val subtypes: List<Attribute> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()

        val valueType: String? = if (!conceptType.isRoot) conceptType.valueType.name.lowercase() else null
        val isKeyable: Boolean get() = conceptType.valueType.isKeyable
        var ownerTypeProperties: Map<ThingType, OwnerTypeProperties> by mutableStateOf(mapOf())
        val ownerTypes get() = ownerTypeProperties.values.map { it.ownerType }

        override fun updateSubtypes(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Attribute }
        }

        override fun loadSupertypes() {
            val remoteType = conceptType.asRemote(schemaMgr.openOrGetReadTx())
            supertype = remoteType.supertype
                ?.let { if (it.isAttributeType) schemaMgr.createTypeState(it.asAttributeType()) else null }
                ?.also { it.loadProperties() }
            supertypes = remoteType.supertypes
                .filter { it.isAttributeType && it != remoteType }
                .map { schemaMgr.createTypeState(it.asAttributeType()) }.toList()
        }

        override fun loadOtherProperties() {
            super.loadOtherProperties()
            loadOwnerTypes()
        }

        private fun loadOwnerTypes() {
            val props = mutableMapOf<ThingType, OwnerTypeProperties>()
            val typeTx = conceptType.asRemote(schemaMgr.openOrGetReadTx())

            fun load(ownerType: ThingType, isKey: Boolean, isInherited: Boolean) {
                props[ownerType] = OwnerTypeProperties(schemaMgr.createTypeState(ownerType), isKey, isInherited)
            }

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
            ownerTypeProperties = props
        }

        override fun toString(): String {
            return "TypeState.Attribute: $conceptType"
        }
    }

    class Relation internal constructor(
        override val conceptType: RelationType,
        supertype: Relation?,
        hasSubtypes: Boolean,
        schemaMgr: SchemaManager
    ) : Thing(conceptType, conceptType.label.name(), hasSubtypes, schemaMgr) {

        override val parent: Relation? get() = supertype
        override val baseType = TypeQLToken.Type.RELATION
        override var supertype: Relation? by mutableStateOf(supertype)
        override var supertypes: List<Relation> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())
        override var subtypesExplicit: List<Relation> by mutableStateOf(listOf())
        override val subtypes: List<Relation> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()
        var relatesRoleTypeProperties: List<RoleTypeProperties> by mutableStateOf(emptyList())
        val relatesRoleTypes: List<Role> get() = relatesRoleTypeProperties.map { it.roleType }

        override fun updateSubtypes(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Relation }
        }

        override fun loadSupertypes() {
            val remoteType = conceptType.asRemote(schemaMgr.openOrGetReadTx())
            supertype = remoteType.supertype
                ?.let { if (it.isRelationType) schemaMgr.createTypeState(it.asRelationType()) else null }
                ?.also { it.loadProperties() }
            supertypes = remoteType.supertypes
                .filter { it.isRelationType && it != remoteType }
                .map { schemaMgr.createTypeState(it.asRelationType()) }.toList()
        }

        override fun loadOtherProperties() {
            super.loadOtherProperties()
            loadRelatesRoleType()
        }

        fun loadRelatesRoleTypeRecursively() = schemaMgr.coroutineScope.launch {
            try {
                loadRelatesRoleTypeRecursivelyBlocking()
            } catch (e: Exception) {
                LOGGER.error { e }
                e.printStackTrace()
            }
        }

        private fun loadRelatesRoleTypeRecursivelyBlocking() {
            loadRelatesRoleType()
            subtypesExplicit.forEach { it.loadRelatesRoleTypeRecursivelyBlocking() }
        }

        private fun loadRelatesRoleType() {
            val loaded = mutableSetOf<RoleType>()
            val properties = mutableListOf<RoleTypeProperties>()
            val typeTx = conceptType.asRemote(schemaMgr.openOrGetReadTx())

            fun load(roleType: RoleType, isInherited: Boolean) {
                loaded.add(roleType)
                properties.add(
                    RoleTypeProperties(
                        roleType = schemaMgr.createTypeState(roleType),
                        overriddenType = typeTx.getRelatesOverridden(roleType.label.name())?.let {
                            schemaMgr.createTypeState(it)
                        },
                        isInherited = isInherited
                    )
                )
            }

            typeTx.relatesExplicit.forEach { load(roleType = it, isInherited = false) }
            typeTx.relates.filter { !loaded.contains(it) }.forEach { load(roleType = it, isInherited = true) }
            relatesRoleTypeProperties = properties
        }

        fun undefineRelatesRoleType(roleType: Role) {
            // TODO
        }

        override fun toString(): String {
            return "TypeState.Relation: $conceptType"
        }
    }

    class Role constructor(
        override val conceptType: RoleType,
        val relationType: Relation,
        supertype: Role?,
        hasSubtypes: Boolean,
        schemaMgr: SchemaManager
    ) : TypeState(hasSubtypes, schemaMgr) {

        private val name: String by mutableStateOf(conceptType.label.name())
        val scopedName get() = relationType.name + ":" + name

        override val baseType: TypeQLToken.Type = TypeQLToken.Type.ROLE
        override var subtypesExplicit: List<Role> by mutableStateOf(emptyList())
        override val subtypes: List<Role> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()
        override var supertype: Role? by mutableStateOf(supertype)
        override var supertypes: List<Role> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())

        override fun updateSubtypes(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Role }
        }

        override fun loadSupertypes() {
            val remoteType = conceptType.asRemote(schemaMgr.openOrGetReadTx())
            supertype = remoteType.supertype
                ?.let { if (it.isRoleType) schemaMgr.createTypeState(it.asRoleType()) else null }
                ?.also { it.loadProperties() }
            supertypes = remoteType.supertypes
                .filter { it.isRoleType && it != remoteType }
                .map { schemaMgr.createTypeState(it.asRoleType()) }.toList()
        }

        override fun loadOtherProperties() {}

        override fun toString(): String {
            return "TypeState.Role: $conceptType"
        }
    }
}