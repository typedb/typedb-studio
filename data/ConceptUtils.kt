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
