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

import java.io.File

object Paths {
    val SAMPLE_DATA_PATH = File("test/data/sample_file_structure").absolutePath
    val TQL_DATA_PATH = File("test/data").absolutePath

    const val QUERY_FILE_NAME = "query_string.tql"
    const val DATA_FILE_NAME = "data_string.tql"
    const val SCHEMA_FILE_NAME = "schema_string.tql"
}