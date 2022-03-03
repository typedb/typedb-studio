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

package com.vaticle.typedb.studio.state.resource

import com.vaticle.typedb.studio.state.runner.RunnerManager

interface Resource {

    val name: String
    val fullName: String
    val runContent: String
    val runner: RunnerManager
    val isOpen: Boolean
    val isRunnable: Boolean
    val isEmpty: Boolean
    val isUnsavedResource: Boolean
    val hasUnsavedChanges: Boolean
    val needSaving get() = hasUnsavedChanges || (isUnsavedResource && !isEmpty)

    fun tryOpen(): Boolean

    fun launchWatcher()

    fun stopWatcher()

    fun onWatch(function: (Resource) -> Unit)

    fun beforeSave(function: (Resource) -> Unit)

    fun beforeClose(function: (Resource) -> Unit)

    fun onClose(function: (Resource) -> Unit)

    fun onReopen(function: (Resource) -> Unit)

    fun execBeforeClose()

    fun rename(onSuccess: ((Resource) -> Unit)? = null)

    fun save(onSuccess: ((Resource) -> Unit)? = null)

    fun move(onSuccess: ((Resource) -> Unit)? = null)

    fun delete()
    fun close()
}