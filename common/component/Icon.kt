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

package com.vaticle.typedb.studio.common.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.common.theme.Color.fadeable
import com.vaticle.typedb.studio.common.theme.Theme

object Icon {

    private val ICON_DEFAULT_SIZE: TextUnit = 12.sp
    private val FONT_AWESOME = FontFamily(Font(resource = "icons/fontawesome/font-awesome-6-pro-solid-900.otf"))

    data class Offset(val x: Dp, val y: Dp)

    enum class Code(
        private val hexcode: UShort,
        val defaultSize: TextUnit = ICON_DEFAULT_SIZE,
        val offset: Offset = Offset(0.dp, 0.dp)
    ) {
        // These codes can be found at https://fontawesome.com/v6.0/icons
        // The icon names in Font Awesome would be the kebab-case version of our names below
        CARET_DOWN(0xf0d7u),
        DATABASE(0xf1c0u),
        DIAGRAM_PROJECT(0xf542u),
        FLOPPY_DISK(0xf0c7u, 14.sp),
        FOLDER_OPEN(0xf07cu),
        FOLDER_BLANK(0xe185u),
        PLAY(0xf04bu, offset = Offset((-1).dp, 0.dp)),
        SITEMAP(0xf0e8u),
        STOP(0xf04du, 14.sp),
        USER(0xf007u),
        USER_GROUP(0xf500u),
        XMARK(0xf00du);

        val unicode: String = Char(hexcode).toString()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Render(
        icon: Code,
        size: TextUnit = icon.defaultSize,
        color: Color = Theme.colors.icon,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        Text(
            text = icon.unicode,
            color = fadeable(color, !enabled),
            fontSize = size,
            fontFamily = FONT_AWESOME,
            modifier = modifier
                .pointerIcon(PointerIcon.Hand)
                .offset(icon.offset.x, icon.offset.y)
                .focusable(true)
        )
    }
}
