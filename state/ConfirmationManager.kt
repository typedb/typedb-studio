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

package com.vaticle.typedb.studio.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.state.common.DialogManager

class ConfirmationManager : DialogManager() {

    var title: String? by mutableStateOf(null)
    var message: String? by mutableStateOf(null); private set
    var rejectLabel: String? by mutableStateOf(null); private set
    var confirmLabel: String? by mutableStateOf(null); private set
    val hasReject get() = rejectLabel != null || onReject != null
    private var cancelOnConfirm by mutableStateOf(false)
    private var onReject: (() -> Unit)? by mutableStateOf(null)
    private var onConfirm: (() -> Unit)? by mutableStateOf(null)

    fun submit(
        title: String,
        message: String,
        rejectLabel: String? = null,
        confirmLabel: String? = null,
        cancelOnConfirm: Boolean = true,
        onReject: (() -> Unit)? = null,
        onConfirm: () -> Unit,
    ) {
        this.title = title
        this.message = message
        this.rejectLabel = rejectLabel
        this.confirmLabel = confirmLabel
        this.cancelOnConfirm = cancelOnConfirm
        this.onReject = onReject
        this.onConfirm = onConfirm
        isOpen = true
    }

    override fun close() {
        isOpen = false
        title = null
        message = null
        onConfirm = null
        onReject = null
    }

    fun reject() {
        onReject?.let { it() }
        close()
    }

    fun confirm() {
        onConfirm?.let { it() }
        if (cancelOnConfirm) close()
    }
}
