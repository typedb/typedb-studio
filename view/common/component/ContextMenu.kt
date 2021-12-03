package com.vaticle.typedb.studio.view.common.component

import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.rememberCursorPositionProvider
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.theme.Theme

object ContextMenu {

    private val ITEM_HEIGHT = 28.dp
    private val ITEM_WIDTH = 160.dp
    private val POPUP_SHADOW = 12.dp

    data class Item(val label: String, val icon: Icon.Code? = null, val onClick: () -> Unit)

    @Composable
    @ExperimentalFoundationApi
    fun Area(items: List<Item>?, enabled: Boolean = true, content: @Composable () -> Unit) {
        val state: ContextMenuState = remember { ContextMenuState() }
        Box(Modifier.contextMenuDetector(state, enabled), propagateMinConstraints = true) { content() }
        if (enabled && !items.isNullOrEmpty()) MenuPopup(state, items)
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun Modifier.contextMenuDetector(state: ContextMenuState, enabled: Boolean = true): Modifier {
        return if (enabled && state.status == ContextMenuState.Status.Closed) this.pointerInput(state) {
            forEachGesture {
                awaitPointerEventScope {
                    val event = awaitEventFirstDown()
                    if (event.buttons.isSecondaryPressed) {
                        event.changes.forEach { it.consumeDownChange() }
                        state.status = ContextMenuState.Status.Open(Rect(event.changes[0].position, 0f))
                    }
                }
            }
        } else Modifier
    }

    private suspend fun AwaitPointerEventScope.awaitEventFirstDown(): PointerEvent {
        var event: PointerEvent
        do event = awaitPointerEvent()
        while (!event.changes.all { it.changedToDown() })
        return event
    }

    @OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
    @Composable
    private fun MenuPopup(state: ContextMenuState, items: List<Item>) {
        if (state.status is ContextMenuState.Status.Open) {
            Popup(
                focusable = true,
                popupPositionProvider = rememberCursorPositionProvider(),
                onDismissRequest = { state.status = ContextMenuState.Status.Closed },
                onKeyEvent = { onKeyEvent(it, state) },
            ) {
                Column(
                    modifier = Modifier.shadow(POPUP_SHADOW)
                        .background(Theme.colors.surface)
                        .border(Form.BORDER_WIDTH, Theme.colors.border, RectangleShape)
                        .width(IntrinsicSize.Max).verticalScroll(rememberScrollState())
                ) {
                    items.forEach {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .pointerHoverIcon(PointerIconDefaults.Hand)
                                .clickable { state.status = ContextMenuState.Status.Closed; it.onClick() }
                                .sizeIn(minWidth = ITEM_WIDTH, minHeight = ITEM_HEIGHT)
                        ) {
                            Box(modifier = Modifier.size(ITEM_HEIGHT), contentAlignment = Alignment.Center) {
                                it.icon?.let { Icon.Render(icon = it) }
                            }
                            Text(value = it.label)
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
    private fun onKeyEvent(it: KeyEvent, state: ContextMenuState): Boolean {
        return when (it.key) {
            Key.Escape -> {
                state.status = ContextMenuState.Status.Closed; true
            }
            else -> false
        }
    }
}
