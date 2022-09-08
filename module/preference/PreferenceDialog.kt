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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.State
import com.vaticle.typedb.studio.framework.material.Form.FormRowSpacer
import com.vaticle.typedb.studio.framework.material.Form.FormColumnSpacer
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
import com.vaticle.typedb.studio.state.page.Navigable

object PreferenceDialog {
    private val WIDTH = 800.dp
    private val HEIGHT = 600.dp
    private val NAVIGATOR_INIT_SIZE = 200.dp
    private val NAVIGATOR_MIN_SIZE = 150.dp
    private val STATE_INIT_SIZE = 600.dp
    private val STATE_MIN_SIZE = 500.dp
    private val QUERY_LIMIT_PLACEHOLDER = "1000"
    private val IGNORED_PATHS_PLACEHOLDER = ".git, .typedb-studio"

    private val appData = StudioState.appData.preferences
    private var focusedPreferenceGroup by mutableStateOf(PreferenceGroup(""))

    sealed interface Preference {
        @Composable fun display()
        fun valid(): Boolean

        class TextInput(initial: String, var label: String, private var placeholder: String,
                        var validator: (String) -> Boolean = { true }) : Preference {

            var value by mutableStateOf(initial)

            override fun valid(): Boolean {
                return validator(value)
            }

            @Composable
            override fun display() {
                var border = Form.Border(1.dp, RoundedCornerShape(Theme.ROUNDED_CORNER_RADIUS)) {
                    if (this.valid()) {
                        Theme.studio.border
                    } else {
                        Theme.studio.errorStroke
                    }
                }

                Field(label = label) {
                    TextInput(
                        value = value,
                        placeholder = placeholder,
                        border = border,
                        onValueChange = { value = it }
                    )
                }
            }
        }

        class Checkbox(initial: Boolean, var label: String): Preference {
            var value by mutableStateOf(initial)
            @Composable
            override fun display() {
                Field(label = label) {
                    Checkbox(
                        value = value,
                        onChange = { value = it }
                    )
                }
            }

            override fun valid(): Boolean {
                return true
            }
        }
    }


    class PreferencesForm : State {
        // Graph Visualiser Preferences
        var graphOutput = Preference.Checkbox(appData.graphOutput, Label.ENABLE_GRAPH_OUTPUT)
        // Project Manager Preferences
        val ignoredPathsData = appData.ignoredPaths.toString()
        var ignoredPaths = Preference.TextInput(ignoredPathsData.substring(1, ignoredPathsData.length - 1),
            Label.PROJECT_IGNORED_PATHS, IGNORED_PATHS_PLACEHOLDER)
        // Query Runner Preferences
        var limit = Preference.TextInput(appData.limit, Label.SET_QUERY_LIMIT, QUERY_LIMIT_PLACEHOLDER) { it.toLongOrNull() != null }
        // Text Editor Preferences
        var autoSave = Preference.Checkbox(appData.autoSave, Label.ENABLE_EDITOR_AUTOSAVE)

        override fun cancel() {
            StudioState.preference.openPreferenceDialog.close()
        }

        fun apply() {
            trySubmit()
        }

        fun ok() {
            apply()
            cancel()
        }

        override fun isValid(): Boolean {
            return graphOutput.valid() && ignoredPaths.valid() && limit.valid() && autoSave.valid()
        }

        override fun trySubmit() {
            if (isValid()) {
                appData.autoSave = autoSave.value
                appData.ignoredPaths = ignoredPaths.value.split(',').map { it.trim() }
                appData.limit = limit.value
                appData.graphOutput = graphOutput.value
            }
        }
    }

    class PreferenceGroup(
        override val name: String,
        override var entries: List<PreferenceGroup> = emptyList(),
        val content: @Composable () -> Unit = {},
    ) : Navigable<PreferenceGroup> {

        override var parent: Navigable<PreferenceGroup>? = null
        override val info: String? = null
        override val isExpandable = entries.isNotEmpty()
        override val isBulkExpandable = false
        val isRoot: Boolean
            get() = parent == null

        override fun reloadEntries() {}

        override fun compareTo(other: Navigable<PreferenceGroup>): Int {
            return this.name.compareTo(other.name);
        }

        private fun addParent(parent: PreferenceGroup) {
            this.parent = parent
        }

        fun addEntry(entry: PreferenceGroup) {
            this.entries = entries + entry
            entry.addParent(this)
        }
    }

    @Composable
    private fun NavigatorLayout(state: PreferencesForm) {
        val preferenceGroups = listOf(
            PreferenceGroup(Label.GRAPH_VISUALISER, content = { GraphPreferences(state) }),
            PreferenceGroup(Label.PROJECT_MANAGER, content = { ProjectPreferences(state) }),
            PreferenceGroup(Label.QUERY_RUNNER, content = { QueryPreferences(state)}),
            PreferenceGroup(Label.TEXT_EDITOR, content = { EditorPreferences(state) }),
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
            Column {
                Frame.Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    separator = Frame.SeparatorArgs(Separator.WEIGHT),
                    Frame.Pane(id = PreferenceDialog.javaClass.canonicalName + ".primary",
                        initSize = Either.first(NAVIGATOR_INIT_SIZE), minSize = NAVIGATOR_MIN_SIZE) {
                        Column(modifier = Modifier.fillMaxSize().background(Theme.studio.backgroundLight)) {
                            FormColumnSpacer()
                            NavigatorLayout(state)
                        }
                    },
                    Frame.Pane(id = PreferenceDialog.javaClass.canonicalName + ".secondary",
                        initSize = Either.first(STATE_INIT_SIZE), minSize = STATE_MIN_SIZE) {
                        Column(modifier = Modifier.fillMaxHeight().padding(10.dp)) {
                            if (!focusedPreferenceGroup.isRoot) {
                                PreferencesHeader(focusedPreferenceGroup.name)
                            }
                            focusedPreferenceGroup.content()
                        }
                    }
                )
                Separator.Horizontal()
                FormColumnSpacer()
                Row {
                    Column() {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            ChangeFormButtons(state)
                            FormRowSpacer()
                            FormRowSpacer()
                        }
                    }
                }
                FormColumnSpacer()
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
        state.ignoredPaths.display()
    }

    @Composable
    private fun EditorPreferences(state: PreferencesForm) {
        state.autoSave.display()
    }

    @Composable
    private fun QueryPreferences(state: PreferencesForm) {
        state.limit.display()
    }

    @Composable
    private fun GraphPreferences(state: PreferencesForm) {
        state.graphOutput.display()
        FormRowSpacer()
        FormRowSpacer()
        Text(Label.GRAPH_MATCH_CAPTION)
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
    private fun ChangeFormButtons(state: PreferencesForm) {
        TextButton(Label.CANCEL) {
            state.cancel()
        }
        FormRowSpacer()
        TextButton(Label.APPLY) {
            state.apply()
        }
        FormRowSpacer()
        TextButton(Label.OK) {
            state.ok()
        }
    }
}