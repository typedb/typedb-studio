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
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Message.Companion.UNKNOWN
import com.vaticle.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_RENAME_TYPE
import mu.KotlinLogging

sealed class TypeState<T : Type, TS : TypeState<T, TS>> constructor(
    conceptType: T,
    supertype: TS?,
    val encoding: Encoding,
    val schemaSrv: SchemaService
) {

    enum class Encoding(val label: String) {
        ENTITY_TYPE(Label.ENTITY.lowercase()),
        ATTRIBUTE_TYPE(Label.ATTRIBUTE.lowercase()),
        RELATION_TYPE(Label.RELATION.lowercase()),
        ROLE_TYPE(Label.ROLE.lowercase())
    }

    interface OverridingTypeProperties<T : TypeState<*, *>> {
        val type: T
        val overriddenType: T?
    }

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

    protected fun loadHasSubtypes() = schemaSrv.mayRunReadTx { tx ->
        // TODO: Implement API to retrieve .hasSubtypes() on TypeDB Type API
        hasSubtypes = conceptType.asRemote(tx).subtypesExplicit.findAny().isPresent
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
}