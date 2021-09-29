package com.vaticle.typedb.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
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
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.StudioSnackbarHost
import com.vaticle.typedb.studio.login.LoginScreen
import com.vaticle.typedb.studio.navigation.LoginScreenState
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import com.vaticle.typedb.studio.appearance.VisualiserTheme
import com.vaticle.typedb.studio.workspace.WorkspaceScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "TypeDB Studio",
//        undecorated = true, // TODO: ideally we want undecorated (no title bar), but it seems to cause intermittent crashes on startup
        state = rememberWindowState(placement = WindowPlacement.Maximized)
    ) {

        var devicePixelRatio by remember { mutableStateOf(1F) }
        var titleBarHeight by remember { mutableStateOf(0F) }
        with(LocalDensity.current) { devicePixelRatio = 1.dp.toPx() }

        val scaffoldState = rememberScaffoldState()
        val snackbarHostState = scaffoldState.snackbarHostState

        val navigator = remember { Navigator(initialState = LoginScreenState()) }

        StudioTheme {
            Scaffold(modifier = Modifier.fillMaxSize()
                .border(BorderStroke(1.dp, SolidColor(StudioTheme.colors.uiElementBorder)))
                .onGloballyPositioned { coordinates ->
                titleBarHeight = window.height - coordinates.size.height / devicePixelRatio
            }) {

                when (val screenState = navigator.activeScreenState) {
                    is LoginScreenState -> LoginScreen(form = screenState, navigator, snackbarHostState)
                    is WorkspaceScreenState -> WorkspaceScreen(workspace = screenState,
                        visualiserTheme = VisualiserTheme.Default, window, devicePixelRatio, titleBarHeight, snackbarHostState)
                }

                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    StudioSnackbarHost(snackbarHostState)
                }
            }
        }
    }
}
