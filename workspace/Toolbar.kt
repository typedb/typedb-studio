package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.Icon
import com.vaticle.typedb.studio.ui.elements.IconSize.*
import com.vaticle.typedb.studio.ui.elements.StudioDropdownBox
import com.vaticle.typedb.studio.ui.elements.StudioIcon

@Composable
fun Toolbar(modifier: Modifier = Modifier, dbName: String, onRun: () -> Unit) {

    Column(modifier = modifier.height(31.dp)) {
        Row(modifier = modifier.weight(1F).background(StudioTheme.colors.background),
            verticalAlignment = Alignment.CenterVertically) {

            Spacer(modifier.width(8.dp))
            StudioDropdownBox()

            Spacer(modifier.width(8.dp))
            StudioIcon(Icon.ChevronLeft, size = Size20)

            Spacer(modifier.width(8.dp))
            StudioIcon(Icon.ChevronRight, size = Size20)

            Spacer(modifier.width(8.dp))
            StudioIcon(Icon.FloppyDisk)

            Spacer(modifier.width(14.dp))
            StudioIcon(Icon.FolderOpen)

            Spacer(modifier.width(10.dp))
            StudioIcon(Icon.Play, Color(0xFF499C54), size = Size20, modifier = Modifier.clickable { onRun() })

            Spacer(modifier.width(8.dp))
            StudioIcon(Icon.Stop, Color(0xFFA1250C), size = Size20)

            Spacer(modifier.weight(1F))

            StudioIcon(Icon.Cog)
            Spacer(modifier.width(12.dp))

            StudioIcon(Icon.LogOut)
            Spacer(modifier.width(10.dp))
        }
        Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
    }
}
