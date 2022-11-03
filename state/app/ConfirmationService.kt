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

package com.vaticle.typedb.studio.state.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ConfirmationService : DialogState() {

    var title: String? by mutableStateOf(null)
    var message: String? by mutableStateOf(null); private set
    var verificationValue: String? by mutableStateOf(null)
    var cancelLabel: String? by mutableStateOf(null); private set
    var rejectLabel: String? by mutableStateOf(null); private set
    var confirmLabel: String? by mutableStateOf(null); private set
    val hasReject get() = rejectLabel != null || onReject != null
    val hasConfirm get() = confirmLabel != null || onConfirm != null
    private var cancelOnConfirm by mutableStateOf(false)
    private var onReject: (() -> Unit)? by mutableStateOf(null)
    private var onConfirm: (() -> Unit)? by mutableStateOf(null)

    fun submit(
        title: String,
        message: String,
        verificationValue: String? = null,
        cancelLabel: String? = null,
        rejectLabel: String? = null,
        confirmLabel: String? = null,
        onReject: (() -> Unit)? = null,
        onConfirm: (() -> Unit)? = null,
    ) {
        this.title = title
        this.message = message
        this.verificationValue = verificationValue
        this.cancelLabel = cancelLabel
        this.rejectLabel = rejectLabel
        this.confirmLabel = confirmLabel
        this.onReject = onReject
        this.onConfirm = onConfirm
        isOpen = true
    }

    override fun close() {
        isOpen = false
        title = null
        message = null
        verificationValue = null
        rejectLabel = null
        confirmLabel = null
        onConfirm = null
        onReject = null
    }

    fun reject() {
        onReject?.let { it() }
        close()
    }

    fun confirm() {
        onConfirm?.let { it() }
        close()
    }
}
