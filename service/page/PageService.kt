/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.service.page

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import mu.KotlinLogging

class PageService {

    val opened: MutableList<Pageable> = mutableStateListOf()
    var active: Pageable? by mutableStateOf(null); private set
    val next: Pageable get() = opened[(opened.indexOf(active) + 1).mod(opened.size)]
    val previous: Pageable get() = opened[(opened.indexOf(active) - 1).mod(opened.size)]

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
