/*
 * Copyright (C) 2022 Vaticle
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

import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Label.UNTITLED
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.state.common.util.Message.Companion.UNKNOWN
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.DIRECTORY_HAS_BEEN_MOVED_OUT
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.DIRECTORY_NOT_DELETABLE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_CREATE_DIRECTORY
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_CREATE_FILE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_CREATE_OR_RENAME_DIRECTORY_DUE_TO_DUPLICATE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_CREATE_OR_RENAME_FILE_DUE_TO_DUPLICATE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_MOVE_DIRECTORY
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_MOVE_DIRECTORY_AS_PATH_NOT_EXIST
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_MOVE_DIRECTORY_DUE_TO_DUPLICATE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_MOVE_DIRECTORY_TO_SAME_LOCATION
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_RENAME_DIRECTORY
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PATH_NOT_EXIST
import com.vaticle.typedb.studio.state.common.util.Message.System.Companion.ILLEGAL_CAST
import com.vaticle.typedb.studio.state.common.util.Property
import com.vaticle.typedb.studio.state.common.util.Sentence
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import mu.KotlinLogging

class DirectoryState internal constructor(
    path: Path,
    parent: DirectoryState?,
    projectMgr: ProjectManager
) : PathState(parent, path, Type.DIRECTORY, projectMgr) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override var entries: List<PathState> = emptyList()
    override val isReadable: Boolean get() = path.isReadable()
    override val isWritable: Boolean get() = path.isWritable()
    override val isBulkExpandable: Boolean get() = !isProjectData
    override val isExpandable: Boolean = true

    override fun asDirectory(): DirectoryState {
        return this
    }

    override fun asFile(): FileState {
        throw TypeCastException(ILLEGAL_CAST.message(DirectoryState::class.simpleName, FileState::class.simpleName))
    }

    override fun reloadEntries() {
        if (!path.exists() || !path.isReadable()) {
            projectMgr.notification.userError(LOGGER, PATH_NOT_EXIST, path)
            return
        }
        val new = path.listDirectoryEntries().filter {
            it.isReadable() && !projectMgr.preference.isIgnoredPath(it)
        }.toSet()
        val old = entries.map { it.path }.toSet()
        if (new != old) {
            val deleted = old - new
            val added = new - old
            entries.filter { deleted.contains(it.path) }.forEach { it.closeRecursive() }
            entries = (entries.filter { !deleted.contains(it.path) } + added.map { pathStateOf(it) }).sorted()
        }
    }

    private fun pathStateOf(path: Path): PathState {
        return if (path.isDirectory()) DirectoryState(path, this, projectMgr)
        else FileState(path, this, projectMgr)
    }

    fun nextUntitledDirName(): String {
        var counter = 1
        reloadEntries()
        while (entries.filter { it.name == UNTITLED + counter }.isNotEmpty()) counter++
        return UNTITLED + counter
    }

    fun nextUntitledFileName(): String {
        val format = Property.FileType.TYPEQL.extensions[0]
        var counter = 1
        reloadEntries()
        while (entries.filter { it.name == "$UNTITLED$counter.$format" }.isNotEmpty()) counter++
        return "$UNTITLED$counter.$format"
    }

    internal fun contains(newName: String): Boolean {
        reloadEntries()
        return entries.any { it.name == newName }
    }

    fun initiateCreateDirectory(onSuccess: () -> Unit) {
        projectMgr.createPathDialog.open(this, Type.DIRECTORY, onSuccess)
    }

    fun tryCreateDirectory(name: String): DirectoryState? = tryCreatePath(
        newPath = path.resolve(name),
        failureMessage = FAILED_TO_CREATE_DIRECTORY,
        createFn = { it.createDirectory() }
    )?.asDirectory()?.also {
        projectMgr.createPathDialog.onSuccess?.let { it() }
        updateContentAndCloseDialog(projectMgr.createPathDialog)
    }

    fun initiateCreateFile(onSuccess: () -> Unit) {
        projectMgr.createPathDialog.open(this, Type.FILE, onSuccess)
    }

    fun tryCreateFile(name: String): FileState? = tryCreatePath(
        newPath = path.resolve(name),
        failureMessage = FAILED_TO_CREATE_FILE,
        createFn = { it.createFile() }
    )?.asFile()?.also {
        projectMgr.createPathDialog.onSuccess?.let { it() }
        updateContentAndCloseDialog(projectMgr.createPathDialog)
    }

    override fun initiateRename() {
        projectMgr.renameDirectoryDialog.open(this)
    }

    fun tryRename(newName: String): DirectoryState? {
        val newPath = path.resolveSibling(newName)
        if (newPath == path) return this
        val newDir = if (parent?.contains(newName) == true) {
            projectMgr.notification.userError(LOGGER, FAILED_TO_CREATE_OR_RENAME_DIRECTORY_DUE_TO_DUPLICATE, newPath)
            null
        } else try {
            closeRecursive()
            movePathTo(newPath)
            if (this == projectMgr.current!!.directory) {
                projectMgr.tryOpenProject(newPath)
                projectMgr.current!!.directory
            } else find(newPath)?.asDirectory()
        } catch (e: Exception) {
            projectMgr.notification.systemError(LOGGER, e, FAILED_TO_RENAME_DIRECTORY, newPath, e.message ?: UNKNOWN)
            null
        }
        return newDir?.also { updateContentAndCloseDialog(projectMgr.renameDirectoryDialog) }
    }

    override fun initiateMove() {
        projectMgr.moveDirectoryDialog.open(this)
    }

    fun tryMoveTo(newParent: Path): DirectoryState? {
        val newPath = newParent.resolve(name)
        val newDir = if (newParent == path.parent) {
            projectMgr.notification.userWarning(LOGGER, FAILED_TO_MOVE_DIRECTORY_TO_SAME_LOCATION, newParent)
            null
        } else if (newParent.notExists()) {
            projectMgr.notification.userError(LOGGER, FAILED_TO_MOVE_DIRECTORY_AS_PATH_NOT_EXIST, newParent)
            null
        } else if (newPath.exists()) {
            projectMgr.notification.userError(LOGGER, FAILED_TO_MOVE_DIRECTORY_DUE_TO_DUPLICATE, newParent)
            null
        } else try {
            closeRecursive()
            movePathTo(newPath)
            find(newPath)?.asDirectory()
        } catch (e: Exception) {
            projectMgr.notification.systemError(LOGGER, e, FAILED_TO_MOVE_DIRECTORY, newParent, e.message ?: UNKNOWN)
            null
        }
        if (!newParent.startsWith(projectMgr.current!!.path)) {
            projectMgr.notification.userWarning(LOGGER, DIRECTORY_HAS_BEEN_MOVED_OUT, newParent)
        }
        return newDir?.also { updateContentAndCloseDialog(projectMgr.moveDirectoryDialog) }
    }

    override fun initiateDelete(onSuccess: () -> Unit) {
        projectMgr.confirmation.submit(
            title = Label.CONFIRM_DIRECTORY_DELETION,
            message = Sentence.CONFIRM_DIRECTORY_DELETION,
            onConfirm = { delete(); onSuccess() }
        )
    }

    private fun tryCreatePath(newPath: Path, failureMessage: Message.Project, createFn: (Path) -> Unit): PathState? {
        return if (newPath.exists()) {
            projectMgr.notification.userError(LOGGER, FAILED_TO_CREATE_OR_RENAME_FILE_DUE_TO_DUPLICATE, newPath)
            null
        } else try {
            createFn(newPath)
            reloadEntries()
            entries.first { it.name == newPath.name }
        } catch (e: Exception) {
            projectMgr.notification.systemError(LOGGER, e, failureMessage, newPath, e.message ?: UNKNOWN)
            null
        }
    }

    fun remove(path: PathState) {
        entries = entries.filter { it != path }
    }

    override fun delete() {
        try {
            reloadEntries()
            entries.filter { it.isDirectory }.forEach { it.delete() }
            entries.filter { it.isFile }.forEach { it.delete() }
            entries = emptyList()
            path.deleteExisting()
            parent?.remove(this)
        } catch (e: Exception) {
            projectMgr.notification.systemError(LOGGER, e, DIRECTORY_NOT_DELETABLE, path.name)
        }
    }

    override fun close() {}

    override fun closeRecursive() = entries.forEach { it.closeRecursive() }
}
