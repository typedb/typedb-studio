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
    var selectedPage: Pageable? by mutableStateOf(null); private set

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    fun isSelected(page: Pageable): Boolean {
        return selectedPage == page
    }

    fun select(page: Pageable) {
        selectedPage?.mayStopWatcher()
        selectedPage = page
        selectedPage?.mayLaunchWatcher()
    }

    fun saveSelectedPageAndReopen() {
        val index = openedPages.indexOf(selectedPage)
        selectedPage?.saveFile { open(it, index) }
    }

    fun open(page: Pageable) {
        open(page, openedPages.size)
    }

    fun open(page: Pageable, index: Int) {
        selectedPage?.mayStopWatcher()
        if (page !in openedPages) {
            if (page.tryOpen()) openedPages.add(index, page)
            else return
        }
        selectedPage = page
        selectedPage?.mayLaunchWatcher()
        selectedPage?.let { it.onClose { close(it) } }
    }

    fun close(page: Pageable) {
        if (!openedPages.contains(page)) return
        val selectedPageIndex = openedPages.indexOf(selectedPage)
        val closingPageIndex = openedPages.indexOf(page)
        openedPages.remove(page)
        page.close()
        val newPageIndex = when {
            selectedPageIndex > closingPageIndex -> selectedPageIndex - 1
            else -> closingPageIndex
        }.coerceIn(0, (openedPages.size - 1).coerceAtLeast(0))
        selectedPage = if (openedPages.isNotEmpty()) openedPages[newPageIndex] else null
    }

    fun closeAll() {
        openedPages.toList().forEach { it.close() }
        selectedPage = null
    }
}