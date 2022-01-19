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

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.studio.state.common.Message.System.Companion.ILLEGAL_CAST
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.page.Editable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension

class File(path: Path, parent: Directory) : ProjectItem(Type.FILE, path, parent), Editable {

    val content: SnapshotStateList<String> = mutableStateListOf()
    val extension: String = this.path.extension
    val isTypeQL: Boolean = Property.File.TYPEQL.extensions.contains(extension)

    override fun asDirectory(): Directory {
        throw TypeCastException(ILLEGAL_CAST.message(File::class.simpleName, Directory::class.simpleName))
    }

    override fun asFile(): File {
        return this
    }

    override fun load() {
        content.addAll(Files.readAllLines(path))
    }

    fun save() {
        Files.write(path, content)
    }
}
