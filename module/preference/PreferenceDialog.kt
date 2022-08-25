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

//                var borderColor by remember { mutableStateOf(defaultBorderColor) }
//                var border = Form.Border(1.dp, RoundedCornerShape(Theme.ROUNDED_CORNER_RADIUS)) { borderColor }
//                Form.TextInput(value, preference.placeholder, border = border, onValueChange = {
//                    value = it
//                    preference.value = value
//                    borderColor = if (!preference.validInput()) {
//                        errorBorderColor
//                    } else {
//                        defaultBorderColor
//                    }
//                })

package com.vaticle.typedb.studio.module.preference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.Submission
import com.vaticle.typedb.studio.framework.material.Form.Text
import com.vaticle.typedb.studio.framework.material.Navigator
import com.vaticle.typedb.studio.framework.material.Separator
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label

object PreferenceDialog {
    private val preferencePages = listOf("Graph Visualiser, Text Editor, Query Runner")

    private val WIDTH = 600.dp
    private val HEIGHT = 600.dp
    private val appData = StudioState.appData.preferences
    private val preferencesNavState = Navigator.NavigatorState(
        container = preferencePages,
        title = Label.SUBTYPES_OF + " " + type.name,
        mode = Navigator.Mode.LIST,
        initExpandDepth = 4,
        coroutineScope = coroutineScope
    ) { it.item.tryOpen() }

    private class PreferencesForm : Form.State {
        var autoSave by mutableStateOf(true)
        var ignoredPaths by mutableStateOf(listOf(".git"))
        var limit: String by mutableStateOf(appData.limit ?: "")
        var graphOutput: Boolean by mutableStateOf(appData.graphOutput ?: true)

        override fun cancel() {
            StudioState.preference.openPreferenceDialog.close()
        }

        override fun isValid(): Boolean {
            return (limit.toIntOrNull() != null)
        }

        override fun trySubmit() {
            appData.limit = limit
            appData.graphOutput = graphOutput
        }
    }

    @Composable
    fun MayShowDialogs() {
        if (StudioState.preference.openPreferenceDialog.isOpen) Preferences()
    }

    @Composable
    private fun Preferences() {
        val state = remember { PreferencesForm() }

        Dialog.Layout(StudioState.preference.openPreferenceDialog, Label.MANAGE_PREFERENCES, WIDTH, HEIGHT) {
            Column(modifier = Modifier.fillMaxSize()) {
                Navigator.Layout(
                    state = preferencesNavState,
                    modifier = Modifier.weight(1f),
                    itemHeight = Navigator.ITEM_HEIGHT,
                    bottomSpace = 0.dp,
                    iconArg =
                )
            }
            Submission(state, modifier = Modifier.fillMaxSize()) {
                EditorPreferences(state)
                Form.FormColumnSpacer()

                ProjectPreferences(state)
                Form.FormColumnSpacer()

                GraphPreferences(state)
                Form.FormColumnSpacer()

                QueryPreferences(state)
                Form.FormColumnSpacer()
                Row(verticalAlignment = Alignment.Bottom) {
                    SpacedSeperator()
                    ConfirmChanges(state)
                }
            }
        }
    }


    @Composable
    private fun ProjectPreferences(state: PreferencesForm) {
        PreferencesHeader("Project")
        ProjectIgnoredPathsField(state)
    }

    @Composable
    private fun EditorPreferences(state: PreferencesForm) {
        PreferencesHeader("Editor")
        EditorAutoSaveField(state)
    }

    @Composable
    private fun QueryPreferences(state: PreferencesForm) {
        PreferencesHeader("Query")
        QueryLimitField(state)
    }

    @Composable
    private fun GraphPreferences(state: PreferencesForm) {
        PreferencesHeader("Graph")
        GraphOutputField(state)
    }

    @Composable
    private fun GraphOutputField(state: PreferencesForm) {
        Form.Field(label = Label.ENABLE_GRAPH_OUTPUT) {
            Form.Checkbox(
                value = state.graphOutput,
            ) { state.graphOutput = it }
        }
    }

    @Composable
    private fun ProjectIgnoredPathsField(state: PreferencesForm) {
        Form.Field(label = Label.PROJECT_IGNORED_PATHS) {
            Form.TextInput(
                value = state.ignoredPaths.toString(),
                placeholder = ".git",
                onValueChange = { state.limit = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun EditorAutoSaveField(state: PreferencesForm) {
        Form.Field(label = Label.ENABLE_EDITOR_AUTOSAVE) {
            Form.Checkbox(
                value = state.autoSave,
            ) { state.autoSave = it }
        }
    }

    @Composable
    private fun QueryLimitField(state: PreferencesForm) {
        Form.Field(label = Label.SET_QUERY_LIMIT) {
            Form.TextInput(
                value = state.limit,
                placeholder = "1000",
                onValueChange = { state.limit = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun PreferencesHeader(text: String) {
        Text(text, fontWeight = FontWeight.Bold)
        SpacedSeperator()
    }

    @Composable
    private fun SpacedSeperator() {
        Form.FormColumnSpacer()
        Separator.Horizontal()
        Form.FormColumnSpacer()
    }

    @Composable
    private fun ConfirmChanges(state: PreferencesForm) {
        // On accept we need to:
        // - Verify all inputs are valid for their given preference
        // - Assign them to their state variable (and thus write them to disk)
        Form.TextButton("Accept") {
            if (state.isValid()) {
                StudioState.appData.preferences.limit = state.limit
                StudioState.appData.preferences.graphOutput = state.graphOutput
                StudioState.appData.preferences.autoSave = state.autoSave
                StudioState.appData.preferences.ignoredPaths = state.ignoredPaths
                StudioState.preference.openPreferenceDialog.close()
            } else {

            }
        }
        Form.FormRowSpacer()
        Form.TextButton("Discard") { StudioState.preference.openPreferenceDialog.close() }
    }
}