/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.framework.output

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaticle.typedb.driver.api.answer.ConceptMap
import com.vaticle.typedb.studio.framework.graph.GraphVisualiser
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.connection.TransactionState

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
