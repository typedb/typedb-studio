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
import com.vaticle.typedb.studio.state.schema.TypeState
import com.vaticle.typedb.studio.view.common.component.Form.IconArg
import com.vaticle.typedb.studio.view.common.component.Frame
import com.vaticle.typedb.studio.view.common.component.Frame.createFrameState
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme.PANEL_BAR_HEIGHT
import com.vaticle.typedb.studio.view.output.RunOutputArea

abstract class Page {

    companion object {

        private val CONTENT_MIN_HEIGHT = 64.dp
        private val RUN_PANEL_MIN_HEIGHT = 64.dp

        @Composable
        fun of(resource: Resource): Page {
            return when (resource) {
                is File -> FilePage.create(resource)
                is TypeState.Thing -> TypePage.create(resource)
                else -> throw IllegalStateException("Unrecognised resource type")
            }
        }
    }


    private var frameState: Frame.FrameState? by mutableStateOf(null)
    internal var tabSize by mutableStateOf(0.dp)

    protected abstract val hasSecondary: Boolean
    internal abstract val icon: IconArg

    abstract fun updateResource(resource: Resource)

    @Composable
    abstract fun PrimaryContent()

    @Composable
    protected open fun SecondaryContent(paneState: Frame.PaneState) {}

    @Composable
    private fun frameState(): Frame.FrameState {
        if (frameState == null) {
            frameState = createFrameState(
                separator = Frame.SeparatorArgs(Separator.WEIGHT),
                Frame.Pane(
                    id = Page::class.java.canonicalName,
                    order = 1,
                    minSize = CONTENT_MIN_HEIGHT,
                    initSize = Either.second(1f)
                ) { PrimaryContent() },
                Frame.Pane(
                    id = RunOutputArea::class.java.canonicalName,
                    order = 2,
                    minSize = RUN_PANEL_MIN_HEIGHT,
                    initSize = Either.first(PANEL_BAR_HEIGHT),
                    initFreeze = true
                ) { paneState -> SecondaryContent(paneState) }
            )
        }
        return frameState!!
    }

    @Composable
    internal fun Layout() {
        if (!hasSecondary) PrimaryContent()
        else Frame.Column(state = frameState(), modifier = Modifier.fillMaxSize())
    }
}
