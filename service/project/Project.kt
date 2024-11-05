/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.project

import com.typedb.studio.service.common.NotificationService.Companion.launchAndHandle
import com.typedb.studio.service.common.util.Message.Project.Companion.PATH_NO_LONGER_EXIST
import com.typedb.studio.service.page.Navigable
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import mu.KotlinLogging

class Project internal constructor(val path: Path, private val projectSrv: ProjectService) : Navigable<PathState> {

    private val isOpen = AtomicBoolean(false)
    private val coroutines = CoroutineScope(Dispatchers.Default)
    val directory: DirectoryState = DirectoryState(path, null, projectSrv)

    override val name: String get() = "${Project::class.simpleName} (${directory.name})"
    override val info: String? = null
    override val parent: Navigable<PathState>? = null
    override val entries = listOf(directory)
    override val isExpandable: Boolean = true
    override val isBulkExpandable: Boolean = true

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private val WATCHER_REFRESH_RATE = 1.seconds
    }

    override fun reloadEntries() {
        directory.reloadEntries()
    }

    fun open() {
        if (isOpen.compareAndSet(false, true)) launchWatcher()
    }

    private fun launchWatcher() = coroutines.launchAndHandle(projectSrv.notification, LOGGER) {
        do {
            if (!path.exists() || !path.isReadable()) {
                close()
                projectSrv.notification.userError(LOGGER, PATH_NO_LONGER_EXIST, path)
            } else delay(WATCHER_REFRESH_RATE)
        } while (isOpen.get())
    }

    fun close() {
        if (isOpen.compareAndSet(true, false)) {
            directory.closeRecursive()
            projectSrv.close(this)
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
