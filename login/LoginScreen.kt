package com.vaticle.typedb.studio.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.data.DBServer
import com.vaticle.typedb.studio.navigation.LoginScreenState
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(form: LoginScreenState, navigator: Navigator, snackbarHostState: SnackbarHostState) {

    val snackbarCoroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(StudioTheme.colors.windowBackdrop), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(480.dp, 360.dp).background(StudioTheme.colors.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {

                TextField(value = form.serverAddress, onValueChange = { value ->
                    form.serverAddress = value // TODO: fetch DBs on blur, or on change with debounce time (300ms)
                    try {
                        form.dbServer?.close()
                        form.dbServer = DBServer(value)
                        form.dbServer?.let { dbServer -> form.db = DB(dbServer = dbServer, dbName = form.dbName) }
                    } catch (e: Exception) {
                        snackbarCoroutineScope.launch {
                            println(e.toString())
                            snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
                        }
                    }
                }, label = { Text("Server address") })

                TextField(value = form.dbName, onValueChange = {
                    form.dbName = it
                    form.dbServer?.let { dbServer -> form.db = DB(dbServer = dbServer, dbName = form.dbName) }
                }, label = { Text("Database") })

                Button(enabled = form.db != null, onClick = {
                    navigator.pushState(WorkspaceScreenState(db = requireNotNull(form.db)))
                }) {
                    Text("Connect to TypeDB")
                }
            }
        }
    }
}
