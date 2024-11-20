/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.schema

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.typedb.studio.service.common.util.Label
import com.typedb.studio.service.common.util.Message.Companion.UNKNOWN
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_RENAME_TYPE
import com.typedb.driver.api.concept.Concept.Transitivity.EXPLICIT
import com.typedb.driver.api.concept.type.Type
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
    internal abstract fun loadConstraints()
    internal abstract fun loadDependencies()
    internal abstract fun updateConceptType(label: String = name)
    internal abstract fun resetLoadedConnectedTypes()
    abstract override fun toString(): String

    fun loadSupertype(): Unit = schemaSrv.mayRunReadTx { tx ->
        if (!conceptType.isRoot) {
            supertype = conceptType.getSupertype(tx).resolve()?.let {
                if (isSameEncoding(it)) typeStateOf(asSameEncoding(it)) else null
            }
        }
    } ?: Unit

    fun loadSupertypesAsync() = coroutines.launchAndHandle(notifications, LOGGER) { loadSupertypes() }

    fun loadSupertypes(): Unit = schemaSrv.mayRunReadTx { _ ->
        loadSupertype()
        supertype?.loadInheritables()
        supertype?.loadSupertypes()
        supertypes = supertype?.let { listOf(it) + it.supertypes } ?: listOf()
    } ?: Unit

    protected fun loadHasSubtypes() = schemaSrv.mayRunReadTx { tx ->
        // TODO: Implement API to retrieve .hasSubtypes() on TypeDB Type API
        hasSubtypes = conceptType.getSubtypes(tx, EXPLICIT).findAny().isPresent
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
            conceptType.setLabel(it, label)
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
