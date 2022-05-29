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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.view.common.component.Browser
import com.vaticle.typedb.studio.view.types.TypeBrowser
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Frame
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.project.ProjectBrowser

object BrowserArea {

    val WIDTH = 300.dp
    val MIN_WIDTH = 120.dp
    private val SIDE_TAB_WIDTH = 22.dp
    private val SIDE_TAB_HEIGHT = 100.dp
    private val SIDE_TAB_SPACING = 8.dp
    private val ICON_SIZE = 10.sp
    private val TAB_OFFSET = (-40).dp

    class State constructor(private val paneState: Frame.PaneState) {

        private var unfreezeSize: Dp by mutableStateOf(MIN_WIDTH)
        internal val openedBrowsers: List<Browser> get() = browsers.filter { it.isOpen }
        internal val browsers = listOf(
            ProjectBrowser(1, true) { mayUpdatePaneState() },
            TypeBrowser(true, 2) { mayUpdatePaneState() },
//            RuleBrowser(false, 3) { mayUpdatePaneState() },
//            UserBrowser(false, 4) { mayUpdatePaneState() },
//            RoleBrowser(false, 5) { mayUpdatePaneState() },
        )

        fun mayUpdatePaneState() {
            if (openedBrowsers.isEmpty()) {
                unfreezeSize = paneState.size
                paneState.freeze(SIDE_TAB_WIDTH)
            } else if (paneState.isFrozen) paneState.unfreeze(unfreezeSize)
        }
    }

    @Composable
    fun Layout(paneState: Frame.PaneState) {
        val state = remember { State(paneState) }
        val openedBrowsers = state.openedBrowsers
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(SIDE_TAB_WIDTH), verticalArrangement = Arrangement.Top) {
                state.browsers.forEach { Tab(it) }
            }
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

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Tab(browser: Browser) {
        @Composable
        fun bgColor(): Color = if (browser.isOpen) Theme.studio.surface else Theme.studio.background0
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SIDE_TAB_HEIGHT)
                .pointerHoverIcon(PointerIconDefaults.Hand)
                .clickable { browser.toggle() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.requiredWidth(SIDE_TAB_HEIGHT)
                    .rotate(-90f)
                    .offset(x = TAB_OFFSET)
                    .background(color = bgColor())
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Icon.Render(icon = browser.icon, size = ICON_SIZE)
                Spacer(modifier = Modifier.width(SIDE_TAB_SPACING))
                Text(value = browser.label)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Separator.Horizontal()
    }
}
