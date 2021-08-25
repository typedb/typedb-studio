// Implementation notes: https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material/material/src/commonMain/kotlin/androidx/compose/material/Colors.kt

package com.vaticle.typedb.studio.appearance

import androidx.compose.material.ProvideTextStyle
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

@Composable
fun StudioTheme(
    colors: StudioColors = StudioTheme.colors,
    typography: StudioTypography = StudioTheme.typography,
    shapes: StudioShapes = StudioTheme.shapes,
    content: @Composable () -> Unit
) {
    val rememberedColors = remember {
        colors.copy()
    }.apply { updateColorsFrom(colors) }
    CompositionLocalProvider(
        LocalColors provides rememberedColors,
        LocalShapes provides shapes,
        LocalTypography provides typography
    ) {
        ProvideTextStyle(value = typography.body1, content = content)
    }
}

object StudioTheme {
    val colors: StudioColors
        @Composable
        @ReadOnlyComposable
        get() = LocalColors.current

    val typography: StudioTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current

    val shapes: StudioShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalShapes.current
}

@Stable
class StudioColors(primary: Color, onPrimary: Color, background: Color, uiElementBackground: Color, error: Color, panelSeparator: Color, windowBackdrop: Color, text: Color) {
    var primary by mutableStateOf(primary, structuralEqualityPolicy())
        private set
    var onPrimary by mutableStateOf(onPrimary, structuralEqualityPolicy())
        private set
    var background by mutableStateOf(background, structuralEqualityPolicy())
        private set
    var uiElementBackground by mutableStateOf(uiElementBackground, structuralEqualityPolicy())
        private set
    var error by mutableStateOf(error, structuralEqualityPolicy())
        private set
    var panelSeparator by mutableStateOf(panelSeparator, structuralEqualityPolicy())
        private set
    var windowBackdrop by mutableStateOf(windowBackdrop, structuralEqualityPolicy())
        private set
    var text by mutableStateOf(text, structuralEqualityPolicy())
        private set

    fun copy(primary: Color = this.primary, onPrimary: Color = this.onPrimary, background: Color = this.background,
             uiElementBackground: Color = this.uiElementBackground, error: Color = this.error,
             panelSeparator: Color = this.panelSeparator, windowBackdrop: Color = this.windowBackdrop,
             text: Color = this.text): StudioColors
    = StudioColors(primary, onPrimary, background, uiElementBackground, error, panelSeparator, windowBackdrop, text)

    fun updateColorsFrom(other: StudioColors) {
        primary = other.primary
        onPrimary = other.onPrimary
        background = other.background
        uiElementBackground = other.uiElementBackground
        error = other.error
        panelSeparator = other.panelSeparator
        windowBackdrop = other.windowBackdrop
        text = other.text
    }
}

class VaticlePalette {
    companion object {
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
}

fun studioDarkColors(
    primary: Color = VaticlePalette.Green,
    onPrimary: Color = VaticlePalette.Purple3,
    background: Color = VaticlePalette.Purple4,
    uiElementBackground: Color = VaticlePalette.Purple3,
    error: Color = VaticlePalette.Red1,
    panelSeparator: Color = VaticlePalette.Purple1,
    windowBackdrop: Color = VaticlePalette.Purple1,
    text: Color = Color.White
): StudioColors = StudioColors(primary, onPrimary, background, uiElementBackground, error, panelSeparator, windowBackdrop, text)

val LocalColors = staticCompositionLocalOf { studioDarkColors() }

@Immutable
class StudioTypography(val defaultFontFamily: FontFamily = FontFamily.Default, body1: TextStyle = TextStyle(fontSize = 16.sp), body2: TextStyle = TextStyle(fontSize = 14.sp)) {

    val body1 = body1.withDefaultFontFamily(defaultFontFamily)
    val body2 = body2.withDefaultFontFamily(defaultFontFamily)

    fun copy(defaultFontFamily: FontFamily, body1: TextStyle = this.body1, body2: TextStyle = this.body2): StudioTypography = StudioTypography(defaultFontFamily, body1, body2)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StudioTypography) return false

        if (body1 != other.body1) return false
        if (body2 != other.body2) return false

        return true
    }

    override fun hashCode(): Int {
        var result = body1.hashCode()
        result = 31 * result + body2.hashCode()
        return result
    }
}

private fun TextStyle.withDefaultFontFamily(default: FontFamily): TextStyle {
    return if (fontFamily != null) this else copy(fontFamily = default)
}

private val titilliumWeb = FontFamily(
    Font(resource = "fonts/TitilliumWeb/TitilliumWeb-Regular.ttf", weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resource = "fonts/TitilliumWeb/TitilliumWeb-SemiBold.ttf", weight = FontWeight.SemiBold, style = FontStyle.Normal)
)

val LocalTypography = staticCompositionLocalOf { StudioTypography(defaultFontFamily = titilliumWeb) }

class StudioShapes {

}

val LocalShapes = staticCompositionLocalOf { StudioShapes() }
