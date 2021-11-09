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
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.login.ServerSoftware.*
import com.vaticle.typedb.studio.ui.elements.StudioButton
import com.vaticle.typedb.studio.ui.elements.StudioDropdownBox
import com.vaticle.typedb.studio.ui.elements.StudioTextField

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CoreLoginPanel(form: LoginScreenState, onDatabaseDropdownFocused: () -> Unit, onSelectDatabase: (dbName: String) -> Unit,
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
                Text("Database", style = StudioTheme.typography.body1, modifier = labelWeightModifier)
                StudioDropdownBox(items = form.allDBNames, text = form.dbFieldText, onTextChange = { onSelectDatabase(it) },
                    modifier = fieldWeightModifier.height(28.dp),
                    textFieldModifier = Modifier.onFocusChanged {
                        // sanity check - for some reason onFocusChanged triggers when switching tabs
                        if (it.isFocused && form.serverSoftware == CORE) onDatabaseDropdownFocused()
                    })
            }
        }

        StudioButton(text = "Connect to TypeDB", enabled = form.databaseSelected, onClick = { onSubmit() })

        LaunchedEffect(Unit) { focusManager.moveFocus(FocusDirection.Down) }
    }
}
