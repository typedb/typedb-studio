package com.vaticle.typedb.studio

import VaticleSnackbarHost
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
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

        MaterialTheme(typography = vaticleTypography) {
            Scaffold {
                val snackbarCoroutineScope = rememberCoroutineScope()
                val snackbarHostState = SnackbarHostState()
                TypeDBVisualiser(db = dbClient, theme = VisualiserTheme.DEFAULT, snackbarHostState, snackbarCoroutineScope)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    VaticleSnackbarHost(snackbarHostState)
                }
            }
        }
    }
}
