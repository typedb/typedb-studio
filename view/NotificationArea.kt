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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.app.NotificationManager.Notification
import com.vaticle.typedb.studio.state.app.NotificationManager.Notification.Type.ERROR
import com.vaticle.typedb.studio.state.app.NotificationManager.Notification.Type.INFO
import com.vaticle.typedb.studio.state.app.NotificationManager.Notification.Type.WARNING
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.SelectableText
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Theme

object NotificationArea {

    private val NOTIFICATION_MARGIN = 30.dp
    private val NOTIFICATION_WIDTH = 360.dp
    private val NOTIFICATION_HEIGHT = 80.dp
    private val MESSAGE_PADDING = 8.dp
    private val MESSAGE_CLOSE_SIZE = 26.dp

    data class ColorArgs(val background: Color, val foreground: Color)

    @Composable
    fun Layout() {
        Popup(alignment = Alignment.BottomEnd) {
            Column(modifier = Modifier.padding(NOTIFICATION_MARGIN)) {
                if (GlobalState.notification.queue.size > 1) DismissAllButton()
                GlobalState.notification.queue.forEach { notification ->
                    Notification(notification = notification)
                }
            }
        }
    }

    @Composable
    private fun DismissAllButton() {
        val colorArgs = colorArgsOf(GlobalState.notification.queue.first().type)
        Row(modifier = Modifier.width(NOTIFICATION_WIDTH).padding(MESSAGE_PADDING)) {
            Spacer(Modifier.weight(1f))
            Form.TextButton(
                text = Label.DISMISS_ALL,
                textColor = colorArgs.foreground,
                bgColor = colorArgs.background,
                trailingIcon = Form.IconArg(Icon.Code.XMARK) { colorArgs.foreground },
            ) { GlobalState.notification.dismissAll() }
        }
    }

    @Composable
    private fun Notification(notification: Notification) {
        val colorArgs = colorArgsOf(notification.type)
        Row(
            modifier = Modifier.width(NOTIFICATION_WIDTH)
                .defaultMinSize(minHeight = NOTIFICATION_HEIGHT).padding(MESSAGE_PADDING)
                .background(color = colorArgs.background, shape = Theme.ROUNDED_CORNER_SHAPE)
        ) {
            SelectableText(
                value = notification.message,
                color = colorArgs.foreground,
                modifier = Modifier.padding(MESSAGE_PADDING).weight(1f)
            )
            IconButton(
                icon = Icon.Code.XMARK,
                modifier = Modifier.size(MESSAGE_CLOSE_SIZE),
                iconColor = colorArgs.foreground,
                bgColor = Color.Transparent
            ) { GlobalState.notification.dismiss(notification) }
        }
    }

    @Composable
    private fun colorArgsOf(type: Notification.Type): ColorArgs {
        return when (type) {
            INFO -> ColorArgs(Theme.colors.border, Theme.colors.onSurface)
            WARNING -> ColorArgs(Theme.colors.quaternary, Theme.colors.onSecondary)
            ERROR -> ColorArgs(Theme.colors.error, Theme.colors.onError)
        }
    }
}
