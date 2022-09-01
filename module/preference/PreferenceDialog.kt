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

package com.vaticle.typedb.studio.module.preference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form.IconArg
import com.vaticle.typedb.studio.framework.material.Form.State
import com.vaticle.typedb.studio.framework.material.Form.FormRowSpacer
import com.vaticle.typedb.studio.framework.material.Form.FormColumnSpacer
import com.vaticle.typedb.studio.framework.material.Form.Submission
import com.vaticle.typedb.studio.framework.material.Form.Field
import com.vaticle.typedb.studio.framework.material.Form.Checkbox
import com.vaticle.typedb.studio.framework.material.Form.TextInput
import com.vaticle.typedb.studio.framework.material.Form.TextButton
import com.vaticle.typedb.studio.framework.material.Form.Text
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Navigator
import com.vaticle.typedb.studio.framework.material.Navigator.rememberNavigatorState
import com.vaticle.typedb.studio.framework.material.Separator
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label

object PreferenceDialog {
    private val WIDTH = 800.dp
    private val HEIGHT = 800.dp
    private val appData = StudioState.appData.preferences

    private class PreferencesForm : State {
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
    private fun NavigatorLayout() {
        val pref1 = PrefState("Graph")
        val pref2 = PrefState("Text Editor")
        val prefState = PrefState("Root", listOf(pref1, pref2))
        val navState = rememberNavigatorState(
            container = prefState,
            title = Label.MANAGE_PREFERENCES,
            mode = Navigator.Mode.LIST,
            initExpandDepth = 1,
        ) { }

        Navigator.Layout(
            state = navState,
            iconArg = { IconArg(Icon.Code.GEAR) }
        )

        LaunchedEffect(navState) { navState.launch() }
    }

    @Composable
    fun MayShowDialogs() {
        if (StudioState.preference.openPreferenceDialog.isOpen) Preferences()
    }

    @Composable
    private fun Preferences() {
        val state = remember { PreferencesForm() }

        Dialog.Layout(StudioState.preference.openPreferenceDialog, Label.MANAGE_PREFERENCES, WIDTH, HEIGHT) {
            Submission(state, modifier = Modifier.fillMaxSize(), showButtons = false) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column() {
                        NavigatorLayout()
                    }
//                    Column {
//                        SpacedVerticalSeperator()
//                    }
                    Column() {
                        EditorPreferences(state)
                        FormColumnSpacer()

                        ProjectPreferences(state)
                        FormColumnSpacer()

                        GraphPreferences(state)
                        FormColumnSpacer()

                        QueryPreferences(state)
                        FormColumnSpacer()
                    }
                }
                Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.Bottom) {
                    Column() {
                        SpacedHorizontalSeperator()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            ChangeFormButtons(state)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ProjectPreferences(state: PreferencesForm) {
        ProjectIgnoredPathsField(state)
    }

    @Composable
    private fun EditorPreferences(state: PreferencesForm) {
        EditorAutoSaveField(state)
    }

    @Composable
    private fun QueryPreferences(state: PreferencesForm) {
        QueryLimitField(state)
    }

    @Composable
    private fun GraphPreferences(state: PreferencesForm) {
        GraphOutputField(state)
    }

    @Composable
    private fun GraphOutputField(state: PreferencesForm) {
        Field(label = Label.ENABLE_GRAPH_OUTPUT) {
            Checkbox(
                value = state.graphOutput,
            ) { state.graphOutput = it }
        }
    }

    @Composable
    private fun ProjectIgnoredPathsField(state: PreferencesForm) {
        Field(label = Label.PROJECT_IGNORED_PATHS) {
            TextInput(
                value = state.ignoredPaths.toString(),
                placeholder = ".git",
                onValueChange = { state.limit = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Composable
    private fun EditorAutoSaveField(state: PreferencesForm) {
        Field(label = Label.ENABLE_EDITOR_AUTOSAVE) {
            Checkbox(
                value = state.autoSave,
            ) { state.autoSave = it }
        }
    }

    @Composable
    private fun QueryLimitField(state: PreferencesForm) {
        Field(label = Label.SET_QUERY_LIMIT) {
            TextInput(
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
        SpacedHorizontalSeperator()
    }

    @Composable
    private fun SpacedHorizontalSeperator() {
        FormColumnSpacer()
        Separator.Horizontal()
        FormColumnSpacer()
    }

    @Composable
    private fun SpacedVerticalSeperator() {
        FormRowSpacer()
        Separator.Vertical()
        FormRowSpacer()
    }

    @Composable
    private fun ChangeFormButtons(state: PreferencesForm) {
        TextButton(Label.CANCEL) {
//            state.cancel()
        }
        FormRowSpacer()
        TextButton(Label.APPLY) {
//            state.apply()
        }
        FormRowSpacer()
        TextButton(Label.OK) {
//            state.ok()
        }
    }
}