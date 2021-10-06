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
fun QuerySettingsPanel(settings: QuerySettings, onSettingsChange: (settings: QuerySettings) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Column {
            SidebarPanelHeader(title = "Query Settings")

            Row(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(6.dp)) {
                    val toggleReasoning = { onSettingsChange(settings.copy(enableReasoning = !settings.enableReasoning)) }
                    Row(modifier = Modifier.clickable { toggleReasoning() }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = settings.enableReasoning, onCheckedChange = { toggleReasoning() },
                            modifier = Modifier.scale(.75f),
                            colors = CheckboxDefaults.colors(checkedColor = StudioTheme.colors.icon))
                        Spacer(Modifier.width(4.dp))
                        Text("Enable reasoning and explanations", style = StudioTheme.typography.body1)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
        }
    }
}

data class QuerySettings(var enableReasoning: Boolean = false)
