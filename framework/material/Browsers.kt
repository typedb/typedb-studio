/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.framework.common.theme.Theme
import java.util.concurrent.LinkedBlockingQueue

object Browsers {

    val DEFAULT_WIDTH = 300.dp
    val MIN_WIDTH = 120.dp

    enum class Position { LEFT, RIGHT }

    private class State constructor(
        val browsers: List<Browser>,
        val paneState: Frame.PaneState
    ) {

        var unfreezeSize: Dp by mutableStateOf(MIN_WIDTH)
        val openedBrowsers: List<Browser> get() = browsers.filter { it.isOpen }

        init {
            browsers.forEach { it.onUpdatePane { mayUpdatePaneState() } }
        }

        fun mayUpdatePaneState() {
            if (openedBrowsers.isEmpty()) {
                unfreezeSize = paneState.size
                paneState.freeze(Tabs.Vertical.WIDTH)
            } else if (paneState.isFrozen) paneState.unfreeze(unfreezeSize)
        }
    }

    @Composable
    fun Layout(browsers: List<Browser>, paneState: Frame.PaneState, position: Position) {
        val state = remember { State(browsers, paneState) }
        val openedBrowsers = state.openedBrowsers
        Row(Modifier.fillMaxSize()) {
            if (position == Position.LEFT) Tabs(state)
            if (openedBrowsers.isNotEmpty()) {
                if (position == Position.LEFT) Separator.Vertical()
                Browsers(Modifier.fillMaxHeight().weight(1f), openedBrowsers)
                if (position == Position.RIGHT) Separator.Vertical()
            }
            if (position == Position.RIGHT) Tabs(state)
        }
    }

    @Composable
    private fun Tabs(state: State) {
        Tabs.Vertical.Layout(
            tabs = state.browsers,
            position = Tabs.Vertical.Position.LEFT,
            labelFn = { it.label },
            iconFn = { it.icon },
            isActiveFn = { it.isOpen }
        ) { it.toggle() }
    }

    @Composable
    private fun Browsers(modifier: Modifier, openedBrowsers: List<Browser>) {
        Frame.Column(
            modifier = modifier,
            separator = Frame.SeparatorArgs(Separator.WEIGHT),
            *openedBrowsers.map { browser ->
                Frame.Pane(
                    id = browser.label,
                    order = browser.order,
                    minSize = Browser.MIN_HEIGHT,
                    initSize = Either.second(1f)
                ) { browser.Layout() }
            }.toTypedArray()
        )
    }

    abstract class Browser(isOpen: Boolean = false, val order: Int) {

        companion object {
            val MIN_HEIGHT = 80.dp
        }

        abstract val label: String
        abstract val icon: Icon
        abstract val isActive: Boolean
        abstract val buttons: List<Form.IconButtonArg>

        private val onUpdatePane = LinkedBlockingQueue<() -> Unit>()

        internal var isOpen: Boolean by mutableStateOf(isOpen)

        @Composable
        abstract fun Content()

        fun toggle() {
            isOpen = !isOpen
            onUpdatePane.forEach { it() }
        }

        fun onUpdatePane(function: () -> Unit) {
            onUpdatePane.put(function)
        }

        @Composable
        fun Layout() {
            Column {
                Bar()
                Separator.Horizontal()
                Box(modifier = Modifier.weight(1f)) { Content() }
            }
        }

        @Composable
        private fun Bar() {
            Row(
                modifier = Modifier.fillMaxWidth().height(Theme.PANEL_BAR_HEIGHT).background(Theme.studio.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(Theme.PANEL_BAR_SPACING))
                Icon.Render(icon = icon)
                Spacer(Modifier.width(Theme.PANEL_BAR_SPACING))
                Form.Text(value = label)
                Spacer(Modifier.weight(1f))
                Buttons(*buttons.toTypedArray(), isActive = isActive)
                Buttons(Form.IconButtonArg(Icon.CLOSE) { toggle() }, isActive = true)
            }
        }

        @Composable
        private fun Buttons(vararg buttons: Form.IconButtonArg, isActive: Boolean) {
            buttons.forEach {
                Form.IconButton(
                    icon = it.icon,
                    hoverIcon = it.hoverIcon,
                    modifier = Modifier.size(Theme.PANEL_BAR_HEIGHT),
                    iconColor = it.color(),
                    iconHoverColor = it.hoverColor?.invoke(),
                    disabledColor = it.disabledColor?.invoke(),
                    bgColor = Color.Transparent,
                    roundedCorners = Theme.RoundedCorners.NONE,
                    enabled = isActive && it.enabled,
                    tooltip = it.tooltip,
                    onClick = it.onClick,
                )
            }
        }
    }
}
