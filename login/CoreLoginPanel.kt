package com.vaticle.typedb.studio.login

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.data.CoreClient
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.navigation.LoginScreenState
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import com.vaticle.typedb.studio.ui.elements.StudioButton
import com.vaticle.typedb.studio.ui.elements.StudioDropdownBox
import com.vaticle.typedb.studio.ui.elements.StudioTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CoreLoginPanel(form: LoginScreenState, navigator: Navigator, snackbarHostState: SnackbarHostState) {
    val snackbarCoroutineScope = rememberCoroutineScope()

    fun tryCreateClient() {
        try {
            form.dbClient?.close()
            if (form.serverAddress.isNotBlank()) {
                val client = CoreClient(form.serverAddress)
                form.dbClient = client
                form.db = DB(client, form.dbName)
            }
        } catch (e: Exception) {
            form.db = null
            snackbarCoroutineScope.launch {
                println(e.toString())
                snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)) {

        val focusManager = LocalFocusManager.current

        FormFieldGroup {
            FormField {
                Text("Server address", style = StudioTheme.typography.body1, modifier = Modifier.weight(2f))
                StudioTextField(value = form.serverAddress, onValueChange = { value -> form.serverAddress = value },
                    textStyle = StudioTheme.typography.body1, modifier = Modifier.weight(3f).height(28.dp))
            }
            FormField {
                Text("Database", style = StudioTheme.typography.body1, modifier = Modifier.weight(2f))
//                StudioTextField(value = form.dbName, onValueChange = { value ->
//                    form.dbName = value
//                    form.db = form.dbClient?.let { client -> DB(client, form.dbName) }
//                }, textStyle = StudioTheme.typography.body1, modifier = Modifier.weight(3f).height(28.dp)
//                    .onPreviewKeyEvent { event: KeyEvent ->
//                        if (event.nativeKeyEvent.id == java.awt.event.KeyEvent.KEY_RELEASED) return@onPreviewKeyEvent true
//                        when (event.key) {
//                            Key.Enter, Key.NumPadEnter -> {
//                                form.db?.let { navigator.pushState(WorkspaceScreenState(db = it)) }
//                                return@onPreviewKeyEvent true
//                            }
//                            else -> return@onPreviewKeyEvent false
//                        }
//                    })
                StudioDropdownBox(text = form.dbName, onTextChange = { value ->
                    form.dbName = value
                    form.db = form.dbClient?.let { client -> DB(client, form.dbName) }
                }, modifier = Modifier.weight(3f).height(28.dp))
            }
        }

        StudioButton(text = "Connect to TypeDB", enabled = form.db != null,
            onClick = { navigator.pushState(WorkspaceScreenState(db = requireNotNull(form.db))) })

        LaunchedEffect(Unit) {
            focusManager.moveFocus(FocusDirection.Down)
            // TODO: this is overly aggressive and may trigger an error with no user action
            tryCreateClient()
        }
    }
}
