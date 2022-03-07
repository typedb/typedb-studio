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
import com.vaticle.typedb.studio.state.runner.OutputManager
import com.vaticle.typedb.studio.state.runner.RunnerOutput
import com.vaticle.typedb.studio.state.runner.TransactionRunner
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.ButtonArg
import com.vaticle.typedb.studio.view.common.component.Frame
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.component.Tabs
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.PANEL_BAR_HEIGHT
import com.vaticle.typedb.studio.view.common.theme.Theme.PANEL_BAR_SPACING
import kotlinx.coroutines.CoroutineScope

object RunOutputArea {

    const val DEFAULT_OPEN = false

    class State constructor(
        internal val resource: Resource,
        private val paneState: Frame.PaneState,
        private val coroutineScope: CoroutineScope
    ) {

        internal var isOpen: Boolean by mutableStateOf(DEFAULT_OPEN)
        internal val runnerTabsState = Tabs.State<TransactionRunner>(coroutineScope)
        private val outputTabState: MutableMap<TransactionRunner, Tabs.State<RunnerOutput>> = mutableMapOf()
        private var unfreezeSize: Dp? by mutableStateOf(null)

        init {
            resource.runner.onRegister { toggle(true) }
        }

        internal fun outputTabState(runner: TransactionRunner): Tabs.State<RunnerOutput> {
            return outputTabState.getOrPut(runner) { Tabs.State(coroutineScope) }
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
                paneState.unfreeze(unfreezeSize ?: (paneState.frameState.maxSize * 0.5f))
            }
        }
    }

    @Composable
    fun Layout(state: State) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT)) {
                RunOutputGroupTabs(state, Modifier.weight(1f))
                ToggleButton(state)
            }
            if (state.isOpen) {
                Separator.Horizontal()
                state.resource.runner.activeRunner?.let {
                    RunOutputGroup(it.output, state.outputTabState(it), Modifier.fillMaxSize())
                }
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
            rounded = false,
        )
    }

    @Composable
    private fun RunOutputGroupTabs(state: State, modifier: Modifier) {
        val runnerMgr = state.resource.runner
        fun runnerName(runner: TransactionRunner): String {
            return "${state.resource.name} (${runnerMgr.numberOf(runner)})"
        }
        Row(modifier.height(PANEL_BAR_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            Form.Text(value = Label.RUN + ":")
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            Box(Modifier.weight(1f)) {
                Tabs.Layout(
                    state = state.runnerTabsState,
                    tabs = runnerMgr.runners,
                    labelFn = { AnnotatedString(runnerName(it)) },
                    isActiveFn = { runnerMgr.isActive(it) },
                    onClick = { runnerMgr.activate(it) },
                    closeButtonFn = { ButtonArg(icon = Icon.Code.XMARK) { runnerMgr.delete(it) } },
                    trailingTabButtonFn = {
                        ButtonArg(
                            icon = Icon.Code.THUMBTACK,
                            color = { Theme.colors.icon.copy(if (runnerMgr.isSaved(it)) 1f else 0.3f) },
                            hoverColor = { Theme.colors.icon },
                            disabledColor = { Theme.colors.icon },
                            enabled = !runnerMgr.isSaved(it)
                        ) { if (!runnerMgr.isSaved(it)) runnerMgr.save(it) }
                    }
                )
            }
        }
    }

    @Composable
    private fun RunOutputGroup(outputMgr: OutputManager, tabsState: Tabs.State<RunnerOutput>, modifier: Modifier) {
        Column(modifier) {
            RunOutput(outputMgr.active, Modifier.fillMaxWidth().weight(1f))
            Separator.Horizontal()
            RunOutputTabs(outputMgr, tabsState, Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT))
        }
    }

    @Composable
    private fun RunOutput(output: RunnerOutput, modifier: Modifier) {
        Box(modifier) {
            when (output) {
                is RunnerOutput.Log -> LogOutput.Layout()
                is RunnerOutput.Graph -> GraphOutput.Layout()
                is RunnerOutput.Table -> TableOutput.Layout()
            }
        }
    }

    @Composable
    private fun RunOutputTabs(outputMgr: OutputManager, tabsState: Tabs.State<RunnerOutput>, modifier: Modifier) {
        fun outputIcon(output: RunnerOutput): Icon.Code {
            return when (output) {
                is RunnerOutput.Log -> Icon.Code.ALIGN_LEFT
                is RunnerOutput.Graph -> Icon.Code.DIAGRAM_PROJECT
                is RunnerOutput.Table -> Icon.Code.TABLE
            }
        }

        fun outputName(output: RunnerOutput): String {
            return when (output) {
                is RunnerOutput.Log -> Label.LOG
                is RunnerOutput.Graph -> Label.GRAPH + if (outputMgr.hasMultipleGraphs) " (${outputMgr.numberOf(output)})" else ""
                is RunnerOutput.Table -> Label.TABLE + if (outputMgr.hasMultipleTables) " (${outputMgr.numberOf(output)})" else ""
            }
        }

        Row(modifier.height(PANEL_BAR_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            Form.Text(value = Label.OUTPUT + ":")
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            Box(Modifier.weight(1f)) {
                Tabs.Layout(
                    state = tabsState,
                    tabs = outputMgr.outputs,
                    position = Tabs.Position.BOTTOM,
                    iconFn = { Form.IconArg(outputIcon(it)) },
                    labelFn = { AnnotatedString(outputName(it)) },
                    isActiveFn = { outputMgr.isActive(it) },
                    onClick = { outputMgr.activate(it) },
                )
            }
        }
    }
}
