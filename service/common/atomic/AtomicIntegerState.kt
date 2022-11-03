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

package com.vaticle.typedb.studio.service.common.atomic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.atomic.AtomicInteger

class AtomicIntegerState constructor(initValue: Int) {

    var state by mutableStateOf(initValue); private set
    val atomic = AtomicInteger(initValue)

    fun set(value: Int) {
        atomic.set(value)
        state = value
    }

    fun incrementAndGet(): Int = atomic.incrementAndGet().also { state = it }

    fun decrementAndGet() = atomic.decrementAndGet().also { state = it }
}