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

package com.vaticle.typedb.studio.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.diagnostics.rememberErrorReporter
import com.vaticle.typedb.studio.connection.ServerSoftware.CLUSTER
import com.vaticle.typedb.studio.connection.ServerSoftware.CORE
import com.vaticle.typedb.studio.routing.ConnectionRoute
import com.vaticle.typedb.studio.ui.elements.StudioButton
import com.vaticle.typedb.studio.ui.elements.StudioDropdownBox
import com.vaticle.typedb.studio.ui.elements.StudioTab
import com.vaticle.typedb.studio.ui.elements.StudioTabs
import com.vaticle.typedb.studio.ui.elements.StudioTextField
import mu.KotlinLogging

object ConnectionScreen {

    private val ColumnScope.labelWeightModifier: Modifier get() = Modifier.weight(2f)
    private val ColumnScope.fieldWeightModifier: Modifier get() = Modifier.weight(3f)

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Main(routeData: ConnectionRoute, snackbarHostState: SnackbarHostState) {
        val snackbarCoroutineScope = rememberCoroutineScope()
        val log = remember { KotlinLogging.logger {} }
        val errorReporter = rememberErrorReporter(log, snackbarHostState, snackbarCoroutineScope)
        var databasesLastLoadedFromAddress by remember { mutableStateOf<String?>(null) }
        var databasesLastLoadedAtMillis by remember { mutableStateOf<Long?>(null) }
        var loadingDatabases by remember { mutableStateOf(false) }
        val form = remember {
            connectionScreenStateOf(
                routeData,
                errorReporter,
                databasesLastLoadedFromAddress,
                databasesLastLoadedAtMillis,
                loadingDatabases
            )
        }

        Box(
            modifier = Modifier.fillMaxSize().background(StudioTheme.colors.windowBackdrop)
                .border(1.dp, StudioTheme.colors.uiElementBorder), contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier.size(400.dp, 320.dp).background(StudioTheme.colors.background)
                    .border(1.dp, StudioTheme.colors.uiElementBorder)
            ) {

                StudioTabs(Modifier.height(32.dp)) {
                    StudioTab(text = CORE.displayName, selected = form.serverSoftware == CORE,
                        arrangement = Arrangement.Center, textStyle = StudioTheme.typography.body1,
                        modifier = Modifier.weight(1f).clickable { form.selectServerSoftware(CORE) })
                    StudioTab(text = CLUSTER.displayName, selected = form.serverSoftware == CLUSTER,
                        arrangement = Arrangement.Center, textStyle = StudioTheme.typography.body1,
                        modifier = Modifier.weight(1f).clickable { form.selectServerSoftware(CLUSTER) })
                }
                Spacer(
                    modifier = Modifier.fillMaxWidth().height(2.dp).background(StudioTheme.colors.backgroundHighlight)
                )
                when (form.serverSoftware) {
                    CORE -> TypeDBPanel(form, form::onDatabaseDropdown, form::onDatabaseSelected, form::onSubmit)
                    CLUSTER -> TypeDBClusterPanel(form, form::onDatabaseDropdown, form::onDatabaseSelected, form::onSubmit)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun TypeDBPanel(
        form: ConnectionState, onDBDropdown: () -> Unit, onDBSelected: (dbName: String) -> Unit, onSubmit: () -> Unit
    ) {
        val focusManager: FocusManager = LocalFocusManager.current
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FormFieldGroup {
                FormField {
                    Text("Server address", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                    StudioTextField(
                        value = form.serverAddress, onValueChange = { value -> form.serverAddress = value },
                        textStyle = StudioTheme.typography.body1, modifier = fieldWeightModifier.height(28.dp)
                    )
                }
                FormField {
                    Text("Database", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                    StudioDropdownBox(
                        items = form.allDBNames,
                        text = form.dbFieldText,
                        onTextChange = { onDBSelected(it) },
                        modifier = fieldWeightModifier.height(28.dp),
                        textFieldModifier = Modifier.onFocusChanged {
                            // sanity check - for some reason onFocusChanged triggers when switching tabs
                            if (it.isFocused && form.serverSoftware == CORE) onDBDropdown()
                        })
                }
            }
            StudioButton(text = "Connect to TypeDB", enabled = form.databaseSelected, onClick = { onSubmit() })
            LaunchedEffect(Unit) { focusManager.moveFocus(FocusDirection.Down) }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun TypeDBClusterPanel(
        form: ConnectionState, onDBDropdown: () -> Unit, onDBSelected: (dbName: String) -> Unit, onSubmit: () -> Unit
    ) {
        val focusManager: FocusManager = LocalFocusManager.current

        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            FormFieldGroup {
                FormField {
                    Text("Server address", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                    StudioTextField(
                        value = form.serverAddress, onValueChange = { value -> form.serverAddress = value },
                        textStyle = StudioTheme.typography.body1, modifier = fieldWeightModifier.height(28.dp)
                    )
                }
                FormField {
                    Text("Username", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                    StudioTextField(
                        value = form.username, onValueChange = { value -> form.username = value },
                        textStyle = StudioTheme.typography.body1, modifier = fieldWeightModifier.height(28.dp)
                    )
                }
                FormField {
                    Text("Password", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                    StudioTextField(
                        value = form.password, onValueChange = { value -> form.password = value },
                        textStyle = StudioTheme.typography.body1, modifier = fieldWeightModifier.height(28.dp),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
                FormField {
                    Text("Root CA path", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                    StudioTextField(
                        value = form.rootCAPath, onValueChange = { value -> form.rootCAPath = value },
                        textStyle = StudioTheme.typography.body1, modifier = fieldWeightModifier.height(28.dp)
                    )
                }
                FormField {
                    Text("Database", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                    StudioDropdownBox(items = form.allDBNames,
                        text = form.dbFieldText,
                        onTextChange = { onDBSelected(it) },
                        modifier = fieldWeightModifier.height(28.dp),
                        textFieldModifier = Modifier.onFocusChanged { if (it.isFocused && form.serverSoftware == CLUSTER) onDBDropdown() })
                }
            }

            StudioButton(text = "Connect to TypeDB Cluster", enabled = form.databaseSelected, onClick = { onSubmit() })

            LaunchedEffect(Unit) { focusManager.moveFocus(FocusDirection.Down) }
        }
    }

    @Composable
    private fun FormField(content: @Composable () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }

    @Composable
    private fun FormFieldGroup(content: @Composable () -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}
