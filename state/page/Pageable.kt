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

package com.vaticle.typedb.studio.state.page

import com.vaticle.typedb.studio.state.connection.RunnerManager

interface Pageable {

    val name: String
    val windowTitle: String
    val isOpen: Boolean
    val isWritable: Boolean
    val isEmpty: Boolean
    val isUnsavedPageable: Boolean
    val hasUnsavedChanges: Boolean
    val isRunnable: Boolean get() = false
    val needSaving get() = hasUnsavedChanges || (isUnsavedPageable && !isEmpty)

    fun asRunnable(): Runnable {
        throw ClassCastException("Illegal cast of pageable into runnable")
    }

    fun tryOpen(): Boolean

    fun activate()

    fun deactivate()

    fun onClose(function: (Pageable) -> Unit)

    fun onReopen(function: (Pageable) -> Unit)

    fun execBeforeClose()

    fun initiateSave(reopen: Boolean = true)

    fun close()

    fun closeRecursive()

    fun delete()

    interface Runnable : Pageable {

        val runContent: String
        val runners: RunnerManager
        val isRunning: Boolean get() = runners.isRunning
        override val isRunnable: Boolean get() = true

        fun mayOpenAndRun(content: String = runContent)

        override fun asRunnable(): Runnable {
            return this
        }
    }
}