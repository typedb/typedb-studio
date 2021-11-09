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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxColors
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.Icon
import com.vaticle.typedb.studio.ui.elements.StudioIcon

@Composable
fun QuerySettingsPanel(settings: QuerySettings, onSettingsChange: (settings: QuerySettings) -> Unit, onCollapse: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Column {
            SidebarPanelHeader(title = "Query Settings", onCollapse = onCollapse)

            Row(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(6.dp)) {
                    val toggleReasoning = { onSettingsChange(settings.copy(enableReasoning = !settings.enableReasoning)) }
                    Row(modifier = Modifier.clickable { toggleReasoning() }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = settings.enableReasoning, onCheckedChange = { toggleReasoning() },
                            modifier = Modifier.scale(.75f).size(21.dp),
                            colors = CheckboxDefaults.colors(checkedColor = StudioTheme.colors.icon))
                        Spacer(Modifier.width(4.dp))
                        Text("Enable Reasoning and Explanations", style = StudioTheme.typography.body1)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
        }
    }
}

data class QuerySettings(var enableReasoning: Boolean = false)
