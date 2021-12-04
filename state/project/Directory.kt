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
import com.vaticle.typedb.studio.state.common.Catalog
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

class Directory internal constructor(path: Path) : Catalog.Item.Expandable<ProjectItem>, ProjectItem(path) {

    override val isExpandable: Boolean = true
    override var isExpanded: Boolean by mutableStateOf(false); private set
    override var entries: List<ProjectItem> by mutableStateOf(emptyList())
    override val isDirectory: Boolean = true
    override val isFile: Boolean = false

    override fun asDirectory(): Directory {
        return this
    }

    override fun asFile(): File {
        throw TypeCastException("Invalid casting of Directory to File") // TODO: generalise
    }

    override fun toggle() {
        toggle(!isExpanded)
    }

    override fun expand() = expandAndReloadEntries()

    override fun collapse() {
        toggle(false)
    }

    internal fun expandAndReloadEntries() {
        toggle(true)
    }

    private fun toggle(isExpanded: Boolean) {
        this.isExpanded = isExpanded
        if (isExpanded) reloadEntries()
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
                (added).map { Directory(it) }
    }

    private fun updatedFiles(new: Set<Path>): List<File> {
        val old = entries.filter { it.isFile }.map { it.path }.toSet()
        val deleted = old - new
        val added = new - old
        return entries.filterIsInstance<File>().filter { !(deleted).contains(it.path) } + (added).map { File(it) }
    }

    internal fun checkForUpdate() {
        if (!isExpanded) return
        val new = path.listDirectoryEntries().toSet()
        val old = entries.map { it.path }.toSet()
        if (new != old) reloadEntries()
        entries.filterIsInstance<Directory>().filter { it.isExpanded }.forEach { it.checkForUpdate() }
    }
}
