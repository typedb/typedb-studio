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

package com.vaticle.typedb.studio.service.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Message
import com.vaticle.typedb.studio.service.common.util.Message.Companion.UNKNOWN
import com.vaticle.typedb.studio.service.common.util.Message.Project.Companion.FAILED_TO_CREATE_OR_RENAME_FILE_DUE_TO_DUPLICATE
import com.vaticle.typedb.studio.service.common.util.Message.Project.Companion.FAILED_TO_RENAME_FILE
import com.vaticle.typedb.studio.service.common.util.Message.Project.Companion.FAILED_TO_SAVE_FILE
import com.vaticle.typedb.studio.service.common.util.Message.Project.Companion.FILE_HAS_BEEN_MOVED_OUT
import com.vaticle.typedb.studio.service.common.util.Message.Project.Companion.FILE_NOT_DELETABLE
import com.vaticle.typedb.studio.service.common.util.Message.Project.Companion.FILE_NOT_READABLE
import com.vaticle.typedb.studio.service.common.util.Message.System.Companion.ILLEGAL_CAST
import com.vaticle.typedb.studio.service.common.util.Property.FileType
import com.vaticle.typedb.studio.service.common.util.Property.FileType.TYPEQL
import com.vaticle.typedb.studio.service.common.util.Sentence
import com.vaticle.typedb.studio.service.connection.QueryRunnerService
import com.vaticle.typedb.studio.service.page.Pageable
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
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mu.KotlinLogging

class FileState internal constructor(
    path: Path,
    parent: DirectoryState,
    projectSrv: ProjectService,
) : PathState(parent, path, Type.FILE, projectSrv), Pageable.Runnable {

    companion object {
        private val LIVE_UPDATE_REFRESH_RATE: Duration = 1.seconds
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
    private val coroutines = CoroutineScope(Dispatchers.Default)

    override val windowTitle: String = computeWindowTitle(path, projectSrv)
    override val runContent: String
        get() {
            callbacks.beforeRun.forEach { it(this) }
            return content.joinToString("\n")
        }
    override var runners: QueryRunnerService = QueryRunnerService()
    override val isOpen: Boolean get() = isOpenAtomic.get()
    override val isRunnable: Boolean = fileType.isRunnable
    override val isEmpty: Boolean get() = content.size == 1 && content[0].isBlank()
    override val isUnsavedPageable: Boolean get() = parent == projectSrv.unsavedFilesDir
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

    private fun computeWindowTitle(path: Path, projectSrv: ProjectService): String {
        return if (isUnsavedPageable) projectSrv.current?.directory?.name + " (unsaved: " + name + ")"
        else path.relativeTo(projectSrv.current!!.directory.path.parent).toString()
    }

    private fun tryOpen(index: Int? = null): Boolean {
        return if (!path.isReadable()) {
            projectSrv.notification.userError(LOGGER, FILE_NOT_READABLE, path)
            false
        } else try {
            readContent()
            isOpenAtomic.set(true)
            callbacks.onReopen.forEach { it(this) }
            projectSrv.pages.opened(this, index)
            activate()
            true
        } catch (e: Exception) { // TODO: specialise error message to actual error, e.g. read/write permissions
            projectSrv.notification.systemError(LOGGER, e, FILE_NOT_READABLE, path)
            false
        }
    }

    override fun activate() {
        assert(isOpen) { "Only opened files can be activated" }
        if (watchFileSystem.compareAndSet(false, true)) launchWatcher()
        projectSrv.pages.active(this)
    }

    override fun deactivate() {
        watchFileSystem.set(false)
    }

    override fun mayOpenAndRun() {
        if (!isRunnable || (!isOpen && !tryOpen())) return
        mayRunSnippet(runContent)
    }

    fun mayRunSnippet(snippet: String) {
        projectSrv.driver.run(snippet)?.let { runners.launched(it) }
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
        if (projectSrv.preference.autoSave) saveContent()
    }

    private fun launchWatcher() = coroutines.launchAndHandle(projectSrv.notification, LOGGER) {
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
            projectSrv.notification.systemError(LOGGER, e, Message.Framework.UNEXPECTED_ERROR)
        }
    }

    override fun initiateRename() {
        saveContent()
        // currentIndex must be computed before passing into lambda
        val currentIndex = if (isOpen) projectSrv.pages.opened.indexOf(this) else -1
        projectSrv.renameFileDialog.open(this, if (!isOpen) null else ({ it.tryOpen(currentIndex) }))
    }

    fun tryRename(name: String) = mayConfirm(path.resolveSibling(name), projectSrv.renameFileDialog) { onSuccess ->
        val newPath = path.resolveSibling(name)
        if (parent!!.contains(name)) {
            projectSrv.notification.userError(LOGGER, FAILED_TO_CREATE_OR_RENAME_FILE_DUE_TO_DUPLICATE, newPath)
            null
        } else try {
            val clonedRunnerSrv = runners.clone()
            val clonedCallbacks = callbacks.clone()
            close()
            movePathTo(newPath)
            find(newPath)?.asFile()?.also { newFile ->
                newFile.runners = clonedRunnerSrv
                newFile.callbacks = clonedCallbacks
                onSuccess?.let { it(newFile) }
                projectSrv.execContentChange()
            }
        } catch (e: Exception) {
            projectSrv.notification.systemError(LOGGER, e, FAILED_TO_RENAME_FILE, newPath, e.message ?: UNKNOWN)
            null
        }
    }

    override fun initiateSave(reopen: Boolean) = initiateMoveOrSave(isMove = false, reopen = reopen)

    override fun initiateMove() = initiateMoveOrSave(isMove = true)

    private fun initiateMoveOrSave(isMove: Boolean, reopen: Boolean = true) {
        saveContent()
        if (isUnsavedPageable || isMove) {
            // currentIndex must be computed before passing into lambda
            val currentIndex = if (isOpen && reopen) projectSrv.pages.opened.indexOf(this) else -1
            projectSrv.saveFileDialog.open(this, if (!isOpen) null else ({ it.tryOpen(currentIndex) }))
        }
    }

    fun trySave(path: Path, overwrite: Boolean) = mayConfirm(path, projectSrv.saveFileDialog) { onSuccess ->
        try {
            val clonedRunner = runners.clone()
            val clonedCallbacks = callbacks.clone()
            close()
            if (overwrite && path.exists()) find(path)?.tryDelete()
            movePathTo(path, overwrite)
            if (!path.startsWith(projectSrv.current!!.path)) {
                projectSrv.notification.userWarning(LOGGER, FILE_HAS_BEEN_MOVED_OUT, path)
                null
            } else find(path)?.asFile()?.also { newFile ->
                newFile.runners = clonedRunner
                newFile.callbacks = clonedCallbacks
                onSuccess?.let { it(newFile) }
                projectSrv.execContentChange()
            }
        } catch (e: Exception) {
            projectSrv.notification.systemError(LOGGER, e, FAILED_TO_SAVE_FILE, path)
            null
        }
    }

    private fun mayConfirm(
        newPath: Path,
        dialog: ProjectService.ModifyFileDialogState,
        onConfirm: (onSuccess: ((FileState) -> Unit)?) -> FileState?
    ) {
        if (isRunnable && !FileType.of(newPath).isRunnable) {
            // we need to record dialog.onSuccess before dialog.close() which clears it
            val onSuccess = dialog.onSuccess
            dialog.close()
            projectSrv.confirmation.submit(
                title = Label.CONVERT_FILE_TYPE,
                message = Sentence.CONFIRM_FILE_TYPE_CHANGE_NON_RUNNABLE.format(
                    name, newPath.fileName, FileType.RUNNABLE_EXTENSIONS_STR
                )
            ) { onConfirm(onSuccess) }
        } else onConfirm { newFile ->
            dialog.onSuccess?.let { it(newFile) }
            dialog.close()
        }
    }

    override fun initiateDelete(onSuccess: () -> Unit) {
        projectSrv.confirmation.submit(
            title = Label.CONFIRM_FILE_DELETION,
            message = Sentence.CONFIRM_FILE_DELETION.format(name),
            onConfirm = { tryDelete(); onSuccess() }
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

    override fun tryDelete() {
        try {
            close()
            path.deleteExisting()
            parent!!.remove(this)
        } catch (e: Exception) {
            projectSrv.notification.systemError(LOGGER, e, FILE_NOT_DELETABLE, path.name)
        }
    }

    override fun closeRecursive() = close()

    override fun close() {
        if (isOpenAtomic.compareAndSet(true, false)) {
            runners.close()
            watchFileSystem.set(false)
            projectSrv.pages.close(this)
            callbacks.onClose.forEach { it(this) }
            callbacks.clear()
        }
    }
}
