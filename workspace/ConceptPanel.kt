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

package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.data.schemaString
import com.vaticle.typedb.studio.data.valueString
import com.vaticle.typedb.studio.ui.elements.StudioTextField
import com.vaticle.typedb.studio.ui.elements.StudioTextFieldVariant
import com.vaticle.typedb.studio.visualiser.VertexState

@Composable
fun ConceptPanel(vertex: VertexState?, onCollapse: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Column {
            SidebarPanelHeader("${vertex?.encoding?.displayName ?: "Concept"} Details", onCollapse)

            if (vertex == null) {
                Row(Modifier.weight(1f).padding(8.dp)) {
                    Text("Click on a Concept from a results graph to inspect it", style = StudioTheme.typography.body1)
                }
            } else {
                val concept = vertex.concept
                val rows: List<List<String>> = listOfNotNull(
                    if (concept.isThing) listOf("Type", concept.asThing().type.label.name())
                    else listOf("Label", concept.asType().label.name()),

                    if (concept.isAttribute) listOf("Value", concept.asAttribute().valueString()) else null,
                    if (concept.isAttribute) listOf("Value Type", concept.asAttribute().type.valueType.schemaString()) else null,
                    if (concept.isAttributeType) listOf("Value Type", concept.asAttributeType().valueType.schemaString()) else null,
                    if (concept.isThing) listOf("Internal ID", concept.asThing().iid) else null,
                )

                Table(rows, Modifier.weight(1f), columnWeights = listOf(3f, 8f))
            }

            Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
        }
    }
}

@Composable
private fun Table(rows: List<List<String>>, modifier: Modifier = Modifier, columnWeights: List<Float> = listOf()) {
    Column(modifier) {
        rows.mapIndexed { rowIndex: Int, cells: List<String> ->
            Row(
                Modifier.height(20.dp)
                    .background(if (rowIndex % 2 == 0) StudioTheme.colors.background else StudioTheme.colors.backgroundHighlight),
                verticalAlignment = Alignment.CenterVertically) {

                cells.mapIndexed { columnIndex: Int, cell: String ->
                    Row(Modifier.weight(columnWeights.elementAtOrElse(columnIndex) { 1f }).padding(horizontal = 6.dp)) {
                        when (columnIndex) {
                            0 -> Text(
                                text = cell, maxLines = 1, style = StudioTheme.typography.body1,
                                fontWeight = if (columnIndex == 0) FontWeight.SemiBold else FontWeight.Normal
                            )
                            // In BasicTextField, readOnly = false is required to display a caret marker
                            else -> StudioTextField(
                                value = cell, onValueChange = {},
                                variant = StudioTextFieldVariant.UNDECORATED, readOnly = false,
                                textStyle = StudioTheme.typography.body1.copy(fontWeight = if (columnIndex == 0) FontWeight.SemiBold else FontWeight.Normal)
                            )
                        }
                    }
                }
            }
        }
    }
}
