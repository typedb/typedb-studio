/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.service.common.atomic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AtomicReferenceState<T : Any> constructor(initValue: T) {

    var state: T by mutableStateOf(initValue); private set

    fun set(value: T) = synchronized(this) {
        state = value
    }

    fun compareAndSet(expected: T, new: T): Boolean = synchronized(this) {
        return if (state == expected) {
            state = new
            true
        } else false
    }
}
