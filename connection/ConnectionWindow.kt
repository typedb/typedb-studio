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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.common.Label
import com.vaticle.typedb.studio.common.Property
import com.vaticle.typedb.studio.common.Property.Server.TYPEDB
import com.vaticle.typedb.studio.common.Property.Server.TYPEDB_CLUSTER
import com.vaticle.typedb.studio.common.component.Form
import com.vaticle.typedb.studio.common.component.Form.Button
import com.vaticle.typedb.studio.common.component.Form.Dropdown
import com.vaticle.typedb.studio.common.component.Form.Text
import com.vaticle.typedb.studio.common.component.Form.TextInput
import com.vaticle.typedb.studio.common.theme.Theme
import com.vaticle.typedb.studio.service.ConnectionService
import com.vaticle.typedb.studio.service.ConnectionService.Status.CONNECTED
import com.vaticle.typedb.studio.service.ConnectionService.Status.CONNECTING
import com.vaticle.typedb.studio.service.ConnectionService.Status.DISCONNECTED
import com.vaticle.typedb.studio.service.Service

object ConnectionWindow {

    private val FORM_SPACING = 12.dp
    private val WINDOW_WIDTH = 500.dp
    private val WINDOW_HEIGHT = 300.dp

    private object State {
        // We keep this static to maintain the values through application lifetime,
        // and easily accessible to all functions in this object without being passed around

        var server: Property.Server by mutableStateOf(TYPEDB)
        var address: String by mutableStateOf("")
        var username: String by mutableStateOf("")
        var password: String by mutableStateOf("")
        var caCertificate: String by mutableStateOf("")

        fun trySubmit() {
            when (server) {
                TYPEDB -> Service.connection.tryConnectToTypeDB(address)
                TYPEDB_CLUSTER -> Service.connection.tryConnectToTypeDBCluster(
                    address, username, password, caCertificate
                )
            }
        }
    }

    @Composable
    fun Layout() {
        Window(
            title = Label.CONNECT_TO_TYPEDB,
            onCloseRequest = { Service.connection.showWindow = false },
            alwaysOnTop = true,
            state = rememberWindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition.Aligned(Alignment.Center),
                size = WindowSize(WINDOW_WIDTH, WINDOW_HEIGHT)
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Theme.colors.background).padding(FORM_SPACING)) {
                Form.FieldGroup {
                    ServerFormField()
                    AddressFormField()
                    if (State.server == TYPEDB_CLUSTER) {
                        UsernameFormField()
                        PasswordFormField()
                        CACertificateFormField()
                    }
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.Bottom) {
                        ServerConnectionStatus()
                        Spacer(modifier = Modifier.weight(1f))
                        when (Service.connection.status) {
                            DISCONNECTED -> DisconnectedFormButtons()
                            CONNECTED -> ConnectedFormButtons()
                            CONNECTING -> ConnectingFormButtons()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ServerFormField() {
        Form.Field(label = Label.SERVER) {
            Dropdown(
                values = Property.Server.values().toList(),
                selected = State.server,
                onSelection = { State.server = it },
                enabled = Service.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun AddressFormField() {
        Form.Field(label = Label.ADDRESS) {
            TextInput(
                value = State.address,
                placeholder = Property.DEFAULT_SERVER_ADDRESS,
                onValueChange = { State.address = it },
                enabled = Service.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun UsernameFormField() {
        Form.Field(label = Label.USERNAME) {
            TextInput(
                value = State.username,
                placeholder = Label.USERNAME.lowercase(),
                onValueChange = { State.username = it },
                enabled = Service.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun PasswordFormField() {
        Form.Field(label = Label.PASSWORD) {
            TextInput(
                value = State.password,
                placeholder = Label.PASSWORD.lowercase(),
                onValueChange = { State.password = it },
                enabled = Service.connection.isDisconnected(),
                isPassword = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun CACertificateFormField() {
        Form.Field(label = Label.CA_CERTIFICATE) {
            TextInput(
                value = State.caCertificate,
                placeholder = Label.PATH_TO_CA_CERTIFICATE,
                onValueChange = { State.caCertificate = it },
                enabled = Service.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun ServerConnectionStatus() {
        val statusText = "${Label.STATUS}: ${Service.connection.status.name.lowercase()}"
        Text(value = statusText, color = colorOf(Service.connection.status))
    }

    @Composable
    private fun colorOf(status: ConnectionService.Status): Color {
        return when (status) {
            DISCONNECTED -> Theme.colors.error2
            CONNECTING, CONNECTED -> Theme.colors.secondary
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun DisconnectedFormButtons() {
        Button(text = Label.CANCEL, onClick = { Service.connection.showWindow = false })
        Spacer(modifier = Modifier.width(FORM_SPACING))
        Button(text = Label.CONNECT, onClick = { State.trySubmit() })
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ConnectedFormButtons() {
        Button(text = Label.DISCONNECT, onClick = { Service.connection.disconnect() })
        Spacer(modifier = Modifier.width(FORM_SPACING))
        Button(text = Label.CLOSE, onClick = { Service.connection.showWindow = false })
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ConnectingFormButtons() {
        Button(text = Label.CANCEL, onClick = { Service.connection.disconnect() })
        Spacer(modifier = Modifier.width(FORM_SPACING))
        Button(text = Label.CONNECTING, onClick = {}, enabled = false)
    }
}
