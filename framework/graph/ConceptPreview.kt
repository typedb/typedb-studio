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

package com.vaticle.typedb.studio.framework.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.thing.Attribute
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Browsers
import com.vaticle.typedb.studio.framework.material.ConceptDisplay
import com.vaticle.typedb.studio.framework.material.ConceptDisplay.attributeValue
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Table
import com.vaticle.typedb.studio.service.common.util.Label

class ConceptPreview constructor(
    private val graphArea: GraphArea,
    order: Int,
    isOpen: Boolean
) : Browsers.Browser(isOpen, order) {

    override val label: String = Label.PREVIEW
    override val icon: Icon = Icon.PREVIEW
    override val isActive: Boolean = true
    override var buttons: List<Form.IconButtonArg> = emptyList()

    companion object {
        private val MESSAGE_PADDING = 20.dp
    }

    @Composable
    override fun Content() {
        val focusedVertex = graphArea.interactions.focusedVertex
        if (focusedVertex == null) SelectVertexMessage()
        else Column(Modifier.fillMaxSize().background(Theme.studio.backgroundMedium)) {
            Table(focusedVertex.concept)
        }
    }

    @Composable
    private fun SelectVertexMessage() {
        Box(
            modifier = Modifier.fillMaxSize().background(Theme.studio.backgroundLight).padding(MESSAGE_PADDING),
            contentAlignment = Alignment.Center
        ) { Form.Text(Label.GRAPH_CONCEPT_PREVIEW_PLACEHOLDER, align = TextAlign.Center, softWrap = true) }
    }

    @Composable
    private fun Table(concept: Concept) {
        Table.Layout(
            items = propertiesOf(concept),
            modifier = Modifier.fillMaxWidth().height(Table.ROW_HEIGHT * propertiesOf(concept).size),
            showHeader = false,
            columns = listOf(
                Table.Column(header = null, contentAlignment = Alignment.CenterStart, size = Either.first(1f)) {
                    it.layout.Key()
                },
                Table.Column(header = null, contentAlignment = Alignment.CenterStart, size = Either.first(2f)) {
                    it.layout.Value()
                }
            )
        )
    }

    @Composable
    private fun ConceptTypePreview(concept: Concept) {
        val type = if (concept is Type) concept else concept.asThing().type
        Row(verticalAlignment = Alignment.CenterVertically) {
            ConceptDisplay.icon(type).let { Icon.Render(it.icon, it.color()) }
            Form.ButtonSpacer()
            Form.Text(ConceptDisplay.TypeLabelWithDetails(type))
        }
    }

    private sealed class Property {
        abstract val layout: Layout

        class Layout(private val key: kotlin.String, private val valueView: @Composable () -> Unit) {
            @Composable
            fun Key() {
                Form.Text(key, fontWeight = FontWeight.Bold)
            }

            @Composable
            fun Value() {
                valueView()
            }
        }

        class Generic(key: kotlin.String, val valueView: @Composable () -> Unit) : Property() {
            override val layout = Layout(key) { valueView() }
        }

        class String(key: kotlin.String, val value: kotlin.String) : Property() {
            override val layout = Layout(key) { Form.SelectableText(value, singleLine = true) }
        }
    }

    private fun propertiesOf(concept: Concept): List<Property> {
        return listOfNotNull(
            Property.Generic(Label.TYPE) { ConceptTypePreview(concept) },
            if (concept is Thing) Property.String(Label.INTERNAL_ID, concept.iid) else null,
            if (concept is Attribute<*>) Property.String(Label.VALUE, attributeValue(concept)) else null,
        )
    }
}