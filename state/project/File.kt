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

package com.vaticle.typedb.studio.state.project

import com.vaticle.typedb.studio.state.common.Property
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isSymbolicLink

class File(val path: Path) {

    val name: String = path.fileName.toString()
    val absolutePath = path.toAbsolutePath()
    val isSymbolicLink: Boolean = path.isSymbolicLink()
    val type: String = path.extension
    val isTypeQL: Boolean = Property.File.TYPEQL.extensions.contains(type)
}