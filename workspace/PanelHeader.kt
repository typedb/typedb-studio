package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme

@Composable
fun PanelHeader(modifier: Modifier = Modifier, content: @Composable (RowScope.() -> Unit)) {
    Row(modifier = modifier.height(26.dp).background(StudioTheme.colors.background),
        verticalAlignment = Alignment.CenterVertically) {
        content()
    }

    Row(modifier = modifier.height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
}
