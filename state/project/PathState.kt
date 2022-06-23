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

import com.vaticle.typedb.studio.state.app.DialogManager
import com.vaticle.typedb.studio.state.page.Navigable
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Objects
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.readSymbolicLink
import kotlin.io.path.relativeTo
import mu.KotlinLogging

sealed class PathState constructor(
    final override val parent: DirectoryState?,
    val path: Path,
    val type: Type,
    val projectMgr: ProjectManager,
) : Navigable<PathState> {

    enum class Type(val index: Int) {
        DIRECTORY(0),
        FILE(1);
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    private val hash = Objects.hash(path)
    override val name = path.fileName.toString()
    override val info = if (path.isSymbolicLink()) "→ " + path.readSymbolicLink().toString() else null
    val isRoot = parent == null

    val isSymbolicLink: Boolean = path.isSymbolicLink()
    val isDirectory: Boolean = type == Type.DIRECTORY
    val isFile: Boolean = type == Type.FILE
    val isProjectData: Boolean by lazy { if (this == projectMgr.dataDir) true else parent?.isProjectData ?: false }

    abstract val isReadable: Boolean
    abstract val isWritable: Boolean
    abstract fun asDirectory(): DirectoryState
    abstract fun asFile(): FileState
    abstract fun initiateRename()
    abstract fun initiateMove()
    abstract fun initiateDelete(onSuccess: () -> Unit)
    abstract fun close()
    abstract fun closeRecursive()
    abstract fun delete()

    internal fun movePathTo(newPath: Path, overwrite: Boolean = false) {
        path.moveTo(newPath, overwrite)
        FileChannel.open(newPath, StandardOpenOption.WRITE).lock().release() // This waits till file is ready
    }

    internal fun find(newPath: Path): PathState? {
        if (!newPath.startsWith(projectMgr.current!!.path)) return null
        var relPath = newPath.relativeTo(projectMgr.current!!.path)
        var dir: DirectoryState = projectMgr.current!!.directory
        while (relPath.nameCount > 1) {
            dir.reloadEntries()
            dir = dir.entries.first { it.name == relPath.first().name }.asDirectory()
            relPath = relPath.relativeTo(relPath.first())
        }
        dir.reloadEntries()
        return dir.entries.first { it.name == relPath.first().name }
    }

    protected fun updateContentAndCloseDialog(dialog: DialogManager) {
        projectMgr.onContentChange?.let { fn -> fn() }
        dialog.close()
    }

    override fun toString(): String {
        return path.toString()
    }

    override fun compareTo(other: Navigable<PathState>): Int {
        other as PathState
        return if (this.type == other.type) {
            this.path.toString().compareTo(other.path.toString(), ignoreCase = true)
        } else this.type.index.compareTo(other.type.index)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PathState
        return path == other.path
    }

    override fun hashCode(): Int {
        return hash
    }
}
