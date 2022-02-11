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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.rememberCursorPositionProvider
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.theme.Theme
import java.awt.event.MouseEvent
import mu.KotlinLogging

object ContextMenu {

    private val ITEM_HEIGHT = 28.dp
    private val ITEM_WIDTH = 180.dp
    private val ITEM_PADDING = 6.dp
    private val POPUP_SHADOW = 12.dp
    private val LOGGER = KotlinLogging.logger {}

    data class Item(
        val label: String,
        val icon: Icon.Code? = null,
        val info: String? = null,
        val enabled: Boolean = true,
        val onClick: () -> Unit
    )

    class State {

        internal var isOpen by mutableStateOf(false)

        suspend fun onPointerInput(
            pointerInputScope: PointerInputScope,
            onSinglePrimaryPressed: (MouseEvent) -> Unit = {},
            onDoublePrimaryPressed: (MouseEvent) -> Unit = {},
            onTriplePrimaryPressed: (MouseEvent) -> Unit = {},
            onSecondaryClick: (MouseEvent) -> Unit = {}
        ) {
            pointerInputScope.forEachGesture {
                awaitPointerEventScope {
                    val event = awaitEventFirstDown()
                    event.changes.forEach { it.consumeDownChange() }
                    when {
                        event.buttons.isPrimaryPressed -> {
                            when (event.awtEvent.clickCount) {
                                0, 1 -> onSinglePrimaryPressed(event.awtEvent)
                                2 -> onDoublePrimaryPressed(event.awtEvent)
                                3 -> onTriplePrimaryPressed(event.awtEvent)
                            }
                        }
                        event.buttons.isSecondaryPressed -> {
                            onSecondaryClick(event.awtEvent)
                            isOpen = true
                        }
                    }
                }
            }
        }

        private suspend fun AwaitPointerEventScope.awaitEventFirstDown(): PointerEvent {
            var event: PointerEvent
            do event = awaitPointerEvent()
            while (!event.changes.all { it.changedToDown() })
            return event
        }

        @OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
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
    fun Popup(state: State, itemListsFn: () -> List<List<Item>>) {
        if (state.isOpen) {
            Popup(
                focusable = true,
                popupPositionProvider = rememberCursorPositionProvider(),
                onDismissRequest = { state.isOpen = false },
                onKeyEvent = { state.onKeyEvent(it) },
            ) {
                Column(
                    modifier = Modifier.shadow(POPUP_SHADOW)
                        .background(Theme.colors.surface)
                        .border(Form.BORDER_WIDTH, Theme.colors.border, RectangleShape)
                        .width(IntrinsicSize.Max).verticalScroll(rememberScrollState())
                ) {
                    itemListsFn().forEach { list ->
                        list.forEach { item -> Item(item, state) }
                        Separator.Horizontal()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Item(item: Item, state: State) {
        var modifier = Modifier.sizeIn(minWidth = ITEM_WIDTH, minHeight = ITEM_HEIGHT) // TODO: compute max minWidth
        if (item.enabled) modifier = modifier
            .pointerHoverIcon(PointerIconDefaults.Hand)
            .clickable { state.isOpen = false; item.onClick() }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
            Box(modifier = Modifier.size(ITEM_HEIGHT), contentAlignment = Alignment.Center) {
                item.icon?.let { Icon.Render(icon = it, enabled = item.enabled) }
            }
            Text(value = item.label, enabled = item.enabled)
            item.info?.let {
                Spacer(Modifier.weight(1f))
                Text(value = it, enabled = false)
                Spacer(Modifier.width(ITEM_PADDING))
            }
        }
    }
}
