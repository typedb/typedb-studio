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

package com.vaticle.typedb.studio.common.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import java.awt.Font
import javax.swing.JPanel

private val systemDefaultSwingFont: Font = JPanel().font

@Stable
class TypographyTheme(
    private val variableWidthFontFamily: FontFamily, fixedWidthFontFamily: FontFamily,
    mediumSize: Int, smallSize: Int,
    val body1SwingFont: Font, val code1SwingFont: Font,
) {
    val body1 = TextStyle(fontSize = mediumSize.sp, fontFamily = variableWidthFontFamily)
    val body2 = TextStyle(fontSize = smallSize.sp, fontFamily = variableWidthFontFamily)
    val code1 = TextStyle(fontSize = mediumSize.sp, fontFamily = fixedWidthFontFamily)
    val code2 = TextStyle(fontSize = smallSize.sp, fontFamily = fixedWidthFontFamily)

    @Composable
    @ReadOnlyComposable
    fun materialTypography(): androidx.compose.material.Typography {
        return androidx.compose.material.Typography(
            defaultFontFamily = Theme.typography.variableWidthFontFamily,
            body1 = Theme.typography.body1,
            button = TextStyle(fontWeight = FontWeight.SemiBold, letterSpacing = 0.25.sp)
        )
    }

    object Presets {
        private const val DEFAULT_MEDIUM_SIZE = 13
        private const val DEFAULT_SMALL_SIZE = 11

        val default = TypographyTheme(
            variableWidthFontFamily = titilliumWeb,
            fixedWidthFontFamily = ubuntuMono,
            mediumSize = DEFAULT_MEDIUM_SIZE,
            smallSize = DEFAULT_SMALL_SIZE,
            body1SwingFont = swingFontOf(TITILLIUM_WEB_REGULAR, DEFAULT_MEDIUM_SIZE),
            code1SwingFont = swingFontOf(UBUNTU_MONO_REGULAR, DEFAULT_MEDIUM_SIZE),
        )
    }


}

fun swingFontOf(filePath: String, fontSize: Int): Font {
    return ClassLoader.getSystemResourceAsStream(filePath)?.let {
        Font.createFont(Font.TRUETYPE_FONT, it).deriveFont(fontSize)
    } ?: JPanel().font /* system default font */
}

private const val TITILLIUM_WEB_REGULAR = "fonts/titilliumweb/TitilliumWeb-Regular.ttf"
private const val TITILLIUM_WEB_SEMI_BOLD = "fonts/titilliumweb/TitilliumWeb-SemiBold.ttf"
private const val UBUNTU_MONO_REGULAR = "fonts/ubuntumono/UbuntuMono-Regular.ttf"

private val titilliumWeb = FontFamily(
    Font(TITILLIUM_WEB_REGULAR, FontWeight.Normal, FontStyle.Normal),
    Font(TITILLIUM_WEB_SEMI_BOLD, FontWeight.SemiBold, FontStyle.Normal)
)

private val ubuntuMono = FontFamily(Font(UBUNTU_MONO_REGULAR, FontWeight.Normal, FontStyle.Normal))
