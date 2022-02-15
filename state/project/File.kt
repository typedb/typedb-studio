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
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable
import kotlin.io.path.name
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
    notificationMgr: NotificationManager
) :
    ProjectItem(Type.FILE, path, parent, settings, notificationMgr), Pageable {

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

    private var isOpen: AtomicBoolean = AtomicBoolean(false)
    private var onUpdate: ((File) -> Unit)? by mutableStateOf(null)
    private var onPermissionChange: ((File) -> Unit)? by mutableStateOf(null)
    private var onClose: (() -> Unit)? by mutableStateOf(null)
    private var lastModified by mutableStateOf(path.toFile().lastModified())
    private var watchFileSystem by mutableStateOf(false)
    private val coroutineScope = CoroutineScope(EmptyCoroutineContext)

    override var hasChanges by mutableStateOf(false); private set
    override var isReadable: Boolean by mutableStateOf(path.isReadable())
    override var isWritable: Boolean by mutableStateOf(path.isWritable())

    private fun checkIsTextFile(): Boolean {
        val type = Files.probeContentType(path)
        return type != null && type.startsWith("text")
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
            isOpen.set(true)
            true
        } catch (e: Exception) { // TODO: specialise error message to actual error, e.g. read/write permissions
            notificationMgr.userError(LOGGER, FILE_NOT_READABLE, path)
            false
        }
    }

    fun hasChanged() {
        hasChanges = true
    }

    fun readLines(): List<String> {
        content = if (isTextFile) loadTextFileLines() else loadBinaryFileLines()
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
        if (settings.autosave) save()
    }

    fun save() {
        Files.write(path, content)
        lastModified = System.currentTimeMillis()
        hasChanges = false
    }

    override fun mayLaunchWatcher() = launchWatcher()

    override fun mayStopWatcher() {
        watchFileSystem = false
    }

    @OptIn(ExperimentalTime::class)
    private fun launchWatcher() {
        if (watchFileSystem) return else watchFileSystem = true
        coroutineScope.launch {
            try {
                do {
                    if (lastModified < path.toFile().lastModified()) {
                        println("launchWatcher calling onUpdate ...")
                        lastModified = path.toFile().lastModified()
                        onUpdate?.let { it(this@File) }
                    }
                    if (isReadable != path.isReadable() || isWritable != path.isWritable()) {
                        isReadable = path.isReadable()
                        isWritable = path.isWritable()
                        onPermissionChange?.let { it(this@File) }
                    }
                    if (!path.exists() || !path.isReadable()) onClose?.let { it() }
                    delay(LIVE_UPDATE_REFRESH_RATE) // TODO: is there better way?
                } while (watchFileSystem)
            } catch (e: CancellationException) {
            } catch (e: java.lang.Exception) {
                notificationMgr.systemError(LOGGER, e, Message.View.UNEXPECTED_ERROR)
            }
        }
    }

    fun onChangeContentFromDisk(function: (File) -> Unit) {
        onUpdate = function
    }

    fun onChangePermissionFromDisk(function: (File) -> Unit) {
        onPermissionChange = function
    }

    override fun onClose(function: () -> Unit) {
        onClose = function
    }

    override fun close() {
        if (isOpen.compareAndSet(true, false)) {
            watchFileSystem = false
            onUpdate = null
            onPermissionChange = null
            onClose?.let { it() }
            onClose = null
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
