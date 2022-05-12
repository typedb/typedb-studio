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

class TypeState constructor(
    private val type: ThingType,
    supertypeInit: TypeState?,
    isExpandableInit: Boolean,
    val schemaMgr: SchemaManager,
) : Navigable<TypeState>, Resource {

    data class AttributeTypeProperties(
        val attributeType: TypeState, val overriddenType: TypeState?, val isKey: Boolean, val isInherited: Boolean
    )

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override val name: String by mutableStateOf(type.label.name())
    override val parent: TypeState? get() = supertype
    override val info: String? get() = computeInfo()
    override val isBulkExpandable: Boolean = true
    override var isExpandable: Boolean by mutableStateOf(isExpandableInit)
    override var entries: List<TypeState> = emptyList()
    override val windowTitle: String get() = computeWindowTitle()
    override val isOpen: Boolean get() = isOpenAtomic.get()
    override val isWritable: Boolean = true
    override val isEmpty: Boolean = false
    override val isUnsavedResource: Boolean = false
    override val hasUnsavedChanges: Boolean by mutableStateOf(false)

    val isEntityType get() = type.isEntityType
    val isRelationType get() = type.isRelationType
    val isAttributeType get() = type.isAttributeType
    val isRoot get() = type.isRoot
    val valueType: String? = computeValueType()
    val isKeyable: Boolean get() = type.isAttributeType && type.asAttributeType().valueType.isKeyable

    var supertype: TypeState? by mutableStateOf(supertypeInit)
    var isAbstract: Boolean by mutableStateOf(false)
    var ownedAttributes: Map<AttributeType, AttributeTypeProperties> by mutableStateOf(mapOf())
    val subtypes: List<TypeState> get() = entries.map { listOf(it) + it.subtypes }.flatten()

    private val isOpenAtomic = AtomicBoolean(false)
    private val onClose = LinkedBlockingQueue<(TypeState) -> Unit>()

    private fun computeInfo(): String? = when {
        type.isAttributeType && !type.isRoot -> valueType
        else -> null
    }

    private fun computeWindowTitle(): String {
        val props = mutableListOf(
            when {
                type.isEntityType -> TypeQLToken.Type.ENTITY
                type.isRelationType -> TypeQLToken.Type.RELATION
                type.isAttributeType -> TypeQLToken.Type.ATTRIBUTE
                type.isThingType -> TypeQLToken.Type.THING
                else -> throw IllegalStateException("Unrecognised concept base type")
            }.name.lowercase()
        )
        computeInfo()?.let { props.add(it) }
        return "$name (" + props.joinToString(", ") + ") @ " + schemaMgr.database
    }

    private fun computeValueType(): String? {
        return if (type.isAttributeType && !type.isRoot) type.asAttributeType().valueType.name.lowercase()
        else null
    }

    override fun launchWatcher() {}
    override fun stopWatcher() {}
    override fun beforeRun(function: (Resource) -> Unit) {}
    override fun beforeSave(function: (Resource) -> Unit) {}
    override fun beforeClose(function: (Resource) -> Unit) {}
    override fun execBeforeClose() {}
    override fun save(onSuccess: ((Resource) -> Unit)?) {}
    override fun move(onSuccess: ((Resource) -> Unit)?) {}

    override fun tryOpen(): Boolean {
        reloadProperties()
        isOpenAtomic.set(true)
        return true
    }

    fun reloadProperties() = schemaMgr.coroutineScope.launch {
        loadSupertype()
        loadAbstract()
        loadOwnedAttributes()
    }

    private fun loadSupertype() {
        supertype = type.asRemote(schemaMgr.openOrGetReadTx()).supertype?.let { schemaMgr.createTypeState(it) }
    }

    private fun loadAbstract() {
        isAbstract = type.asRemote(schemaMgr.openOrGetReadTx()).isAbstract
    }

    private fun loadOwnedAttributes() {
        val map = mutableMapOf<AttributeType, AttributeTypeProperties>()
        val conceptTx = type.asRemote(schemaMgr.openOrGetReadTx())

        fun properties(attributeType: AttributeType, isKey: Boolean, isInherited: Boolean) {
            map[attributeType] = AttributeTypeProperties(
                attributeType = schemaMgr.createTypeState(attributeType),
                overriddenType = conceptTx.getOwnsOverridden(attributeType)?.let { schemaMgr.createTypeState(it) },
                isKey = isKey,
                isInherited = isInherited
            )
        }

        conceptTx.getOwnsExplicit(true).forEach {
            properties(it, isKey = true, isInherited = false)
        }
        conceptTx.getOwnsExplicit(false).filter { !map.contains(it) }.forEach {
            properties(it, isKey = false, isInherited = false)
        }
        conceptTx.getOwns(true).filter { !map.contains(it) }.forEach {
            properties(it, isKey = true, isInherited = true)
        }
        conceptTx.getOwns(false).filter { !map.contains(it) }.forEach {
            properties(it, isKey = false, isInherited = true)
        }
        ownedAttributes = map
    }

    fun addOwnedAttributes(attributeType: TypeState, overriddenType: TypeState?, key: Boolean) {
        // TODO
    }

    fun removeOwnedAttribute(attType: TypeState) {
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
            entries = (retainedEntries + newEntries).sorted()
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
}