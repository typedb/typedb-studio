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

package com.vaticle.typedb.studio.test.integration.common

import com.vaticle.typedb.studio.framework.material.Icon
import java.io.File

object Data {
    val SAVE_ICON_STRING = Icon.Code.FLOPPY_DISK.unicode
    val PLUS_ICON_STRING = Icon.Code.PLUS.unicode
    val PLAY_ICON_STRING = Icon.Code.PLAY.unicode
    val CHECK_ICON_STRING = Icon.Code.CHECK.unicode
    val ROLLBACK_ICON_STRING = Icon.Code.ROTATE_LEFT.unicode
    val CHEVRON_UP_ICON_STRING = Icon.Code.CHEVRON_UP.unicode
    val DOUBLE_CHEVRON_DOWN_ICON_STRING = Icon.Code.CHEVRONS_DOWN.unicode
    val DOUBLE_CHEVRON_UP_ICON_STRING = Icon.Code.CHEVRONS_UP.unicode

    val SAMPLE_DATA_PATH = File("test/data/sample_file_structure").absolutePath
    val TQL_DATA_PATH = File("test/data").absolutePath

    const val QUERY_FILE_NAME = "query_string.tql"
    const val DATA_FILE_NAME = "data_string.tql"
    const val SCHEMA_FILE_NAME = "schema_string.tql"
}