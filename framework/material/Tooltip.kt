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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.rememberCursorPositionProvider
import com.vaticle.typedb.studio.framework.common.Context.LocalWindowContext
import com.vaticle.typedb.studio.framework.common.Util.toDP
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Form.BORDER_WIDTH
import com.vaticle.typedb.studio.framework.material.Form.ClickableText
import com.vaticle.typedb.studio.framework.material.Form.Text
import com.vaticle.typedb.studio.framework.material.Form.URLText
import com.vaticle.typedb.studio.service.common.util.Label
import java.awt.MouseInfo
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Tooltip {

    private val TOOLTIP_DELAY = 800.milliseconds
    private val TOOLTIP_WIDTH = 250.dp
    private val TOOLTIP_OFFSET = 24.dp
    private val TOOLTIP_SPACE = 8.dp

    data class Arg(val title: String, val description: String? = null, val url: URL? = null)

    class State(internal val arg: Arg) {

        internal var isOpen by mutableStateOf(false)
        private val mouseHoverTarget = AtomicBoolean(false)
        private val mouseHoverDisabled = AtomicBoolean(false)
        private val mouseHoverTooltip = AtomicBoolean(false)
        private var tooltipExpanded = AtomicBoolean(false)
        private val coroutines = CoroutineScope(Dispatchers.Default)

        internal fun keepShowingOnTooltipHover(expanded: Boolean = false) {
            mouseHoverTooltip.set(true)
            tooltipExpanded.set(expanded)
            isOpen = true
        }

        fun mayShowOnTargetHover() {
            if (mouseHoverDisabled.get()) return
            mouseHoverTarget.set(true)
            coroutines.launch {
                delay(TOOLTIP_DELAY)
                if (mouseHoverTarget.get()) isOpen = true
            }
        }

        fun mayHideOnTargetExit() {
            mouseHoverTarget.set(false)
            delayHide()
        }

        internal fun hideOnTargetClicked() {
            mouseHoverTarget.set(false)
            mouseHoverDisabled.set(true)
            isOpen = false
            coroutines.launch {
                delay(TOOLTIP_DELAY)
                mouseHoverDisabled.set(false)
            }
        }

        internal fun mayHideOnTooltipExit() {
            if (tooltipExpanded.compareAndExchange(true, false)) return
            mouseHoverTooltip.set(false)
            delayHide()
        }

        private fun delayHide() {
            coroutines.launch {
                delay(TOOLTIP_DELAY)
                if (!mouseHoverTarget.get() && !mouseHoverTooltip.get()) isOpen = false
            }
        }

        @OptIn(ExperimentalComposeUiApi::class)
        internal fun onKeyEvent(event: KeyEvent) = when (event.key) {
            Key.Escape -> {
                isOpen = false
                true
            }
            else -> false
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Popup(state: State) {
        if (state.isOpen) {
            var showAll by remember { mutableStateOf(false) }
            val density = LocalDensity.current.density
            var height by remember { mutableStateOf(0.dp) }
            val mouseY = MouseInfo.getPointerInfo().location.y - LocalWindowContext.current!!.y
            val positionBelowMouse = mouseY < LocalWindowContext.current!!.height - height.value - TOOLTIP_OFFSET.value
            val offsetY = if (positionBelowMouse) TOOLTIP_OFFSET else -TOOLTIP_OFFSET
            Popup(
                focusable = true,
                popupPositionProvider = rememberCursorPositionProvider(DpOffset(0.dp, offsetY)),
                onDismissRequest = { state.isOpen = false },
                onKeyEvent = { state.onKeyEvent(it) }
            ) {
                val hasDetails = state.arg.description != null || state.arg.url != null
                val boxMod = if (hasDetails) Modifier.width(TOOLTIP_WIDTH) else Modifier.widthIn(max = TOOLTIP_WIDTH)
                val contentMod = if (hasDetails) Modifier.fillMaxWidth() else Modifier
                Box(
                    boxMod.background(color = Theme.studio.surface)
                        .border(BORDER_WIDTH, Theme.studio.border, RectangleShape)
                        .onSizeChanged { height = toDP(it.width, density) }
                        .onPointerEvent(Enter) { state.keepShowingOnTooltipHover() }
                        .onPointerEvent(Exit) { state.mayHideOnTooltipExit() }
                ) {
                    Column(contentMod.padding(TOOLTIP_SPACE)) {
                        Row(contentMod, Arrangement.SpaceBetween) {
                            Text(value = state.arg.title, softWrap = true)
                            if (!showAll && hasDetails) {
                                ClickableText(Label.READ_MORE, Theme.studio.secondary) {
                                    state.keepShowingOnTooltipHover(true)
                                    showAll = true
                                }
                            }
                        }
                        if (showAll) {
                            Spacer(Modifier.height(TOOLTIP_SPACE))
                            Separator.Horizontal()
                            state.arg.description?.let {
                                Spacer(Modifier.height(TOOLTIP_SPACE))
                                Text(value = it, softWrap = true)
                            }
                            state.arg.url?.let {
                                Spacer(Modifier.height(TOOLTIP_SPACE))
                                URLText(it, text = Label.LEARN_MORE)
                            }
                        }
                    }
                }
            }
        }
    }
}
