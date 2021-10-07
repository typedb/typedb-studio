package com.vaticle.typedb.studio.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.data.DBClient
import com.vaticle.typedb.studio.navigation.LoginScreenState
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import com.vaticle.typedb.studio.ui.elements.Icon
import com.vaticle.typedb.studio.ui.elements.StudioIcon
import com.vaticle.typedb.studio.ui.elements.StudioTab
import com.vaticle.typedb.studio.ui.elements.StudioTabs
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(form: LoginScreenState, navigator: Navigator, snackbarHostState: SnackbarHostState) {
    var selectedServerType by remember { mutableStateOf(ServerType.CORE) }
    Box(modifier = Modifier.fillMaxSize().background(StudioTheme.colors.windowBackdrop)
        .border(1.dp, StudioTheme.colors.uiElementBorder), contentAlignment = Alignment.Center) {

        Column(modifier = Modifier.size(400.dp, 300.dp).background(StudioTheme.colors.background)
            .border(1.dp, StudioTheme.colors.uiElementBorder)) {

            StudioTabs(Modifier.height(24.dp)) {
                StudioTab("TypeDB", selected = selectedServerType == ServerType.CORE,
                    arrangement = Arrangement.Center, textStyle = StudioTheme.typography.body1,
                    modifier = Modifier.weight(1f).clickable {
                        selectedServerType = ServerType.CORE
                    })
                StudioTab("TypeDB Cluster", selected = selectedServerType == ServerType.CLUSTER,
                    arrangement = Arrangement.Center, textStyle = StudioTheme.typography.body1,
                    modifier = Modifier.weight(1f).clickable {
                        selectedServerType = ServerType.CLUSTER
                    })
            }

            Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

            when (selectedServerType) {
                ServerType.CORE -> CoreLoginPanel(form, navigator, snackbarHostState)
                ServerType.CLUSTER -> ClusterLoginPanel(form, navigator, snackbarHostState)
            }
        }
    }
}

enum class ServerType {
    CORE,
    CLUSTER
}
