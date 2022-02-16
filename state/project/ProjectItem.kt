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

import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FAILED_TO_CREATE_OR_RENAME_FILE_TO_DUPLICATE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FAILED_TO_RENAME_FILE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FAILED_TO_SAVE_FILE
import com.vaticle.typedb.studio.state.common.Navigable
import com.vaticle.typedb.studio.state.common.Settings
import com.vaticle.typedb.studio.state.notification.NotificationManager
import java.nio.file.Path
import java.util.Objects
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.moveTo
import kotlin.io.path.readSymbolicLink
import mu.KotlinLogging

sealed class ProjectItem(
    val projectItemType: Type,
    val path: Path,
    override val parent: Directory?,
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
    val isRoot get() = parent == null

    val absolutePath: Path = path.toAbsolutePath()
    val isSymbolicLink: Boolean = path.isSymbolicLink()
    val isDirectory: Boolean = projectItemType == Type.DIRECTORY
    val isFile: Boolean = projectItemType == Type.FILE
    val isProjectData: Boolean get() = if (this == projectMgr.dataDir) true else parent?.isProjectData ?: false

    abstract val isReadable: Boolean
    abstract val isWritable: Boolean
    abstract fun asDirectory(): Directory
    abstract fun asFile(): File
    abstract fun close()
    abstract fun delete()

    internal fun tryRename(newName: String): Boolean {
        val newPath = path.resolveSibling(newName)
        return if (parent?.contains(newName) == true) {
            notificationMgr.userError(LOGGER, FAILED_TO_CREATE_OR_RENAME_FILE_TO_DUPLICATE, newPath)
            false
        } else try {
            path.moveTo(newPath)
            true
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_RENAME_FILE, newPath)
            false
        }
    }

    fun trySaveTo(newPath: Path, overwrite: Boolean): Boolean {
        return try {
            close()
            path.moveTo(newPath, overwrite)
            true
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_SAVE_FILE, newPath)
            false
        }
    }

    override fun toString(): String {
        return path.toString()
    }

    override fun compareTo(other: Navigable.Item<ProjectItem>): Int {
        other as ProjectItem
        return if (this.projectItemType == other.projectItemType) this.path.compareTo(other.path)
        else this.projectItemType.index.compareTo(other.projectItemType.index)
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
