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

package com.vaticle.typedb.studio.view.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.state.State
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.IconButton
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme

object PageArea {

    const val ID = "PAGE_AREA"
    val MIN_WIDTH = 100.dp
    private val TAB_HEIGHT = 26.dp
    private val TAB_SPACING = 8.dp
    private val ICON_SIZE = 10.sp

    internal class AreaState {

    }

    @Composable
    fun Area() {
        val areaState = remember { AreaState() }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().height(TAB_HEIGHT), horizontalArrangement = Arrangement.Start) {
                State.page.openedPages.forEach { Tab(Page.of(it)) }
            }
            Separator.Horizontal()
            Row(Modifier.fillMaxWidth()) {
                State.page.selectedPage?.let { Page.of(it).Layout() }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun Tab(page: Page) {
        @Composable
        fun bgColor(): Color = when {
            State.page.isSelected(page.editable) -> Theme.colors.surface
            else -> Theme.colors.background
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(color = bgColor())
                .height(TAB_HEIGHT)
                .pointerHoverIcon(PointerIconDefaults.Hand)
                .clickable { State.page.selectedPage = page.editable }
        ) {
            Spacer(modifier = Modifier.width(TAB_SPACING))
            Icon.Render(icon = page.icon.code, size = ICON_SIZE, color = page.icon.color())
            Spacer(modifier = Modifier.width(TAB_SPACING))
            Text(value = page.label)
            Spacer(modifier = Modifier.width(TAB_SPACING))
            val button = Form.ButtonArgs(Icon.Code.XMARK) { State.page.close(page.editable) }
            IconButton(
                icon = button.icon,
                onClick = { button.onClick() },
                modifier = Modifier.size(TAB_HEIGHT),
                bgColor = Color.Transparent,
                rounded = false,
            )
        }
        Separator.Vertical()
    }
}
