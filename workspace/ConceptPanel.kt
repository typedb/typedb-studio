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
import com.vaticle.typedb.studio.visualiser.VertexState

@Composable
fun ConceptPanel(vertex: VertexState?, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Column {
            SidebarPanelHeader(title = "Concept")

            if (vertex == null) {
                Row(Modifier.weight(1f).padding(8.dp)) {
                    Text("Click on a Concept from a results graph to inspect it", style = StudioTheme.typography.body1)
                }
            } else {
                val concept = vertex.concept
                val rows = listOfNotNull(
                    listOf("Type", if (concept.isThing) concept.asThing().type.label.name() else concept.asType().label.name()),
                    if (concept.isAttribute) listOf("Value", concept.asAttribute().valueString()) else null,
                    if (concept.isAttribute) listOf("Value Type", concept.asAttribute().type.valueType.schemaString()) else null,
                    if (concept.isAttributeType) listOf("Value Type", concept.asAttributeType().valueType.schemaString()) else null,
                    listOf("Encoding", vertex.encoding.displayName),
                    if (concept.isThing) listOf("Internal ID", "123b548m565") else null,
                )

                Table(rows = rows, columnWeights = listOf(1f, 2f), modifier = Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
        }
    }
}

@Composable
private fun Table(rows: List<List<String>>, modifier: Modifier = Modifier, columnWeights: List<Float> = listOf()) {
    Column(modifier) {
        rows.mapIndexed { rowIndex: Int, cells: List<String> ->
            Row(Modifier.height(20.dp)
                .background(if (rowIndex % 2 == 0) StudioTheme.colors.background else StudioTheme.colors.backgroundHighlight),
                verticalAlignment = Alignment.CenterVertically) {

                cells.mapIndexed { columnIndex: Int, cell: String ->
                    Row(Modifier.weight(columnWeights.elementAtOrElse(columnIndex) { 1f }).padding(horizontal = 6.dp)) {
                        Text(text = cell, maxLines = 1, style = StudioTheme.typography.body1,
                            fontWeight = if (columnIndex == 0) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
