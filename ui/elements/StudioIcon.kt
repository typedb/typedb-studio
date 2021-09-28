package com.vaticle.typedb.studio.ui.elements

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.IconSize.*

@Composable
fun StudioIcon(icon: Icon, color: Color = StudioTheme.colors.icon, size: IconSize = Size16, modifier: Modifier = Modifier) {
    Text(text = icon.charString, modifier = modifier, color = color, fontSize = size.fontSize, fontFamily = size.fontFamily)
}

private val blueprintIcons16 = FontFamily(Font(resource = "icons/blueprint-icons-16.ttf"))
private val blueprintIcons20 = FontFamily(Font(resource = "icons/blueprint-icons-20.ttf"))

enum class IconSize(val fontSize: TextUnit, val fontFamily: FontFamily) {
    Size16(fontSize = 16.sp, fontFamily = blueprintIcons16),
    Size20(fontSize = 20.sp, fontFamily = blueprintIcons20),
}

enum class Icon(charCode: UShort) {
    CaretDown(0x2304u),
    Database(0xe683u);

    val charString: String = Char(charCode).toString()
}
