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

package com.vaticle.typedb.studio.framework.material

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.vaticle.typedb.client.api.concept.thing.Attribute
import com.vaticle.typedb.client.api.concept.thing.Relation
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.AttributeType.ValueType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.state.StudioState
import java.time.format.DateTimeFormatter

object Concept {

    fun conceptIcon(concept: com.vaticle.typedb.client.api.concept.Concept) {
        val graphTheme = StudioState.preference.graphTheme
        when (concept) {
            is Relation, is RelationType -> Form.IconArg(Icon.RELATION) { graphTheme.vertex.relationType }
            is Attribute<*>, is AttributeType -> Form.IconArg(Icon.ATTRIBUTE) { graphTheme.vertex.attributeType }
            is ThingType -> Form.IconArg(Icon.THING) { graphTheme.vertex.entityType }
            else -> throw IllegalArgumentException("Type icon not defined for concept: $concept")
        }
    }

    @Composable
    fun ConceptDetailedLabel(
        concept: com.vaticle.typedb.client.api.concept.Concept,
        baseFontColor: Color = Theme.studio.onPrimary
    ): AnnotatedString {
        val type = if (concept is Thing) concept.type else (concept.asType())
        val primary = type.label.scopedName()
        val secondary = if (type is AttributeType) valueTypeString(type.valueType) else null
        return annotatedString(primary, secondary, baseFontColor)
    }

    private fun annotatedString(primary: String, secondary: String? = null, baseFontColor: Color): AnnotatedString {
        return buildAnnotatedString {
            append(primary)
            secondary?.let {
                append(" ")
                withStyle(SpanStyle(baseFontColor.copy(com.vaticle.typedb.studio.framework.common.theme.Color.FADED_OPACITY))) {
                    append(
                        "(${it})"
                    )
                }
            }
        }
    }

    fun attributeValueString(attribute: Attribute<*>) = when (attribute) {
        is Attribute.DateTime -> attribute.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        else -> attribute.value.toString()
    }

    fun valueTypeString(valueType: ValueType) = valueType.name.lowercase()
}
