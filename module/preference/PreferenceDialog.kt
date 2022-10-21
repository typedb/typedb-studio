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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.Field
import com.vaticle.typedb.studio.framework.material.Form.State
import com.vaticle.typedb.studio.framework.material.Form.Text
import com.vaticle.typedb.studio.framework.material.Form.TextButton
import com.vaticle.typedb.studio.framework.material.Frame
import com.vaticle.typedb.studio.framework.material.Navigator
import com.vaticle.typedb.studio.framework.material.Navigator.rememberNavigatorState
import com.vaticle.typedb.studio.framework.material.Separator
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Label.APPLY
import com.vaticle.typedb.studio.service.common.util.Label.CANCEL
import com.vaticle.typedb.studio.service.common.util.Label.ENABLE_EDITOR_AUTOSAVE
import com.vaticle.typedb.studio.service.common.util.Label.ENABLE_GRAPH_OUTPUT
import com.vaticle.typedb.studio.service.common.util.Label.GRAPH_VISUALISER
import com.vaticle.typedb.studio.service.common.util.Label.MANAGE_PREFERENCES
import com.vaticle.typedb.studio.service.common.util.Label.OK
import com.vaticle.typedb.studio.service.common.util.Label.PROJECT_IGNORED_PATHS
import com.vaticle.typedb.studio.service.common.util.Label.PROJECT_MANAGER
import com.vaticle.typedb.studio.service.common.util.Label.QUERY_RUNNER
import com.vaticle.typedb.studio.service.common.util.Label.RESET
import com.vaticle.typedb.studio.service.common.util.Label.SET_QUERY_LIMIT
import com.vaticle.typedb.studio.service.common.util.Label.TEXT_EDITOR
import com.vaticle.typedb.studio.service.common.util.Sentence.IGNORED_PATHS_CAPTION
import com.vaticle.typedb.studio.service.common.util.Sentence.PREFERENCES_GRAPH_OUTPUT_CAPTION
import com.vaticle.typedb.studio.service.common.util.Sentence.PREFERENCES_MATCH_QUERY_LIMIT_CAPTION
import com.vaticle.typedb.studio.service.page.Navigable

object PreferenceDialog {
    private val WIDTH = 800.dp
    private val HEIGHT = 600.dp
    private val NAVIGATOR_INIT_SIZE = 200.dp
    private val NAVIGATOR_MIN_SIZE = 150.dp
    private val PREFERENCE_GROUP_INIT_SIZE = 600.dp
    private val PREFERENCE_GROUP_MIN_SIZE = 500.dp
    private val RESET_BUTTON_HEIGHT = 20.dp
    private val MULTILINE_FIELD_HEIGHT = Form.FIELD_HEIGHT * 5

    private val preferenceSrv = Service.preference

    private var selectedPreferenceGroup by mutableStateOf<PreferenceGroup>(PreferenceGroup.Root())
    private var state by mutableStateOf(PreferencesForm())

    sealed class PreferenceField(private val label: String, private val caption: String?,
                                 private val fieldHeight: Dp = Form.FIELD_HEIGHT) {
        abstract fun isValid(): Boolean

        @Composable
        abstract fun Display()

        var modified by mutableStateOf(false)

        @Composable
        fun Layout(caption: String?, fieldContent: @Composable () -> Unit) {
            Field(label, caption, fieldHeight) {
                fieldContent()
            }
        }

        class TextInputValidated(
            initValue: String,
            label: String, private val caption: String? = null,
            private val placeholder: String, private val invalidWarning: String,
            private val validator: (String) -> Boolean = { true }
        ) : PreferenceField(label, caption) {

            var value by mutableStateOf(initValue)

            @Composable
            override fun Display() {
                Layout(caption) {
                    val borderColour = if (this.isValid()) Theme.studio.border else Theme.studio.errorStroke
                    val modifier = Modifier.border(1.dp, borderColour, RoundedCornerShape(Theme.ROUNDED_CORNER_RADIUS))
                    val positionProvider = rememberComponentRectPositionProvider(
                        anchor = Alignment.TopStart,
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset(0.dp, -(Form.FIELD_HEIGHT.value + Form.FIELD_SPACING.value).dp)
                    )
                    Form.TextInput(
                        value = value,
                        placeholder = placeholder,
                        modifier = modifier,
                        onValueChange = { value = it; modified = true }
                    )
                    if (!this.isValid()) {
                        InvalidPopup(invalidWarning, positionProvider)
                    }
                }
            }

            override fun isValid(): Boolean {
                return validator(value)
            }

            @Composable
            fun InvalidPopup(text: String, popupPositionProvider: PopupPositionProvider) {
                Popup(
                    popupPositionProvider
                ) {
                    Box(
                        Modifier.background(color = Theme.studio.errorBackground)
                            .border(Form.BORDER_WIDTH, Theme.studio.errorStroke, RectangleShape)
                    ) {
                        Column {
                            Row(Modifier.padding(5.dp), Arrangement.SpaceBetween) {
                                Text(value = text)
                            }
                        }
                    }
                }
            }
        }

        class TextInput(
            initValue: String,
            label: String, private val caption: String? = null,
            private val placeholder: String
        ) : PreferenceField(label, caption) {

            var value by mutableStateOf(initValue)

            @Composable
            override fun Display() {
                Layout(caption) {
                    Form.TextInput(
                        value = value,
                        placeholder = placeholder,
                        modifier = Modifier.border(
                            1.dp,  Theme.studio.border , RoundedCornerShape(Theme.ROUNDED_CORNER_RADIUS)
                        ),
                        onValueChange = { value = it; modified = true }
                    )
                }
            }

            override fun isValid(): Boolean {
                return true
            }
        }

        class MultilineTextInput(
            initValue: String,
            label: String, private val caption: String? = null,
        ): PreferenceField(label, caption, fieldHeight = MULTILINE_FIELD_HEIGHT) {
            var value by mutableStateOf(TextFieldValue(initValue))

            @Composable
            override fun Display() {
                Layout(caption) {
                    Form.MultilineTextInput(
                        value = value,
                        onValueChange = { value = it; modified = true },
                        onTextLayout = { },
                        textFieldPadding = Form.MULTILINE_INPUT_PADDING,
                        modifier = Modifier.border(
                            1.dp, Theme.studio.border, RoundedCornerShape(Theme.ROUNDED_CORNER_RADIUS)
                        ),
                    )
                }
            }

            override fun isValid(): Boolean {
                return true
            }
        }

        class Checkbox(
            initValue: Boolean, label: String, private val caption: String? = null
        ) : PreferenceField(label, caption) {

            var value by mutableStateOf(initValue)

            @Composable
            override fun Display() {
                Layout(caption) {
                    Form.Checkbox(
                        value = value,
                        onChange = { value = it; modified = true }
                    )
                }
            }

            override fun isValid(): Boolean {
                return true
            }
        }

        class Dropdown<T : Any>(
            initValue: T, val values: List<T>, label: String, private val caption: String? = null
        ) : PreferenceField(label, caption) {

            private var selected by mutableStateOf(values.find { it == initValue })

            @Composable
            override fun Display() {
                Layout(caption) {
                    Form.Dropdown(
                        values = values,
                        selected = selected,
                        onSelection = { selected = it!!; modified = true }
                    )
                }
            }

            override fun isValid(): Boolean {
                return true
            }
        }
    }

    class PreferencesForm : State {
        private val preferenceGroups: List<PreferenceGroup> = listOf(
            PreferenceGroup.GraphVisualiser(),
            PreferenceGroup.TextEditor(),
            PreferenceGroup.Project(),
            PreferenceGroup.QueryRunner()
        )

        val rootPreferenceGroup = PreferenceGroup.Root(entries = preferenceGroups)

        override fun cancel() {
            Service.preference.preferencesDialog.close()
        }

        fun apply() = trySubmit()

        fun ok() {
            apply()
            if (rootPreferenceGroup.isValid()) cancel()
        }

        fun isModified(): Boolean {
            return rootPreferenceGroup.isModified()
        }

        override fun isValid(): Boolean {
            return rootPreferenceGroup.isValid()
        }

        override fun trySubmit() {
            if (preferenceGroups.all { it.isValid() }) {
                preferenceGroups.forEach {
                    it.submit()
                }
            }
        }
    }

    abstract class PreferenceGroup(
        override val name: String = "",
        override val entries: List<PreferenceGroup> = emptyList(),
        override val parent: Navigable<PreferenceGroup>? = null,
        override val isExpandable: Boolean = entries.isNotEmpty(),
        override val isBulkExpandable: Boolean = entries.isNotEmpty(),
        open val preferences: List<PreferenceField> = emptyList(),
    ) : Navigable<PreferenceGroup> {

        override val info: String? = null

        abstract fun submit()
        abstract fun reset()

        fun resetSelfAndDescendants() {
            this.reset()
            entries.forEach { it.resetSelfAndDescendants() }
        }

        override fun reloadEntries() {}

        override fun compareTo(other: Navigable<PreferenceGroup>): Int {
            return this.name.compareTo(other.name)
        }

        fun isModified(): Boolean {
            return preferences.any { it.modified } || entries.any { it.isModified() }
        }

        fun isValid(): Boolean {
            return preferences.all { it.isValid() } &&
                    entries.all { it.isValid() }
        }

        @Composable
        fun Display() {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        PreferencesHeader(name)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        ResetButton(this@PreferenceGroup)
                    }
                }
            }
            SpacedHorizontalSeparator()
            preferences.forEach { it.Display() }
        }

        class Root(override val entries: List<PreferenceGroup> = emptyList()) : PreferenceGroup(entries = entries) {
            override val preferences: List<PreferenceField> = emptyList()

            override fun submit() {}

            override fun reset() {}
        }

        class GraphVisualiser : PreferenceGroup(GRAPH_VISUALISER) {
            private var graphOutput = PreferenceField.Checkbox(
                initValue = preferenceSrv.graphOutputEnabled, label = ENABLE_GRAPH_OUTPUT,
                caption = PREFERENCES_GRAPH_OUTPUT_CAPTION
            )

            override val preferences: List<PreferenceField> = listOf(graphOutput)

            override fun submit() {
                preferenceSrv.graphOutputEnabled = graphOutput.value
                graphOutput.modified = false
            }

            override fun reset() {
                graphOutput.value = preferenceSrv.graphOutputEnabled
                graphOutput.modified = false
            }
        }

        class TextEditor : PreferenceGroup(TEXT_EDITOR) {
            private var autoSave = PreferenceField.Checkbox(
                initValue = preferenceSrv.autoSave, label = ENABLE_EDITOR_AUTOSAVE
            )

            override val preferences: List<PreferenceField> = listOf(autoSave)

            override fun submit() {
                preferenceSrv.autoSave = autoSave.value
                autoSave.modified = false
            }

            override fun reset() {
                autoSave.value = preferenceSrv.autoSave
                autoSave.modified = false
            }
        }

        class Project : PreferenceGroup(PROJECT_MANAGER) {
            private val ignoredPathsString = preferenceSrv.ignoredPaths.joinToString("\n")
            private var ignoredPaths = PreferenceField.MultilineTextInput(
                initValue = ignoredPathsString,
                label = PROJECT_IGNORED_PATHS,
                caption = IGNORED_PATHS_CAPTION,
            )

            override val preferences: List<PreferenceField> = listOf(ignoredPaths)

            override fun submit() {
                preferenceSrv.ignoredPaths = ignoredPaths.value.text.split('\n').map { it.trim() }
                ignoredPaths.modified = false
            }

            override fun reset() {
                ignoredPaths.value = preferenceSrv.ignoredPaths.joinToString("\n").let { TextFieldValue(it) }
                ignoredPaths.modified = false
            }
        }

        class QueryRunner : PreferenceGroup(QUERY_RUNNER) {
            companion object {
                private const val QUERY_LIMIT_PLACEHOLDER = "1000"
            }

            private var matchQueryLimit = PreferenceField.TextInputValidated(
                initValue = preferenceSrv.matchQueryLimit.toString(),
                label = SET_QUERY_LIMIT, placeholder = QUERY_LIMIT_PLACEHOLDER,
                invalidWarning = Label.PREFERENCE_INTEGER_WARNING, caption = PREFERENCES_MATCH_QUERY_LIMIT_CAPTION
            ) {/* validator = */ it.toLongOrNull() != null && it.toLongOrNull()!! >= 0 }

            override val preferences: List<PreferenceField> = listOf(matchQueryLimit)

            override fun submit() {
                preferenceSrv.matchQueryLimit = matchQueryLimit.value.toLong()
                matchQueryLimit.modified = false
            }

            override fun reset() {
                matchQueryLimit.value = preferenceSrv.matchQueryLimit.toString()
                matchQueryLimit.modified = false
            }
        }
    }

    @Composable
    private fun NavigatorLayout() {
        val navState = rememberNavigatorState(
            container = state.rootPreferenceGroup,
            title = MANAGE_PREFERENCES,
            behaviour = Navigator.Behaviour.Browser(clicksToOpenItem = 1),
            initExpandDepth = 0,
            openFn = { selectedPreferenceGroup = it.item },
        )

        Navigator.Layout(
            state = navState,
            modifier = Modifier.fillMaxSize(),
        )

        LaunchedEffect(navState) {
            navState.launch()
            navState.maySelectFirstWithoutFocus()
        }
    }

    @Composable
    fun MayShowDialogs() {
        if (Service.preference.preferencesDialog.isOpen) Preferences()
    }

    @Composable
    private fun Preferences() {
        state.rootPreferenceGroup.resetSelfAndDescendants()

        Dialog.Layout(Service.preference.preferencesDialog, MANAGE_PREFERENCES, WIDTH, HEIGHT, padding = 0.dp) {
            Column {
                Frame.Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    separator = Frame.SeparatorArgs(Separator.WEIGHT),
                    Frame.Pane(
                        id = PreferenceDialog.javaClass.canonicalName + ".primary",
                        initSize = Either.first(NAVIGATOR_INIT_SIZE), minSize = NAVIGATOR_MIN_SIZE
                    ) {
                        Column(modifier = Modifier.fillMaxSize().background(Theme.studio.backgroundLight)) {
                            Spacer(Modifier.height(Theme.DIALOG_PADDING))
                            NavigatorLayout()
                        }
                    },
                    Frame.Pane(
                        id = PreferenceDialog.javaClass.canonicalName + ".secondary",
                        initSize = Either.first(PREFERENCE_GROUP_INIT_SIZE), minSize = PREFERENCE_GROUP_MIN_SIZE
                    ) {
                        Column(modifier = Modifier.fillMaxHeight().padding(Theme.DIALOG_PADDING)) {
                            selectedPreferenceGroup.Display()
                        }
                    }
                )
                Separator.Horizontal()
                Row(modifier = Modifier.fillMaxWidth().padding(Theme.DIALOG_PADDING), horizontalArrangement = Arrangement.End) {
                    ChangeFormButtons(state)
                }
            }
        }
        LaunchedEffect(Unit) {
            state.rootPreferenceGroup.resetSelfAndDescendants()
            selectedPreferenceGroup = state.rootPreferenceGroup.entries.first()
        }
    }

    @Composable
    private fun PreferencesHeader(text: String) {
        Text(text, fontWeight = FontWeight.Bold)
    }

    @Composable
    private fun ChangeFormButtons(state: PreferencesForm) {
        TextButton(CANCEL) {
            state.cancel()
        }
        Form.RowSpacer()
        TextButton(APPLY, enabled = state.isModified() && state.isValid()) {
            state.apply()
        }
        Form.RowSpacer()
        TextButton(OK) {
            state.ok()
        }
    }

    @Composable
    private fun ResetButton(preferenceGroup: PreferenceGroup) {
        if (preferenceGroup.isModified()) {
            TextButton(RESET, modifier = Modifier.height(RESET_BUTTON_HEIGHT)) {
                preferenceGroup.reset()
            }
        }
    }

    @Composable
    private fun SpacedHorizontalSeparator() {
        Form.ColumnSpacer()
        Separator.Horizontal()
        Form.ColumnSpacer()
    }
}