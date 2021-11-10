/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.data.ClusterClient
import com.vaticle.typedb.studio.data.CoreClient
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.diagnostics.rememberErrorHandler
import com.vaticle.typedb.studio.diagnostics.withUnexpectedErrorHandling
import com.vaticle.typedb.studio.login.ServerSoftware.CLUSTER
import com.vaticle.typedb.studio.login.ServerSoftware.CORE
import com.vaticle.typedb.studio.routing.LoginRoute
import com.vaticle.typedb.studio.routing.Router
import com.vaticle.typedb.studio.routing.WorkspaceRoute
import com.vaticle.typedb.studio.ui.elements.StudioTab
import com.vaticle.typedb.studio.ui.elements.StudioTabs
import java.util.concurrent.CompletableFuture
import mu.KotlinLogging.logger

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(routeData: LoginRoute, snackbarHostState: SnackbarHostState) {
    val log = remember { logger {} }
    val snackbarCoroutineScope = rememberCoroutineScope()
    var errorHandler = rememberErrorHandler(log, snackbarHostState, snackbarCoroutineScope)
    val form = remember { loginScreenStateOf(routeData) }
    var databasesLastLoadedFromAddress by remember { mutableStateOf<String?>(null) }
    var databasesLastLoadedAtMillis by remember { mutableStateOf<Long?>(null) }
    var loadingDatabases by remember { mutableStateOf(false) }

    fun selectServerSoftware(software: ServerSoftware) {
        if (form.serverSoftware == software) return
        form.serverSoftware = software
        form.clearDBList()
        form.closeClient()
        databasesLastLoadedFromAddress = null
        databasesLastLoadedAtMillis = null
    }

    fun populateDBListAsync() {
        CompletableFuture.supplyAsync {
            try {
                val client = when (form.serverSoftware) {
                    CORE -> CoreClient(form.serverAddress)
                    CLUSTER -> ClusterClient(form.serverAddress, form.username, form.password, form.rootCAPath)
                }
                form.dbClient = client
                form.allDBNames.let { dbNames ->
                    dbNames += client.listDatabases()
                    if (dbNames.isEmpty()) form.dbFieldText = "This server has no databases"
                    else if (!form.databaseSelected) form.dbFieldText = "Select a database"
                }
            } catch (e: Exception) {
                databasesLastLoadedFromAddress = null
                form.dbFieldText = "Failed to load databases"
                errorHandler.handleError(e, { "Failed to load databases at address ${form.serverAddress}" })
            } finally {
                loadingDatabases = false
            }
        }
    }

    fun onDatabaseDropdownFocused() {
        if (loadingDatabases) return
        val lastLoadedMillis = databasesLastLoadedAtMillis
        if (form.serverAddress == databasesLastLoadedFromAddress
            && lastLoadedMillis != null && System.currentTimeMillis() - lastLoadedMillis < 2000
        ) return

        loadingDatabases = true
        databasesLastLoadedFromAddress = form.serverAddress
        databasesLastLoadedAtMillis = System.currentTimeMillis()
        form.closeClient()
        if (form.serverAddress.isNotBlank()) {
            if (!form.databaseSelected) form.dbFieldText = "Loading databases..."
            form.allDBNames.clear()
            populateDBListAsync()
        }
    }

    fun onSelectDatabase(dbName: String) {
        form.dbClient.let {
            if (it != null) {
                form.dbFieldText = dbName
                form.db = DB(it, dbName)
            } else {
                form.db = null
                form.clearDBList()
            }
        }
    }

    fun onSubmit() {
        withUnexpectedErrorHandling(
            errorHandler,
            { "Failed to login to ${form.serverSoftware.displayName}:${form.db?.name}" }) {
            val submission = form.asSubmission()
            Router.navigateTo(WorkspaceRoute(submission))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(StudioTheme.colors.windowBackdrop)
            .border(1.dp, StudioTheme.colors.uiElementBorder), contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.size(400.dp, 320.dp).background(StudioTheme.colors.background)
                .border(1.dp, StudioTheme.colors.uiElementBorder)
        ) {

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
                CORE -> CoreLoginPanel(form, ::onDatabaseDropdownFocused, ::onSelectDatabase, ::onSubmit)
                CLUSTER -> ClusterLoginPanel(form, ::onDatabaseDropdownFocused, ::onSelectDatabase, ::onSubmit)
            }
        }
    }
}

internal val ColumnScope.labelWeightModifier: Modifier
    get() = Modifier.weight(2f)

internal val ColumnScope.fieldWeightModifier: Modifier
    get() = Modifier.weight(3f)
