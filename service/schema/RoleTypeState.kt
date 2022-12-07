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
import com.vaticle.typedb.client.api.concept.type.RoleType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.studio.service.common.atomic.AtomicBooleanState
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.streams.toList
import mu.KotlinLogging

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

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    val scopedName get() = relationType.name + ":" + name
    var playerTypeProperties: List<PlayerTypeProperties> by mutableStateOf(emptyList())
    val playerTypes get() = playerTypeProperties.map { it.playerType }
    val playerTypesExplicit get() = playerTypeProperties.filter { !it.isInherited }.map { it.playerType }
    var hasPlayerInstancesExplicit: Boolean by mutableStateOf(false)
    override val canBeDeleted: Boolean get() = !hasSubtypes && !hasPlayerInstancesExplicit
    override val canBeAbstract get() = !hasPlayerInstancesExplicit
    private val loadedPlayerTypePropsAtomic = AtomicBooleanState(false)

    override fun loadInheritables() {}
    override fun isSameEncoding(conceptType: Type) = conceptType.isRoleType
    override fun asSameEncoding(conceptType: Type) = conceptType.asRoleType()!!
    override fun typeStateOf(type: RoleType) = schemaSrv.typeStateOf(type)

    override fun updateConceptType(label: String) = schemaSrv.mayRunReadTx { tx ->
        val newConceptType = relationType.conceptType.asRemote(tx).getRelates(label)!!
        isAbstract = newConceptType.isAbstract
        name = newConceptType.label.name()
        conceptType = newConceptType // we need to update the mutable state last
    } ?: Unit

    override fun requestSubtypesExplicit() = schemaSrv.mayRunReadTx { tx ->
        conceptType.asRemote(tx).subtypesExplicit.toList()
    }

    fun loadConstraints() {
        loadHasPlayerInstances()
    }

    override fun loadDependencies() {
        loadHasPlayerInstances()
    }

    private fun loadHasPlayerInstances() = schemaSrv.mayRunReadTx { tx ->
        hasPlayerInstancesExplicit = conceptType.asRemote(tx).playerInstancesExplicit.findAny().isPresent
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
            val roleTypeTx = conceptType.asRemote(tx)
            if (!loadedPlayerTypePropsAtomic.state) {
                loadedPlayerTypePropsAtomic.set(true)
                roleTypeTx.playerTypesExplicit.forEach { load(it, isInherited = false) }
                roleTypeTx.playerTypes.filter { !loaded.contains(it) }.forEach { load(it, isInherited = true) }
                playerTypeProperties = properties
            }
        }
    }

    fun resetLoadedConnectedTypes() {
        loadedPlayerTypePropsAtomic.set(false)
        playerTypeProperties = emptyList()
    }

    override fun toString(): String = "TypeState.Role: $conceptType"
}