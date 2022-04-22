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

import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.common.api.Navigable
import com.vaticle.typedb.studio.state.common.util.Settings
import java.nio.file.Path

class Project internal constructor(
    val path: Path,
    settings: Settings,
    projectMgr: ProjectManager,
    notificationMgr: NotificationManager
) : Navigable<ProjectItem> {

    val directory: Directory = Directory(path, null, settings, projectMgr, notificationMgr)
    override val name: String get() = "${Project::class.simpleName} (${directory.name})"
    override val info: String? = null
    override val parent: Navigable<ProjectItem>? = null
    override val entries = listOf(directory)
    override val isExpandable: Boolean = true
    override val isBulkExpandable: Boolean = true

    override fun reloadEntries() {
        directory.reloadEntries()
    }

    override fun compareTo(other: Navigable<ProjectItem>): Int {
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
