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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.state.common.CatalogItem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchService
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

class Directory internal constructor(dirPath: Path, projectPath: Path) :
    CatalogItem.Expandable<ProjectItem>, ProjectItem(dirPath, projectPath) {

    internal constructor(path: Path) : this(path, path)

    override val isExpandable: Boolean = true
    override var isExpanded: Boolean by mutableStateOf(false); private set
    override var entries: List<ProjectItem> by mutableStateOf(emptyList())
    override val isDirectory: Boolean = true
    override val isFile: Boolean = false

    var directories: List<Directory> by mutableStateOf(emptyList())
    var files: List<File> by mutableStateOf(emptyList())

    override fun toggle() {
        isExpanded = !isExpanded
        if (isExpanded) loadEntries()
    }

    override fun asDirectory(): Directory {
        return this
    }

    override fun asFile(): File {
        throw TypeCastException("Invalid casting of Directory to File") // TODO: generalise
    }

    fun watchService(): WatchService {
        val watchService = FileSystems.getDefault().newWatchService()
        path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        return watchService
    }

    fun loadEntries() {
        val dirList: MutableList<Directory> = mutableListOf()
        val fileList: MutableList<File> = mutableListOf()
        path.forEachDirectoryEntry { entry ->
            if (entry.isDirectory()) dirList.add(Directory(entry, projectPath))
            else if (entry.isRegularFile()) fileList.add(File(entry, projectPath))
        }
        directories = dirList.sortedBy { it.name }
        files = fileList.sortedBy { it.name }
        entries = directories + files
    }
}
