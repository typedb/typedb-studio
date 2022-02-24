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

package com.vaticle.typedb.studio.state.page

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.state.notification.NotificationManager
import mu.KotlinLogging

class PageManager(val notification: NotificationManager) {

    val openedPages: MutableList<Pageable> = mutableStateListOf()
    var activePage: Pageable? by mutableStateOf(null); private set

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    fun isActive(page: Pageable): Boolean {
        return activePage == page
    }

    fun activate(page: Pageable) {
        activePage?.stopWatcher()
        activePage = page
        activePage?.launchWatcher()
    }

    fun renameAndReopen(page: Pageable) {
        val index = openedPages.indexOf(page) // must be computed before passing into lambda
        page.rename { open(it, index) }
    }

    fun saveAndReopen(page: Pageable) {
        val index = openedPages.indexOf(page) // must be computed before passing into lambda
        page.save { open(it, index) }
    }

    fun moveAndReopen(page: Pageable) {
        val index = openedPages.indexOf(page) // must be computed before passing into lambda
        page.move { open(it, index) }
    }

    fun open(page: Pageable) {
        open(page, openedPages.size)
    }

    fun open(page: Pageable, index: Int) {
        activePage?.stopWatcher()
        if (page !in openedPages) {
            if (page.tryOpen()) openedPages.add(index, page)
            else return
        }
        activePage = page
        activePage?.launchWatcher()
        activePage?.let { it.onClose { close(it) } }
    }

    fun close(page: Pageable) {
        if (!openedPages.contains(page)) return
        val activePageIndex = openedPages.indexOf(activePage)
        val closingPageIndex = openedPages.indexOf(page)
        openedPages.remove(page)
        page.close()
        val newPageIndex = when {
            activePageIndex > closingPageIndex -> activePageIndex - 1
            else -> closingPageIndex
        }.coerceIn(0, (openedPages.size - 1).coerceAtLeast(0))
        activePage = if (openedPages.isNotEmpty()) openedPages[newPageIndex] else null
    }

    fun closeAll() {
        openedPages.toList().forEach { it.close() }
        activePage = null
    }
}