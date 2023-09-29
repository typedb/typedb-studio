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

import com.vaticle.typedb.driver.api.TypeDBTransaction
import com.vaticle.typedb.driver.api.concept.Concept.Transitivity.EXPLICIT
import com.vaticle.typedb.driver.api.concept.type.EntityType
import com.vaticle.typedb.driver.api.concept.type.Type
import kotlin.streams.toList

class EntityTypeState internal constructor(
    conceptType: EntityType,
    supertype: EntityTypeState?,
    schemaSrv: SchemaService
) : ThingTypeState<EntityType, EntityTypeState>(conceptType, supertype, Encoding.ENTITY_TYPE, schemaSrv) {

    override val parent: EntityTypeState? get() = supertype

    override fun isSameEncoding(conceptType: Type) = conceptType.isEntityType
    override fun asSameEncoding(conceptType: Type) = conceptType.asEntityType()!!
    override fun fetchSameEncoding(tx: TypeDBTransaction, label: String) = tx.concepts().getEntityType(label)
    override fun typeStateOf(type: EntityType) = schemaSrv.typeStateOf(type)

    override fun requestSubtypesExplicit() = schemaSrv.mayRunReadTx { tx ->
        conceptType.getSubtypes(tx, EXPLICIT).toList()
    }

    override fun initiateCreateSubtype(onSuccess: () -> Unit) =
        schemaSrv.createEntityTypeDialog.open(this, onSuccess)

    override fun tryCreateSubtype(
        label: String, isAbstract: Boolean
    ) = tryCreateSubtype(label, schemaSrv.createEntityTypeDialog) { tx ->
        val type = tx.concepts().putEntityType(label)
        if (isAbstract || !isRoot) {
            if (isAbstract) type.setAbstract(tx)
            if (!isRoot) type.setSupertype(tx, conceptType)
        }
    }

    override fun initiateChangeSupertype() = schemaSrv.changeEntitySupertypeDialog.open(this) {
        schemaSrv.execOnTypesUpdated()
        loadConstraintsAsync()
    }

    override fun tryChangeSupertype(
        supertypeState: EntityTypeState
    ) = super.tryChangeSupertype(schemaSrv.changeEntitySupertypeDialog) {
        conceptType.setSupertype(it, supertypeState.conceptType)
    }

    override fun toString(): String = "TypeState.Entity: $conceptType"
}
