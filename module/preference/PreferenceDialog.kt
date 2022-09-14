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
import com.vaticle.typedb.studio.framework.common.theme.Color
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.CaptionSpacer
import com.vaticle.typedb.studio.framework.material.Form.State
import com.vaticle.typedb.studio.framework.material.Form.RowSpacer
import com.vaticle.typedb.studio.framework.material.Form.ColumnSpacer
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
import com.vaticle.typedb.studio.state.common.util.Label.APPLY
import com.vaticle.typedb.studio.state.common.util.Label.CANCEL
import com.vaticle.typedb.studio.state.common.util.Label.ENABLE_EDITOR_AUTOSAVE
import com.vaticle.typedb.studio.state.common.util.Label.ENABLE_GRAPH_OUTPUT
import com.vaticle.typedb.studio.state.common.util.Label.GRAPH_VISUALISER
import com.vaticle.typedb.studio.state.common.util.Label.MANAGE_PREFERENCES
import com.vaticle.typedb.studio.state.common.util.Label.OK
import com.vaticle.typedb.studio.state.common.util.Label.PROJECT_IGNORED_PATHS
import com.vaticle.typedb.studio.state.common.util.Label.PROJECT_MANAGER
import com.vaticle.typedb.studio.state.common.util.Label.QUERY_RUNNER
import com.vaticle.typedb.studio.state.common.util.Label.SELECT_PREFERENCE_GROUP
import com.vaticle.typedb.studio.state.common.util.Label.SET_QUERY_LIMIT
import com.vaticle.typedb.studio.state.common.util.Label.TEXT_EDITOR
import com.vaticle.typedb.studio.state.common.util.Sentence.PREFERENCES_GRAPH_OUTPUT_CAPTION
import com.vaticle.typedb.studio.state.common.util.Sentence.PREFERENCES_QUERY_LIMIT_CAPTION
import com.vaticle.typedb.studio.state.page.Navigable

object PreferenceDialog {
    private val WIDTH = 800.dp
    private val HEIGHT = 600.dp
    private val NAVIGATOR_INIT_SIZE = 200.dp
    private val NAVIGATOR_MIN_SIZE = 150.dp
    private val STATE_INIT_SIZE = 600.dp
    private val STATE_MIN_SIZE = 500.dp

    private val appData = StudioState.appData.preferences
    private val preferenceMgr = StudioState.preference
    private var focusedPreferenceGroup by mutableStateOf(PreferenceGroup(""))

    sealed interface PreferenceField {
        @Composable fun Display()
        fun isValid(): Boolean

        class TextInput(val state: PreferencesForm, initialValue: String, var label: String, private var placeholder: String,
                        var validator: (String) -> Boolean = { true }) : PreferenceField {

            var value by mutableStateOf(initialValue)

            override fun isValid(): Boolean {
                return validator(value)
            }

            @Composable
            override fun Display() {
                var border = Form.Border(1.dp, RoundedCornerShape(Theme.ROUNDED_CORNER_RADIUS)) {
                    if (this.isValid()) Theme.studio.border else Theme.studio.errorStroke
                }

                Field(label) {
                    TextInput(
                        value = value,
                        placeholder = placeholder,
                        border = border,
                        onValueChange = { value = it; state.modified = true }
                    )
                }
            }
        }

        class Checkbox(val state: PreferencesForm, initialValue: Boolean, var label: String): PreferenceField {
            var value by mutableStateOf(initialValue)

            @Composable
            override fun Display() {
                Field(label) {
                    Checkbox(
                        value = value,
                        onChange = { value = it; state.modified = true }
                    )
                }
            }

            override fun isValid(): Boolean {
                return true
            }
        }
    }


    class PreferencesForm : State {
        private val QUERY_LIMIT_PLACEHOLDER = "1000"
        private val IGNORED_PATHS_PLACEHOLDER = ".git"
        var modified by mutableStateOf(false)

        // Graph Visualiser Preferences
        var graphOutput = PreferenceField.Checkbox(this, initialValue = preferenceMgr.graphOutputEnabled, label = ENABLE_GRAPH_OUTPUT)

        // Project Manager Preferences
        val ignoredPathsString = preferenceMgr.ignoredPaths.joinToString(",")
        var ignoredPaths = PreferenceField.TextInput(
            this, initialValue = ignoredPathsString,
            label = PROJECT_IGNORED_PATHS, placeholder = IGNORED_PATHS_PLACEHOLDER
        )

        // Query Runner Preferences
        var queryLimit = PreferenceField.TextInput(
            this,
            initialValue = preferenceMgr.matchQueryLimit.toString(),
            label = SET_QUERY_LIMIT, 
            placeholder = QUERY_LIMIT_PLACEHOLDER
        ) {/* validator = */ it.toLongOrNull() != null }
        
        // Text Editor Preferences
        var autoSave = PreferenceField.Checkbox(this, initialValue = preferenceMgr.autoSave, label = ENABLE_EDITOR_AUTOSAVE)

        override fun cancel() {
            StudioState.preference.preferencesDialog.close()
        }

        fun apply() {
            if (isValid()) {
                trySubmit()
                modified = false
            }
        }

        fun ok() {
            apply()
            cancel()
        }

        override fun isValid(): Boolean {
            return graphOutput.isValid() && ignoredPaths.isValid() && queryLimit.isValid() && autoSave.isValid()
        }


        override fun trySubmit() {
            appData.autoSave = autoSave.value
            appData.ignoredPaths = ignoredPaths.value.split(',').map { it.trim() }
            appData.matchQueryLimit = queryLimit.value
            appData.graphOutputEnabled = graphOutput.value
        }
    }

    class PreferenceGroup(
        override val name: String = "",
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
            PreferenceGroup(GRAPH_VISUALISER, content = { GraphPreferences(state) }),
            PreferenceGroup(PROJECT_MANAGER, content = { ProjectPreferences(state) }),
            PreferenceGroup(QUERY_RUNNER, content = { QueryPreferences(state) }),
            PreferenceGroup(TEXT_EDITOR, content = { EditorPreferences(state) }),
        )

        val rootPreferenceGroup = PreferenceGroup(content = { RootPreferences() })

        for (group in preferenceGroups) {
            rootPreferenceGroup.addEntry(group)
        }

        focusedPreferenceGroup = preferenceGroups.first()

        val navState = rememberNavigatorState(
            container = rootPreferenceGroup,
            title = MANAGE_PREFERENCES,
            mode = Navigator.Mode.MENU,
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
        if (StudioState.preference.preferencesDialog.isOpen) Preferences()
    }

    @Composable
    private fun Preferences() {
        val state = remember { PreferencesForm() }

        Dialog.Layout(StudioState.preference.preferencesDialog, MANAGE_PREFERENCES, WIDTH, HEIGHT, padding = 0.dp) {
            Column {
                Frame.Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    separator = Frame.SeparatorArgs(Separator.WEIGHT),
                    Frame.Pane(id = PreferenceDialog.javaClass.canonicalName + ".primary",
                        initSize = Either.first(NAVIGATOR_INIT_SIZE), minSize = NAVIGATOR_MIN_SIZE) {
                        Column(modifier = Modifier.fillMaxSize().background(Theme.studio.backgroundLight)) {
                            ColumnSpacer()
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
                ColumnSpacer()
                Row {
                    Column() {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            ChangeFormButtons(state)
                            RowSpacer()
                            RowSpacer()
                        }
                    }
                }
                ColumnSpacer()
            }
        }
    }

    @Composable
    private fun RootPreferences() {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(SELECT_PREFERENCE_GROUP)
        }

    }

    @Composable
    private fun ProjectPreferences(state: PreferencesForm) {
        state.ignoredPaths.Display()
    }

    @Composable
    private fun EditorPreferences(state: PreferencesForm) {
        state.autoSave.Display()
    }

    @Composable
    private fun QueryPreferences(state: PreferencesForm) {
        state.queryLimit.Display()
        Caption(PREFERENCES_QUERY_LIMIT_CAPTION)
    }

    @Composable
    private fun GraphPreferences(state: PreferencesForm) {
        state.graphOutput.Display()
        Caption(PREFERENCES_GRAPH_OUTPUT_CAPTION)
    }

    @Composable
    private fun PreferencesHeader(text: String) {
        Text(text, fontWeight = FontWeight.Bold)
        SpacedHorizontalSeparator()
    }

    @Composable
    private fun ChangeFormButtons(state: PreferencesForm) {
        TextButton(CANCEL) {
            state.cancel()
        }
        RowSpacer()
        TextButton(APPLY, enabled = state.modified) {
            state.apply()
        }
        RowSpacer()
        TextButton(OK) {
            state.ok()
        }
    }

    @Composable
    private fun SpacedHorizontalSeparator() {
        ColumnSpacer()
        Separator.Horizontal()
        ColumnSpacer()
    }

    @Composable
    private fun Caption(text: String) {
        CaptionSpacer()
        Row {
            RowSpacer()
            Text(text, alpha = Color.FADED_OPACITY)
        }
    }
}