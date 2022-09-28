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

package com.vaticle.typedb.studio.framework.material

import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import com.vaticle.typedb.studio.framework.common.theme.Color.fadeable
import com.vaticle.typedb.studio.framework.common.theme.Theme

object Icon {

    private const val FONT_FILE = "resources/icons/fontawesome/font-awesome-6-pro-solid-900.otf"
    private val FONT_AWESOME = FontFamily(Font(FONT_FILE))
    private val ICON_DEFAULT_SIZE: TextUnit = 12.sp

    data class Offset(val x: Dp, val y: Dp)
    data class Scale(val x: Float, val y: Float)

    enum class Purpose(val shape: Shape) {
        ADD_OWNS(Shape.PLUS),
        ADD_PLAYS(Shape.PLUS),
        ADD_RELATES(Shape.PLUS),
        ALIGN_TEXT_LEFT(Shape.ALIGN_LEFT),
        ATTRIBUTE(Shape.OVAL),
        CLOSE(Shape.XMARK),
        COLLAPSE(Shape.CHEVRONS_UP),
        COMMIT(Shape.CHECK),
        CONNECT_TO_TYPEDB(Shape.SERVER),
        COPY(Shape.OVERLAPPED_PAGES),
        CREATE_DIRECTORY(Shape.FOLDER_PLUS),
        CREATE_FILE(Shape.FILE_PLUS),
        CREATE_SUBTYPE(Shape.SQUARE_PLUS),
        CURSOR(Shape.ARROW_POINTER),
        CUT(Shape.SCISSORS),
        DECREASE_TEXT_SIZE(Shape.ARROWS_MINIMIZE),
        DELETE(Shape.TRASH_CAN),
        EXACT_WORD(Shape.LETTER_W),
        EXPAND(Shape.CHEVRONS_DOWN),
        EXPANDED_ITEM(Shape.CHEVRON_RIGHT),
        EXPORT(Shape.ARROW_UP_RIGHT_FROM_SQUARE),
        FIND(Shape.MAGNIFYING_GLASS),
        FOLDER(Shape.FOLDER_BLANK),
        FONT_CASE(Shape.CAPITAL_A_LOWER_A),
        GRAPH(Shape.DIAGRAM_PROJECT),
        HIDE(Shape.CHEVRON_DOWN),
        INCREASE_TEXT_SIZE(Shape.ARROWS_MAXIMIZE),
        JUMP_TO_BOTTOM(Shape.ARROW_DOWN_TO_LINE),
        JUMP_TO_TOP(Shape.ARROW_UP_TO_LINE),
        MANAGE_DATABASE(Shape.DATABASE),
        MOVE(Shape.FOLDER_ARROW_DOWN),
        NEW_PAGE(Shape.PLUS),
        NEXT_DOWN(Shape.CHEVRON_DOWN),
        NEXT_RIGHT(Shape.CARET_RIGHT),
        ONLINE(Shape.CIRCLE),
        OPEN(Shape.BLOCK_QUOTE),
        OPEN_DIRECTORY(Shape.FOLDER_OPEN),
        OPEN_FILE(Shape.BLOCK_QUOTE),
        OPEN_PROJECT(Shape.FOLDER_OPEN),
        OTHER_FILE(Shape.FILE_WITH_LINES),
        PASTE(Shape.PAGE_OVER_CLIPBOARD),
        PIN(Shape.THUMBTACK),
        PREVIEW(Shape.EYE),
        PREVIOUS_LEFT(Shape.CARET_LEFT),
        PREVIOUS_UP(Shape.CHEVRON_UP),
        PROJECT(Shape.FOLDER_BLANK),
        REFRESH(Shape.ROTATE),
        REGULAR_EXPRESSION(Shape.ASTERISK),
        RELATION(Shape.RHOMBUS),
        REMOVE(Shape.MINUS),
        RENAME(Shape.PEN),
        REPLACE(Shape.OPPOSITE_HORIZONTAL_ARROWS),
        RESET_TEXT_SIZE(Shape.EXPAND),
        RESPONSE_TIME(Shape.CLOCK),
        ROLES(Shape.USER_GROUP),
        ROLLBACK(Shape.ROTATE_LEFT),
        RULES(Shape.DIAGRAM_SUBTASK),
        RUN(Shape.PLAY),
        SAVE(Shape.FLOPPY_DISK),
        SELECT(Shape.CARET_DOWN),
        SELECT_DATABASE(Shape.DATABASE),
        SHOW(Shape.CHEVRON_UP),
        STOP(Shape.BOLT),
        SYMLINK(Shape.LINK_SIMPLE),
        TABLE(Shape.TABLE_CELLS_LARGE),
        THING(Shape.RECTANGLE),
        TICK(Shape.CHECK),
        TYPEQL_FILE(Shape.RECTANGLE_CODE),
        TYPES(Shape.SITEMAP),
        UNEXPANDED_ITEM(Shape.CHEVRON_DOWN),
        USERS(Shape.USERS);

        val unicode: String = shape.unicode
    }

    enum class Shape(
        hexcode: UShort,
        val size: TextUnit = ICON_DEFAULT_SIZE,
        val offset: Offset = Offset(0.dp, 0.dp),
        val rotate: Float = 0f,
        val scale: Scale = Scale(1f, 1f)
    ) {
        // These codes can be found at https://fontawesome.com/v6/icons
        // The icon names in Font Awesome would be the kebab-case version of our names below
        ALIGN_LEFT(0xf036u),
        ARROW_DOWN_TO_LINE(0xf33du),
        ARROW_UP_RIGHT_FROM_SQUARE(0xf08eu),
        ARROW_UP_TO_LINE(0xf341u),
        ARROW_POINTER(0xf245u),
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
        CLOCK(0xf017u),
        OVERLAPPED_PAGES(0xf0c5u),
        SCISSORS(0xf0c4u),
        CURSOR(0xf246u),
        DATABASE(0xf1c0u),
        DELETE_RIGHT(0xe154u),
        DIAGRAM_PROJECT(0xf542u),
        DIAGRAM_SUBTASK(0xe479u),
        EXPAND(0xf065u),
        EYE(0xf06eu),
        FILE_WITH_LINES(0xf15cu),
        FILE_PLUS(0xf319u),
        FLOPPY_DISK(0xf0c7u, 13.sp),
        FOLDER_ARROW_DOWN(0xe053u),
        FOLDER_ARROW_UP(0xe054u),
        FOLDER_OPEN(0xf07cu),
        FOLDER_BLANK(0xe185u),
        FOLDER_PLUS(0xf65eu),
        CAPITAL_A_LOWER_A(0xf866u),
        HEXAGON(0xf312u),
        LETTER_W(0x57u),
        LINK_SIMPLE(0xe1cdu),
        MAGNIFYING_GLASS(0xf002u, rotate = 90f),
        MINUS(0xf068u),
        OVAL(0xf111u, scale = Scale(1f, 0.618f)), // base icon is a Circle
        PAGE_OVER_CLIPBOARD(0xf0eau),
        PEN(0xf304u),
        PLUS(0x2bu),
        PLAY(0xf04bu),
        RECTANGLE(0xf2fau),
        RECTANGLE_CODE(0xe322u),
        RHOMBUS(0xe23bu, rotate = 90f),
        ROTATE(0xf2f1u),
        ROTATE_LEFT(0xf2eau),
        OPPOSITE_HORIZONTAL_ARROWS(0xf362u),
        SERVER(0xf233u),
        SITEMAP(0xf0e8u),
        SQUARE_BOLT(0xe265u, 13.sp),
        SQUARE_PLUS(0xf0feu),
        STOP(0xf04du, 13.sp),
        TABLE_CELLS_LARGE(0xf009u),
        THUMBTACK(0xf08du),
        TIMER(0xe29eu),
        TRASH_CAN(0xf2edu),
        USER(0xf007u),
        USER_GROUP(0xf500u),
        USERS(0xf0c0u),
        XMARK(0xf00du, 13.sp);

        val unicode: String = Char(hexcode).toString()
    }

    @Composable
    fun Render(
        icon: Purpose,
        color: Color = Theme.studio.icon,
        disabledColor: Color? = null,
        size: TextUnit = icon.shape.size,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        Text(
            text = icon.shape.unicode,
            color = if (!enabled && disabledColor != null) disabledColor else fadeable(color, !enabled),
            fontSize = size,
            fontFamily = FONT_AWESOME,
            modifier = modifier.rotate(icon.shape.rotate)
                .offset(icon.shape.offset.x, icon.shape.offset.y)
                .scale(icon.shape.scale.x, icon.shape.scale.y)
        )
    }
}
