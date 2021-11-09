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

package com.vaticle.typedb.studio.data

import com.vaticle.typedb.client.api.concept.thing.Attribute
import com.vaticle.typedb.client.api.concept.type.AttributeType
import java.time.format.DateTimeFormatter

fun Attribute<*>.valueString(): String = when {
    isDateTime -> asDateTime().value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    else -> value.toString()
}

fun AttributeType.ValueType.schemaString(): String = when (this) {
    AttributeType.ValueType.BOOLEAN -> "boolean"
    AttributeType.ValueType.LONG -> "long"
    AttributeType.ValueType.DOUBLE -> "double"
    AttributeType.ValueType.STRING -> "string"
    AttributeType.ValueType.DATETIME -> "datetime"
    AttributeType.ValueType.OBJECT -> "object"
}
