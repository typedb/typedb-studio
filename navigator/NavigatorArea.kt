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

package com.vaticle.typedb.studio.navigator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.common.Label
import com.vaticle.typedb.studio.common.component.Form
import com.vaticle.typedb.studio.common.component.Separator

object NavigatorArea {

    private val AREA_WIDTH = 300.dp
    private val TAB_HEIGHT = 26.dp
    private val TAB_WIDTH = 100.dp
    private val TAB_OFFSET = (-40).dp

    @Composable
    fun Layout() {
        Row(Modifier.width(AREA_WIDTH)) {
            Column(Modifier.width(TAB_HEIGHT), verticalArrangement = Arrangement.Top) {
                Tab(Label.PROJECT)
                Tab(Label.TYPES)
                Tab(Label.RULES)
                Tab(Label.USERS)
                Tab(Label.ROLES)
            }
            Separator.Vertical()
            Column(Modifier.weight(1f)) {
                val panels = arrayOf(1, 2, 3)
                for ((i, panel) in panels.withIndex()) {
                    if (i > 0) Separator.Horizontal()
                    Row(Modifier.fillMaxWidth().weight(1f)) {

                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Tab(text: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TAB_WIDTH)
                .pointerIcon(PointerIcon.Hand)
        ) {
            Form.Text(
                value = text, align = TextAlign.Center, modifier = Modifier
                    .requiredWidth(TAB_WIDTH)
                    .rotate(-90f)
                    .offset(x = TAB_OFFSET)
            )
        }
        Separator.Horizontal()
    }
}
