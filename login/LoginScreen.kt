package com.vaticle.typedb.studio.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.client.common.exception.ErrorMessage
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.data.ClusterClient
import com.vaticle.typedb.studio.data.CoreClient
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.login.ServerSoftware.*
import com.vaticle.typedb.studio.navigation.LoginScreenState
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import com.vaticle.typedb.studio.ui.elements.StudioTab
import com.vaticle.typedb.studio.ui.elements.StudioTabs
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(form: LoginScreenState, navigator: Navigator, snackbarHostState: SnackbarHostState) {
    val snackbarCoroutineScope = rememberCoroutineScope()
    var selectedServerSoftware by remember { mutableStateOf(CORE) }
    var lastCheckedAddress by remember { mutableStateOf("") }
    var loadingDatabases by remember { mutableStateOf(false) }

    fun loadDatabases() {
        if (form.serverAddress == lastCheckedAddress || loadingDatabases) return
        loadingDatabases = true
        form.dbClient?.close()
        if (form.serverAddress.isNotBlank()) {
            if (!form.databaseSelected) form.dbName = "Loading databases..."
            val client = when (selectedServerSoftware) {
                CORE -> CoreClient(form.serverAddress)
                CLUSTER -> ClusterClient(form.serverAddress, form.username, form.password, form.rootCAPath)
            }
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

    fun selectDatabase(value: String) {
        form.dbName = value
        form.db = form.dbClient?.let { client -> DB(client, form.dbName) }
        form.databaseSelected = true
    }

    fun onSubmit() {
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
    }

    Box(modifier = Modifier.fillMaxSize().background(StudioTheme.colors.windowBackdrop)
        .border(1.dp, StudioTheme.colors.uiElementBorder), contentAlignment = Alignment.Center) {

        Column(modifier = Modifier.size(400.dp, 320.dp).background(StudioTheme.colors.background)
            .border(1.dp, StudioTheme.colors.uiElementBorder)) {

            StudioTabs(Modifier.height(24.dp)) {
                StudioTab("TypeDB", selected = selectedServerSoftware == CORE,
                    arrangement = Arrangement.Center, textStyle = StudioTheme.typography.body1,
                    modifier = Modifier.weight(1f).clickable { selectedServerSoftware = CORE })
                StudioTab("TypeDB Cluster", selected = selectedServerSoftware == CLUSTER,
                    arrangement = Arrangement.Center, textStyle = StudioTheme.typography.body1,
                    modifier = Modifier.weight(1f).clickable { selectedServerSoftware = CLUSTER })
            }

            Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

            when (selectedServerSoftware) {
                CORE -> CoreLoginPanel(form, ::loadDatabases, ::selectDatabase, ::onSubmit)
                CLUSTER -> ClusterLoginPanel(form, ::loadDatabases, ::selectDatabase, ::onSubmit)
            }
        }
    }
}

enum class ServerSoftware {
    CORE,
    CLUSTER
}

internal val ColumnScope.labelWeightModifier: Modifier
    get() = Modifier.weight(2f)

internal val ColumnScope.fieldWeightModifier: Modifier
    get() = Modifier.weight(3f)
