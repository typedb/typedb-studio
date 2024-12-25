/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.graph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.typedb.studio.framework.material.Browsers
import com.typedb.studio.framework.material.Frame
import com.typedb.studio.framework.material.Separator
import com.typedb.studio.framework.material.Tabs
import com.typedb.studio.service.connection.TransactionState
import com.typedb.common.collection.Either
import com.typedb.driver.api.answer.ConceptRow

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

    fun output(conceptMap: ConceptRow) {
        graphArea.graphBuilder.loadConceptRow(conceptMap)
    }

    fun setCompleted() {
        graphArea.graphBuilder.completeAllEdges()
    }
}
