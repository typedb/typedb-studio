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

package com.vaticle.typedb.studio.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.common.component.Form.TextSelectable
import com.vaticle.typedb.studio.common.component.Icon
import com.vaticle.typedb.studio.common.theme.Theme
import com.vaticle.typedb.studio.service.NotifierService
import com.vaticle.typedb.studio.service.NotifierService.MessageType.ERROR
import com.vaticle.typedb.studio.service.NotifierService.MessageType.INFO
import com.vaticle.typedb.studio.service.Service

object NotificationArea {

    private val NOTIFICATION_MARGIN = 30.dp
    private val MESSAGE_WIDTH = 360.dp
    private val MESSAGE_HEIGHT = 80.dp
    private val MESSAGE_PADDING = 8.dp
    private val MESSAGE_SHAPE = RoundedCornerShape(4.dp)

    data class ColorConfig(val background: Color, val foreground: Color)

    @Composable
    fun Layout() {
        androidx.compose.ui.window.Popup(alignment = Alignment.BottomEnd) {
            Column(modifier = Modifier.padding(NOTIFICATION_MARGIN)) {
                Service.notifier.messages.forEach { notification ->
                    Message(message = notification)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Message(message: NotifierService.Message, modifier: Modifier = Modifier) {
        val colorConfig = colorConfigOf(message.type)
        Row(
            modifier = modifier
                .width(MESSAGE_WIDTH)
                .defaultMinSize(minHeight = MESSAGE_HEIGHT)
                .padding(MESSAGE_PADDING)
                .background(color = colorConfig.background, shape = MESSAGE_SHAPE)
        ) {
            TextSelectable(
                value = message.text,
                color = colorConfig.foreground,
                modifier = Modifier.padding(MESSAGE_PADDING).weight(1f)
            )
            Icon.Render(
                icon = Icon.Code.XMARK,
                color = colorConfig.foreground,
                modifier = Modifier
                    .padding(MESSAGE_PADDING)
                    .pointerIcon(PointerIcon.Hand)
                    .clickable { Service.notifier.dismiss(message) }
            )
        }
    }

    @Composable
    private fun colorConfigOf(type: NotifierService.MessageType): ColorConfig {
        return when (type) {
            INFO -> ColorConfig(Theme.colors.surface3, Theme.colors.onSurface)
            ERROR -> ColorConfig(Theme.colors.error, Theme.colors.onError)
        }
    }
}
