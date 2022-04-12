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

open class SchemaType(
    private val concept: ThingType,
    override val parent: SchemaType?,
    private val session: SessionState,
    isExpandableInit: Boolean,
) : Navigable.Item<SchemaType> {

    val isEntityType get() = concept.isEntityType
    val isRelationType get() = concept.isRelationType
    val isAttributeType get() = concept.isAttributeType
    override val name: String get() = concept.label.name()
    override val info: String? = null
    override val isBulkExpandable: Boolean = true
    override var isExpandable: Boolean by mutableStateOf(isExpandableInit)
    override var entries: List<SchemaType> = emptyList()

    override fun reloadEntries() {
        val tx = session.schemaTypeTx()
        entries = concept.asRemote(tx).subtypesExplicit
            .map { schemaTypeOf(it, tx) }
            .sorted().toList()
        isExpandable = entries.isNotEmpty()
    }

    private fun schemaTypeOf(concept: ThingType, transaction: TypeDBTransaction): SchemaType {
        val isExpandable = concept.asRemote(transaction).subtypesExplicit.findAny().isPresent
        return SchemaType(concept, this, session, isExpandable)
    }

    override fun compareTo(other: Navigable.Item<SchemaType>): Int {
        return this.name.compareTo(other.name, ignoreCase = true)
    }

    override fun toString(): String {
        return "SchemaType: $concept"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SchemaType
        return this.concept == other.concept && this.isExpandable == other.isExpandable
    }

    override fun hashCode(): Int {
        return concept.hashCode()
    }

    class Root constructor(
        concept: ThingType, session: SessionState
    ) : SchemaType(concept, null, session, true), Navigable.Container<SchemaType> {
        override var isExpandable: Boolean = true
    }
}