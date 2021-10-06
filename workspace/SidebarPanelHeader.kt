package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.Icon
import com.vaticle.typedb.studio.ui.elements.StudioIcon

@Composable
fun SidebarPanelHeader(title: String) {
    PanelHeader(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(6.dp))
        Text(title, style = StudioTheme.typography.body2)

        Spacer(Modifier.weight(1f))

        StudioIcon(Icon.Minus, modifier = Modifier.width(16.dp))
        Spacer(Modifier.width(12.dp))
    }
}
