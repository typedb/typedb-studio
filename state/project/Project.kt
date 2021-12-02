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

import com.vaticle.typedb.studio.state.notification.Error
import com.vaticle.typedb.studio.state.notification.Message
import com.vaticle.typedb.studio.state.notification.Message.Project.Companion.MAX_DIR_EXPANDED_REACHED
import com.vaticle.typedb.studio.state.notification.Message.Project.Companion.PROJECT_CLOSED
import com.vaticle.typedb.studio.state.notification.NotificationManager
import java.nio.file.Path
import java.nio.file.WatchService
import java.util.*
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Project internal constructor(val path: Path, val notificationMgr: NotificationManager) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private const val MAX_ITEM_EXPANDED = 256
    }

    val directory: Directory = Directory(path, this)
    val name: String get() = directory.name
    private var coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    init {
        directory.expandAndReloadEntries()
        initDirectoryWatcher(directory)
    }

    fun expand() {
        toggle(true)
    }

    fun collapse() {
        toggle(false)
    }

    private fun toggle(isExpanded: Boolean) {
        var i = 1
        val directories: LinkedList<Directory> = LinkedList()
        directories.add(directory)

        while (directories.isNotEmpty() && i < MAX_ITEM_EXPANDED) {
            val dir = directories.pop()
            if (isExpanded) {
                dir.expandAndReloadEntries()
                i += dir.entries.count()
                directories.addAll(dir.entries.filterIsInstance<Directory>())
            } else {
                dir.collapse()
                directories.addAll(dir.entries.filterIsInstance<Directory>().filter { it.isExpanded })
            }
        }
        if (directories.isNotEmpty()) {
            notificationMgr.userError(Error.fromUser(MAX_DIR_EXPANDED_REACHED, path, MAX_ITEM_EXPANDED), LOGGER)
        }
    }

    private fun initDirectoryWatcher(directory: Directory) {
        val watcher: WatchService = directory.watchService()
        coroutineScope.launch {
            try {
                while (true) {
                    val watchKey = async(Dispatchers.IO) { watcher.take() }.await()
                    directory.reloadEntries()
                    watchKey.reset()
                }
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                notificationMgr.systemError(Error.fromSystem(e, Message.Project.UNEXPECTED_ERROR), LOGGER)
            }
        }
    }

    private fun cancelDirectoryWatcher() {
        coroutineScope.cancel(PROJECT_CLOSED.message(directory.path.toAbsolutePath()))
    }

    internal fun close() {
        cancelDirectoryWatcher()
    }
}
