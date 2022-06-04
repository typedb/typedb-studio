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

package com.vaticle.typedb.studio.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.view.material.Browser
import com.vaticle.typedb.studio.view.material.Frame
import com.vaticle.typedb.studio.view.material.Separator
import com.vaticle.typedb.studio.view.material.Tabs

object BrowserGroup {

    val WIDTH = 300.dp
    val MIN_WIDTH = 120.dp

    class State constructor(
        internal val browsers: List<Browser>,
        private val paneState: Frame.PaneState
    ) {

        private var unfreezeSize: Dp by mutableStateOf(MIN_WIDTH)
        internal val openedBrowsers: List<Browser> get() = browsers.filter { it.isOpen }

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
    fun Layout(browsers: List<Browser>, paneState: Frame.PaneState) {
        val state = remember { State(browsers, paneState) }
        val openedBrowsers = state.openedBrowsers
        Row(Modifier.fillMaxSize()) {
            Tabs.Vertical.Layout(
                tabs = state.browsers,
                position = Tabs.Vertical.Position.LEFT,
                labelFn = { it.label },
                iconFn = { it.icon },
                isActiveFn = { it.isOpen }
            ) { it.toggle() }
            if (openedBrowsers.isNotEmpty()) {
                Separator.Vertical()
                Frame.Column(
                    modifier = Modifier.fillMaxHeight().weight(1f),
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
        }
    }
}
