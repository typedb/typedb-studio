/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.material

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.typedb.studio.framework.common.theme.Color.FADED_OPACITY
import com.typedb.studio.framework.common.theme.Theme
import com.typedb.studio.service.common.util.Label
import com.typedb.driver.api.concept.Concept
import com.typedb.driver.api.concept.instance.Attribute
import com.typedb.driver.api.concept.instance.Relation
import com.typedb.driver.api.concept.type.AttributeType
import com.typedb.driver.api.concept.type.RelationType
import com.typedb.driver.api.concept.type.Type
import java.time.format.DateTimeFormatter

object ConceptDisplay {

    fun iconOf(concept: Concept) = when (concept) {
        is Relation, is RelationType -> Form.IconArg(Icon.RELATION) { Theme.graph.vertex.relationType }
        is Attribute, is AttributeType -> Form.IconArg(Icon.ATTRIBUTE) { Theme.graph.vertex.attributeType }
        is Type -> Form.IconArg(Icon.THING) { Theme.graph.vertex.entityType }
        else -> throw IllegalArgumentException("Type icon not defined for concept: $concept")
    }

    @Composable
    fun TypeLabelWithDetails(
        concept: Type,
        isAbstract: Boolean = false,
        baseFontColor: Color = Theme.studio.onPrimary
    ): AnnotatedString {
        val details = mutableListOf<String>()
        if (concept is AttributeType) details.add(concept.tryGetValueType().get().lowercase())
        if (isAbstract) details.add(Label.ABSTRACT.lowercase())
        return buildAnnotatedString {
            append(concept.label)
            if (details.isNotEmpty()) details.joinToString(separator = ", ").let {
                append(" ")
                withStyle(SpanStyle(baseFontColor.copy(FADED_OPACITY))) { append("(${it})") }
            }
        }
    }

    fun attributeValue(attribute: Attribute) =
        if (attribute.value.isDatetime) attribute.value.datetime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        else attribute.value.toString()
}
