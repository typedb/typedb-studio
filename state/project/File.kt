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
import com.vaticle.typedb.studio.state.common.Message
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FAILED_TO_CREATE_OR_RENAME_FILE_DUE_TO_DUPLICATE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FAILED_TO_RENAME_FILE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FAILED_TO_SAVE_FILE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FILE_NOT_DELETABLE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FILE_NOT_READABLE
import com.vaticle.typedb.studio.state.common.Message.System.Companion.ILLEGAL_CAST
import com.vaticle.typedb.studio.state.common.Property.FileType
import com.vaticle.typedb.studio.state.common.Property.FileType.TYPEQL
import com.vaticle.typedb.studio.state.common.Property.FileType.UNKNOWN
import com.vaticle.typedb.studio.state.common.Settings
import com.vaticle.typedb.studio.state.notification.NotificationManager
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.state.runner.RunnerManager
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

class File internal constructor(
    path: Path,
    parent: Directory,
    settings: Settings,
    projectMgr: ProjectManager,
    notificationMgr: NotificationManager
) : ProjectItem(Type.FILE, path, parent, settings, projectMgr, notificationMgr), Resource.Runnable {

    @OptIn(ExperimentalTime::class)
    companion object {
        private val LIVE_UPDATE_REFRESH_RATE: Duration = Duration.seconds(1)
        private val LOGGER = KotlinLogging.logger {}
    }

    private class Callbacks {

        val onDiskChangeContent = LinkedBlockingDeque<(File) -> Unit>()
        val onDiskChangePermission = LinkedBlockingDeque<(File) -> Unit>()
        val onReopen = LinkedBlockingDeque<(File) -> Unit>()
        val beforeRun = LinkedBlockingDeque<(File) -> Unit>()
        val beforeSave = LinkedBlockingDeque<(File) -> Unit>()
        val beforeClose = LinkedBlockingDeque<(File) -> Unit>()
        val onClose = LinkedBlockingDeque<(File) -> Unit>()

        fun clone(): Callbacks {
            val newCallbacks = Callbacks()
            newCallbacks.onDiskChangeContent.addAll(this.onDiskChangeContent)
            newCallbacks.onDiskChangePermission.addAll(this.onDiskChangePermission)
            newCallbacks.onReopen.addAll(this.onReopen)
            newCallbacks.beforeRun.addAll(this.beforeRun)
            newCallbacks.beforeSave.addAll(this.beforeSave)
            newCallbacks.beforeClose.addAll(this.beforeClose)
            newCallbacks.onClose.addAll(this.onClose)
            return newCallbacks
        }

        fun clear() {
            onDiskChangeContent.clear()
            onDiskChangePermission.clear()
            onReopen.clear()
            beforeRun.clear()
            beforeSave.clear()
            beforeClose.clear()
            onClose.clear()
        }
    }

    val extension: String = this.path.extension
    val fileType: FileType = when {
        TYPEQL.extensions.contains(extension) -> TYPEQL
        else -> UNKNOWN
    }
    val isTypeQL: Boolean = fileType == TYPEQL
    val isTextFile: Boolean = checkIsTextFile()

    private var callbacks = Callbacks()
    private var content: List<String> by mutableStateOf(listOf())
    private var watchFileSystem = AtomicBoolean(false)
    private var lastModified = AtomicLong(path.toFile().lastModified())
    private var isOpenAtomic: AtomicBoolean = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    override val fullName: String = computeFullName(path, projectMgr)
    override val runContent: String get() {
        callbacks.beforeRun.forEach { it(this) }
        return content.joinToString("\n")
    }
    override var runner: RunnerManager = RunnerManager()
    override val isOpen: Boolean get() = isOpenAtomic.get()
    override val isRunnable: Boolean = isTypeQL
    override val isEmpty: Boolean get() = content.size == 1 && content[0].isBlank()
    override val isUnsavedResource: Boolean get() = parent == projectMgr.unsavedFilesDir
    override var hasUnsavedChanges: Boolean by mutableStateOf(false)
    override val isReadable: Boolean get() = isReadableAtomic.get()
    override val isWritable: Boolean get() = isWritableAtomic.get()
    override val isExpandable: Boolean = false
    override val isBulkExpandable: Boolean = false
    override val entries: List<ProjectItem> = listOf()
    private var isReadableAtomic = AtomicBoolean(path.isReadable())
    private var isWritableAtomic = AtomicBoolean(path.isWritable())

    override fun reloadEntries() {}

    private fun checkIsTextFile(): Boolean {
        val type = Files.probeContentType(path)
        return type != null && type.startsWith("text")
    }

    private fun computeFullName(path: Path, projectMgr: ProjectManager): String {
        return if (isUnsavedResource) projectMgr.current!!.directory.name + " (unsaved: " + name + ")"
        else path.relativeTo(projectMgr.current!!.directory.path.parent).toString()
    }

    override fun asDirectory(): Directory {
        throw TypeCastException(ILLEGAL_CAST.message(File::class.simpleName, Directory::class.simpleName))
    }

    override fun asFile(): File {
        return this
    }

    override fun tryOpen(): Boolean {
        if (!path.isReadable()) {
            notificationMgr.userError(LOGGER, FILE_NOT_READABLE, path)
            return false
        }
        return try {
            readContent()
            isOpenAtomic.set(true)
            callbacks.onReopen.forEach { it(this) }
            true
        } catch (e: Exception) { // TODO: specialise error message to actual error, e.g. read/write permissions
            notificationMgr.userError(LOGGER, FILE_NOT_READABLE, path)
            false
        }
    }

    internal fun tryRename(newName: String): File? {
        val newPath = path.resolveSibling(newName)
        return if (parent!!.contains(newName)) {
            notificationMgr.userError(LOGGER, FAILED_TO_CREATE_OR_RENAME_FILE_DUE_TO_DUPLICATE, newPath)
            null
        } else try {
            val clonedRunner = runner.clone()
            val clonedCallbacks = callbacks.clone()
            close()
            movePathTo(newPath)
            find(newPath)?.asFile()?.also {
                it.runner = clonedRunner
                it.callbacks = clonedCallbacks
            }
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_RENAME_FILE, newPath)
            null
        }
    }

    internal fun trySaveTo(newPath: Path, overwrite: Boolean): File? {
        return try {
            val clonedRunner = runner.clone()
            val clonedCallbacks = callbacks.clone()
            close()
            if (overwrite && newPath.exists()) find(newPath)?.delete()
            movePathTo(newPath, overwrite)
            find(newPath)?.asFile()?.also {
                it.runner = clonedRunner
                it.callbacks = clonedCallbacks
            }
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_SAVE_FILE, newPath)
            null
        }
    }

    fun isChanged() {
        hasUnsavedChanges = true
    }

    fun readContent(): List<String> {
        val loadedContent = if (isTextFile) loadTextFileLines() else loadBinaryFileLines()
        content = loadedContent.ifEmpty { listOf("") }
        return content
    }

    private fun loadTextFileLines(): List<String> {
        val content = Files.readAllLines(path)
        if (content.isEmpty()) content.add("")
        return content
    }

    private fun loadBinaryFileLines(): List<String> {
        val reader = BufferedReader(InputStreamReader(FileInputStream(path.toFile())))
        val content = mutableListOf<String>()
        var line: String?
        while (reader.readLine().let { line = it; line != null }) content.add(line!!)
        if (content.isEmpty()) content.add("")
        return content
    }

    fun content(string: String) {
        content(string.split("\n"))
    }

    fun content(lines: List<String>) {
        content = lines
        if (settings.autosave) saveContent()
    }

    override fun stopWatcher() {
        watchFileSystem.set(false)
    }

    override fun launchWatcher() {
        if (watchFileSystem.compareAndSet(false, true)) {
            launchWatcherCoroutine()
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun launchWatcherCoroutine() {
        coroutineScope.launch {
            try {
                do {
                    val isReadable = path.isReadable()
                    val isWritable = path.isWritable()
                    if (!path.exists() || !isReadable) close()
                    else {
                        if (isReadableAtomic.compareAndSet(!isReadable, isReadable)
                            || isWritableAtomic.compareAndSet(!isWritable, isWritable)
                        ) callbacks.onDiskChangePermission.forEach { it(this@File) }
                        if (synchronized(this) { lastModified.get() < path.toFile().lastModified() }) {
                            lastModified.set(path.toFile().lastModified())
                            callbacks.onDiskChangeContent.forEach { it(this@File) }
                        }
                    }
                    delay(LIVE_UPDATE_REFRESH_RATE) // TODO: is there better way?
                } while (watchFileSystem.get())
            } catch (e: CancellationException) {
            } catch (e: java.lang.Exception) {
                notificationMgr.systemError(LOGGER, e, Message.View.UNEXPECTED_ERROR)
            }
        }
    }

    fun onDiskChangeContent(function: (File) -> Unit) {
        callbacks.onDiskChangeContent.push(function)
    }

    fun onDiskChangePermission(function: (File) -> Unit) {
        callbacks.onDiskChangePermission.push(function)
    }

    override fun beforeRun(function: (Resource) -> Unit) {
        callbacks.beforeRun.push(function)
    }

    override fun beforeSave(function: (Resource) -> Unit) {
        callbacks.beforeSave.push(function)
    }

    override fun beforeClose(function: (Resource) -> Unit) {
        callbacks.beforeClose.push(function)
    }

    override fun onClose(function: (Resource) -> Unit) {
        callbacks.onClose.push(function)
    }

    override fun onReopen(function: (Resource) -> Unit) {
        callbacks.onReopen.push(function)
    }

    override fun execBeforeClose() {
        callbacks.beforeClose.forEach { it(this) }
    }

    override fun rename(onSuccess: ((Resource) -> Unit)?) {
        if (isUnsavedResource) saveContent()
        projectMgr.renameFileDialog.open(this, onSuccess)
    }

    override fun move(onSuccess: ((Resource) -> Unit)?) {
        if (isUnsavedResource) saveContent()
        projectMgr.saveFileDialog.open(this, onSuccess)
    }

    override fun save(onSuccess: ((Resource) -> Unit)?) {
        saveContent()
        if (isUnsavedResource) projectMgr.saveFileDialog.open(this, onSuccess)
    }

    private fun saveContent() {
        callbacks.beforeSave.forEach { it(this) }
        synchronized(this) {
            Files.write(path, content)
            lastModified.set(path.toFile().lastModified())
        }
        hasUnsavedChanges = false
    }

    override fun close() {
        if (isOpenAtomic.compareAndSet(true, false)) {
            runner.reset()
            watchFileSystem.set(false)
            callbacks.onClose.forEach { it(this) }
            callbacks.clear()
        }
    }

    override fun delete() {
        try {
            close()
            path.deleteExisting()
            parent!!.remove(this)
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FILE_NOT_DELETABLE, path.name)
        }
    }
}
