/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.material

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.rememberCursorPositionProvider
import com.typedb.studio.framework.common.Util.toDP
import com.typedb.studio.framework.common.theme.Theme
import com.typedb.studio.framework.material.Form.Text
import java.awt.event.MouseEvent

object ContextMenu {

    private val ITEM_HEIGHT = 28.dp
    private val ITEM_WIDTH = 180.dp
    private val ITEM_PADDING = 6.dp
    private val ITEM_SPACING = 20.dp
    private val POPUP_SHADOW = 12.dp

    data class Item(
        val label: String,
        val icon: Icon? = null,
        val iconColor: @Composable () -> Color = { Theme.studio.icon },
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
                    event.changes.forEach { if (it.pressed != it.previousPressed) it.consume() }
                    when {
                        event.buttons.isPrimaryPressed -> {
                            event.awtEventOrNull?.let {
                                when (it.clickCount) {
                                    0, 1 -> onSinglePrimaryPressed(it)
                                    2 -> onDoublePrimaryPressed(it)
                                    else -> onTriplePrimaryPressed(it)
                                }
                            }
                        }
                        event.buttons.isSecondaryPressed -> {
                            event.awtEventOrNull?.let(onSecondaryClick)
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

    @Composable
    fun Popup(state: State, itemListsFn: () -> List<List<Item>>) {
        if (state.isOpen) {
            Popup(
                focusable = true,
                popupPositionProvider = rememberCursorPositionProvider(),
                onDismissRequest = { state.isOpen = false },
                onKeyEvent = { state.onKeyEvent(it) },
            ) {
                val density = LocalDensity.current.density
                var width by remember { mutableStateOf(ITEM_WIDTH) }
                fun mayUpdateWidth(rawWidth: Int) {
                    val newWidth = toDP(rawWidth, density)
                    if (newWidth > width) width = newWidth
                }
                Column(
                    modifier = Modifier.shadow(POPUP_SHADOW)
                        .background(Theme.studio.surface)
                        .border(Form.BORDER_WIDTH, Theme.studio.border, RectangleShape)
                        .width(IntrinsicSize.Max).onSizeChanged { mayUpdateWidth(it.width) }
                        .verticalScroll(rememberScrollState())
                ) {
                    val itemsLists = remember { itemListsFn() }
                    assert(itemsLists.isNotEmpty()) { "You should not pass an empty list in to a context menu" }
                    itemsLists.forEach { list ->
                        list.forEach { item -> Item(state, item, width) }
                        Separator.Horizontal()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Item(state: State, item: Item, minWidth: Dp) {
        var modifier = Modifier.defaultMinSize(minWidth).height(ITEM_HEIGHT)
        if (item.enabled) modifier = modifier
            .pointerHoverIcon(PointerIconDefaults.Hand)
            .clickable { state.isOpen = false; item.onClick() }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
            Box(modifier = Modifier.size(ITEM_HEIGHT), contentAlignment = Alignment.Center) {
                item.icon?.let { Icon.Render(icon = it, color = item.iconColor(), enabled = item.enabled) }
            }
            Text(value = item.label, enabled = item.enabled)
            item.info?.let {
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(ITEM_SPACING))
                Text(value = it, enabled = false)
            }
            Spacer(Modifier.width(ITEM_PADDING))
        }
    }
}
