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
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.common.Property.Server.TYPEDB
import com.vaticle.typedb.studio.state.common.Property.Server.TYPEDB_CLUSTER
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.CONNECTED
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.CONNECTING
import com.vaticle.typedb.studio.state.connection.ConnectionManager.Status.DISCONNECTED
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.Checkbox
import com.vaticle.typedb.studio.view.common.component.Form.ComponentSpacer
import com.vaticle.typedb.studio.view.common.component.Form.Dropdown
import com.vaticle.typedb.studio.view.common.component.Form.Field
import com.vaticle.typedb.studio.view.common.component.Form.Submission
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Form.TextButton
import com.vaticle.typedb.studio.view.common.component.Form.TextInput
import com.vaticle.typedb.studio.view.common.theme.Theme

object ConnectionDialog {

    private val CONNECT_SERVER_WINDOW_WIDTH = 500.dp
    private val CONNECT_SERVER_WINDOW_HEIGHT = 340.dp
    private val SELECT_DB_WINDOW_WIDTH = 400.dp
    private val SELECT_DB_WINDOW_HEIGHT = 200.dp

    private object ConnectServerForm : Form.State {
        // We keep this static to maintain the values through application lifetime,
        // and easily accessible to all functions in this object without being passed around

        var server: Property.Server by mutableStateOf(TYPEDB)
        var address: String by mutableStateOf("")
        var username: String by mutableStateOf("")
        var password: String by mutableStateOf("")
        var tlsEnabled: Boolean by mutableStateOf(false)
        var caCertificate: String by mutableStateOf("")

        override fun isValid(): Boolean {
            return when (server) {
                TYPEDB -> !address.isBlank()
                TYPEDB_CLUSTER -> !(address.isBlank() || username.isBlank() || password.isBlank())
            }
        }

        override fun trySubmit() {
            when (server) {
                TYPEDB -> GlobalState.connection.tryConnectToTypeDB(address)
                TYPEDB_CLUSTER -> when {
                    caCertificate.isBlank() -> GlobalState.connection.tryConnectToTypeDBCluster(
                        address, username, password, tlsEnabled
                    )
                    else -> GlobalState.connection.tryConnectToTypeDBCluster(
                        address, username, password, caCertificate
                    )
                }
            }
        }
    }

    @Composable
    fun ConnectServer() {
        Dialog(
            title = Label.CONNECT_TO_TYPEDB,
            onCloseRequest = { GlobalState.connection.connectServerDialog.close() },
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(CONNECT_SERVER_WINDOW_WIDTH, CONNECT_SERVER_WINDOW_HEIGHT)
            )
        ) {
            Submission(state = ConnectServerForm) {
                ServerFormField()
                AddressFormField()
                if (ConnectServerForm.server == TYPEDB_CLUSTER) {
                    UsernameFormField()
                    PasswordFormField()
                    TLSEnabledFormField()
                    if (ConnectServerForm.tlsEnabled) CACertificateFormField()
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    ServerConnectionStatus()
                    Spacer(modifier = Modifier.weight(1f))
                    when (GlobalState.connection.status) {
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
                selected = ConnectServerForm.server,
                onSelection = { ConnectServerForm.server = it },
                enabled = GlobalState.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun AddressFormField() {
        Field(label = Label.ADDRESS) {
            TextInput(
                value = ConnectServerForm.address,
                placeholder = Property.DEFAULT_SERVER_ADDRESS,
                onValueChange = { ConnectServerForm.address = it },
                enabled = GlobalState.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun UsernameFormField() {
        Field(label = Label.USERNAME) {
            TextInput(
                value = ConnectServerForm.username,
                placeholder = Label.USERNAME.lowercase(),
                onValueChange = { ConnectServerForm.username = it },
                enabled = GlobalState.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun PasswordFormField() {
        Field(label = Label.PASSWORD) {
            TextInput(
                value = ConnectServerForm.password,
                placeholder = Label.PASSWORD.lowercase(),
                onValueChange = { ConnectServerForm.password = it },
                enabled = GlobalState.connection.isDisconnected(),
                isPassword = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun TLSEnabledFormField() {
        Field(label = Label.ENABLE_TLS) {
            Checkbox(
                value = ConnectServerForm.tlsEnabled,
                onChange = { ConnectServerForm.tlsEnabled = it },
                enabled = GlobalState.connection.isDisconnected(),
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun CACertificateFormField() {
        Field(label = Label.CA_CERTIFICATE) {
            TextInput(
                value = ConnectServerForm.caCertificate,
                placeholder = "${Label.PATH_TO_CA_CERTIFICATE} (${Label.OPTIONAL.lowercase()})",
                onValueChange = { ConnectServerForm.caCertificate = it },
                enabled = GlobalState.connection.isDisconnected(),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun ServerConnectionStatus() {
        val statusText = "${Label.STATUS}: ${GlobalState.connection.status.name.lowercase()}"
        Text(
            value = statusText, color = when (GlobalState.connection.status) {
                DISCONNECTED -> Theme.colors.error2
                CONNECTING -> Theme.colors.quaternary2
                CONNECTED -> Theme.colors.secondary
            }
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun DisconnectedFormButtons() {
        TextButton(text = Label.CANCEL, onClick = { GlobalState.connection.connectServerDialog.close() })
        ComponentSpacer()
        TextButton(
            text = Label.CONNECT,
            enabled = ConnectServerForm.isValid(),
            onClick = { ConnectServerForm.trySubmit() })
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ConnectedFormButtons() {
        TextButton(
            text = Label.DISCONNECT,
            onClick = { GlobalState.connection.disconnect() },
            textColor = Theme.colors.error2
        )
        ComponentSpacer()
        TextButton(text = Label.CLOSE, onClick = { GlobalState.connection.connectServerDialog.close() })
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ConnectingFormButtons() {
        TextButton(text = Label.CANCEL, onClick = { GlobalState.connection.disconnect() })
        ComponentSpacer()
        TextButton(text = Label.CONNECTING, onClick = {}, enabled = false)
    }

    @Composable
    fun SelectDatabase() {
        val selectDBDialog = GlobalState.connection.selectDatabaseDialog
        Dialog(
            title = Label.SELECT_DATABASE,
            onCloseRequest = { selectDBDialog.close() },
            state = rememberDialogState(
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(SELECT_DB_WINDOW_WIDTH, SELECT_DB_WINDOW_HEIGHT)
            )
        ) {
            Submission {
                Field(label = Label.SELECT_DATABASE) { DatabaseDropdown() }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(text = Label.CLOSE, onClick = { selectDBDialog.close() })
                }
            }
        }
    }

    @Composable
    fun DatabaseDropdown(modifier: Modifier = Modifier) {
        Dropdown(
            modifier = modifier,
            values = GlobalState.connection.current?.databaseList ?: emptyList(),
            selected = GlobalState.connection.current?.getDatabase() ?: "",
            onExpand = { GlobalState.connection.current?.refreshDatabaseList() },
            onSelection = { GlobalState.connection.current?.setDatabase(it) },
            placeholder = Label.SELECT_DATABASE,
            enabled = GlobalState.connection.isConnected()
        )
    }
}
