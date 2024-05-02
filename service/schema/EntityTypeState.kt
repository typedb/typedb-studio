/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
    override fun fetchSameEncoding(tx: TypeDBTransaction, label: String) = tx.concepts().getEntityType(label).resolve()
    override fun typeStateOf(type: EntityType) = schemaSrv.typeStateOf(type)

    override fun requestSubtypesExplicit() = schemaSrv.mayRunReadTx { tx ->
        conceptType.getSubtypes(tx, EXPLICIT).toList()
    }

    override fun initiateCreateSubtype(onSuccess: () -> Unit) =
        schemaSrv.createEntityTypeDialog.open(this, onSuccess)

    override fun tryCreateSubtype(
        label: String, isAbstract: Boolean
    ) = tryCreateSubtype(label, schemaSrv.createEntityTypeDialog) { tx ->
        val type = tx.concepts().putEntityType(label).resolve()
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
