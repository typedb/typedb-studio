package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

    Row(modifier = modifier.height(28.dp).background(StudioTheme.colors.background),
        verticalAlignment = Alignment.CenterVertically) {

        Spacer(Modifier.width(8.dp))
        StudioDropdownBox()

        Spacer(Modifier.width(8.dp))
        StudioIcon(Icon.ChevronLeft, size = Size16Light)

        Spacer(Modifier.width(8.dp))
        StudioIcon(Icon.ChevronRight, size = Size16Light)

        Spacer(Modifier.width(8.dp))
        StudioIcon(Icon.FloppyDisk)

        Spacer(Modifier.width(14.dp))
        StudioIcon(Icon.FolderOpen, size = Size14)

        Spacer(Modifier.width(10.dp))
        StudioIcon(Icon.Play, Color(0xFF499C54), size = Size18, modifier = Modifier.clickable { onRun() })

        Spacer(Modifier.width(8.dp))
        StudioIcon(Icon.Stop, Color(0xFFA1250C), size = Size18)

        Spacer(Modifier.weight(1F))

        StudioIcon(Icon.Cog)
        Spacer(Modifier.width(14.dp))

        StudioIcon(Icon.LogOut)
        Spacer(Modifier.width(12.dp))
    }
}
