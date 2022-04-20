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

package com.vaticle.typedb.studio.view.output

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.state.runner.Runner
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.IconButtonArg
import com.vaticle.typedb.studio.view.common.component.Frame
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.component.Tabs
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.PANEL_BAR_HEIGHT
import com.vaticle.typedb.studio.view.common.theme.Theme.PANEL_BAR_SPACING
import com.vaticle.typedb.studio.view.editor.TextEditor
import com.vaticle.typedb.studio.view.output.LogOutput.END_OF_OUTPUT_SPACE
import kotlinx.coroutines.CoroutineScope

object RunOutputArea {

    const val DEFAULT_OPEN = false

    class State(
        var resource: Resource,
        private val paneState: Frame.PaneState,
        private val coroutineScope: CoroutineScope
    ) {

        internal var isOpen: Boolean by mutableStateOf(DEFAULT_OPEN)
        internal val runnerTabs = Tabs.State<Runner>(coroutineScope)
        private val outputGroup: MutableMap<Runner, RunOutputGroup> = mutableMapOf()
        private var unfreezeSize: Dp? by mutableStateOf(null)

        init {
            resource.runner.onLaunch { toggle(true) }
        }

        @Composable
        internal fun outputGroup(runner: Runner): RunOutputGroup {
            return outputGroup.getOrPut(runner) {
                RunOutputGroup(
                    runner = runner,
                    textEditorState = TextEditor.createState(END_OF_OUTPUT_SPACE),
                    colors = Theme.colors,
                    coroutineScope = coroutineScope
                )
            }
        }

        internal fun toggle() {
            toggle(!isOpen)
        }

        private fun toggle(isOpen: Boolean) {
            this.isOpen = isOpen
            mayUpdatePaneState()
        }

        private fun mayUpdatePaneState() {
            if (!isOpen) {
                unfreezeSize = paneState.size
                paneState.freeze(PANEL_BAR_HEIGHT)
            } else if (paneState.isFrozen) {
                paneState.unfreeze(unfreezeSize ?: (paneState.frame.maxSize * 0.5f))
            }
            paneState.hasBeenResized()
        }
    }

    @Composable
    fun Layout(state: State) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT)) {
                OutputGroupTabs(state, Modifier.weight(1f))
                ToggleButton(state)
            }
            if (state.isOpen) {
                Separator.Horizontal()
                OutputGroup(state, Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun ToggleButton(state: State) {
        Form.IconButton(
            icon = if (state.isOpen) Icon.Code.CHEVRON_DOWN else Icon.Code.CHEVRON_UP,
            onClick = { state.toggle() },
            modifier = Modifier.size(PANEL_BAR_HEIGHT),
            bgColor = Color.Transparent,
            roundedCorners = Theme.RoundedCorners.NONE,
        )
    }

    @Composable
    private fun OutputGroupTabs(state: State, modifier: Modifier) {
        val runnerMgr = state.resource.runner
        fun runnerName(runner: Runner): String {
            return "${state.resource.name} (${runnerMgr.numberOf(runner)})"
        }
        Row(modifier.height(PANEL_BAR_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            Form.Text(value = Label.RUN + ":")
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            Box(Modifier.weight(1f)) {
                Tabs.Layout(
                    state = state.runnerTabs,
                    tabs = runnerMgr.runners,
                    labelFn = { AnnotatedString(runnerName(it)) },
                    isActiveFn = { runnerMgr.isActive(it) },
                    onClick = { runnerMgr.activate(it) },
                    closeButtonFn = { IconButtonArg(icon = Icon.Code.XMARK) { runnerMgr.delete(it) } },
                    trailingTabButtonFn = {
                        IconButtonArg(
                            icon = Icon.Code.THUMBTACK,
                            color = { Theme.colors.icon.copy(if (it.isSaved) 1f else 0.3f) },
                            hoverColor = { Theme.colors.icon },
                            disabledColor = { Theme.colors.icon },
                            enabled = !it.isSaved
                        ) { if (!it.isSaved) it.save() }
                    }
                )
            }
        }
    }

    @Composable
    private fun OutputGroup(state: State, modifier: Modifier) {
        state.resource.runner.activeRunner?.let { runner ->
            val outputGroup = state.outputGroup(runner)
            Column(modifier) {
                Output(outputGroup.active, Modifier.fillMaxWidth().weight(1f))
                Separator.Horizontal()
                OutputTabs(outputGroup, Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT))
            }
        }
    }

    @Composable
    private fun Output(outputState: RunOutput.State, modifier: Modifier) {
        Box(modifier) {
            when (outputState) {
                is LogOutput.State -> LogOutput.Layout(outputState)
                is GraphOutput.State -> GraphOutput.Layout(outputState)
                is TableOutput.State -> TableOutput.Layout(outputState)
            }
        }
    }

    @Composable
    private fun OutputTabs(outputGroup: RunOutputGroup, modifier: Modifier) {
        fun outputIcon(output: RunOutput.State): Icon.Code {
            return when (output) {
                is LogOutput.State -> Icon.Code.ALIGN_LEFT
                is GraphOutput.State -> Icon.Code.DIAGRAM_PROJECT
                is TableOutput.State -> Icon.Code.TABLE_CELLS_LARGE
            }
        }

        fun outputName(output: RunOutput.State): String {
            return when (output) {
                is LogOutput.State -> Label.LOG
                is GraphOutput.State -> {
                    Label.GRAPH + if (outputGroup.hasMultipleGraphs) " (${outputGroup.numberOf(output)})" else ""
                }
                is TableOutput.State -> {
                    Label.TABLE + if (outputGroup.hasMultipleTables) " (${outputGroup.numberOf(output)})" else ""
                }
            }
        }

        Row(modifier.height(PANEL_BAR_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            Form.Text(value = Label.OUTPUT + ":")
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            Box(Modifier.weight(1f)) {
                Tabs.Layout(
                    state = outputGroup.tabsState,
                    tabs = outputGroup.outputs,
                    position = Tabs.Position.BOTTOM,
                    iconFn = { Form.IconArg(outputIcon(it)) },
                    labelFn = { AnnotatedString(outputName(it)) },
                    isActiveFn = { outputGroup.isActive(it) },
                    onClick = { outputGroup.activate(it) },
                )
            }
        }
    }
}
