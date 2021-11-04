package com.vaticle.typedb.studio.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.login.ServerSoftware.*
import com.vaticle.typedb.studio.ui.elements.StudioButton
import com.vaticle.typedb.studio.ui.elements.StudioDropdownBox
import com.vaticle.typedb.studio.ui.elements.StudioTextField

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClusterLoginPanel(form: LoginScreenState, onDatabaseDropdownFocused: () -> Unit, onSelectDatabase: (dbName: String) -> Unit,
                      onSubmit: () -> Unit) {
    val focusManager: FocusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)) {

        FormFieldGroup {
            FormField {
                Text("Server address", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                StudioTextField(value = form.serverAddress, onValueChange = { value -> form.serverAddress = value },
                    textStyle = StudioTheme.typography.body1, modifier = fieldWeightModifier.height(28.dp))
            }
            FormField {
                Text("Username", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                StudioTextField(value = form.username, onValueChange = { value -> form.username = value },
                    textStyle = StudioTheme.typography.body1, modifier = fieldWeightModifier.height(28.dp))
            }
            FormField {
                Text("Password", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                StudioTextField(value = form.password, onValueChange = { value -> form.password = value },
                    textStyle = StudioTheme.typography.body1, modifier = fieldWeightModifier.height(28.dp),
                    visualTransformation = PasswordVisualTransformation())
            }
            FormField {
                Text("Root CA path", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                StudioTextField(value = form.rootCAPath, onValueChange = { value -> form.rootCAPath = value },
                    textStyle = StudioTheme.typography.body1, modifier = fieldWeightModifier.height(28.dp))
            }
            FormField {
                Text("Database", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                StudioDropdownBox(items = form.allDBNames, text = form.dbFieldText, onTextChange = { onSelectDatabase(it) },
                    modifier = fieldWeightModifier.height(28.dp),
                    textFieldModifier = Modifier.onFocusChanged { if (it.isFocused && form.serverSoftware == CLUSTER) onDatabaseDropdownFocused() })
            }
        }

        StudioButton(text = "Connect to TypeDB Cluster", enabled = form.databaseSelected, onClick = { onSubmit() })

        LaunchedEffect(Unit) { focusManager.moveFocus(FocusDirection.Down) }
    }
}
