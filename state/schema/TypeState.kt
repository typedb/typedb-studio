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
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.RoleType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.common.util.Message.Companion.UNKNOWN
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_DELETE_TYPE
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_LOAD_TYPE
import com.vaticle.typedb.studio.state.page.Navigable
import com.vaticle.typedb.studio.state.page.Pageable
import com.vaticle.typeql.lang.common.TypeQLToken
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.streams.toList
import mu.KotlinLogging

sealed class TypeState private constructor(val schemaMgr: SchemaManager) {

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

    abstract val conceptType: Type
    internal abstract val baseType: TypeQLToken.Type
    internal abstract val subtypesExplicit: List<TypeState>
    abstract val subtypes: List<TypeState>
    abstract val supertype: TypeState?
    abstract val supertypes: List<TypeState>

    val isRoot get() = conceptType.isRoot
    var isAbstract: Boolean by mutableStateOf(false)
    var hasSubtypes: Boolean by mutableStateOf(false)
    var ownsAttributeTypeProperties: List<AttributeTypeProperties> by mutableStateOf(emptyList())
    val ownsAttributeTypes: List<Attribute> get() = ownsAttributeTypeProperties.map { it.attributeType }
    var playsRoleTypeProperties: List<RoleTypeProperties> by mutableStateOf(emptyList())
    val playsRoleTypes: List<Role> get() = playsRoleTypeProperties.map { it.roleType }

    abstract fun updateSubtypesExplicit(newSubtypes: List<TypeState>)
    abstract fun loadSupertypes()
    abstract fun loadOtherProperties()
    abstract override fun toString(): String

    fun loadProperties() = schemaMgr.coroutineScope.launchAndHandle(schemaMgr.notificationMgr, LOGGER) {
        try {
            loadSupertypes()
            loadAbstract()
            loadOtherProperties()
            loadSubtypesRecursivelyBlocking()
        } catch (e: TypeDBClientException) {
            schemaMgr.notificationMgr.userError(LOGGER, FAILED_TO_LOAD_TYPE, e.message ?: UNKNOWN)
        }
    }

    private fun loadAbstract() {
        schemaMgr.openOrGetReadTx()?.let { isAbstract = conceptType.asRemote(it).isAbstract }
    }

    fun loadSubtypesRecursively() = schemaMgr.coroutineScope.launchAndHandle(schemaMgr.notificationMgr, LOGGER) {
        loadSubtypesRecursivelyBlocking()
    }

    private fun loadSubtypesRecursivelyBlocking() {
        loadSubtypesExplicit()
        subtypes.forEach { it.loadSubtypesRecursivelyBlocking() }
    }

    protected fun loadSubtypesExplicit(): Unit = synchronized(this) {
        schemaMgr.openOrGetReadTx()?.let { tx ->
            val new = conceptType.asRemote(tx).subtypesExplicit.toList().toSet()
            val old = subtypes.map { it.conceptType }.toSet()
            val retained: List<TypeState>
            if (new != old) {
                val deleted = old - new
                val added = (new - old).mapNotNull { schemaMgr.createTypeState(it) }
                retained = subtypes.filter { !deleted.contains(it.conceptType) }
                updateSubtypesExplicit((retained + added).sortedBy { it.conceptType.label.scopedName() })
            } else retained = subtypes
            retained.onEach { it.hasSubtypes = it.conceptType.asRemote(tx).subtypesExplicit.findAny().isPresent }
            hasSubtypes = subtypesExplicit.isNotEmpty()
        }
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
        schemaMgr: SchemaManager
    ) : TypeState(schemaMgr), Navigable<Thing>, Pageable {

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
            loadProperties()
            return true
        }

        override fun activate() {
            schemaMgr.pages.active(this)
            loadProperties()
        }

        override fun close() {
            if (isOpenAtomic.compareAndSet(true, false)) {
                schemaMgr.pages.close(this)
                schemaMgr.remove(this)
                callbacks.onClose.forEach { it(this) }
                callbacks.clear()
            }
        }

        fun closeRecursive() {
            close()
            entries.forEach { it.closeRecursive() }
        }

        override fun delete() {
            try {
                close()
                // TODO
            } catch (e: Exception) {
                schemaMgr.notificationMgr.userError(LOGGER, FAILED_TO_DELETE_TYPE, e.message ?: UNKNOWN)
            }
        }

        override fun loadOtherProperties() {
            loadOwnsAttributeTypes()
            loadPlaysRoleTypes()
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

        fun defineOwnsAttributeType(attributeType: Attribute, overriddenType: Attribute?, key: Boolean) {
            // TODO
        }

        fun undefineOwnsAttributeType(attributeType: Attribute) {
            // TODO
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
                typeTx.playsExplicit.forEach { load(typeTx = typeTx, roleType = it, isInherited = false) }
                typeTx.plays.filter { !loaded.contains(it) }.forEach {
                    load(
                        typeTx = typeTx,
                        roleType = it,
                        isInherited = true
                    )
                }
            }
            playsRoleTypeProperties = properties
        }

        fun definePlaysRoleType(roleType: Role, overriddenType: Role?) {
            // TODO
        }

        fun undefinePlaysRoleType(roleType: Role) {
            // TODO
        }

        fun exportSyntax(onSuccess: (syntax: String) -> Unit) =
            schemaMgr.coroutineScope.launchAndHandle(schemaMgr.notificationMgr, LOGGER) {
                schemaMgr.openOrGetReadTx()?.let { tx ->
                    conceptType.asRemote(tx).syntax?.let { onSuccess(it) }
                }
            }
    }

    class Entity internal constructor(
        override val conceptType: EntityType,
        supertype: Entity?,
        schemaMgr: SchemaManager
    ) : Thing(conceptType, conceptType.label.name(), schemaMgr) {

        override val parent: Entity? get() = supertype
        override val baseType = TypeQLToken.Type.ENTITY
        override var supertype: Entity? by mutableStateOf(supertype)
        override var supertypes: List<Entity> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())
        override var subtypesExplicit: List<Entity> by mutableStateOf(listOf())
        override val subtypes: List<Entity> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()

        override fun updateSubtypesExplicit(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Entity }
        }

        override fun loadSupertypes() {
            schemaMgr.openOrGetReadTx()?.let { tx ->
                val typeTx = conceptType.asRemote(tx)
                supertype = typeTx.supertype?.let {
                    if (it.isEntityType) schemaMgr.createTypeState(it.asEntityType()) else null
                }
                supertypes = typeTx.supertypes
                    .filter { it.isEntityType && it != typeTx }
                    .map { schemaMgr.createTypeState(it.asEntityType()) }.filter { it != null }.toList().filterNotNull()
            }
        }

        override fun toString(): String {
            return "TypeState.Entity: $conceptType"
        }
    }

    class Attribute internal constructor(
        override val conceptType: AttributeType,
        supertype: Attribute?,
        schemaMgr: SchemaManager
    ) : Thing(conceptType, conceptType.label.name(), schemaMgr) {

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

        override fun updateSubtypesExplicit(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Attribute }
        }

        override fun loadSupertypes() {
            schemaMgr.openOrGetReadTx()?.let { tx ->
                val typeTx = conceptType.asRemote(tx)
                supertype = typeTx.supertype?.let {
                    if (it.isAttributeType) schemaMgr.createTypeState(it.asAttributeType()) else null
                }
                supertypes = typeTx.supertypes
                    .filter { it.isAttributeType && it != typeTx }
                    .map { schemaMgr.createTypeState(it.asAttributeType()) }.toList().filterNotNull()
            }
        }

        override fun loadOtherProperties() {
            super.loadOtherProperties()
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

        override fun toString(): String {
            return "TypeState.Attribute: $conceptType"
        }
    }

    class Relation internal constructor(
        override val conceptType: RelationType,
        supertype: Relation?,
        schemaMgr: SchemaManager
    ) : Thing(conceptType, conceptType.label.name(), schemaMgr) {

        override val parent: Relation? get() = supertype
        override val baseType = TypeQLToken.Type.RELATION
        override var supertype: Relation? by mutableStateOf(supertype)
        override var supertypes: List<Relation> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())
        override var subtypesExplicit: List<Relation> by mutableStateOf(listOf())
        override val subtypes: List<Relation> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()
        var relatesRoleTypeProperties: List<RoleTypeProperties> by mutableStateOf(emptyList())
        val relatesRoleTypes: List<Role> get() = relatesRoleTypeProperties.map { it.roleType }

        override fun updateSubtypesExplicit(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Relation }
        }

        override fun loadSupertypes() {
            schemaMgr.openOrGetReadTx()?.let { tx ->
                val typeTx = conceptType.asRemote(tx)
                supertype = typeTx.supertype?.let {
                    if (it.isRelationType) schemaMgr.createTypeState(it.asRelationType()) else null
                }
                supertypes = typeTx.supertypes
                    .filter { it.isRelationType && it != typeTx }
                    .map { schemaMgr.createTypeState(it.asRelationType()) }.toList().filterNotNull()
            }
        }

        override fun loadOtherProperties() {
            super.loadOtherProperties()
            loadRelatesRoleType()
        }

        fun loadRelatesRoleTypeRecursively() =
            schemaMgr.coroutineScope.launchAndHandle(schemaMgr.notificationMgr, LOGGER) {
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
                typeTx.relatesExplicit.forEach {
                    load(typeTx = typeTx, roleType = it, isInherited = false)
                }
                typeTx.relates.filter { !loaded.contains(it) }.forEach {
                    load(typeTx = typeTx, roleType = it, isInherited = true)
                }
            }
            relatesRoleTypeProperties = properties
        }

        fun defineRelatesRoleType(roleType: String, overriddenType: Role?) {
            // TODO
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
        schemaMgr: SchemaManager
    ) : TypeState(schemaMgr) {

        private val name: String by mutableStateOf(conceptType.label.name())
        val scopedName get() = relationType.name + ":" + name

        override val baseType: TypeQLToken.Type = TypeQLToken.Type.ROLE
        override var subtypesExplicit: List<Role> by mutableStateOf(emptyList())
        override val subtypes: List<Role> get() = subtypesExplicit.map { listOf(it) + it.subtypes }.flatten()
        override var supertype: Role? by mutableStateOf(supertype)
        override var supertypes: List<Role> by mutableStateOf(supertype?.let { listOf(it) } ?: listOf())

        override fun updateSubtypesExplicit(newSubtypes: List<TypeState>) {
            subtypesExplicit = newSubtypes.map { it as Role }
        }

        override fun loadSupertypes() {
            schemaMgr.openOrGetReadTx()?.let { tx ->
                val typeTx = conceptType.asRemote(tx)
                supertype = typeTx.supertype?.let {
                    if (it.isRoleType) schemaMgr.createTypeState(it.asRoleType()) else null
                }
                supertypes = typeTx.supertypes
                    .filter { it.isRoleType && it != typeTx }
                    .map { schemaMgr.createTypeState(it.asRoleType()) }.toList().filterNotNull()
            }
        }

        override fun loadOtherProperties() {}

        override fun toString(): String {
            return "TypeState.Role: $conceptType"
        }
    }
}