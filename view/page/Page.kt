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

package com.vaticle.typedb.studio.view.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.view.common.component.Form

abstract class Page(var resource: Resource) {

    companion object {
        @Composable
        fun of(resource: Resource): Page {
            return when (resource) {
                is File -> FilePage.create(resource)
                else -> throw IllegalStateException("should never be reached")
            }
        }
    }

    abstract val name: String
    abstract val isWritable: Boolean
    abstract val icon: Form.IconArgs

    var tabSize by mutableStateOf(0.dp)

    abstract fun resetFocus()
    abstract fun updateResourceInner(resource: Resource)

    @Composable
    abstract fun Layout()

    fun updateResource(resource: Resource) {
        this.resource = resource
        updateResourceInner(resource)
    }
}
