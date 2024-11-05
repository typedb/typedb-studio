/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.framework.output

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.typedb.studio.framework.graph.GraphVisualiser
import com.typedb.studio.framework.material.Form
import com.typedb.studio.framework.material.Icon
import com.typedb.studio.service.common.util.Label
import com.typedb.studio.service.connection.TransactionState
import com.vaticle.typedb.driver.api.answer.ConceptMap

internal class GraphOutput constructor(transactionState: TransactionState, number: Int) : RunOutput() {

    private val graphVisualiser = GraphVisualiser(transactionState)

    override val name: String = "${Label.GRAPH} ($number)"
    override val icon: Icon = Icon.GRAPH
    override var buttons: List<Form.IconButtonArg> = emptyList()

    fun output(conceptMap: ConceptMap) {
        graphVisualiser.output(conceptMap)
    }

    fun setCompleted() {
        graphVisualiser.setCompleted()
    }

    @Composable
    override fun content(modifier: Modifier) {
        graphVisualiser.Layout(modifier)
    }
}
