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

sealed class TypeState private constructor(
    name: String,
    isExpandable: Boolean,
    val schemaMgr: SchemaManager,
) : Navigable<TypeState>, Resource {

    data class AttributeTypeProperties(
        val attributeType: Attribute,
        val overriddenType: Attribute?,
        val isKey: Boolean,
        val isInherited: Boolean
    )

    data class PlaysRoleTypeProperties(
        val roleType: Relation.Role,
        val overriddenType: Relation.Role?,
        val isInherited: Boolean
    )

    data class RelatesRoleTypeProperties(
        val roleType: Relation.Role,
        val overriddenType: Relation.Role?,
        val isInherited: Boolean
    )

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override val name: String by mutableStateOf(name)
    override val info: String? = null
    override val isBulkExpandable: Boolean = true
    override var isExpandable: Boolean by mutableStateOf(isExpandable)
    override val entries: List<TypeState> get() = subtypesExplicit

    override val windowTitle: String get() = computeWindowTitle()
    override val isOpen: Boolean get() = isOpenAtomic.get()
    override val isWritable: Boolean = true
    override val isEmpty: Boolean = false
    override val isUnsavedResource: Boolean = false
    override val hasUnsavedChanges: Boolean by mutableStateOf(false)

    internal abstract val conceptType: ThingType
    internal abstract val baseType: TypeQLToken.Type
    abstract val supertype: TypeState?
    abstract val supertypes: List<TypeState>
    abstract val subtypesExplicit: List<TypeState>
    abstract val subtypes: List<TypeState>

    val isRoot get() = conceptType.isRoot
    var isAbstract: Boolean by mutableStateOf(false)
    var ownsAttributeTypeProperties: Map<AttributeType, AttributeTypeProperties> by mutableStateOf(mapOf())
    val ownsAttributeTypes: List<Attribute> get() = ownsAttributeTypeProperties.values.map { it.attributeType }
    var playsRoleTypeProperties: Map<RoleType, PlaysRoleTypeProperties> by mutableStateOf(mapOf())
    val playsRoleTypes: List<Relation.Role> get() = playsRoleTypeProperties.values.map { it.roleType }

    private val isOpenAtomic = AtomicBoolean(false)
    private val onClose = LinkedBlockingQueue<(TypeState) -> Unit>()

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

    abstract fun updateSubtypes(newSubtypes: List<TypeState>)
    abstract fun loadSupertypes()
    abstract fun loadOtherProperties()

    override fun tryOpen(): Boolean {
        isOpenAtomic.set(true)
        return true
    }

    override fun activate() {
        reloadProperties()
    }

    override fun reloadEntries() {
        reloadSubtypesExplicit()
    }

    fun reloadProperties() = schemaMgr.coroutineScope.launch {
        try {
            loadSupertypes()
            loadAbstract()
            loadOwnsAttributeTypes()
            loadPlaysRoleTypes()
            loadOtherProperties()
        } catch (e: TypeDBClientException) {
            schemaMgr.notificationMgr.userError(LOGGER, FAILED_TO_LOAD_TYPE, e.message ?: "Unknown")
        }
    }

    private fun loadAbstract() {
        isAbstract = conceptType.asRemote(schemaMgr.openOrGetReadTx()).isAbstract
    }

    private fun loadOwnsAttributeTypes() {
        val props = mutableMapOf<AttributeType, AttributeTypeProperties>()
        val typeTx = conceptType.asRemote(schemaMgr.openOrGetReadTx())

        fun load(attributeType: AttributeType, isKey: Boolean, isInherited: Boolean) {
            props[attributeType] = AttributeTypeProperties(
                attributeType = schemaMgr.createTypeState(attributeType),
                overriddenType = typeTx.getOwnsOverridden(attributeType)?.let { schemaMgr.createTypeState(it) },
                isKey = isKey,
                isInherited = isInherited
            )
        }

        typeTx.getOwnsExplicit(true).forEach {
            load(attributeType = it, isKey = true, isInherited = false)
        }
        typeTx.getOwnsExplicit(false).filter { !props.contains(it) }.forEach {
            load(attributeType = it, isKey = false, isInherited = false)
        }
        typeTx.getOwns(true).filter { !props.contains(it) }.forEach {
            load(attributeType = it, isKey = true, isInherited = true)
        }
        typeTx.getOwns(false).filter { !props.contains(it) }.forEach {
            load(attributeType = it, isKey = false, isInherited = true)
        }
        ownsAttributeTypeProperties = props
    }

    fun defineOwnsAttributeTypes(attributeType: Attribute, overriddenType: Attribute?, key: Boolean) {
        // TODO
    }

    fun undefineOwnsAttributeType(attType: Attribute) {
        // TODO
    }

    private fun loadPlaysRoleTypes() {
        val props = mutableMapOf<RoleType, PlaysRoleTypeProperties>()
        val typeTx = conceptType.asRemote(schemaMgr.openOrGetReadTx())

        fun load(roleType: RoleType, isInherited: Boolean) {
            val relationType = schemaMgr.createTypeState(roleType.asRemote(schemaMgr.openOrGetReadTx()).relationType)
            props[roleType] = PlaysRoleTypeProperties(
                roleType = relationType.Role(roleType),
                overriddenType = typeTx.getPlaysOverridden(roleType)?.let { relationType.Role(it) },
                isInherited = isInherited
            )
        }

        typeTx.playsExplicit.forEach { load(roleType = it, isInherited = false) }
        typeTx.plays.filter { !props.contains(it) }.forEach { load(roleType = it, isInherited = true) }
        playsRoleTypeProperties = props
    }

    fun definePlaysRoleType(roleType: Relation.Role, overriddenType: Relation.Role?) {
        // TODO
    }

    fun undefinePlaysRoleType(roleType: Relation.Role) {
        // TODO
    }

    fun reloadSubtypesRecursively() = schemaMgr.coroutineScope.launch {
        reloadSubtypesRecursivelyBlocking()
    }

    private fun reloadSubtypesRecursivelyBlocking() {
        reloadSubtypesExplicit()
        subtypes.forEach { it.reloadSubtypesRecursivelyBlocking() }
    }

    private fun reloadSubtypesExplicit() {
        val tx = schemaMgr.openOrGetReadTx()
        val new = conceptType.asRemote(tx).subtypesExplicit.toList().toSet()
        val old = subtypes.map { it.conceptType }.toSet()
        val retained: List<TypeState>
        if (new != old) {
            val deleted = old - new
            val added = new - old
            retained = subtypes.filter { !deleted.contains(it.conceptType) }
            updateSubtypes((retained + added.map { schemaMgr.createTypeState(it) }).sorted())
        } else retained = subtypes
        retained.onEach { it.isExpandable = it.conceptType.asRemote(tx).subtypesExplicit.findAny().isPresent }
        isExpandable = entries.isNotEmpty()
    }

    override fun rename(onSuccess: ((Resource) -> Unit)?) {
        // TODO
    }

    override fun onClose(function: (Resource) -> Unit) {
        onClose.put(function)
    }

    override fun onReopen(function: (Resource) -> Unit) {
        // TODO
    }

    override fun delete() {
        try {
            close()
            // TODO
        } catch (e: Exception) {
            schemaMgr.notificationMgr.userError(LOGGER, FAILED_TO_DELETE_TYPE, e.message ?: "Unknown")
        }
    }

    override fun close() {
        if (isOpenAtomic.compareAndSet(true, false)) onClose.forEach { it(this) }
    }

    override fun closeRecursive() {
        close()
        entries.forEach { it.closeRecursive() }
    }

    override fun compareTo(other: Navigable<TypeState>): Int {
        return this.name.compareTo(other.name, ignoreCase = true)
    }

    override fun toString(): String {
        return "TypeState: $conceptType"
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

    class Entity internal constructor(
        override val conceptType: EntityType,
        supertypeInit: Entity?,
        isExpandable: Boolean,
        schemaMgr: SchemaManager
    ) : TypeState(conceptType.label.name(), isExpandable, schemaMgr) {

        override val parent: Entity? get() = supertype
        override val baseType = TypeQLToken.Type.ENTITY
        override var supertype: Entity? by mutableStateOf(supertypeInit)
        override var supertypes: List<Entity> by mutableStateOf(supertypeInit?.let { listOf(it) } ?: listOf())
        override var subtypesExplicit: List<Entity> = emptyList()
        override val subtypes: List<Entity> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()

        override fun updateSubtypes(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Entity }
        }

        override fun loadSupertypes() {
            val remoteType = conceptType.asRemote(schemaMgr.openOrGetReadTx())
            supertype = remoteType.supertype
                ?.let { if (it.isEntityType) schemaMgr.createTypeState(it.asEntityType()) else null }
                ?.also { it.reloadProperties() }
            supertypes = remoteType.supertypes
                .filter { it.isEntityType && it != remoteType }
                .map { schemaMgr.createTypeState(it.asEntityType()) }.toList()
        }

        override fun loadOtherProperties() {}
    }

    class Relation internal constructor(
        override val conceptType: RelationType,
        supertype: Relation?,
        isExpandable: Boolean,
        schemaMgr: SchemaManager
    ) : TypeState(conceptType.label.name(), isExpandable, schemaMgr) {

        inner class Role constructor(internal val conceptType: RoleType) {
            val relationType get() = this@Relation
            val name by mutableStateOf(conceptType.label.name())
            val scopedName get() = relationType.name + ":" + name
        }

        override val parent: Relation? get() = supertype
        override val baseType = TypeQLToken.Type.RELATION
        override var supertype: Relation? by mutableStateOf(supertype)
        override var supertypes: List<Relation> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())
        override var subtypesExplicit: List<Relation> = emptyList()
        override val subtypes: List<Relation> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()
        var relatesRoleTypeProperties: Map<RoleType, RelatesRoleTypeProperties> by mutableStateOf(mapOf())
        val relatesRoleTypes: List<Role> get() = relatesRoleTypeProperties.values.map { it.roleType }

        override fun updateSubtypes(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Relation }
        }

        override fun loadSupertypes() {
            val remoteType = conceptType.asRemote(schemaMgr.openOrGetReadTx())
            supertype = remoteType.supertype
                ?.let { if (it.isRelationType) schemaMgr.createTypeState(it.asRelationType()) else null }
                ?.also { it.reloadProperties() }
            supertypes = remoteType.supertypes
                .filter { it.isRelationType && it != remoteType }
                .map { schemaMgr.createTypeState(it.asRelationType()) }.toList()
        }

        override fun loadOtherProperties() {
            loadRelatesRoleType()
        }

        fun loadRelatesRoleTypeRecursively() = schemaMgr.coroutineScope.launch {
            loadRelatesRoleTypeRecursivelyBlocking()
        }

        private fun loadRelatesRoleTypeRecursivelyBlocking() {
            loadRelatesRoleType()
            subtypesExplicit.forEach { it.loadRelatesRoleTypeRecursivelyBlocking() }
        }

        private fun loadRelatesRoleType() {
            val props = mutableMapOf<RoleType, RelatesRoleTypeProperties>()
            val typeTx = conceptType.asRemote(schemaMgr.openOrGetReadTx())

            fun load(roleType: RoleType, isInherited: Boolean) {
                val relationType =
                    schemaMgr.createTypeState(roleType.asRemote(schemaMgr.openOrGetReadTx()).relationType)
                props[roleType] = RelatesRoleTypeProperties(
                    roleType = relationType.Role(roleType),
                    overriddenType = typeTx.getRelatesOverridden(roleType.label.name())?.let { relationType.Role(it) },
                    isInherited = isInherited
                )
            }

            typeTx.relatesExplicit.forEach { load(roleType = it, isInherited = false) }
            typeTx.relates.filter { !props.contains(it) }.forEach { load(roleType = it, isInherited = true) }
            relatesRoleTypeProperties = props
        }
    }

    class Attribute internal constructor(
        override val conceptType: AttributeType,
        supertype: Attribute?,
        isExpandable: Boolean,
        schemaMgr: SchemaManager
    ) : TypeState(conceptType.label.name(), isExpandable, schemaMgr) {

        data class OwnerTypeProperties(val ownerType: TypeState, val isKey: Boolean, val isInherited: Boolean)

        override val info get() = valueType
        override val parent: Attribute? get() = supertype
        override val baseType = TypeQLToken.Type.ATTRIBUTE
        override var supertype: Attribute? by mutableStateOf(supertype)
        override var supertypes: List<Attribute> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())
        override var subtypesExplicit: List<Attribute> = emptyList()
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
                ?.also { it.reloadProperties() }
            supertypes = remoteType.supertypes
                .filter { it.isAttributeType && it != remoteType }
                .map { schemaMgr.createTypeState(it.asAttributeType()) }.toList()
        }

        override fun loadOtherProperties() {
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
    }
}