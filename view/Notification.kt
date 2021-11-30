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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.state.notification.Notification
import com.vaticle.typedb.studio.state.notification.Notification.Type.ERROR
import com.vaticle.typedb.studio.state.notification.Notification.Type.INFO
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.TextSelectable
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Theme

object Notification {

    private val NOTIFICATION_MARGIN = 30.dp
    private val MESSAGE_WIDTH = 360.dp
    private val MESSAGE_HEIGHT = 80.dp
    private val MESSAGE_PADDING = 8.dp
    private val MESSAGE_CLOSE_SIZE = 26.dp
    private val MESSAGE_SHAPE = RoundedCornerShape(4.dp)

    data class ColorConfig(val background: Color, val foreground: Color)

    @Composable
    fun Area() {
        androidx.compose.ui.window.Popup(alignment = Alignment.BottomEnd) {
            Column(modifier = Modifier.padding(NOTIFICATION_MARGIN)) {
                State.notification.messages.forEach { notification ->
                    Message(notification = notification)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Message(notification: Notification, modifier: Modifier = Modifier) {
        val colorConfig = colorConfigOf(notification.type)
        Row(
            modifier = modifier
                .width(MESSAGE_WIDTH)
                .defaultMinSize(minHeight = MESSAGE_HEIGHT)
                .padding(MESSAGE_PADDING)
                .background(color = colorConfig.background, shape = MESSAGE_SHAPE)
        ) {
            TextSelectable(
                value = notification.text,
                color = colorConfig.foreground,
                modifier = Modifier.padding(MESSAGE_PADDING).weight(1f)
            )
            IconButton(
                icon = Icon.Code.XMARK,
                onClick = { State.notification.dismiss(notification) },
                iconColor = colorConfig.foreground,
                bgColor = Color.Transparent,
                modifier = Modifier.size(MESSAGE_CLOSE_SIZE)
            )
        }
    }

    @Composable
    private fun colorConfigOf(type: Notification.Type): ColorConfig {
        return when (type) {
            INFO -> ColorConfig(Theme.colors.border, Theme.colors.onSurface)
            ERROR -> ColorConfig(Theme.colors.error, Theme.colors.onError)
        }
    }
}
