package com.vaticle.typedb.studio.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.client.common.exception.ErrorMessage
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.data.CoreClient
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.data.DBClient
import com.vaticle.typedb.studio.navigation.LoginScreenState
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import com.vaticle.typedb.studio.ui.elements.StudioButton
import com.vaticle.typedb.studio.ui.elements.StudioDropdownBox
import com.vaticle.typedb.studio.ui.elements.StudioTextField
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CoreLoginPanel(form: LoginScreenState, navigator: Navigator, snackbarHostState: SnackbarHostState) {
    val snackbarCoroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var lastCheckedAddress by remember { mutableStateOf("") }
    var addressFocused by remember { mutableStateOf(false) }
    var loadingDatabases by remember { mutableStateOf(false) }

    fun loadDatabases() {
        if (form.serverAddress == lastCheckedAddress || loadingDatabases) return
        loadingDatabases = true
        form.dbClient?.close()
        if (form.serverAddress.isNotBlank()) {
            if (!form.databaseSelected) form.dbName = "Loading databases..."
            val client = CoreClient(form.serverAddress)
            form.dbClient = client
            form.allDBNames.clear()
            CompletableFuture.supplyAsync {
                try {
                    form.allDBNames.let { dbNames ->
                        dbNames += client.listDatabases()
                        lastCheckedAddress = form.serverAddress
                        if (dbNames.isEmpty()) form.dbName = "This server has no databases"
                        else {
                            if (!form.databaseSelected) {
                                val dbName = dbNames[0]
                                form.dbName = dbName
                                form.db = DB(client, dbName)
                            }
                            form.databaseSelected = true
                        }
                    }
                } catch (e: Exception) {
                    lastCheckedAddress = ""
                    form.db = null
                    form.databaseSelected = false
                    form.dbName = "Failed to load databases"
                    snackbarCoroutineScope.launch {
                        println(e.toString())
                        snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
                    }
                } finally {
                    loadingDatabases = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)) {

        FormFieldGroup {
            FormField {
                Text("Server address", style = StudioTheme.typography.body1, modifier = Modifier.weight(2f))
                StudioTextField(value = form.serverAddress, onValueChange = { value -> form.serverAddress = value },
                    textStyle = StudioTheme.typography.body1, modifier = Modifier
                        .weight(3f)
                        .height(28.dp)
                        .onFocusChanged {
                            if (it.isFocused) addressFocused = true
                            else {
                                if (addressFocused) {
                                    addressFocused = false
                                    loadDatabases()
                                }
                            }
                        })
            }
            FormField {
                Text("Database", style = StudioTheme.typography.body1, modifier = Modifier.weight(2f))
                StudioDropdownBox(items = form.allDBNames, text = form.dbName, onTextChange = { value ->
                    form.dbName = value
                    form.db = form.dbClient?.let { client -> DB(client, form.dbName) }
                    form.databaseSelected = true
                }, modifier = Modifier.weight(3f).height(28.dp),
                    textFieldModifier = Modifier.onFocusChanged { if (it.isFocused) loadDatabases() })
            }
        }

        StudioButton(text = "Connect to TypeDB", enabled = form.databaseSelected,
            onClick = {
                val dbClient = requireNotNull(form.dbClient)
                val db = requireNotNull(form.db)

                if (dbClient.containsDatabase(db.name)) navigator.pushState(WorkspaceScreenState(db = db))
                else {
                    val e = TypeDBClientException(ErrorMessage.Client.DB_DOES_NOT_EXIST, db.name)
                    snackbarCoroutineScope.launch {
                        println(e.toString())
                        snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
                    }
                }
            })

        LaunchedEffect(Unit) {
            focusManager.moveFocus(FocusDirection.Down)
        }
    }
}
