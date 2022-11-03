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

package com.vaticle.typedb.studio.module

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
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Separator
import com.vaticle.typedb.studio.framework.material.Tooltip
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.app.StatusService
import com.vaticle.typedb.studio.state.app.StatusService.Key.OUTPUT_RESPONSE_TIME
import com.vaticle.typedb.studio.state.app.StatusService.Key.QUERY_RESPONSE_TIME
import com.vaticle.typedb.studio.state.app.StatusService.Key.TEXT_CURSOR_POSITION
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Sentence

object StatusBar {

    private val HEIGHT = 22.dp
    private val PADDING = 12.dp
    private val SPACING = 8.dp
    private val ICON_SIZE = 10.sp

    @Composable
    fun Layout() {
        val statusSrv = StudioState.status
        val fontStyle = Theme.typography.body2
        Row(Modifier.fillMaxWidth().height(HEIGHT), verticalAlignment = Alignment.CenterVertically) {
            if (statusSrv.loadingStatus.isNotEmpty()) {
                Spacer(Modifier.width(PADDING))
                Form.Text(value = statusSrv.loadingStatus, textStyle = fontStyle)
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(PADDING))
            StatusService.Key.values().reversed().forEach {
                val statusValue = statusSrv.statuses[it]
                if (!statusValue.isNullOrEmpty()) {
                    Separator.Vertical()
                    StatusDisplay(it, statusValue, fontStyle)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun StatusDisplay(key: StatusService.Key, value: String, fontStyle: TextStyle) {
        val tooltipState: Tooltip.State = Tooltip.State(tooltipArg(key))
        Tooltip.Popup(tooltipState)
        Column(Modifier.pointerMoveFilter(
            onEnter = { tooltipState.mayShowOnTargetHover(); false },
            onExit = { tooltipState.mayHideOnTargetExit(); false }
        )) {
            Row(Modifier.height(HEIGHT), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(PADDING))
                Icon.Render(icon = icon(key), size = ICON_SIZE)
                Spacer(Modifier.width(SPACING))
                Form.Text(value = value, textStyle = fontStyle)
                Spacer(Modifier.width(PADDING))
            }
            Spacer(Modifier.height(2.dp))
        }
    }

    private fun tooltipArg(key: StatusService.Key): Tooltip.Arg {
        return when (key) {
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
        }
    }

    private fun icon(key: StatusService.Key): Icon {
        return when (key) {
            TEXT_CURSOR_POSITION -> Icon.CURSOR
            OUTPUT_RESPONSE_TIME -> Icon.RESPONSE_TIME
            QUERY_RESPONSE_TIME -> Icon.RESPONSE_TIME
        }
    }
}
