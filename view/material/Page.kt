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

package com.vaticle.typedb.studio.view.material

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.view.common.theme.Theme.PANEL_BAR_HEIGHT
import com.vaticle.typedb.studio.view.material.Form.IconArg
import com.vaticle.typedb.studio.view.material.Frame.createFrameState

abstract class Page {

    companion object {
        private val CONTENT_MIN_HEIGHT = 64.dp
        private val RUN_PANEL_MIN_HEIGHT = 64.dp
    }

    private var frameState: Frame.FrameState? by mutableStateOf(null)
    protected abstract val hasSecondary: Boolean
    abstract val icon: IconArg

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
                    id = Page::class.java.canonicalName + ".primary",
                    order = 1,
                    minSize = CONTENT_MIN_HEIGHT,
                    initSize = Either.second(1f)
                ) { PrimaryContent() },
                Frame.Pane(
                    id = Page::class.java.canonicalName + ".secondary",
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
    fun Layout() {
        if (!hasSecondary) PrimaryContent()
        else Frame.Column(
            state = frameState(),
            modifier = Modifier.fillMaxSize()
        )
    }
}
