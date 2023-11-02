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
import com.vaticle.typedb.driver.api.TypeDBTransaction
import com.vaticle.typedb.driver.api.concept.Concept.Transitivity.EXPLICIT
import com.vaticle.typedb.driver.api.concept.type.AttributeType
import com.vaticle.typedb.driver.api.concept.type.ThingType
import com.vaticle.typedb.driver.api.concept.type.ThingType.Annotation.key
import com.vaticle.typedb.driver.api.concept.type.Type
import com.vaticle.typedb.driver.api.concept.value.Value
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.streams.toList

class AttributeTypeState internal constructor(
    conceptType: AttributeType,
    supertype: AttributeTypeState?,
    schemaSrv: SchemaService
) : ThingTypeState<AttributeType, AttributeTypeState>(conceptType, supertype, Encoding.ATTRIBUTE_TYPE, schemaSrv) {

    data class OwnedAttTypeProperties constructor(
        val attributeType: AttributeTypeState,
        override val overriddenType: AttributeTypeState?,
        val extendedType: ThingTypeState<*, *>?,
        val isInherited: Boolean,
        val isKey: Boolean,
        val canBeUndefined: Boolean,
    ) : OverridingTypeProperties<AttributeTypeState> {
        override val type: AttributeTypeState get() = attributeType
    }

    data class AttTypeOwnerProperties constructor(
        val ownerType: ThingTypeState<*, *>,
        val extendedType: ThingTypeState<*, *>?,
        val isInherited: Boolean,
        val isKey: Boolean,
    )

    override val info get() = valueType?.name?.lowercase()
    override val parent: AttributeTypeState? get() = supertype

    val valueType: Value.Type? = if (!conceptType.isRoot) conceptType.valueType else null
    var ownerTypeProperties: List<AttTypeOwnerProperties> by mutableStateOf(listOf())
    val ownerTypes get() = ownerTypeProperties.map { it.ownerType }
    val ownerTypesExplicit get() = ownerTypeProperties.filter { !it.isInherited }.map { it.ownerType }

    private val loadedOwnerTypePropsAtomic = AtomicBoolean(false)

    override fun isSameEncoding(conceptType: Type) = conceptType.isAttributeType
    override fun asSameEncoding(conceptType: Type) = conceptType.asAttributeType()!!
    override fun fetchSameEncoding(tx: TypeDBTransaction, label: String) = tx.concepts().getAttributeType(label).resolve()
    override fun typeStateOf(type: AttributeType) = schemaSrv.typeStateOf(type)

    override fun requestSubtypesExplicit() = schemaSrv.mayRunReadTx { tx ->
        conceptType.getSubtypes(tx, EXPLICIT).toList()
    }

    override fun loadOtherConstraints() {
        super.loadOtherConstraints()
        loadOwnerTypes()
    }

    fun loadOwnerTypes() {
        val loaded = mutableSetOf<ThingType>()
        val properties = mutableListOf<AttTypeOwnerProperties>()

        fun load(ownerTypeConcept: ThingType, isKey: Boolean, isInherited: Boolean) {
            loaded.add(ownerTypeConcept)
            schemaSrv.typeStateOf(ownerTypeConcept.asThingType())?.let { ownerType ->
                val extendedType = if (isInherited) {
                    ownerType.loadSupertypes()
                    ownerType.supertypes.toSet().intersect(
                        properties.filter { !it.isInherited }.map { it.ownerType }.toSet()
                    ).firstOrNull()
                } else null
                properties.add(AttTypeOwnerProperties(ownerType, extendedType, isInherited, isKey))
            }
        }

        schemaSrv.mayRunReadTx { tx ->
            if (!loadedOwnerTypePropsAtomic.get()) {
                loadedOwnerTypePropsAtomic.set(true)
                conceptType.getOwners(tx, setOf(key()), EXPLICIT).forEach {
                    load(it, isKey = true, isInherited = false)
                }
                conceptType.getOwners(tx, EXPLICIT).filter { !loaded.contains(it) }.forEach {
                    load(it, isKey = false, isInherited = false)
                }
                conceptType.getOwners(tx, setOf(key())).filter { !loaded.contains(it) }.forEach {
                    load(it, isKey = true, isInherited = true)
                }
                conceptType.getOwners(tx, EXPLICIT).filter { !loaded.contains(it) }.forEach {
                    load(it, isKey = false, isInherited = true)
                }
                ownerTypeProperties = properties
            }
        }
    }

    override fun resetLoadedConnectedTypes() {
        loadedOwnerTypePropsAtomic.set(false)
        ownerTypeProperties = emptyList()
        super.resetLoadedConnectedTypes()
    }

    override fun initiateCreateSubtype(onSuccess: () -> Unit) =
        schemaSrv.createAttributeTypeDialog.open(this, onSuccess)

    override fun tryCreateSubtype(label: String, isAbstract: Boolean) = tryCreateSubtype(
        label, isAbstract, conceptType.valueType
    )

    fun tryCreateSubtype(
        label: String, isAbstract: Boolean, valueType: Value.Type
    ) = tryCreateSubtype(label, schemaSrv.createAttributeTypeDialog) { tx ->
        val type = tx.concepts().putAttributeType(label, valueType).resolve()
        if (isAbstract || !isRoot) {
            if (isAbstract) type.setAbstract(tx)
            if (!isRoot) type.setSupertype(tx, conceptType)
        }
    }

    override fun initiateChangeSupertype() = schemaSrv.changeAttributeSupertypeDialog.open(this) {
        schemaSrv.execOnTypesUpdated()
        loadConstraintsAsync()
    }

    override fun tryChangeSupertype(
        supertypeState: AttributeTypeState
    ) = super.tryChangeSupertype(schemaSrv.changeAttributeSupertypeDialog) {
        conceptType.setSupertype(it, supertypeState.conceptType)
    }

    override fun toString(): String = "TypeState.Attribute: $conceptType"
}
