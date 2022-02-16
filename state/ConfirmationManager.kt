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

class ConfirmationManager {

    val dialog = DialogManager.Base()
    var title: String? by mutableStateOf(null); private set
    var message: String? by mutableStateOf(null); private set
    var cancelLabel: String? by mutableStateOf(null); private set
    var confirmLabel: String? by mutableStateOf(null); private set
    private var onConfirm: (() -> Unit)? by mutableStateOf(null); private set
    private var onCancel: (() -> Unit)? by mutableStateOf(null); private set

    fun submit(
        title: String,
        message: String,
        cancelLabel: String? = null,
        confirmLabel: String? = null,
        onCancel: (() -> Unit)? = null,
        onConfirm: () -> Unit,
    ) {
        this.title = title
        this.message = message
        this.cancelLabel = cancelLabel
        this.confirmLabel = confirmLabel
        this.onCancel = onCancel
        this.onConfirm = onConfirm
        dialog.open()
    }

    fun cancel() {
        onCancel?.let { it() }
        dialog.close()
        title = null
        message = null
        onConfirm = null
        onCancel = null
    }

    fun confirm() {
        onConfirm?.let { it() }
        dialog.close()
    }
}
