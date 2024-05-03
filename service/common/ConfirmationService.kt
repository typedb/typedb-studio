/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.service.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.service.common.util.DialogState

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
