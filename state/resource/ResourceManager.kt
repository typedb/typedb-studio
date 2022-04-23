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

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    fun isActive(resource: Resource): Boolean {
        return active == resource
    }

    fun renameAndReopen(resource: Resource) {
        val index = opened.indexOf(resource) // must be computed before passing into lambda
        resource.rename { openAndActivate(it, index) }
    }

    fun saveAndReopen(resource: Resource) {
        val index = opened.indexOf(resource) // must be computed before passing into lambda
        resource.save { openAndActivate(it, index) }
    }

    fun moveAndReopen(resource: Resource) {
        val index = opened.indexOf(resource) // must be computed before passing into lambda
        resource.move { openAndActivate(it, index) }
    }

    fun open(resource: Resource) {
        openAndActivate(resource, opened.size)
        resource.onClose { close(it) }
    }

    private fun openAndActivate(resource: Resource, index: Int) {
        if (resource !in opened) {
            if (resource.tryOpen()) opened.add(index.coerceIn(0, (opened.size).coerceAtLeast(0)), resource)
            else return
        }
        activate(resource)
    }

    fun activate(resource: Resource) {
        active?.stopWatcher()
        active = resource
        active?.launchWatcher()
    }

    fun activateNext() {
        activate(opened[(opened.indexOf(active) + 1) % opened.size])
    }

    fun activatePrevious() {
        var previousIndex = opened.indexOf(active) - 1
        if (previousIndex < 0) previousIndex += opened.size
        activate(opened[previousIndex])
    }

    fun mayRun(resource: Resource.Runnable, content: String = resource.runContent) {
        client.runner(content) { resource.runner.launch(it) }
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

    fun closeAll() {
        opened.toList().forEach { it.close() }
        active = null
    }
}