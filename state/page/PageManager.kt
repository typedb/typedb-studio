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

package com.vaticle.typedb.studio.state.page

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mu.KotlinLogging

class PageManager {

    val opened: MutableList<Pageable> = mutableStateListOf()
    var active: Pageable? by mutableStateOf(null); private set
    val next: Pageable get() = opened[(opened.indexOf(active) + 1).mod(opened.size)]
    val previous: Pageable get() = opened[(opened.indexOf(active) - 1).mod(opened.size)]

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    fun opened(pageable: Pageable, index: Int? = null) {
        val i = index ?: opened.size
        if (pageable !in opened) opened.add(i.coerceIn(0, (opened.size).coerceAtLeast(0)), pageable)
        active(pageable)
    }

    fun active(pageable: Pageable) {
        if (active == pageable) return
        active?.deactivate()
        active = pageable
    }

    fun close(pageable: Pageable) {
        if (!opened.contains(pageable)) return
        val activeIndex = opened.indexOf(active)
        val closingIndex = opened.indexOf(pageable)
        opened.remove(pageable)
        pageable.close()
        val replacementIndex = when {
            activeIndex > closingIndex -> activeIndex - 1
            else -> closingIndex
        }.coerceIn(0, (opened.size - 1).coerceAtLeast(0))
        active = if (opened.isNotEmpty()) opened[replacementIndex] else null
    }
}