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

package com.vaticle.typedb.studio.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.state.Controller
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.common.Property.Server.TYPEDB
import com.vaticle.typedb.studio.state.common.Property.Server.TYPEDB_CLUSTER
import com.vaticle.typedb.studio.state.connection.Connection.Status.CONNECTED
import com.vaticle.typedb.studio.state.connection.Connection.Status.CONNECTING
import com.vaticle.typedb.studio.state.connection.Connection.Status.DISCONNECTED
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form.Checkbox
import com.vaticle.typedb.studio.view.common.component.Form.ComponentSpacer
import com.vaticle.typedb.studio.view.common.component.Form.Dropdown
import com.vaticle.typedb.studio.view.common.component.Form.Field
import com.vaticle.typedb.studio.view.common.component.Form.Submission
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Form.TextInput
import com.vaticle.typedb.studio.view.common.theme.Theme

object Connection {

    private val WINDOW_WIDTH = 500.dp
    private val WINDOW_HEIGHT = 340.dp

    private object FormState {
        // We keep this static to maintain the values through application lifetime,
        // and easily accessible to all functions in this object without being passed around

        var server: Property.Server by mutableStateOf(TYPEDB)
        var address: String by mutableStateOf("")
        var username: String by mutableStateOf("")
        var password: String by mutableStateOf("")
        var tlsEnabled: Boolean by mutableStateOf(false)
        var caCertificate: String by mutableStateOf("")

        fun isValid(): Boolean {
            return when (com.vaticle.typedb.studio.view.Connection.FormState.server) {
                TYPEDB -> !com.vaticle.typedb.studio.view.Connection.FormState.address.isBlank()
                TYPEDB_CLUSTER -> !(com.vaticle.typedb.studio.view.Connection.FormState.address.isBlank() || com.vaticle.typedb.studio.view.Connection.FormState.username.isBlank() || com.vaticle.typedb.studio.view.Connection.FormState.password.isBlank())
            }
        }

        fun trySubmitIfValid() {
            if (com.vaticle.typedb.studio.view.Connection.FormState.isValid()) com.vaticle.typedb.studio.view.Connection.FormState.trySubmit()
        }

        fun trySubmit() {
            when (com.vaticle.typedb.studio.view.Connection.FormState.server) {
                TYPEDB -> Controller.connection.tryConnectToTypeDB(com.vaticle.typedb.studio.view.Connection.FormState.address)
                TYPEDB_CLUSTER -> when {
                    com.vaticle.typedb.studio.view.Connection.FormState.caCertificate.isBlank() -> Controller.connection.tryConnectToTypeDBCluster(
                        com.vaticle.typedb.studio.view.Connection.FormState.address,
                        com.vaticle.typedb.studio.view.Connection.FormState.username,
                        com.vaticle.typedb.studio.view.Connection.FormState.password,
                        com.vaticle.typedb.studio.view.Connection.FormState.tlsEnabled
                    )
                    else -> Controller.connection.tryConnectToTypeDBCluster(
                        com.vaticle.typedb.studio.view.Connection.FormState.address,
                        com.vaticle.typedb.studio.view.Connection.FormState.username,
                        com.vaticle.typedb.studio.view.Connection.FormState.password,
                        com.vaticle.typedb.studio.view.Connection.FormState.caCertificate
                    )
                }
            }
        }
    }

    @Composable
    fun Window() {
        Window(
            title = Label.CONNECT_TO_TYPEDB,
            onCloseRequest = { Controller.connection.showWindow = false },
            alwaysOnTop = true,
            state = rememberWindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(
                    com.vaticle.typedb.studio.view.Connection.WINDOW_WIDTH,
                    com.vaticle.typedb.studio.view.Connection.WINDOW_HEIGHT
                )
            )
        ) {
            Submission(onSubmit = { com.vaticle.typedb.studio.view.Connection.FormState.trySubmitIfValid() }) {
                com.vaticle.typedb.studio.view.Connection.ServerFormField()
                com.vaticle.typedb.studio.view.Connection.AddressFormField()
                if (com.vaticle.typedb.studio.view.Connection.FormState.server == TYPEDB_CLUSTER) {
                    com.vaticle.typedb.studio.view.Connection.UsernameFormField()
                    com.vaticle.typedb.studio.view.Connection.PasswordFormField()
                    com.vaticle.typedb.studio.view.Connection.TLSEnabledFormField()
                    if (com.vaticle.typedb.studio.view.Connection.FormState.tlsEnabled) com.vaticle.typedb.studio.view.Connection.CACertificateFormField()
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    com.vaticle.typedb.studio.view.Connection.ServerConnectionStatus()
                    Spacer(modifier = Modifier.weight(1f))
                    when (Controller.connection.status) {
                        DISCONNECTED -> com.vaticle.typedb.studio.view.Connection.DisconnectedFormButtons()
                        CONNECTED -> com.vaticle.typedb.studio.view.Connection.ConnectedFormButtons()
                        CONNECTING -> com.vaticle.typedb.studio.view.Connection.ConnectingFormButtons()
                    }
                }
            }
        }
    }

    @Composable
    private fun ServerFormField() {
        Field(label = Label.SERVER) {
            Dropdown(
                values = Property.Server.values().toList(),
                selected = com.vaticle.typedb.studio.view.Connection.FormState.server,
                onSelection = { com.vaticle.typedb.studio.view.Connection.FormState.server = it },
                enabled = Controller.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun AddressFormField() {
        Field(label = Label.ADDRESS) {
            TextInput(
                value = com.vaticle.typedb.studio.view.Connection.FormState.address,
                placeholder = Property.DEFAULT_SERVER_ADDRESS,
                onValueChange = { com.vaticle.typedb.studio.view.Connection.FormState.address = it },
                enabled = Controller.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun UsernameFormField() {
        Field(label = Label.USERNAME) {
            TextInput(
                value = com.vaticle.typedb.studio.view.Connection.FormState.username,
                placeholder = Label.USERNAME.lowercase(),
                onValueChange = { com.vaticle.typedb.studio.view.Connection.FormState.username = it },
                enabled = Controller.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun PasswordFormField() {
        Field(label = Label.PASSWORD) {
            TextInput(
                value = com.vaticle.typedb.studio.view.Connection.FormState.password,
                placeholder = Label.PASSWORD.lowercase(),
                onValueChange = { com.vaticle.typedb.studio.view.Connection.FormState.password = it },
                enabled = Controller.connection.isDisconnected(),
                isPassword = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun TLSEnabledFormField() {
        Field(label = Label.ENABLE_TLS) {
            Checkbox(
                value = com.vaticle.typedb.studio.view.Connection.FormState.tlsEnabled,
                onChange = { com.vaticle.typedb.studio.view.Connection.FormState.tlsEnabled = it },
                enabled = Controller.connection.isDisconnected(),
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun CACertificateFormField() {
        Field(label = Label.CA_CERTIFICATE) {
            TextInput(
                value = com.vaticle.typedb.studio.view.Connection.FormState.caCertificate,
                placeholder = "${Label.PATH_TO_CA_CERTIFICATE} (${Label.OPTIONAL.lowercase()})",
                onValueChange = { com.vaticle.typedb.studio.view.Connection.FormState.caCertificate = it },
                enabled = Controller.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun ServerConnectionStatus() {
        val statusText = "${Label.STATUS}: ${Controller.connection.status.name.lowercase()}"
        Text(
            value = statusText, color = when (Controller.connection.status) {
                DISCONNECTED -> Theme.colors.error2
                CONNECTING -> Theme.colors.quaternary
                CONNECTED -> Theme.colors.secondary
            }
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun DisconnectedFormButtons() {
        TextButton(text = Label.CANCEL, onClick = { Controller.connection.showWindow = false })
        ComponentSpacer()
        TextButton(text = Label.CONNECT, enabled = com.vaticle.typedb.studio.view.Connection.FormState.isValid(), onClick = { com.vaticle.typedb.studio.view.Connection.FormState.trySubmit() })
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ConnectedFormButtons() {
        TextButton(
            text = Label.DISCONNECT,
            onClick = { Controller.connection.disconnect() },
            textColor = Theme.colors.error2
        )
        ComponentSpacer()
        TextButton(text = Label.CLOSE, onClick = { Controller.connection.showWindow = false })
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ConnectingFormButtons() {
        TextButton(text = Label.CANCEL, onClick = { Controller.connection.disconnect() })
        ComponentSpacer()
        TextButton(text = Label.CONNECTING, onClick = {}, enabled = false)
    }
}
