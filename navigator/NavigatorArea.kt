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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.common.Label
import com.vaticle.typedb.studio.common.component.Form
import com.vaticle.typedb.studio.common.component.Icon
import com.vaticle.typedb.studio.common.component.Separator
import com.vaticle.typedb.studio.common.theme.Theme
import com.vaticle.typedb.studio.navigator.NavigatorArea.NavigatorType.PROJECT
import com.vaticle.typedb.studio.navigator.NavigatorArea.NavigatorType.ROLES
import com.vaticle.typedb.studio.navigator.NavigatorArea.NavigatorType.RULES
import com.vaticle.typedb.studio.navigator.NavigatorArea.NavigatorType.TYPES
import com.vaticle.typedb.studio.navigator.NavigatorArea.NavigatorType.USERS

object NavigatorArea {

    private val AREA_WIDTH = 300.dp
    private val SIDE_TAB_WIDTH = 22.dp
    private val SIDE_TAB_HEIGHT = 100.dp
    private val PANEL_BAR_HEIGHT = 26.dp
    private val PANEL_BAR_SPACING = 8.dp
    private val ICON_SIZE = 10.sp
    private val TAB_OFFSET = (-40).dp

    private enum class NavigatorType(val label: String, val icon: Icon.Code) {
        PROJECT(Label.PROJECT, Icon.Code.FOLDER_BLANK),
        TYPES(Label.TYPES, Icon.Code.SITEMAP),
        RULES(Label.RULES, Icon.Code.DIAGRAM_PROJECT),
        USERS(Label.USERS, Icon.Code.USER),
        ROLES(Label.ROLES, Icon.Code.USER_GROUP)
    }

    private class NavigatorState(val type: NavigatorType, initOpen: Boolean = false) {
        var isOpen: Boolean by mutableStateOf(initOpen)
        val icon; get() = type.icon
        val label; get() = type.label

        fun toggle() {
            isOpen = !isOpen
        }
    }

    private class AreaState {
        val navigators = linkedMapOf(
            PROJECT to NavigatorState(PROJECT, true),
            TYPES to NavigatorState(TYPES, true),
            RULES to NavigatorState(RULES),
            USERS to NavigatorState(USERS),
            ROLES to NavigatorState(ROLES)
        )

        fun openedNavigators(): List<NavigatorState> {
            return navigators.values.filter { it.isOpen }
        }
    }

    @Composable
    fun Layout() {
        val areaState = remember { AreaState() }
        Row(Modifier.width(if (areaState.openedNavigators().isEmpty()) SIDE_TAB_WIDTH else AREA_WIDTH)) {
            Column(Modifier.width(SIDE_TAB_WIDTH), verticalArrangement = Arrangement.Top) {
                areaState.navigators.values.forEach { Tab(it) }
            }
            Separator.Vertical()
            Column(Modifier.weight(1f)) {
                val openNavigators: List<NavigatorState> = areaState.openedNavigators()
                openNavigators.forEachIndexed { i, navigator ->
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        Panel(navigator) {
                            when (navigator.type) {
                                PROJECT -> ProjectNavigator.Layout()
                                TYPES -> TypeNavigator.Layout()
                                RULES -> RuleNavigator.Layout()
                                USERS -> UserNavigator.Layout()
                                ROLES -> RolesNavigator.Layout()
                            }
                        }
                    }
                    if (i < openNavigators.size - 1) Separator.Horizontal()
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Tab(navigator: NavigatorState) {
        @Composable
        fun bgColor(): Color = if (navigator.isOpen) Theme.colors.surface else Theme.colors.background2
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SIDE_TAB_HEIGHT)
                .pointerIcon(PointerIcon.Hand)
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
                Spacer(modifier = Modifier.width(PANEL_BAR_SPACING))
                Form.Text(value = navigator.label)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Separator.Horizontal()
    }

    @Composable
    private fun Panel(navigator: NavigatorState, content: @Composable () -> Unit) {
        Column(modifier = Modifier.fillMaxSize()) {
            PanelTitle(navigator)
            Separator.Horizontal()
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun PanelTitle(navigator: NavigatorState) {
        Row(
            modifier = Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT).background(color = Theme.colors.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PanelBarSpace()
            Icon.Render(icon = navigator.icon)
            PanelBarSpace()
            Form.Text(value = navigator.label)
            Spacer(Modifier.weight(1f))
            Icon.Render(
                icon = Icon.Code.XMARK,
                size = ICON_SIZE,
                modifier = Modifier.pointerIcon(PointerIcon.Hand).clickable { navigator.toggle() })
            PanelBarSpace()
        }
    }

    @Composable
    private fun PanelBarSpace() {
        Spacer(Modifier.width(PANEL_BAR_SPACING))
    }
}
