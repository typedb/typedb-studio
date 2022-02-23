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

interface Pageable {

    val name: String
    val fullName: String
    val isOpen: Boolean
    val isUnsaved: Boolean
    val isUnsavedFile: Boolean
    val isRunnable: Boolean
    val onClosePage: (() -> Unit)?

    fun tryOpen(): Boolean

    fun launchWatcher()

    fun stopWatcher()

    fun onWatch(function: () -> Unit)

    fun onSave(function: () -> Unit)

    fun onClose(function: () -> Unit)

    fun onClosePage(function: () -> Unit)

    fun rename(onSuccess: ((Pageable) -> Unit)? = null)

    fun save(onSuccess: ((Pageable) -> Unit)? = null)

    fun move(onSuccess: ((Pageable) -> Unit)? = null)

    fun delete()

    fun close()
}