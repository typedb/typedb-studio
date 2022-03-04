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
import java.awt.event.MouseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object Tabs {

    private val TAB_UNDERLINE_HEIGHT = 2.dp
    private val TAB_SCROLL_DELTA = 200.dp
    private val ICON_SIZE = 10.sp

    class State<T : Any>(private val coroutineScope: CoroutineScope) {

        var density: Float by mutableStateOf(0f)
        val tabsScroller = ScrollState(0)
        var tabsScrollTo: Dp? by mutableStateOf(null)
        var tabsRowMaxWidth by mutableStateOf(4096.dp)
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
                val scrollerPos = Theme.toDP(tabsScroller.value, density)
                if (start + 5.dp < scrollerPos) tabsScrollTo = start
                else if (end - 5.dp > scrollerPos + tabsRowMaxWidth) tabsScrollTo = end - tabsRowMaxWidth
                activeTab = tab
            }
        }

        internal fun scrollTabsBy(dp: Dp) {
            val pos = tabsScroller.value + (dp.value * density).toInt()
            coroutineScope.launch { tabsScroller.animateScrollTo(pos) }
        }
    }

    @Composable
    fun <T : Any> Layout(
        state: State<T>,
        tabs: List<T>,
        tabIconFn: (T) -> IconArgs?,
        tabLabelFn: @Composable (T) -> AnnotatedString,
        tabIsActiveFn: (T) -> Boolean,
        tabOnClick: (T) -> Unit,
        tabOnClose: (T) -> Unit,
        tabContextMenuFn: ((T) -> List<List<ContextMenu.Item>>)? = null,
        vararg extraButtons: ButtonArgs
    ) {
        val scrollState = state.tabsScroller
        val closedTabs = state.openedTabSize.keys - tabs.toSet()
        closedTabs.forEach { state.openedTabSize.remove(it) }
        fun updateTabsRowMaxWidth(rawAreaWidth: Int) {
            state.tabsRowMaxWidth = Theme.toDP(rawAreaWidth, state.density) - PANEL_BAR_HEIGHT * 3
        }
        Row(Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT).onSizeChanged { updateTabsRowMaxWidth(it.width) }) {
            if (scrollState.maxValue > 0) {
                PreviousTabsButton(state)
                Separator.Vertical()
            }
            Row(Modifier.widthIn(max = state.tabsRowMaxWidth).height(PANEL_BAR_HEIGHT).horizontalScroll(scrollState)) {
                tabs.forEach { tab ->
                    Tab(
                        state, tab, tabIconFn(tab), tabLabelFn(tab), tabIsActiveFn(tab),
                        tabOnClick, tabOnClose, tabContextMenuFn
                    )
                }
            }
            if (scrollState.maxValue > 0) {
                if (scrollState.value < scrollState.maxValue) Separator.Vertical()
                NextTabsButton(state)
                Separator.Vertical()
            }
            if (extraButtons.isNotEmpty()) ExtraButtons(*extraButtons)
            Separator.Vertical()
        }
        LaunchedEffect(state.tabsScrollTo) {
            state.tabsScrollTo?.let {
                scrollState.scrollTo((it.value * state.density).toInt())
                state.tabsScrollTo = null
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun <T : Any> Tab(
        state: State<T>,
        tab: T,
        icon: IconArgs?,
        label: AnnotatedString,
        isActive: Boolean,
        onClick: (T) -> Unit,
        onClose: (T) -> Unit,
        contextMenuFn: ((T) -> List<List<ContextMenu.Item>>)?
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
                        Spacer(modifier = Modifier.width(Theme.PANEL_BAR_SPACING))
                        Icon.Render(icon = it.code, color = it.color(), size = ICON_SIZE)
                    }
                    Spacer(modifier = Modifier.width(Theme.PANEL_BAR_SPACING))
                    Form.Text(value = label)
                    Form.IconButton(
                        icon = Icon.Code.XMARK,
                        onClick = { onClose(tab) },
                        modifier = Modifier.size(PANEL_BAR_HEIGHT),
                        bgColor = Color.Transparent,
                        rounded = false,
                    )
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
            enabled = state.tabsScroller.value > 0
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
            enabled = state.tabsScroller.value < state.tabsScroller.maxValue
        )
    }

    @Composable
    private fun ExtraButtons(vararg buttons: ButtonArgs) {
        buttons.forEach {
            Form.IconButton(
                icon = it.icon,
                onClick = { it.onClick() },
                modifier = Modifier.size(PANEL_BAR_HEIGHT),
                bgColor = Color.Transparent,
                rounded = false,
                enabled = it.enabled
            )
        }
    }
}