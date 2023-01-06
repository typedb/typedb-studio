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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.vaticle.typedb.studio.framework.material.ActionableList
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.Checkbox
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
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Property
import com.vaticle.typedb.studio.service.common.util.Property.Server.TYPEDB
import com.vaticle.typedb.studio.service.common.util.Property.Server.TYPEDB_CLUSTER
import com.vaticle.typedb.studio.service.common.util.Sentence
import com.vaticle.typedb.studio.service.connection.ClientState.Status.CONNECTED
import com.vaticle.typedb.studio.service.connection.ClientState.Status.CONNECTING
import com.vaticle.typedb.studio.service.connection.ClientState.Status.DISCONNECTED

object ServerDialog {

    private val WIDTH = 500.dp
    private val HEIGHT = 340.dp
    private val ADDRESS_MANAGER_WIDTH = 400.dp
    private val ADDRESS_MANAGER_HEIGHT = 500.dp
    private val appData = Service.data.connection

    private val state by mutableStateOf(ConnectServerForm())

    private class ConnectServerForm : Form.State() {
        var server: Property.Server by mutableStateOf(appData.server ?: Property.Server.TYPEDB)
        var coreAddress: String by mutableStateOf(appData.coreAddress ?: "")
        var clusterAddresses: MutableList<String> = mutableStateListOf<String>().also {
            appData.clusterAddresses?.let { saved -> it.addAll(saved) }
        }
        var username: String by mutableStateOf(appData.username ?: "")
        var password: String by mutableStateOf("")
        var tlsEnabled: Boolean by mutableStateOf(appData.tlsEnabled ?: false)
        var caCertificate: String by mutableStateOf(appData.caCertificate ?: "")

        override fun cancel() = Service.client.connectServerDialog.close()
        override fun isValid(): Boolean = when (server) {
            TYPEDB -> coreAddress.isNotBlank()
            TYPEDB_CLUSTER -> !(clusterAddresses.isEmpty() || username.isBlank() || password.isBlank())
        }

        override fun submit() {
            when (server) {
                TYPEDB -> {
                    Service.client.tryConnectToTypeDBAsync(coreAddress) {
                        Service.client.connectServerDialog.close()
                    }
                }
                TYPEDB_CLUSTER -> {
                    when {
                        caCertificate.isBlank() -> Service.client.tryConnectToTypeDBClusterAsync(
                            clusterAddresses.toSet(), username, password, tlsEnabled
                        ) {
                            Service.client.connectServerDialog.close()
                        }
                        else -> Service.client.tryConnectToTypeDBClusterAsync(
                            clusterAddresses.toSet(), username, password, caCertificate
                        ) {
                            Service.client.connectServerDialog.close()
                        }
                    }
                }
            }
            appData.server = server
            appData.coreAddress = coreAddress
            appData.clusterAddresses = clusterAddresses
            appData.username = username
            appData.tlsEnabled = tlsEnabled
            appData.caCertificate = caCertificate
        }
    }

    private object AddAddressForm : Form.State() {
        var value: String by mutableStateOf("")
        override fun cancel() = Service.client.manageAddressesDialog.close()
        override fun isValid() = value.isNotBlank() && validAddressFormat() && !state.clusterAddresses.contains(value)

        override fun submit() {
            assert(isValid())
            state.clusterAddresses.add(value)
            value = ""
        }

        private fun validAddressFormat(): Boolean {
            val addressParts = value.split(":")
            return addressParts.size == 2 && addressParts[1].toIntOrNull()?.let { it in 1024..65535 } == true
        }
    }

    @Composable
    fun MayShowDialogs() {
        if (Service.client.connectServerDialog.isOpen) ConnectServer()
        if (Service.client.manageAddressesDialog.isOpen) ManageClusterAddresses()
    }

    @Composable
    private fun ConnectServer() = Dialog.Layout(
        state = Service.client.connectServerDialog,
        title = Label.CONNECT_TO_TYPEDB,
        width = WIDTH,
        height = HEIGHT
    ) {
        Submission(state = state, modifier = Modifier.fillMaxSize(), showButtons = false) {
            ServerFormField(state)
            if (state.server == TYPEDB_CLUSTER) {
                ManageClusterAddressesButton(state = state, shouldFocus = Service.client.isDisconnected)
                UsernameFormField(state)
                PasswordFormField(state)
                TLSEnabledFormField(state)
                if (state.tlsEnabled) CACertificateFormField(state = state, dialogWindow = window)
            } else if (state.server == TYPEDB) {
                CoreAddressFormField(state, shouldFocus = Service.client.isDisconnected)
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

    @Composable
    private fun ServerFormField(state: ConnectServerForm) = Field(label = Label.SERVER) {
        Form.Dropdown(
            values = Property.Server.values().toList(),
            selected = state.server,
            onSelection = { state.server = it!! },
            modifier = Modifier.fillMaxSize(),
            enabled = Service.client.isDisconnected
        )
    }

    @Composable
    private fun CoreAddressFormField(state: ConnectServerForm, shouldFocus: Boolean) {
        var modifier = Modifier.fillMaxSize()
        val focusReq = if (shouldFocus) remember { FocusRequester() } else null
        focusReq?.let { modifier = modifier.focusRequester(focusReq) }
        Field(label = Label.ADDRESS) {
            TextInput(
                value = state.coreAddress,
                placeholder = Label.DEFAULT_SERVER_ADDRESS,
                onValueChange = { state.coreAddress = it },
                enabled = Service.client.isDisconnected,
                modifier = modifier
            )
        }
        LaunchedEffect(focusReq) { focusReq?.requestFocus() }
    }

    @Composable
    private fun ManageClusterAddressesButton(state: ConnectServerForm, shouldFocus: Boolean) {
        val focusReq = if (shouldFocus) remember { FocusRequester() } else null
        Field(label = Label.ADDRESSES) {
            TextButton(
                text = Label.MANAGE_CLUSTER_ADDRESSES + " (${state.clusterAddresses.size})",
                focusReq = focusReq, leadingIcon = Form.IconArg(Icon.CONNECT_TO_TYPEDB),
                enabled = Service.client.isDisconnected
            ) {
                Service.client.manageAddressesDialog.open()
            }
        }
        LaunchedEffect(focusReq) { focusReq?.requestFocus() }
    }

    @Composable
    private fun ManageClusterAddresses() {
        val dialogState = Service.client.manageAddressesDialog
        Dialog.Layout(dialogState, Label.MANAGE_CLUSTER_ADDRESSES, ADDRESS_MANAGER_WIDTH, ADDRESS_MANAGER_HEIGHT) {
            Column(Modifier.fillMaxSize()) {
                Text(value = Sentence.MANAGE_ADDRESSES_MESSAGE, softWrap = true)
                Spacer(Modifier.height(Dialog.DIALOG_SPACING))
                ClusterAddressList(Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(Dialog.DIALOG_SPACING))
                AddClusterAddressForm()
                Spacer(Modifier.height(Dialog.DIALOG_SPACING * 2))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    RowSpacer()
                    TextButton(text = Label.CLOSE) { dialogState.close() }
                }
            }
        }
    }

    @Composable
    private fun AddClusterAddressForm() {
        val focusReq = remember { FocusRequester() }
        Submission(AddAddressForm, modifier = Modifier.height(Form.FIELD_HEIGHT), showButtons = false) {
            Row {
                TextInput(
                    value = AddAddressForm.value,
                    placeholder = Label.DEFAULT_SERVER_ADDRESS,
                    onValueChange = { AddAddressForm.value = it },
                    modifier = Modifier.weight(1f).focusRequester(focusReq),
                )
                RowSpacer()
                TextButton(text = Label.ADD, enabled = AddAddressForm.isValid()) { AddAddressForm.submit() }
            }
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun ClusterAddressList(modifier: Modifier) = ActionableList.SingleButtonLayout(
        items = state.clusterAddresses.toMutableList(),
        modifier = modifier.border(1.dp, Theme.studio.border),
        buttonSide = ActionableList.Side.RIGHT,
        buttonFn = { address ->
            Form.IconButtonArg(
                icon = Icon.REMOVE,
                color = { Theme.studio.errorStroke },
                onClick = { state.clusterAddresses.remove(address) }
            )
        }
    )

    @Composable
    private fun UsernameFormField(state: ConnectServerForm) = Field(label = Label.USERNAME) {
        TextInput(
            value = state.username,
            placeholder = Label.USERNAME.lowercase(),
            onValueChange = { state.username = it },
            enabled = Service.client.isDisconnected,
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    private fun PasswordFormField(state: ConnectServerForm) = Field(label = Label.PASSWORD) {
        TextInput(
            value = state.password,
            placeholder = Label.PASSWORD.lowercase(),
            onValueChange = { state.password = it },
            enabled = Service.client.isDisconnected,
            isPassword = true,
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Composable
    private fun TLSEnabledFormField(state: ConnectServerForm) = Field(label = Label.ENABLE_TLS) {
        Checkbox(value = state.tlsEnabled, enabled = Service.client.isDisconnected) { state.tlsEnabled = it }
    }

    @Composable
    private fun CACertificateFormField(
        state: ConnectServerForm, dialogWindow: ComposeDialog
    ) = Field(label = Label.CA_CERTIFICATE) {
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
            if (selectedFilePath != null) state.caCertificate = selectedFilePath
        }
    }

    @Composable
    private fun ServerConnectionStatus() {
        val statusText = "${Label.STATUS}: ${Service.client.status.name.lowercase()}"
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
        TextButton(text = Label.CONNECT, enabled = state.isValid()) { state.submit() }
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
        TextButton(text = Label.CANCEL, focusReq = focusReq) { Service.client.closeAsync() }
        RowSpacer()
        TextButton(text = Label.CONNECTING, enabled = false) {}
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }
}