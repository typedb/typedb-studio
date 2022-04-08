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

import com.vaticle.typedb.studio.state.common.Navigable
import com.vaticle.typedb.studio.state.common.Settings
import com.vaticle.typedb.studio.state.notification.NotificationManager
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

sealed class ProjectItem constructor(
    val projectItemType: Type,
    val path: Path,
    final override val parent: Directory?,
    val settings: Settings,
    val projectMgr: ProjectManager,
    val notificationMgr: NotificationManager
) : Navigable.Item<ProjectItem> {

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
    val isDirectory: Boolean = projectItemType == Type.DIRECTORY
    val isFile: Boolean = projectItemType == Type.FILE
    val isProjectData: Boolean by lazy { if (this == projectMgr.dataDir) true else parent?.isProjectData ?: false }

    abstract val isReadable: Boolean
    abstract val isWritable: Boolean
    abstract fun asDirectory(): Directory
    abstract fun asFile(): File
    abstract fun close()
    abstract fun delete()

    internal fun movePathTo(newPath: Path, overwrite: Boolean = false) {
        path.moveTo(newPath, overwrite)
        FileChannel.open(newPath, StandardOpenOption.WRITE).lock().release() // This waits till file is ready
    }

    internal fun find(newPath: Path): ProjectItem? {
        if (!newPath.startsWith(projectMgr.current!!.path)) return null
        var relPath = newPath.relativeTo(projectMgr.current!!.path)
        var dir: Directory = projectMgr.current!!.directory
        while (relPath.nameCount > 1) {
            dir.reloadEntries()
            dir = dir.entries.first { it.name == relPath.first().name }.asDirectory()
            relPath = relPath.relativeTo(relPath.first())
        }
        dir.reloadEntries()
        return dir.entries.first { it.name == relPath.first().name }
    }

    override fun toString(): String {
        return path.toString()
    }

    override fun compareTo(other: Navigable.Item<ProjectItem>): Int {
        other as ProjectItem
        return if (this.projectItemType == other.projectItemType) {
            this.path.toString().compareTo(other.path.toString(), ignoreCase = true)
        } else this.projectItemType.index.compareTo(other.projectItemType.index)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProjectItem
        return path == other.path
    }

    override fun hashCode(): Int {
        return hash
    }
}
