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
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.RoleType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Message
import com.vaticle.typedb.studio.service.common.util.Message.Companion.UNKNOWN
import com.vaticle.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_CHANGE_ABSTRACT
import com.vaticle.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_CREATE_TYPE
import com.vaticle.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_DEFINE_OWN_ATTRIBUTE_TYPE
import com.vaticle.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_DEFINE_PLAY_ROLE_TYPE
import com.vaticle.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_DELETE_TYPE
import com.vaticle.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_UNDEFINE_PLAYS_ROLE_TYPE
import com.vaticle.typedb.studio.service.common.util.Sentence
import com.vaticle.typedb.studio.service.page.Navigable
import com.vaticle.typedb.studio.service.page.Pageable
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.TypeQL.rel
import com.vaticle.typeql.lang.TypeQL.`var`
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import mu.KotlinLogging

sealed class ThingTypeState<TT : ThingType, TTS : ThingTypeState<TT, TTS>> constructor(
    conceptType: TT,
    supertype: TTS?,
    encoding: Encoding,
    schemaSrv: SchemaService
) : TypeState<TT, TTS>(conceptType, supertype, encoding, schemaSrv), Navigable<ThingTypeState<TT, TTS>>, Pageable {

    private class Callbacks {

        val onReopen = LinkedBlockingQueue<(ThingTypeState<*, *>) -> Unit>()
        val onClose = LinkedBlockingQueue<(ThingTypeState<*, *>) -> Unit>()
        val onSubtypesUpdated = LinkedBlockingQueue<() -> Unit>()

        fun clear() {
            onReopen.clear()
            onClose.clear()
            onSubtypesUpdated.clear()
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    var ownsAttTypeProperties: List<AttributeTypeState.OwnsAttTypeProperties> by mutableStateOf(emptyList())
    val ownsAttTypes: List<AttributeTypeState> get() = ownsAttTypeProperties.map { it.attributeType }
    var playsRoleTypeProperties: List<RoleTypeState.PlaysRoleTypeProperties> by mutableStateOf(emptyList())
    val playsRoleTypes: List<RoleTypeState> get() = playsRoleTypeProperties.map { it.roleType }

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
    override fun compareTo(other: Navigable<ThingTypeState<TT, TTS>>): Int = name.compareTo(other.name)

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
        val properties = mutableListOf<AttributeTypeState.OwnsAttTypeProperties>()

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
                    AttributeTypeState.OwnsAttTypeProperties(
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
        val properties = mutableListOf<RoleTypeState.PlaysRoleTypeProperties>()

        fun load(tx: TypeDBTransaction, typeTx: ThingType.Remote, roleTypeConcept: RoleType, isInherited: Boolean) {
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
                val canBeUndefined = !tx.query().match(
                    TypeQL.match(
                        `var`("x").isa(typeTx.label.name()),
                        rel(roleTypeConcept.label.name(), "x")
                    )
                ).findFirst().isPresent
                properties.add(
                    RoleTypeState.PlaysRoleTypeProperties(
                        roleType, overriddenType, extendedType, isInherited, canBeUndefined
                    )
                )
            }
        }

        schemaSrv.mayRunReadTx { tx ->
            val typeTx = conceptType.asRemote(tx)
            typeTx.playsExplicit.forEach { load(tx, typeTx, it, false) }
            typeTx.plays.filter { !loaded.contains(it) }.forEach { load(tx, typeTx, it, true) }
        }
        playsRoleTypeProperties = properties
    }

    protected fun tryCreateSubtype(
        label: String, dialogState: SchemaService.TypeDialogState<*>, creatorFn: (TypeDBTransaction) -> Unit
    ) {
        if (schemaSrv.mayRunReadTx { it.concepts()?.getThingType(label) } != null) notifications.userError(
            LOGGER, Message.Schema.FAILED_TO_CREATE_TYPE_DUE_TO_DUPLICATE, encoding.label, label
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
        attributeType: AttributeTypeState, overriddenType: AttributeTypeState?, isKey: Boolean, onSuccess: () -> Unit
    ) = schemaSrv.mayRunWriteTxAsync { tx ->
        try {
            overriddenType?.let {
                conceptType.asRemote(tx).setOwns(attributeType.conceptType, overriddenType.conceptType, isKey)
            } ?: conceptType.asRemote(tx).setOwns(attributeType.conceptType, isKey)
            loadOwnsAttributeTypes()
            onSuccess()
        } catch (e: Exception) {
            notifications.userError(
                LOGGER, FAILED_TO_DEFINE_OWN_ATTRIBUTE_TYPE,
                encoding.label, name, attributeType.name, e.message ?: UNKNOWN
            )
        }
    }

    fun tryUndefineOwnsAttributeType(attributeType: AttributeTypeState) {
        // TODO
    }

    fun tryDefinePlaysRoleType(
        roleType: RoleTypeState, overriddenType: RoleTypeState?, onSuccess: () -> Unit
    ) = schemaSrv.mayRunWriteTxAsync { tx ->
        try {
            overriddenType?.let {
                conceptType.asRemote(tx).setPlays(roleType.conceptType, it.conceptType)
            } ?: conceptType.asRemote(tx).setPlays(roleType.conceptType)
            loadPlaysRoleTypes()
            onSuccess()
        } catch (e: Exception) {
            notifications.userError(
                LOGGER, FAILED_TO_DEFINE_PLAY_ROLE_TYPE, encoding.label, name, roleType.name, e.message ?: UNKNOWN
            )
        }
    }

    fun tryUndefinePlaysRoleType(roleType: RoleTypeState) = schemaSrv.mayRunWriteTxAsync { tx ->
        try {
            conceptType.asRemote(tx).unsetPlays(roleType.conceptType)
            loadPlaysRoleTypes()
        } catch (e: Exception) {
            notifications.userError(
                LOGGER, FAILED_TO_UNDEFINE_PLAYS_ROLE_TYPE,
                encoding.label, name, roleType.scopedName, e.message ?: UNKNOWN
            )
        }
    }

    fun initiateDelete() = schemaSrv.confirmation.submit(
        title = Label.CONFIRM_TYPE_DELETION,
        message = Sentence.CONFIRM_TYPE_DELETION.format(encoding.label, name),
        onConfirm = { this.tryDelete() }
    )

    override fun tryDelete() = schemaSrv.mayRunWriteTxAsync { tx ->
        try {
            conceptType.asRemote(tx).delete()
            purge()
            schemaSrv.execOnTypesUpdated()
        } catch (e: Exception) {
            notifications.userError(
                LOGGER, FAILED_TO_DELETE_TYPE, encoding.label, e.message ?: UNKNOWN
            )
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