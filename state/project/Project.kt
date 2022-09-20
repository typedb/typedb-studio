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

import com.vaticle.typedb.studio.state.app.NotificationManager.Companion.launchAndHandle
import com.vaticle.typedb.studio.state.common.util.Message.Project.Companion.PATH_NO_LONGER_EXIST
import com.vaticle.typedb.studio.state.page.Navigable
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mu.KotlinLogging

@OptIn(ExperimentalTime::class)
class Project internal constructor(val path: Path, private val projectMgr: ProjectManager) : Navigable<PathState> {

    private val isOpen = AtomicBoolean(false)
    private val coroutines = CoroutineScope(Dispatchers.Default)
    val directory: DirectoryState = DirectoryState(path, null, projectMgr)

    override val name: String get() = "${Project::class.simpleName} (${directory.name})"
    override val info: String? = null
    override val parent: Navigable<PathState>? = null
    override val entries = listOf(directory)
    override val isExpandable: Boolean = true
    override val isBulkExpandable: Boolean = true

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private val WATCHER_REFRESH_RATE = Duration.seconds(1)
    }

    override fun reloadEntries() {
        directory.reloadEntries()
    }

    fun open() {
        if (isOpen.compareAndSet(false, true)) launchWatcher()
    }

    private fun launchWatcher() = coroutines.launchAndHandle(projectMgr.notification, LOGGER) {
        do {
            if (!path.exists() || !path.isReadable()) {
                close()
                projectMgr.notification.userError(LOGGER, PATH_NO_LONGER_EXIST, path)
            } else delay(WATCHER_REFRESH_RATE)
        } while (isOpen.get())
    }

    fun close() {
        if (isOpen.compareAndSet(true, false)) {
            directory.closeRecursive()
            projectMgr.close(this)
        }
    }

    override fun compareTo(other: Navigable<PathState>): Int {
        return if (other is Project) directory.compareTo(other.directory)
        else -1
    }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Project
        return directory == other.directory
    }

    override fun hashCode(): Int {
        return directory.hashCode()
    }
}
