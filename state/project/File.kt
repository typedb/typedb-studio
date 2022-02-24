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
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FILE_NOT_DELETABLE
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FILE_NOT_READABLE
import com.vaticle.typedb.studio.state.common.Message.System.Companion.ILLEGAL_CAST
import com.vaticle.typedb.studio.state.common.Property.FileType
import com.vaticle.typedb.studio.state.common.Property.FileType.TYPEQL
import com.vaticle.typedb.studio.state.common.Property.FileType.UNKNOWN
import com.vaticle.typedb.studio.state.common.Settings
import com.vaticle.typedb.studio.state.notification.NotificationManager
import com.vaticle.typedb.studio.state.page.Pageable
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
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
    override val parent: Directory,
    settings: Settings,
    projectMgr: ProjectManager,
    notificationMgr: NotificationManager
) : ProjectItem(Type.FILE, path, parent, settings, projectMgr, notificationMgr), Pageable {

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
    private var onDiskChangeContent: ((File) -> Unit)? by mutableStateOf(null)
    private var onDiskChangePermission: ((File) -> Unit)? by mutableStateOf(null)
    private var onWatch: (() -> Unit)? by mutableStateOf(null)
    private var beforeSave = AtomicReference<(() -> Unit)?>(null)
    private var beforeClose = AtomicReference<(() -> Unit)?>(null)
    private var onClose = AtomicReference<(() -> Unit)?>(null)
    private var watchFileSystem = AtomicBoolean(false)
    private var hasChanges by mutableStateOf(false)
    private var lastModified = AtomicLong(path.toFile().lastModified())
    private var isOpenAtomic: AtomicBoolean = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    override val isOpen: Boolean get() = isOpenAtomic.get()
    override val isUnsavedFile: Boolean get() = parent == projectMgr.unsavedFilesDir
    override val isUnsaved: Boolean get() = hasChanges || (isUnsavedFile && !isContentEmpty())
    override val isReadable: Boolean get() = isReadableAtomic.get()
    override val isWritable: Boolean get() = isWritableAtomic.get()
    override val isRunnable: Boolean = isTypeQL
    override val fullName: String = computeFullName(path, projectMgr)
    private var isReadableAtomic = AtomicBoolean(path.isReadable())
    private var isWritableAtomic = AtomicBoolean(path.isWritable())

    private fun checkIsTextFile(): Boolean {
        val type = Files.probeContentType(path)
        return type != null && type.startsWith("text")
    }

    private fun computeFullName(path: Path, projectMgr: ProjectManager): String {
        return if (isUnsavedFile) projectMgr.current!!.directory.name + " (unsaved: " + name + ")"
        else path.relativeTo(projectMgr.current!!.directory.path.parent).toString()
    }

    private fun isContentEmpty(): Boolean {
        return content.size == 1 && content[0].isBlank()
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
            // TODO: find a more efficient way to verify access without having to load the entire file
            if (isTextFile) loadTextFileLines()
            else loadBinaryFileLines()
            isOpenAtomic.set(true)
            true
        } catch (e: Exception) { // TODO: specialise error message to actual error, e.g. read/write permissions
            notificationMgr.userError(LOGGER, FILE_NOT_READABLE, path)
            false
        }
    }

    fun isChanged() {
        hasChanges = true
    }

    fun reloadFromDisk(): List<String> {
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

    override fun onWatch(function: () -> Unit) {
        onWatch = function
    }

    override fun stopWatcher() {
        watchFileSystem.set(false)
    }

    override fun launchWatcher() {
        onWatch?.let { it() }
        if (watchFileSystem.compareAndSet(false, true)){
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
                        if (permissionChanged) onDiskChangePermission?.let { it(this@File) }
                        if (lastModified.get() < path.toFile().lastModified()) {
                            lastModified.set(path.toFile().lastModified())
                            onDiskChangeContent?.let { it(this@File) }
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
        onDiskChangeContent = function
    }

    fun onDiskChangePermission(function: (File) -> Unit) {
        onDiskChangePermission = function
    }

    override fun beforeSave(function: () -> Unit) {
        beforeSave.set(function)
    }

    override fun beforeClose(function: () -> Unit) {
        beforeClose.set(function)
    }

    override fun onClose(function: () -> Unit) {
        onClose.set(function)
    }

    private fun execBeforeSave() {
        beforeSave.getAndSet(null)?.let { it() }
    }

    override fun execBeforeClose() {
        beforeClose.getAndSet(null)?.let { it() }
    }

    private fun execOnClose() {
        onClose.getAndSet(null)?.let { it() }
    }

    override fun rename(onSuccess: ((Pageable) -> Unit)?) {
        if (isUnsavedFile) saveContent()
        projectMgr.renameFileDialog.open(this, onSuccess)
    }

    override fun move(onSuccess: ((Pageable) -> Unit)?) {
        if (isUnsavedFile) saveContent()
        projectMgr.saveFileDialog.open(this, onSuccess)
    }

    override fun save(onSuccess: ((Pageable) -> Unit)?) {
        saveContent()
        if (isUnsavedFile) projectMgr.saveFileDialog.open(this, onSuccess)
    }

    fun saveContent() {
        execBeforeSave()
        Files.write(path, content)
        lastModified.set(System.currentTimeMillis())
        hasChanges = false
    }

    override fun close() {
        if (isOpenAtomic.compareAndSet(true, false)) {
            watchFileSystem.set(false)
            onDiskChangeContent = null
            onDiskChangePermission = null
            onWatch = null
            execBeforeClose()
            execOnClose()
        }
    }

    override fun delete() {
        try {
            close()
            path.deleteExisting()
        } catch (e: Exception) {
            notificationMgr.userError(LOGGER, FILE_NOT_DELETABLE, path.name)
        }
    }
}
