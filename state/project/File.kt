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

import com.vaticle.typedb.studio.state.common.Catalog
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.page.Page
import java.nio.file.Path
import kotlin.io.path.extension

class File(path: Path) : Catalog.Item<ProjectItem>, ProjectItem(path), Page {

    override val isExpandable: Boolean = false
    override val isDirectory: Boolean = false
    override val isFile: Boolean = true

    val type: String = this.path.extension
    val isTypeQL: Boolean = Property.File.TYPEQL.extensions.contains(type)

    override fun asDirectory(): Directory {
        throw TypeCastException("Invalid casting of File to Directory") // TODO: generalise
    }

    override fun asFile(): File {
        return this
    }

    override fun toString(): String {
        return path.toString()
    }
}