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
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchService
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink

class Directory(val path: Path) {

    val name: String = path.fileName.toString()
    val absolutePath: Path = path.toAbsolutePath()
    val isSymbolicLink: Boolean = path.isSymbolicLink()
    var isExpanded: Boolean by mutableStateOf(false); private set
    var directories: List<Directory> by mutableStateOf(emptyList())
    var files: List<File> by mutableStateOf(emptyList())

    fun watchService(): WatchService {
        val watchService = FileSystems.getDefault().newWatchService()
        path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        return watchService
    }

    fun expand() {
        loadEntries()
        isExpanded = true
    }

    fun collapse() {
        isExpanded = false
    }

    fun loadEntries() {
        val dirList: MutableList<Directory> = mutableListOf()
        val fileList: MutableList<File> = mutableListOf()
        path.forEachDirectoryEntry { entry ->
            if (entry.isDirectory()) dirList.add(Directory((entry)))
            else if (entry.isRegularFile()) fileList.add(File(entry))
        }
        directories = dirList.sortedBy { it.name }
        files = fileList.sortedBy { it.name }
    }
}
