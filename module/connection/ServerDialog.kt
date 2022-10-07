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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.Checkbox
import com.vaticle.typedb.studio.framework.material.Form.Field
import com.vaticle.typedb.studio.framework.material.Form.IconButton
import com.vaticle.typedb.studio.framework.material.Form.RowSpacer
import com.vaticle.typedb.studio.framework.material.Form.MultilineTextInput
import com.vaticle.typedb.studio.framework.material.Form.Submission
import com.vaticle.typedb.studio.framework.material.Form.Text
import com.vaticle.typedb.studio.framework.material.Form.TextButton
import com.vaticle.typedb.studio.framework.material.Form.TextButtonRow
import com.vaticle.typedb.studio.framework.material.Form.TextInput
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.SelectFileDialog
import com.vaticle.typedb.studio.framework.material.SelectFileDialog.SelectorOptions
import com.vaticle.typedb.studio.framework.material.Tooltip
import com.vaticle.typedb.studio.service.Service
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
    private val appData = Service.data.connection

    private class ConnectServerForm : Form.State {
        var server: Property.Server by mutableStateOf(appData.server ?: TYPEDB)
        var coreAddress: String by mutableStateOf(appData.coreAddress ?: "")
        var clusterAddresses: TextFieldValue by mutableStateOf(TextFieldValue(""))
        var username: String by mutableStateOf(appData.username ?: "")
        var password: String by mutableStateOf("")
        var tlsEnabled: Boolean by mutableStateOf(appData.tlsEnabled ?: false)
        var caCertificate: String by mutableStateOf(appData.caCertificate ?: "")

        override fun cancel() {
            Service.client.connectServerDialog.close()
        }

        override fun isValid(): Boolean {
            return when (server) {
                TYPEDB -> !coreAddress.isBlank()
                TYPEDB_CLUSTER -> !(clusterAddresses.text.isBlank() || username.isBlank() || password.isBlank())
            }
        }

        override fun trySubmit() {
            when (server) {
                TYPEDB -> Service.client.tryConnectToTypeDBAsync(coreAddress) { Service.client.connectServerDialog.close() }
                TYPEDB_CLUSTER -> when {
                    caCertificate.isBlank() -> Service.client.tryConnectToTypeDBClusterAsync(
                        clusterAddresses, username, password, tlsEnabled
                    ) { Service.client.connectServerDialog.close() }
                    else -> Service.client.tryConnectToTypeDBClusterAsync(
                        clusterAddresses, username, password, caCertificate
                    ) { Service.client.connectServerDialog.close() }
                }
            }
            appData.server = server
            appData.coreAddress = coreAddress
            appData.clusterAddresses = clusterAddresses.text
            appData.username = username
            appData.tlsEnabled = tlsEnabled
            appData.caCertificate = caCertificate
        }
    }

    @Composable
    fun MayShowDialogs() {
        if (Service.client.connectServerDialog.isOpen) ConnectServer()
    }

    @Composable
    private fun ConnectServer() {
        val state = remember { ConnectServerForm() }
        Dialog.Layout(
            Service.client.connectServerDialog,
            Label.CONNECT_TO_TYPEDB,
            WIDTH,
            HEIGHT
        ) {
            Submission(state = state, modifier = Modifier.fillMaxSize(), showButtons = false) {
                ServerFormButtons(state)
                if (state.server == TYPEDB_CLUSTER) {
                    ClusterAddressFormField(state, Service.client.isConnected)
                    UsernameFormField(state)
                    PasswordFormField(state)
                    TLSEnabledFormField(state)
                    if (state.tlsEnabled) CACertificateFormField(state = state, dialogWindow = window)
                } else if (state.server == TYPEDB) {
                    CoreAddressFormField(state, Service.client.isDisconnected)
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    ServerConnectionStatus()
                    Spacer(modifier = Modifier.weight(1f))
                    when (Service.client.status) {
                        DISCONNECTED -> DisconnectedFormButtons(state)
                        CONNECTING -> ConnectingFormButtons()
                        CONNECTED -> ConnectedFormButtons(state)
                    }
                }
            }
        }
    }

    @Composable
    private fun ServerFormButtons(state: ConnectServerForm) {
        Field(label = Label.SERVER) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButtonRow(
                    listOf(
                        Form.TextButtonArg("TypeDB Core",
                            color = { Form.toggleButtonColor(state.server == Property.Server.TYPEDB) },
                            enabled = Service.client.isDisconnected)
                            {state.server = Property.Server.TYPEDB},
                        Form.TextButtonArg("TypeDB Cluster",
                            color = { Form.toggleButtonColor(state.server == Property.Server.TYPEDB_CLUSTER) },
                            enabled = Service.client.isDisconnected)
                            { state.server = Property.Server.TYPEDB_CLUSTER}
                    )
                )
            }
        }
    }

    @Composable
    private fun CoreAddressFormField(state: ConnectServerForm, shouldFocus: Boolean) {
        var modifier = Modifier.fillMaxSize()
        val focusReq = if (shouldFocus) FocusRequester() else null
        focusReq?.let { modifier = modifier.focusRequester(focusReq) }
        Field(label = Label.ADDRESS) {
            TextInput(
                value = state.coreAddress,
                placeholder = Property.DEFAULT_SERVER_ADDRESS,
                onValueChange = { state.coreAddress = it },
                enabled = Service.client.isDisconnected,
                modifier = modifier
            )
        }
        LaunchedEffect(focusReq) { focusReq?.requestFocus() }
    }

    @Composable
    private fun ClusterAddressesFormField(state: ConnectServerForm, shouldFocus: Boolean) {
        var modifier = Modifier.fillMaxSize()
        val focusReq = if (shouldFocus) FocusRequester() else null
        focusReq?.let { modifier = modifier.focusRequester(focusReq) }
        Field(label = Label.ADDRESSES) {
            MultilineTextInput(
                value = state.clusterAddresses,
                onValueChange = { state.clusterAddresses = it },
                modifier = modifier,
                onTextLayout = {}
            )
        }

        Field(label = Label.CA_CERTIFICATE) {
            Row {
                TextInput(
                    value = state.caCertificate,
                    placeholder = "${Label.PATH_TO_CA_CERTIFICATE} (${Label.OPTIONAL.lowercase()})",
                    onValueChange = { state.caCertificate = it },
                    enabled = StudioState.client.isDisconnected,
                    modifier = Modifier.weight(1f).focusRequester(focusReq),
                )
                FormRowSpacer()
                IconButton(
                    icon = Icon.FOLDER_OPEN,
                    tooltip = Tooltip.Arg(Label.OPEN_PROJECT_DIRECTORY)
                ) { state.caCertificate = selectFilePath(window, Label.SELECT_CERTIFICATE, SelectorOptions.FILES) }
            }
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
                enabled = Service.client.isDisconnected,
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
                enabled = Service.client.isDisconnected,
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
                enabled = Service.client.isDisconnected,
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
                enabled = Service.client.isDisconnected,
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
            "${Label.STATUS}: ${Service.client.status.name.lowercase()}"
        Text(
            value = statusText, color = when (Service.client.status) {
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
        ) { Service.client.closeAsync() }
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
        ) { Service.client.closeAsync() }
        RowSpacer()
        TextButton(text = Label.CONNECTING, enabled = false) {}
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }
}
