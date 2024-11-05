/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.common.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

abstract class DialogState {

    var isOpen by mutableStateOf(false)

    fun toggle() {
        isOpen = !isOpen
    }

    open fun close() {
        isOpen = false
    }

    class Base : DialogState() {

        fun open() {
            isOpen = true
        }
    }
}
