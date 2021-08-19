package com.vaticle

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.graph.renderer.GraphVisualiser
import com.vaticle.graph.renderer.VisualiserTheme

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Compose for Desktop",
        state = rememberWindowState(placement = WindowPlacement.Maximized)
    ) {
        val titilliumWeb = FontFamily(
            Font(resource = "fonts/TitilliumWeb/TitilliumWeb-Regular.ttf", weight = FontWeight.W400, style = FontStyle.Normal)
        )
        val vaticleTypography = Typography(
            button = TextStyle(
                fontFamily = titilliumWeb,
            )
        )

        MaterialTheme(typography = vaticleTypography) {
            GraphVisualiser(theme = VisualiserTheme.DEFAULT)
        }
    }
}
