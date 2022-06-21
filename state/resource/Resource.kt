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

interface Resource {

    val name: String
    val windowTitle: String
    val isOpen: Boolean
    val isWritable: Boolean
    val isEmpty: Boolean
    val isUnsavedResource: Boolean
    val hasUnsavedChanges: Boolean
    val isRunnable: Boolean get() = false
    val needSaving get() = hasUnsavedChanges || (isUnsavedResource && !isEmpty)

    fun asRunnable(): Runnable {
        throw ClassCastException("Illegal cast of resource into runnable")
    }

    fun tryOpen(): Boolean

    fun activate()

    fun deactivate()

    fun beforeRun(function: (Resource) -> Unit)

    fun beforeSave(function: (Resource) -> Unit)

    fun beforeClose(function: (Resource) -> Unit)

    fun onClose(function: (Resource) -> Unit)

    fun onReopen(function: (Resource) -> Unit)

    fun execBeforeClose()

    fun initiateRename()

    fun initiateMove()

    fun initiateSave(reopen: Boolean = true)

    fun close()

    fun closeRecursive()

    fun delete()

    interface Runnable : Resource {

        val runContent: String
        val runner: RunnerManager
        override val isRunnable: Boolean get() = true

        override fun asRunnable(): Runnable {
            return this
        }
    }
}