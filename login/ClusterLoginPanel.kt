package com.vaticle.typedb.studio.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.data.ClusterClient
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.navigation.LoginScreenState
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import kotlinx.coroutines.launch
import java.awt.event.KeyEvent.KEY_RELEASED

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClusterLoginPanel(form: LoginScreenState, navigator: Navigator, snackbarHostState: SnackbarHostState) {
    val snackbarCoroutineScope = rememberCoroutineScope()

    fun tryCreateClient() {
        try {
            form.dbClient?.close()
            if (form.serverAddress.isNotBlank() && form.username.isNotBlank() && form.password.isNotBlank()) {
                val client = ClusterClient(form.serverAddress, form.username, form.password, form.rootCAPath)
                form.dbClient = client
                form.db = DB(client, form.dbName)
            }
        } catch (e: Exception) {
            snackbarCoroutineScope.launch {
                println(e.toString())
                snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)) {

        val (addressFocus, dbNameFocus, usernameFocus, passwordFocus, rootCAPathFocus)
        = listOf(FocusRequester(), FocusRequester(), FocusRequester(), FocusRequester(), FocusRequester())

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TextField(value = form.serverAddress, onValueChange = { value ->
                form.serverAddress = value // TODO: fetch DBs on blur, or on change with debounce time (300ms)
                tryCreateClient()
            }, label = { Text("Server address") }, modifier = Modifier
                .width(210.dp)
                .focusRequester(addressFocus)
                .onPreviewKeyEvent { e: KeyEvent ->
                    val event = e.nativeKeyEvent
                    if (event.id == KEY_RELEASED) return@onPreviewKeyEvent true
                    when (e.key) {
                        Key.Tab -> {
                            usernameFocus.requestFocus()
                            return@onPreviewKeyEvent true
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TextField(value = form.username, onValueChange = { value ->
                form.username = value
                tryCreateClient()
            }, label = { Text("Username") }, modifier = Modifier
                .width(210.dp)
                .focusRequester(usernameFocus)
                .onPreviewKeyEvent { e: KeyEvent ->
                    val event = e.nativeKeyEvent
                    if (event.id == KEY_RELEASED) return@onPreviewKeyEvent true
                    when (e.key) {
                        Key.Tab -> {
                            passwordFocus.requestFocus()
                            return@onPreviewKeyEvent true
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                })

            TextField(value = form.password, onValueChange = { value ->
                form.password = value
                tryCreateClient()
            }, label = { Text("Password") }, modifier = Modifier
                .width(210.dp)
                .focusRequester(passwordFocus)
                .onPreviewKeyEvent { e: KeyEvent ->
                    val event = e.nativeKeyEvent
                    if (event.id == KEY_RELEASED) return@onPreviewKeyEvent true
                    when (e.key) {
                        Key.Tab -> {
                            rootCAPathFocus.requestFocus()
                            return@onPreviewKeyEvent true
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TextField(value = form.rootCAPath, onValueChange = { value ->
                form.rootCAPath = value
                tryCreateClient()
            }, label = { Text("Root CA path") }, modifier = Modifier
                .width(210.dp)
                .focusRequester(rootCAPathFocus)
                .onPreviewKeyEvent { e: KeyEvent ->
                    val event = e.nativeKeyEvent
                    if (event.id == KEY_RELEASED) return@onPreviewKeyEvent true
                    when (e.key) {
                        Key.Tab -> {
                            dbNameFocus.requestFocus()
                            return@onPreviewKeyEvent true
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                })

            TextField(value = form.dbName, onValueChange = {
                form.dbName = it
                form.db = form.dbClient?.let { client -> DB(client, form.dbName) }
            }, label = { Text("Database") }, singleLine = true, modifier = Modifier
                .width(210.dp)
                .focusRequester(dbNameFocus)
                .onPreviewKeyEvent { e: KeyEvent ->
                    val event = e.nativeKeyEvent
                    if (event.id == KEY_RELEASED) return@onPreviewKeyEvent true
                    when (e.key) {
                        Key.Tab -> {
                            return@onPreviewKeyEvent true
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                })
        }

        Button(enabled = form.db != null, onClick = {
            navigator.pushState(WorkspaceScreenState(db = requireNotNull(form.db)))
        }) {
            Text("Connect to TypeDB Cluster")
        }

        LaunchedEffect(Unit) {
            addressFocus.requestFocus()
        }
    }
}
