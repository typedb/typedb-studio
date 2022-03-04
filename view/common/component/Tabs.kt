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

package com.vaticle.typedb.studio.view.common.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.view.common.component.Form.ButtonArgs
import com.vaticle.typedb.studio.view.common.component.Form.IconArgs
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.PANEL_BAR_HEIGHT
import com.vaticle.typedb.studio.view.common.theme.Theme.PANEL_BAR_SPACING
import java.awt.event.MouseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object Tabs {

    private val TAB_UNDERLINE_HEIGHT = 2.dp
    private val TAB_SCROLL_DELTA = 200.dp
    private val ICON_SIZE = 10.sp

    class State<T : Any> constructor(private val coroutineScope: CoroutineScope) {

        var density: Float by mutableStateOf(1f)
        val scroller = ScrollState(0)
        var tabsScrollTo: Dp? by mutableStateOf(null)
        var maxWidth by mutableStateOf(4096.dp)
        val openedTabSize: MutableMap<T, Dp> = mutableMapOf()
        var activeTab: T? by mutableStateOf(null)

        internal fun initTab(tab: T, isActive: Boolean, rawWidth: Int) {
            if (tab == activeTab) return
            val newTabSize = Theme.toDP(rawWidth, density) + Separator.WEIGHT
            if (newTabSize != openedTabSize[tab]) openedTabSize[tab] = newTabSize
            if (isActive) {
                var start = 0.dp
                var found = false
                openedTabSize.entries.forEach { if (it.key != tab && !found) start += it.value else found = true }
                val end = start + openedTabSize[tab]!!
                val scrollerPos = Theme.toDP(scroller.value, density)
                if (start + 5.dp < scrollerPos) tabsScrollTo = start
                else if (end - 5.dp > scrollerPos + maxWidth) tabsScrollTo = end - maxWidth
                activeTab = tab
            }
        }

        internal fun scrollTabsBy(dp: Dp) {
            val pos = scroller.value + (dp.value * density).toInt()
            coroutineScope.launch { scroller.animateScrollTo(pos) }
        }
    }

    @Composable
    fun <T : Any> Layout(
        state: State<T>, tabs: List<T>, iconFn: ((T) -> IconArgs?)? = null, labelFn: @Composable (T) -> AnnotatedString,
        isActiveFn: (T) -> Boolean, onClick: (T) -> Unit, contextMenuFn: ((T) -> List<List<ContextMenu.Item>>)? = null,
        closeButtonFn: ((T) -> ButtonArgs), extraTabButtonsFn: ((T) -> List<ButtonArgs>)? = null,
        vararg extraBarButtons: ButtonArgs
    ) {
        val closedTabs = state.openedTabSize.keys - tabs.toSet()
        closedTabs.forEach { state.openedTabSize.remove(it) }
        Row(Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT).onSizeChanged {
            state.maxWidth = Theme.toDP(it.width, state.density) - PANEL_BAR_HEIGHT * 3
        }) {
            if (state.scroller.maxValue > 0) {
                PreviousTabsButton(state)
                Separator.Vertical()
            }
            Row(Modifier.widthIn(max = state.maxWidth).height(PANEL_BAR_HEIGHT).horizontalScroll(state.scroller)) {
                tabs.forEach { tab ->
                    val icon = iconFn?.let { it(tab) }
                    val label = labelFn(tab)
                    val isActive = isActiveFn(tab)
                    val closeButtonArgs = closeButtonFn(tab)
                    val extraTabButtons = extraTabButtonsFn?.let { it(tab) } ?: listOf()
                    Tab(state, tab, icon, label, isActive, closeButtonArgs, onClick, contextMenuFn, extraTabButtons)
                }
            }
            if (state.scroller.maxValue > 0) {
                if (state.scroller.value < state.scroller.maxValue) Separator.Vertical()
                NextTabsButton(state)
                Separator.Vertical()
            }
            if (extraBarButtons.isNotEmpty()) ExtraButtons(extraBarButtons.toList())
            if (tabs.isNotEmpty()) Separator.Vertical()
        }
        LaunchedEffect(state.tabsScrollTo) {
            state.tabsScrollTo?.let {
                state.scroller.scrollTo((it.value * state.density).toInt())
                state.tabsScrollTo = null
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun <T : Any> Tab(
        state: State<T>, tab: T, icon: IconArgs?, label: AnnotatedString, isActive: Boolean,
        closeButtonArgs: ButtonArgs, onClick: (T) -> Unit, contextMenuFn: ((T) -> List<List<ContextMenu.Item>>)?,
        extraTabButtons: List<ButtonArgs>
    ) {
        val contextMenuState = remember { ContextMenu.State() }
        val bgColor = if (isActive) Theme.colors.primary else Theme.colors.background
        val height = if (isActive) PANEL_BAR_HEIGHT - TAB_UNDERLINE_HEIGHT else PANEL_BAR_HEIGHT
        var width by remember { mutableStateOf(0.dp) }

        Box {
            contextMenuFn?.let { ContextMenu.Popup(contextMenuState) { it(tab) } }
            Column(Modifier.onSizeChanged { state.initTab(tab, isActive, it.width) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(height)
                        .background(color = bgColor)
                        .pointerHoverIcon(PointerIconDefaults.Hand)
                        .pointerInput(state, tab) { onPointerInput(contextMenuState) { onClick(tab) } }
                        .onSizeChanged { width = Theme.toDP(it.width, state.density) }
                ) {
                    icon?.let {
                        Spacer(modifier = Modifier.width(PANEL_BAR_SPACING))
                        Icon.Render(icon = it.code, color = it.color(), size = ICON_SIZE)
                    }
                    Spacer(modifier = Modifier.width(PANEL_BAR_SPACING))
                    Form.Text(value = label)
                    if (extraTabButtons.isNotEmpty()) Spacer(Modifier.width(PANEL_BAR_SPACING))
                    ExtraButtons(extraTabButtons.toList() + listOf(closeButtonArgs))
                }
                if (isActive) Separator.Horizontal(TAB_UNDERLINE_HEIGHT, Theme.colors.secondary, Modifier.width(width))
            }
            Separator.Vertical()
        }
    }

    private suspend fun PointerInputScope.onPointerInput(
        contextMenu: ContextMenu.State,
        onClick: (MouseEvent) -> Unit
    ) {
        contextMenu.onPointerInput(
            pointerInputScope = this,
            onSinglePrimaryPressed = onClick
        )
    }

    @Composable
    private fun <T : Any> PreviousTabsButton(state: State<T>) {
        Form.IconButton(
            icon = Icon.Code.CARET_LEFT,
            onClick = { state.scrollTabsBy(-TAB_SCROLL_DELTA) },
            modifier = Modifier.size(PANEL_BAR_HEIGHT),
            bgColor = Color.Transparent,
            rounded = false,
            enabled = state.scroller.value > 0
        )
    }

    @Composable
    private fun <T : Any> NextTabsButton(state: State<T>) {
        Form.IconButton(
            icon = Icon.Code.CARET_RIGHT,
            onClick = { state.scrollTabsBy(TAB_SCROLL_DELTA) },
            modifier = Modifier.size(PANEL_BAR_HEIGHT),
            bgColor = Color.Transparent,
            rounded = false,
            enabled = state.scroller.value < state.scroller.maxValue
        )
    }

    @Composable
    private fun ExtraButtons(buttons: List<ButtonArgs>) {
        buttons.forEach {
            Form.IconButton(
                icon = it.icon,
                hoverIcon = it.hoverIcon,
                iconColor = it.color(),
                iconHoverColor = it.hoverColor(),
                onClick = { it.onClick() },
                modifier = Modifier.size(PANEL_BAR_HEIGHT),
                bgColor = Color.Transparent,
                rounded = false,
                enabled = it.enabled
            )
        }
    }
}