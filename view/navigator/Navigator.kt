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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.common.collection.Either.second
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Layout
import com.vaticle.typedb.studio.view.common.component.Layout.Resizable
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.navigator.Navigator.NavigatorType.PROJECT
import com.vaticle.typedb.studio.view.navigator.Navigator.NavigatorType.ROLES
import com.vaticle.typedb.studio.view.navigator.Navigator.NavigatorType.RULES
import com.vaticle.typedb.studio.view.navigator.Navigator.NavigatorType.TYPES
import com.vaticle.typedb.studio.view.navigator.Navigator.NavigatorType.USERS

object Navigator {

    const val ID = "NAVIGATOR_AREA"
    val WIDTH = 300.dp
    val MIN_WIDTH = 120.dp
    private val SIDE_TAB_WIDTH = 22.dp
    private val SIDE_TAB_HEIGHT = 100.dp
    private val PANEL_BAR_HEIGHT = 26.dp
    private val PANEL_BAR_SPACING = 8.dp
    private val PANEL_MIN_HEIGHT = 80.dp
    private val ICON_SIZE = 10.sp
    private val TAB_OFFSET = (-40).dp

    private enum class NavigatorType(val label: String, val icon: Icon.Code) {
        PROJECT(Label.PROJECT, Icon.Code.FOLDER_BLANK),
        TYPES(Label.TYPES, Icon.Code.SITEMAP),
        RULES(Label.RULES, Icon.Code.DIAGRAM_PROJECT),
        USERS(Label.USERS, Icon.Code.USER),
        ROLES(Label.ROLES, Icon.Code.USER_GROUP)
    }

    private class NavigatorState(val type: NavigatorType, val areaState: AreaState, initOpen: Boolean = false) {
        var isOpen: Boolean by mutableStateOf(initOpen)
        val icon; get() = type.icon
        val label; get() = type.label

        fun toggle() {
            isOpen = !isOpen
            areaState.mayHidePanelArea()
        }
    }

    private class AreaState(val layoutState: Resizable.ItemState) {
        val navigators = linkedMapOf(
            PROJECT to NavigatorState(PROJECT, this, true),
            TYPES to NavigatorState(TYPES, this, true),
            RULES to NavigatorState(RULES, this),
            USERS to NavigatorState(USERS, this),
            ROLES to NavigatorState(ROLES, this)
        )

        init {
            mayHidePanelArea()
        }

        fun openedNavigators(): List<NavigatorState> {
            return navigators.values.filter { it.isOpen }
        }

        fun mayHidePanelArea() {
            if (openedNavigators().isEmpty()) layoutState.freeze(SIDE_TAB_WIDTH)
            else layoutState.unfreeze()
        }
    }

    @Composable
    fun Area(layoutState: Resizable.ItemState) {
        val areaState = remember { AreaState(layoutState) }
        val openedNavigators = areaState.openedNavigators()
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(SIDE_TAB_WIDTH), verticalArrangement = Arrangement.Top) {
                areaState.navigators.values.forEach { Tab(it) }
            }
            if (openedNavigators.isNotEmpty()) {
                Layout.VerticalSeparator()
                if (openedNavigators.size == 1) Panel(openedNavigators.first())
                else Resizable.Column(
                    modifier = Modifier.fillMaxHeight().weight(1f),
                    separator = Resizable.Separator(Layout.SEPARATOR_WEIGHT),
                    *openedNavigators.map { navigator ->
                        Resizable.Item(
                            id = navigator.label,
                            initSize = second(1f),
                            minSize = PANEL_MIN_HEIGHT
                        ) { Panel(navigator) }
                    }.toTypedArray()
                )
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
                Spacer(modifier = Modifier.width(PANEL_BAR_SPACING))
                Text(value = navigator.label)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Layout.HorizontalSeparator()
    }

    @Composable
    private fun Panel(navigator: NavigatorState, modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            PanelTitle(navigator)
            Layout.HorizontalSeparator()
            Box(modifier = Modifier.weight(1f)) {
                when (navigator.type) {
                    PROJECT -> ProjectNavigator.Layout()
                    TYPES -> TypeNavigator.Layout()
                    RULES -> RuleNavigator.Layout()
                    USERS -> UserNavigator.Layout()
                    ROLES -> RolesNavigator.Layout()
                }
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
            Text(value = navigator.label)
            Spacer(Modifier.weight(1f))
            IconButton(
                icon = Icon.Code.XMARK,
                onClick = { navigator.toggle() },
                bgColor = Color.Transparent,
                modifier = Modifier.size(PANEL_BAR_HEIGHT)
            )
        }
    }

    @Composable
    private fun PanelBarSpace() {
        Spacer(Modifier.width(PANEL_BAR_SPACING))
    }
}
