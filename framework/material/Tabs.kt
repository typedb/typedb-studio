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

package com.vaticle.typedb.studio.framework.material

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.framework.common.Util.toDP
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Form.IconArg
import com.vaticle.typedb.studio.framework.material.Form.IconButtonArg
import java.awt.event.MouseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object Tabs {

    private val TAB_UNDERLINE_HEIGHT = 2.dp
    private val TAB_SCROLL_DELTA = 200.dp
    private val TAB_ICON_SIZE = 10.sp
    private val TAB_SPACING = Theme.PANEL_BAR_SPACING

    object Vertical {

        val WIDTH = 22.dp
        private val TAB_HEIGHT = 100.dp

        enum class Position(internal val degree: Float, internal val offset: Dp) {
            LEFT(-90f, (-40).dp),
            RIGHT(90f, 40.dp)
        }

        @Composable
        fun <T : Any> Layout(
            tabs: List<T>,
            position: Position,
            labelFn: (T) -> String,
            iconFn: (T) -> Icon.Code,
            isActiveFn: (T) -> Boolean,
            onClick: (T) -> Unit,
        ) {
            Column(
                modifier = Modifier.width(WIDTH).fillMaxHeight().background(Theme.studio.backgroundMedium),
                verticalArrangement = Arrangement.Top
            ) { tabs.forEach { Tab(it, position, labelFn, iconFn, isActiveFn, onClick) } }
        }

        @OptIn(ExperimentalComposeUiApi::class)
        @Composable
        private fun <T : Any> Tab(
            tab: T,
            position: Position,
            labelFn: (T) -> String,
            iconFn: (T) -> Icon.Code,
            isActiveFn: (T) -> Boolean,
            onClick: (T) -> Unit,
        ) {
            @Composable
            fun bgColor(): Color = if (isActiveFn(tab)) Theme.studio.surface else Theme.studio.backgroundDark
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TAB_HEIGHT)
                    .pointerHoverIcon(PointerIconDefaults.Hand)
                    .clickable { onClick(tab) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.requiredWidth(TAB_HEIGHT)
                        .rotate(position.degree)
                        .offset(x = position.offset)
                        .background(color = bgColor())
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon.Render(icon = iconFn(tab), size = TAB_ICON_SIZE)
                    Spacer(modifier = Modifier.width(TAB_SPACING))
                    Form.Text(value = labelFn(tab))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Separator.Horizontal()
        }
    }

    object Horizontal {

        private val HEIGHT = Theme.PANEL_BAR_HEIGHT

        enum class Position { TOP, BOTTOM }

        class State<T : Any> {

            var density: Float by mutableStateOf(1f)
            val scroller = ScrollState(0)
            var tabsScrollTo: Dp? by mutableStateOf(null)
            var maxWidth by mutableStateOf(4096.dp)
            val openedTabSize: MutableMap<T, Dp> = mutableMapOf()
            var activeTab: T? by mutableStateOf(null)
            internal var coroutineScope: CoroutineScope? = null

            internal fun initTab(tab: T, isActive: Boolean, rawWidth: Int) {
                if (tab == activeTab) return
                val newTabSize = toDP(rawWidth, density) + Separator.WEIGHT
                if (newTabSize != openedTabSize[tab]) openedTabSize[tab] = newTabSize
                if (isActive) {
                    var start = 0.dp
                    var found = false
                    openedTabSize.entries.forEach { if (it.key != tab && !found) start += it.value else found = true }
                    val end = start + openedTabSize[tab]!!
                    val scrollerPos = toDP(scroller.value, density)
                    if (start + 5.dp < scrollerPos) tabsScrollTo = start
                    else if (end - 5.dp > scrollerPos + maxWidth) tabsScrollTo = end - maxWidth
                    activeTab = tab
                }
            }

            internal fun scrollTabsBy(dp: Dp) {
                val pos = scroller.value + (dp.value * density).toInt()
                coroutineScope?.launch { scroller.animateScrollTo(pos) }
            }
        }

        @Composable
        fun <T : Any> Layout(
            state: State<T>,
            tabs: List<T>,
            position: Position = Position.TOP,
            iconFn: (@Composable (T) -> IconArg?)? = null,
            labelFn: @Composable (T) -> AnnotatedString,
            isActiveFn: (T) -> Boolean,
            onClick: (T) -> Unit,
            contextMenuFn: ((T) -> List<List<ContextMenu.Item>>)? = null,
            closeButtonFn: ((T) -> IconButtonArg)? = null,
            leadingButtonFn: ((T) -> IconButtonArg?)? = null,
            buttons: List<IconButtonArg> = listOf()
        ) {
            state.density = LocalDensity.current.density
            state.coroutineScope = rememberCoroutineScope()
            val closedTabs = state.openedTabSize.keys - tabs.toSet()
            closedTabs.forEach { state.openedTabSize.remove(it) }
            Row(Modifier.fillMaxWidth().height(HEIGHT).onSizeChanged {
                state.maxWidth = toDP(it.width, state.density) - HEIGHT * 3
            }) {
                if (tabs.isNotEmpty()) Separator.Vertical()
                if (state.scroller.maxValue > 0) {
                    PreviousTabsButton(state)
                    Separator.Vertical()
                }
                Row(Modifier.widthIn(max = state.maxWidth).height(HEIGHT).horizontalScroll(state.scroller)) {
                    tabs.forEach { tab ->
                        val icon = iconFn?.let { it(tab) }
                        val label = labelFn(tab)
                        val isActive = isActiveFn(tab)
                        val closeBtn = closeButtonFn?.let { it(tab) }
                        val leadingBtn = leadingButtonFn?.let { it(tab) }
                        Tab(state, tab, position, icon, label, isActive, closeBtn, onClick, contextMenuFn, leadingBtn)
                        Separator.Vertical()
                    }
                }
                if (state.scroller.maxValue > 0) {
                    if (state.scroller.value < state.scroller.maxValue) Separator.Vertical()
                    NextTabsButton(state)
                    Separator.Vertical()
                }
                buttons.forEach {
                    Button(it)
                    Separator.Vertical()
                }
            }
            LaunchedEffect(state.tabsScrollTo) {
                state.tabsScrollTo?.let {
                    state.scroller.scrollTo((it.value * state.density).toInt())
                    state.tabsScrollTo = null
                }
            }
        }

        @Composable
        private fun Spacer() {
            Spacer(modifier = Modifier.width(TAB_SPACING))
        }

        @OptIn(ExperimentalComposeUiApi::class)
        @Composable
        private fun <T : Any> Tab(
            state: State<T>, tab: T, position: Position, icon: IconArg?, label: AnnotatedString,
            isActive: Boolean, closeButtonArg: IconButtonArg?, onClick: (T) -> Unit,
            contextMenuFn: ((T) -> List<List<ContextMenu.Item>>)?,
            leadingIconButton: IconButtonArg?
        ) {
            val contextMenuState = remember { ContextMenu.State() }
            val bgColor = if (isActive) Theme.studio.primary else Color.Transparent
            val height = if (isActive) HEIGHT - TAB_UNDERLINE_HEIGHT else HEIGHT
            var width by remember { mutableStateOf(0.dp) }

            Box {
                contextMenuFn?.let { ContextMenu.Popup(contextMenuState) { it(tab) } }
                Column(Modifier.onSizeChanged { state.initTab(tab, isActive, it.width) }) {
                    if (isActive && position == Position.BOTTOM) ActiveIndicator(width)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(height)
                            .background(color = bgColor)
                            .pointerHoverIcon(PointerIconDefaults.Hand)
                            .pointerInput(state, tab) { onPointerInput(contextMenuState) { onClick(tab) } }
                            .onSizeChanged { width = toDP(it.width, state.density) }
                    ) {
                        leadingIconButton?.let { Button(it) }
                        icon?.let {
                            Spacer()
                            Icon.Render(icon = it.code, color = it.color(), size = TAB_ICON_SIZE)
                            Spacer()
                        }
                        if (leadingIconButton == null && icon == null) Spacer()
                        Form.Text(value = label)
                        Spacer()
                        closeButtonArg?.let { Button(it) }
                    }
                    if (isActive && position == Position.TOP) ActiveIndicator(width)
                }
            }
        }

        @Composable
        private fun ActiveIndicator(width: Dp) {
            Separator.Horizontal(TAB_UNDERLINE_HEIGHT, Theme.studio.secondary, Modifier.width(width))
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
                modifier = Modifier.size(HEIGHT),
                bgColor = Color.Transparent,
                roundedCorners = Theme.RoundedCorners.NONE,
                enabled = state.scroller.value > 0
            ) { state.scrollTabsBy(-TAB_SCROLL_DELTA) }
        }

        @Composable
        private fun <T : Any> NextTabsButton(state: State<T>) {
            Form.IconButton(
                icon = Icon.Code.CARET_RIGHT,
                modifier = Modifier.size(HEIGHT),
                bgColor = Color.Transparent,
                roundedCorners = Theme.RoundedCorners.NONE,
                enabled = state.scroller.value < state.scroller.maxValue
            ) { state.scrollTabsBy(TAB_SCROLL_DELTA) }
        }

        @Composable
        private fun Button(buttonArg: IconButtonArg) {
            Form.IconButton(
                icon = buttonArg.icon,
                hoverIcon = buttonArg.hoverIcon,
                modifier = Modifier.size(HEIGHT),
                iconColor = buttonArg.color(),
                iconHoverColor = buttonArg.hoverColor?.invoke(),
                disabledColor = buttonArg.disabledColor?.invoke(),
                bgColor = Color.Transparent,
                roundedCorners = Theme.RoundedCorners.NONE,
                enabled = buttonArg.enabled,
                onClick = buttonArg.onClick,
            )
        }
    }
}