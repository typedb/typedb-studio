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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.vaticle.typedb.studio.state.State
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
            return when (server) {
                TYPEDB -> !address.isBlank()
                TYPEDB_CLUSTER -> !(address.isBlank() || username.isBlank() || password.isBlank())
            }
        }

        fun trySubmitIfValid() {
            if (isValid()) trySubmit()
        }

        fun trySubmit() {
            when (server) {
                TYPEDB -> State.connection.tryConnectToTypeDB(address)
                TYPEDB_CLUSTER -> when {
                    caCertificate.isBlank() -> State.connection.tryConnectToTypeDBCluster(
                        address,
                        username,
                        password,
                        tlsEnabled
                    )
                    else -> State.connection.tryConnectToTypeDBCluster(
                        address,
                        username,
                        password,
                        caCertificate
                    )
                }
            }
        }
    }

    @Composable
    fun Dialog() {
        Dialog(
            title = Label.CONNECT_TO_TYPEDB,
            onCloseRequest = { State.connection.showWindow = false },
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(WINDOW_WIDTH, WINDOW_HEIGHT)
            )
        ) {
            Submission(onSubmit = { FormState.trySubmitIfValid() }) {
                ServerFormField()
                AddressFormField()
                if (FormState.server == TYPEDB_CLUSTER) {
                    UsernameFormField()
                    PasswordFormField()
                    TLSEnabledFormField()
                    if (FormState.tlsEnabled) CACertificateFormField()
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    ServerConnectionStatus()
                    Spacer(modifier = Modifier.weight(1f))
                    when (State.connection.status) {
                        DISCONNECTED -> DisconnectedFormButtons()
                        CONNECTED -> ConnectedFormButtons()
                        CONNECTING -> ConnectingFormButtons()
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
                selected = FormState.server,
                onSelection = { FormState.server = it },
                enabled = State.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun AddressFormField() {
        Field(label = Label.ADDRESS) {
            TextInput(
                value = FormState.address,
                placeholder = Property.DEFAULT_SERVER_ADDRESS,
                onValueChange = { FormState.address = it },
                enabled = State.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun UsernameFormField() {
        Field(label = Label.USERNAME) {
            TextInput(
                value = FormState.username,
                placeholder = Label.USERNAME.lowercase(),
                onValueChange = { FormState.username = it },
                enabled = State.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun PasswordFormField() {
        Field(label = Label.PASSWORD) {
            TextInput(
                value = FormState.password,
                placeholder = Label.PASSWORD.lowercase(),
                onValueChange = { FormState.password = it },
                enabled = State.connection.isDisconnected(),
                isPassword = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun TLSEnabledFormField() {
        Field(label = Label.ENABLE_TLS) {
            Checkbox(
                value = FormState.tlsEnabled,
                onChange = { FormState.tlsEnabled = it },
                enabled = State.connection.isDisconnected(),
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun CACertificateFormField() {
        Field(label = Label.CA_CERTIFICATE) {
            TextInput(
                value = FormState.caCertificate,
                placeholder = "${Label.PATH_TO_CA_CERTIFICATE} (${Label.OPTIONAL.lowercase()})",
                onValueChange = { FormState.caCertificate = it },
                enabled = State.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun ServerConnectionStatus() {
        val statusText = "${Label.STATUS}: ${State.connection.status.name.lowercase()}"
        Text(
            value = statusText, color = when (State.connection.status) {
                DISCONNECTED -> Theme.colors.error2
                CONNECTING -> Theme.colors.quaternary
                CONNECTED -> Theme.colors.secondary
            }
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun DisconnectedFormButtons() {
        TextButton(text = Label.CANCEL, onClick = { State.connection.showWindow = false })
        ComponentSpacer()
        TextButton(
            text = Label.CONNECT,
            enabled = FormState.isValid(),
            onClick = { FormState.trySubmit() })
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ConnectedFormButtons() {
        TextButton(
            text = Label.DISCONNECT,
            onClick = { State.connection.disconnect() },
            textColor = Theme.colors.error2
        )
        ComponentSpacer()
        TextButton(text = Label.CLOSE, onClick = { State.connection.showWindow = false })
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ConnectingFormButtons() {
        TextButton(text = Label.CANCEL, onClick = { State.connection.disconnect() })
        ComponentSpacer()
        TextButton(text = Label.CONNECTING, onClick = {}, enabled = false)
    }
}
