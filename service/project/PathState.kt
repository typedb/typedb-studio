/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.project

import com.typedb.studio.service.common.util.DialogState
import com.typedb.studio.service.page.Navigable
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.WRITE
import java.util.Objects
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.readSymbolicLink
import kotlin.io.path.relativeTo

sealed class PathState constructor(
    final override val parent: DirectoryState?,
    val path: Path,
    val type: Type,
    val projectSrv: ProjectService,
) : Navigable<PathState> {

    enum class Type(val index: Int) {
        DIRECTORY(0),
        FILE(1);
    }

    private val hash = Objects.hash(path)
    override val name = path.fileName.toString()
    override val info = if (path.isSymbolicLink()) "→ " + path.readSymbolicLink().toString() else null
    val isRoot = parent == null

    val isSymbolicLink: Boolean = path.isSymbolicLink()
    val isDirectory: Boolean = type == Type.DIRECTORY
    val isFile: Boolean = type == Type.FILE
    val isProjectData: Boolean by lazy { if (this == projectSrv.dataDir) true else parent?.isProjectData ?: false }

    abstract val isReadable: Boolean
    abstract val isWritable: Boolean
    abstract fun asDirectory(): DirectoryState
    abstract fun asFile(): FileState
    abstract fun initiateRename()
    abstract fun initiateMove()
    abstract fun initiateDelete(onSuccess: () -> Unit)
    abstract fun close()
    abstract fun closeRecursive()
    abstract fun tryDelete()

    internal fun movePathTo(newPath: Path, overwrite: Boolean = false) {
        path.moveTo(newPath, overwrite)
        if (newPath.isRegularFile()) FileChannel.open(newPath, WRITE).lock().release() // This waits till file is ready
    }

    internal fun find(newPath: Path): PathState? {
        if (!newPath.startsWith(projectSrv.current!!.path)) return null
        var relPath = newPath.relativeTo(projectSrv.current!!.path)
        var dir: DirectoryState = projectSrv.current!!.directory
        while (relPath.nameCount > 1) {
            dir.reloadEntries()
            dir = dir.entries.first { it.name == relPath.first().name }.asDirectory()
            relPath = relPath.relativeTo(relPath.first())
        }
        dir.reloadEntries()
        return dir.entries.first { it.name == relPath.first().name }
    }

    protected fun updateContentAndCloseDialog(dialog: DialogState) {
        projectSrv.execContentChange()
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
