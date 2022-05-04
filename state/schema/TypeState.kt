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
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.studio.state.common.util.Message.Schema.Companion.FAILED_TO_DELETE_TYPE
import com.vaticle.typedb.studio.state.resource.Navigable
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typeql.lang.common.TypeQLToken
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.streams.toList
import mu.KotlinLogging

class TypeState constructor(
    private val concept: ThingType,
    initSupertype: TypeState?,
    val schemaMgr: SchemaManager,
    isExpandableInit: Boolean,
) : Navigable<TypeState>, Resource {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override val name: String by mutableStateOf(concept.label.name())
    override val parent: TypeState? get() = supertype
    override val info: String? = null
    override val isBulkExpandable: Boolean = true
    override var isExpandable: Boolean by mutableStateOf(isExpandableInit)
    override var entries: List<TypeState> = emptyList()
    override val fullName: String = computeFullName()
    override val isOpen: Boolean get() = isOpenAtomic.get()
    override val isWritable: Boolean = true
    override val isEmpty: Boolean = false
    override val isUnsavedResource: Boolean = false
    override val hasUnsavedChanges: Boolean by mutableStateOf(false)

    val isEntityType get() = concept.isEntityType
    val isRelationType get() = concept.isRelationType
    val isAttributeType get() = concept.isAttributeType
    val isRoot get() = concept.isRoot
    var supertype: TypeState? by mutableStateOf(initSupertype)

    private val isOpenAtomic = AtomicBoolean(false)
    private val onClose = LinkedBlockingQueue<(TypeState) -> Unit>()

    private fun computeFullName(): String {
        val base = if (concept.isEntityType) TypeQLToken.Type.ENTITY
        else if (concept.isRelationType) TypeQLToken.Type.RELATION
        else if (concept.isAttributeType) TypeQLToken.Type.ATTRIBUTE
        else if (concept.isRoleType) TypeQLToken.Type.ROLE
        else if (concept.isThingType) TypeQLToken.Type.THING
        else throw IllegalStateException("Unrecognised concept base type")
        return "$base: $name"
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
        isOpenAtomic.set(true)
        return true
    }

    override fun reloadEntries() {
        val tx = schemaMgr.openOrGetReadTx()
        val new = concept.asRemote(tx).subtypesExplicit.toList().toSet()
        val old = entries.map { it.concept }.toSet()
        if (new != old) {
            val deleted = old - new
            val added = new - old
            val retainedEntries = entries.filter { !deleted.contains(it.concept) }
            val newEntries = added.map { TypeState(it, this, schemaMgr, false) }
            entries = (retainedEntries + newEntries).sorted()
        }
        entries.onEach { it.isExpandable = it.concept.asRemote(tx).subtypesExplicit.findAny().isPresent }
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
        return "TypeState: $concept"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TypeState
        return this.concept == other.concept
    }

    override fun hashCode(): Int {
        return concept.hashCode()
    }
}