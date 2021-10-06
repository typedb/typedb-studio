package com.vaticle.typedb.studio.login

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(form: LoginScreenState, navigator: Navigator, snackbarHostState: SnackbarHostState) {

    val snackbarCoroutineScope = rememberCoroutineScope()
    val (addressFocus, dbNameFocus) = listOf(FocusRequester(), FocusRequester())

    Box(modifier = Modifier.fillMaxSize().background(StudioTheme.colors.windowBackdrop), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(480.dp, 360.dp).background(StudioTheme.colors.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {

                TextField(value = form.serverAddress, onValueChange = { value ->
                    form.serverAddress = value // TODO: fetch DBs on blur, or on change with debounce time (300ms)
                    try {
                        form.dbClient.close()
                        form.dbClient = DBClient(value)
                        form.db = DB(form.dbClient, form.dbName)
                    } catch (e: Exception) {
                        snackbarCoroutineScope.launch {
                            println(e.toString())
                            snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
                        }
                    }
                }, label = { Text("Server address") }, modifier = Modifier
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
                    form.db = DB(form.dbClient, form.dbName)
                }, label = { Text("Database") }, singleLine = true, modifier = Modifier
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
            }
        }
    }

    LaunchedEffect(Unit) {
        addressFocus.requestFocus()
    }
}
