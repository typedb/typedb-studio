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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Frame
import com.vaticle.typedb.studio.view.common.component.Frame.createFrameState
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.response.Response

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

    private var responseState: Response.State? by mutableStateOf(null)
    private var frameState: Frame.FrameState? by mutableStateOf(null)
    internal var tabSize by mutableStateOf(0.dp)

    internal abstract val name: String
    internal abstract val isWritable: Boolean
    internal abstract val icon: Form.IconArgs

    internal abstract fun resetFocus()
    internal abstract fun updateResourceInner(resource: Resource)

    @Composable
    abstract fun Content()

    fun updateResource(resource: Resource) {
        this.resource = resource
        this.responseState = null
        updateResourceInner(resource)
    }

    private fun responseState(paneState: Frame.PaneState): Response.State {
        if (responseState == null) responseState = Response.State(paneState, resource.name)
        return responseState!!
    }

    private fun frameState(): Frame.FrameState {
        if (frameState == null) {
            frameState = createFrameState(
                separator = Frame.SeparatorArgs(Separator.WEIGHT),
                Frame.Pane(
                    id = Page::class.java.canonicalName,
                    initSize = Either.second(1f),
                    minSize = Theme.PANEL_BAR_HEIGHT,
                    order = 1
                ) { Content() },
                Frame.Pane(
                    id = Response::class.java.canonicalName,
                    initSize = Either.second(1f),
                    minSize = Theme.PANEL_BAR_HEIGHT,
                    order = 2
                ) { paneState -> Response.Layout(responseState(paneState)) }
            )
        }
        return frameState!!
    }

    @Composable
    internal fun Layout() {
        if (!resource.isRunnable) Content()
        else { Frame.Column(state = frameState(), modifier = Modifier.fillMaxSize()) }
    }
}
