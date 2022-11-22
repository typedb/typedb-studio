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

enum class Icon(private val shape: Shape) {
    ADD(Shape.PLUS),
    ATTRIBUTE(Shape.OVAL),
    CLOSE(Shape.XMARK),
    COLLAPSE(Shape.CHEVRONS_UP),
    COMMIT(Shape.CHECK),
    CONNECT_TO_TYPEDB(Shape.SERVER),
    COPY(Shape.OVERLAPPED_PAGES),
    CURSOR(Shape.ARROW_POINTER),
    CUT(Shape.SCISSORS),
    DATABASE(Shape.DATABASE),
    DELETE(Shape.TRASH_CAN),
    DIRECTORY_CREATE(Shape.FOLDER_PLUS),
    DROPDOWN_SELECT(Shape.CARET_DOWN),
    EXACT_WORD(Shape.LETTER_W),
    EXPAND(Shape.CHEVRONS_DOWN),
    EXPORT(Shape.ARROW_UP_RIGHT_FROM_SQUARE),
    FILE_CREATE(Shape.FILE_PLUS),
    FILE_OPEN(Shape.BLOCK_QUOTE),
    FILE_OTHER(Shape.FILE_WITH_LINES),
    FILE_TYPEQL(Shape.CODE_FILE),
    FIND(Shape.MAGNIFYING_GLASS),
    FOLDER(Shape.FOLDER_BLANK),
    FOLDER_OPEN(Shape.FOLDER_OPEN),
    FONT_CASE(Shape.CAPITAL_A_LOWER_A),
    GO_TO(Shape.ARROW_UP_RIGHT),
    GRAPH(Shape.DIAGRAM_PROJECT),
    HIDE(Shape.CHEVRON_DOWN),
    ITEM_COLLAPSED(Shape.CHEVRON_RIGHT),
    ITEM_EXPANDED(Shape.CHEVRON_DOWN),
    JUMP_TO_BOTTOM(Shape.ARROW_DOWN_TO_LINE),
    JUMP_TO_TOP(Shape.ARROW_UP_TO_LINE),
    MOVE(Shape.FOLDER_ARROW_DOWN),
    NEXT_DOWN(Shape.CHEVRON_DOWN),
    NEXT_RIGHT(Shape.CARET_RIGHT),
    ONLINE(Shape.CIRCLE),
    OPEN(Shape.BLOCK_QUOTE),
    PASTE(Shape.PAGE_OVER_CLIPBOARD),
    PIN(Shape.THUMBTACK),
    PREFERENCES(Shape.GEAR),
    PREVIEW(Shape.EYE),
    PREVIOUS_LEFT(Shape.CARET_LEFT),
    PREVIOUS_UP(Shape.CHEVRON_UP),
    REFRESH(Shape.ROTATE),
    REGULAR_EXPRESSION(Shape.ASTERISK),
    RELATION(Shape.DIAMOND_SIDEWAYS),
    REMOVE(Shape.MINUS),
    RENAME(Shape.PEN),
    REPLACE(Shape.ARROWS_OPPOSITE_HORIZONTAL),
    RESPONSE_TIME(Shape.CLOCK),
    ROLES(Shape.USER_GROUP),
    ROLLBACK(Shape.ROTATE_LEFT),
    RULES(Shape.DIAGRAM_SUBTASK),
    RUN(Shape.PLAY),
    SAVE(Shape.FLOPPY_DISK),
    SHOW(Shape.CHEVRON_UP),
    STOP(Shape.BOLT),
    SUBTYPE_CREATE(Shape.SQUARE_PLUS),
    SYMLINK(Shape.LINK_SIMPLE),
    TABLE(Shape.TABLE_CELLS_LARGE),
    TEXT_LEFT_ALIGN(Shape.ALIGN_LEFT),
    TEXT_SIZE_DECREASE(Shape.ARROWS_MINIMIZE),
    TEXT_SIZE_INCREASE(Shape.ARROWS_MAXIMIZE),
    TEXT_SIZE_RESET(Shape.EXPAND),
    THING(Shape.RECTANGLE),
    TICK(Shape.CHECK),
    TYPES(Shape.SITEMAP),
    USERS(Shape.USERS),
    ALERT(Shape.CIRCLE_EXCLAMATION);

    val unicode: String = shape.unicode

    companion object {
        private val FONT_FILE = "resources/icons/fontawesome/font-awesome-6-pro-solid-900.otf"
        private val FONT_AWESOME = FontFamily(Font(FONT_FILE))

        @Composable
        fun Render(
            icon: Icon,
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

    data class Offset(val x: Dp, val y: Dp)
    data class Scale(val x: Float, val y: Float)

    enum class Shape(
        hexcode: UShort,
        val size: TextUnit = Shape.DEFAULT_SIZE.sp,
        val offset: Offset = Offset(0.dp, 0.dp),
        val rotate: Float = 0f,
        val scale: Scale = Scale(1f, 1f)
    ) {
        // These codes can be found at https://fontawesome.com/v6/icons
        // The icon names in Font Awesome would be the kebab-case version of our names below
        ALIGN_LEFT(0xf036u),
        ARROWS_MAXIMIZE(0xf31du),
        ARROWS_MINIMIZE(0xe0a5u),
        ARROWS_OPPOSITE_HORIZONTAL(0xf362u),
        ARROW_DOWN_TO_LINE(0xf33du),
        ARROW_POINTER(0xf245u),
        ARROW_UP_RIGHT_FROM_SQUARE(0xf08eu),
        ARROW_UP_RIGHT(0xe09fu),
        ARROW_UP_TO_LINE(0xf341u),
        ASTERISK(0x2au),
        BLOCK_QUOTE(0xe0b5u),
        BOLT(0xf0e7u),
        CAPITAL_A_LOWER_A(0xf866u),
        CARET_DOWN(0xf0d7u),
        CARET_LEFT(0xf0d9u),
        CARET_RIGHT(0xf0dau),
        CARET_UP(0xf0d8u),
        CHART_NETWORK(0xf78au),
        CHECK(0xf00cu),
        CHEVRONS_DOWN(0xf322u),
        CHEVRONS_LEFT(0xf323u),
        CHEVRONS_RIGHT(0xf324u),
        CHEVRONS_UP(0xf325u),
        CHEVRON_DOWN(0xf078u),
        CHEVRON_LEFT(0xf053u),
        CHEVRON_RIGHT(0xf054u),
        CHEVRON_UP(0xf077u),
        CIRCLE(0xf111u),
        CIRCLE_EXCLAMATION(0xf06au),
        CLOCK(0xf017u),
        CODE_FILE(0xe322u),
        CURSOR(0xf246u),
        DATABASE(0xf1c0u),
        DELETE_RIGHT(0xe154u),
        DIAGRAM_PROJECT(0xf542u),
        DIAGRAM_SUBTASK(0xe479u),
        DIAMOND_SIDEWAYS(0xe23bu, rotate = 90f),
        EXPAND(0xf065u),
        EYE(0xf06eu),
        FILE_PLUS(0xf319u),
        FILE_WITH_LINES(0xf15cu),
        FLOPPY_DISK(0xf0c7u, size = 13.sp),
        FOLDER_ARROW_DOWN(0xe053u),
        FOLDER_ARROW_UP(0xe054u),
        FOLDER_BLANK(0xe185u),
        FOLDER_OPEN(0xf07cu),
        FOLDER_PLUS(0xf65eu),
        FONT_CASE(0xf866u),
        GEAR(0xf013u),
        HEXAGON(0xf312u),
        LETTER_W(0x57u),
        LINK_SIMPLE(0xe1cdu),
        MAGNIFYING_GLASS(0xf002u, rotate = 90f),
        MINUS(0xf068u),
        OVAL(0xf111u, scale = Scale(1f, 0.618f)), // base icon is a Circle
        OVERLAPPED_PAGES(0xf0c5u),
        PAGE_OVER_CLIPBOARD(0xf0eau),
        PEN(0xf304u),
        PLAY(0xf04bu),
        PLUS(0x2bu),
        RECTANGLE(0xf2fau),
        ROTATE(0xf2f1u),
        ROTATE_LEFT(0xf2eau),
        SCISSORS(0xf0c4u),
        SERVER(0xf233u),
        SITEMAP(0xf0e8u),
        SQUARE_BOLT(0xe265u, size = 13.sp),
        SQUARE_PLUS(0xf0feu),
        STOP(0xf04du, size = 13.sp),
        TABLE_CELLS_LARGE(0xf009u),
        THUMBTACK(0xf08du),
        TIMER(0xe29eu),
        TRASH_CAN(0xf2edu),
        USER(0xf007u),
        USERS(0xf0c0u),
        USER_GROUP(0xf500u),
        XMARK(0xf00du, size = 13.sp);

        val unicode: String = Char(hexcode).toString()

        companion object {
            private const val DEFAULT_SIZE = 12
        }
    }
}
