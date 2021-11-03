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
import com.vaticle.typedb.studio.navigation.LoginScreenState
import com.vaticle.typedb.studio.navigation.Navigator
import com.vaticle.typedb.studio.navigation.ServerSoftware
import com.vaticle.typedb.studio.navigation.ServerSoftware.*
import com.vaticle.typedb.studio.navigation.WorkspaceScreenState
import com.vaticle.typedb.studio.ui.elements.StudioTab
import com.vaticle.typedb.studio.ui.elements.StudioTabs
import kotlinx.coroutines.launch
import mu.KotlinLogging.logger
import java.util.concurrent.CompletableFuture

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(form: LoginScreenState, navigator: Navigator, snackbarHostState: SnackbarHostState) {
    val log = remember { logger {} }
    val snackbarCoroutineScope = rememberCoroutineScope()
    var lastCheckedAddress by remember { mutableStateOf("") }
    var loadingDatabases by remember { mutableStateOf(false) }

    fun selectServerSoftware(software: ServerSoftware) {
        form.serverSoftware = software
        form.clearDBList()
        form.closeClient()
        lastCheckedAddress = ""
    }

    fun loadDatabases() {
        if (form.serverAddress == lastCheckedAddress || loadingDatabases) return
        loadingDatabases = true
        lastCheckedAddress = form.serverAddress
        form.closeClient()
        if (form.serverAddress.isNotBlank()) {
            if (!form.databaseSelected) form.dbFieldText = "Loading databases..."
            form.allDBNames.clear()
            CompletableFuture.supplyAsync {
                try {
                    val client = when (form.serverSoftware) {
                        CORE -> CoreClient(form.serverAddress)
                        CLUSTER -> ClusterClient(form.serverAddress, form.username, form.password, form.rootCAPath)
                    }
                    form.dbClient = client
                    form.allDBNames.let { dbNames ->
                        dbNames += client.listDatabases()
                        lastCheckedAddress = form.serverAddress
                        if (dbNames.isEmpty()) form.dbFieldText = "This server has no databases"
                        else if (!form.databaseSelected) form.dbFieldText = "Select a database"
                    }
                } catch (e: Exception) {
                    lastCheckedAddress = ""
                    form.dbFieldText = "Failed to load databases"
                    snackbarCoroutineScope.launch {
                        log.error(e) { "Failed to load databases at address ${form.serverAddress}" }
                        snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
                    }
                } finally {
                    loadingDatabases = false
                }
            }
        }
    }

    fun selectDatabase(dbName: String) {
        form.dbClient.let {
            if (it != null) {
                form.dbFieldText = dbName
                form.db = DB(it, dbName)
            } else {
                form.db = null
                form.clearDBList()
                lastCheckedAddress = ""
            }
        }
    }

    fun onSubmit() {
        try {
            val dbClient = requireNotNull(form.dbClient)
            val db = requireNotNull(form.db)
            if (dbClient.containsDatabase(db.name)) navigator.pushState(WorkspaceScreenState(form))
            else throw TypeDBClientException(ErrorMessage.Client.DB_DOES_NOT_EXIST, db.name)
        } catch (e: Exception) {
            snackbarCoroutineScope.launch {
                log.error(e) { "Failed to login to ${form.serverSoftware.displayName}:${form.db?.name}" }
                snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", SnackbarDuration.Long)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(StudioTheme.colors.windowBackdrop)
        .border(1.dp, StudioTheme.colors.uiElementBorder), contentAlignment = Alignment.Center) {

        Column(modifier = Modifier.size(400.dp, 320.dp).background(StudioTheme.colors.background)
            .border(1.dp, StudioTheme.colors.uiElementBorder)) {

            StudioTabs(Modifier.height(24.dp)) {
                StudioTab(text = CORE.displayName, selected = form.serverSoftware == CORE,
                    arrangement = Arrangement.Center, textStyle = StudioTheme.typography.body1,
                    modifier = Modifier.weight(1f).clickable { selectServerSoftware(CORE) })
                StudioTab(text = CLUSTER.displayName, selected = form.serverSoftware == CLUSTER,
                    arrangement = Arrangement.Center, textStyle = StudioTheme.typography.body1,
                    modifier = Modifier.weight(1f).clickable { selectServerSoftware(CLUSTER) })
            }

            Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

            when (form.serverSoftware) {
                CORE -> CoreLoginPanel(form, ::loadDatabases, ::selectDatabase, ::onSubmit)
                CLUSTER -> ClusterLoginPanel(form, ::loadDatabases, ::selectDatabase, ::onSubmit)
            }
        }
    }
}

internal val ColumnScope.labelWeightModifier: Modifier
    get() = Modifier.weight(2f)

internal val ColumnScope.fieldWeightModifier: Modifier
    get() = Modifier.weight(3f)
