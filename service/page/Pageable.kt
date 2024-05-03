/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.service.page

import com.vaticle.typedb.studio.service.connection.QueryRunnerService

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

    fun tryDelete()

    fun close()

    interface Runnable : Pageable {

        val runContent: String
        val runners: QueryRunnerService
        val isRunning: Boolean get() = runners.isRunning
        override val isRunnable: Boolean get() = true

        fun mayOpenAndRun()

        override fun asRunnable(): Runnable {
            return this
        }
    }
}
