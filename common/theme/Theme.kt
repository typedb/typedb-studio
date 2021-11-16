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

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import java.awt.Font.TRUETYPE_FONT
import java.awt.Font.createFont
import javax.swing.JPanel

// Implementation notes: https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material/material/src/commonMain/kotlin/androidx/compose/material/Colors.kt

@Composable
fun Theme(
    colors: com.vaticle.typedb.studio.common.theme.Colors = Theme.colors,
    typography: com.vaticle.typedb.studio.common.theme.Typography = Theme.typography,
    content: @Composable () -> Unit
) {
    val rememberedColors = remember {
        colors.copy()
    }.apply { updateColorsFrom(colors) }
    CompositionLocalProvider(
        LocalColors provides rememberedColors,
        LocalTypography provides typography
    ) {
        ProvideTextStyle(value = typography.body1) {
            MaterialTheme(
                colors = Colors(
                    primary = Theme.colors.primary,
                    primaryVariant = Color.Green,
                    secondary = Color.Yellow,
                    secondaryVariant = Color.Cyan,
                    background = Theme.colors.background,
                    surface = Theme.colors.uiElementBackground,
                    error = Theme.colors.error,
                    onPrimary = Theme.colors.onPrimary,
                    onSecondary = Theme.colors.text,
                    onBackground = Theme.colors.text,
                    onSurface = Theme.colors.text,
                    onError = Theme.colors.onPrimary,
                    isLight = false
                ),
                typography = Typography(
                    defaultFontFamily = Theme.typography.defaultFontFamily,
                    button = TextStyle(fontWeight = FontWeight.SemiBold, letterSpacing = 0.25.sp)
                ),
                content = content
            )
        }
    }
}

object Theme {
    val colors: com.vaticle.typedb.studio.common.theme.Colors
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current

    val typography: com.vaticle.typedb.studio.common.theme.Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current
}

@Stable
class Colors(
    primary: Color, onPrimary: Color, background: Color, backgroundHighlight: Color, uiElementBackground: Color,
    uiElementBorder: Color, editorBackground: Color, error: Color, windowBackdrop: Color,
    text: Color, icon: Color
) {
    var primary by mutableStateOf(primary, structuralEqualityPolicy())
        private set
    var onPrimary by mutableStateOf(onPrimary, structuralEqualityPolicy())
        private set
    var background by mutableStateOf(background, structuralEqualityPolicy())
        private set
    var backgroundHighlight by mutableStateOf(backgroundHighlight, structuralEqualityPolicy())
        private set
    var uiElementBackground by mutableStateOf(uiElementBackground, structuralEqualityPolicy())
        private set
    var uiElementBorder by mutableStateOf(uiElementBorder, structuralEqualityPolicy())
        private set
    var editorBackground by mutableStateOf(editorBackground, structuralEqualityPolicy())
        private set
    var error by mutableStateOf(error, structuralEqualityPolicy())
        private set
    var windowBackdrop by mutableStateOf(windowBackdrop, structuralEqualityPolicy())
        private set
    var text by mutableStateOf(text, structuralEqualityPolicy())
        private set
    var icon by mutableStateOf(icon, structuralEqualityPolicy())
        private set

    fun copy(
        primary: Color = this.primary, onPrimary: Color = this.onPrimary, background: Color = this.background,
        backgroundHighlight: Color = this.backgroundHighlight, uiElementBackground: Color = this.uiElementBackground,
        uiElementBorder: Color = this.uiElementBorder, editorBackground: Color = this.editorBackground,
        error: Color = this.error, windowBackdrop: Color = this.windowBackdrop, text: Color = this.text,
        icon: Color = this.icon
    ): com.vaticle.typedb.studio.common.theme.Colors = Colors(
        primary, onPrimary, background, backgroundHighlight, uiElementBackground,
        uiElementBorder, editorBackground, error, windowBackdrop, text, icon
    )

    fun updateColorsFrom(other: com.vaticle.typedb.studio.common.theme.Colors) {
        primary = other.primary
        onPrimary = other.onPrimary
        background = other.background
        backgroundHighlight = other.backgroundHighlight
        uiElementBackground = other.uiElementBackground
        uiElementBorder = other.uiElementBorder
        editorBackground = other.editorBackground
        error = other.error
        windowBackdrop = other.windowBackdrop
        text = other.text
        icon = other.icon
    }
}

fun Color.toSwingColor() = java.awt.Color(red, green, blue, alpha)

object Palette {
    val Purple0 = Color(0xFF08022E)
    val Purple1 = Color(0xFF0E053F)
    val Purple2 = Color(0xFF180F49)
    val Purple3 = Color(0xFF1D1354)
    val Purple4 = Color(0xFF261C5E)
    val Purple5 = Color(0xFF372E6A)
    val Purple6 = Color(0xFF392D7F)
    val Purple7 = Color(0xFF544899)
    val Purple8 = Color(0xFFA488CA)
    val Green = Color(0xFF02DAC9)
    val Red1 = Color(0xFFF66B65)
    val Red2 = Color(0xFFFFA187)
    val Yellow1 = Color(0xFFF6C94C)
    val Yellow2 = Color(0xFFFFE4A7)
    val Pink1 = Color(0xFFF28DD7)
    val Pink2 = Color(0xFFFFA9E8)
}

fun studioDarkColors(
    primary: Color = Palette.Green,
    onPrimary: Color = Palette.Purple3,
    background: Color = Palette.Purple1,
    backgroundHighlight: Color = Palette.Purple4,
    uiElementBackground: Color = Palette.Purple3,
    uiElementBorder: Color = Palette.Purple6,
    editorBackground: Color = Palette.Purple0,
    error: Color = Palette.Red1,
    windowBackdrop: Color = Palette.Purple0,
    text: Color = Color.White,
    icon: Color = Color(0xFF888DCA),
): com.vaticle.typedb.studio.common.theme.Colors = Colors(
    primary, onPrimary, background, backgroundHighlight, uiElementBackground, uiElementBorder, editorBackground, error,
    windowBackdrop, text, icon
)

val LocalColors = staticCompositionLocalOf { studioDarkColors() }

private val systemDefaultFont: java.awt.Font = JPanel().font

@Immutable
class Typography(
    val defaultFontFamily: FontFamily = FontFamily.Default,
    val defaultMonospaceFontFamily: FontFamily = FontFamily.Monospace,
    body1: TextStyle = TextStyle(fontSize = 13.sp),
    body2: TextStyle = TextStyle(fontSize = 11.sp),
    code1: TextStyle = TextStyle(fontSize = 13.sp),
    code2: TextStyle = TextStyle(fontSize = 11.sp),
    val codeEditorSwing: java.awt.Font = ubuntuMonoSize13Swing ?: systemDefaultFont,
    val codeEditorContextMenuSwing: java.awt.Font = titilliumWebSize13Swing ?: systemDefaultFont,
) {
    val body1 = body1.withDefaultFontFamily(defaultFontFamily)
    val body2 = body2.withDefaultFontFamily(defaultFontFamily)
    val code1 = code1.withDefaultFontFamily(defaultMonospaceFontFamily)
    val code2 = code2.withDefaultFontFamily(defaultMonospaceFontFamily)

    fun copy(
        defaultFontFamily: FontFamily, defaultMonospaceFontFamily: FontFamily, body1: TextStyle = this.body1,
        body2: TextStyle = this.body2, code1: TextStyle = this.code1,
        code2: TextStyle = this.code2, codeEditorSwing: java.awt.Font = this.codeEditorSwing,
        codeEditorContextMenuSwing: java.awt.Font = this.codeEditorContextMenuSwing
    ): com.vaticle.typedb.studio.common.theme.Typography = Typography(
        defaultFontFamily, defaultMonospaceFontFamily, body1, body2, code1, code2, codeEditorSwing,
        codeEditorContextMenuSwing
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is com.vaticle.typedb.studio.common.theme.Typography) return false

        if (body1 != other.body1) return false
        if (body2 != other.body2) return false
        if (code1 != other.code1) return false
        if (code2 != other.code2) return false
        if (codeEditorSwing != other.codeEditorSwing) return false
        if (codeEditorContextMenuSwing != other.codeEditorContextMenuSwing) return false

        return true
    }

    override fun hashCode(): Int {
        var result = body1.hashCode()
        result = 31 * result + body2.hashCode()
        result = 31 * result + code1.hashCode()
        result = 31 * result + code2.hashCode()
        result = 31 * result + codeEditorSwing.hashCode()
        result = 31 * result + codeEditorContextMenuSwing.hashCode()
        return result
    }
}

private fun TextStyle.withDefaultFontFamily(default: FontFamily): TextStyle {
    return if (fontFamily != null) this else copy(fontFamily = default)
}

private const val TITILLIUM_WEB_REGULAR = "fonts/titilliumweb/TitilliumWeb-Regular.ttf"
private const val TITILLIUM_WEB_SEMI_BOLD = "fonts/titilliumweb/TitilliumWeb-SemiBold.ttf"
private const val UBUNTU_MONO_REGULAR = "fonts/ubuntumono/UbuntuMono-Regular.ttf"

private val titilliumWeb = FontFamily(
    Font(TITILLIUM_WEB_REGULAR, FontWeight.Normal, FontStyle.Normal),
    Font(TITILLIUM_WEB_SEMI_BOLD, FontWeight.SemiBold, FontStyle.Normal)
)

private val ubuntuMono = FontFamily(
    Font(UBUNTU_MONO_REGULAR, FontWeight.Normal, FontStyle.Normal)
)

private val ubuntuMonoSize13Swing: java.awt.Font? = ClassLoader.getSystemResourceAsStream(UBUNTU_MONO_REGULAR)?.let {
    createFont(TRUETYPE_FONT, it).deriveFont(13f)
}

private val titilliumWebSize13Swing: java.awt.Font? = ClassLoader.getSystemResourceAsStream(TITILLIUM_WEB_REGULAR)?.let {
    createFont(TRUETYPE_FONT, it).deriveFont(13f)
}

val LocalTypography = staticCompositionLocalOf {
    Typography(defaultFontFamily = titilliumWeb, defaultMonospaceFontFamily = ubuntuMono)
}
