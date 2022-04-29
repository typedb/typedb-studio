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

package com.vaticle.typedb.studio.view.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.util.Property
import com.vaticle.typedb.studio.state.common.util.Property.Server.TYPEDB
import com.vaticle.typedb.studio.state.common.util.Property.Server.TYPEDB_CLUSTER
import com.vaticle.typedb.studio.state.connection.ClientState.Status.CONNECTED
import com.vaticle.typedb.studio.state.connection.ClientState.Status.CONNECTING
import com.vaticle.typedb.studio.state.connection.ClientState.Status.DISCONNECTED
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.Checkbox
import com.vaticle.typedb.studio.view.common.component.Form.Dropdown
import com.vaticle.typedb.studio.view.common.component.Form.Field
import com.vaticle.typedb.studio.view.common.component.Form.FormRowSpacer
import com.vaticle.typedb.studio.view.common.component.Form.Submission
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Form.TextInput
import com.vaticle.typedb.studio.view.common.theme.Theme

object ConnectionDialog {

    private val WIDTH = 500.dp
    private val HEIGHT = 340.dp
    private val appData = GlobalState.appData.connection

    private class ConnectServerForm : Form.State {
        var server: Property.Server by mutableStateOf(appData.server ?: TYPEDB)
        var address: String by mutableStateOf(appData.address ?: "")
        var username: String by mutableStateOf(appData.username ?: "")
        var password: String by mutableStateOf("")
        var tlsEnabled: Boolean by mutableStateOf(appData.tlsEnabled ?: false)
        var caCertificate: String by mutableStateOf(appData.caCertificate ?: "")

        override fun cancel() {
            GlobalState.client.connectServerDialog.close()
        }

        override fun isValid(): Boolean {
            return when (server) {
                TYPEDB -> !address.isBlank()
                TYPEDB_CLUSTER -> !(address.isBlank() || username.isBlank() || password.isBlank())
            }
        }

        override fun trySubmit() {
            when (server) {
                TYPEDB -> GlobalState.client.tryConnectToTypeDB(address)
                TYPEDB_CLUSTER -> when {
                    caCertificate.isBlank() -> GlobalState.client.tryConnectToTypeDBCluster(
                        address, username, password, tlsEnabled
                    )
                    else -> GlobalState.client.tryConnectToTypeDBCluster(
                        address, username, password, caCertificate
                    )
                }
            }
            appData.server = server
            appData.address = address
            appData.username = username
            appData.tlsEnabled = tlsEnabled
            appData.caCertificate = caCertificate
        }
    }

    @Composable
    fun ConnectServer() {
        val state = remember { ConnectServerForm() }
        Dialog.Layout(GlobalState.client.connectServerDialog, Label.CONNECT_TO_TYPEDB, WIDTH, HEIGHT) {
            Submission(state = state, modifier = Modifier.fillMaxSize(), showButtons = false) {
                ServerFormField(state)
                AddressFormField(state, GlobalState.client.isDisconnected)
                if (state.server == TYPEDB_CLUSTER) {
                    UsernameFormField(state)
                    PasswordFormField(state)
                    TLSEnabledFormField(state)
                    if (state.tlsEnabled) CACertificateFormField(state)
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    ServerConnectionStatus()
                    Spacer(modifier = Modifier.weight(1f))
                    when (GlobalState.client.status) {
                        DISCONNECTED -> DisconnectedFormButtons(state)
                        CONNECTING -> ConnectingFormButtons()
                        CONNECTED -> ConnectedFormButtons(state)
                    }
                }
            }
        }
    }

    @Composable
    private fun ServerFormField(state: ConnectServerForm) {
        Field(label = Label.SERVER) {
            Dropdown(
                values = Property.Server.values().toList(),
                selected = state.server,
                onSelection = { state.server = it },
                enabled = GlobalState.client.isDisconnected,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @Composable
    private fun AddressFormField(state: ConnectServerForm, shouldFocus: Boolean) {
        var modifier = Modifier.fillMaxSize()
        val focusReq = if (shouldFocus) FocusRequester() else null
        focusReq?.let { modifier = modifier.focusRequester(focusReq) }
        Field(label = Label.ADDRESS) {
            TextInput(
                value = state.address,
                placeholder = Property.DEFAULT_SERVER_ADDRESS,
                onValueChange = { state.address = it },
                enabled = GlobalState.client.isDisconnected,
                modifier = modifier
            )
        }
        LaunchedEffect(focusReq) { focusReq?.requestFocus() }
    }

    @Composable
    private fun UsernameFormField(state: ConnectServerForm) {
        Field(label = Label.USERNAME) {
            TextInput(
                value = state.username,
                placeholder = Label.USERNAME.lowercase(),
                onValueChange = { state.username = it },
                enabled = GlobalState.client.isDisconnected,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @Composable
    private fun PasswordFormField(state: ConnectServerForm) {
        Field(label = Label.PASSWORD) {
            TextInput(
                value = state.password,
                placeholder = Label.PASSWORD.lowercase(),
                onValueChange = { state.password = it },
                enabled = GlobalState.client.isDisconnected,
                isPassword = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun TLSEnabledFormField(state: ConnectServerForm) {
        Field(label = Label.ENABLE_TLS) {
            Checkbox(
                value = state.tlsEnabled,
                onChange = { state.tlsEnabled = it },
                enabled = GlobalState.client.isDisconnected,
            )
        }
    }

    @Composable
    private fun CACertificateFormField(state: ConnectServerForm) {
        Field(label = Label.CA_CERTIFICATE) {
            TextInput(
                value = state.caCertificate,
                placeholder = "${Label.PATH_TO_CA_CERTIFICATE} (${Label.OPTIONAL.lowercase()})",
                onValueChange = { state.caCertificate = it },
                enabled = GlobalState.client.isDisconnected,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun ServerConnectionStatus() {
        val statusText = "${Label.STATUS}: ${GlobalState.client.status.name.lowercase()}"
        Text(
            value = statusText, color = when (GlobalState.client.status) {
                DISCONNECTED -> Theme.colors.error2
                CONNECTING -> Theme.colors.quaternary2
                CONNECTED -> Theme.colors.secondary
            }
        )
    }

    @Composable
    private fun DisconnectedFormButtons(state: ConnectServerForm) {
        TextButton(text = Label.CANCEL, onClick = { state.cancel() })
        FormRowSpacer()
        TextButton(
            text = Label.CONNECT,
            onClick = { state.trySubmit() },
            enabled = state.isValid()
        )
    }

    @Composable
    private fun ConnectedFormButtons(state: ConnectServerForm) {
        val focusReq = remember { FocusRequester() }
        TextButton(
            text = Label.DISCONNECT,
            onClick = { GlobalState.client.close() },
            textColor = Theme.colors.error2
        )
        FormRowSpacer()
        TextButton(text = Label.CLOSE, focusReq = focusReq, onClick = { state.cancel() })
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun ConnectingFormButtons() {
        val focusReq = remember { FocusRequester() }
        TextButton(text = Label.CANCEL, focusReq = focusReq, onClick = { GlobalState.client.close() })
        FormRowSpacer()
        TextButton(text = Label.CONNECTING, onClick = {}, enabled = false)
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }
}
