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

package com.vaticle.typedb.studio.view.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.thing.Attribute
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.theme.Color
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.material.BrowserGroup
import com.vaticle.typedb.studio.view.material.Form
import com.vaticle.typedb.studio.view.material.Icon
import java.time.format.DateTimeFormatter

class ConceptPreview constructor(
    private val graphArea: GraphArea,
    order: Int,
    isOpen: Boolean
) : BrowserGroup.Browser(isOpen, order) {

    override val label: String = Label.PREVIEW
    override val icon: Icon.Code = Icon.Code.EYE
    override val isActive: Boolean = true
    override var buttons: List<Form.IconButtonArg> = emptyList()

    private val titleSectionPadding = 10.dp

    data class Property(val key: String, val value: String)

    companion object {
        private val MESSAGE_PADDING = 20.dp

        fun propertiesOf(concept: Concept): List<Property> {
            return listOfNotNull(
                if (concept is Thing) Label.INTERNAL_ID to concept.iid else null,
                if (concept is Attribute<*>) Label.VALUE to concept.valueString() else null,
            )
        }

        private infix fun String.to(value: String): Property {
            return Property(this, value)
        }

        private fun Attribute<*>.valueString(): String = when {
            isDateTime -> asDateTime().value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            else -> value.toString()
        }
    }

    @Composable
    override fun Content() {
        val focusedVertex = graphArea.interactions.focusedVertex
        if (focusedVertex == null) SelectVertexMessage()
        else Column(Modifier.fillMaxSize().background(Theme.studio.backgroundMedium)) {
            TitleSection(focusedVertex.concept)
            if (propertiesOf(focusedVertex.concept).isNotEmpty()) Table(focusedVertex.concept)
        }
    }

    @Composable
    private fun SelectVertexMessage() {
        Box(
            modifier = Modifier.fillMaxSize().background(Theme.studio.backgroundLight).padding(MESSAGE_PADDING),
            contentAlignment = Alignment.Center
        ) { Form.Text(Label.GRAPH_CONCEPT_PREVIEW_PLACEHOLDER, align = TextAlign.Center, softWrap = true) }
    }

    // TODO: copied from TypePage.kt on 23/05/2022
    @Composable
    private fun TitleSection(concept: Concept) {
        val type = if (concept is Type) concept else concept.asThing().type
        Box(Modifier.padding(titleSectionPadding)) {
            Form.TextBox(text = displayName(type), leadingIcon = Vertex.Type.typeIcon(type))
        }
    }

    @Composable
    private fun displayName(type: Type): AnnotatedString = displayName(type, Theme.studio.onPrimary)

    private fun displayName(type: Type, baseFontColor: androidx.compose.ui.graphics.Color): AnnotatedString {
        return buildAnnotatedString {
            append(type.label.scopedName())
            if (type is AttributeType) type.valueType?.let { valueType ->
                append(" ")
                withStyle(SpanStyle(baseFontColor.copy(Color.FADED_OPACITY))) {
                    append("(${valueType.name.lowercase()})")
                }
            }
        }
    }

    @Composable
    private fun Table(concept: Concept) {
        com.vaticle.typedb.studio.view.material.Table.Layout(
            items = propertiesOf(concept),
            modifier = Modifier.fillMaxWidth()
                .height(com.vaticle.typedb.studio.view.material.Table.ROW_HEIGHT * (propertiesOf(concept).size + 1)),
            columns = listOf(
                com.vaticle.typedb.studio.view.material.Table.Column(
                    Label.PROPERTY,
                    Alignment.CenterStart,
                    size = Either.first(1f)
                ) {
                    Form.Text(it.key, fontWeight = FontWeight.Bold)
                },
                com.vaticle.typedb.studio.view.material.Table.Column(
                    Label.VALUE,
                    Alignment.CenterStart,
                    size = Either.first(2f)
                ) {
                    Form.SelectableText(it.value, singleLine = true)
                }
            )
        )
    }
}