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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.VisualiserTheme
import com.vaticle.typedb.studio.login.LoginScreen
import com.vaticle.typedb.studio.routing.CoreLoginRoute
import com.vaticle.typedb.studio.routing.LoginRoute
import com.vaticle.typedb.studio.routing.Router
import com.vaticle.typedb.studio.routing.WorkspaceRoute
import com.vaticle.typedb.studio.ui.elements.StudioSnackbarHost
import com.vaticle.typedb.studio.workspace.WorkspaceScreen

@Composable
fun MainWindow(onCloseRequest: () -> Unit) {

    val windowState: WindowState = rememberWindowState(placement = WindowPlacement.Maximized)
    var titleBarHeight by remember { mutableStateOf(0F) }
    val scaffoldState = rememberScaffoldState()
    val snackbarHostState = scaffoldState.snackbarHostState
    val router = remember { Router(initialRoute = CoreLoginRoute()) }
    val pixelDensity = LocalDensity.current.density

    // TODO: we want undecorated (no title bar), but it seems to cause intermittent crashes on startup (see #40)
    //       Test if they occur when running the distribution, or only with bazel run :studio-bin-*
    Window(title = "TypeDB Studio", onCloseRequest = onCloseRequest, state = windowState /*undecorated = true*/) {
        StudioTheme {
            Scaffold(modifier = Modifier.fillMaxSize()
                .border(BorderStroke(1.dp, SolidColor(StudioTheme.colors.uiElementBorder)))
                .onGloballyPositioned { coordinates ->
                    // used to translate from screen coordinates to window coordinates in the visualiser
                    titleBarHeight = window.height - coordinates.size.height / pixelDensity
                }) {

                when (val currentRoute = router.currentRoute) {
                    is LoginRoute -> LoginScreen(routeData = currentRoute, router, snackbarHostState)
                    is WorkspaceRoute -> WorkspaceScreen(workspace = currentRoute, router,
                        visualiserTheme = VisualiserTheme.Default, window, titleBarHeight, snackbarHostState)
                }

                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    StudioSnackbarHost(snackbarHostState)
                }
            }
        }
    }
}
