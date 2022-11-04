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
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.concept.type.Type
import kotlin.streams.toList
import mu.KotlinLogging

class AttributeTypeState internal constructor(
    conceptType: AttributeType, supertype: AttributeTypeState?, schemaSrv: SchemaService
) : ThingTypeState<AttributeType, AttributeTypeState>(conceptType, supertype, Encoding.ATTRIBUTE_TYPE, schemaSrv) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override val info get() = valueType?.name?.lowercase()
    override val parent: AttributeTypeState? get() = supertype

    val valueType: AttributeType.ValueType? = if (!conceptType.isRoot) conceptType.valueType else null
    val isKeyable: Boolean get() = conceptType.valueType.isKeyable
    var ownerTypeProperties: List<OwnerTypeProperties> by mutableStateOf(listOf())
    val ownerTypes get() = ownerTypeProperties.map { it.ownerType }
    val ownerTypesExplicit get() = ownerTypeProperties.filter { !it.isInherited }.map { it.ownerType }

    override fun isSameEncoding(conceptType: Type) = conceptType.isAttributeType
    override fun asSameEncoding(conceptType: Type) = conceptType.asAttributeType()!!
    override fun typeStateOf(type: AttributeType) = schemaSrv.typeStateOf(type)

    override fun requestSubtypesExplicit() = schemaSrv.mayRunReadTx {
        conceptType.asRemote(it).subtypesExplicit.toList()
    }

    override fun loadOtherConstraints() {
        super.loadOtherConstraints()
        loadOwnerTypes()
    }

    fun loadOwnerTypes() {
        val loaded = mutableSetOf<ThingType>()
        val properties = mutableListOf<OwnerTypeProperties>()

        fun load(ownerTypeConcept: ThingType, isKey: Boolean, isInherited: Boolean) {
            loaded.add(ownerTypeConcept)
            schemaSrv.typeStateOf(ownerTypeConcept.asThingType())?.let { ownerType ->
                val extendedType = if (isInherited) {
                    ownerType.loadSupertypes()
                    ownerType.supertypes.toSet().intersect(
                        properties.filter { !it.isInherited }.map { it.ownerType }.toSet()
                    ).firstOrNull()
                } else null
                properties.add(OwnerTypeProperties(ownerType, extendedType, isInherited, isKey))
            }
        }

        schemaSrv.mayRunReadTx { tx ->
            val typeTx = conceptType.asRemote(tx)
            typeTx.getOwnersExplicit(true).forEach {
                load(it, isKey = true, isInherited = false)
            }
            typeTx.getOwnersExplicit(false).filter { !loaded.contains(it) }.forEach {
                load(it, isKey = false, isInherited = false)
            }
            typeTx.getOwners(true).filter { !loaded.contains(it) }.forEach {
                load(it, isKey = true, isInherited = true)
            }
            typeTx.getOwners(false).filter { !loaded.contains(it) }.forEach {
                load(it, isKey = false, isInherited = true)
            }
        }

        ownerTypeProperties = properties
    }

    override fun initiateCreateSubtype(onSuccess: () -> Unit) =
        schemaSrv.createAttributeTypeDialog.open(this, onSuccess)

    override fun tryCreateSubtype(label: String, isAbstract: Boolean) = tryCreateSubtype(
        label, isAbstract, conceptType.valueType
    )

    fun tryCreateSubtype(
        label: String, isAbstract: Boolean, valueType: AttributeType.ValueType
    ) = tryCreateSubtype(label, schemaSrv.createAttributeTypeDialog) { tx ->
        val type = tx.concepts().putAttributeType(label, valueType)
        if (isAbstract || !isRoot) {
            val typeTx = type.asRemote(tx)
            if (isAbstract) typeTx.setAbstract()
            if (!isRoot) typeTx.setSupertype(conceptType)
        }
    }

    override fun initiateChangeSupertype() = schemaSrv.changeAttributeSupertypeDialog.open(this) {
        schemaSrv.execOnTypesUpdated()
        loadConstraintsAsync()
    }

    override fun tryChangeSupertype(
        supertypeState: AttributeTypeState
    ) = super.tryChangeSupertype(schemaSrv.changeAttributeSupertypeDialog) {
        conceptType.asRemote(it).setSupertype(supertypeState.conceptType)
    }

    override fun toString(): String = "TypeState.Attribute: $conceptType"
}