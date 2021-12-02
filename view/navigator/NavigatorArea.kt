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

package com.vaticle.typedb.studio.view.navigator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.common.collection.Either.second
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Frame
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme

object NavigatorArea {

    const val ID = "NAVIGATOR_AREA"
    val WIDTH = 300.dp
    val MIN_WIDTH = 120.dp
    private val SIDE_TAB_WIDTH = 22.dp
    private val SIDE_TAB_HEIGHT = 100.dp
    private val SIDE_TAB_SPACING = 8.dp
    private val ICON_SIZE = 10.sp
    private val TAB_OFFSET = (-40).dp

    internal class AreaState(private val paneState: Frame.PaneState) {
        val navigators = listOf(
            ProjectNavigator(this, true),
            TypeNavigator(this, true),
            RuleNavigator(this),
            UserNavigator(this),
            RoleNavigator(this)
        )

        init {
            mayHidePanelArea()
        }

        fun openedNavigators(): List<Navigator> {
            return navigators.filter { it.isOpen }
        }

        fun mayHidePanelArea() {
            if (openedNavigators().isEmpty()) paneState.freeze(SIDE_TAB_WIDTH)
            else paneState.unfreeze()
        }
    }

    @Composable
    fun Layout(paneState: Frame.PaneState) {
        val areaState = remember { AreaState(paneState) }
        val openedNavigators = areaState.openedNavigators()
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(SIDE_TAB_WIDTH), verticalArrangement = Arrangement.Top) {
                areaState.navigators.forEach { Tab(it) }
            }
            if (openedNavigators.isNotEmpty()) {
                Separator.Vertical()
                if (openedNavigators.size == 1) openedNavigators.first().Layout()
                else Frame.Column(
                    modifier = Modifier.fillMaxHeight().weight(1f),
                    separator = Frame.SeparatorArgs(Separator.WEIGHT),
                    *openedNavigators.map { navigator ->
                        Frame.Pane(
                            id = navigator.label,
                            initSize = second(1f),
                            minSize = Navigator.MIN_HEIGHT
                        ) { navigator.Layout() }
                    }.toTypedArray()
                )
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Tab(navigator: Navigator) {
        @Composable
        fun bgColor(): Color = if (navigator.isOpen) Theme.colors.surface else Theme.colors.background2
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SIDE_TAB_HEIGHT)
                .pointerHoverIcon(icon = PointerIconDefaults.Hand)
                .clickable { navigator.toggle() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.requiredWidth(SIDE_TAB_HEIGHT)
                    .rotate(-90f)
                    .offset(x = TAB_OFFSET)
                    .background(color = bgColor())
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Icon.Render(icon = navigator.icon, size = ICON_SIZE)
                Spacer(modifier = Modifier.width(SIDE_TAB_SPACING))
                Text(value = navigator.label)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Separator.Horizontal()
    }
}
