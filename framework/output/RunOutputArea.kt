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

package com.vaticle.typedb.studio.framework.output

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.common.theme.Theme.PANEL_BAR_HEIGHT
import com.vaticle.typedb.studio.framework.common.theme.Theme.PANEL_BAR_SPACING
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.IconButtonArg
import com.vaticle.typedb.studio.framework.material.Frame
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Separator
import com.vaticle.typedb.studio.framework.material.Tabs
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Sentence
import com.vaticle.typedb.studio.service.connection.QueryRunner
import com.vaticle.typedb.studio.service.page.Pageable
import kotlinx.coroutines.delay

object RunOutputArea {

    const val DEFAULT_OPEN = false

    class State constructor(var pageable: Pageable.Runnable, private val paneState: Frame.PaneState) {

        internal var isOpen: Boolean by mutableStateOf(DEFAULT_OPEN)
        internal val runnerTabs = Tabs.Horizontal.State<QueryRunner>()
        private val outputGroup: MutableMap<QueryRunner, RunOutputGroup> = mutableMapOf()
        private var unfreezeSize: Dp? by mutableStateOf(null)

        init {
            pageable.runners.onLaunch { toggle(true) }
        }

        @Composable
        internal fun outputGroup(runner: QueryRunner) = outputGroup.getOrPut(runner) {
            RunOutputGroup.createAndLaunch(runner)
        }

        internal fun toggle() = toggle(!isOpen)

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
    fun Layout(state: State) = Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT)) {
            OutputGroupTabs(state, Modifier.weight(1f))
            ToggleButton(state)
        }
        if (state.isOpen) {
            Separator.Horizontal()
            state.pageable.runners.active?.let { runner ->
                OutputGroup(state, runner, Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun ToggleButton(state: State) = Form.IconButton(
        icon = if (state.isOpen) Icon.HIDE else Icon.SHOW,
        modifier = Modifier.size(PANEL_BAR_HEIGHT),
        bgColor = Color.Transparent,
        roundedCorners = Theme.RoundedCorners.NONE,
    ) { state.toggle() }

    @Composable
    private fun OutputGroupTabs(state: State, modifier: Modifier) {
        val runnerSrv = state.pageable.runners
        fun runnerName(runner: QueryRunner): String = "${state.pageable.name} (${runnerSrv.numberOf(runner)})"
        fun mayCloseRunner(runner: QueryRunner) {
            if (runner.isRunning.get()) Service.confirmation.submit(
                title = Label.QUERY_IS_RUNNING,
                message = Sentence.STOP_RUNNING_QUERY_BEFORE_CLOSING_OUTPUT_GROUP_TAB_DESCRIPTION,
                cancelLabel = Label.OK,
            ) else runnerSrv.close(runner)
        }
        Row(modifier.height(PANEL_BAR_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            Form.Text(value = Label.RUN + ":")
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            Box(Modifier.weight(1f)) {
                Tabs.Horizontal.Layout(
                    state = state.runnerTabs,
                    tabs = runnerSrv.launched,
                    labelFn = { AnnotatedString(runnerName(it)) },
                    isActiveFn = { runnerSrv.isActive(it) },
                    onClick = { runnerSrv.activate(it) },
                    closeButtonFn = { IconButtonArg(icon = Icon.CLOSE) { mayCloseRunner(it) } },
                    leadingButtonFn = {
                        IconButtonArg(
                            icon = Icon.PIN,
                            color = { Theme.studio.icon.copy(if (runnerSrv.isSaved(it)) 1f else 0.3f) },
                            hoverColor = { Theme.studio.icon },
                            disabledColor = { Theme.studio.icon },
                            enabled = !runnerSrv.isSaved(it)
                        ) { if (!runnerSrv.isSaved(it)) runnerSrv.save(it) }
                    }
                )
            }
        }
    }

    @Composable
    private fun OutputGroup(state: State, runner: QueryRunner, modifier: Modifier) {
        val outputGroup = state.outputGroup(runner)
        Column(modifier) {
            Box(Modifier.fillMaxWidth().weight(1f)) { outputGroup.active.Layout() }
            Separator.Horizontal()
            OutputTabs(outputGroup, Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT))
        }
        LaunchedEffect(outputGroup) {
            do {
                delay(50)
                outputGroup.publishStatus()
            } while (!runner.isConsumed)
        }
    }

    @Composable
    private fun OutputTabs(
        outputGroup: RunOutputGroup, modifier: Modifier
    ) = Row(modifier.height(PANEL_BAR_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(PANEL_BAR_SPACING))
        Form.Text(value = Label.OUTPUT + ":")
        Spacer(Modifier.width(PANEL_BAR_SPACING))
        Box(Modifier.weight(1f)) {
            Tabs.Horizontal.Layout(
                state = outputGroup.tabsState,
                tabs = outputGroup.outputs,
                position = Tabs.Horizontal.Position.BOTTOM,
                iconFn = { Form.IconArg(it.icon) },
                labelFn = { AnnotatedString(it.name) },
                    isActiveFn = { outputGroup.isActive(it) },
                    onClick = { outputGroup.activate(it) },
                )
            }
        }
}
