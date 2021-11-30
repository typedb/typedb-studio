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
import com.vaticle.typedb.studio.state.notification.Message.Project.Companion.PROJECT_CLOSED
import com.vaticle.typedb.studio.state.notification.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.nio.file.Path
import java.nio.file.WatchService
import kotlin.coroutines.EmptyCoroutineContext

class Project internal constructor(path: Path, private val notificationMgr: NotificationManager) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    val directory: Directory = Directory(path)
    val name: String get() = directory.name
    private var coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    init {
        initDirectoryWatcher(directory)
    }

    private fun initDirectoryWatcher(directory: Directory) {
        val watcher: WatchService = directory.watchService()
        coroutineScope.launch {
            try {
                while (true) {
                    val watchKey = async(Dispatchers.IO) { watcher.take() }.await()
                    for (event in watchKey.pollEvents()) {
                        println() // TODO
                    }
                    watchKey.reset()
                }
            } catch (e: Exception) {
                notificationMgr.systemError(Error.fromSystem(e, Message.Connection.UNEXPECTED_ERROR), LOGGER)
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
