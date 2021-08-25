package com.vaticle.typedb.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Colors
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.components.VaticleSnackbarHost
import com.vaticle.typedb.studio.login.LoginScreen
import com.vaticle.typedb.studio.navigation.LoginScreenState
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.TypeDBVisualiserState
import com.vaticle.typedb.studio.visualiser.TypeDBVisualiser
import com.vaticle.typedb.studio.visualiser.VisualiserTheme

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Compose for Desktop",
        state = rememberWindowState(placement = WindowPlacement.Maximized)
    ) {

        var devicePixelRatio by remember { mutableStateOf(1F) }
        var titleBarHeight by remember { mutableStateOf(0F) }
        with(LocalDensity.current) { devicePixelRatio = 1.dp.toPx() }

        val navigator = remember { Navigator(initialState = LoginScreenState()) }

        StudioTheme {
            MaterialTheme(
                colors = Colors(
                    primary = StudioTheme.colors.primary,
                    primaryVariant = Color.Black,
                    secondary = Color.Blue,
                    secondaryVariant = Color.Cyan,
                    background = StudioTheme.colors.background,
                    surface = StudioTheme.colors.uiElementBackground,
                    error = StudioTheme.colors.error,
                    onPrimary = StudioTheme.colors.onPrimary,
                    onSecondary = StudioTheme.colors.text,
                    onBackground = StudioTheme.colors.text,
                    onSurface = StudioTheme.colors.text,
                    onError = StudioTheme.colors.onPrimary,
                    isLight = false
                ),
                typography = Typography(
                    defaultFontFamily = StudioTheme.typography.defaultFontFamily
                )) {
                Scaffold(modifier = Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                    titleBarHeight = window.height - coordinates.size.height / devicePixelRatio
                }) {
                    val snackbarCoroutineScope = rememberCoroutineScope()
                    val snackbarHostState = SnackbarHostState()

                    when (val screenState = navigator.activeScreenState) {
                        is LoginScreenState -> LoginScreen(form = screenState, navigator, snackbarHostState, snackbarCoroutineScope)
                        is TypeDBVisualiserState -> TypeDBVisualiser(visualiser = screenState, theme = VisualiserTheme.Default,
                            window, devicePixelRatio, titleBarHeight, snackbarHostState, snackbarCoroutineScope)
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        VaticleSnackbarHost(snackbarHostState)
                    }
                }
            }
        }
    }
}
