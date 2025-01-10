/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.schema

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.typedb.driver.api.concept.Concept.Transitivity.EXPLICIT
import com.typedb.driver.api.concept.type.RoleType
import com.typedb.driver.api.concept.type.ThingType
import com.typedb.driver.api.concept.type.Type
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.streams.toList

class RoleTypeState constructor(
    val relationType: RelationTypeState,
    conceptType: RoleType,
    supertype: RoleTypeState?,
    schemaSrv: SchemaService
) : TypeState<RoleType, RoleTypeState>(conceptType, supertype, Encoding.ROLE_TYPE, schemaSrv) {

    interface RoleTypeProperties : OverridingTypeProperties<RoleTypeState> {
        val roleType: RoleTypeState
        override val type: RoleTypeState get() = roleType
        override val overriddenType: RoleTypeState?
        val extendedType: ThingTypeState<*, *>?
        val isInherited: Boolean
    }

    data class RelatedRoleTypeProperties constructor(
        override val roleType: RoleTypeState,
        override val overriddenType: RoleTypeState?,
        override val extendedType: RelationTypeState?,
        override val isInherited: Boolean,
    ) : RoleTypeProperties

    data class PlayedRoleTypeProperties constructor(
        override val roleType: RoleTypeState,
        override val overriddenType: RoleTypeState?,
        override val extendedType: ThingTypeState<*, *>?,
        override val isInherited: Boolean,
        val canBeUndefined: Boolean,
    ) : RoleTypeProperties

    data class PlayerTypeProperties constructor(
        val playerType: ThingTypeState<*, *>,
        val isInherited: Boolean,
    )

    val scopedName get() = relationType.name + ":" + name
    var playerTypeProperties: List<PlayerTypeProperties> by mutableStateOf(emptyList())
    val playerTypes get() = playerTypeProperties.map { it.playerType }
    val playerTypesExplicit get() = playerTypeProperties.filter { !it.isInherited }.map { it.playerType }
    var hasPlayerInstancesExplicit: Boolean by mutableStateOf(false)
    override val canBeDeleted: Boolean get() = !hasSubtypes && !hasPlayerInstancesExplicit
    override val canBeAbstract get() = !hasPlayerInstancesExplicit
    private val loadedPlayerTypePropsAtomic = AtomicBoolean(false)

    override fun loadInheritables() {}
    override fun isSameEncoding(conceptType: Type) = conceptType.isRoleType
    override fun asSameEncoding(conceptType: Type) = conceptType.asRoleType()!!
    override fun typeStateOf(type: RoleType) = schemaSrv.typeStateOf(type)

    override fun updateConceptType(label: String) = schemaSrv.mayRunReadTx { tx ->
        val newConceptType = relationType.conceptType.getRelates(tx, label).resolve()!!
        isAbstract = newConceptType.isAbstract
        name = newConceptType.label.name()
        conceptType = newConceptType // we need to update the mutable state last
    } ?: Unit

    override fun requestSubtypesExplicit() = schemaSrv.mayRunReadTx { tx ->
        conceptType.getSubtypes(tx, EXPLICIT).toList()
    }

    override fun loadConstraints() {
        loadHasPlayerInstances()
    }

    override fun loadDependencies() {
        loadHasPlayerInstances()
    }

    private fun loadHasPlayerInstances() = schemaSrv.mayRunReadTx { tx ->
        hasPlayerInstancesExplicit = conceptType.getPlayerInstances(tx, EXPLICIT).findAny().isPresent
    }

    fun loadPlayerTypes() {
        val loaded = mutableSetOf<ThingType>()
        val properties = mutableListOf<PlayerTypeProperties>()

        fun load(playerType: ThingType, isInherited: Boolean) {
            loaded.add(playerType)
            schemaSrv.typeStateOf(playerType.asThingType())?.let {
                properties.add(PlayerTypeProperties(it, isInherited))
            }
        }

        schemaSrv.mayRunReadTx { tx ->
            if (!loadedPlayerTypePropsAtomic.get()) {
                loadedPlayerTypePropsAtomic.set(true)
                conceptType.getPlayerTypes(tx, EXPLICIT).forEach { load(it, isInherited = false) }
                conceptType.getPlayerTypes(tx).filter { !loaded.contains(it) }.forEach { load(it, isInherited = true) }
                playerTypeProperties = properties
            }
        }
    }

    override fun resetLoadedConnectedTypes() {
        loadedPlayerTypePropsAtomic.set(false)
        playerTypeProperties = emptyList()
    }

    override fun toString(): String = "TypeState.Role: $conceptType"
}
