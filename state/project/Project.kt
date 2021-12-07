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
import com.vaticle.typedb.studio.state.common.Navigable
import com.vaticle.typedb.studio.state.notification.Error
import com.vaticle.typedb.studio.state.notification.Message
import com.vaticle.typedb.studio.state.notification.Message.Project.Companion.PROJECT_CLOSED
import com.vaticle.typedb.studio.state.notification.NotificationManager
import com.vaticle.typedb.studio.state.page.PageManager
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Project internal constructor(val path: Path, val pageMgr: PageManager, val notificationMgr: NotificationManager) :
    Navigable<ProjectItem> {

    companion object {
        @OptIn(ExperimentalTime::class)
        private val DIRECTORY_REFRESH_RATE = Duration.seconds(2)
        private val LOGGER = KotlinLogging.logger {}
    }

    override val entries: List<ProjectItem> get() = listOf(directory)
    override var selected: ProjectItem? by mutableStateOf(null)

    val directory: Directory = Directory(path, null)
    val name: String get() = directory.name

    private var coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    init {
        directory.expand()
        initDirectoryWatcher(directory)
    }

    override fun open(item: ProjectItem) {
        when (item) {
            is Directory -> item.toggle()
            is File -> pageMgr.open(item)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun initDirectoryWatcher(directory: Directory) {
        coroutineScope.launch {
            try {
                do {
                    delay(DIRECTORY_REFRESH_RATE) // TODO: is there better way?
                    directory.checkForUpdate()
                } while (true)
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
