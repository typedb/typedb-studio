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

package com.vaticle.typedb.studio.state.resource

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.connection.ClientState
import mu.KotlinLogging

class ResourceManager(
    private val client: ClientState,
    private val notificationMgr: NotificationManager
) {

    val opened: MutableList<Resource> = mutableStateListOf()
    var active: Resource? by mutableStateOf(null); private set
    val next: Resource get() = opened[(opened.indexOf(active) + 1).mod(opened.size)]
    val previous: Resource get() = opened[(opened.indexOf(active) - 1).mod(opened.size)]

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    fun opened(resource: Resource, index: Int? = null) {
        val i = index ?: opened.size
        if (resource !in opened) opened.add(i.coerceIn(0, (opened.size).coerceAtLeast(0)), resource)
        active(resource)
    }

    fun active(resource: Resource) {
        if (active == resource) return
        active?.deactivate()
        active = resource
    }

    fun openAndMayRun(resource: Resource.Runnable, content: String = resource.runContent) {
        resource.tryOpen()
        client.runner(content)?.let { resource.runner.launch(it) }
    }

    fun close(resource: Resource) {
        if (!opened.contains(resource)) return
        val activeIndex = opened.indexOf(active)
        val closingIndex = opened.indexOf(resource)
        opened.remove(resource)
        resource.close()
        val replacementIndex = when {
            activeIndex > closingIndex -> activeIndex - 1
            else -> closingIndex
        }.coerceIn(0, (opened.size - 1).coerceAtLeast(0))
        active = if (opened.isNotEmpty()) opened[replacementIndex] else null
    }
}