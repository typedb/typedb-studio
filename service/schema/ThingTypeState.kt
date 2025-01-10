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
import com.typedb.studio.service.common.util.Message
import com.typedb.studio.service.common.util.Message.Companion.UNKNOWN
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_CHANGE_ABSTRACT
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_CHANGE_OVERRIDDEN_OWNED_ATT_TYPE
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_CHANGE_OVERRIDDEN_OWNED_ATT_TYPE_TO_REMOVE
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_CHANGE_OVERRIDDEN_PLAYED_ROLE_TYPE
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_CHANGE_OVERRIDDEN_PLAYED_ROLE_TYPE_TO_REMOVE
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_CHANGE_SUPERTYPE
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_CREATE_TYPE
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_DEFINE_OWN_ATT_TYPE
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_DEFINE_PLAY_ROLE_TYPE
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_DELETE_TYPE
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_LOAD_TYPE
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_UNDEFINE_OWNED_ATT_TYPE
import com.typedb.studio.service.common.util.Message.Schema.Companion.FAILED_TO_UNDEFINE_PLAYED_ROLE_TYPE
import com.typedb.studio.service.common.util.Sentence
import com.typedb.studio.service.page.Navigable
import com.typedb.studio.service.page.Pageable
import com.typedb.driver.api.TypeDBTransaction
import com.typedb.driver.api.concept.Concept.Transitivity.EXPLICIT
import com.typedb.driver.api.concept.type.AttributeType
import com.typedb.driver.api.concept.type.RoleType
import com.typedb.driver.api.concept.type.ThingType
import com.typedb.driver.api.concept.type.ThingType.Annotation.key
import com.typedb.driver.common.exception.TypeDBDriverException
import com.typeql.lang.TypeQL
import com.typeql.lang.TypeQL.cVar
import com.typeql.lang.TypeQL.rel
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
        val onSchemaWrite = LinkedBlockingQueue<() -> Unit>()

        fun clear() {
            onReopen.clear()
            onClose.clear()
            onSubtypesUpdated.clear()
            onSchemaWrite.clear()
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    var ownedAttTypeProperties: List<AttributeTypeState.OwnedAttTypeProperties> by mutableStateOf(emptyList())
    val ownedAttTypes: List<AttributeTypeState> get() = ownedAttTypeProperties.map { it.attributeType }
    var playedRoleTypeProperties: List<RoleTypeState.PlayedRoleTypeProperties> by mutableStateOf(emptyList())
    val playedRoleTypes: List<RoleTypeState> get() = playedRoleTypeProperties.map { it.roleType }

    private val loadedOwnedAttTypePropsAtomic = AtomicBoolean(false)
    private val loadedPlayedRoleTypePropsAtomic = AtomicBoolean(false)

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
    override fun reloadEntries() {
        try {
            loadSubtypesExplicit()
        } catch (e: TypeDBDriverException) {
            notifications.userError(LOGGER, FAILED_TO_LOAD_TYPE, e.message ?: UNKNOWN)
            LOGGER.error { e.stackTraceToString() }
        }
    }

    override fun onReopen(function: (Pageable) -> Unit) = callbacks.onReopen.put(function)
    override fun onClose(function: (Pageable) -> Unit) = callbacks.onClose.put(function)
    override fun compareTo(other: Navigable<ThingTypeState<TT, TTS>>): Int = name.compareTo(other.name)

    abstract fun initiateCreateSubtype(onSuccess: () -> Unit)
    abstract fun tryCreateSubtype(label: String, isAbstract: Boolean)
    abstract fun initiateChangeSupertype()
    abstract fun tryChangeSupertype(supertypeState: TTS)

    fun onSubtypesUpdated(function: () -> Unit) = callbacks.onSubtypesUpdated.put(function)

    fun onSchemaWrite(function: () -> Unit) = callbacks.onSchemaWrite.put(function)

    fun removeSchemaWriteCallback(function: () -> Unit) {
        callbacks.onSchemaWrite.remove(function)
    }

    fun notifySchemaWrite() {
        callbacks.onSchemaWrite.forEach { it.invoke() }
    }

    protected abstract fun fetchSameEncoding(tx: TypeDBTransaction, label: String): TT?
    override fun updateConceptType(label: String) = schemaSrv.mayRunReadTx { tx ->
        val newConceptType = fetchSameEncoding(tx, label)!!
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

    fun overridableOwnedAttributeTypes(attributeType: AttributeTypeState) = attributeType.supertypes
        .intersect(supertype!!.ownedAttTypes.toSet())
        .sortedBy { it.name }

    fun overridablePlayedRoleTypes(roleType: RoleTypeState) = roleType.supertypes
        .intersect(supertype!!.playedRoleTypes.toSet())
        .sortedBy { it.scopedName }

    fun exportSyntaxAsync(onSuccess: (String) -> Unit) = coroutines.launchAndHandle(notifications, LOGGER) {
        schemaSrv.mayRunReadTx { tx -> conceptType.getSyntax(tx).resolve()?.let { onSuccess(it) } }
    }

    fun loadConstraintsAsync() = coroutines.launchAndHandle(notifications, LOGGER) {
        loadConstraints()
    }

    override fun loadConstraints() {
        schemaSrv.mayRunReadTx {
            try {
                loadSupertypes()
                loadOtherConstraints()
                loadSubtypesRecursively()
            } catch (e: TypeDBDriverException) {
                notifications.userError(LOGGER, FAILED_TO_LOAD_TYPE, e.message ?: UNKNOWN)
                LOGGER.error { e.stackTraceToString() }
            }
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
        loadOwnedAttributeTypes()
        loadPlayedRoleTypes()
    }

    open fun loadOtherConstraints() {
        loadHasInstancesExplicit()
        loadOwnedAttributeTypes()
        loadPlayedRoleTypes()
    }

    private fun loadHasInstancesExplicit() = schemaSrv.mayRunReadTx { tx ->
        hasInstancesExplicit = conceptType.getInstances(tx, EXPLICIT).findAny().isPresent
    }

    private fun loadOwnedAttributeTypes() {
        val loaded = mutableSetOf<AttributeType>()
        val properties = mutableListOf<AttributeTypeState.OwnedAttTypeProperties>()

        fun load(
                tx: TypeDBTransaction,
                attTypeConcept: AttributeType, isKey: Boolean, isInherited: Boolean
        ) {
            loaded.add(attTypeConcept)
            schemaSrv.typeStateOf(attTypeConcept)?.let { attType ->
                val overriddenType = conceptType.getOwnsOverridden(tx, attTypeConcept).resolve()?.let { schemaSrv.typeStateOf(it) }
                val inheritedType = when {
                    isInherited -> attType
                    else -> overriddenType
                }?.also { it.loadOwnerTypes() }
                val extendedType = inheritedType?.ownerTypesExplicit?.toSet()
                    ?.intersect(supertypes.toSet())?.firstOrNull()
                val canBeUndefined = !tx.query().get(
                    TypeQL.match(cVar("x").isa(conceptType.label.name()).has(attType.name, cVar("y"))).get()
                ).findFirst().isPresent
                properties.add(
                    AttributeTypeState.OwnedAttTypeProperties(
                        attType, overriddenType, extendedType, isInherited, isKey, canBeUndefined
                    )
                )
            }
        }

        schemaSrv.mayRunReadTx { tx ->
            if (!loadedOwnedAttTypePropsAtomic.get()) {
                loadedOwnedAttTypePropsAtomic.set(true)
                conceptType.getOwns(tx, setOf(key()), EXPLICIT).forEach {
                    load(tx = tx, attTypeConcept = it, isKey = true, isInherited = false)
                }
                conceptType.getOwns(tx, EXPLICIT).filter { !loaded.contains(it) }.forEach {
                    load(tx = tx, attTypeConcept = it, isKey = false, isInherited = false)
                }
                conceptType.getOwns(tx, setOf(key())).filter { !loaded.contains(it) }.forEach {
                    load(tx = tx, attTypeConcept = it, isKey = true, isInherited = true)
                }
                conceptType.getOwns(tx).filter { !loaded.contains(it) }.forEach {
                    load(tx = tx, attTypeConcept = it, isKey = false, isInherited = true)
                }
                ownedAttTypeProperties = properties
            }
        }
    }

    private fun loadPlayedRoleTypes() {
        val loaded = mutableSetOf<RoleType>()
        val properties = mutableListOf<RoleTypeState.PlayedRoleTypeProperties>()

        fun load(tx: TypeDBTransaction, roleTypeConcept: RoleType, isInherited: Boolean) {
            loaded.add(roleTypeConcept)
            schemaSrv.typeStateOf(roleTypeConcept)?.let { roleType ->
                roleType.loadConstraints()
                val overriddenType = conceptType.getPlaysOverridden(tx, roleTypeConcept).resolve()?.let { schemaSrv.typeStateOf(it) }
                val inheritedType = when {
                    isInherited -> roleType
                    else -> overriddenType
                }?.also { it.loadPlayerTypes() }
                val extendedType = inheritedType?.playerTypesExplicit?.toSet()
                    ?.intersect(supertypes.toSet())?.firstOrNull()
                val canBeUndefined = !tx.query().get(
                    TypeQL.match(
                        cVar("x").isa(conceptType.label.name()),
                        rel(roleTypeConcept.label.name(), cVar("x"))
                    ).get()
                ).findFirst().isPresent
                properties.add(
                    RoleTypeState.PlayedRoleTypeProperties(
                        roleType, overriddenType, extendedType, isInherited, canBeUndefined
                    )
                )
            }
        }

        schemaSrv.mayRunReadTx { tx ->
            if (!loadedPlayedRoleTypePropsAtomic.get()) {
                loadedPlayedRoleTypePropsAtomic.set(true)
                conceptType.getPlays(tx, EXPLICIT).forEach { load(tx, it, false) }
                conceptType.getPlays(tx).filter { !loaded.contains(it) }.forEach { load(tx, it, true) }
                playedRoleTypeProperties = properties
            }
        }
    }

    override fun resetLoadedConnectedTypes() {
        loadedPlayedRoleTypePropsAtomic.set(false)
        playedRoleTypeProperties = emptyList()
        loadedOwnedAttTypePropsAtomic.set(false)
        ownedAttTypeProperties = emptyList()
    }

    protected fun tryCreateSubtype(
        label: String, dialogState: SchemaService.TypeDialogState<*>, creatorFn: (TypeDBTransaction) -> Unit
    ) {
        if (schemaSrv.mayRunReadTx { tx -> fetchSameEncoding(tx, label) } != null) notifications.userError(
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

    protected fun tryChangeSupertype(
        dialogState: SchemaService.TypeDialogState<*>, function: (TypeDBTransaction) -> Unit
    ) = schemaSrv.mayRunWriteTxAsync {
        try {
            function(it)
            dialogState.onSuccess?.invoke()
            dialogState.close()
            loadConstraints()
        } catch (e: Exception) {
            notifications.userError(
                LOGGER, FAILED_TO_CHANGE_SUPERTYPE, encoding.label, conceptType.label, e.message ?: UNKNOWN
            )
        }
    }

    fun initiateChangeAbstract() = schemaSrv.changeAbstractDialog.open(this)

    fun tryChangeAbstract(isAbstract: Boolean) = schemaSrv.mayRunWriteTxAsync { tx ->
        try {
            conceptType.let { if (isAbstract) it.setAbstract(tx) else it.unsetAbstract(tx) }
            schemaSrv.changeAbstractDialog.close()
            updateConceptType()
        } catch (e: Exception) {
            notifications.userError(LOGGER, FAILED_TO_CHANGE_ABSTRACT, encoding.label, e.message ?: UNKNOWN)
        }
    }

    fun tryDefineOwnsAttributeType(
        attributeType: AttributeTypeState, overriddenType: AttributeTypeState?, isKey: Boolean, onSuccess: () -> Unit
    ) = schemaSrv.mayRunWriteTxAsync { tx ->
        val annotationSet = if (isKey) setOf(key()) else emptySet()
        try {
            overriddenType?.let {
                conceptType.setOwns(tx, attributeType.conceptType, overriddenType.conceptType, annotationSet)
            } ?: conceptType.setOwns(tx, attributeType.conceptType, annotationSet)
            loadOwnedAttributeTypes()
            onSuccess()
        } catch (e: Exception) {
            notifications.userError(
                LOGGER, FAILED_TO_DEFINE_OWN_ATT_TYPE,
                encoding.label, name, attributeType.name, e.message ?: UNKNOWN
            )
        }
    }

    fun tryUndefineOwnedAttributeType(attType: AttributeTypeState) = schemaSrv.mayRunWriteTxAsync { tx ->
        try {
            conceptType.unsetOwns(tx, attType.conceptType)
            loadOwnedAttributeTypes()
        } catch (e: Exception) {
            notifications.userError(
                LOGGER, FAILED_TO_UNDEFINE_OWNED_ATT_TYPE, encoding.label, name, attType.name, e.message ?: UNKNOWN
            )
        }
    }

    fun initiateChangeOverriddenOwnedAttributeType(
        props: AttributeTypeState.OwnedAttTypeProperties
    ) = schemaSrv.changeOverriddenOwnedAttributeTypeDialog.open(this, props)

    fun tryChangeOverriddenOwnedAttributeType(
        attType: AttributeTypeState, overriddenType: AttributeTypeState?
    ) = schemaSrv.mayRunWriteTxAsync { tx ->
        try {
            overriddenType?.let {
                conceptType.setOwns(tx, attType.conceptType, overriddenType.conceptType)
            } ?: conceptType.setOwns(tx, attType.conceptType)
            loadOwnedAttributeTypes()
            schemaSrv.changeOverriddenOwnedAttributeTypeDialog.close()
        } catch (e: Exception) {
            overriddenType?.let {
                notifications.userError(
                    LOGGER, FAILED_TO_CHANGE_OVERRIDDEN_OWNED_ATT_TYPE,
                    encoding.label, name, attType.name, overriddenType.name
                )
            } ?: notifications.userError(
                LOGGER, FAILED_TO_CHANGE_OVERRIDDEN_OWNED_ATT_TYPE_TO_REMOVE, encoding.label, name, attType.name
            )
        }
    }

    fun tryDefinePlaysRoleType(
        roleType: RoleTypeState, overriddenType: RoleTypeState?, onSuccess: () -> Unit
    ) = schemaSrv.mayRunWriteTxAsync { tx ->
        try {
            overriddenType?.let {
                conceptType.setPlays(tx, roleType.conceptType, it.conceptType)
            } ?: conceptType.setPlays(tx, roleType.conceptType)
            loadPlayedRoleTypes()
            onSuccess()
        } catch (e: Exception) {
            notifications.userError(
                LOGGER, FAILED_TO_DEFINE_PLAY_ROLE_TYPE, encoding.label, name, roleType.name, e.message ?: UNKNOWN
            )
        }
    }

    fun tryUndefinePlayedRoleType(roleType: RoleTypeState) = schemaSrv.mayRunWriteTxAsync { tx ->
        try {
            conceptType.unsetPlays(tx, roleType.conceptType)
            loadPlayedRoleTypes()
        } catch (e: Exception) {
            notifications.userError(
                LOGGER, FAILED_TO_UNDEFINE_PLAYED_ROLE_TYPE,
                encoding.label, name, roleType.scopedName, e.message ?: UNKNOWN
            )
        }
    }

    fun initiateChangeOverriddenPlayedRoleType(
        props: RoleTypeState.PlayedRoleTypeProperties
    ) = schemaSrv.changeOverriddenPlayedRoleTypeDialog.open(this, props)

    fun tryChangeOverriddenPlayedRoleType(
        roleType: RoleTypeState, overriddenType: RoleTypeState?
    ) = schemaSrv.mayRunWriteTxAsync { tx ->
        try {
            overriddenType?.let {
                conceptType.setPlays(tx, roleType.conceptType, overriddenType.conceptType)
            } ?: conceptType.setPlays(tx, roleType.conceptType)
            loadPlayedRoleTypes()
            schemaSrv.changeOverriddenPlayedRoleTypeDialog.close()
        } catch (e: Exception) {
            overriddenType?.let {
                notifications.userError(
                    LOGGER, FAILED_TO_CHANGE_OVERRIDDEN_PLAYED_ROLE_TYPE,
                    encoding.label, name, roleType.name, overriddenType.name
                )
            } ?: notifications.userError(
                LOGGER, FAILED_TO_CHANGE_OVERRIDDEN_PLAYED_ROLE_TYPE_TO_REMOVE, encoding.label, name, roleType.name
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
            conceptType.delete(tx)
            purge()
            schemaSrv.execOnTypesUpdated()
        } catch (e: Exception) {
            notifications.userError(LOGGER, FAILED_TO_DELETE_TYPE, encoding.label, e.message ?: UNKNOWN)
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
