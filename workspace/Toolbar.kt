package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.Icon
import com.vaticle.typedb.studio.ui.elements.IconSize.*
import com.vaticle.typedb.studio.ui.elements.StudioDropdownBox
import com.vaticle.typedb.studio.ui.elements.StudioIcon

@Composable
fun Toolbar(modifier: Modifier = Modifier, dbName: String, allDBNames: List<String>, onDBNameChange: (value: String) -> Unit, onRun: () -> Unit, onLogout: () -> Unit) {

    Row(modifier = modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(8.dp))
        StudioDropdownBox(items = allDBNames, text = dbName, onTextChange = onDBNameChange, textStyle = StudioTheme.typography.body2,
            modifier = Modifier.size(width = 96.dp, height = 24.dp),
            leadingIcon = {
                val pixelDensity = LocalDensity.current.density
                when {
                    pixelDensity <= 1f -> Image(painter = painterResource("icons/database.png"),
                        contentDescription = "Database",
                        modifier = Modifier.graphicsLayer(scaleX = pixelDensity, scaleY = pixelDensity))
                    else -> Image(painter = loadSvgPainter(ClassLoader.getSystemResourceAsStream("icons/database.svg")!!, LocalDensity.current),
                        contentDescription = "Database",
                        modifier = Modifier.graphicsLayer(scaleX = 14f / 12f, scaleY = 14f / 12f))
                }
            })

        Spacer(Modifier.width(8.dp))
        StudioIcon(Icon.ChevronLeft, size = Size16Light)

        Spacer(Modifier.width(8.dp))
        StudioIcon(Icon.ChevronRight, size = Size16Light)

        Spacer(Modifier.width(10.dp))
        StudioIcon(Icon.FolderOpen, size = Size14)

        Spacer(Modifier.width(12.dp))
        StudioIcon(Icon.FloppyDisk)

        Spacer(Modifier.width(10.dp))
        StudioIcon(Icon.Play, Color(0xFF499C54), size = Size18, modifier = Modifier.clickable { onRun() })

        Spacer(Modifier.width(8.dp))
        StudioIcon(Icon.Stop, Color(0xFFA1250C), size = Size18)

        Spacer(Modifier.weight(1F))

//        StudioIcon(Icon.Cog)
//        Spacer(Modifier.width(15.dp))

        StudioIcon(Icon.LogOut, modifier = Modifier.clickable { onLogout() })
        Spacer(Modifier.width(12.dp))
    }
}
