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
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_DELETE_TYPE
import com.vaticle.typedb.studio.state.resource.Navigable
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typeql.lang.common.TypeQLToken
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.streams.toList
import kotlinx.coroutines.launch
import mu.KotlinLogging

sealed class TypeState private constructor(
    nameInit: String,
    isExpandableInit: Boolean,
    val schemaMgr: SchemaManager,
) : Navigable<TypeState>, Resource {

    data class AttributeTypeProperties(
        val attributeType: Attribute,
        val overriddenType: Attribute?,
        val isKey: Boolean,
        val isInherited: Boolean
    )

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override val name: String by mutableStateOf(nameInit)
    override val info: String? = null
    override val isBulkExpandable: Boolean = true
    override var isExpandable: Boolean by mutableStateOf(isExpandableInit)

    override val windowTitle: String get() = computeWindowTitle()
    override val isOpen: Boolean get() = isOpenAtomic.get()
    override val isWritable: Boolean = true
    override val isEmpty: Boolean = false
    override val isUnsavedResource: Boolean = false
    override val hasUnsavedChanges: Boolean by mutableStateOf(false)

    abstract val type: ThingType
    abstract val baseType: TypeQLToken.Type
    abstract val supertype: TypeState?
    abstract val supertypes: List<TypeState>
    abstract val subtypes: List<TypeState>

    val isRoot get() = type.isRoot
    var isAbstract: Boolean by mutableStateOf(false)
    var ownedAttributeTypeProperties: Map<AttributeType, AttributeTypeProperties> by mutableStateOf(mapOf())
    val ownedAttributeTypes: List<Attribute> get() = ownedAttributeTypeProperties.values.map { it.attributeType }

    private val isOpenAtomic = AtomicBoolean(false)
    private val onClose = LinkedBlockingQueue<(TypeState) -> Unit>()

    private fun computeWindowTitle(): String {
        val props = listOf(baseType.name.lowercase()) + info?.let { listOf(it) }
        return "$name (" + props.joinToString(", ") + ") @ " + schemaMgr.database
    }

    override fun launchWatcher() {}
    override fun stopWatcher() {}
    override fun beforeRun(function: (Resource) -> Unit) {}
    override fun beforeSave(function: (Resource) -> Unit) {}
    override fun beforeClose(function: (Resource) -> Unit) {}
    override fun execBeforeClose() {}
    override fun save(onSuccess: ((Resource) -> Unit)?) {}
    override fun move(onSuccess: ((Resource) -> Unit)?) {}

    abstract fun updateEntries(newEntries: List<TypeState>)
    abstract fun loadSupertypes()
    abstract fun loadOtherProperties()

    override fun tryOpen(): Boolean {
        reloadProperties()
        isOpenAtomic.set(true)
        return true
    }

    fun reloadProperties() = schemaMgr.coroutineScope.launch {
        loadSupertypes()
        loadAbstract()
        loadOwnedAttributeTypes()
        loadOtherProperties()
    }

    private fun loadAbstract() {
        isAbstract = type.asRemote(schemaMgr.openOrGetReadTx()).isAbstract
    }

    private fun loadOwnedAttributeTypes() {
        val props = mutableMapOf<AttributeType, AttributeTypeProperties>()
        val conceptTx = type.asRemote(schemaMgr.openOrGetReadTx())

        fun properties(attributeType: AttributeType, isKey: Boolean, isInherited: Boolean) {
            props[attributeType] = AttributeTypeProperties(
                attributeType = schemaMgr.createTypeState(attributeType),
                overriddenType = conceptTx.getOwnsOverridden(attributeType)?.let { schemaMgr.createTypeState(it) },
                isKey = isKey,
                isInherited = isInherited
            )
        }

        conceptTx.getOwnsExplicit(true).forEach {
            properties(it, isKey = true, isInherited = false)
        }
        conceptTx.getOwnsExplicit(false).filter { !props.contains(it) }.forEach {
            properties(it, isKey = false, isInherited = false)
        }
        conceptTx.getOwns(true).filter { !props.contains(it) }.forEach {
            properties(it, isKey = true, isInherited = true)
        }
        conceptTx.getOwns(false).filter { !props.contains(it) }.forEach {
            properties(it, isKey = false, isInherited = true)
        }
        ownedAttributeTypeProperties = props
    }

    fun addOwnedAttributeTypes(attributeType: TypeState.Attribute, overriddenType: TypeState.Attribute?, key: Boolean) {
        // TODO
    }

    fun removeOwnedAttributeType(attType: TypeState.Attribute) {
        // TODO
    }

    fun reloadEntriesRecursively() = schemaMgr.coroutineScope.launch {
        reloadEntriesRecursivelyBlocking()
    }

    private fun reloadEntriesRecursivelyBlocking() {
        reloadEntries()
        entries.forEach { it.reloadEntriesRecursivelyBlocking() }
    }

    override fun reloadEntries() {
        val tx = schemaMgr.openOrGetReadTx()
        val new = type.asRemote(tx).subtypesExplicit.toList().toSet()
        val old = entries.map { it.type }.toSet()
        val refresh: List<TypeState>
        if (new != old) {
            val deleted = old - new
            val added = new - old
            val retainedEntries = entries.filter { !deleted.contains(it.type) }
            val newEntries = added.map { schemaMgr.createTypeState(it) }
            updateEntries((retainedEntries + newEntries).sorted())
            refresh = retainedEntries
        } else refresh = entries
        refresh.onEach { it.isExpandable = it.type.asRemote(tx).subtypesExplicit.findAny().isPresent }
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
        return "TypeState: $type"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TypeState
        return this.type == other.type
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    class Entity internal constructor(
        override val type: EntityType,
        supertypeInit: Entity?,
        isExpandable: Boolean,
        schemaMgr: SchemaManager
    ) : TypeState(type.label.name(), isExpandable, schemaMgr) {

        override val parent: Entity? get() = supertype
        override var entries: List<Entity> = emptyList()
        override val baseType = TypeQLToken.Type.ENTITY
        override var supertype: Entity? by mutableStateOf(supertypeInit)
        override var supertypes: List<Entity> by mutableStateOf(supertypeInit?.let { listOf(it) } ?: listOf())
        override val subtypes: List<Entity> get() = entries.map { listOf(it) + it.subtypes }.flatten()

        override fun updateEntries(newEntries: List<TypeState>) {
            entries = newEntries.map { it as Entity }
        }

        override fun loadSupertypes() {
            val remoteType = type.asRemote(schemaMgr.openOrGetReadTx())
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
        override val type: RelationType,
        supertypeInit: Relation?,
        isExpandable: Boolean,
        schemaMgr: SchemaManager
    ) : TypeState(type.label.name(), isExpandable, schemaMgr) {

        override val parent: Relation? get() = supertype
        override var entries: List<Relation> = emptyList()
        override val baseType = TypeQLToken.Type.RELATION
        override var supertype: Relation? by mutableStateOf(supertypeInit)
        override var supertypes: List<Relation> by mutableStateOf(supertypeInit?.let { listOf(it) } ?: listOf())
        override val subtypes: List<Relation> get() = entries.map { listOf(it) + it.subtypes }.flatten()

        override fun updateEntries(newEntries: List<TypeState>) {
            entries = newEntries.map { it as Relation }
        }

        override fun loadSupertypes() {
            val remoteType = type.asRemote(schemaMgr.openOrGetReadTx())
            supertype = remoteType.supertype
                ?.let { if (it.isRelationType) schemaMgr.createTypeState(it.asRelationType()) else null }
                ?.also { it.reloadProperties() }
            supertypes = remoteType.supertypes
                .filter { it.isRelationType && it != remoteType }
                .map { schemaMgr.createTypeState(it.asRelationType()) }.toList()
        }

        override fun loadOtherProperties() {}
    }

    class Attribute internal constructor(
        override val type: AttributeType,
        supertypeInit: Attribute?,
        isExpandable: Boolean,
        schemaMgr: SchemaManager
    ) : TypeState(type.label.name(), isExpandable, schemaMgr) {

        override val info get() = valueType
        override val parent: Attribute? get() = supertype
        override var entries: List<Attribute> = emptyList()
        override val baseType = TypeQLToken.Type.ATTRIBUTE
        override var supertype: Attribute? by mutableStateOf(supertypeInit)
        override var supertypes: List<Attribute> by mutableStateOf(supertypeInit?.let { listOf(it) } ?: listOf())
        override val subtypes: List<Attribute> get() = entries.map { listOf(it) + it.subtypes }.flatten()

        val valueType: String? = if (!type.isRoot) type.valueType.name.lowercase() else null
        val isKeyable: Boolean get() = type.valueType.isKeyable

        override fun updateEntries(newEntries: List<TypeState>) {
            entries = newEntries.map { it as Attribute }
        }

        override fun loadSupertypes() {
            val remoteType = type.asRemote(schemaMgr.openOrGetReadTx())
            supertype = remoteType.supertype
                ?.let { if (it.isAttributeType) schemaMgr.createTypeState(it.asAttributeType()) else null }
                ?.also { it.reloadProperties() }
            supertypes = remoteType.supertypes
                .filter { it.isAttributeType && it != remoteType }
                .map { schemaMgr.createTypeState(it.asAttributeType()) }.toList()
        }

        override fun loadOtherProperties() {
            loadAttributeTypeOwners()
        }

        private fun loadAttributeTypeOwners() {

        }
    }
}