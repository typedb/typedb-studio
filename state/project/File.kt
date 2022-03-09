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
import kotlin.io.path.moveTo
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
    override val parent: Directory,
    settings: Settings,
    projectMgr: ProjectManager,
    notificationMgr: NotificationManager
) : ProjectItem(Type.FILE, path, parent, settings, projectMgr, notificationMgr), Resource {

    @OptIn(ExperimentalTime::class)
    companion object {
        private val LIVE_UPDATE_REFRESH_RATE: Duration = Duration.seconds(1)
        private val LOGGER = KotlinLogging.logger {}
    }

    val extension: String = this.path.extension
    val fileType: FileType = when {
        TYPEQL.extensions.contains(extension) -> TYPEQL
        else -> UNKNOWN
    }
    val isTypeQL: Boolean = fileType == TYPEQL
    val isTextFile: Boolean = checkIsTextFile()

    private var content: List<String> by mutableStateOf(listOf())
    private val onDiskChangeContent = LinkedBlockingDeque<(File) -> Unit>()
    private val onDiskChangePermission = LinkedBlockingDeque<(File) -> Unit>()
    private val onReopen = LinkedBlockingDeque<(File) -> Unit>()
    private val onWatch = LinkedBlockingDeque<(File) -> Unit>()
    private val beforeSave = LinkedBlockingDeque<(File) -> Unit>()
    private val beforeClose = LinkedBlockingDeque<(File) -> Unit>()
    private val onClose = LinkedBlockingDeque<(File) -> Unit>()
    private var watchFileSystem = AtomicBoolean(false)
    private var lastModified = AtomicLong(path.toFile().lastModified())
    private var isOpenAtomic: AtomicBoolean = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    override val fullName: String = computeFullName(path, projectMgr)
    override val runContent: String get() = content.joinToString("\n")
    override val runner: RunnerManager = RunnerManager()
    override val isOpen: Boolean get() = isOpenAtomic.get()
    override val isRunnable: Boolean = isTypeQL
    override val isEmpty: Boolean get() = content.size == 1 && content[0].isBlank()
    override val isUnsavedResource: Boolean get() = parent == projectMgr.unsavedFilesDir
    override var hasUnsavedChanges: Boolean by mutableStateOf(false)
    override val isReadable: Boolean get() = isReadableAtomic.get()
    override val isWritable: Boolean get() = isWritableAtomic.get()
    private var isReadableAtomic = AtomicBoolean(path.isReadable())
    private var isWritableAtomic = AtomicBoolean(path.isWritable())

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
            onReopen.forEach { it(this) }
            true
        } catch (e: Exception) { // TODO: specialise error message to actual error, e.g. read/write permissions
            notificationMgr.userError(LOGGER, FILE_NOT_READABLE, path)
            false
        }
    }

    internal fun trySaveTo(newPath: Path, overwrite: Boolean): File? {
        return try {
            if (overwrite && newPath.exists()) find(newPath)?.delete()
            path.moveTo(newPath, overwrite)
            val newFile = replaceWith(newPath)?.asFile()
            close()
            newFile
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FAILED_TO_SAVE_FILE, newPath)
            null
        }
    }

    override fun initialiseWith(other: ProjectItem) {
        val otherFile = other as File
        this.isOpenAtomic.set(other.isOpenAtomic.get())
        this.onDiskChangeContent.addAll(otherFile.onDiskChangeContent)
        this.onDiskChangePermission.addAll(otherFile.onDiskChangePermission)
        this.onWatch.addAll(otherFile.onWatch)
        this.onReopen.addAll(otherFile.onReopen)
        this.beforeSave.addAll(otherFile.beforeSave)
        this.beforeClose.addAll(otherFile.beforeClose)
        this.onClose.addAll(otherFile.onClose)
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

    fun writeLines(lines: List<String>) {
        content = lines
        if (settings.autosave) saveContent()
    }

    override fun onWatch(function: (Resource) -> Unit) {
        onWatch.push(function)
    }

    override fun stopWatcher() {
        watchFileSystem.set(false)
    }

    override fun launchWatcher() {
        onWatch.forEach { it(this) }
        if (watchFileSystem.compareAndSet(false, true)) {
            launchWatcherCoroutine()
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun launchWatcherCoroutine() {
        coroutineScope.launch {
            try {
                do {
                    if (!path.exists() || !path.isReadable()) close()
                    else {
                        var permissionChanged = false
                        if (isReadableAtomic.compareAndSet(!path.isReadable(), path.isReadable())) {
                            permissionChanged = true
                        }
                        if (isWritableAtomic.compareAndSet(!path.isWritable(), path.isWritable())) {
                            permissionChanged = true
                        }
                        if (permissionChanged) onDiskChangePermission.forEach { it(this@File) }
                        if (lastModified.get() < path.toFile().lastModified()) {
                            lastModified.set(path.toFile().lastModified())
                            onDiskChangeContent.forEach { it(this@File) }
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
        onDiskChangeContent.push(function)
    }

    fun onDiskChangePermission(function: (File) -> Unit) {
        onDiskChangePermission.push(function)
    }

    override fun beforeSave(function: (Resource) -> Unit) {
        beforeSave.push(function)
    }

    override fun beforeClose(function: (Resource) -> Unit) {
        beforeClose.push(function)
    }

    override fun onClose(function: (Resource) -> Unit) {
        onClose.push(function)
    }

    override fun onReopen(function: (Resource) -> Unit) {
        onReopen.push(function)
    }

    override fun execBeforeClose() {
        beforeClose.forEach { it(this) }
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
        beforeSave.forEach { it(this) }
        Files.write(path, content)
        lastModified.set(System.currentTimeMillis())
        hasUnsavedChanges = false
    }

    override fun close() {
        if (isOpenAtomic.compareAndSet(true, false)) {
            watchFileSystem.set(false)
            runner.reset()
            onDiskChangeContent.clear()
            onDiskChangePermission.clear()
            onWatch.clear()
            onReopen.clear()
            beforeSave.clear()
            beforeClose.clear()
            onClose.forEach { it(this) }
            onClose.clear()
        }
    }

    override fun delete() {
        try {
            close()
            path.deleteExisting()
            parent.remove(this)
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FILE_NOT_DELETABLE, path.name)
        }
    }
}
