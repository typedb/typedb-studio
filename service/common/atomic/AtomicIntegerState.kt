/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.common.atomic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AtomicIntegerState constructor(initValue: Int) {

    var state by mutableStateOf(initValue); private set

    fun set(value: Int) {
        state = value
    }

    fun increment() = synchronized(this) { state += 1 }

    fun decrement() = synchronized(this) { state -= 1 }
}
