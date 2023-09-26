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
