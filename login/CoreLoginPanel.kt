package com.vaticle.typedb.studio.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.vaticle.typedb.studio.data.CoreClient
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.navigation.LoginScreenState
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
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
            snackbarCoroutineScope.launch {
                println(e.toString())
                snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)) {

        val (addressFocus, dbNameFocus) = listOf(FocusRequester(), FocusRequester())

        TextField(value = form.serverAddress, onValueChange = { value ->
            form.serverAddress = value // TODO: fetch DBs on blur, or on change with debounce time (300ms)
            tryCreateClient()
        }, label = { Text("Server address") }, modifier = Modifier
            .width(220.dp)
            .focusRequester(addressFocus)
            .onPreviewKeyEvent { event: KeyEvent ->
                when (event.key) {
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
            .width(220.dp)
            .focusRequester(dbNameFocus)
            .onPreviewKeyEvent { event: KeyEvent ->
                when (event.key) {
                    Key.Tab -> {
                        return@onPreviewKeyEvent true
                    }
                    Key.Enter -> {
                        form.db?.let { navigator.pushState(WorkspaceScreenState(db = it)) }
                        return@onPreviewKeyEvent true
                    }
                    else -> return@onPreviewKeyEvent false
                }
            })

        Button(enabled = form.db != null, onClick = {
            navigator.pushState(WorkspaceScreenState(db = requireNotNull(form.db)))
        }) {
            Text("Connect to TypeDB")
        }

        LaunchedEffect(Unit) {
            addressFocus.requestFocus()
            // TODO: this is overly aggressive and may trigger an error with no user action
            tryCreateClient()
        }
    }
}
