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
import java.lang.Integer.max

class PageManager(val notification: NotificationManager) {

    val openedPages: MutableList<Editable> = mutableStateListOf()
    var selectedPage: Editable? by mutableStateOf(null)

    fun isSelected(page: Editable): Boolean {
        return selectedPage == page
    }

    fun open(page: Editable) {
        if (page !in openedPages) {
            page.open()
            openedPages.add(page)
        }
        selectedPage = page
    }

    fun close(page: Editable) {
        val newPageIndex = max(openedPages.indexOf(page) - 1, 0)
        openedPages.remove(page)
        page.close()
        selectedPage = if (openedPages.isNotEmpty()) openedPages[newPageIndex] else null
    }
}