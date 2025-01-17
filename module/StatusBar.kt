/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.module

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Enter
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Exit
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typedb.studio.framework.common.theme.Theme
import com.typedb.studio.framework.material.Form
import com.typedb.studio.framework.material.Icon
import com.typedb.studio.framework.material.Separator
import com.typedb.studio.framework.material.Tooltip
import com.typedb.studio.service.Service
import com.typedb.studio.service.common.StatusService
import com.typedb.studio.service.common.StatusService.Key.OUTPUT_RESPONSE_TIME
import com.typedb.studio.service.common.StatusService.Key.QUERY_RESPONSE_TIME
import com.typedb.studio.service.common.StatusService.Key.SCHEMA_EXCEPTIONS
import com.typedb.studio.service.common.StatusService.Key.TEXT_CURSOR_POSITION
import com.typedb.studio.service.common.StatusService.Status.Type.ERROR
import com.typedb.studio.service.common.StatusService.Status.Type.INFO
import com.typedb.studio.service.common.StatusService.Status.Type.WARNING
import com.typedb.studio.service.common.util.Label
import com.typedb.studio.service.common.util.Sentence

object StatusBar {

    private val HEIGHT = 22.dp
    private val PADDING = 12.dp
    private val SPACING = 8.dp
    private val ICON_SIZE = 10.sp

    @Composable
    fun Layout() {
        val statusSrv = Service.status
        val textStyle = Theme.typography.body2
        Row(Modifier.fillMaxWidth().height(HEIGHT), verticalAlignment = Alignment.CenterVertically) {
            if (statusSrv.loadingStatus.isNotEmpty()) {
                Spacer(Modifier.width(PADDING))
                Form.Text(value = statusSrv.loadingStatus, textStyle = textStyle)
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(PADDING))
            StatusService.Key.values().reversed().forEach {
                val status = statusSrv.statuses[it]
                if (status != null) {
                    Separator.Vertical()
                    StatusDisplay(status, textStyle)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun StatusDisplay(status: StatusService.Status, textStyle: TextStyle) {
        val tooltipState: Tooltip.State = Tooltip.State(tooltipArg(status.key))
        Tooltip.Popup(tooltipState)
        Column(Modifier
            .onPointerEvent(Enter) { tooltipState.mayShowOnTargetHover() }
            .onPointerEvent(Exit) { tooltipState.mayHideOnTargetExit() }
        ) {
            Row(Modifier.height(HEIGHT), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(PADDING))
                Icon.Render(icon = icon(status.key), color = iconColor(status.type), size = ICON_SIZE)
                Spacer(Modifier.width(SPACING))
                status.onClick?.let { fn ->
                    val textColor = textColor(status.type)
                    Form.ClickableText(
                        value = status.message, color = textColor, hoverColor = textColor,
                        textStyle = textStyle, onClick = fn
                    )
                } ?: Form.Text(value = status.message, textStyle = textStyle)
                Spacer(Modifier.width(PADDING))
            }
            Spacer(Modifier.height(2.dp))
        }
    }

    private fun tooltipArg(key: StatusService.Key) = when (key) {
        TEXT_CURSOR_POSITION -> Tooltip.Arg(
            title = Label.TEXT_CURSOR_POSITION
        )
        OUTPUT_RESPONSE_TIME -> Tooltip.Arg(
            title = Label.OUTPUT_RESPONSE_TIME,
            description = Sentence.QUERY_RESPONSE_TIME_DESCRIPTION,
        )
        QUERY_RESPONSE_TIME -> Tooltip.Arg(
            title = Label.QUERY_RESPONSE_TIME,
            description = Sentence.OUTPUT_RESPONSE_TIME_DESCRIPTION,
        )
        SCHEMA_EXCEPTIONS -> Tooltip.Arg(
            title = Label.SCHEMA_EXCEPTIONS,
            description = Sentence.SCHEMA_EXCEPTIONS_DESCRIPTION,
        )
    }

    private fun icon(key: StatusService.Key) = when (key) {
        TEXT_CURSOR_POSITION -> Icon.CURSOR
        OUTPUT_RESPONSE_TIME -> Icon.RESPONSE_TIME
        QUERY_RESPONSE_TIME -> Icon.RESPONSE_TIME
        SCHEMA_EXCEPTIONS -> Icon.ALERT
    }

    @Composable
    private fun iconColor(type: StatusService.Status.Type): Color = when (type) {
        INFO -> Theme.studio.icon
        WARNING -> Theme.studio.warningStroke
        ERROR -> Theme.studio.errorStroke
    }

    @Composable
    private fun textColor(type: StatusService.Status.Type): Color = when (type) {
        INFO -> Theme.studio.onPrimary
        WARNING -> Theme.studio.warningStroke
        ERROR -> Theme.studio.errorStroke
    }
}
