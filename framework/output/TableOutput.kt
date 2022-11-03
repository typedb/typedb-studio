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

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.connection.TransactionState

internal class TableOutput constructor(
    val transaction: TransactionState,
    number: Int
) : RunOutput() {

    override val name: String = Label.TABLE + " ($number)"
    override val icon: Icon = Icon.TABLE
    override val buttons: List<Form.IconButtonArg> = listOf()

    internal fun outputFn(conceptMap: ConceptMap): () -> Unit {
        return {} // TODO
    }

    @Composable
    override fun content(modifier: Modifier) {
        Box(modifier) // TODO
    }
}
