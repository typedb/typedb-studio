package com.vaticle.typedb.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.VisualiserTheme
import com.vaticle.typedb.studio.login.LoginScreen
import com.vaticle.typedb.studio.routing.LoginRoute
import com.vaticle.typedb.studio.routing.Router
import com.vaticle.typedb.studio.routing.WorkspaceRoute
import com.vaticle.typedb.studio.storage.AppData
import com.vaticle.typedb.studio.ui.elements.StudioSnackbarHost
import com.vaticle.typedb.studio.workspace.WorkspaceScreen
import mu.KotlinLogging.logger

@Composable
fun Studio(onCloseRequest: () -> Unit) {
    val windowState = rememberWindowState(WindowPlacement.Maximized);
    val snackbarHostState = rememberScaffoldState().snackbarHostState
    var titleBarHeight by remember { mutableStateOf(0F) }
    val router = remember { Router(initialRoute = LoginRoute.Core()) }
    val pixelDensity = LocalDensity.current.density

    // TODO: we want undecorated (no title bar), by passing undecorated = true,
    //       but it seems to cause intermittent crashes on startup (see #40).
    //       Test if they occur when running the distribution, or only with bazel run :studio-bin-*
    androidx.compose.ui.window.Window(title = "TypeDB Studio", onCloseRequest = onCloseRequest, state = windowState) {
        StudioTheme {
            Scaffold(modifier = Modifier.fillMaxSize()
                .border(BorderStroke(1.dp, SolidColor(StudioTheme.colors.uiElementBorder)))
                .onGloballyPositioned { coordinates ->
                    // used to translate from screen coordinates to window coordinates in the visualiser
                    titleBarHeight = window.height - coordinates.size.height / pixelDensity
                }) {

                when (val routeData = router.currentRoute) {
                    is LoginRoute -> LoginScreen(routeData, router, snackbarHostState)
                    is WorkspaceRoute -> WorkspaceScreen(
                        routeData, router, VisualiserTheme.Default, window, titleBarHeight, snackbarHostState
                    )
                }

                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    StudioSnackbarHost(snackbarHostState)
                }
            }
        }
    }
}

fun main() {
    AppData().initialise()
    val log = logger {}

    application {
        fun onCloseRequest() {
            log.debug { "Closing TypeDB Studio" }
            exitApplication() // TODO: I think this is the wrong behaviour on MacOS
        }
        Studio(::onCloseRequest)
    }
}
