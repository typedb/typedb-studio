/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.framework.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.framework.material.Browsers
import com.vaticle.typedb.studio.framework.material.Frame
import com.vaticle.typedb.studio.framework.material.Separator
import com.vaticle.typedb.studio.framework.material.Tabs
import com.vaticle.typedb.studio.service.connection.TransactionState

class GraphVisualiser constructor(transactionState: TransactionState) {

    private val graphArea = GraphArea(transactionState)
    private val browsers: List<Browsers.Browser> = listOf(ConceptPreview(graphArea, 0, false))
    private var frameState: Frame.FrameState = Frame.createFrameState(
        separator = Frame.SeparatorArgs(Separator.WEIGHT),
        Frame.Pane(
            id = GraphArea::class.java.name,
            order = 1,
            minSize = GraphArea.MIN_WIDTH,
            initSize = Either.second(1f)
        ) { graphArea.Layout() },
        Frame.Pane(
            id = ConceptPreview::class.java.name,
            order = 2,
            minSize = Browsers.DEFAULT_WIDTH,
            initSize = Either.first(Tabs.Vertical.WIDTH),
            initFreeze = true
        ) { Browsers.Layout(browsers, it, Browsers.Position.RIGHT) }
    )

    @Composable
    fun Layout(modifier: Modifier = Modifier) {
        key(this) { Frame.Row(frameState, modifier) }
    }

    fun output(conceptMap: ConceptMap) {
        graphArea.graphState.loadConceptMap(conceptMap)
    }

    fun setCompleted() {
        graphArea.graphState.completeAllEdges()
    }
}