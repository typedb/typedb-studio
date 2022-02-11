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

class ConfirmationManager {

    var showDialog by mutableStateOf(false); private set
    var title: String? by mutableStateOf(null); private set
    var message: String? by mutableStateOf(null); private set
    private var action: (() -> Unit)? by mutableStateOf(null); private set

    fun submit(title: String, message: String, action: () -> Unit) {
        this.title = title
        this.message = message
        this.action = action
        showDialog = true
    }

    fun cancel() {
        title = null
        message = null
        action = null
        showDialog = false
    }

    fun confirm() {
        showDialog = false
        action?.let { it() }
    }
}
