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
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.app.StatusManager
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.material.Form
import com.vaticle.typedb.studio.view.material.Separator
import com.vaticle.typedb.studio.view.material.Tooltip

object StatusBar {

    private val HEIGHT = 22.dp
    private val PADDING = 12.dp

    @Composable
    fun Layout() {
        val statusMgr = GlobalState.status
        val fontStyle = Theme.typography.body2
        Row(Modifier.fillMaxWidth().height(HEIGHT), verticalAlignment = Alignment.CenterVertically) {
            if (statusMgr.loadingStatus.isNotEmpty()) {
                Spacer(Modifier.width(PADDING))
                Form.Text(value = statusMgr.loadingStatus, textStyle = fontStyle)
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(PADDING))
            StatusManager.Key.values().reversed().forEach {
                val statusValue = statusMgr.statuses[it]
                if (!statusValue.isNullOrEmpty()) {
                    Separator.Vertical()
                    StatusDisplay(it.displayName, it.description, statusValue, fontStyle)
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun StatusDisplay(name: String, description: String?, value: String, fontStyle: TextStyle) {
        val tooltipState: Tooltip.State = Tooltip.State(Tooltip.Arg(name, description))
        Tooltip.Popup(tooltipState)
        Column(Modifier.pointerMoveFilter(
            onEnter = { tooltipState.mayShowOnTargetHover(); false },
            onExit = { tooltipState.mayHideOnTargetExit(); false }
        )) {
            Row {
                Spacer(Modifier.width(PADDING))
                Form.Text(value = value, textStyle = fontStyle)
                Spacer(Modifier.width(PADDING))
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
