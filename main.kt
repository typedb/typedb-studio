package com.vaticle.typedb.studio

import VaticleSnackbarHost
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Typography
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.db.DB
import com.vaticle.typedb.studio.visualiser.ui.TypeDBVisualiser
import com.vaticle.typedb.studio.visualiser.ui.VisualiserTheme

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
            defaultFontFamily = titilliumWeb,
        )
        val dbClient = DB()
        var devicePixelRatio by remember { mutableStateOf(1F) }
        var titleBarHeight by remember { mutableStateOf(0F) }
        with(LocalDensity.current) { devicePixelRatio = 1.dp.toPx() }

        MaterialTheme(typography = vaticleTypography) {
            Scaffold(modifier = Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                titleBarHeight = window.height - coordinates.size.height / devicePixelRatio
            }) {
                val snackbarCoroutineScope = rememberCoroutineScope()
                val snackbarHostState = SnackbarHostState()

                TypeDBVisualiser(db = dbClient, theme = VisualiserTheme.DEFAULT, window, devicePixelRatio, titleBarHeight, snackbarHostState, snackbarCoroutineScope)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    VaticleSnackbarHost(snackbarHostState)
                }
            }
        }
    }
}
