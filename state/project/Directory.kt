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
import kotlin.io.path.listDirectoryEntries

class Directory internal constructor(dirPath: Path, projectPath: Path) :
    CatalogItem.Expandable<ProjectItem>, ProjectItem(dirPath, projectPath) {

    internal constructor(path: Path) : this(path, path)

    override val isExpandable: Boolean = true
    override var isExpanded: Boolean by mutableStateOf(false); private set
    override var entries: List<ProjectItem> by mutableStateOf(emptyList())
    override val isDirectory: Boolean = true
    override val isFile: Boolean = false

//    var directories: List<Directory> by mutableStateOf(emptyList())
//    var files: List<File> by mutableStateOf(emptyList())

    init {
//        if (isExpanded) reloadEntries()
    }

    override fun toggle() {
        isExpanded = !isExpanded
        if (isExpanded) reloadEntries()
    }

    override fun asDirectory(): Directory {
        return this
    }

    override fun asFile(): File {
        throw TypeCastException("Invalid casting of Directory to File") // TODO: generalise
    }

    internal fun watchService(): WatchService {
        val watchService = FileSystems.getDefault().newWatchService()
        path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        return watchService
    }

    internal fun reloadEntries() {
        val newPaths = path.listDirectoryEntries()
        val updatedDirs = updatedDirs(newPaths.filter { it.isDirectory() }.toSet())
        val updatedFiles = updatedFiles(newPaths.filter { it.isRegularFile() }.toSet())
        entries = updatedDirs.sortedBy { it.name } + updatedFiles.sortedBy { it.name }
        entries.filterIsInstance<Directory>().filter { it.isExpanded }.forEach { it.reloadEntries() }
    }

    private fun updatedDirs(new: Set<Path>): List<Directory> {
        val old = entries.filter { it.isDirectory }.map { it.path }.toSet()
        val deleted = old - new
        val added = new - old
        return entries.filterIsInstance<Directory>().filter { !(deleted).contains(it.path) } +
                (added).map { Directory(it, projectPath) }
    }

    private fun updatedFiles(new: Set<Path>): List<File> {
        val old = entries.filter { it.isFile }.map { it.path }.toSet()
        val deleted = old - new
        val added = new - old
        return entries.filterIsInstance<File>().filter { !(deleted).contains(it.path) } +
                (added).map { File(it, projectPath) }
    }
}
