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
import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Message
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_CREATE_OR_RENAME_FILE_DUE_TO_DUPLICATE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_RENAME_FILE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FAILED_TO_SAVE_FILE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FILE_NOT_DELETABLE
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.FILE_NOT_READABLE
import com.vaticle.typedb.studio.state.common.util.Message.System.Companion.ILLEGAL_CAST
import com.vaticle.typedb.studio.state.common.util.Property.FileType
import com.vaticle.typedb.studio.state.common.util.Property.FileType.TYPEQL
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.connection.RunnerManager
import com.vaticle.typedb.studio.state.page.Pageable
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mu.KotlinLogging

class FileState internal constructor(
    path: Path,
    parent: DirectoryState,
    projectMgr: ProjectManager
) : PathState(parent, path, Type.FILE, projectMgr), Pageable.Runnable {

    @OptIn(ExperimentalTime::class)
    companion object {
        private val LIVE_UPDATE_REFRESH_RATE: Duration = Duration.seconds(1)
        private val LOGGER = KotlinLogging.logger {}
    }

    private class Callbacks {

        val onDiskChangeContent = LinkedBlockingQueue<(FileState) -> Unit>()
        val onDiskChangePermission = LinkedBlockingQueue<(FileState) -> Unit>()
        val onReopen = LinkedBlockingQueue<(FileState) -> Unit>()
        val beforeRun = LinkedBlockingQueue<(FileState) -> Unit>()
        val beforeSave = LinkedBlockingQueue<(FileState) -> Unit>()
        val beforeClose = LinkedBlockingQueue<(FileState) -> Unit>()
        val onClose = LinkedBlockingQueue<(FileState) -> Unit>()

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

    val fileType: FileType = FileType.of(path.extension)
    val isTypeQL: Boolean = fileType == TYPEQL
    val isTextFile: Boolean = checkIsTextFile()

    private var callbacks = Callbacks()
    private var content: List<String> by mutableStateOf(listOf())
    private var watchFileSystem = AtomicBoolean(false)
    private var lastModified = AtomicLong(path.toFile().lastModified())
    private var isOpenAtomic: AtomicBoolean = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override val windowTitle: String = computeWindowTitle(path, projectMgr)
    override val runContent: String
        get() {
            callbacks.beforeRun.forEach { it(this) }
            return content.joinToString("\n")
        }
    override var runners: RunnerManager = RunnerManager()
    override val isOpen: Boolean get() = isOpenAtomic.get()
    override val isRunnable: Boolean = fileType.isRunnable
    override val isEmpty: Boolean get() = content.size == 1 && content[0].isBlank()
    override val isUnsavedPageable: Boolean get() = parent == projectMgr.unsavedFilesDir
    override var hasUnsavedChanges: Boolean by mutableStateOf(false)
    override val isReadable: Boolean get() = isReadableAtomic.get()
    override val isWritable: Boolean get() = isWritableAtomic.get()
    override val isExpandable: Boolean = false
    override val isBulkExpandable: Boolean = false
    override val entries: List<PathState> = listOf()
    private var isReadableAtomic = AtomicBoolean(path.isReadable())
    private var isWritableAtomic = AtomicBoolean(path.isWritable())

    fun onDiskChangeContent(function: (FileState) -> Unit) = callbacks.onDiskChangeContent.put(function)
    fun onDiskChangePermission(function: (FileState) -> Unit) = callbacks.onDiskChangePermission.put(function)
    fun beforeRun(function: (Pageable) -> Unit) = callbacks.beforeRun.put(function)
    fun beforeSave(function: (Pageable) -> Unit) = callbacks.beforeSave.put(function)
    fun beforeClose(function: (Pageable) -> Unit) = callbacks.beforeClose.put(function)
    override fun onClose(function: (Pageable) -> Unit) = callbacks.onClose.put(function)
    override fun onReopen(function: (Pageable) -> Unit) = callbacks.onReopen.put(function)
    override fun execBeforeClose() = callbacks.beforeClose.forEach { it(this) }
    override fun tryOpen(): Boolean = tryOpen(null)
    override fun reloadEntries() {}
    override fun asFile(): FileState = this
    override fun asDirectory(): DirectoryState {
        throw TypeCastException(ILLEGAL_CAST.message(FileState::class.simpleName, DirectoryState::class.simpleName))
    }

    private fun checkIsTextFile(): Boolean {
        val type = Files.probeContentType(path)
        return type != null && type.startsWith("text")
    }

    private fun computeWindowTitle(path: Path, projectMgr: ProjectManager): String {
        return if (isUnsavedPageable) projectMgr.current!!.directory.name + " (unsaved: " + name + ")"
        else path.relativeTo(projectMgr.current!!.directory.path.parent).toString()
    }

    private fun tryOpen(index: Int? = null): Boolean {
        return if (!path.isReadable()) {
            projectMgr.notification.userError(LOGGER, FILE_NOT_READABLE, path)
            false
        } else try {
            readContent()
            isOpenAtomic.set(true)
            callbacks.onReopen.forEach { it(this) }
            projectMgr.pages.opened(this, index)
            activate()
            true
        } catch (e: Exception) { // TODO: specialise error message to actual error, e.g. read/write permissions
            projectMgr.notification.userError(LOGGER, FILE_NOT_READABLE, path)
            false
        }
    }

    override fun activate() {
        assert(isOpen) { "Only opened files can be activated" }
        if (watchFileSystem.compareAndSet(false, true)) launchWatcherCoroutine()
        projectMgr.pages.active(this)
    }

    override fun deactivate() {
        watchFileSystem.set(false)
    }

    override fun mayOpenAndRun(content: String) {
        if (!isRunnable || (!isOpen && !tryOpen())) return
        projectMgr.client.runner(content)?.let { runners.launch(it) }
    }

    internal fun tryRename(newName: String): FileState? {
        val newPath = path.resolveSibling(newName)
        return if (parent!!.contains(newName)) {
            projectMgr.notification.userError(LOGGER, FAILED_TO_CREATE_OR_RENAME_FILE_DUE_TO_DUPLICATE, newPath)
            null
        } else try {
            val clonedRunner = runners.clone()
            val clonedCallbacks = callbacks.clone()
            close()
            movePathTo(newPath)
            find(newPath)?.asFile()?.also {
                it.runners = clonedRunner
                it.callbacks = clonedCallbacks
            }
        } catch (e: Exception) {
            projectMgr.notification.userError(LOGGER, FAILED_TO_RENAME_FILE, newPath)
            null
        }
    }

    internal fun trySaveTo(newPath: Path, overwrite: Boolean): FileState? {
        return try {
            val clonedRunner = runners.clone()
            val clonedCallbacks = callbacks.clone()
            close()
            if (overwrite && newPath.exists()) find(newPath)?.delete()
            movePathTo(newPath, overwrite)
            find(newPath)?.asFile()?.also {
                it.runners = clonedRunner
                it.callbacks = clonedCallbacks
            }
        } catch (e: Exception) {
            projectMgr.notification.userError(LOGGER, FAILED_TO_SAVE_FILE, newPath)
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
        if (projectMgr.preference.autosave) saveContent()
    }

    @OptIn(ExperimentalTime::class)
    private fun launchWatcherCoroutine() {
        coroutineScope.launchAndHandle(projectMgr.notification, LOGGER) {
            try {
                do {
                    val isReadable = path.isReadable()
                    val isWritable = path.isWritable()
                    if (!path.exists() || !isReadable) close()
                    else {
                        if (isReadableAtomic.compareAndSet(!isReadable, isReadable)
                            || isWritableAtomic.compareAndSet(!isWritable, isWritable)
                        ) callbacks.onDiskChangePermission.forEach { it(this@FileState) }
                        if (synchronized(this) { lastModified.get() < path.toFile().lastModified() }) {
                            lastModified.set(path.toFile().lastModified())
                            callbacks.onDiskChangeContent.forEach { it(this@FileState) }
                        }
                    }
                    delay(LIVE_UPDATE_REFRESH_RATE) // TODO: is there better way?
                } while (watchFileSystem.get())
            } catch (e: CancellationException) {
            } catch (e: java.lang.Exception) {
                projectMgr.notification.systemError(LOGGER, e, Message.View.UNEXPECTED_ERROR)
            }
        }
    }

    override fun initiateRename() {
        saveContent()
        // currentIndex must be computed before passing into lambda
        val currentIndex = if (isOpen) projectMgr.pages.opened.indexOf(this) else -1
        projectMgr.renameFileDialog.open(this, if (!isOpen) null else ({ it.tryOpen(currentIndex) }))
    }

    override fun initiateMove() {
        initiateMoveOrSave(isMove = true, reopen = true)
    }

    override fun initiateSave(reopen: Boolean) {
        initiateMoveOrSave(isMove = false, reopen = reopen)
    }

    private fun initiateMoveOrSave(isMove: Boolean, reopen: Boolean) {
        saveContent()
        if (isUnsavedPageable || isMove) {
            // currentIndex must be computed before passing into lambda
            val currentIndex = if (isOpen && reopen) projectMgr.pages.opened.indexOf(this) else -1
            projectMgr.saveFileDialog.open(this, if (!isOpen) null else ({ it.tryOpen(currentIndex) }))
        }
    }

    override fun initiateDelete(onSuccess: () -> Unit) {
        projectMgr.confirmation.submit(
            title = Label.CONFIRM_FILE_DELETION,
            message = Sentence.CONFIRM_FILE_DELETION,
            onConfirm = { delete(); onSuccess() }
        )
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
            runners.close()
            watchFileSystem.set(false)
            projectMgr.pages.close(this)
            callbacks.onClose.forEach { it(this) }
            callbacks.clear()
        }
    }

    override fun closeRecursive() {
        close()
    }

    override fun delete() {
        try {
            close()
            path.deleteExisting()
            parent!!.remove(this)
        } catch (e: Exception) {
            projectMgr.notification.userError(LOGGER, FILE_NOT_DELETABLE, path.name)
        }
    }
}
