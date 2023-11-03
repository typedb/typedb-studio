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
import com.vaticle.typedb.studio.framework.material.Form.TextInputValidated
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.SelectFileDialog
import com.vaticle.typedb.studio.framework.material.SelectFileDialog.SelectorOptions
import com.vaticle.typedb.studio.framework.material.Tooltip
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Property
import com.vaticle.typedb.studio.service.common.util.Property.Server.TYPEDB
import com.vaticle.typedb.studio.service.common.util.Property.Server.TYPEDB_ENTERPRISE
import com.vaticle.typedb.studio.service.common.util.Sentence
import com.vaticle.typedb.studio.service.connection.DriverState.Status.CONNECTED
import com.vaticle.typedb.studio.service.connection.DriverState.Status.CONNECTING
import com.vaticle.typedb.studio.service.connection.DriverState.Status.DISCONNECTED

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
        var enterpriseAddresses: MutableList<String> = mutableStateListOf<String>().also {
            appData.enterpriseAddresses?.let { saved -> it.addAll(saved) }
        }
        var username: String by mutableStateOf(appData.username ?: "")
        var password: String by mutableStateOf("")
        var tlsEnabled: Boolean by mutableStateOf(appData.tlsEnabled ?: false)
        var caCertificate: String by mutableStateOf(appData.caCertificate ?: "")

        override fun cancel() = Service.driver.connectServerDialog.close()
        override fun isValid(): Boolean = when (server) {
            TYPEDB -> coreAddress.isNotBlank() && addressFormatIsValid(coreAddress)
            TYPEDB_ENTERPRISE -> !(enterpriseAddresses.isEmpty() || username.isBlank() || password.isBlank())
        }

        override fun submit() {
            when (server) {
                TYPEDB -> {
                    Service.driver.tryConnectToTypeDBAsync(coreAddress) {
                        Service.driver.connectServerDialog.close()
                    }
                }
                TYPEDB_ENTERPRISE -> {
                    val onSuccess = Service.driver.connectServerDialog::close
                    when {
                        caCertificate.isBlank() || !tlsEnabled -> Service.driver.tryConnectToTypeDBEnterpriseAsync(
                            enterpriseAddresses.toSet(), username, password, tlsEnabled, onSuccess
                        )
                        else -> Service.driver.tryConnectToTypeDBEnterpriseAsync(
                            enterpriseAddresses.toSet(), username, password, caCertificate, onSuccess
                        )
                    }
                }
            }
            appData.server = server
            appData.coreAddress = coreAddress
            appData.enterpriseAddresses = enterpriseAddresses
            appData.username = username
            appData.tlsEnabled = tlsEnabled
            appData.caCertificate = caCertificate
        }
    }

    private object AddAddressForm : Form.State() {
        var value: String by mutableStateOf("")
        override fun cancel() = Service.driver.manageAddressesDialog.close()
        override fun isValid() = value.isNotBlank() && addressFormatIsValid(value) && !state.enterpriseAddresses.contains(value)

        override fun submit() {
            assert(isValid())
            state.enterpriseAddresses.add(value)
            value = ""
        }
    }

    private fun addressFormatIsValid(address: String): Boolean {
        val addressParts = address.split(":")
        return addressParts.size == 2 && addressParts[1].toIntOrNull()?.let { it in 1024..65535 } == true
    }

    @Composable
    fun MayShowDialogs() {
        if (Service.driver.connectServerDialog.isOpen) ConnectServer()
        if (Service.driver.manageAddressesDialog.isOpen) ManageEnterpriseAddresses()
    }

    @Composable
    private fun ConnectServer() = Dialog.Layout(
        state = Service.driver.connectServerDialog,
        title = Label.CONNECT_TO_TYPEDB,
        width = WIDTH,
        height = HEIGHT
    ) {
        Submission(state = state, modifier = Modifier.fillMaxSize(), showButtons = false) {
            ServerFormField(state)
            if (state.server == TYPEDB_ENTERPRISE) {
                ManageEnterpriseAddressesButton(state = state, shouldFocus = Service.driver.isDisconnected)
                UsernameFormField(state)
                PasswordFormField(state)
                TLSEnabledFormField(state)
                if (state.tlsEnabled) CACertificateFormField(state = state, dialogWindow = window)
            } else if (state.server == TYPEDB) {
                CoreAddressFormField(state, shouldFocus = Service.driver.isDisconnected)
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                ServerConnectionStatus()
                Spacer(modifier = Modifier.weight(1f))
                when (Service.driver.status) {
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
            enabled = Service.driver.isDisconnected
        )
    }

    @Composable
    private fun CoreAddressFormField(state: ConnectServerForm, shouldFocus: Boolean) {
        var modifier = Modifier.fillMaxSize()
        val focusReq = if (shouldFocus) remember { FocusRequester() } else null
        focusReq?.let { modifier = modifier.focusRequester(focusReq) }
        Field(label = Label.ADDRESS) {
            TextInputValidated(
                value = state.coreAddress,
                placeholder = Label.DEFAULT_SERVER_ADDRESS,
                onValueChange = { state.coreAddress = it },
                enabled = Service.driver.isDisconnected,
                modifier = modifier,
                invalidWarning = Label.ADDRESS_PORT_WARNING,
                validator = { state.coreAddress.isNotBlank() && addressFormatIsValid(state.coreAddress) }
            )
        }
        LaunchedEffect(focusReq) { focusReq?.requestFocus() }
    }

    @Composable
    private fun ManageEnterpriseAddressesButton(state: ConnectServerForm, shouldFocus: Boolean) {
        val focusReq = if (shouldFocus) remember { FocusRequester() } else null
        Field(label = Label.ADDRESSES) {
            TextButton(
                text = Label.MANAGE_ENTERPRISE_ADDRESSES + " (${state.enterpriseAddresses.size})",
                focusReq = focusReq, leadingIcon = Form.IconArg(Icon.CONNECT_TO_TYPEDB),
                enabled = Service.driver.isDisconnected
            ) {
                Service.driver.manageAddressesDialog.open()
            }
        }
        LaunchedEffect(focusReq) { focusReq?.requestFocus() }
    }

    @Composable
    private fun ManageEnterpriseAddresses() {
        val dialogState = Service.driver.manageAddressesDialog
        Dialog.Layout(dialogState, Label.MANAGE_ENTERPRISE_ADDRESSES, ADDRESS_MANAGER_WIDTH, ADDRESS_MANAGER_HEIGHT) {
            Column(Modifier.fillMaxSize()) {
                Text(value = Sentence.MANAGE_ADDRESSES_MESSAGE, softWrap = true)
                Spacer(Modifier.height(Dialog.DIALOG_SPACING))
                EnterpriseAddressList(Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(Dialog.DIALOG_SPACING))
                AddEnterpriseAddressForm()
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
    private fun AddEnterpriseAddressForm() {
        val focusReq = remember { FocusRequester() }
        Submission(AddAddressForm, modifier = Modifier.height(Form.FIELD_HEIGHT), showButtons = false) {
            Row {
                TextInputValidated(
                    value = AddAddressForm.value,
                    placeholder = Label.DEFAULT_SERVER_ADDRESS,
                    onValueChange = { AddAddressForm.value = it },
                    modifier = Modifier.weight(1f).focusRequester(focusReq),
                    invalidWarning = Label.ADDRESS_PORT_WARNING,
                    validator = { AddAddressForm.value.isNotBlank() && addressFormatIsValid(AddAddressForm.value) }
                )
                RowSpacer()
                TextButton(text = Label.ADD, enabled = AddAddressForm.isValid()) { AddAddressForm.submit() }
            }
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun EnterpriseAddressList(modifier: Modifier) = ActionableList.SingleButtonLayout(
        items = state.enterpriseAddresses.toMutableList(),
        modifier = modifier.border(1.dp, Theme.studio.border),
        buttonSide = ActionableList.Side.RIGHT,
        buttonFn = { address ->
            Form.IconButtonArg(
                icon = Icon.REMOVE,
                color = { Theme.studio.errorStroke },
                onClick = { state.enterpriseAddresses.remove(address) }
            )
        }
    )

    @Composable
    private fun UsernameFormField(state: ConnectServerForm) = Field(label = Label.USERNAME) {
        TextInput(
            value = state.username,
            placeholder = Label.USERNAME.lowercase(),
            onValueChange = { state.username = it },
            enabled = Service.driver.isDisconnected,
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    private fun PasswordFormField(state: ConnectServerForm) = Field(label = Label.PASSWORD) {
        TextInput(
            value = state.password,
            placeholder = Label.PASSWORD.lowercase(),
            onValueChange = { state.password = it },
            enabled = Service.driver.isDisconnected,
            isPassword = true,
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Composable
    private fun TLSEnabledFormField(state: ConnectServerForm) = Field(label = Label.ENABLE_TLS) {
        Checkbox(value = state.tlsEnabled, enabled = Service.driver.isDisconnected) { state.tlsEnabled = it }
    }

    @Composable
    private fun CACertificateFormField(
        state: ConnectServerForm, dialogWindow: ComposeDialog
    ) = Field(label = Label.CA_CERTIFICATE) {
        TextInput(
            value = state.caCertificate,
            placeholder = "${Label.PATH_TO_CA_CERTIFICATE} (${Label.OPTIONAL.lowercase()})",
            onValueChange = { state.caCertificate = it },
            enabled = Service.driver.isDisconnected,
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
        val statusText = "${Label.STATUS}: ${Service.driver.status.name.lowercase()}"
        Text(
            value = statusText, color = when (Service.driver.status) {
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
        ) { Service.driver.closeAsync() }
        RowSpacer()
        TextButton(text = Label.CLOSE, focusReq = focusReq) { state.cancel() }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun ConnectingFormButtons() {
        val focusReq = remember { FocusRequester() }
        TextButton(text = Label.CANCEL, focusReq = focusReq) { Service.driver.closeAsync() }
        RowSpacer()
        TextButton(text = Label.CONNECTING, enabled = false) {}
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }
}
