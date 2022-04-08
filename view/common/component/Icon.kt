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

package com.vaticle.typedb.studio.view.common.component

import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaticle.typedb.studio.view.common.theme.Color.fadeable
import com.vaticle.typedb.studio.view.common.theme.Theme

object Icon {

    private val ICON_DEFAULT_SIZE: TextUnit = 12.sp
    private val FONT_FILE = "resources/icons/fontawesome/font-awesome-solid-900.ttf"
    private val FONT_AWESOME = FontFamily(Font(FONT_FILE))

    data class Offset(val x: Dp, val y: Dp)
    data class Scale(val x: Float, val y: Float)

    enum class Code(
        hexcode: UShort,
        val size: TextUnit = ICON_DEFAULT_SIZE,
        val offset: Offset = Offset(0.dp, 0.dp),
        val rotate: Float = 0f,
        val scale: Scale = Scale(1f, 1f)
    ) {
        // These codes can be found at https://fontawesome.com/v6.0/icons
        // The icon names in Font Awesome would be the kebab-case version of our names below
        ALIGN_LEFT(0xf036u),
        ARROW_DOWN_TO_LINE(0xf33du),
        ARROW_UP_TO_LINE(0xf341u),
        ARROWS_MAXIMIZE(0xf31du),
        ARROWS_MINIMIZE(0xe0a5u),
        ASTERISK(0x2au),
        BLOCK_QUOTE(0xe0b5u),
        BOLT(0xf0e7u),
        CARET_DOWN(0xf0d7u),
        CARET_LEFT(0xf0d9u),
        CARET_RIGHT(0xf0dau),
        CARET_UP(0xf0d8u),
        CHART_NETWORK(0xf78au),
        CHECK(0xf00cu),
        CHEVRON_DOWN(0xf078u),
        CHEVRON_LEFT(0xf053u),
        CHEVRON_RIGHT(0xf054u),
        CHEVRON_UP(0xf077u),
        CHEVRONS_DOWN(0xf322u),
        CHEVRONS_LEFT(0xf323u),
        CHEVRONS_RIGHT(0xf324u),
        CHEVRONS_UP(0xf325u),
        CIRCLE(0xf111u),
        COPY(0xf0c5u),
        CUT(0xf0c4u),
        DATABASE(0xf1c0u),
        DELETE_RIGHT(0xe154u),
        DIAGRAM_PROJECT(0xf542u),
        DIAGRAM_SUBTASK(0xe479u),
        EXPAND(0xf065u),
        FILE_LINES(0xf15cu),
        FILE_PLUS(0xf319u),
        FLOPPY_DISK(0xf0c7u, 13.sp),
        FOLDER_ARROW_DOWN(0xe053u),
        FOLDER_ARROW_UP(0xe054u),
        FOLDER_OPEN(0xf07cu),
        FOLDER_BLANK(0xe185u),
        FOLDER_PLUS(0xf65eu),
        FONT_CASE(0xf866u),
        LETTER_W(0x57u),
        LINK_SIMPLE(0xe1cdu),
        MAGNIFYING_GLASS(0xf002u, rotate = 90f),
        OVAL(0xf111u, scale = Scale(1f, 0.618f)), // base icon is a Circle
        PASTE(0xf0eau),
        PEN(0xf304u),
        PLUS(0x2bu),
        PLAY(0xf04bu),
        RECTANGLE(0xf2fau),
        RECTANGLE_CODE(0xe322u),
        RHOMBUS(0xe23bu, rotate = 90f),
        ROTATE(0xf2f1u),
        ROTATE_LEFT(0xf2eau),
        RIGHT_LEFT(0xf362u),
        SERVER(0xf233u),
        SITEMAP(0xf0e8u),
        SQUARE_BOLT(0xe265u, 13.sp),
        STOP(0xf04du, 13.sp),
        TABLE_CELLS_LARGE(0xf009u),
        THUMBTACK(0xf08du),
        TRASH_CAN(0xf2edu),
        USER(0xf007u),
        USER_GROUP(0xf500u),
        XMARK(0xf00du, 13.sp);

        val unicode: String = Char(hexcode).toString()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Render(
        icon: Code,
        color: Color = Theme.colors.icon,
        disabledColor: Color? = null,
        size: TextUnit = icon.size,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        Text(
            text = icon.unicode,
            color = if (!enabled && disabledColor != null) disabledColor else fadeable(color, !enabled),
            fontSize = size,
            fontFamily = FONT_AWESOME,
            modifier = modifier.rotate(icon.rotate)
                .offset(icon.offset.x, icon.offset.y)
                .scale(icon.scale.x, icon.scale.y)
        )
    }
}
