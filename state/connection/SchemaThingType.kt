/*
 * Copyright (C) 2021 Vaticle
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

package com.vaticle.typedb.studio.state.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.studio.state.common.Navigable
import kotlin.streams.toList

open class SchemaThingType(
    private val concept: ThingType,
    override val parent: SchemaThingType?,
    private val session: SessionState,
    isExpandableInit: Boolean,
) : Navigable.Item<SchemaThingType> {

    val isEntityType get() = concept.isEntityType
    val isRelationType get() = concept.isRelationType
    val isAttributeType get() = concept.isAttributeType
    override val name: String get() = concept.label.name()
    override val info: String? = null
    override val isBulkExpandable: Boolean = true
    override var isExpandable: Boolean by mutableStateOf(isExpandableInit)
    override var entries: List<SchemaThingType> = emptyList()

    override fun reloadEntries() {
        // TODO: find a more efficient way than opening a new transaction for each type
        session.transaction()?.let {
            reloadEntries(it)
            it.close()
        }
    }

    private fun reloadEntries(transaction: TypeDBTransaction) {
        val new = concept.asRemote(transaction).subtypesExplicit.toList().toSet()
        val old = entries.map { it.concept }.toSet()
        if (new != old) {
            val deleted = old - new
            val added = new - old
            val oldConcepts = entries.filter { !deleted.contains(it.concept) }
            val newConcepts = added.map { schemaThingTypeOf(it, transaction) }
            entries = (oldConcepts + newConcepts).sorted()
            isExpandable = entries.isNotEmpty()
        }
    }

    private fun schemaThingTypeOf(concept: ThingType, transaction: TypeDBTransaction): SchemaThingType {
        val isExpandable = concept.asRemote(transaction).subtypesExplicit.findAny().isPresent
        return SchemaThingType(concept, this, session, isExpandable)
    }

    override fun compareTo(other: Navigable.Item<SchemaThingType>): Int {
        return this.name.compareTo(other.name, ignoreCase = true)
    }

    override fun toString(): String {
        return "Schema ThingType: $concept"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SchemaThingType
        return this.concept == other.concept
    }

    override fun hashCode(): Int {
        return concept.hashCode()
    }

    class Root constructor(
        concept: ThingType, session: SessionState
    ) : SchemaThingType(concept, null, session, true), Navigable.Container<SchemaThingType> {
        override var isExpandable: Boolean = true
    }
}