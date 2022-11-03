/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.module.connection

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
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.Checkbox
import com.vaticle.typedb.studio.framework.material.Form.Dropdown
import com.vaticle.typedb.studio.framework.material.Form.Field
import com.vaticle.typedb.studio.framework.material.Form.IconButton
import com.vaticle.typedb.studio.framework.material.Form.RowSpacer
import com.vaticle.typedb.studio.framework.material.Form.Submission
import com.vaticle.typedb.studio.framework.material.Form.Text
import com.vaticle.typedb.studio.framework.material.Form.TextButton
import com.vaticle.typedb.studio.framework.material.Form.TextInput
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.SelectFileDialog
import com.vaticle.typedb.studio.framework.material.SelectFileDialog.SelectorOptions
import com.vaticle.typedb.studio.framework.material.Tooltip
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Property
import com.vaticle.typedb.studio.service.common.util.Property.Server.TYPEDB
import com.vaticle.typedb.studio.service.common.util.Property.Server.TYPEDB_CLUSTER
import com.vaticle.typedb.studio.service.connection.ClientState.Status.CONNECTED
import com.vaticle.typedb.studio.service.connection.ClientState.Status.CONNECTING
import com.vaticle.typedb.studio.service.connection.ClientState.Status.DISCONNECTED

object ServerDialog {

    private val WIDTH = 500.dp
    private val HEIGHT = 340.dp
    private val appData = com.vaticle.typedb.studio.service.Service.data.connection

    private class ConnectServerForm : Form.State {
        var server: Property.Server by mutableStateOf(appData.server ?: TYPEDB)
        var address: String by mutableStateOf(appData.address ?: "")
        var username: String by mutableStateOf(appData.username ?: "")
        var password: String by mutableStateOf("")
        var tlsEnabled: Boolean by mutableStateOf(appData.tlsEnabled ?: false)
        var caCertificate: String by mutableStateOf(appData.caCertificate ?: "")

        override fun cancel() {
            com.vaticle.typedb.studio.service.Service.client.connectServerDialog.close()
        }

        override fun isValid(): Boolean {
            return when (server) {
                TYPEDB -> !address.isBlank()
                TYPEDB_CLUSTER -> !(address.isBlank() || username.isBlank() || password.isBlank())
            }
        }

        override fun trySubmit() {
            when (server) {
                TYPEDB -> com.vaticle.typedb.studio.service.Service.client.tryConnectToTypeDBAsync(address) { com.vaticle.typedb.studio.service.Service.client.connectServerDialog.close() }
                TYPEDB_CLUSTER -> when {
                    caCertificate.isBlank() -> com.vaticle.typedb.studio.service.Service.client.tryConnectToTypeDBClusterAsync(
                        address, username, password, tlsEnabled
                    ) { com.vaticle.typedb.studio.service.Service.client.connectServerDialog.close() }
                    else -> com.vaticle.typedb.studio.service.Service.client.tryConnectToTypeDBClusterAsync(
                        address, username, password, caCertificate
                    ) { com.vaticle.typedb.studio.service.Service.client.connectServerDialog.close() }
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
    fun MayShowDialogs() {
        if (com.vaticle.typedb.studio.service.Service.client.connectServerDialog.isOpen) ConnectServer()
    }

    @Composable
    private fun ConnectServer() {
        val state = remember { ConnectServerForm() }
        Dialog.Layout(
            com.vaticle.typedb.studio.service.Service.client.connectServerDialog,
            Label.CONNECT_TO_TYPEDB,
            WIDTH,
            HEIGHT
        ) {
            Submission(state = state, modifier = Modifier.fillMaxSize(), showButtons = false) {
                ServerFormField(state)
                AddressFormField(state, com.vaticle.typedb.studio.service.Service.client.isDisconnected)
                if (state.server == TYPEDB_CLUSTER) {
                    UsernameFormField(state)
                    PasswordFormField(state)
                    TLSEnabledFormField(state)
                    if (state.tlsEnabled) CACertificateFormField(state = state, dialogWindow = window)
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    ServerConnectionStatus()
                    Spacer(modifier = Modifier.weight(1f))
                    when (com.vaticle.typedb.studio.service.Service.client.status) {
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
                onSelection = { state.server = it!! },
                modifier = Modifier.fillMaxSize(),
                enabled = com.vaticle.typedb.studio.service.Service.client.isDisconnected
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
                enabled = com.vaticle.typedb.studio.service.Service.client.isDisconnected,
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
                enabled = com.vaticle.typedb.studio.service.Service.client.isDisconnected,
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
                enabled = com.vaticle.typedb.studio.service.Service.client.isDisconnected,
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
                enabled = com.vaticle.typedb.studio.service.Service.client.isDisconnected,
            ) { state.tlsEnabled = it }
        }
    }

    @Composable
    private fun CACertificateFormField(state: ConnectServerForm, dialogWindow: ComposeDialog) {
        Field(label = Label.CA_CERTIFICATE) {
            TextInput(
                value = state.caCertificate,
                placeholder = "${Label.PATH_TO_CA_CERTIFICATE} (${Label.OPTIONAL.lowercase()})",
                onValueChange = { state.caCertificate = it },
                enabled = com.vaticle.typedb.studio.service.Service.client.isDisconnected,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                icon = Icon.FOLDER_OPEN,
                tooltip = Tooltip.Arg(Label.SELECT_CERTIFICATE_FILE)
            ) {
                val (selectedFilePath) = SelectFileDialog.open(
                    dialogWindow, Label.SELECT_CERTIFICATE_FILE, SelectorOptions.FILES_ONLY
                )
                if (selectedFilePath != null) {
                    state.caCertificate = selectedFilePath
                }
            }
        }
    }

    @Composable
    private fun ServerConnectionStatus() {
        val statusText =
            "${Label.STATUS}: ${com.vaticle.typedb.studio.service.Service.client.status.name.lowercase()}"
        Text(
            value = statusText, color = when (com.vaticle.typedb.studio.service.Service.client.status) {
                DISCONNECTED -> Theme.studio.errorStroke
                CONNECTING -> Theme.studio.warningStroke
                CONNECTED -> Theme.studio.secondary
            }
        )
    }

    @Composable
    private fun DisconnectedFormButtons(state: ConnectServerForm) {
        TextButton(text = Label.CANCEL) { state.cancel() }
        RowSpacer()
        TextButton(text = Label.CONNECT, enabled = state.isValid()) { state.trySubmit() }
    }

    @Composable
    private fun ConnectedFormButtons(state: ConnectServerForm) {
        val focusReq = remember { FocusRequester() }
        TextButton(
            text = Label.DISCONNECT,
            textColor = Theme.studio.errorStroke
        ) { com.vaticle.typedb.studio.service.Service.client.closeAsync() }
        RowSpacer()
        TextButton(text = Label.CLOSE, focusReq = focusReq) { state.cancel() }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun ConnectingFormButtons() {
        val focusReq = remember { FocusRequester() }
        TextButton(
            text = Label.CANCEL,
            focusReq = focusReq
        ) { com.vaticle.typedb.studio.service.Service.client.closeAsync() }
        RowSpacer()
        TextButton(text = Label.CONNECTING, enabled = false) {}
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }
}
