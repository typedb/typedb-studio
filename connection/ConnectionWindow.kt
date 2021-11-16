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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import com.vaticle.typedb.studio.common.component.Form.INPUT_MODIFIER
import com.vaticle.typedb.studio.common.component.Form.LABEL_MODIFIER
import com.vaticle.typedb.studio.common.theme.Theme
import com.vaticle.typedb.studio.service.ConnectionService.Status.CONNECTED
import com.vaticle.typedb.studio.service.ConnectionService.Status.CONNECTING
import com.vaticle.typedb.studio.service.ConnectionService.Status.DISCONNECTED
import com.vaticle.typedb.studio.service.Service

object ConnectionWindow {

    private val WINDOW_WIDTH = 500.dp
    private val WINDOW_HEIGHT = 280.dp

    private object FormState {
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
            onCloseRequest = { Service.connection.openDialog = false },
            alwaysOnTop = true,
            state = rememberWindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition.Aligned(Alignment.Center),
                size = WindowSize(WINDOW_WIDTH, WINDOW_HEIGHT)
            )
        ) {
            Theme {
                Column(modifier = Modifier.fillMaxSize().background(Theme.colors.background)) {
                    Form.FieldGroup {
                        ServerFormField(LABEL_MODIFIER, INPUT_MODIFIER)
                        AddressFormField(LABEL_MODIFIER, INPUT_MODIFIER)
                        if (FormState.server == TYPEDB_CLUSTER) {
                            UsernameFormField(LABEL_MODIFIER, INPUT_MODIFIER)
                            PasswordFormField(LABEL_MODIFIER, INPUT_MODIFIER)
                            CACertificateFormField(LABEL_MODIFIER, INPUT_MODIFIER)
                        }
                        Spacer(Modifier.weight(1f))
                        Row {
                            ServerConnectionStatus()
                            Spacer(Modifier.weight(1f))
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
    }

    @Composable
    private fun ServerFormField(labelModifier: Modifier, inputModifier: Modifier) {
        Form.Field {
            Text(text = Label.SERVER, style = Theme.typography.body1, modifier = labelModifier)
//            Dropdown(
//                entries = Property.Server.values().associateWith { it.displayName },
//                selected = FormState.server,
//                modifier = inputModifier,
//                onSelection = { server -> FormState.server = server },
//                enabled = Service.connection.isDisconnected()
//            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun AddressFormField(labelModifier: Modifier, inputModifier: Modifier) {
        Form.Field {
            Text(text = Label.ADDRESS, style = Theme.typography.body1, modifier = labelModifier)
//            TextField(
//                value = FormState.address,
//                placeholderText = Property.DEFAULT_SERVER_ADDRESS,
//                modifier = inputModifier,
//                textStyle = Theme.typography.body1,
//                onValueChange = { FormState.address = it },
//                enabled = Service.connection.isDisconnected()
//            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun UsernameFormField(labelModifier: Modifier, inputModifier: Modifier) {
        Form.Field {
            Text(text = Label.USERNAME, style = Theme.typography.body1, modifier = labelModifier)
//            TextField(
//                value = FormState.username,
//                placeholderText = Label.USERNAME.lowercase(),
//                modifier = inputModifier,
//                textStyle = Theme.typography.body1,
//                onValueChange = { FormState.username = it },
//                enabled = Service.connection.isDisconnected()
//            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun PasswordFormField(labelModifier: Modifier, inputModifier: Modifier) {
        Form.Field {
            Text(text = Label.PASSWORD, style = Theme.typography.body1, modifier = labelModifier)
//            TextField(
//                value = FormState.password,
//                placeholderText = Label.PASSWORD.lowercase(),
//                modifier = inputModifier,
//                textStyle = Theme.typography.body1,
//                onValueChange = { FormState.password = it },
//                visualTransformation = PasswordVisualTransformation(),
//                enabled = Service.connection.isDisconnected()
//            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun CACertificateFormField(labelModifier: Modifier, inputModifier: Modifier) {
        Form.Field {
            Text(text = Label.CA_CERTIFICATE, style = Theme.typography.body1, modifier = labelModifier)
//            TextField(
//                value = FormState.caCertificate,
//                placeholderText = Label.PATH_TO_CA_CERTIFICATE,
//                modifier = inputModifier,
//                textStyle = Theme.typography.body1,
//                onValueChange = { FormState.caCertificate = it },
//                enabled = Service.connection.isDisconnected()
//            )
        }
    }

    @Composable
    private fun ServerConnectionStatus() {
        val statusText = "${Label.STATUS}: ${Service.connection.status.name.lowercase()}"
        Text(text = statusText, style = Theme.typography.body1)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun DisconnectedFormButtons() {
//        StudioButton(text = Label.CANCEL, onClick = { Service.connection.openDialog = false })
//        StudioButton(text = Label.CONNECT, onClick = { FormState.trySubmit() })
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ConnectedFormButtons() {
//        StudioButton(text = Label.DISCONNECT, onClick = { Service.connection.disconnect() })
//        StudioButton(text = Label.CLOSE, onClick = { Service.connection.openDialog = false })
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ConnectingFormButtons() {
//        StudioButton(text = Label.CANCEL, onClick = { Service.connection.disconnect() })
//        StudioButton(text = Label.CONNECTING, onClick = {}, enabled = false)
    }
}
