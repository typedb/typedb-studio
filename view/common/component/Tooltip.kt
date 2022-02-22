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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.rememberCursorPositionProvider
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form.BORDER_WIDTH
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Form.TextURL
import com.vaticle.typedb.studio.view.common.theme.Theme
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Tooltip {

    @OptIn(ExperimentalTime::class)
    private val TOOLTIP_DELAY = Duration.Companion.milliseconds(800)
    private val TOOLTIP_WIDTH = 400.dp
    private val TOOLTIP_OFFSET = 32.dp
    private val TOOLTIP_SPACE = 8.dp

    data class Args(val title: String, val description: String? = null, val url: URL? = null)

    @OptIn(ExperimentalTime::class)
    class State(internal val args: Args) {

        internal var isOpen by mutableStateOf(false)
        private val mouseHoverTarget = AtomicBoolean(false)
        private val mouseHoverTooltip = AtomicBoolean(false)
        private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

        internal fun mayShowOnTargetHover() {
            mouseHoverTarget.set(true)
            coroutineScope.launch {
                delay(TOOLTIP_DELAY)
                if (mouseHoverTarget.get()) isOpen = true
            }
        }

        internal fun mayHideOnTargetExit() {
            mouseHoverTarget.set(false)
            coroutineScope.launch {
                delay(TOOLTIP_DELAY)
                if (!mouseHoverTarget.get() && !mouseHoverTooltip.get()) isOpen = false
            }
        }

        internal fun keepShowingOnTooltipHover() {
            mouseHoverTooltip.set(true)
            isOpen = true
        }

        internal fun mayHideOnTooltipExit() {
            mouseHoverTooltip.set(false)
            if (!mouseHoverTarget.get()) isOpen = false
        }

        @OptIn(ExperimentalComposeUiApi::class)
        internal fun onKeyEvent(event: KeyEvent): Boolean {
            return when (event.key) {
                Key.Escape -> {
                    isOpen = false
                    true
                }
                else -> false
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Popup(state: State) {
        if (state.isOpen) {
            val colors = Theme.colors
            val positionState = rememberCursorPositionProvider()
            Popup(
                focusable = true,
                popupPositionProvider = positionState,
                onDismissRequest = { state.isOpen = false },
                onKeyEvent = { state.onKeyEvent(it) }
            ) {
                Box(
                    Modifier.width(TOOLTIP_WIDTH).pointerMoveFilter(
                        onEnter = { state.keepShowingOnTooltipHover(); false },
                        onExit = { state.mayHideOnTooltipExit(); false },
                    )
                ) {
                    Box(
                        Modifier.padding(vertical = TOOLTIP_OFFSET).background(colors.surface)
                            .border(BORDER_WIDTH, colors.border, RectangleShape)
                    ) {
                        Column(Modifier.padding(TOOLTIP_SPACE)) {
                            Text(value = state.args.title, softWrap = true)
                            if (state.args.description != null || state.args.url != null) {
                                Spacer(Modifier.height(TOOLTIP_SPACE))
                                Separator.Horizontal()
                                state.args.description?.let {
                                    Spacer(Modifier.height(TOOLTIP_SPACE))
                                    Text(value = it, softWrap = true)
                                }
                                state.args.url?.let {
                                    Spacer(Modifier.height(TOOLTIP_SPACE))
                                    TextURL(it, text = Label.LEARN_MORE)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}