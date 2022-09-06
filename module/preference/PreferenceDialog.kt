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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form.State
import com.vaticle.typedb.studio.framework.material.Form.FormHorizontalSpacer
import com.vaticle.typedb.studio.framework.material.Form.FormVerticalSpacer
import com.vaticle.typedb.studio.framework.material.Form.Field
import com.vaticle.typedb.studio.framework.material.Form.Checkbox
import com.vaticle.typedb.studio.framework.material.Form.TextInput
import com.vaticle.typedb.studio.framework.material.Form.TextButton
import com.vaticle.typedb.studio.framework.material.Form.Text
import com.vaticle.typedb.studio.framework.material.Frame
import com.vaticle.typedb.studio.framework.material.Navigator
import com.vaticle.typedb.studio.framework.material.Navigator.rememberNavigatorState
import com.vaticle.typedb.studio.framework.material.Separator
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label

object PreferenceDialog {
    private val WIDTH = 800.dp
    private val HEIGHT = 600.dp
    private val NAVIGATOR_INIT_SIZE = 200.dp
    private val NAVIGATOR_MIN_SIZE = 150.dp
    private val STATE_INIT_SIZE = 1f
    private val STATE_MIN_SIZE = 500.dp
    private val appData = StudioState.appData.preferences
    private var focusedPreferenceGroup by mutableStateOf(PreferenceGroup(""))

    class PreferencesForm : State {
        var autoSave by mutableStateOf(true)
        var ignoredPaths by mutableStateOf(listOf(".git"))
        var limit: String by mutableStateOf(appData.limit ?: "")
        var graphOutput: Boolean by mutableStateOf(appData.graphOutput ?: true)

        override fun cancel() {
            StudioState.preference.openPreferenceDialog.close()
        }

        override fun isValid(): Boolean {
            return true
//            return (limit.toIntOrNull() != null)
        }

        override fun trySubmit() {
            appData.limit = limit
            appData.graphOutput = graphOutput
        }
    }

    @Composable
    private fun NavigatorLayout(state: PreferencesForm) {
        val preferenceGroups = listOf(
            PreferenceGroup(Label.GRAPH_VISUALISER, content = { GraphPreferences(state) }),
            PreferenceGroup(Label.TEXT_EDITOR, content = { EditorPreferences(state) }),
            PreferenceGroup(Label.QUERY_RUNNER, content = { QueryPreferences(state)}),
            PreferenceGroup(Label.PROJECT_MANAGER, content = { ProjectPreferences(state) }),
        )

        val rootPreferenceGroup = PreferenceGroup("Root", content = { RootPreferences() })

        for (group in preferenceGroups) {
            rootPreferenceGroup.addEntry(group)
        }

        focusedPreferenceGroup = rootPreferenceGroup

        val navState = rememberNavigatorState(
            container = rootPreferenceGroup,
            title = Label.MANAGE_PREFERENCES,
            mode = Navigator.Mode.BROWSER,
            initExpandDepth = 0,
            openFn = { focusedPreferenceGroup = it.item }
        )

        Navigator.Layout(
            state = navState,
            modifier = Modifier.fillMaxSize(),
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

        Dialog.Layout(StudioState.preference.openPreferenceDialog, Label.MANAGE_PREFERENCES, WIDTH, HEIGHT, false) {
//            Submission(state, modifier = Modifier.fillMaxSize(), showButtons = false) {
            Column {
                Frame.Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    separator = Frame.SeparatorArgs(Separator.WEIGHT),
                    Frame.Pane(id = PreferenceDialog.javaClass.canonicalName + ".primary", initSize = Either.first(NAVIGATOR_INIT_SIZE), minSize = NAVIGATOR_MIN_SIZE) {
                        Column(modifier = Modifier.fillMaxSize().background(Theme.studio.backgroundLight)) {
                            FormVerticalSpacer()
                            NavigatorLayout(state)
                        }
                    },
                    Frame.Pane(id = PreferenceDialog.javaClass.canonicalName + ".secondary", initSize = Either.second(STATE_INIT_SIZE), minSize = STATE_MIN_SIZE) {
                        Column(modifier = Modifier.fillMaxHeight().padding(10.dp)) {
                            if (!focusedPreferenceGroup.isRoot) {
                                PreferencesHeader(focusedPreferenceGroup.name)
                            }
                            focusedPreferenceGroup.content()
                        }
                    }
                )
                Separator.Horizontal()
                FormVerticalSpacer()
                Row {
                    Column() {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            ChangeFormButtons(state)
                            FormHorizontalSpacer()
                            FormHorizontalSpacer()
                        }
                    }
                }
                FormVerticalSpacer()
            }
        }
    }

    @Composable
    private fun RootPreferences() {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(Label.SELECT_PREFERENCE_GROUP)
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
        FormVerticalSpacer()
        Separator.Horizontal()
        FormVerticalSpacer()
    }

    @Composable
    private fun ChangeFormButtons(state: PreferencesForm) {
        TextButton(Label.CANCEL) {
            state.cancel()
        }
        FormHorizontalSpacer()
        TextButton(Label.APPLY) {
//            state.apply()
        }
        FormHorizontalSpacer()
        TextButton(Label.OK) {
//            state.ok()
        }
    }
}